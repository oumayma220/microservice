package com.example.configuration.dao.repository;

import com.example.configuration.dao.entity.Tiers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface TiersRepository extends JpaRepository<Tiers, Long> {
    Optional<Tiers> findByNom(String nom);
    List<Tiers> findByTenantid(Integer tenantid);
    Optional<Tiers> findByEmail(String email);
    Optional<Tiers> findByNumero(String numero);
    boolean existsByEmail(String email);
    boolean existsByNumero(String numero);
    Optional<Tiers> findByIdAndTenantid(Long id, Integer tenantid);



}

