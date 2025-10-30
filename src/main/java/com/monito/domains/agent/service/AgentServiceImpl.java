package com.monito.domains.agent.service;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import com.monito.domains.agent.dto.request.AgentCreateRequestDTO;
import com.monito.domains.agent.dto.response.AgentCreateResponseDTO;
import com.monito.domains.agent.dto.response.AgentDetailResponseDTO;
import com.monito.domains.agent.dto.response.AgentSummaryResponseDTO;
import com.monito.domains.agent.repository.AgentRepository;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentServiceImpl implements AgentService {
    private final AgentRepository agentRepository;

    @Override
    public AgentDetailResponseDTO getAgent(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(ExceptionMessage.DATA_NOT_FOUND)
                );

        return AgentDetailResponseDTO.from(agent);
    }

    @Override
    public List<AgentSummaryResponseDTO> getAgentList() {
        return agentRepository.findAll().stream()
                .map(AgentSummaryResponseDTO::from)
                .toList();
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
    @Transactional
    public void updateAgentStatus(String agentKey, AgentStatus status) {
        agentRepository.findByAgentKey(agentKey)
                .ifPresent(agent -> {
                    agent.updateStatus(status);
                    log.info("Agent 상태 변경 - agentKey: {}, status: {} -> {}",
                            agentKey, agent.getAgentStatus(), status);
                });
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
}
