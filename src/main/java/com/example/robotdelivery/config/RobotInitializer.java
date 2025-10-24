package com.example.robotdelivery.config;

import com.example.robotdelivery.mapper.RobotInitFlagRepository;
import com.example.robotdelivery.mapper.RobotRepository;
import com.example.robotdelivery.pojo.Robot;
import com.example.robotdelivery.pojo.RobotInitFlag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@Component
public class RobotInitializer {

    @Autowired
    private RobotRepository robotRepository;

    @Autowired
    private RobotInitFlagRepository flagRepository; // 注入标记表Repository

    private static final int TARGET_ROBOT_COUNT = 4;
    private volatile boolean initialized = false;

    /**
     * 纯JPA初始化，无手动SQL
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<Robot> initRobots() {
        if (initialized) {
            return loadFinalRobots();
        }

        synchronized (this) {
            if (initialized) {
                return loadFinalRobots();
            }

            // 1. 获取标记表记录（加悲观锁，确保唯一操作）
            RobotInitFlag flag = flagRepository.findUniqueFlagWithLock()
                    .orElseGet(() -> {
                        // 若标记表无记录，创建唯一记录（singleRow=1）
                        RobotInitFlag newFlag = new RobotInitFlag();
                        newFlag.setSingleRow(1); // 固定值，确保唯一
                        newFlag.setInitialized(false);
                        return flagRepository.save(newFlag);
                    });

            // 2. 已初始化则直接返回
            if (Boolean.TRUE.equals(flag.getInitialized())) {
                List<Robot> robots = loadFinalRobots();
                initialized = true;
                return robots;
            }

            // 3. 强制清空旧机器人数据（消除冲突根源）
            robotRepository.deleteAll();
            System.out.println("⚠️  已清空旧机器人数据，准备重新初始化");

            // 4. 初始化4个机器人（数据库自增ID）
            List<Robot> newRobots = new ArrayList<>();
            for (int i = 1; i <= TARGET_ROBOT_COUNT; i++) {
                Robot robot = new Robot();
                robot.setRobotStatus(Robot.STATUS_FREE);
                robot.setFinishedOrders(0);
                robot.setVersion(0); // 强制初始化版本号
                newRobots.add(robot);
            }
            List<Robot> savedRobots = robotRepository.saveAll(newRobots);
            System.out.println("✅ 成功初始化4个机器人，ID：" +
                    savedRobots.stream().map(Robot::getRobotId).collect(Collectors.toList()));

            // 5. 更新标记表为已初始化
            flag.setInitialized(true);
            flag.setInitTime(LocalDateTime.now());
            flagRepository.save(flag);

            initialized = true;
            return savedRobots;
        }
    }

    // 加载最终4个机器人（原有逻辑不变）
    private List<Robot> loadFinalRobots() {
        return robotRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Robot::getRobotId))
                .limit(TARGET_ROBOT_COUNT)
                .collect(Collectors.toList());
    }

    public boolean isInitialized() {
        return initialized;
    }

    // 重置初始化状态（测试用，纯JPA操作）
    @Transactional
    public void reset() {
        robotRepository.deleteAll();
        // 重置标记表
        flagRepository.findUniqueFlagWithLock().ifPresent(flag -> {
            flag.setInitialized(false);
            flag.setInitTime(null);
            flagRepository.save(flag);
        });
        initialized = false;
        System.out.println("⚠️  已重置机器人初始化状态");
    }
}