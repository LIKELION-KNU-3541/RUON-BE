package com.springboot.ruon.auth.data.entity;

/** 회원가입에서 하나만 선택하는 피부 타입. */
public enum SkinType {
    DRY("건성"),
    NORMAL("중성"),
    OILY("지성"),
    COMBINATION("복합성");

    private final String label;

    SkinType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
