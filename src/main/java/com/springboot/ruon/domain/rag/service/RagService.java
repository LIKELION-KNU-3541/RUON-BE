package com.springboot.ruon.domain.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.ruon.domain.rag.client.RagClient;
import com.springboot.ruon.domain.rag.dto.request.IngredientAnalysisRequest;
import com.springboot.ruon.domain.rag.dto.request.PregnancyCheckRequest;
import com.springboot.ruon.domain.rag.dto.request.RagAnswerRequest;
import com.springboot.ruon.domain.rag.dto.response.PregnancyCheckResponse;
import com.springboot.ruon.domain.rag.dto.response.RagAnswerResponse;
import com.springboot.ruon.domain.rag.dto.response.IngredientAnalysisResponse;
import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RagService {

    private final RagClient ragClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RagAnswerResponse answer(RagAnswerRequest request) {
        return ragClient.answer(request.query(), request.topK());
    }

    public PregnancyCheckResponse checkPregnancySafety(PregnancyCheckRequest request) {
        return ragClient.checkPregnancySafety(request.ingredients());
    }

    public IngredientAnalysisResponse analyzeIngredients(IngredientAnalysisRequest request) {
        return analyzeIngredients(request.ingredients());
    }

    public IngredientAnalysisResponse analyzeIngredients(List<String> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            throw new CustomException(ErrorCode.INGREDIENT_ANALYSIS_FAILED);
        }
        return ragClient.analyzeIngredients(ingredients);
    }

    public String toJson(IngredientAnalysisResponse analysis) {
        try {
            return objectMapper.writeValueAsString(analysis);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INGREDIENT_ANALYSIS_FAILED, "분석 결과 직렬화 실패", e);
        }
    }

    public IngredientAnalysisResponse fromJson(String analysisResult) {
        if (analysisResult == null || analysisResult.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(analysisResult, IngredientAnalysisResponse.class);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INGREDIENT_ANALYSIS_FAILED, "저장된 분석 결과 역직렬화 실패", e);
        }
    }
}
