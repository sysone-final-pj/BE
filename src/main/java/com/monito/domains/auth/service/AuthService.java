package com.monito.domains.auth.service;

import com.monito.domains.auth.dto.request.LoginRequestDTO;
import com.monito.domains.auth.dto.request.RefreshTokenRequestDTO;
import com.monito.domains.auth.dto.response.LoginResponseDTO;

public interface AuthService {
    LoginResponseDTO login(LoginRequestDTO loginRequest);
    LoginResponseDTO refreshToken(RefreshTokenRequestDTO refreshRequest);
}
