package com.springboot.ruon.auth.data.dto.response;

import com.springboot.ruon.auth.data.entity.PregnancyStage;
import com.springboot.ruon.auth.data.entity.RoutineTimeAvailable;
import com.springboot.ruon.auth.data.entity.User;
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

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .pregnancyStage(user.getPregnancyStage())
                .pregnancyWeekNum(user.getPregnancyWeekNum())
                .breastfeeding(user.isBreastfeeding())
                .routineTimeAvailable(user.getRoutineTimeAvailable())
                .build();
    }
}
