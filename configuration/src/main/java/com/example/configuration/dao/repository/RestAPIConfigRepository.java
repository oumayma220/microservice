package com.example.configuration.dao.repository;

import com.example.configuration.dao.entity.RestAPIConfiguration;
import com.example.configuration.dao.entity.Tiers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestAPIConfigRepository  extends JpaRepository<RestAPIConfiguration, Long> {
    Optional<RestAPIConfiguration> findByConfigName(String configName);
   // Optional<RestAPIConfiguration> findByTiersAndConfigName(Tiers tiers, String configName);
    List<RestAPIConfiguration> findByTiers_Id(Long tiersId);
   // List<RestAPIConfiguration> findByTiers_Nom(String nomTiers);
    Optional<RestAPIConfiguration> findById(Long id);
    Optional<RestAPIConfiguration> findByIdAndTiers_Id(Long configId, Integer tiersId);
    List<RestAPIConfiguration> findByTiers_IdAndTenantid(Long tiersId, Integer tenantid);
   // List<RestAPIConfiguration> findByTiers(Tiers tier);
  //  List<RestAPIConfiguration> findByTiers_NomAndTenantid(String nomTiers, Integer tenantid);
   // Optional<RestAPIConfiguration> findByIdAndTiers_IdAndTenantid(Long configId, Long tiersId, Integer tenantid);


    boolean existsByTiersAndConfigName(Tiers tiers, String configName);

    Optional<RestAPIConfiguration> findByIdAndTenantid(Long configId, Integer currentTenantId);

}

