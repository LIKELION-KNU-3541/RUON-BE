package com.springboot.ruon.domain.routine.dto.response;

import com.springboot.ruon.domain.product.entity.Product;
import com.springboot.ruon.domain.routine.service.llm.TomorrowRoutineLlmResult;

import java.util.List;
import java.util.Map;

/**
 * "내일 루틴 추천" 응답. 화면 열 때마다 즉석 계산되며 DB에 저장되지 않음.
 * basedOnReactionScore: 이번 추천의 근거가 된 반응 점수(1~5) - 프론트에서 "이어가요"/"조정했어요" 문구 톤 판단용으로 참고 가능.
 */
public record TomorrowRoutineResponse(
        Integer basedOnReactionScore,
        String explanation,
        String recommendedAction,
        List<TomorrowStepResponse> steps
) {
    public static TomorrowRoutineResponse of(
            TomorrowRoutineLlmResult llmResult, Integer basedOnReactionScore,
            Map<Long, Product> productsById, Map<Long, String> imageUrlsByProductId) {
        return new TomorrowRoutineResponse(
                basedOnReactionScore,
                llmResult.explanation(),
                llmResult.recommendedAction(),
                llmResult.steps().stream()
                        .map(step -> TomorrowStepResponse.from(
                                step,
                                productsById.get(step.productId()),
                                imageUrlsByProductId.get(step.productId())))
                        .toList()
        );
    }
}
