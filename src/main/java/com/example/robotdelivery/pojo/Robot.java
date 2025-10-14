package com.example.robotdelivery.pojo;

import jakarta.persistence.*;

@Table(name="tb_robot")
@Entity
public class Robot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="robot_id")
    private Integer robotId;

    @Column(name="robot_location")
    private Integer robotLcation;

    @Column(name="robot_status")
    private Integer robot_status;

    //补充 robot_status 的 getter 和 setter（必须有）
    public Integer getRobot_status() {
        return robot_status;
    }

    public void setRobot_status(Integer robot_status) {
        this.robot_status = robot_status;
    }

    // 已有的 getter/setter（保持不变）
    public Integer getRobotLcation() {
        return robotLcation;
    }

    public void setRobotLcation(Integer robotLcation) {
        this.robotLcation = robotLcation;
    }

    public Integer getRobotId() {
        return robotId;
    }

    public void setRobotId(Integer robotId) {
        this.robotId = robotId;}
}
