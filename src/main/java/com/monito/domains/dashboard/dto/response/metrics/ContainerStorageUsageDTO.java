package com.monito.domains.dashboard.dto.response.metrics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
/**
 작성자: 이지민
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "컨테이너 스토리지 사용량 응답 DTO")
public class ContainerStorageUsageDTO {

    @Schema(description = "컨테이너 ID")
    private Long containerId;

    @Schema(description = "컨테이너 이름")
    private String containerName;

    @Schema(description = "스토리지 할당량 (bytes), 0이면 무제한")
    private Long storageLimit;

    @Schema(description = "현재 스토리지 사용량 (bytes)")
    private Long storageUsed;
}