package com.example.configuration.dao.repository;

import com.example.configuration.dao.entity.Tiers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface TiersRepository extends JpaRepository<Tiers, Long> {
    Optional<Tiers> findByNom(String nom);
}

