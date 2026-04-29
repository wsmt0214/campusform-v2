package com.campusform.server.global.loadtest;

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
 * loadtest 환경에서 조회 API의 기준선(응답시간 + SQL 수)을 남기는 로거
 *
 * - Hibernate statistics 기반 SQL 수(대략치) 기록
 * - Step 3(Before)와 Step 12(After)에서 동일 방식으로 비교하기 위한 장치
 */
@Slf4j
@Aspect
@Component
@Profile("loadtest")
@RequiredArgsConstructor
public class ApiQueryMetricsAspect {

    private final SessionFactory sessionFactory;

    @Around("execution(* com.campusform.server.recruiting.presentation.ApplicantController.getApplicants(..))")
    public Object measureApplicantList(ProceedingJoinPoint pjp) throws Throwable {
        return measure(pjp, "applicant-list");
    }

    @Around("execution(* com.campusform.server.project.presentation.ProjectController.getProjects(..))")
    public Object measureProjectList(ProceedingJoinPoint pjp) throws Throwable {
        return measure(pjp, "project-list");
    }

    private Object measure(ProceedingJoinPoint pjp, String tag) throws Throwable {
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

                    log.info("[api metrics] tag={}, methodElapsedMs={}, txCompletedElapsedMs={}, flushCommitMs={}, queryExecutionCount={}, preparedStatementCount={}, entityLoadCount={}, collectionLoadCount={}, txStatus={}",
                            tag,
                            methodElapsedMs,
                            txCompletedElapsedMs,
                            flushCommitMs,
                            stats.getQueryExecutionCount(),
                            stats.getPrepareStatementCount(),
                            stats.getEntityLoadCount(),
                            stats.getCollectionLoadCount(),
                            status == STATUS_COMMITTED ? "COMMITTED" : "ROLLED_BACK");
                }
            });
        } else {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            log.info("[api metrics] tag={}, methodElapsedMs={}, txCompletedElapsedMs={}, flushCommitMs={}, queryExecutionCount={}, preparedStatementCount={}, entityLoadCount={}, collectionLoadCount={}, txStatus={}",
                    tag,
                    methodElapsedMs,
                    elapsedMs,
                    Math.max(0, elapsedMs - methodElapsedMs),
                    stats.getQueryExecutionCount(),
                    stats.getPrepareStatementCount(),
                    stats.getEntityLoadCount(),
                    stats.getCollectionLoadCount(),
                    "NO_TX");
        }
        return result;
    }
}

