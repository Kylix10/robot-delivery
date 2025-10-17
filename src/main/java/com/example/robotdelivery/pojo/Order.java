package com.example.robotdelivery.pojo;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 关键添加：@Entity 注解，声明为 JPA 实体
@Entity
@Table(name="tb_order") // 确保表名与数据库中一致（已有此注解，无需修改）

public class Order {

    // 1. 文档需求：订单状态枚举（映射到数据库，避免魔法值）
    public enum OrderStatus {
        PENDING,    // 0=待处理（文档“订单等待调度”状态）
        PROCESSING, // 1=处理中（文档“机器人正在处理”状态）
        COMPLETED   // 2=已完成（文档“订单已完成”状态）
    }

    // 2. JPA主键与基础字段
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="order_id", nullable = false)
    private Integer orderId;

    @Column(name="order_name")
    private String orderName;
    // 定义外键关联
    // 4. 核心关联：订单-菜品（多对一，一个订单对应一道菜，文档“订单定制菜品”需求）
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // 懒加载，避免冗余查询
    @JoinColumn(name = "dish_id", referencedColumnName = "dish_id") // 外键：关联dish表的dish_id
    private Dish dish; // 订单对应的菜品（直接关联菜品的资源需求，便于后续资源申请）

    @Column(name="priority")
    private Integer priority;
    // 创建时间，指定列名，设置为不可为空
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    // 完成时间，指定列名，可为空（因为订单可能还未完成）
    @Column(name = "complete_time")
    private LocalDateTime completeTime;

    // 3. 新增：文档需求字段（映射到数据库）
    @Column(name = "order_status", nullable = false)
    @Enumerated(EnumType.STRING) // 枚举以字符串存储，便于理解
    private OrderStatus orderStatus; // 订单状态（贴合文档“订单状态跟踪”需求）

    // Getter 和 Setter 方法
    public Integer getOrderId() {
        return orderId;
    }

    public void updatePriority(int x)
    {
        this.priority=priority+x;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getOrderName() {
        return orderName;
    }

    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    public Dish getDish() {
        return dish;
    }

    public void setDish(Dish dish) {
        this.dish = dish;
    }


    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(LocalDateTime completeTime) {
        this.completeTime = completeTime;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", orderName='" + orderName + '\'' +
                '}';
    }
}