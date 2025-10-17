package com.monito.domains.container.dto.response;

import com.monito.domains.container.domain.LogLevel;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Agent로부터 받아오는 컨테이너 로그 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerLogResponseDto {

    private String containerHash;
    private LocalDateTime timestamp;
    private LogLevel logLevel;
    private String message;
}