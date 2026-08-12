package com.springboot.ruon.domain.scan.dto.response;

import java.util.List;

public record AnalysisSummaryResponse(
        OverallStatus overallStatus,
        AnalysisCategory category,
        Card primaryCard,
        Card secondaryCard,
        Summary summary,
        List<CautionIngredient> cautionIngredients,
        List<String> unknownIngredients) {

    public enum OverallStatus {
        CAUTION,
        NO_CAUTION_FOUND
    }

    public enum IconType {
        WARNING,
        CHECK,
        BENEFIT,
        INFO
    }

    public enum AnalysisCategory {
        KEEP_USING,
        PAUSE,
        SELECTIVE_USE,
        NEEDS_REVIEW
    }

    public record Card(String title, String description, IconType iconType) {
    }

    public record Summary(
            int totalCount,
            int matchedCount,
            int cautionCount,
            int unknownCount) {
    }

    public record CautionIngredient(
            String input,
            String korName,
            String inciName,
            String function,
            Boolean pregnancySafe,
            String pregnancyNotes,
            String source) {
    }
}
