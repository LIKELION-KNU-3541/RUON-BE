package com.springboot.ruon.domain.scan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.ruon.domain.rag.dto.response.IngredientAnalysisResponse;
import com.springboot.ruon.domain.rag.dto.response.IngredientAnalysisResponse.Ingredient;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.Card;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.AnalysisCategory;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.CautionIngredient;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.IconType;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.OverallStatus;
import com.springboot.ruon.domain.scan.dto.response.AnalysisSummaryResponse.Summary;
import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;
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
        List<Ingredient> selectiveUse = analyzed.stream()
                .filter(ingredient -> !Boolean.FALSE.equals(ingredient.pregnancySafe()))
                .filter(this::requiresSelectiveUse)
                .toList();
        OverallStatus status = resolveStatus(cautions.size());
        Summary summary = new Summary(
                analysis.totalChecked(),
                analysis.matchedCount(),
                cautions.size(),
                unknown.size());

        return new AnalysisSummaryResponse(
                status,
                resolveCategory(cautions.size(), selectiveUse.size(), unknown.size()),
                createPrimaryCard(summary, cautions, selectiveUse.size()),
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

    private OverallStatus resolveStatus(int cautionCount) {
        return cautionCount > 0 ? OverallStatus.CAUTION : OverallStatus.NO_CAUTION_FOUND;
    }

    private Card createPrimaryCard(Summary summary, List<Ingredient> cautions, int selectiveUseCount) {
        if (!cautions.isEmpty()) {
            return new Card(
                    "잠시 보류",
                    cautionDescription(cautions, summary.cautionCount()),
                    IconType.WARNING);
        }
        if (selectiveUseCount > 0) {
            return new Card(
                    "선택 사용",
                    "알레르기·자극 또는 사용 제한을 확인할 성분 " + selectiveUseCount + "개가 있어요.",
                    IconType.WARNING);
        }
        if (summary.unknownCount() > 0) {
            return new Card(
                    "추가 확인",
                    "추가 확인이 필요한 성분 " + summary.unknownCount() + "개가 있어요.",
                    IconType.INFO);
        }
        return new Card(
                "사용 유지",
                "임신 중 주의 성분이 확인되지 않았어요.",
                IconType.CHECK);
    }

    private AnalysisCategory resolveCategory(int pauseCount, int selectiveUseCount, int needsReviewCount) {
        if (pauseCount > 0) {
            return AnalysisCategory.PAUSE;
        }
        if (selectiveUseCount > 0) {
            return AnalysisCategory.SELECTIVE_USE;
        }
        if (needsReviewCount > 0) {
            return AnalysisCategory.NEEDS_REVIEW;
        }
        return AnalysisCategory.KEEP_USING;
    }

    private boolean requiresSelectiveUse(Ingredient ingredient) {
        if (Boolean.TRUE.equals(ingredient.allergen())
                || hasText(ingredient.usageLimit())
                || hasText(ingredient.caution())) {
            return true;
        }
        if (!hasText(ingredient.irritancyPotential())) {
            return false;
        }
        String irritancy = ingredient.irritancyPotential().strip().toLowerCase(Locale.ROOT);
        return irritancy.contains("moderate")
                || irritancy.contains("medium")
                || irritancy.contains("high")
                || irritancy.contains("중간")
                || irritancy.contains("높");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String cautionDescription(List<Ingredient> cautions, int cautionCount) {
        String ingredientName = cautions.stream()
                .findFirst()
                .map(ingredient -> ingredient.korName() != null && !ingredient.korName().isBlank()
                        ? ingredient.korName()
                        : ingredient.input())
                .orElse("주의 성분");
        return ingredientName + " 등 임신 중 주의가 필요한 성분 " + cautionCount + "개가 확인됐어요.";
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
