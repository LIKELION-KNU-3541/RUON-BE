package com.springboot.ruon.domain.rag.controller;

import com.springboot.ruon.domain.rag.dto.request.PregnancyCheckRequest;
import com.springboot.ruon.domain.rag.dto.request.RagAnswerRequest;
import com.springboot.ruon.domain.rag.dto.request.IngredientAnalysisRequest;
import com.springboot.ruon.domain.rag.dto.response.PregnancyCheckResponse;
import com.springboot.ruon.domain.rag.dto.response.RagAnswerResponse;
import com.springboot.ruon.domain.rag.dto.response.IngredientAnalysisResponse;
import com.springboot.ruon.domain.rag.service.RagService;
import com.springboot.ruon.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    @PostMapping("/answer")
    public ResponseEntity<ApiResponse<RagAnswerResponse>> answer(
            @Valid @RequestBody RagAnswerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ragService.answer(request)));
    }

    @PostMapping("/pregnancy-check")
    public ResponseEntity<ApiResponse<PregnancyCheckResponse>> checkPregnancySafety(
            @Valid @RequestBody PregnancyCheckRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ragService.checkPregnancySafety(request)));
    }

    @PostMapping("/ingredient-analysis")
    public ResponseEntity<ApiResponse<IngredientAnalysisResponse>> analyzeIngredients(
            @Valid @RequestBody IngredientAnalysisRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ragService.analyzeIngredients(request)));
    }
}
