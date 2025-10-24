package com.example.robotdelivery.pojo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "robot_init_flag") // 表名
public class RobotInitFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isInitialized = false; // 是否已初始化

    private LocalDateTime initTime; // 初始化时间

    // 仅允许一条记录（通过唯一约束实现，替代数据库CHECK）
    @Column(unique = true, nullable = false)
    private Integer singleRow = 1; // 固定值1，确保唯一

    // getter和setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Boolean getInitialized() { return isInitialized; }
    public void setInitialized(Boolean initialized) { isInitialized = initialized; }

    public LocalDateTime getInitTime() { return initTime; }
    public void setInitTime(LocalDateTime initTime) { this.initTime = initTime; }

    public Integer getSingleRow() { return singleRow; }
    public void setSingleRow(Integer singleRow) { this.singleRow = singleRow; }
}