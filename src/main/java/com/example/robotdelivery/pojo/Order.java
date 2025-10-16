package com.example.robotdelivery.pojo;

import jakarta.persistence.*;

import java.time.LocalDateTime;


@Table(name="tb_order")
@Entity
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="order_id")
    private Integer orderId;
    @Column(name="order_name")
    private String orderName;
    // 定义外键关联
    @ManyToOne
    @JoinColumn(name = "dish_id", referencedColumnName = "dish_id")
    private Dish dish; // 这里使用 Dish 对象来关联，而不是直接用 String dishId

    @Column(name="priority")
    private Integer priority;
    // 创建时间，指定列名，设置为不可为空
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    // 完成时间，指定列名，可为空（因为订单可能还未完成）
    @Column(name = "complete_time")
    private LocalDateTime completeTime;

    public String getOrderName() {
        return orderName;
    }

    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", orderName='" + orderName + '\'' +
                ", dish=" + dish +
                ", priority=" + priority +
                ", createTime=" + createTime +
                '}';
    }

    public void setCreateTime(LocalDateTime now) {
        this.createTime=now;
    }
}
