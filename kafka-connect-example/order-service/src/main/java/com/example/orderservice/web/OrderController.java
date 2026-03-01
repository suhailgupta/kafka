package com.example.orderservice.web;

import com.example.orderservice.domain.Order;
import com.example.orderservice.repository.OrderRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setCustomerId(request.customerId());
        order.setCustomerEmail(request.customerEmail());
        order.setTotalAmount(request.totalAmount());
        order.setCurrency(request.currency() != null ? request.currency() : "USD");
        order.setStatus("PENDING");
        order = orderRepository.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(o -> ResponseEntity.ok(toResponse(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<OrderResponse> listOrders() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        return orderRepository.findById(id)
                .map(order -> {
                    order.setStatus(request.status());
                    order = orderRepository.save(order);
                    return ResponseEntity.ok(toResponse(order));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private OrderResponse toResponse(Order o) {
        return new OrderResponse(
                o.getId(),
                o.getOrderNumber(),
                o.getCustomerId(),
                o.getCustomerEmail(),
                o.getTotalAmount(),
                o.getCurrency(),
                o.getStatus(),
                o.getCreatedAt(),
                o.getUpdatedAt()
        );
    }

    public record CreateOrderRequest(
            @NotBlank String customerId,
            String customerEmail,
            @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
            String currency
    ) {}

    public record UpdateStatusRequest(@NotBlank String status) {}

    public record OrderResponse(
            Long id,
            String orderNumber,
            String customerId,
            String customerEmail,
            BigDecimal totalAmount,
            String currency,
            String status,
            java.time.Instant createdAt,
            java.time.Instant updatedAt
    ) {}
}
