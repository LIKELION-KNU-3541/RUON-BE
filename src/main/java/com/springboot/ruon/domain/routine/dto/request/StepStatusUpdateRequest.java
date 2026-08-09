package com.springboot.ruon.domain.routine.dto.request;

import com.springboot.ruon.domain.routine.entity.StepStatus;
import jakarta.validation.constraints.NotNull;

public record StepStatusUpdateRequest(
        @NotNull StepStatus status
) {
}
