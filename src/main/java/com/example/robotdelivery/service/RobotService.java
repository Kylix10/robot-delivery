package com.example.robotdelivery.service;

import com.example.robotdelivery.mapper.RobotRepository;
import com.example.robotdelivery.pojo.Robot;
import com.example.robotdelivery.pojo.dto.RobotDto;
import com.example.robotdelivery.pojo.Order; // 新增：导入正确的Order类
import com.example.robotdelivery.mapper.RobotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/**
 * 机器人服务实现类
 */
@Service
public class RobotService implements IRobotService {
    @Autowired
    private final RobotRepository robotRepository;

    @Autowired
    public RobotService(RobotRepository robotRepository) {
        this.robotRepository = robotRepository;
    }

    @Override
    public List<RobotDto> getAllRobots() {
        // 查询所有机器人，并转换为DTO
        return robotRepository.findAll().stream()
                .map(RobotDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public RobotDto getRobotById(Integer id) {
        // 根据ID查询机器人，并转换为DTO
        return robotRepository.findById(id)
                .map(RobotDto::fromEntity)
                .orElse(null);
    }
   
    

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

    @Transactional // 默认为 REQUIRED：若当前无事务则新建，有则加入
    public Robot updateRobotToBusy(Integer robotId, Order order) {
        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> new RuntimeException("机器人不存在：" + robotId));
        robot.setRobotStatus(Robot.STATUS_BUSY);
        robot.setCurrentOrder(order);
        return robotRepository.save(robot); // 方法结束后自动提交事务
    }

    /**
     * 独立事务：更新机器人为空闲状态并解除订单关联
     */
    @Transactional
    public Robot updateRobotToFree(Integer robotId) {
        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> new RuntimeException("机器人不存在：" + robotId));
        robot.setRobotStatus(Robot.STATUS_FREE);
        robot.setCurrentOrder(null);
        return robotRepository.save(robot); // 方法结束后自动提交事务
    }



//    // 查询所有机器人
//    public List<Robot> getAllRobots() {
//        return robotRepository.findAll();
//    }

    // 注意：这里不提供公开的更新和删除方法
    // 后端内部修改可以通过其他方式实现，如：
    // 1. 提供内部使用的方法（不加@Transactional或限制访问）
    // 2. 通过其他服务类进行状态更新
    // 3. 事件驱动的方式更新状态
}

