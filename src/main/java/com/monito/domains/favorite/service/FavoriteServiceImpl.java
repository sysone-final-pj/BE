package com.monito.domains.favorite.service;

import com.monito.domains.container.domain.Container;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.favorite.domain.Favorite;
import com.monito.domains.favorite.repository.FavoriteRepository;
import com.monito.domains.member.domain.Member;
import com.monito.domains.member.repository.MemberRepository;
import com.monito.global.cache.FavoriteCache;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.NotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;
    private final ContainerRepository containerRepository;
    private final FavoriteCache favoriteCache;

    /**
     * 서버 시작 시 DB의 모든 favorite 데이터를 캐시로 로드
     */
    @PostConstruct
    @Transactional(readOnly = true)
    public void initializeFavoriteCache() {
        log.info("[FavoriteCache] 초기화 시작");

        try {
            // 1. DB에서 모든 favorite 조회
            List<Favorite> allFavorites = favoriteRepository.findAll();

            if (allFavorites.isEmpty()) {
                log.info("[FavoriteCache] 초기화 완료 - 즐겨찾기 데이터 없음");
                return;
            }

            // 2. memberId별로 그룹핑하여 Set<containerId> 생성
            Map<Long, Set<Long>> favoritesByMember = allFavorites.stream()
                    .collect(Collectors.groupingBy(
                            favorite -> favorite.getMember().getId(),
                            Collectors.mapping(
                                    favorite -> favorite.getContainer().getId(),
                                    Collectors.toSet()
                            )
                    ));

            // 3. 캐시 초기화 (기존 데이터 클리어)
            favoriteCache.clear();

            // 4. 각 사용자별로 캐시에 데이터 로드
            favoritesByMember.forEach(favoriteCache::initialize);

            log.info("[FavoriteCache] 초기화 완료 - 총 {} 명의 즐겨찾기 데이터 로드, 전체 favorite 수: {}",
                    favoritesByMember.size(), allFavorites.size());

        } catch (Exception e) {
            log.error("[FavoriteCache] 초기화 실패", e);
            throw new RuntimeException("FavoriteCache 초기화 실패", e);
        }
    }

    @Override
    public void addFavorite(Long memberId, Long containerId) {
        if (favoriteRepository.findByMemberIdAndContainerId(memberId, containerId).isPresent()) {
            favoriteCache.add(memberId, containerId);
            return;
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.MEMBER_NOT_FOUND));

        Container container = containerRepository.findById(containerId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.CONTAINER_NOT_FOUND));

        Favorite favorite = Favorite.builder()
                .member(member)
                .container(container)
                .build();

        favoriteRepository.save(favorite);
        favoriteCache.add(memberId, containerId);
    }

    @Override
    public void removeFavorite(Long memberId, Long containerId) {
        favoriteRepository.deleteByMemberIdAndContainerId(memberId, containerId);
        favoriteCache.remove(memberId, containerId);
    }
}
