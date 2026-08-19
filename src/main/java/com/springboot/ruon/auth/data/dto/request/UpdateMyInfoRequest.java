package com.springboot.ruon.auth.data.dto.request;

import com.springboot.ruon.auth.data.entity.PregnancyStage;
import com.springboot.ruon.auth.data.entity.SkinConcern;
import com.springboot.ruon.auth.data.entity.SkinType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Getter;

/** 전달된 필드만 변경하는 내 정보 수정 요청. */
@Getter
public class UpdateMyInfoRequest {

    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    @Pattern(regexp = ".*\\S.*", message = "이름은 공백일 수 없습니다.")
    private String name;

    private PregnancyStage pregnancyStage;

    @Min(value = 0, message = "임신 주차는 0 이상이어야 합니다.")
    @Max(value = 40, message = "임신 주차는 40 이하여야 합니다.")
    private Integer pregnancyWeekNum;

    private Boolean breastfeeding;

    @Size(max = 6, message = "피부 고민은 최대 6개까지 선택할 수 있습니다.")
    private Set<SkinConcern> skinConcerns;

    private SkinType skinType;
}
