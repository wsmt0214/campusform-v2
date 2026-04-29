package com.campusform.server.recruiting.loadtest;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@Profile("loadtest-cli")
@RequiredArgsConstructor
public class LoadtestSeedRunner implements CommandLineRunner {

    private final LoadtestSeedService loadtestSeedService;

    @Override
    public void run(String... args) {
        loadtestSeedService.seedIfNeeded();
    }
}

