package com.springboot.ruon.domain.routine.dto.response;

import com.springboot.ruon.domain.product.entity.Product;
import com.springboot.ruon.domain.routine.entity.Step;
import com.springboot.ruon.domain.routine.entity.StepAction;
import com.springboot.ruon.domain.routine.entity.StepStatus;
import com.springboot.ruon.domain.routine.entity.TimeOfDay;

public record StepResponse(
        Long stepId,
        Long productId,
        String productName,
        String brandName,
        String description,
        TimeOfDay timeOfDay,
        Integer stepOrder,
        StepAction action,
        StepStatus status
) {
    public static StepResponse from(Step step, Product product) {
        return new StepResponse(
                step.getStepId(),
                step.getProductId(),
                product != null ? product.getProductName() : null,
                product != null ? product.getBrandName() : null,
                product != null ? product.getDescription() : null,
                step.getTimeOfDay(),
                step.getStepOrder(),
                step.getAction(),
                step.getStatus()
        );
    }
}
