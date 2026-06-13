package com.company.bizapi.repository;

import com.company.bizapi.model.entity.EntityRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface EntityRepository extends JpaRepository<EntityRecord, Long> {

    Optional<EntityRecord> findByCode(String code);

    List<EntityRecord> findByCategory(String category);

    List<EntityRecord> findByStatus(String status);

    @Query("SELECT e FROM EntityRecord e WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(e.code) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<EntityRecord> searchByNameOrCode(@Param("q") String query);
}
