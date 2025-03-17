package com.example.configuration.dao.repository;

import com.example.configuration.dao.entity.APIConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface APIConfigurationRepository extends JpaRepository<APIConfiguration, Long> {
}
