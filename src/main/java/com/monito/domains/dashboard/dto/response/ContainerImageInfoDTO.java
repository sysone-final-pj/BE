package com.monito.domains.dashboard.dto.response;

import com.monito.domains.container.domain.Container;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 컨테이너 이미지 정보 DTO
 * Docker 이미지 정보를 REPOSITORY, TAG, IMAGE ID, SIZE 형태로 제공
 */
@Getter
@Builder
@AllArgsConstructor
public class ContainerImageInfoDTO {
    /**
     * 이미지 저장소 이름
     * 예: "ubuntu", "nginx"
     */
    private String repository;

    /**
     * 이미지 태그
     * 예: "latest", "20.04"
     */
    private String tag;

    /**
     * 이미지 ID (컨테이너 해시)
     */
    private String imageId;

    /**
     * 이미지 크기 (bytes)
     */
    private Long size;

    /**
     * Container 엔티티로부터 DTO 생성
     * imageName을 ":" 기준으로 파싱하여 repository와 tag로 분리
     *
     * @param container Container 엔티티
     * @return ContainerImageInfoDTO
     */
    public static ContainerImageInfoDTO from(Container container) {
        String imageName = container.getImageName();
        String repository;
        String tag;

        if (imageName != null && imageName.contains(":")) {
            int colonIndex = imageName.indexOf(':');
            repository = imageName.substring(0, colonIndex);
            tag = imageName.substring(colonIndex + 1);
        } else {
            repository = imageName;
            tag = "latest";
        }

        return ContainerImageInfoDTO.builder()
                .repository(repository)
                .tag(tag)
                .imageId(container.getContainerHash())
                .size(container.getImageSize())
                .build();
    }
}