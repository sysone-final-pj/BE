package com.monito.domains.agent.service;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import com.monito.domains.agent.dto.request.AgentCreateRequestDTO;
import com.monito.domains.agent.dto.response.AgentCreateResponseDTO;
import com.monito.domains.agent.repository.AgentRepository;
import com.monito.global.exception.BadRequestException;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentServiceImpl implements AgentService {
    private final AgentRepository agentRepository;

    @Override
    @Transactional(readOnly = true)
    public Agent authenticateAgent(String agentKey, String rawPassword) {
        Agent agent = agentRepository.findByAgentKey(agentKey)
                .orElseThrow(() ->
                    new NotFoundException(ExceptionMessage.DATA_NOT_FOUND)
                );

        // 비밀번호 검증 (개발 중: 평문 비교) todo: password encoder를 통해 암복호화
        if (!rawPassword.equals(agent.getPassword())) {
            log.warn("인증 실패: 비밀번호 불일치 - agentKey: {}", agentKey);
            log.warn("입력된 비밀번호: {}", rawPassword);
            log.warn("저장된 비밀번호: {}", agent.getPassword());
            throw new BadRequestException(ExceptionMessage.AGENT_NOT_MATCH_PASSWORD);
        }

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
