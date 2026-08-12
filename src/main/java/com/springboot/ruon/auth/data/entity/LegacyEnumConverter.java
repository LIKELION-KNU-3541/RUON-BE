package com.springboot.ruon.auth.data.entity;

final class LegacyEnumConverter {

    private LegacyEnumConverter() {
    }

    static <E extends Enum<E>> E read(String rawValue, E[] values, String fieldName) {
        if (rawValue == null) {
            return null;
        }

        String value = rawValue.strip();
        if (value.isEmpty()) {
            return null;
        }

        try {
            int ordinal = Integer.parseInt(value);
            if (ordinal >= 0 && ordinal < values.length) {
                return values[ordinal];
            }
        } catch (NumberFormatException ignored) {
            //문자열 enum 이름은 아래에서 처리한다.
        }

        for (E candidate : values) {
            if (candidate.name().equalsIgnoreCase(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException(fieldName + " 값이 올바르지 않습니다: " + value);
    }
}
