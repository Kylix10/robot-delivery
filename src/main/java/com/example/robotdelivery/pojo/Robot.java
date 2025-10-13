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
        this.robotId = robotId;
    }
}
