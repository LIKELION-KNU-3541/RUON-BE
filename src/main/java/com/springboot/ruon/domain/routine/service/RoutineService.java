package com.springboot.ruon.domain.routine.service;

import com.springboot.ruon.auth.data.entity.User;
import com.springboot.ruon.auth.data.repository.UserRepository;
import com.springboot.ruon.domain.product.entity.Product;
import com.springboot.ruon.domain.product.repository.ProductRepository;
import com.springboot.ruon.domain.routine.dto.request.RoutineCreateRequest;
import com.springboot.ruon.domain.routine.dto.request.SkinFeeling;
import com.springboot.ruon.domain.routine.dto.request.TodayConditionRequest;
import com.springboot.ruon.domain.routine.dto.response.RoutineResponse;
import com.springboot.ruon.domain.routine.dto.response.StepResponse;
import com.springboot.ruon.domain.routine.entity.Routine;
import com.springboot.ruon.domain.routine.entity.Step;
import com.springboot.ruon.domain.routine.entity.StepAction;
import com.springboot.ruon.domain.routine.entity.StepStatus;
import com.springboot.ruon.domain.routine.entity.TimeOfDay;
import com.springboot.ruon.domain.routine.repository.RoutineRepository;
import com.springboot.ruon.domain.routine.repository.StepRepository;
import com.springboot.ruon.domain.routine.service.llm.LlmStepResult;
import com.springboot.ruon.domain.routine.service.llm.RoutineLlmResult;
import com.springboot.ruon.domain.routine.service.llm.RoutineLlmService;
import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final StepRepository stepRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final RoutineLlmService routineLlmService;

    @Transactional
    public RoutineResponse createRoutine(RoutineCreateRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 전체루틴 읽기: 이전에 생성된 루틴이 있으면 참고 자료로 사용
        Routine previousRoutine = routineRepository
                .findFirstByUserIdOrderByGeneratedAtDesc(request.userId())
                .orElse(null);

        // 화장대에 등록된 제품 목록 (LLM이 실제 존재하는 제품으로만 루틴을 구성하도록)
        List<Product> registeredProducts = productRepository.findByUserId(request.userId());

        Integer pregnancyWeek = request.basedOnPregnancyWeek() != null
                ? request.basedOnPregnancyWeek()
                : user.getPregnancyWeekNum();

        Routine routine = Routine.create(request.userId(), pregnancyWeek);

        // TODO: 성분 중복 확인 로직 - Ingredient 도메인 준비되면 여기에 연결 예정
        RoutineLlmResult llmResult = routineLlmService.generate(user, registeredProducts, previousRoutine);
        applyLlmResultToRoutine(routine, llmResult);

        Routine saved = routineRepository.save(routine);
        return RoutineResponse.from(saved);
    }

    @Transactional
    public RoutineResponse reflectTodayCondition(TodayConditionRequest request) {
        validateCondition(request);

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 오늘 선택한 "사용 가능한 시간"을 User에 반영(덮어쓰기) - 매일 바뀔 수 있는 값이라 여기서 갱신
        user.setRoutineTimeAvailable(request.routineTimeAvailable());

        Routine previousRoutine = routineRepository
                .findFirstByUserIdOrderByGeneratedAtDesc(request.userId())
                .orElse(null);

        List<Product> registeredProducts = productRepository.findByUserId(request.userId());

        Routine routine = Routine.create(request.userId(), user.getPregnancyWeekNum());

        // 오늘의 피부 컨디션 + 방금 갱신한 User.routineTimeAvailable(오늘 사용 가능한 시간)을 반영해서 재생성
        RoutineLlmResult llmResult = routineLlmService.generateWithCondition(
                user, registeredProducts, previousRoutine, request.skinFeelings(), request.customFeeling());
        applyLlmResultToRoutine(routine, llmResult);

        Routine saved = routineRepository.save(routine);
        return RoutineResponse.from(saved);
    }

    // 컨디션 미선택 시 루틴 추천 불가: 최소 1개 선택 필수, CUSTOM(직접 작성) 선택 시 텍스트 필수
    private void validateCondition(TodayConditionRequest request) {
        if (request.skinFeelings() == null || request.skinFeelings().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (request.skinFeelings().contains(SkinFeeling.CUSTOM)
                && (request.customFeeling() == null || request.customFeeling().isBlank())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void applyLlmResultToRoutine(Routine routine, RoutineLlmResult llmResult) {
        try {
            for (LlmStepResult stepResult : llmResult.steps()) {
                Step step = Step.create(
                        stepResult.productId(),
                        TimeOfDay.valueOf(stepResult.timeOfDay()),
                        stepResult.order(),
                        StepAction.valueOf(stepResult.action())
                );
                routine.addStep(step);
            }
            routine.applyLlmResult(llmResult.explanation(), llmResult.recommendedAction());
        } catch (IllegalArgumentException e) {
            // LLM이 정의되지 않은 enum 값을 반환한 경우
            throw new CustomException(ErrorCode.LLM_GENERATION_FAILED);
        }
    }

    public RoutineResponse getTodayRoutine(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        Routine routine = routineRepository
                .findFirstByUserIdAndGeneratedAtBetweenOrderByGeneratedAtDesc(userId, start, end)
                .orElseThrow(() -> new CustomException(ErrorCode.ROUTINE_NOT_FOUND));

        return RoutineResponse.from(routine);
    }

    @Transactional
    public StepResponse updateStepStatus(Long routineId, Long stepId, StepStatus status) {
        Step step = stepRepository.findById(stepId)
                .orElseThrow(() -> new CustomException(ErrorCode.STEP_NOT_FOUND));

        if (!step.getRoutine().getRoutineId().equals(routineId)) {
            throw new CustomException(ErrorCode.STEP_NOT_FOUND);
        }

        step.changeStatus(status);
        return StepResponse.from(step);
    }
}
