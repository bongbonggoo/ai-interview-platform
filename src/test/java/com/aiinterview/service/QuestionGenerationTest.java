package com.aiinterview.service;

import com.aiinterview.dto.request.QuestionGenerateRequest;
import com.aiinterview.dto.response.QuestionGenerateResponse;
import com.aiinterview.repository.QuestionRepository;
import com.aiinterview.service.scorer.GeneratedQuestion;
import com.aiinterview.service.scorer.QuestionContext;
import com.aiinterview.service.scorer.QuestionGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 기관을 고르면 그 기관 인재상에 맞는 면접 질문이 나오고, 그대로 연습에 쓸 수 있어야 한다. */
@SpringBootTest
class QuestionGenerationTest {

    static class ProgrammableQuestions implements QuestionGenerator {
        Function<QuestionContext, List<GeneratedQuestion>> behaviour = ctx -> ctx.competencies().stream()
                .map(c -> new GeneratedQuestion(
                        c.companyLabel() + "이(가) 드러난 경험을 말씀해 주십시오.",
                        "직무", List.of(c.canonicalId()), "확인하려는 것"))
                .toList();
        QuestionContext last;

        @Override public List<GeneratedQuestion> generate(QuestionContext context) {
            last = context;
            return behaviour.apply(context);
        }
        @Override public String name() { return "programmable"; }
    }

    @TestConfiguration
    static class Config {
        @Bean @Primary ProgrammableQuestions programmableQuestions() { return new ProgrammableQuestions(); }
    }

    @Autowired ProgrammableQuestions generator;
    @Autowired QuestionGenerationService service;
    @Autowired QuestionRepository questionRepository;

    @BeforeEach
    void reset() {
        generator.behaviour = ctx -> ctx.competencies().stream()
                .map(c -> new GeneratedQuestion(
                        c.companyLabel() + "이(가) 드러난 경험을 말씀해 주십시오.",
                        "직무", List.of(c.canonicalId()), "확인하려는 것"))
                .toList();
    }

    @Test
    @DisplayName("질문은 그 기관의 인재상에서 만들어지고, 출처가 함께 나온다")
    void generatesFromCompanyCompetencies() {
        QuestionGenerateResponse res = service.generate(
                new QuestionGenerateRequest("코레일", "사무영업", 3));

        assertThat(res.companyName()).isEqualTo("한국철도공사");
        assertThat(res.competencyConfirmed()).isTrue();
        assertThat(res.sourceUrl()).contains("korail.com");
        assertThat(res.questions()).extracting(q -> q.targets().get(0).companyLabel())
                .containsExactlyInAnyOrder("사람지향 소통인", "고객지향 전문인", "미래지향 혁신인");

        // 프롬프트에 직무와 역량 정의가 실렸는가
        assertThat(generator.last.jobTitle()).isEqualTo("사무영업");
        assertThat(generator.last.competencies()).allSatisfy(c ->
                assertThat(c.definition()).isNotBlank());
    }

    @Test
    @DisplayName("만든 질문은 저장돼 그대로 면접 연습에 쓸 수 있다")
    void generatedQuestionsArePersisted() {
        QuestionGenerateResponse res = service.generate(new QuestionGenerateRequest("한국전력공사", null, 4));

        assertThat(res.questions()).isNotEmpty();
        res.questions().forEach(q -> {
            assertThat(q.questionId()).isNotNull();
            assertThat(questionRepository.findById(q.questionId())).isPresent();
        });
    }

    @Test
    @DisplayName("인재상 미확인 기관은 공통 역량으로 질문을 만들고 그 사실을 드러낸다")
    void unconfirmedCompanyUsesCommonCompetencies() {
        QuestionGenerateResponse res = service.generate(new QuestionGenerateRequest("금감원", null, 5));

        assertThat(res.companyName()).isEqualTo("금융감독원");
        assertThat(res.competencyConfirmed()).isFalse();
        assertThat(res.sourceUrl()).isNull();
        assertThat(generator.last.competencyConfirmed()).isFalse();
        assertThat(res.questions()).extracting(q -> q.targets().get(0).canonicalId())
                .containsExactly("COMMUNICATION", "PROBLEM_SOLVING", "INTERPERSONAL",
                        "ORGANIZATIONAL_UNDERSTANDING", "WORK_ETHIC");
    }

    @Test
    @DisplayName("빈 질문이나 기관 역량과 무관한 질문은 저장하지 않고 502")
    void rejectsBrokenOutput() {
        generator.behaviour = ctx -> List.of(
                new GeneratedQuestion("", "직무", List.of(ctx.competencies().get(0).canonicalId()), ""));
        assertThatThrownBy(() -> service.generate(new QuestionGenerateRequest("코레일", null, 3)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);

        generator.behaviour = ctx -> List.of(
                new GeneratedQuestion("엉뚱한 질문", "직무", List.of("NOT_A_COMPETENCY"), ""));
        assertThatThrownBy(() -> service.generate(new QuestionGenerateRequest("코레일", null, 3)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    @DisplayName("등록되지 않은 기관은 404")
    void unknownCompany() {
        assertThatThrownBy(() -> service.generate(new QuestionGenerateRequest("없는공사", null, 3)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
