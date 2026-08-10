package com.springboot.ruon.domain.rag.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.springboot.ruon.domain.rag.dto.response.PregnancyCheckResponse;
import com.springboot.ruon.domain.rag.dto.response.RagAnswerResponse;
import com.springboot.ruon.domain.rag.dto.response.IngredientAnalysisResponse;
import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RagClient {

    private final RestClient restClient;

    public RagClient(@Qualifier("ragRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public RagAnswerResponse answer(String query, int topK) {
        AnswerPayload response = post("/answer", new AnswerRequestPayload(query, topK), AnswerPayload.class);
        List<RagAnswerResponse.Source> sources = response.sources() == null
                ? List.of()
                : response.sources().stream()
                        .map(source -> new RagAnswerResponse.Source(source.inciName(), source.korName()))
                        .toList();
        return new RagAnswerResponse(response.answer(), sources);
    }

    public PregnancyCheckResponse checkPregnancySafety(List<String> ingredients) {
        PregnancyCheckPayload response = post(
                "/pregnancy-check",
                new PregnancyCheckRequestPayload(ingredients),
                PregnancyCheckPayload.class);
        List<PregnancyCheckResponse.Warning> warnings = response.warnings() == null
                ? List.of()
                : response.warnings().stream()
                        .map(warning -> new PregnancyCheckResponse.Warning(
                                warning.input(),
                                warning.korName(),
                                warning.inciName(),
                                warning.reason()))
                        .toList();
        return new PregnancyCheckResponse(
                response.totalChecked(),
                response.pregnancySafe(),
                warnings,
                response.unknownIngredients() == null ? List.of() : response.unknownIngredients());
    }

    public IngredientAnalysisResponse analyzeIngredients(List<String> ingredients) {
        IngredientAnalysisResponse response = post(
                "/ingredient-analysis",
                new IngredientAnalysisRequestPayload(ingredients),
                IngredientAnalysisResponse.class);
        return new IngredientAnalysisResponse(
                response.totalChecked(),
                response.matchedCount(),
                response.cautionCount(),
                response.analyzedIngredients() == null ? List.of() : response.analyzedIngredients(),
                response.unknownIngredients() == null ? List.of() : response.unknownIngredients());
    }

    private <T> T post(String uri, Object request, Class<T> responseType) {
        try {
            T response = restClient.post()
                    .uri(uri)
                    .body(request)
                    .retrieve()
                    .body(responseType);
            if (response == null) {
                throw new CustomException(ErrorCode.RAG_SERVICE_ERROR);
            }
            return response;
        } catch (ResourceAccessException e) {
            throw new CustomException(ErrorCode.RAG_SERVICE_UNAVAILABLE, "RAG service connection failed", e);
        } catch (RestClientResponseException e) {
            throw new CustomException(
                    ErrorCode.RAG_SERVICE_ERROR,
                    "RAG service returned HTTP " + e.getStatusCode().value(),
                    e);
        } catch (RestClientException e) {
            throw new CustomException(ErrorCode.RAG_SERVICE_ERROR, "Invalid RAG service response", e);
        }
    }

    private record AnswerRequestPayload(String query, @JsonProperty("top_k") int topK) {
    }

    private record PregnancyCheckRequestPayload(List<String> ingredients) {
    }

    private record IngredientAnalysisRequestPayload(List<String> ingredients) {
    }

    private record AnswerPayload(String answer, List<SourcePayload> sources) {
    }

    private record SourcePayload(
            @JsonProperty("inci_name") String inciName,
            @JsonProperty("kor_name") String korName) {
    }

    private record PregnancyCheckPayload(
            int totalChecked,
            boolean pregnancySafe,
            List<WarningPayload> warnings,
            List<String> unknownIngredients) {
    }

    private record WarningPayload(
            String input,
            @JsonProperty("kor_name") String korName,
            @JsonProperty("inci_name") String inciName,
            String reason) {
    }
}
