package com.example.robotdelivery.pojo.vo;

import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.pojo.Dish;
import java.time.LocalDateTime;

/**
 * 订单视图对象 (Order View Object)
 * 用于将 Order 实体对象转换为前端展示所需的数据结构。
 */
public class OrderVO {
    // 对应前端 table 的订单号 (ID)
    private Integer orderId;

    // 对应前端 table 的菜品 (Dish Name)
    private String dishName;

    // 对应前端 table 的创建时间（格式化后的字符串）
    private String createTime;

    // 对应前端 table 的优先级
    private Integer priority;

    // 对应前端 table 的状态（字符串，如 "pending"）
    private String status;

    // 对应前端 table 的状态显示文本（如 "待处理"）
    private String statusText;

    // 简化处理，实际项目中应关联机器人实体
    private String robotId; // 机器人ID或名称，用于展示

    // 简化处理，假设前端需要一个算法类型文本
    private String algorithmText; // 处理算法的文本描述

    // 用于操作按钮的原始状态，方便JS判断是否显示修改/取消按钮
    private Order.OrderStatus originalStatus;

    // --- 构造函数 ---
    public OrderVO() {}

    // --- Getters and Setters ---
    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }

    public String getRobotId() { return robotId; }
    public void setRobotId(String robotId) { this.robotId = robotId; }

    public String getAlgorithmText() { return algorithmText; }
    public void setAlgorithmText(String algorithmText) { this.algorithmText = algorithmText; }

    public Order.OrderStatus getOriginalStatus() { return originalStatus; }
    public void setOriginalStatus(Order.OrderStatus originalStatus) { this.originalStatus = originalStatus; }
}