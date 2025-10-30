package com.monito.domains.container.controller;

import com.monito.domains.container.dto.response.ContainerListResponseDTO;
import com.monito.domains.container.service.ContainerService;
import com.monito.global.common.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 컨테이너 대시보드 API Controller
 * - 컨테이너 목록 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/containers")
@RequiredArgsConstructor
public class ContainerController {

    private final ContainerService containerService;

    /**
     * 전체 컨테이너 목록 조회 (최신 통계 포함)
     * @return 전체 컨테이너 목록
     */
    @GetMapping
    public ApiResponse<List<ContainerListResponseDTO>> getAllContainers() {
        log.info("GET /api/containers");

        List<ContainerListResponseDTO> containers = containerService.getAllContainers();

        return ApiResponse.ok(containers, "전체 컨테이너 목록을 성공적으로 조회했습니다.");
    }
}