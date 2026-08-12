package com.springboot.ruon.domain.scan.dto.response;

import java.util.List;

public record AnalysisSummaryResponse(
        OverallStatus overallStatus,
        Card primaryCard,
        Card secondaryCard,
        Summary summary,
        List<CautionIngredient> cautionIngredients,
        List<String> unknownIngredients) {

    public enum OverallStatus {
        CAUTION,
        UNKNOWN,
        NO_CAUTION_FOUND
    }

    public enum IconType {
        WARNING,
        UNKNOWN,
        CHECK,
        BENEFIT,
        INFO
    }

    public record Card(String title, String description, IconType iconType) {
    }

    public record Summary(
            int totalCount,
            int matchedCount,
            int cautionCount,
            int unclassifiedCount,
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
