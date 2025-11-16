package com.monito.domains.history.service;

import com.monito.domains.container.domain.ContainerHealth;
import com.monito.domains.container.domain.ContainerState;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.history.dto.request.ContainerHistoryRequest;
import com.monito.domains.history.dto.response.ContainerHistoryPageResponse;
import com.monito.domains.history.dto.response.ContainerHistoryResponse;
import com.monito.domains.history.dto.response.ContainerListForHistoryDTO;
import com.monito.domains.history.repository.ContainerHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContainerHistoryServiceImpl implements ContainerHistoryService {

    private final ContainerHistoryRepository containerHistoryRepository;
    private final ContainerRepository containerRepository;

    @Override
    public ContainerHistoryPageResponse getContainerHistory(ContainerHistoryRequest request) {
        // 페이지 요청 생성 (정렬 포함)
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(
                        "DESC".equalsIgnoreCase(request.getSortDirection())
                                ? Sort.Direction.DESC
                                : Sort.Direction.ASC,
                        request.getSortBy()
                )
        );

        // Repository에서 데이터 조회
        Page<Object[]> resultPage = containerHistoryRepository.findContainerHistory(
                request.getStartTime(),
                request.getEndTime(),
                request.getContainerId(),
                request.getIsDeleted(),
                pageable
        );

        // Object[] -> ContainerHistoryResponse 변환
        List<ContainerHistoryResponse> content = resultPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        // 페이지 응답 생성
        return ContainerHistoryPageResponse.builder()
                .content(content)
                .pageNumber(resultPage.getNumber())
                .pageSize(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .first(resultPage.isFirst())
                .last(resultPage.isLast())
                .hasNext(resultPage.hasNext())
                .hasPrevious(resultPage.hasPrevious())
                .build();
    }

    /**
     * Object[] 배열을 ContainerHistoryResponse로 변환
     */
    private ContainerHistoryResponse mapToResponse(Object[] row) {
        int idx = 0;

        return ContainerHistoryResponse.builder()
                // 기본 정보
                .collectedAt((LocalDateTime) row[idx++])
                .containerName((String) row[idx++])
                .containerHash((String) row[idx++])
                .agentName((String) row[idx++])
                .imgNameTag((String) row[idx++])
                .state((ContainerState) row[idx++])
                .health((ContainerHealth) row[idx++])
                .containerCreatedAt((LocalDateTime) row[idx++])
                .isDeleted((Integer) row[idx++])

                // CPU 메트릭
                .cpuPercent((BigDecimal) row[idx++])
                .cpuCoreUsage((BigDecimal) row[idx++])
                .hostCpuUsageTotal((Long) row[idx++])
                .cpuUsageTotal((Long) row[idx++])
                .cpuUser((Long) row[idx++])
                .cpuSystem((Long) row[idx++])
                .cpuQuota((Long) row[idx++])
                .cpuPeriod((Long) row[idx++])
                .onlineCpus((Integer) row[idx++])
                .throttlingPeriods((Long) row[idx++])
                .throttledPeriods((Long) row[idx++])
                .throttledTime((Long) row[idx++])
                .cpuLimitCores((BigDecimal) row[idx++])
                .isCpuUnlimited((Boolean) row[idx++])

                // Memory 메트릭
                .memPercent((BigDecimal) row[idx++])
                .memUsage((Long) row[idx++])
                .memMaxUsage((Long) row[idx++])
                .memLimit((Long) row[idx++])
                .isMemoryUnlimited((Boolean) row[idx++])
                .lastOomKilledAt((LocalDateTime) row[idx++])

                // Block I/O 메트릭
                .blkRead((Long) row[idx++])
                .blkWrite((Long) row[idx++])
                .blkReadPerSec((Long) row[idx++])
                .blkWritePerSec((Long) row[idx++])

                // Network 메트릭
                .rxBytes((Long) row[idx++])
                .txBytes((Long) row[idx++])
                .rxPackets((Long) row[idx++])
                .txPackets((Long) row[idx++])
                .networkTotalBytes((Long) row[idx++])
                .rxBytesPerSec((Long) row[idx++])
                .txBytesPerSec((Long) row[idx++])
                .rxPps((Long) row[idx++])
                .txPps((Long) row[idx++])
                .rxFailureRate((BigDecimal) row[idx++])
                .txFailureRate((BigDecimal) row[idx++])
                .rxErrors((Integer) row[idx++])
                .txErrors((Integer) row[idx++])
                .rxDropped((Integer) row[idx++])
                .txDropped((Integer) row[idx++])

                // Storage 메트릭
                .sizeRw((Long) row[idx++])
                .sizeRootFs((Long) row[idx++])
                .storageLimit((Long) row[idx++])
                .isStorageUnlimited((Boolean) row[idx++])

                .build();
    }

    @Override
    public List<ContainerListForHistoryDTO> getContainerListForHistory(Integer isDeleted) {
        log.info("히스토리 조회용 컨테이너 목록 조회 - isDeleted: {}", isDeleted);

        // Repository에서 네이티브 쿼리로 조회
        List<Object[]> resultList = containerRepository.findContainerListForHistory(isDeleted);

        // Object[] -> ContainerListForHistoryDTO 변환
        return resultList.stream()
                .map(row -> ContainerListForHistoryDTO.builder()
                        .id(((Number) row[0]).longValue())
                        .containerName((String) row[1])
                        .containerHash((String) row[2])
                        .isDeleted(((Number) row[3]).intValue())
                        .build())
                .collect(Collectors.toList());
    }
}