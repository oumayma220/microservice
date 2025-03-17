package com.example.demo.dao.repository;

import com.example.demo.dao.entity.Role;
import com.example.demo.dao.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    List<User> findByRolesContaining(Role role);
    Optional<User> findById(Integer id);
    Optional<User> findByResetCode(String resetCode);
    List<User> findByTenantid(Integer tenantid);
}
