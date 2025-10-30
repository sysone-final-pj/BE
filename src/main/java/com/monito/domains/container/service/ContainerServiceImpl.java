package com.monito.domains.container.service;

import com.monito.domains.container.dto.response.ContainerListResponseDTO;
import com.monito.domains.container.repository.ContainerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 컨테이너 조회 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContainerServiceImpl implements ContainerService {

    private final ContainerRepository containerRepository;

    @Override
    public List<ContainerListResponseDTO> getAllContainers() {
        log.info("Fetching all containers with latest stats");
        return containerRepository.findAllContainerList();
    }

    @Override
    public List<ContainerListResponseDTO> getContainersByAgentId(Long agentId) {
        log.info("Fetching containers for agentId: {}", agentId);
        return containerRepository.findContainerListByAgentId(agentId);
    }
}