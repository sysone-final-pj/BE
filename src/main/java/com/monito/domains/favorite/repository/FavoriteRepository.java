package com.monito.domains.favorite.repository;

import com.monito.domains.favorite.domain.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
/**
 작성자: 백승준
 */
@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByMemberIdAndContainerId(Long memberId, Long containerId);

    void deleteByMemberIdAndContainerId(Long memberId, Long containerId);

    void deleteByContainerId(Long containerId);
}
