package com.springboot.ruon.domain.routine.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 오늘의 컨디션 반영 요청 (POST /api/v1/routines/condition)
 *
 * 사용자가 오늘 피부 느낌을 최소 1개 이상 선택해야만 루틴을 생성할 수 있음
 * (선택 안 하면 400 INVALID_REQUEST).
 * skinFeelings에 CUSTOM(직접 작성)이 포함된 경우 customFeeling은 필수.
 *
 * "오늘 사용 가능한 시간"(30초 퀵루틴/기본 루틴/여유 루틴)은 별도로 받지 않고
 * User.routineTimeAvailable 값을 그대로 사용함.
 */
public record TodayConditionRequest(
        @NotNull Long userId,
        @NotEmpty List<SkinFeeling> skinFeelings,
        String customFeeling
) {
}
