package com.example.robotdelivery.service;

import com.example.robotdelivery.mapper.RobotRepository;
import com.example.robotdelivery.pojo.Robot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RobotService {

    @Autowired
    private RobotRepository robotRepository;

    // 保存单个机器人（自动提交事务）
    @Transactional // 方法执行成功自动提交，失败自动回滚
    public Robot saveRobot(Robot robot) {
        // save()方法自带事务支持，无需手动begin/commit
        return robotRepository.save(robot);
    }

    // 批量保存机器人（事务保证要么全成功，要么全失败）
    @Transactional
    public List<Robot> batchSaveRobots(List<Robot> robots) {
        return robotRepository.saveAll(robots);
    }

    // 查询所有机器人
    public List<Robot> getAllRobots() {
        return robotRepository.findAll();
    }
}