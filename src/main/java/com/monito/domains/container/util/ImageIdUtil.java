package com.monito.domains.container.util;

/**
 * Docker Image ID 유틸리티
 */
public class ImageIdUtil {

    /**
     * Docker Image ID/Name에서 알고리즘 prefix 제거
     * <p>
     * 예시:
     * - sha256:07ccdb7838758e758a4d52a9761636c385125a327355c0c94a6acff9babff938
     *   → 07ccdb7838758e758a4d52a9761636c385125a327355c0c94a6acff9babff938
     * - sha512:abc123def456... → abc123def456...
     * - nginx → nginx (prefix 없으면 그대로)
     *
     * @param value 이미지 ID 또는 이름
     * @return prefix 제거된 값
     */
    public static String removePrefix(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // "sha256:hash" → ["sha256", "hash"]
        String[] parts = value.split(":", 2);

        if (parts.length == 2) {
            // "sha256:" 같은 알고리즘 prefix 제거
            return parts[1];
        } else {
            // ":" 없으면 그대로 반환
            return value;
        }
    }

    /**
     * 짧은 이미지 ID 반환 (앞 12자만)
     * - 호환성 유지용 메서드
     *
     * @deprecated removePrefix() 사용 권장
     */
    @Deprecated
    public static String shortenImageId(String imageId) {
        String hash = removePrefix(imageId);
        if (hash == null) {
            return null;
        }
        return hash.length() > 12 ? hash.substring(0, 12) : hash;
    }

    private ImageIdUtil() {
        // 유틸리티 클래스는 인스턴스화 방지
        throw new IllegalStateException("Utility class");
    }
}
