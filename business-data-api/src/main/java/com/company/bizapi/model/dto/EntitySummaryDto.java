package com.company.bizapi.model.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class EntitySummaryDto {
    private String entityCode;
    private String name;
    private String category;
    private String status;
    // Computed by business logic
    private int    healthScore;        // 0-100
    private String healthLabel;        // HEALTHY / DEGRADED / CRITICAL
    private double uptimePercent;
    private double avgResponseTimeMs;
    private int    openEventCount;
    private int    criticalEventCount;
    private List<String> activeAlerts;
}
