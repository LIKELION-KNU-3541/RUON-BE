package com.springboot.ruon.auth.data.dto.request;

import com.springboot.ruon.auth.data.entity.PregnancyStage;
import com.springboot.ruon.auth.data.entity.SkinConcern;
import com.springboot.ruon.auth.data.entity.SkinType;
import jakarta.validation.constraints.*;
import java.util.Set;
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
    private String name;

    @NotNull
    private PregnancyStage pregnancyStage;

    @Min(0)
    @Max(40)
    private int pregnancyWeekNum;

    private boolean breastfeeding;

    @Size(max = 6, message = "피부 고민은 최대 6개까지 선택할 수 있습니다.")
    private Set<SkinConcern> skinConcerns = Set.of();

    @NotNull(message = "피부 타입을 선택해주세요.")
    private SkinType skinType;
}
