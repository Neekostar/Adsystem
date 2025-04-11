package com.neekostar.adsystem.repository;

import java.util.Optional;
import java.util.UUID;
import com.neekostar.adsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findUserByUsername(String username);

    Optional<User> findUserByUsernameOrEmail(String username, String email);

    boolean existsUserByUsername(String username);

    boolean existsUserByEmail(String email);
}
