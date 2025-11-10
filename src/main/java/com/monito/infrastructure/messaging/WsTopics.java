package com.monito.infrastructure.messaging;

public final class WsTopics {
    public static final String CONTAINER_SUMMARY = "/topic/containers/summary";
    public static String containerMetrics(Long containerId){
        return "/topic/containers/" + containerId + "/metrics";
    }
    public static String containerLogs(Long containerId){
        return "/topic/containers/" + containerId + "/logs";
    }
}
