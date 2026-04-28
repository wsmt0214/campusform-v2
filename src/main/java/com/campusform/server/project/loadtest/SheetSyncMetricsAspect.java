package com.campusform.server.project.loadtest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * loadtest 전용 syncSheet() 성능 측정 로거
 *
 * - 실행 시간
 * - Hibernate statistics 기반 SQL 수(대략치) 수집
 *
 * Step 3(Before 측정)에서 동일 방식으로 반복 수집하기 위한 장치
 */
@Slf4j
@Aspect
@Component
@Profile("loadtest")
@RequiredArgsConstructor
public class SheetSyncMetricsAspect {

    private final SessionFactory sessionFactory;

    @Around("execution(* com.campusform.server.project.application.service.SpreadsheetService.syncSheet(..))")
    public Object measureSyncSheet(ProceedingJoinPoint pjp) throws Throwable {
        Statistics stats = sessionFactory.getStatistics();
        stats.clear();

        long startNs = System.nanoTime();
        Object result = pjp.proceed();
        long methodEndNs = System.nanoTime();
        long methodElapsedMs = (methodEndNs - startNs) / 1_000_000;

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    long txCompletedElapsedMs = (System.nanoTime() - startNs) / 1_000_000;
                    long flushCommitMs = Math.max(0, txCompletedElapsedMs - methodElapsedMs);

                    long entityInsertCount = stats.getEntityInsertCount();
                    long entityUpdateCount = stats.getEntityUpdateCount();
                    long entityDeleteCount = stats.getEntityDeleteCount();

                    long queryExecutionCount = stats.getQueryExecutionCount();
                    long preparedStatementCount = stats.getPrepareStatementCount();

                    log.info("[sheet-sync metrics] methodElapsedMs={}, txCompletedElapsedMs={}, flushCommitMs={}, queryExecutionCount={}, preparedStatementCount={}, entityInsertCount={}, entityUpdateCount={}, entityDeleteCount={}, txStatus={}",
                            methodElapsedMs, txCompletedElapsedMs, flushCommitMs,
                            queryExecutionCount, preparedStatementCount, entityInsertCount, entityUpdateCount, entityDeleteCount,
                            status == STATUS_COMMITTED ? "COMMITTED" : "ROLLED_BACK");
                }
            });
        } else {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            log.info("[sheet-sync metrics] methodElapsedMs={}, txCompletedElapsedMs={}, flushCommitMs={}, queryExecutionCount={}, preparedStatementCount={}, entityInsertCount={}, entityUpdateCount={}, entityDeleteCount={}, txStatus={}",
                    methodElapsedMs,
                    elapsedMs,
                    Math.max(0, elapsedMs - methodElapsedMs),
                    stats.getQueryExecutionCount(),
                    stats.getPrepareStatementCount(),
                    stats.getEntityInsertCount(),
                    stats.getEntityUpdateCount(),
                    stats.getEntityDeleteCount(),
                    "NO_TX");
        }

        return result;
    }
}

