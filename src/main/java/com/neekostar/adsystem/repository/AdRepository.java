package com.neekostar.adsystem.repository;

import java.util.List;
import java.util.UUID;
import com.neekostar.adsystem.model.Ad;
import com.neekostar.adsystem.model.AdStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdRepository extends JpaRepository<Ad, UUID> {
    List<Ad> findAll(Specification<Ad> spec, Sort sort);

    Page<Ad> findAdByUserUsernameAndStatus(String username, AdStatus status, Pageable pageable);
}
