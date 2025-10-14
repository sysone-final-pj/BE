package com.monito.domains.auth.controller;

import com.monito.domains.auth.dto.request.LoginRequestDTO;
import com.monito.domains.auth.dto.request.RefreshTokenRequestDTO;
import com.monito.domains.auth.dto.response.LoginResponseDTO;
import com.monito.domains.auth.service.AuthService;
import com.monito.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        LoginResponseDTO response = authService.login(loginRequest);
        return ApiResponse.ok(response, "로그인 성공");
    }

    /**
     * 토큰 갱신
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO refreshRequest) {
        LoginResponseDTO response = authService.refreshToken(refreshRequest);
        return ApiResponse.ok(response, "토큰 갱신 성공");
    }

    /**
     * 로그아웃 (클라이언트에서 토큰 삭제)
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok("로그아웃 성공");
    }
}
