package com.springboot.ruon.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 임시 보안 설정.
 * 로그인/JWT 인증(auth 담당자 작업)이 아직 없어서, 개발 중 API 테스트가 가능하도록
 * 전체 permitAll로 열어둠. 인증 로직 붙으면 이 클래스는 auth 담당자가 교체/수정 예정.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
