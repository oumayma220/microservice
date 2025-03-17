package com.example.configuration.dao.repository;

import com.example.configuration.configuration.RestAPIConfig;
import com.example.configuration.dao.entity.APIMethod;
import com.example.configuration.dao.entity.RestAPIConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface APIMethodRepository extends JpaRepository<APIMethod, Long> {
    Optional<APIMethod> findByRestAPIConfigAndEndpoint(RestAPIConfiguration restAPIConfig, String endpoint);
    Optional<APIMethod> findByHttpMethodAndEndpoint(String httpMethod, String endpoint);


    Optional<APIMethod> findByRestAPIConfig(RestAPIConfiguration restAPIConfig);
}
