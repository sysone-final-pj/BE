package com.monito.domains.agent.service;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import com.monito.domains.agent.dto.request.AgentCreateRequestDTO;
import com.monito.domains.agent.dto.request.AgentUpdateRequestDTO;
import com.monito.domains.agent.dto.response.AgentCreateResponseDTO;
import com.monito.domains.agent.dto.response.AgentDetailResponseDTO;
import com.monito.domains.agent.dto.response.AgentSummaryResponseDTO;
import com.monito.domains.agent.dto.response.AgentUpdateResponseDTO;
import com.monito.domains.agent.repository.AgentRepository;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerState;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.global.cache.AgentMetadataCache;
import com.monito.global.common.entity.BaseEntity;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AgentServiceImpl implements AgentService {
    private final AgentRepository agentRepository;
    private final ContainerRepository containerRepository;
    private final AgentMetadataCache agentMetadataCache;

    @Override
    @Transactional(readOnly = true)
    public AgentDetailResponseDTO getAgent(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(ExceptionMessage.DATA_NOT_FOUND)
                );

        return AgentDetailResponseDTO.from(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentSummaryResponseDTO> getAgentList(String keyword) {
        // keyword trim 처리 (빈 문자열은 null로 변환)
        String searchKeyword = keyword != null && !keyword.trim().isEmpty()
                ? keyword.trim()
                : null;

        // 하나의 쿼리로 전체 조회 및 검색 처리
        return agentRepository.findAllWithSearch(searchKeyword).stream()
                .map(AgentSummaryResponseDTO::from)
                .toList();
    }

    @Override
    public AgentUpdateResponseDTO updateAgent(Long id, AgentUpdateRequestDTO agentUpdateRequestDTO) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(ExceptionMessage.DATA_NOT_FOUND)
                );

        agent.updateAgentName(agentUpdateRequestDTO.getAgentName());
        agent.updateDescription(agentUpdateRequestDTO.getDescription());

        return AgentUpdateResponseDTO.from(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public Agent authenticateAgent(String agentKey) {
        Agent agent = agentRepository.findByAgentKey(agentKey)
                .orElseThrow(() ->
                    new NotFoundException(ExceptionMessage.DATA_NOT_FOUND)
                );

        log.info("인증 성공 - agentKey: {}, agentName: {}", agentKey, agent.getAgentName());
        return agent;
    }

    @Override
    public void updateAgentStatus(String agentKey, AgentStatus status) {
        agentRepository.findByAgentKey(agentKey)
                .ifPresent(agent -> {
                    agent.updateStatus(status);
                    log.info("Agent 상태 변경 - agentKey: {}, status: {} -> {}",
                            agentKey, agent.getAgentStatus(), status);

                    if (status == AgentStatus.OFFLINE) {
                        markContainersAsUnknown(agent);
                    }
                });
    }

    @Override
    public void deleteAgent(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(ExceptionMessage.DATA_NOT_FOUND)
                );

        agent.markAsDeleted();

        // Soft delete all containers belonging to this agent
        containerRepository.findAllByAgent_Id(id)
                .forEach(BaseEntity::markAsDeleted);

        // agent 메타 데이터 삭제
        agentMetadataCache.removeMetadata(agent.getAgentKey());
    }

    @Override
    @Transactional(readOnly = true)
    public Agent findByAgentKey(String agentKey) {
        return agentRepository.findByAgentKey(agentKey)
                .orElseThrow(() ->
                        new NotFoundException(ExceptionMessage.DATA_NOT_FOUND)
                );
    }

    @Override
    public AgentCreateResponseDTO createAgent(AgentCreateRequestDTO dto) {
        Agent save = agentRepository.save(dto.toEntity());
        return AgentCreateResponseDTO.from(save);
    }

    public void markContainersAsUnknown(Agent agent) {
        List<Container> containers = containerRepository.findAllByAgent(agent);
        for (Container container : containers) {
            container.changeState(ContainerState.UNKNOWN);
        }
        log.info("Agent OFFLINE → 컨테이너 {}개 UNKNOWN 처리 - agentKey: {}",
                containers.size(), agent.getAgentKey());
    }
}
