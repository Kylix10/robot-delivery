package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.dto.RobotDto;
import java.util.List;

/**
 * 机器人服务接口，只提供查询相关方法
 */
public interface IRobotService {
    /**
     * 获取所有机器人信息
     */
    List<RobotDto> getAllRobots();

    /**
     * 根据ID获取机器人信息
     */
    RobotDto getRobotById(Integer id);
}
