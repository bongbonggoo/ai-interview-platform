package com.aiinterview.controller;

import com.aiinterview.dto.request.FeedbackRequest;
import com.aiinterview.dto.response.CompanyResponse;
import com.aiinterview.dto.response.FeedbackResponse;
import com.aiinterview.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /** 기관을 고르기 위한 목록. 인재상 확인 여부까지 같이 준다. */
    @GetMapping("/companies")
    public List<CompanyResponse> companies() {
        return feedbackService.selectableCompanies();
    }

    @PostMapping
    public FeedbackResponse feedback(@Valid @RequestBody FeedbackRequest request) {
        return feedbackService.giveFeedback(request);
    }
}
