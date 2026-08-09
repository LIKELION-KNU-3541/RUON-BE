package com.springboot.ruon.domain.routine.dto.request;

/**
 * "오늘의 컨디션" 화면에서 선택하는 오늘의 피부 느낌 (10가지 카테고리)
 * CUSTOM(직접 작성)을 선택한 경우, TodayConditionRequest.customFeeling에
 * 실제 텍스트가 함께 와야 함.
 */
public enum SkinFeeling {
    DRY("건조해요"),
    SENSITIVE("민감해요"),
    ITCHY("가려워요"),
    TROUBLED("트러블이 있어요"),
    TIGHT("당겨요"),
    RED("붉어졌어요"),
    HOT("열감이 있어요"),
    FLAKY("각질이 올라와요"),
    NORMAL("평소와 같아요"),
    CUSTOM("직접 작성");

    private final String description;

    SkinFeeling(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
