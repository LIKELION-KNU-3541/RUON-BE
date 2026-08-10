package com.springboot.ruon.domain.rag.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record IngredientAnalysisRequest(
        @NotEmpty @Size(max = 200) List<@Valid @NotBlank String> ingredients) {
}
