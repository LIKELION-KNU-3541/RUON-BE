package com.springboot.ruon.auth.data.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** 피부 고민 Enum 집합을 쉼표로 연결해 한 컬럼에 저장한다. */
@Converter
public class SkinConcernSetConverter implements AttributeConverter<Set<SkinConcern>, String> {

    @Override
    public String convertToDatabaseColumn(Set<SkinConcern> concerns) {
        if (concerns == null || concerns.isEmpty()) {
            return "";
        }
        return concerns.stream()
                .sorted()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<SkinConcern> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<SkinConcern> concerns = Arrays.stream(value.split(","))
                .map(String::strip)
                .filter(item -> !item.isEmpty())
                .map(SkinConcern::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(concerns);
    }
}
