package com.springboot.ruon.domain.routine.dto.response;

import com.springboot.ruon.domain.product.entity.Product;
import com.springboot.ruon.domain.routine.entity.Routine;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record RoutineResponse(
        Long routineId,
        Long userId,
        LocalDateTime generatedAt,
        Integer basedOnPregnancyWeek,
        String explanation,
        String recommendedAction,
        List<StepResponse> steps
) {
    // productsById: 이 루틴의 스텝들이 참조하는 productId -> Product. 없으면 해당 스텝의 productName/brandName/description은 null로 나감.
    public static RoutineResponse from(Routine routine, Map<Long, Product> productsById) {
        return new RoutineResponse(
                routine.getRoutineId(),
                routine.getUserId(),
                routine.getGeneratedAt(),
                routine.getBasedOnPregnancyWeek(),
                routine.getExplanation(),
                routine.getRecommendedAction(),
                routine.getSteps().stream()
                        .map(step -> StepResponse.from(step, productsById.get(step.getProductId())))
                        .toList()
        );
    }
}
