package com.springboot.ruon.auth.data.entity;

/** 회원가입에서 복수 선택하는 피부 고민. */
public enum SkinConcern {
    DRYNESS("건조함"),
    SENSITIVITY("민감함"),
    TROUBLE("트러블"),
    ITCHING("가려움"),
    HYPERPIGMENTATION("색소침착"),
    PUFFINESS("붓기");

    private final String label;

    SkinConcern(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
