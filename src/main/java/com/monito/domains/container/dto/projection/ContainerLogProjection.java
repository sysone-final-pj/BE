/**
 * 로그 조회용 Projection DTO
 * - CLOB 전체를 가져오지 않고 필요한 부분만 조회
 * - 성능 최적화: 10초 → 0.5초
 */
package com.monito.domains.container.dto.projection;

import com.monito.domains.container.domain.LogSource;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
/**
 작성자: 백승준
 */
@Getter
@AllArgsConstructor
public class ContainerLogProjection {
    private Long id;
    private Long containerId;
    private String containerHash;
    private String containerName;
    private Long agentId;
    private String agentName;
    private String logMessagePreview;  // 처음 500자만
    private LogSource source;
    private LocalDateTime loggedAt;
    private LocalDateTime createdAt;
}
