package com.springboot.ruon.domain.rag.dto.response;

import java.util.List;

public record IngredientAnalysisResponse(
        int totalChecked,
        int matchedCount,
        int cautionCount,
        List<Ingredient> analyzedIngredients,
        List<String> unknownIngredients) {

    public record Ingredient(
            String input,
            String korName,
            String inciName,
            String synonyms,
            String casNo,
            String function,
            String origin,
            String usageLimit,
            String caution,
            String description,
            String irritancyPotential,
            Integer comedogenicityRating,
            Integer safetyScore,
            Boolean pregnancySafe,
            String pregnancyNotes,
            Boolean allergen,
            String source) {
    }
}
