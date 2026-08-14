package com.springboot.ruon.domain.routine.controller;

import com.springboot.ruon.domain.routine.dto.request.RoutineReactionRequest;
import com.springboot.ruon.domain.routine.dto.request.StepStatusUpdateRequest;
import com.springboot.ruon.domain.routine.dto.request.TodayConditionRequest;
import com.springboot.ruon.domain.routine.dto.response.RoutineResponse;
import com.springboot.ruon.domain.routine.dto.response.StepResponse;
import com.springboot.ruon.domain.routine.dto.response.TomorrowRoutineResponse;
import com.springboot.ruon.domain.routine.service.RoutineService;
import com.springboot.ruon.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/routines")
@RequiredArgsConstructor
public class RoutineController {

    private final RoutineService routineService;

    // 오늘의 컨디션 반영 → 오늘 사용 가능한 시간(User.routineTimeAvailable)에 맞춰 루틴 재생성
    @PostMapping("/condition")
    public ResponseEntity<ApiResponse<RoutineResponse>> reflectTodayCondition(
            @Valid @RequestBody TodayConditionRequest request) {
        RoutineResponse response = routineService.reflectTodayCondition(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // 오늘의 루틴 반환
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<RoutineResponse>> getTodayRoutine(@RequestParam Long userId) {
        RoutineResponse response = routineService.getTodayRoutine(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // step 상태 변경
    @PatchMapping("/{routineId}/steps/{stepId}")
    public ResponseEntity<ApiResponse<StepResponse>> updateStepStatus(
            @PathVariable Long routineId,
            @PathVariable Long stepId,
            @Valid @RequestBody StepStatusUpdateRequest request) {
        StepResponse response = routineService.updateStepStatus(routineId, stepId, request.status());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 루틴 반응 기록 (재제출 가능 - upsert)
    @PostMapping("/{routineId}/reaction")
    public ResponseEntity<ApiResponse<RoutineResponse>> recordReaction(
            @PathVariable Long routineId,
            @Valid @RequestBody RoutineReactionRequest request) {
        RoutineResponse response = routineService.recordReaction(routineId, request.score());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 내일 루틴 추천 - 저장하지 않고 호출할 때마다 즉석 계산. 반응 기록이 있는 가장 최근 루틴 기준.
    @GetMapping("/tomorrow")
    public ResponseEntity<ApiResponse<TomorrowRoutineResponse>> getTomorrowRoutine(@RequestParam Long userId) {
        TomorrowRoutineResponse response = routineService.getTomorrowRoutine(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
