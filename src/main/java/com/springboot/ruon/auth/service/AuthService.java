package com.springboot.ruon.auth.service;

import com.springboot.ruon.auth.data.dto.request.LoginRequest;
import com.springboot.ruon.auth.data.dto.request.SignUpRequest;
import com.springboot.ruon.auth.data.dto.response.SignUpResponse;
import com.springboot.ruon.auth.data.dto.response.TokenResponse;
import com.springboot.ruon.auth.data.dto.response.UserResponse;
import com.springboot.ruon.auth.data.entity.User;
import com.springboot.ruon.auth.data.repository.UserRepository;
import com.springboot.ruon.auth.security.JwtTokenProvider;
import com.springboot.ruon.global.exception.BusinessException;
import com.springboot.ruon.global.exception.CustomException;
import com.springboot.ruon.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public SignUpResponse signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .pregnancyStage(request.getPregnancyStage())
                .pregnancyWeekNum(request.getPregnancyWeekNum())
                .breastfeeding(request.isBreastfeeding())
                .routineTimeAvailable(request.getRoutineTimeAvailable())
                .build();

        userRepository.save(user);

        return SignUpResponse.builder()
                .user(UserResponse.from(user))
                .token(issueToken(user))
                .build();
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return issueToken(user);
    }

    public UserResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        return UserResponse.from(user);
    }

    private TokenResponse issueToken(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                .build();
    }
}
