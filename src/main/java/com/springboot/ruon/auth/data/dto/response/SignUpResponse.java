package com.springboot.ruon.auth.data.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignUpResponse {
    private UserResponse user;
    private TokenResponse token;
}
