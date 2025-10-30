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

    /**
     * 파티션 정리를 위한 전용 스레드 풀
     * - 매일 정해진 시간에 오래된 파티션 삭제
     * - 단일 스레드로 순차 처리하여 DB 부하 최소화
     */
    @Bean(name = "partitionCleanupExecutor")
    public Executor partitionCleanupExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);  // 단일 스레드
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("partition-cleanup-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(300);  // 파티션 삭제는 시간이 걸릴 수 있으므로 5분
        executor.initialize();
        return executor;
    }
}