package com.springboot.ruon.domain.routine.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.ruon.auth.data.entity.User;
import com.springboot.ruon.domain.product.entity.Product;
import com.springboot.ruon.domain.routine.dto.request.SkinFeeling;
import com.springboot.ruon.domain.routine.entity.Routine;
import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;
import com.springboot.ruon.global.llm.OpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 루틴 생성 흐름: 전체루틴 읽기 → (중복 성분 확인: TODO, Ingredient 준비되면 연결)
 * → 피부 상태/생활 패턴 확인 → 설명 → 행동추천
 *
 * 위 흐름 중 "중복 성분 확인"은 Ingredient 도메인이 아직 없어서
 * 지금은 프롬프트에서도 빼고 스킵함. Ingredient 준비되면 프롬프트에 추가 예정.
 */
@Service
@RequiredArgsConstructor
public class RoutineLlmService {

    private static final String SYSTEM_PROMPT = """
            당신은 임산부/수유부를 위한 스킨케어 루틴 추천 시스템입니다.
            사용자의 프로필(임신 상태, 임신 주차, 수유 여부, 루틴 가능 시간대)과
            화장대에 등록된 제품, 이전에 생성된 루틴을 참고해서 아침/저녁 스킨케어 루틴을 구성하고,
            루틴에 대한 설명과 사용자에게 줄 행동 추천을 작성하세요.

            반드시 아래 JSON 형식으로만 응답하세요. 그 외 텍스트는 절대 포함하지 마세요.
            {
              "steps": [
                { "productId": number, "timeOfDay": "MORNING" | "EVENING", "order": number, "action": "CLEANSE" | "TONER" | "ESSENCE" | "SERUM" | "MOISTURIZER" | "SUNSCREEN" }
              ],
              "explanation": string,
              "recommendedAction": string
            }

            규칙:
            - steps의 productId는 반드시 사용자가 화장대에 등록한 제품 목록에 있는 productId 중 하나여야 합니다.
            - 등록된 제품이 없으면 steps는 빈 배열([])로 반환하고, explanation에 그 이유를 설명하세요.
            - 성분 중복 확인 기능은 아직 지원하지 않으니 언급하지 마세요.
            """;

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RoutineLlmResult generate(User user, List<Product> registeredProducts, Routine previousRoutine) {
        String userPrompt = buildUserPrompt(user, registeredProducts, previousRoutine);
        String content = openAiClient.requestJsonCompletion(SYSTEM_PROMPT, userPrompt);

        try {
            return objectMapper.readValue(content, RoutineLlmResult.class);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.LLM_GENERATION_FAILED);
        }
    }

    /**
     * "오늘의 컨디션" 반영 (POST /api/v1/routines/condition)
     * 오늘의 피부 느낌(skinFeelings)과 User.routineTimeAvailable(오늘 사용 가능한 시간)을
     * 프롬프트에 추가로 반영해서 루틴을 재생성함.
     */
    public RoutineLlmResult generateWithCondition(User user, List<Product> registeredProducts, Routine previousRoutine,
                                                   List<SkinFeeling> skinFeelings, String customFeeling) {
        int maxStepsPerTimeOfDay = resolveMaxSteps(user.getRoutineTimeAvailable());
        String userPrompt = buildConditionUserPrompt(
                user, registeredProducts, previousRoutine, skinFeelings, customFeeling, maxStepsPerTimeOfDay);
        String content = openAiClient.requestJsonCompletion(SYSTEM_PROMPT, userPrompt);

        try {
            return objectMapper.readValue(content, RoutineLlmResult.class);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.LLM_GENERATION_FAILED);
        }
    }

    private String buildConditionUserPrompt(User user, List<Product> products, Routine previousRoutine,
                                              List<SkinFeeling> skinFeelings, String customFeeling, int maxStepsPerTimeOfDay) {
        StringBuilder sb = new StringBuilder(buildUserPrompt(user, products, previousRoutine));

        sb.append("[오늘의 피부 컨디션]\n");
        for (SkinFeeling feeling : skinFeelings) {
            if (feeling == SkinFeeling.CUSTOM) {
                sb.append("- 직접 작성: ").append(customFeeling).append('\n');
            } else {
                sb.append("- ").append(feeling.getDescription()).append('\n');
            }
        }
        sb.append('\n');

        sb.append("[오늘 사용 가능한 시간]\n")
                .append(user.getRoutineTimeAvailable()).append('\n')
                .append("아침 루틴과 저녁 루틴 각각 최대 ").append(maxStepsPerTimeOfDay).append("개의 스텝(steps)만 포함해야 합니다 ")
                .append("(아침 최대 ").append(maxStepsPerTimeOfDay).append("개 + 저녁 최대 ").append(maxStepsPerTimeOfDay).append("개). ")
                .append("가장 중요한 케어 단계부터 우선 선택하세요.\n");

        return sb.toString();
    }

    // routineTimeAvailable 문자열 → 아침/저녁 루틴 각각에 포함할 최대 스텝 개수
    // 주의: "30초 퀵루틴"/"기본 루틴"/"여유 루틴" 문자열이 User 저장 값과 정확히 일치해야 함.
    // 값이 다르게 저장되고 있다면(예: 담당자가 다른 문구/enum 사용) 이 매핑을 맞춰서 수정 필요.
    private int resolveMaxSteps(String routineTimeAvailable) {
        if (routineTimeAvailable == null) {
            return 4;
        }
        return switch (routineTimeAvailable) {
            case "30초 퀵루틴" -> 3;
            case "기본 루틴" -> 4;
            case "여유 루틴" -> 5;
            default -> 4;
        };
    }

    private String buildUserPrompt(User user, List<Product> products, Routine previousRoutine) {
        StringBuilder sb = new StringBuilder();

        sb.append("[사용자 정보]\n")
                .append("임신 상태: ").append(user.getPregnancyStage()).append('\n')
                .append("임신 주차: ").append(user.getPregnancyWeekNum()).append('\n')
                .append("수유 여부: ").append(user.isBreastfeeding() ? "예" : "아니오").append('\n')
                .append("루틴 가능 시간대: ").append(user.getRoutineTimeAvailable()).append("\n\n");

        sb.append("[화장대에 등록된 제품]\n");
        if (products.isEmpty()) {
            sb.append("등록된 제품 없음\n\n");
        } else {
            for (Product product : products) {
                sb.append("- productId: ").append(product.getProductId())
                        .append(", 이름: ").append(product.getProductName())
                        .append(", 카테고리: ").append(product.getCategory())
                        .append('\n');
            }
            sb.append('\n');
        }

        sb.append("[이전 루틴]\n");
        if (previousRoutine == null) {
            sb.append("없음 (처음 생성하는 루틴)\n");
        } else {
            sb.append("생성일: ").append(previousRoutine.getGeneratedAt()).append('\n');
            previousRoutine.getSteps().forEach(step ->
                    sb.append("- ").append(step.getTimeOfDay())
                            .append(" / ").append(step.getAction())
                            .append(" (productId: ").append(step.getProductId()).append(")\n"));
        }

        return sb.toString();
    }
}
