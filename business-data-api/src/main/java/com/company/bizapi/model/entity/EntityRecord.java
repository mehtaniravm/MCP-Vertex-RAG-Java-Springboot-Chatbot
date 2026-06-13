package com.company.bizapi.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "entities")
public class EntityRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String category;
    private String status;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
