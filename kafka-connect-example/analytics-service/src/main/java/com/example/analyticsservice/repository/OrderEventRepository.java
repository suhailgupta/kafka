package com.example.analyticsservice.repository;

import com.example.analyticsservice.domain.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    List<OrderEvent> findByCustomerIdOrderByCreatedAtDesc(String customerId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT SUM(o.totalAmount) FROM OrderEvent o")
    java.math.BigDecimal sumTotalAmount();

    long countByStatus(String status);
}
