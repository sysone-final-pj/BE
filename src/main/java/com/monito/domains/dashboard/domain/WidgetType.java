package com.monito.domains.dashboard.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WidgetType {
    CPU_GAUGE_CHART("CPU 게이지 차트"),
    MEM_DONUT_CHART("메모리 도넛 차트"),
    NETWORK_PROGRESS_BAR("네트워크 프로그레스 바"),
    CONTAINER_LIST("컨테이너 목록"),
    CONTAINER_STATUS("컨테이너 상태"),
    LOG_VIEWER("로그 뷰어");

    private final String description;
}
