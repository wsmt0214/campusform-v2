package com.campusform.server.global.loadtest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.Getter;

/**
 * 시트 동기화 성능 측정을 위한 loadtest 전용 데이터셋 설정값
 *
 * 실제 운영 로직과 분리된 "실험 재현성" 확보 목적
 */
@Getter
@Component
@Profile("loadtest")
public class SheetSyncLoadtestProperties {

    @Value("${loadtest.sheet-sync.rows:1000}")
    private int rows;

    @Value("${loadtest.sheet-sync.extra-columns:5}")
    private int extraColumns;

    @Value("${loadtest.sheet-sync.ratio.new:0.10}")
    private double newRatio;

    @Value("${loadtest.sheet-sync.ratio.unchanged:0.70}")
    private double unchangedRatio;

    @Value("${loadtest.sheet-sync.ratio.changed:0.20}")
    private double changedRatio;

    @Value("${loadtest.sheet-sync.random-seed:42}")
    private long randomSeed;

    @Value("${loadtest.sheet-sync.scenario:mixed}")
    private String scenario;
}

