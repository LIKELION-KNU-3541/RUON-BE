package com.springboot.ruon.auth.controller;
import com.springboot.ruon.auth.data.dto.request.LoginRequest;
import com.springboot.ruon.auth.data.dto.request.SignUpRequest;
import com.springboot.ruon.auth.data.dto.response.SignUpResponse;
import com.springboot.ruon.auth.data.dto.response.TokenResponse;
import com.springboot.ruon.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}