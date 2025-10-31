package com.monito.domains.favorite.service;

public interface FavoriteService {
    void addFavorite(Long memberId, Long containerId);
    void removeFavorite(Long memberId, Long containerId);
}
