package com.example.robotdelivery.pojo.dto;

import com.example.robotdelivery.pojo.Robot;
import java.util.Objects;

/**
 * 机器人数据传输对象，用于向前端返回需要展示的数据
 */
public class RobotDto {
    private Integer robotId;
    private Integer robotLocation;
    private Integer robotStatus;
    // 可以添加状态的文字描述，方便前端直接展示
    private String robotStatusDesc;

    // 从实体类转换为DTO
    public static RobotDto fromEntity(Robot robot) {
        if (Objects.isNull(robot)) {
            return null;
        }

        RobotDto dto = new RobotDto();
        dto.setRobotId(robot.getRobotId());
        dto.setRobotLocation(robot.getRobotLcation());
        dto.setRobotStatus(robot.getRobot_status());
        // 设置状态描述
        dto.setRobotStatusDesc(getStatusDescription(robot.getRobot_status()));
        return dto;
    }

    // 将状态码转换为文字描述
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

    // Getters and Setters
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
