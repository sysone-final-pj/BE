package com.monito.global.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
/**
 작성자: 백승준
 */
@Slf4j
@Component
public class CpuMetricsBufferCache {

    private static final int MAX_SIZE = 360; // 5초 * 360 = 30분
    private final ConcurrentHashMap<Long, Deque<BigDecimal>> buffers = new ConcurrentHashMap<>();

    public void addCpuSample(Long containerId, BigDecimal cpuPercent) {
        buffers.compute(containerId, (id, buffer) -> {
            if (buffer == null) buffer = new ArrayDeque<>();
            if (buffer.size() >= MAX_SIZE) buffer.pollFirst(); // 가장 오래된 값 제거
            buffer.addLast(cpuPercent);
            return buffer;
        });
    }

    public List<BigDecimal> getSamples(Long containerId) {
        return new ArrayList<>(buffers.getOrDefault(containerId, new ArrayDeque<>()));
    }

    public void removeContainer(Long containerId) {
        buffers.remove(containerId);
    }
}
