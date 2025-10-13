package com.monito.global.common.aop;

import com.monito.global.common.entity.BaseEntity;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class JDBCAuditingAspect {

    /**
     * com.monito 패키지 내 모든 Service 클래스의 save* 또는 update* 메서드를 대상으로 지정
     */
    @Around("execution(* com.monito..domains.*.service.*Service.save*(..)) || " +
            "execution(* com.monito..domains.*.service.*Service.update*(..)) || " +
            "execution(* com.monito..domains.*.service.*Service.create*(..)) || " +
            "execution(* com.monito..domains.*.service.*Service.add*(..)) || " +
            "execution(* com.monito..domains.*.service.*Service.regis*(..))")
    public Object setAuditingFields(ProceedingJoinPoint pjp) throws Throwable {

        Object[] args = pjp.getArgs();

        for (Object arg : args) {
            if (arg instanceof BaseEntity) {
                BaseEntity entity = (BaseEntity) arg;

                // BaseEntity의 createdAt 필드가 null이면 최초 생성(INSERT)으로 간주
                if (entity.getCreatedAt() == null) {
                    entity.onCreate();
                    log.debug("AOP Auditing: INSERT - Entity onCreate() called.");
                } else {
                    // 이미 createdAt이 있다면 수정(UPDATE)으로 간주
                    entity.onUpdate();
                    log.debug("AOP Auditing: UPDATE - Entity onUpdate() called.");
                }
            }
        }

        // 원본 Service 메서드를 실행 (JdbcTemplate으로 DB에 저장)
        return pjp.proceed();
    }
}