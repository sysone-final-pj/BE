package com.monito.domians.auth.service;

import com.monito.domians.auth.dto.request.LoginRequestDTO;
import com.monito.domians.auth.dto.request.RefreshTokenRequestDTO;
import com.monito.domians.auth.dto.response.LoginResponseDTO;

public interface AuthService {
    LoginResponseDTO login(LoginRequestDTO loginRequest);
    LoginResponseDTO refreshToken(RefreshTokenRequestDTO refreshRequest);
}
