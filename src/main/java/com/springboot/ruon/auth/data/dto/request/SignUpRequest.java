package com.springboot.ruon.auth.data.dto.request;

import com.springboot.ruon.auth.data.entity.PregnancyStage;
import com.springboot.ruon.auth.data.entity.RoutineTimeAvailable;
import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class SignUpRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @NotNull
    private PregnancyStage pregnancyStage;

    @Min(0)
    @Max(36)
    private int pregnancyWeekNum;

    private boolean breastfeeding;

    @NotNull
    private RoutineTimeAvailable routineTimeAvailable;
}
