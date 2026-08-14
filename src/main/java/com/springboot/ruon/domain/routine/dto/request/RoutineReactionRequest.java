package com.springboot.ruon.domain.routine.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 루틴 반응 기록 (POST /api/v1/routines/{routineId}/reaction)
 *
 * 프론트에서 이모지 5개 중 하나를 고르면 아래 점수로 변환해서 보냄(왼쪽부터):
 * 당기고 건조했어요=1, 따갑거나 붉어졌어요=2, 평소와 같아요=3, 편안했어요=4, 촉촉해졌어요=5.
 * 재제출 가능(upsert) — 이미 반응이 있어도 새 값으로 덮어씀.
 */
public record RoutineReactionRequest(
        @NotNull @Min(1) @Max(5) Integer score
) {
}
