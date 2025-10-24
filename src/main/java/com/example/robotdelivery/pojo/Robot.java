package com.example.robotdelivery.pojo;

import jakarta.persistence.*;

@Table(name="tb_robot")
@Entity
public class Robot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 数据库自增策略
    // 不需要手动设置，由数据库自动生成
    @Column(name="robot_id")
    private Integer robotId;

    @Column(name="robot_location")
    private Integer robotLcation;

    @Column(name="robot_status")
    private Integer robotStatus;

    // 与 Order 的多对一关联（已正确配置，无需修改）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_order_id")
    private Order currentOrder;
    // 新增属性，统计机器人完成的订单数量
    @Column(name="finished_orders")
    private Integer finishedOrders;

    // 添加 @Transient 注解，排除 Tools 类型字段的数据库映射
    @Transient // 标记为“非数据库字段”，仅内存中使用
    private Tools occupiedOven; // 占用的烤箱（内存临时状态，不存数据库）

    @Transient // 同上
    private Tools occupiedFryPan; // 占用的煎锅（内存临时状态，不存数据库）

    @Transient // 同理，工作区也为内存临时状态，需添加 @Transient
    private Memory occupiedWorkbench; // 占用的工作区

    @Transient // 同上
    private Tools occupiedFryPot; // 占用的煎锅（内存临时状态，不存数据库）




    // 常量：状态定义
    public static final Integer STATUS_FREE = 0;
    public static final Integer STATUS_BUSY = 1;


    // 乐观锁版本号（数据库需同步添加该字段）
    @Version
    @Column(name = "version")
    private Integer version;

    // 新增 getter/setter
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    // 所有 Getter/Setter 方法不变...
    public Integer getRobotStatus() { return robotStatus; }
    public void setRobotStatus(Integer robotStatus) { this.robotStatus = robotStatus; }
    public Integer getRobotLcation() { return robotLcation; }
    public void setRobotLcation(Integer robotLcation) { this.robotLcation = robotLcation; }
    public Integer getRobotId() { return robotId; }
    public void setRobotId(Integer robotId) { this.robotId = robotId; }
    public Order getCurrentOrder() { return currentOrder; }
    public void setCurrentOrder(Order currentOrder) { this.currentOrder = currentOrder; }
    public Tools getOccupiedOven() { return occupiedOven; }
    public void setOccupiedOven(Tools occupiedOven) { this.occupiedOven = occupiedOven; }
    public Tools getOccupiedFryPan() { return occupiedFryPan; }
    public void setOccupiedFryPan(Tools occupiedFryPan) { this.occupiedFryPan = occupiedFryPan; }
    public Memory getOccupiedWorkbench() { return occupiedWorkbench; }
    public void setOccupiedWorkbench(Memory occupiedWorkbench) { this.occupiedWorkbench = occupiedWorkbench; }
    // 新增炸锅的getter/setter
    public Tools getOccupiedFryPot() {
        return occupiedFryPot;
    }

    public void setOccupiedFryPot(Tools occupiedFryPot) {
        this.occupiedFryPot = occupiedFryPot;
    }

    // *新增统计机器人完成的订单数量
    public Integer getFinishedOrders() {
        return finishedOrders;
    }

    public void setFinishedOrders(Integer finishedOrders) {
        this.finishedOrders = finishedOrders;
    }

    public void incFinishedOrders(){
        this.finishedOrders++;
    }
}