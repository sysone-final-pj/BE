package com.monito.domains.favorite.controller;

import com.monito.domains.favorite.service.FavoriteService;
import com.monito.global.common.response.ApiResponse;
import com.monito.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
/**
 작성자: 백승준
 */
@Slf4j
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;

    @PostMapping("/{containerId}")
    public ApiResponse<Void> addFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long containerId
    ) {
        favoriteService.addFavorite(userDetails.getId(), containerId);
        return ApiResponse.ok("즐겨찾기가 등록되었습니다.");
    }

    @DeleteMapping("/{containerId}")
    public ApiResponse<Void> removeFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long containerId
    ) {
        favoriteService.removeFavorite(userDetails.getId(), containerId);
        return ApiResponse.ok("즐겨찾기가 삭제되었습니다.");
    }
}
