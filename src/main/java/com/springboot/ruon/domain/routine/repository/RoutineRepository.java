package com.springboot.ruon.domain.routine.repository;

import com.springboot.ruon.domain.routine.entity.Routine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RoutineRepository extends JpaRepository<Routine, Long> {

    // 오늘의 루틴 조회 (같은 유저, 오늘 날짜 시간 범위 내 가장 최근 1건) - getTodayRoutine 내부에서만 사용
    Optional<Routine> findFirstByUserIdAndGeneratedAtBetweenOrderByGeneratedAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end);

    // 루틴 생성 시 "전체 루틴 읽기" 용도 - 유저의 가장 최근 루틴 1건
    Optional<Routine> findFirstByUserIdOrderByGeneratedAtDesc(Long userId);
}
