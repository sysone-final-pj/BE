package com.monito.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {

    @Value("${app.scheduler.thread-pool.core-size}")
    private int threadPoolCoreSize;

    @Value("${app.scheduler.thread-pool.max-size}")
    private int threadPoolMaxSize;

    @Value("${app.scheduler.thread-pool.queue-capacity}")
    private int threadPoolQueueCapacity;

    /**
     * Agent 폴링을 위한 비동기 실행 스레드 풀
     * - 각 Agent별로 비동기로 데이터 수집
     * - application.properties 파일에서 환경변수로 조정 가능
     */
    @Bean(name = "agentPollingExecutor")
    public Executor agentPollingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolCoreSize);
        executor.setMaxPoolSize(threadPoolMaxSize);
        executor.setQueueCapacity(threadPoolQueueCapacity);
        executor.setThreadNamePrefix("agent-poll-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}