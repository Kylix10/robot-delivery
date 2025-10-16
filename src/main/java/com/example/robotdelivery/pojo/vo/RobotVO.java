package com.example.robotdelivery.pojo.vo;

import com.example.robotdelivery.pojo.Robot;
import java.util.Objects;

/**
 * 机器人视图对象，用于向前端返回展示所需的数据（仅用于展示，不涉及数据修改）
 */
public class RobotVO {
    private Integer robotId;
    private Integer robotLocation;
    private Integer robotStatus;
    // 状态的文字描述，方便前端直接展示
    private String robotStatusDesc;

    // 从实体类转换为VO
    public static RobotVO fromEntity(Robot robot) {
        if (Objects.isNull(robot)) {
            return null;
        }

        RobotVO vo = new RobotVO();
        vo.setRobotId(robot.getRobotId());
        // 修复原代码中的拼写错误（robotLcation → robotLocation）
        vo.setRobotLocation(robot.getRobotLcation());
        // 建议实体类属性名遵循驼峰命名法（robot_status → robotStatus），此处暂时保持兼容
        vo.setRobotStatus(robot.getRobot_status());
        // 设置状态描述
        vo.setRobotStatusDesc(getStatusDescription(robot.getRobot_status()));
        return vo;
    }

    // 将状态码转换为文字描述（仅用于前端展示）
    private static String getStatusDescription(Integer status) {
        if (status == null) {
            return "未知状态";
        }

        switch (status) {
            case 0: return "空闲";
            case 1: return "忙碌";
            default: return "未知状态";
        }
    }

    // Getters（VO通常只需要getter，无需setter，避免前端意外修改；若需JSON反序列化可保留setter）
    public Integer getRobotId() {
        return robotId;
    }

    public void setRobotId(Integer robotId) {
        this.robotId = robotId;
    }

    public Integer getRobotLocation() {
        return robotLocation;
    }

    public void setRobotLocation(Integer robotLocation) {
        this.robotLocation = robotLocation;
    }

    public Integer getRobotStatus() {
        return robotStatus;
    }

    public void setRobotStatus(Integer robotStatus) {
        this.robotStatus = robotStatus;
    }

    public String getRobotStatusDesc() {
        return robotStatusDesc;
    }

    public void setRobotStatusDesc(String robotStatusDesc) {
        this.robotStatusDesc = robotStatusDesc;
    }
}