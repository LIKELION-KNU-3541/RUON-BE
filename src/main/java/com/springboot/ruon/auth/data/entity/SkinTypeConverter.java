package com.springboot.ruon.auth.data.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SkinTypeConverter implements AttributeConverter<SkinType, String> {

    @Override
    public String convertToDatabaseColumn(SkinType skinType) {
        return skinType == null ? null : skinType.name();
    }

    @Override
    public SkinType convertToEntityAttribute(String value) {
        return LegacyEnumConverter.read(value, SkinType.values(), "skinType");
    }
}
