package com.company.bizapi.repository;

import com.company.bizapi.model.entity.EntityMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EntityMetricRepository extends JpaRepository<EntityMetric, Long> {

    @Query("SELECT m FROM EntityMetric m WHERE m.entity.id = :entityId AND m.metricName = :metricName ORDER BY m.measuredAt DESC")
    List<EntityMetric> findLatestByEntityAndMetric(@Param("entityId") Long entityId, @Param("metricName") String metricName);

    @Query("SELECT m FROM EntityMetric m WHERE m.entity.id = :entityId AND m.measuredAt >= :since ORDER BY m.measuredAt DESC")
    List<EntityMetric> findByEntitySince(@Param("entityId") Long entityId, @Param("since") LocalDateTime since);

    @Query("SELECT AVG(m.metricValue) FROM EntityMetric m WHERE m.entity.id = :entityId AND m.metricName = :metricName AND m.measuredAt >= :since")
    Optional<Double> avgMetricSince(@Param("entityId") Long entityId, @Param("metricName") String metricName, @Param("since") LocalDateTime since);
}
