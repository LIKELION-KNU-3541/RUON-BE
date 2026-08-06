package com.springboot.ruon.auth.data.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

@Entity
@Table(name = "user")
@NoArgsConstructor
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userId", updatable = false)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private PregnancyStage pregnancyStage;

    @Column(nullable = false)
    private int pregnancyWeekNum;

    @Column(nullable = false)
    private boolean breastfeeding;

    @Column(nullable = false)
    private RoutineTimeAvailable routineTimeAvailable;

    @Builder
    public User(String email, String password, String name, PregnancyStage pregnancyStage,
                int pregnancyWeekNum, boolean breastfeeding, RoutineTimeAvailable routineTimeAvailable) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.pregnancyStage = pregnancyStage;
        this.pregnancyWeekNum = pregnancyWeekNum;
        this.breastfeeding = breastfeeding;
        this.routineTimeAvailable = routineTimeAvailable;
    }

}
