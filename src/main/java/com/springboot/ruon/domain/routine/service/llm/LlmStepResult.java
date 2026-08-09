package com.springboot.ruon.domain.routine.service.llm;

/**
 * OpenAI 응답 JSON의 steps 배열 항목 하나를 매핑하는 DTO.
 * timeOfDay/action은 문자열로 받아서 서비스단에서 enum으로 변환.
 */
public record LlmStepResult(
        Long productId,
        String timeOfDay,
        Integer order,
        String action
) {
}
