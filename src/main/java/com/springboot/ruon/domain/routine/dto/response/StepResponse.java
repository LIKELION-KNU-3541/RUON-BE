package com.springboot.ruon.domain.routine.dto.response;

import com.springboot.ruon.domain.routine.entity.Step;
import com.springboot.ruon.domain.routine.entity.StepAction;
import com.springboot.ruon.domain.routine.entity.StepStatus;
import com.springboot.ruon.domain.routine.entity.TimeOfDay;

public record StepResponse(
        Long stepId,
        Long productId,
        TimeOfDay timeOfDay,
        Integer stepOrder,
        StepAction action,
        StepStatus status
) {
    public static StepResponse from(Step step) {
        return new StepResponse(
                step.getStepId(),
                step.getProductId(),
                step.getTimeOfDay(),
                step.getStepOrder(),
                step.getAction(),
                step.getStatus()
        );
    }
}
