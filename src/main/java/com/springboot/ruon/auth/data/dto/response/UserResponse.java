package com.springboot.ruon.auth.data.dto.response;

import com.springboot.ruon.auth.data.entity.PregnancyStage;
import com.springboot.ruon.auth.data.entity.RoutineTimeAvailable;
import com.springboot.ruon.auth.data.entity.User;
import com.springboot.ruon.auth.data.entity.SkinConcern;
import com.springboot.ruon.auth.data.entity.SkinType;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private PregnancyStage pregnancyStage;
    private int pregnancyWeekNum;
    private boolean breastfeeding;
    private RoutineTimeAvailable routineTimeAvailable;
    private Set<SkinConcern> skinConcerns;
    private SkinType skinType;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .pregnancyStage(user.getPregnancyStage())
                .pregnancyWeekNum(user.getPregnancyWeekNum())
                .breastfeeding(user.isBreastfeeding())
                .routineTimeAvailable(user.getRoutineTimeAvailable())
                .skinConcerns(user.getSkinConcerns() == null ? Set.of() : user.getSkinConcerns())
                .skinType(user.getSkinType())
                .build();
    }
}
