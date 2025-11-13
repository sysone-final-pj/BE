package com.monito.domains.container.util;

/**
 * Docker Image ID 유틸리티
 */
public class ImageIdUtil {

    /**
     * Docker Image ID를 짧은 형태로 변환 (알고리즘 prefix 제거)
     * <p>
     * 예시:
     * - sha256:07ccdb7838758e758a4d52a9761636c385125a327355c0c94a6acff9babff938 → 07ccdb783875
     * - sha512:abc123def456... → abc123def456
     * - 07ccdb78... → 07ccdb78 (알고리즘 prefix 없는 경우)
     *
     * @param imageId 전체 이미지 ID
     * @return 짧은 이미지 ID (해시 앞 12자만, 알고리즘 prefix 제거)
     */
    public static String shortenImageId(String imageId) {
        if (imageId == null || imageId.isEmpty()) {
            return null;
        }

        // "sha256:07ccdb78..." → ["sha256", "07ccdb78..."]
        String[] parts = imageId.split(":", 2);

        String hash;
        if (parts.length == 2) {
            // "sha256:hash" 형태
            hash = parts[1];
        } else {
            // ":" 없으면 전체가 해시
            hash = imageId;
        }

        // 해시 앞 12자만 반환
        return hash.length() > 12 ? hash.substring(0, 12) : hash;
    }

    private ImageIdUtil() {
        // 유틸리티 클래스는 인스턴스화 방지
        throw new IllegalStateException("Utility class");
    }
}
