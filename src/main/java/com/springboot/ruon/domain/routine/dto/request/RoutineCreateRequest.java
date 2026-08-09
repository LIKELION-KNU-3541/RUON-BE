package com.springboot.ruon.domain.routine.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 루틴 생성 요청 (POST /api/v1/routines)
 *
 * userId는 인증(JWT) 붙기 전까지 임시로 요청 바디에 포함.
 * basedOnPregnancyWeek도 임시로 요청에서 받되, 추후 User 정보에서 자동 계산하도록 변경 예정.
 */
public record RoutineCreateRequest(
        @NotNull Long userId,
        Integer basedOnPregnancyWeek
) {
}
