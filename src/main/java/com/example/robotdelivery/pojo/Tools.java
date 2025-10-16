package com.example.robotdelivery.pojo;

/**
 * 烹饪器具资源（烤箱、煎锅）
 * 对应文档“资源申请与释放”中的“烹饪器具”，绑定机器人（核心单元）
 */
public class Tools {
    // 工具类型枚举（文档“资源种类动态变化”需求）
    public enum ToolType {
        OVEN, // 烤箱
        FRY_PAN // 煎锅
    }

    private Integer toolId;
    private ToolType toolType; // 工具类型
    private Integer toolStatus; // 0=空闲，1=占用（文档“资源状态管理”）
    private Integer occupiedByRobotId; // 绑定机器人（核心单元，非订单）

    // 常量：状态定义
    public static final Integer STATUS_FREE = 0;
    public static final Integer STATUS_OCCUPIED = 1;



    // Getter 和 Setter 方法
    public Integer getToolId() {
        return toolId;
    }

    public void setToolId(Integer toolId) {
        this.toolId = toolId;
    }

    public ToolType getToolType() {
        return toolType;
    }

    public void setToolType(ToolType toolType) {
        this.toolType = toolType;
    }

    public Integer getToolStatus() {
        return toolStatus;
    }

    public void setToolStatus(Integer toolStatus) {
        this.toolStatus = toolStatus;
    }

    public Integer getOccupiedByRobotId() {
        return occupiedByRobotId;
    }

    public void setOccupiedByRobotId(Integer occupiedByRobotId) {
        this.occupiedByRobotId = occupiedByRobotId;
    }
}