package com.springboot.ruon.domain.routine.service.llm;

/**
 * "내일 루틴 추천"용 steps 배열 항목. 항상 아침 루틴처럼 구성되므로 timeOfDay는 따로 받지 않음
 * (팀 결정: 이 기능에서는 아침/저녁 구분을 신경 쓰지 않고 결과물이 아침 루틴처럼 보이면 됨).
 */
public record TomorrowLlmStepResult(
        Long productId,
        Integer order,
        String action
) {
}
