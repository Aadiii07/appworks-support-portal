package com.appworks.portal.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exists specifically to fix a real, repeated point of confusion: since the
 * "postgres" Spring profile is activated per-terminal-session (via
 * -Dspring-boot.run.profiles or $env:SPRING_PROFILES_ACTIVE), it's easy to
 * accidentally run against the default in-memory H2 database without
 * noticing — data silently goes into a database that vanishes on restart,
 * while pgAdmin (looking at the real Postgres database) shows something
 * different. This endpoint reports which database is actually active, so the
 * frontend can show it directly instead of that mismatch only being
 * discoverable by manually cross-checking pgAdmin against the browser.
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @GetMapping("/info")
    public Map<String, String> getInfo() {
        boolean isH2 = datasourceUrl != null && datasourceUrl.startsWith("jdbc:h2:mem");
        return Map.of(
                "datasourceUrl", datasourceUrl,
                "isInMemory", String.valueOf(isH2),
                "warning", isH2
                        ? "Running on in-memory H2 — all data will be lost when the app stops. Not connected to PostgreSQL."
                        : "Connected to a persistent database."
        );
    }
}
