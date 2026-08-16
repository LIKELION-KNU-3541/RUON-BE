package com.springboot.ruon.domain.routine.service;

import com.springboot.ruon.auth.data.entity.User;
import com.springboot.ruon.auth.data.repository.UserRepository;
import com.springboot.ruon.domain.product.entity.Product;
import com.springboot.ruon.domain.product.repository.ProductRepository;
import com.springboot.ruon.domain.routine.dto.request.SkinFeeling;
import com.springboot.ruon.domain.routine.dto.request.TodayConditionRequest;
import com.springboot.ruon.domain.routine.dto.response.RoutineResponse;
import com.springboot.ruon.domain.routine.dto.response.StepResponse;
import com.springboot.ruon.domain.routine.dto.response.TomorrowRoutineResponse;
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
import com.springboot.ruon.domain.routine.service.llm.TomorrowRoutineLlmResult;
import com.springboot.ruon.domain.scan.entity.ScanJob;
import com.springboot.ruon.domain.scan.repository.ScanJobRepository;
import com.springboot.ruon.domain.scan.service.ScanImageUrlService;
import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final StepRepository stepRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final RoutineLlmService routineLlmService;
    private final ScanJobRepository scanJobRepository;
    private final ScanImageUrlService scanImageUrlService;

    @Transactional
    public RoutineResponse reflectTodayCondition(Long userId, TodayConditionRequest request) {
        validateCondition(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 오늘 선택한 "사용 가능한 시간"을 User에 반영(덮어쓰기) - 매일 바뀔 수 있는 값이라 여기서 갱신
        user.setRoutineTimeAvailable(request.routineTimeAvailable());

        Routine previousRoutine = routineRepository
                .findFirstByUserIdOrderByGeneratedAtDesc(userId)
                .orElse(null);

        List<Product> registeredProducts = productRepository.findByUserId(userId);

        // 오늘의 컨디션(피부 느낌)과 방금 반영한 사용 가능한 시간을 이 루틴에 스냅샷으로 저장
        Routine routine = Routine.create(
                userId, user.getPregnancyWeekNum(),
                request.skinFeelings(), request.customFeeling(), request.routineTimeAvailable());

        // 오늘의 피부 컨디션 + 방금 갱신한 User.routineTimeAvailable(오늘 사용 가능한 시간)을 반영해서 재생성
        RoutineLlmResult llmResult = routineLlmService.generateWithCondition(
                user, registeredProducts, previousRoutine, request.skinFeelings(), request.customFeeling());
        Map<Long, Product> productsById = toProductMap(registeredProducts);
        applyLlmResultToRoutine(routine, llmResult, user, productsById);

        Routine saved = routineRepository.save(routine);
        return RoutineResponse.from(saved, productsById, resolveImageUrls(productsById.values()));
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

    private void applyLlmResultToRoutine(Routine routine, RoutineLlmResult llmResult, User user, Map<Long, Product> productsById) {
        try {
            for (LlmStepResult stepResult : llmResult.steps()) {
                Step step = Step.create(
                        stepResult.productId(),
                        TimeOfDay.valueOf(stepResult.timeOfDay()),
                        stepResult.order(),
                        StepAction.valueOf(stepResult.action())
                );
                routine.addStep(step);

                // 이 제품의 한 줄 소개가 아직 없으면 지금 한 번만 생성해서 저장 (이후로는 재사용, 재호출 없음)
                Product product = productsById.get(stepResult.productId());
                if (product != null && product.getDescription() == null) {
                    String description = routineLlmService.generateProductDescription(product, user);
                    product.applyDescription(description);
                }
            }
            routine.applyLlmResult(llmResult.explanation(), llmResult.recommendedAction());
        } catch (IllegalArgumentException e) {
            // LLM이 정의되지 않은 enum 값을 반환한 경우
            throw new CustomException(ErrorCode.LLM_GENERATION_FAILED);
        }
    }

    private Map<Long, Product> toProductMap(List<Product> products) {
        return products.stream().collect(Collectors.toMap(Product::getProductId, Function.identity()));
    }

    // 화장품 이미지는 scanId로 ScanJob을 찾아서 그때그때 새로 발급한다 (presigned URL이라 저장해두지 않음).
    // 스텝마다 따로 조회하면 N+1이 나므로, 넘어온 제품들의 scanId를 한 번에 모아서 조회한다.
    private Map<Long, String> resolveImageUrls(Collection<Product> products) {
        List<Long> scanIds = products.stream()
                .map(Product::getScanId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ScanJob> scansById = scanIds.isEmpty()
                ? Map.of()
                : scanJobRepository.findAllById(scanIds).stream()
                        .collect(Collectors.toMap(ScanJob::getScanId, Function.identity()));

        Map<Long, String> imageUrlsByProductId = new HashMap<>();
        for (Product product : products) {
            ScanJob scanJob = product.getScanId() != null ? scansById.get(product.getScanId()) : null;
            imageUrlsByProductId.put(product.getProductId(), scanImageUrlService.resolveViewUrl(scanJob));
        }
        return imageUrlsByProductId;
    }

    public RoutineResponse getTodayRoutine(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        Routine routine = routineRepository
                .findFirstByUserIdAndGeneratedAtBetweenOrderByGeneratedAtDesc(userId, start, end)
                .orElseThrow(() -> new CustomException(ErrorCode.ROUTINE_NOT_FOUND));

        List<Long> productIds = routine.getSteps().stream().map(Step::getProductId).distinct().toList();
        Map<Long, Product> productsById = toProductMap(productRepository.findAllById(productIds));

        return RoutineResponse.from(routine, productsById, resolveImageUrls(productsById.values()));
    }

    // 내일 루틴 추천 - 저장하지 않고 호출할 때마다 즉석 계산. 반응 기록이 있는 가장 최근 루틴 기준.
    public TomorrowRoutineResponse getTomorrowRoutine(Long userId) {
        Routine lastReactedRoutine = routineRepository
                .findFirstByUserIdAndReactionScoreIsNotNullOrderByGeneratedAtDesc(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROUTINE_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Product> registeredProducts = productRepository.findByUserId(userId);
        Map<Long, Product> productsById = toProductMap(registeredProducts);

        try {
            TomorrowRoutineLlmResult llmResult =
                    routineLlmService.generateTomorrowRecommendation(user, registeredProducts, lastReactedRoutine);
            return TomorrowRoutineResponse.of(
                    llmResult, lastReactedRoutine.getReactionScore(), productsById, resolveImageUrls(productsById.values()));
        } catch (IllegalArgumentException e) {
            // LLM이 정의되지 않은 action enum 값을 반환한 경우 (TomorrowStepResponse.from의 StepAction.valueOf 포함)
            throw new CustomException(ErrorCode.LLM_GENERATION_FAILED);
        }
    }

    // 반응 기록(재제출 가능/upsert). 점수 자체의 1~5 범위 검증은 RoutineReactionRequest에서 이미 끝남.
    @Transactional
    public RoutineResponse recordReaction(Long userId, Long routineId, Integer score) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROUTINE_NOT_FOUND));
        requireOwner(routine, userId);

        routine.applyReaction(score);

        List<Long> productIds = routine.getSteps().stream().map(Step::getProductId).distinct().toList();
        Map<Long, Product> productsById = toProductMap(productRepository.findAllById(productIds));

        return RoutineResponse.from(routine, productsById, resolveImageUrls(productsById.values()));
    }

    @Transactional
    public StepResponse updateStepStatus(Long userId, Long routineId, Long stepId, StepStatus status) {
        Step step = stepRepository.findById(stepId)
                .orElseThrow(() -> new CustomException(ErrorCode.STEP_NOT_FOUND));

        if (!step.getRoutine().getRoutineId().equals(routineId)) {
            throw new CustomException(ErrorCode.STEP_NOT_FOUND);
        }
        requireOwner(step.getRoutine(), userId);

        step.changeStatus(status);
        Product product = productRepository.findById(step.getProductId()).orElse(null);

        ScanJob scanJob = product != null && product.getScanId() != null
                ? scanJobRepository.findById(product.getScanId()).orElse(null)
                : null;
        String imageUrl = scanImageUrlService.resolveViewUrl(scanJob);

        return StepResponse.from(step, product, imageUrl);
    }

    // routineId/stepId는 순차 생성되는 값이라 남의 것도 추측 가능 - 소유자 검증 필수(issue #61)
    private void requireOwner(Routine routine, Long userId) {
        if (!routine.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "본인의 루틴만 접근할 수 있습니다.", null);
        }
    }
}
