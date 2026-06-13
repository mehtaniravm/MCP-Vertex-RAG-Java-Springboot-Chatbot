package com.company.bizapi.repository;

import com.company.bizapi.model.entity.EventRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<EventRecord, Long> {

    @Query("SELECT e FROM EventRecord e WHERE e.status = 'OPEN' AND e.occurredAt >= :since ORDER BY e.occurredAt DESC")
    List<EventRecord> findOpenEventsSince(@Param("since") LocalDateTime since);

    @Query("SELECT e FROM EventRecord e WHERE e.status = 'OPEN' AND e.severity = :severity AND e.occurredAt >= :since")
    List<EventRecord> findOpenBySeveritySince(@Param("severity") String severity, @Param("since") LocalDateTime since);

    @Query("SELECT e FROM EventRecord e WHERE e.entity.id = :entityId AND e.status = 'OPEN'")
    List<EventRecord> findOpenByEntityId(@Param("entityId") Long entityId);

    long countByStatusAndOccurredAtAfter(String status, LocalDateTime after);
}
