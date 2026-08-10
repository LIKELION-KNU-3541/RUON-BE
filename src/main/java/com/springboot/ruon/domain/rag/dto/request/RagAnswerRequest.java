package com.springboot.ruon.domain.rag.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RagAnswerRequest(
        @NotBlank String query,
        @Min(1) @Max(20) Integer topK) {

    public RagAnswerRequest {
        if (topK == null) {
            topK = 6;
        }
    }
}
