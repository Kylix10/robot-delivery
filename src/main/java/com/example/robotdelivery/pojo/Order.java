package com.example.robotdelivery.pojo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Table(name="tb_order")
@Entity
public class Order {
    @Id
    private Integer orderId;
}
