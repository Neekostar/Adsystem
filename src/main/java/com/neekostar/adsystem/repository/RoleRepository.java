package com.neekostar.adsystem.repository;

import java.util.Optional;
import java.util.UUID;
import com.neekostar.adsystem.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findRoleByName(String name);
}
