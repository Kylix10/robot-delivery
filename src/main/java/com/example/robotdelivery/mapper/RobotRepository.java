package com.example.robotdelivery.mapper;

import com.example.robotdelivery.pojo.Robot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// JpaRepository<实体类, 主键类型>：提供默认的增删改查方法
@Repository
public interface RobotRepository extends JpaRepository<Robot, Integer> {
    // 无需额外写方法，默认已包含：save(更新/新增)、findById(查)、delete(删)等
}


