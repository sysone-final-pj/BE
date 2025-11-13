package com.monito.domains.dashboard.dto.response.metrics;

import com.monito.domains.container.domain.Container;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대시보드 이미지 정보 DTO
 *
 * @deprecated 이미지 정보가 DashboardContainerInfoDTO에 통합되었습니다.
 *             더 이상 별도 객체로 사용하지 않습니다.
 */
@Deprecated
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대시보드 이미지 정보 (deprecated)")
public class DashboardImageInfoDTO {

    @Schema(description = "이미지 레포지토리 (imageName을 ':'로 split한 [0])")
    private String repository;

    @Schema(description = "이미지 태그 (imageName을 ':'로 split한 [1])")
    private String tag;

    @Schema(description = "이미지 이름 (원본)")
    private String imageName;

    @Schema(description = "이미지 크기 (bytes)")
    private Long imageSize;

    public static DashboardImageInfoDTO from(Container container) {
        // Image Name 파싱: repository와 tag 분리
        String repository = null;
        String tag = null;
        if (container.getImageName() != null && container.getImageName().contains(":")) {
            String[] parts = container.getImageName().split(":", 2);
            repository = parts[0];
            tag = parts.length > 1 ? parts[1] : null;
        } else {
            repository = container.getImageName();
            tag = "latest"; // 기본값
        }

        return DashboardImageInfoDTO.builder()
                .repository(repository)
                .tag(tag)
                .imageName(container.getImageName())
                .imageSize(container.getImageSize())
                .build();
    }
}