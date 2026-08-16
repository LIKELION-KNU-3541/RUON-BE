package com.springboot.ruon.domain.routine.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.ruon.auth.data.entity.PregnancyStage;
import com.springboot.ruon.auth.data.entity.RoutineTimeAvailable;
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
            - 화장대에 제품이 1개 이상 등록되어 있다면 아침(MORNING) 루틴과 저녁(EVENING) 루틴을 각각 최소 1개 이상의 스텝으로 반드시 구성하세요.
              저녁에 특별히 더 적합한 제품이 없어 보여도, 등록된 제품 중 세안 등 기본적인 것으로 최소한의 저녁 루틴을 만드세요.
              아침 또는 저녁 중 한쪽만 만들고 나머지를 빈 채로 두면 안 됩니다.
            - 등록된 제품이 없으면 steps는 빈 배열([])로 반환하고, explanation에 그 이유를 설명하세요.
            - 성분 중복 확인 기능은 아직 지원하지 않으니 언급하지 마세요.
            """;

    private static final String DESCRIPTION_SYSTEM_PROMPT = """
            당신은 임산부/수유부를 위한 스킨케어 제품 한 줄 소개를 작성하는 시스템입니다.
            주어진 제품 정보(브랜드, 제품명, 용량)와 사용자 정보를 참고해서,
            이 제품을 스킨케어 루틴에서 사용할 때 어떤 효과/역할을 하는지 1문장으로 작성하세요.
            제품 설명(성분, 브랜드 소개)이 아니라, "이 스텝이 피부에 어떤 도움을 주는지"를 알려주는
            베네핏 중심의 짧은 한 줄이어야 합니다. (예: "세안 후 예민해진 피부에 수분을 가볍게 채워요.")

            반드시 아래 JSON 형식으로만 응답하세요. 그 외 텍스트는 절대 포함하지 마세요.
            { "description": string }
            """;

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 제품 하나에 대한 한 줄 소개를 생성한다.
     * 제품당 한 번만 호출되도록 호출부(RoutineService)에서 product.getDescription()이 null일 때만 불러야 함
     */
    public String generateProductDescription(Product product, User user) {
        String userPrompt = "[제품 정보]\n"
                + "브랜드: " + product.getBrandName() + "\n"
                + "제품명: " + product.getProductName() + "\n"
                + "용량: " + product.getCapacity() + "\n\n"
                + "[사용자 정보]\n"
                + "임신 상태: " + describePregnancyStage(user.getPregnancyStage()) + "\n"
                + "임신 주차: " + user.getPregnancyWeekNum() + "\n"
                + "수유 여부: " + (user.isBreastfeeding() ? "예" : "아니오") + "\n";

        String content = openAiClient.requestJsonCompletion(DESCRIPTION_SYSTEM_PROMPT, userPrompt);
        try {
            ProductDescriptionLlmResult result = objectMapper.readValue(content, ProductDescriptionLlmResult.class);
            return result.description();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.LLM_GENERATION_FAILED);
        }
    }

    private record ProductDescriptionLlmResult(String description) {
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
                .append(describeRoutineTimeAvailable(user.getRoutineTimeAvailable())).append('\n')
                .append("아침 루틴과 저녁 루틴 각각 최대 ").append(maxStepsPerTimeOfDay).append("개의 스텝(steps)만 포함해야 합니다 ")
                .append("(아침 최대 ").append(maxStepsPerTimeOfDay).append("개 + 저녁 최대 ").append(maxStepsPerTimeOfDay).append("개). ")
                .append("가장 중요한 케어 단계부터 우선 선택하세요.\n");

        return sb.toString();
    }

    // routineTimeAvailable(User 도메인 enum) → 아침/저녁 루틴 각각에 포함할 최대 스텝 개수
    // LOW(짧게) = 3, MEDIUM(보통) = 4, HIGH(여유) = 5
    private int resolveMaxSteps(RoutineTimeAvailable routineTimeAvailable) {
        if (routineTimeAvailable == null) {
            return 4;
        }
        return switch (routineTimeAvailable) {
            case LOW -> 3;
            case MEDIUM -> 4;
            case HIGH -> 5;
        };
    }

    // User 도메인 enum들을 LLM 프롬프트용 한국어 설명으로 변환
    private String describeRoutineTimeAvailable(RoutineTimeAvailable routineTimeAvailable) {
        if (routineTimeAvailable == null) {
            return "알 수 없음";
        }
        return switch (routineTimeAvailable) {
            case LOW -> "30초 퀵루틴 (시간 짧음)";
            case MEDIUM -> "기본 루틴 (보통)";
            case HIGH -> "여유 루틴 (시간 넉넉함)";
        };
    }

    private String describePregnancyStage(PregnancyStage pregnancyStage) {
        if (pregnancyStage == null) {
            return "알 수 없음";
        }
        return switch (pregnancyStage) {
            case PRE_PREGNANCY -> "임신 전";
            case PREGNANT -> "임신 중";
            case POSTPARTUM -> "출산 후";
        };
    }

    private static final String TOMORROW_SYSTEM_PROMPT = """
            당신은 임산부/수유부를 위한 스킨케어 루틴 추천 시스템입니다.
            사용자가 오늘 루틴에 대해 남긴 반응 점수(1~5)를 참고해서 내일 아침에 사용할 루틴을 구성하세요.
            반드시 사용자의 화장대에 이미 등록된 제품만 사용해야 하며, 화장대에 없는 새로운 제품을 추천하면 안 됩니다.

            반드시 아래 JSON 형식으로만 응답하세요. 그 외 텍스트는 절대 포함하지 마세요.
            {
              "steps": [
                { "productId": number, "order": number, "action": "CLEANSE" | "TONER" | "ESSENCE" | "SERUM" | "MOISTURIZER" | "SUNSCREEN" }
              ],
              "explanation": string,
              "recommendedAction": string
            }

            규칙:
            - steps의 productId는 반드시 사용자가 화장대에 등록한 제품 목록에 있는 productId 중 하나여야 합니다.
            - 이 루틴은 항상 아침 루틴처럼 구성합니다(timeOfDay는 응답에 포함하지 않습니다).
            - 등록된 제품이 없으면 steps는 빈 배열([])로 반환하고, explanation에 그 이유를 설명하세요.
            """;

    /**
     * "내일 루틴 추천" (GET /api/v1/routines/tomorrow)
     * 반응 기록이 있는 가장 최근 루틴(lastReactedRoutine)의 reactionScore(1~5)에 따라 지시를 다르게 주되,
     * steps + explanation/recommendedAction은 항상 같은 구조로 한 번만 호출한다 (점수별 분기 호출 없음).
     * 결과는 저장하지 않고 호출할 때마다 즉석으로 계산한다.
     */
    public TomorrowRoutineLlmResult generateTomorrowRecommendation(
            User user, List<Product> registeredProducts, Routine lastReactedRoutine) {
        String userPrompt = buildTomorrowUserPrompt(user, registeredProducts, lastReactedRoutine);
        String content = openAiClient.requestJsonCompletion(TOMORROW_SYSTEM_PROMPT, userPrompt);

        try {
            return objectMapper.readValue(content, TomorrowRoutineLlmResult.class);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.LLM_GENERATION_FAILED);
        }
    }

    private String buildTomorrowUserPrompt(User user, List<Product> products, Routine lastReactedRoutine) {
        StringBuilder sb = new StringBuilder(buildUserPrompt(user, products, lastReactedRoutine));

        int score = lastReactedRoutine.getReactionScore();
        sb.append("[오늘 루틴에 대한 반응 점수]\n").append(score).append("점 (1~5점 중)\n\n");

        sb.append("[지시사항]\n");
        if (score >= 4) {
            sb.append("반응이 좋았습니다. 위 [이전 루틴]과 완전히 동일한 productId, order, action으로 steps를 구성하세요. ")
                    .append("explanation과 recommendedAction은 이 루틴을 그대로 이어가도 좋다는 격려 톤의 짧은 문구로 작성하세요.\n");
        } else if (score == 3) {
            sb.append("반응이 평소와 같았습니다. 위 [이전 루틴]과 완전히 동일한 productId, order, action으로 steps를 구성하세요. ")
                    .append("explanation과 recommendedAction은 큰 변화가 필요 없다는 톤의 짧은 문구로 작성하세요.\n");
        } else {
            sb.append("반응이 좋지 않았습니다(당김/건조 또는 따가움/붉어짐). 화장대에 등록된 제품 중에서 1~2개 정도만 조정(교체 또는 스텝 순서 변경)해서 steps를 구성하세요. ")
                    .append("explanation과 recommendedAction에는 무엇을 왜 조정했는지 설명하세요. 화장대에 없는 제품은 절대 추천하지 마세요.\n");
        }

        return sb.toString();
    }

    private String buildUserPrompt(User user, List<Product> products, Routine previousRoutine) {
        StringBuilder sb = new StringBuilder();

        sb.append("[사용자 정보]\n")
                .append("임신 상태: ").append(describePregnancyStage(user.getPregnancyStage())).append('\n')
                .append("임신 주차: ").append(user.getPregnancyWeekNum()).append('\n')
                .append("수유 여부: ").append(user.isBreastfeeding() ? "예" : "아니오").append('\n')
                .append("피부 타입: ")
                .append(user.getSkinType() != null ? user.getSkinType().getLabel() : "알 수 없음")
                .append('\n')
                .append("피부 고민: ")
                .append(user.getSkinConcerns() == null || user.getSkinConcerns().isEmpty()
                        ? "없음"
                        : user.getSkinConcerns().stream()
                                .map(concern -> concern.getLabel())
                                .collect(java.util.stream.Collectors.joining(", ")))
                .append('\n')
                .append("루틴 가능 시간대: ").append(describeRoutineTimeAvailable(user.getRoutineTimeAvailable())).append("\n\n");

        sb.append("[화장대에 등록된 제품]\n");
        if (products.isEmpty()) {
            sb.append("등록된 제품 없음\n\n");
        } else {
            for (Product product : products) {
                sb.append("- productId: ").append(product.getProductId())
                        .append(", 이름: ").append(product.getProductName())
                        .append(", 브랜드: ").append(product.getBrandName())
                        .append(", 용량: ").append(product.getCapacity())
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
