package com.company.bizapi.model.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class SlaStatusDto {
    private String entityCode;
    private String entityName;
    private double slaThresholdPercent;   // target e.g. 99.9
    private double currentUptimePercent;  // actual measured
    private String complianceStatus;      // COMPLIANT / AT_RISK / BREACHED
    private double breachRiskScore;       // 0-100 computed
    private List<String> recommendations;
}
