package com.company.bizapi.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "events")
public class EventRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    private EntityRecord entity;

    private String eventType;
    private String severity;
    private String description;
    private String status;
    private LocalDateTime occurredAt;
    private LocalDateTime resolvedAt;
}
