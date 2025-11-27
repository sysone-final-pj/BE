/**
 * 대시보드 컨테이너 기본 정보 DTO
 * - 컨테이너 기본 정보 + 이미지 정보 통합
 */
package com.monito.domains.dashboard.dto.response.metrics;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.container.util.ImageIdUtil;
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
@Schema(description = "대시보드 컨테이너 기본 정보")
public class DashboardContainerInfoDTO {

    @Schema(description = "컨테이너 ID")
    private Long containerId;

    @Schema(description = "Agent ID")
    private Long agentId;

    @Schema(description = "Agent 이름")
    private String agentName;

    @Schema(description = "컨테이너 이름")
    private String containerName;

    @Schema(description = "컨테이너 해시")
    private String containerHash;

    @Schema(description = "컨테이너 상태 (RUNNING, STOPPED 등)")
    private String state;

    @Schema(description = "컨테이너 실행 정보")
    private String status;

    @Schema(description = "헬스 상태")
    private String health;

    // 이미지 정보 (container에 통합)
    @Schema(description = "이미지 레포지토리 (imageName을 ':'로 split한 [0])")
    private String repository;

    @Schema(description = "이미지 태그 (imageName을 ':'로 split한 [1])")
    private String tag;

    @Schema(description = "이미지 이름 (sha256: prefix 제거됨)", example = "nginx")
    private String imageName;

    @Schema(description = "이미지 ID (전체, sha256: prefix 제거됨)", example = "07ccdb7838758e758a4d52a9761636c385125a327355c0c94a6acff9babff938")
    private String imageId;

    @Schema(description = "이미지 크기 (bytes)")
    private Long imageSize;

    public static DashboardContainerInfoDTO from(Container container, Agent agent, ContainerStatsLog statsLog) {
        // Image Name에서 sha256: prefix 제거
        String processedImageName = ImageIdUtil.removePrefix(container.getImageName());

        // Image Name 파싱: repository와 tag 분리
        String repository = null;
        String tag = null;
        if (processedImageName != null && processedImageName.contains(":")) {
            String[] parts = processedImageName.split(":", 2);
            repository = parts[0];
            tag = parts.length > 1 ? parts[1] : null;
        } else {
            repository = processedImageName;
            tag = "latest"; // 기본값
        }

        return DashboardContainerInfoDTO.builder()
                .containerId(container.getId())
                .agentId(agent.getId())
                .agentName(agent.getAgentName())
                .containerName(container.getName())
                .containerHash(container.getContainerHash())
                .state(statsLog.getState().name())
                .status(container.getStatus())
                .health(statsLog.getHealth().name())
                // 이미지 정보
                .repository(repository)
                .tag(tag)
                .imageName(processedImageName)
                .imageId(ImageIdUtil.removePrefix(container.getImageId()))
                .imageSize(container.getImageSize())
                .build();
    }
}