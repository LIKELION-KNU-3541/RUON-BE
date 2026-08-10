package com.springboot.ruon.auth.service;

import com.springboot.ruon.auth.data.dto.response.UserResponse;
import com.springboot.ruon.auth.data.entity.PregnancyStage;
import com.springboot.ruon.auth.data.entity.RoutineTimeAvailable;
import com.springboot.ruon.auth.data.entity.User;
import com.springboot.ruon.auth.data.repository.UserRepository;
import com.springboot.ruon.auth.security.JwtTokenProvider;
import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void 내_정보를_조회한다() {
        User user = User.builder()
                .email("user@example.com")
                .password("encoded-password")
                .name("루온")
                .pregnancyStage(PregnancyStage.PREGNANT)
                .pregnancyWeekNum(20)
                .breastfeeding(false)
                .build();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = authService.getMyInfo(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getName()).isEqualTo("루온");
        assertThat(response.getRoutineTimeAvailable()).isEqualTo(RoutineTimeAvailable.MEDIUM);
    }

    @Test
    void 토큰의_사용자가_존재하지_않으면_404_예외를_던진다() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getMyInfo(99L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }
}
