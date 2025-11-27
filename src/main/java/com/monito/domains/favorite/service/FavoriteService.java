package com.monito.domains.favorite.service;
/**
 작성자: 백승준
 */
public interface FavoriteService {
    void addFavorite(Long memberId, Long containerId);
    void removeFavorite(Long memberId, Long containerId);
}
