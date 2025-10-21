package com.example.robotdelivery.pojo.vo;

import com.example.robotdelivery.pojo.Tools;

// 用于前端展示烹饪工具状态
public class ToolVo {
    private Integer toolId; // 器具ID
    private String toolType; // 类型 (使用字符串表示枚举名称)
    private String statusText; // 状态文本（如：空闲/占用）
    private String occupiedByRobot; // 占用机器人（如：Robot-1, 或 '无'）

    // 静态工厂方法：将 Tools 实体转换为 ToolVo
    public static ToolVo fromTools(Tools tool) {
        ToolVo vo = new ToolVo();
        vo.setToolId(tool.getToolId());

        // 1. 设置工具类型（枚举转字符串）
        vo.setToolType(tool.getToolType().name()); // OVEN, FRY_PAN, FRY_POT

        // 2. 设置状态文本
        Integer status = tool.getToolStatus();
        if (status.equals(Tools.STATUS_FREE)) {
            vo.setStatusText("空闲");
        } else {
            vo.setStatusText("占用中");
        }

        // 3. 设置占用机器人信息
        Integer robotId = tool.getToolStatus(); // 注意：您的 toolStatus 字段存储了占用它的 robotId
        if (robotId.equals(Tools.STATUS_FREE)) {
            vo.setOccupiedByRobot("无");
        } else {
            // 假设机器人ID即为 toolStatus 的值
            vo.setOccupiedByRobot("Robot-" + robotId);
        }


        return vo;
    }

    // --- Getters and Setters ---
    public Integer getToolId() {
        return toolId;
    }

    public void setToolId(Integer toolId) {
        this.toolId = toolId;
    }

    public String getToolType() {
        return toolType;
    }

    public void setToolType(String toolType) {
        this.toolType = toolType;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getOccupiedByRobot() {
        return occupiedByRobot;
    }

    public void setOccupiedByRobot(String occupiedByRobot) {
        this.occupiedByRobot = occupiedByRobot;
    }
}