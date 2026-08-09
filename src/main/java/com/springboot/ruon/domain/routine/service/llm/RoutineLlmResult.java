package com.springboot.ruon.domain.routine.service.llm;

import java.util.List;

public record RoutineLlmResult(
        List<LlmStepResult> steps,
        String explanation,
        String recommendedAction
) {
}
