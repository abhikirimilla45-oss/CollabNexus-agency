package com.collabnexus.common.health;

import com.collabnexus.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;
    private final String applicationName;
    private final String version;
    private final String phase;

    public HealthController(DataSource dataSource,
                            @Value("${info.app.name:CollabNexus}") String applicationName,
                            @Value("${info.app.version:unknown}") String version,
                            @Value("${info.app.phase:unknown}") String phase) {
        this.dataSource = dataSource;
        this.applicationName = applicationName;
        this.version = version;
        this.phase = phase;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthData>> health() {
        String database = databaseStatus();
        String status = "UP".equals(database) ? "UP" : "DEGRADED";
        HealthData data = new HealthData(status, applicationName, version, phase, database, Instant.now());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    private String databaseStatus() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception ex) {
            return "DOWN";
        }
    }

    public record HealthData(
            String status,
            String application,
            String version,
            String phase,
            String database,
            Instant timestamp
    ) {
    }
}
