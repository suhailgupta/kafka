package com.example.analyticsservice.web;

import com.example.analyticsservice.domain.OrderEvent;
import com.example.analyticsservice.repository.OrderEventRepository;
import com.example.analyticsservice.s3.S3AnalyticsService;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final OrderEventRepository orderEventRepository;
    private final S3AnalyticsService s3AnalyticsService;

    public AnalyticsController(OrderEventRepository orderEventRepository,
                               S3AnalyticsService s3AnalyticsService) {
        this.orderEventRepository = orderEventRepository;
        this.s3AnalyticsService = s3AnalyticsService;
    }

    /**
     * Order events replicated to analytics DB via Kafka Connect JDBC sink.
     */
    @GetMapping("/orders-from-db")
    public List<OrderEventDto> getOrdersFromDb(
            @RequestParam(defaultValue = "50") int limit) {
        return orderEventRepository.findAll(PageRequest.of(0, Math.min(limit, 100)))
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Aggregations from analytics DB (same data as above, for dashboards).
     */
    @GetMapping("/orders-from-db/summary")
    public Map<String, Object> getOrdersSummaryFromDb() {
        BigDecimal totalRevenue = orderEventRepository.sumTotalAmount();
        long totalOrders = orderEventRepository.count();
        long pending = orderEventRepository.countByStatus("PENDING");
        long completed = orderEventRepository.countByStatus("COMPLETED");
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalOrders", totalOrders);
        summary.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        summary.put("pendingCount", pending);
        summary.put("completedCount", completed);
        summary.put("source", "analytics-db (Kafka Connect JDBC Sink)");
        return summary;
    }

    /**
     * Summary of order events stored in S3 (written by Kafka Connect S3 sink).
     */
    @GetMapping("/orders-from-s3/summary")
    public ResponseEntity<S3AnalyticsService.S3Summary> getOrdersSummaryFromS3() {
        return ResponseEntity.ok(s3AnalyticsService.getSummary());
    }

    /**
     * List S3 object keys (partitioned path + file names).
     */
    @GetMapping("/orders-from-s3/keys")
    public List<String> listS3Keys() {
        return s3AnalyticsService.listObjectKeys();
    }

    /**
     * Fetch content of one S3 object (e.g. a JSON file written by S3 sink).
     */
    @GetMapping("/orders-from-s3/content")
    public String getS3ObjectContent(@RequestParam String key) {
        return s3AnalyticsService.getObjectContent(key);
    }

    @GetMapping("/orders-from-db/by-customer/{customerId}")
    public List<OrderEventDto> getByCustomer(@PathVariable String customerId,
                                             @RequestParam(defaultValue = "20") int limit) {
        return orderEventRepository.findByCustomerIdOrderByCreatedAtDesc(
                        customerId, PageRequest.of(0, Math.min(limit, 50)))
                .stream()
                .map(this::toDto)
                .toList();
    }

    private OrderEventDto toDto(OrderEvent e) {
        return new OrderEventDto(
                e.getId(),
                e.getOrderNumber(),
                e.getCustomerId(),
                e.getCustomerEmail(),
                e.getTotalAmount(),
                e.getCurrency(),
                e.getStatus(),
                e.getCreatedAt() != null ? Instant.parse(e.getCreatedAt()) : null
        );
    }

    public record OrderEventDto(
            Long id,
            String orderNumber,
            String customerId,
            String customerEmail,
            java.math.BigDecimal totalAmount,
            String currency,
            String status,
            java.time.Instant createdAt
    ) {}
}
