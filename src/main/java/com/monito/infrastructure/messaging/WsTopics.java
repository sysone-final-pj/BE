package com.monito.infrastructure.messaging;

public final class WsTopics {
    public static final String CONTAINER_SUMMARY = "/topic/containers/summary";
    public static final String AGENT_STATUS = "/topic/agents/status";
    public static final String DASHBOARD_STATUS = "/topic/dashboard/list";

    public static String containerMetrics(Long containerId){
        return "/topic/containers/" + containerId + "/metrics";
    }
    public static String containerLogs(Long containerId){
        return "/topic/containers/" + containerId + "/logs";
    }
    public static String dashboardDetail(Long containerId){
        return "/topic/dashboard/detail/" + containerId;
    }
}
