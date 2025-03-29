package com.example.configuration.dao.repository;

import com.example.configuration.dao.entity.APIMethod;
import com.example.configuration.dao.entity.FieldMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FieldMappingRepository extends JpaRepository<FieldMapping, Long> {
    Optional<FieldMapping> findByApiMethodAndSourceAndTarget(APIMethod apiMethod, String source, String target);
    void deleteByApiMethod_Id(Long apiMethodId);

    List<FieldMapping> findByApiMethod(APIMethod apiMethod);
}
