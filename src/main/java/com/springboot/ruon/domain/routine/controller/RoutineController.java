package com.springboot.ruon.domain.routine.controller;

import com.springboot.ruon.domain.routine.dto.request.RoutineCreateRequest;
import com.springboot.ruon.domain.routine.dto.request.StepStatusUpdateRequest;
import com.springboot.ruon.domain.routine.dto.request.TodayConditionRequest;
import com.springboot.ruon.domain.routine.dto.response.RoutineResponse;
import com.springboot.ruon.domain.routine.dto.response.StepResponse;
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

    // 루틴 생성
    @PostMapping
    public ResponseEntity<ApiResponse<RoutineResponse>> createRoutine(
            @Valid @RequestBody RoutineCreateRequest request) {
        RoutineResponse response = routineService.createRoutine(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

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
}
