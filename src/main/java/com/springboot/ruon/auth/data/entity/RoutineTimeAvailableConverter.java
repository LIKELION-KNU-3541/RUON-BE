package com.springboot.ruon.auth.data.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** 기존 ordinal 값(0, 1, 2)과 신규 enum 이름을 모두 읽는다. 신규 저장은 enum 이름으로 통일한다. */
@Converter
public class RoutineTimeAvailableConverter implements AttributeConverter<RoutineTimeAvailable, String> {

    @Override
    public String convertToDatabaseColumn(RoutineTimeAvailable attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public RoutineTimeAvailable convertToEntityAttribute(String value) {
        return LegacyEnumConverter.read(value, RoutineTimeAvailable.values(), "routineTimeAvailable");
    }
}
