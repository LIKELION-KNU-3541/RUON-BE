package com.springboot.ruon.auth.data.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LegacyEnumConverterTest {

    private final PregnancyStageConverter pregnancyStageConverter = new PregnancyStageConverter();
    private final RoutineTimeAvailableConverter routineTimeConverter = new RoutineTimeAvailableConverter();

    @Test
    void 기존_ordinal_임신단계를_읽는다() {
        assertThat(pregnancyStageConverter.convertToEntityAttribute("0"))
                .isEqualTo(PregnancyStage.PRE_PREGNANCY);
        assertThat(pregnancyStageConverter.convertToEntityAttribute("1"))
                .isEqualTo(PregnancyStage.PREGNANT);
        assertThat(pregnancyStageConverter.convertToEntityAttribute("2"))
                .isEqualTo(PregnancyStage.POSTPARTUM);
    }

    @Test
    void 신규_enum_이름을_읽고_이름으로_저장한다() {
        assertThat(pregnancyStageConverter.convertToEntityAttribute("PREGNANT"))
                .isEqualTo(PregnancyStage.PREGNANT);
        assertThat(pregnancyStageConverter.convertToDatabaseColumn(PregnancyStage.PREGNANT))
                .isEqualTo("PREGNANT");
    }

    @Test
    void 기존_routineTimeAvailable_ordinal도_읽는다() {
        assertThat(routineTimeConverter.convertToEntityAttribute("0"))
                .isEqualTo(RoutineTimeAvailable.LOW);
        assertThat(routineTimeConverter.convertToEntityAttribute("1"))
                .isEqualTo(RoutineTimeAvailable.MEDIUM);
        assertThat(routineTimeConverter.convertToEntityAttribute("2"))
                .isEqualTo(RoutineTimeAvailable.HIGH);
    }

    @Test
    void 정의되지_않은_값은_거부한다() {
        assertThatThrownBy(() -> pregnancyStageConverter.convertToEntityAttribute("3"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
