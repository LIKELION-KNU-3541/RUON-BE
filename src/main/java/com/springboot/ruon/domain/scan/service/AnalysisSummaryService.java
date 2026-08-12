package com.springboot.ruon.domain.scan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.ruon.domain.rag.dto.response.IngredientAnalysisResponse;
import com.springboot.ruon.domain.rag.dto.response.IngredientAnalysisResponse.Ingredient;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.Card;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.CautionIngredient;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.IconType;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.OverallStatus;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.Summary;
import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AnalysisSummaryService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisSummaryResponse create(IngredientAnalysisResponse analysis) {
        List<Ingredient> analyzed = safeList(analysis.analyzedIngredients());
        List<String> unknown = safeList(analysis.unknownIngredients());
        List<Ingredient> cautions = analyzed.stream()
                .filter(ingredient -> Boolean.FALSE.equals(ingredient.pregnancySafe()))
                .toList();
        int unclassifiedCount = (int) analyzed.stream()
                .filter(ingredient -> ingredient.pregnancySafe() == null)
                .count();

        OverallStatus status = resolveStatus(cautions.size(), unclassifiedCount, unknown.size());
        Summary summary = new Summary(
                analysis.totalChecked(),
                analysis.matchedCount(),
                cautions.size(),
                unclassifiedCount,
                unknown.size());

        return new AnalysisSummaryResponse(
                status,
                createPrimaryCard(status, summary),
                createSecondaryCard(analyzed, analysis.matchedCount()),
                summary,
                cautions.stream().map(this::toCautionIngredient).toList(),
                unknown);
    }

    public String toJson(AnalysisSummaryResponse summary) {
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INGREDIENT_ANALYSIS_FAILED, "화면 요약 직렬화 실패", e);
        }
    }

    public AnalysisSummaryResponse fromJson(String summaryResult) {
        if (summaryResult == null || summaryResult.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(summaryResult, AnalysisSummaryResponse.class);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INGREDIENT_ANALYSIS_FAILED, "저장된 화면 요약 역직렬화 실패", e);
        }
    }

    private OverallStatus resolveStatus(int cautionCount, int unclassifiedCount, int unknownCount) {
        if (cautionCount > 0) {
            return OverallStatus.CAUTION;
        }
        if (unclassifiedCount > 0 || unknownCount > 0) {
            return OverallStatus.UNKNOWN;
        }
        return OverallStatus.NO_CAUTION_FOUND;
    }

    private Card createPrimaryCard(OverallStatus status, Summary summary) {
        return switch (status) {
            case CAUTION -> new Card(
                    "사용 전 확인이 필요해요",
                    "임신 중 주의가 필요한 성분 " + summary.cautionCount() + "개가 확인됐어요.",
                    IconType.WARNING);
            case UNKNOWN -> new Card(
                    "확인된 주의 성분은 없어요",
                    unknownDescription(summary),
                    IconType.UNKNOWN);
            case NO_CAUTION_FOUND -> new Card(
                    "확인된 주의 성분이 없어요",
                    "분석된 " + summary.matchedCount() + "개 성분에서는 임신 중 주의 성분이 발견되지 않았어요.",
                    IconType.CHECK);
        };
    }

    private String unknownDescription(Summary summary) {
        List<String> reasons = new ArrayList<>();
        if (summary.unclassifiedCount() > 0) {
            reasons.add("임신 안전성 정보가 없는 성분 " + summary.unclassifiedCount() + "개");
        }
        if (summary.unknownCount() > 0) {
            reasons.add("데이터에서 확인하지 못한 성분 " + summary.unknownCount() + "개");
        }
        return String.join(", ", reasons) + "가 포함되어 있어 사용 가능 여부를 단정하기 어려워요.";
    }

    private Card createSecondaryCard(List<Ingredient> analyzed, int matchedCount) {
        Map<BenefitCategory, Integer> counts = new EnumMap<>(BenefitCategory.class);
        for (Ingredient ingredient : analyzed) {
            for (BenefitCategory category : BenefitCategory.values()) {
                if (category.matches(ingredient.function())) {
                    counts.merge(category, 1, Integer::sum);
                }
            }
        }

        List<Map.Entry<BenefitCategory, Integer>> topCategories = counts.entrySet().stream()
                .sorted(Map.Entry.<BenefitCategory, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(entry -> entry.getKey().ordinal()))
                .limit(2)
                .toList();
        if (topCategories.isEmpty()) {
            return new Card(
                    "성분 분석을 완료했어요",
                    "RAG 성분 데이터에서 " + matchedCount + "개 성분 정보를 확인했어요.",
                    IconType.INFO);
        }

        String title = topCategories.stream()
                .map(entry -> entry.getKey().label)
                .reduce((left, right) -> left + "·" + right)
                .orElse("") + " 케어 성분이 포함되어 있어요";
        String description = topCategories.stream()
                .map(entry -> entry.getKey().label + " 관련 성분 " + entry.getValue() + "개")
                .reduce((left, right) -> left + "와 " + right)
                .orElse("") + "가 확인됐어요.";
        return new Card(title, description, IconType.BENEFIT);
    }

    private CautionIngredient toCautionIngredient(Ingredient ingredient) {
        return new CautionIngredient(
                ingredient.input(),
                ingredient.korName(),
                ingredient.inciName(),
                ingredient.function(),
                ingredient.pregnancySafe(),
                ingredient.pregnancyNotes(),
                ingredient.source());
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private enum BenefitCategory {
        MOISTURE("수분·보습", "보습", "습윤", "수분", "humectant", "moistur", "emollient"),
        SOOTHING("진정", "진정", "soothing", "anti-inflammatory"),
        BRIGHTENING("미백", "미백", "brighten"),
        ANTI_AGING("주름·탄력", "주름", "탄력", "anti-aging"),
        ANTIOXIDANT("항산화", "항산화", "antioxidant"),
        UV_PROTECTION("자외선 차단", "자외선", "sunscreen", "uv filter"),
        CLEANSING("세정", "세정", "cleansing", "surfactant"),
        CONDITIONING("피부 컨디셔닝", "피부 컨디셔닝", "skin conditioning");

        private final String label;
        private final List<String> keywords;

        BenefitCategory(String label, String... keywords) {
            this.label = label;
            this.keywords = List.of(keywords);
        }

        private boolean matches(String function) {
            if (function == null || function.isBlank()) {
                return false;
            }
            String normalized = function.toLowerCase(Locale.ROOT);
            return keywords.stream().anyMatch(normalized::contains);
        }
    }
}
