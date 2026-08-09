package com.springboot.ruon.domain.routine.dto.response;

import com.springboot.ruon.domain.routine.entity.Routine;

import java.time.LocalDateTime;
import java.util.List;

public record RoutineResponse(
        Long routineId,
        Long userId,
        LocalDateTime generatedAt,
        Integer basedOnPregnancyWeek,
        String explanation,
        String recommendedAction,
        List<StepResponse> steps
) {
    public static RoutineResponse from(Routine routine) {
        return new RoutineResponse(
                routine.getRoutineId(),
                routine.getUserId(),
                routine.getGeneratedAt(),
                routine.getBasedOnPregnancyWeek(),
                routine.getExplanation(),
                routine.getRecommendedAction(),
                routine.getSteps().stream().map(StepResponse::from).toList()
        );
    }
}
