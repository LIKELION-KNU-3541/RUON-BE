package com.springboot.ruon.domain.routine.service.llm;

import java.util.List;

public record TomorrowRoutineLlmResult(
        List<TomorrowLlmStepResult> steps,
        String explanation,
        String recommendedAction
) {
}
