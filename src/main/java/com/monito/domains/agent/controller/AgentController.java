package com.monito.domains.agent.controller;

import com.monito.domains.agent.dto.request.AgentCreateRequestDTO;
import com.monito.domains.agent.dto.response.AgentCreateResponseDTO;
import com.monito.domains.agent.service.AgentService;
import com.monito.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    public ApiResponse<AgentCreateResponseDTO> createAgent(@RequestBody AgentCreateRequestDTO dto){
        return ApiResponse.created(agentService.createAgent(dto),"에이전트가 성공적으로 생성되었습니다.");
    }
}
