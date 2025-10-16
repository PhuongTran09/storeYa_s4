package com.storeya.shop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lưu lại tên sản phẩm tại thời điểm mua
    private String productName;

    // Lưu lại giá tại thời điểm mua
    private Double price;

    private Integer quantity;

    // Liên kết tới sản phẩm gốc (tùy chọn nhưng nên có)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // Liên kết ngược lại với Payment (đơn hàng) chứa nó
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;
}