package com.monito.domains.favorite.service;

import com.monito.domains.container.domain.Container;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.favorite.domain.Favorite;
import com.monito.domains.favorite.repository.FavoriteRepository;
import com.monito.domains.member.domain.Member;
import com.monito.domains.member.repository.MemberRepository;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;
    private final ContainerRepository containerRepository;

    @Override
    public void addFavorite(Long memberId, Long containerId) {
        if (favoriteRepository.findByMemberIdAndContainerId(memberId, containerId).isPresent()) {
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
    }

    @Override
    public void removeFavorite(Long memberId, Long containerId) {
        favoriteRepository.deleteByMemberIdAndContainerId(memberId, containerId);
    }
}
