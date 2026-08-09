package com.springboot.ruon.domain.rag.dto.response;

import java.util.List;

public record PregnancyCheckResponse(
        int totalChecked,
        boolean pregnancySafe,
        List<Warning> warnings,
        List<String> unknownIngredients) {

    public record Warning(
            String input,
            String korName,
            String inciName,
            String reason) {
    }
}
