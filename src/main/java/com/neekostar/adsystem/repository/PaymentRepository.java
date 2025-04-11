package com.neekostar.adsystem.repository;

import java.util.UUID;
import com.neekostar.adsystem.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Page<Payment> findPaymentByUserUsername(String username, Pageable pageable);
}
