package com.aiinterview.service.scorer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/** 질문 생성 프롬프트와 파싱. 모델이 바뀌어도 질문의 성격이 갈리지 않도록 한 곳에 둔다. */
final class QuestionPrompt {

    private QuestionPrompt() {}

    static String system() {
        return """
                당신은 공기업 면접관입니다. 지원자가 연습할 수 있도록 면접 질문을 만듭니다.

                질문을 만드는 원칙:
                1. 주어진 그 기관의 인재상(역량)을 확인하기 위한 질문만 만듭니다.
                   역량마다 최소 하나씩은 다뤄지게 하되, 한 질문이 두 역량을 함께 볼 수도 있습니다.
                2. 경험을 묻는 질문으로 만듭니다. "~에 대해 어떻게 생각하십니까" 같은 의견 질문보다
                   "~한 경험을 말씀해 주십시오", "~했을 때 어떻게 하셨습니까"처럼
                   지원자가 실제로 한 행동을 꺼내게 하는 질문이 좋습니다.
                3. 그 기관의 업무 맥락(하는 일, 마주치는 상황)을 질문에 녹입니다.
                   단, 당신이 모르는 그 기관의 구체적 사실을 지어내지 마세요.
                   확실하지 않으면 일반적인 업무 상황으로 씁니다.
                4. 실제 면접에서 쓰는 존댓말 한 문장으로 씁니다. 질문은 한 번에 하나만 묻습니다.

                절대 하지 말 것:
                - 모범답안, 예시 답변, 답변 요령, "이렇게 답하면 좋습니다" 같은 힌트
                - 지원자가 무엇을 말해야 하는지 알려주는 문장
                intent에는 "이 질문으로 무엇을 보려는가"만 적습니다.
                지원자가 볼 수 있는 내용이므로 답을 알려주는 표현은 쓰지 마세요.

                설명 없이 아래 JSON만 출력하세요.
                {"questions":[{"questionText":"<질문 한 문장>","category":"<인성|직무|상황>",
                "targetCanonicalIds":["<주어진 canonicalId>"],"intent":"<이 질문으로 확인하려는 것>"}]}
                """;
    }

    static String user(QuestionContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("[기관] ").append(context.companyName()).append('\n');
        if (context.jobTitle() != null && !context.jobTitle().isBlank()) {
            sb.append("[직무] ").append(context.jobTitle()).append('\n');
        }
        if (!context.competencyConfirmed()) {
            sb.append("(이 기관의 인재상 원문을 확보하지 못해 공기업 공통 역량으로 질문을 만듭니다)\n");
        }
        sb.append("[만들 질문 수] ").append(context.count()).append("개\n\n");
        sb.append("[이 기관이 보는 역량]\n");
        for (QuestionContext.Competency c : context.competencies()) {
            sb.append("- canonicalId: ").append(c.canonicalId()).append('\n');
            sb.append("  역량: ").append(c.companyLabel());
            if (!c.companyLabel().equals(c.label())) {
                sb.append(" (공통 역량 분류: ").append(c.label()).append(")");
            }
            sb.append('\n');
            if (c.definition() != null && !c.definition().isBlank()) {
                sb.append("  정의: ").append(c.definition()).append('\n');
            }
        }
        return sb.toString();
    }

    static List<GeneratedQuestion> parse(ObjectMapper mapper, String text) {
        String json = stripToJsonObject(text);
        try {
            JsonNode questions = mapper.readTree(json).path("questions");
            if (!questions.isArray() || questions.isEmpty()) {
                throw new IllegalArgumentException("questions 배열이 없습니다");
            }
            List<GeneratedQuestion> parsed = new ArrayList<>();
            for (JsonNode n : questions) {
                List<String> targets = new ArrayList<>();
                n.path("targetCanonicalIds").forEach(t -> targets.add(t.asText()));
                parsed.add(new GeneratedQuestion(
                        n.path("questionText").asText(""),
                        n.path("category").asText("직무"),
                        targets,
                        n.path("intent").asText("")));
            }
            return parsed;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "질문 생성 응답을 해석할 수 없습니다: " + e.getMessage(), e);
        }
    }

    private static String stripToJsonObject(String text) {
        if (text == null) return "";
        int start = text.indexOf('{'), end = text.lastIndexOf('}');
        return (start >= 0 && end > start) ? text.substring(start, end + 1) : text;
    }
}
