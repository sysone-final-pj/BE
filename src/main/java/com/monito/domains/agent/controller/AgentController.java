package com.monito.domains.agent.controller;

import com.monito.domains.agent.dto.request.AgentCreateRequestDTO;
import com.monito.domains.agent.dto.request.AgentUpdateRequestDTO;
import com.monito.domains.agent.dto.response.AgentCreateResponseDTO;
import com.monito.domains.agent.dto.response.AgentDetailResponseDTO;
import com.monito.domains.agent.dto.response.AgentSummaryResponseDTO;
import com.monito.domains.agent.dto.response.AgentUpdateResponseDTO;
import com.monito.domains.agent.service.AgentService;
import com.monito.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @GetMapping
    public ApiResponse<List<AgentSummaryResponseDTO>> getAgentList(
            @RequestParam(required = false) String keyword
    ){
        return ApiResponse.ok(agentService.getAgentList(keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<AgentDetailResponseDTO> getAgent(@PathVariable Long id){
        return ApiResponse.ok(agentService.getAgent(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<AgentCreateResponseDTO> createAgent(@RequestBody AgentCreateRequestDTO dto){
        return ApiResponse.created(agentService.createAgent(dto),"에이전트가 성공적으로 생성되었습니다.");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<AgentUpdateResponseDTO> updateAgent(@PathVariable Long id,
                                                           @RequestBody AgentUpdateRequestDTO agentUpdateRequestDTO){
        return ApiResponse.ok(agentService.updateAgent(id, agentUpdateRequestDTO));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteAgent(@PathVariable Long id){
        agentService.deleteAgent(id);
        return ApiResponse.ok("에이전트가 삭제되었습니다.");
    }
}
