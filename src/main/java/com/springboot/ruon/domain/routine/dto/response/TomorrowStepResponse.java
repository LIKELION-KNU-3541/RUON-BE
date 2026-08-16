package com.springboot.ruon.domain.routine.dto.response;

import com.springboot.ruon.domain.product.entity.Product;
import com.springboot.ruon.domain.routine.entity.StepAction;
import com.springboot.ruon.domain.routine.service.llm.TomorrowLlmStepResult;

/**
 * "내일 루틴 추천" 스텝 하나. DB에 저장되는 Step이 아니라 LLM 결과를 그대로 화면용으로 변환한 것이라
 * stepId/status/timeOfDay가 없음(항상 아침 루틴처럼 취급하고 저장하지 않음).
 */
public record TomorrowStepResponse(
        Long productId,
        String productName,
        String brandName,
        String description,
        String imageUrl,
        Integer order,
        StepAction action
) {
    public static TomorrowStepResponse from(TomorrowLlmStepResult llmStep, Product product, String imageUrl) {
        return new TomorrowStepResponse(
                llmStep.productId(),
                product != null ? product.getProductName() : null,
                product != null ? product.getBrandName() : null,
                product != null ? product.getDescription() : null,
                imageUrl,
                llmStep.order(),
                StepAction.valueOf(llmStep.action())
        );
    }
}
