package com.company.bizapi.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "entity_metrics")
public class EntityMetric {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    private EntityRecord entity;

    private String metricName;
    private BigDecimal metricValue;
    private LocalDateTime measuredAt;
    private String period;
}
