package com.monito.domains.auth.service;

import com.monito.domains.auth.dto.request.LoginRequestDTO;
import com.monito.domains.auth.dto.request.RefreshTokenRequestDTO;
import com.monito.domains.auth.dto.response.LoginResponseDTO;
import com.monito.domains.member.domain.Member;
import com.monito.domains.member.repository.MemberRepository;
import com.monito.global.exception.BadRequestException;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService{
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    /**
     * 로그인 처리
     */
    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        try {
            // 사용자 인증
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // 사용자 정보 조회
            Member member = memberRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new BadRequestException(ExceptionMessage.MEMBER_NOT_FOUND));

            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.generateAccessToken(
                    member.getId(),
                    member.getUsername(),
                    member.getRole().name()
            );
            String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

            log.info("User '{}' logged in successfully", member.getUsername());

            return LoginResponseDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpiration / 1000) // 초 단위로 변환
                    .userId(member.getId())
                    .username(member.getUsername())
                    .email(member.getEmail())
                    .role(member.getRole())
                    .build();

        } catch (Exception e) {
            log.error("Login failed for user: {}", loginRequest.getUsername(), e);
            throw new BadRequestException(ExceptionMessage.LOGIN_FAILED);
        }
    }

    /**
     * 토큰 갱신
     */
    public LoginResponseDTO refreshToken(RefreshTokenRequestDTO refreshRequest) {
        String refreshToken = refreshRequest.getRefreshToken();

        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BadRequestException(ExceptionMessage.INVALID_TOKEN);
        }

        try {
            // Refresh Token에서 사용자 ID 추출
            Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

            // 사용자 정보 조회
            Member member = memberRepository.findById(userId)
                    .map(dto -> Member.builder()
                            .id(dto.getId())
                            .username(dto.getUsername())
                            .role(dto.getRole())
                            .email(dto.getEmail())
                            .build())
                    .orElseThrow(() -> new BadRequestException(ExceptionMessage.MEMBER_NOT_FOUND));

            // 새로운 토큰 생성
            String newAccessToken = jwtTokenProvider.generateAccessToken(
                    member.getId(),
                    member.getUsername(),
                    member.getRole().name()
            );
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

            log.info("Token refreshed for user: {}", member.getUsername());

            return LoginResponseDTO.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpiration / 1000)
                    .userId(member.getId())
                    .username(member.getUsername())
                    .email(member.getEmail())
                    .role(member.getRole())
                    .build();

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new BadRequestException(ExceptionMessage.TOKEN_REFRESH_FAILED);
        }
    }
}
