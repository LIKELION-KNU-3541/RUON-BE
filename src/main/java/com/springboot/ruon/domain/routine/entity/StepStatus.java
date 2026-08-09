package com.springboot.ruon.domain.routine.entity;

/**
 * 루틴 단계 수행 상태
 */
public enum StepStatus {
    PENDING, // 대기중 (아직 수행 전)
    SUCCESS, // 완료
    SKIP     // 건너뜀
}
