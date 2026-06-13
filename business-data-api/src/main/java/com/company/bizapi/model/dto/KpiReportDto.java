package com.company.bizapi.model.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data @Builder
public class KpiReportDto {
    private String period;
    private String category;
    private long   totalEntities;
    private double avgUptimePercent;
    private double avgResponseTimeMs;
    private long   totalOpenEvents;
    private long   criticalEventCount;
    private double slaCompliancePercent;
    private Map<String, Long> eventsByType;
    private String overallHealthLabel;
}
