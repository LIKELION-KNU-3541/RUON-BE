package com.springboot.ruon.domain.routine.dto.request;

import com.springboot.ruon.auth.data.entity.RoutineTimeAvailable;
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
 * "오늘 사용 가능한 시간"(30초 퀵루틴/기본 루틴/여유 루틴)도 매일 바뀔 수 있는 값이라
 * 여기서 함께 받아서 User.routineTimeAvailable에 반영(덮어쓰기)한 뒤 루틴을 생성함.
 *
 * userId는 더 이상 여기서 받지 않음 - 로그인 토큰(@AuthenticationPrincipal)에서 꺼내 씀
 */
public record TodayConditionRequest(
        @NotEmpty List<SkinFeeling> skinFeelings,
        String customFeeling,
        @NotNull RoutineTimeAvailable routineTimeAvailable
) {
}
