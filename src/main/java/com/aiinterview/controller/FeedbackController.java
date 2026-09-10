package com.aiinterview.controller;

import com.aiinterview.dto.request.FeedbackRequest;
import com.aiinterview.dto.request.QuestionGenerateRequest;
import com.aiinterview.dto.response.CompanyResponse;
import com.aiinterview.dto.response.FeedbackResponse;
import com.aiinterview.dto.response.QuestionGenerateResponse;
import com.aiinterview.service.FeedbackService;
import com.aiinterview.service.QuestionGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final QuestionGenerationService questionGenerationService;

    /** 기관을 고르기 위한 목록. 인재상 확인 여부까지 같이 준다. */
    @GetMapping("/companies")
    public List<CompanyResponse> companies() {
        return feedbackService.selectableCompanies();
    }

    @PostMapping
    public FeedbackResponse feedback(@Valid @RequestBody FeedbackRequest request) {
        return feedbackService.giveFeedback(request);
    }

    /** 기관 인재상에 맞는 면접 질문을 만들어준다. 만든 질문은 저장돼 그대로 연습에 쓸 수 있다. */
    @PostMapping("/questions")
    public QuestionGenerateResponse generateQuestions(@Valid @RequestBody QuestionGenerateRequest request) {
        return questionGenerationService.generate(request);
    }
}
