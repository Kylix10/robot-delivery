package com.example.robotdelivery.config;

import com.example.robotdelivery.mapper.RobotInitFlagRepository;
import com.example.robotdelivery.mapper.RobotRepository;
import com.example.robotdelivery.pojo.Robot;
import com.example.robotdelivery.pojo.RobotInitFlag;
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
    private RobotInitFlagRepository flagRepository;

    private static final int TARGET_ROBOT_COUNT = 4;
    private volatile boolean initialized = false;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<Robot> initRobots() {
        System.out.println("[RobotInitializer] 开始执行机器人初始化");
        if (initialized) {
            System.out.println("[RobotInitializer] 已完成初始化，直接返回机器人列表");
            return loadFinalRobots();
        }

        synchronized (this) {
            System.out.println("[RobotInitializer] 进入同步代码块，检查初始化状态");
            if (initialized) {
                System.out.println("[RobotInitializer] 同步块内已初始化，直接返回机器人列表");
                return loadFinalRobots();
            }

            RobotInitFlag flag = null;
            try {
                // 1. 查询或创建标记表记录
                System.out.println("[RobotInitializer] 开始查询标记表（singleRow=1）");
                flag = flagRepository.findUniqueFlagWithLock()
                        .orElseGet(() -> {
                            System.out.println("[RobotInitializer] 标记表无记录，创建新标记（singleRow=1）");
                            RobotInitFlag newFlag = new RobotInitFlag();
                            newFlag.setSingleRow(1);
                            newFlag.setInitialized(false);
                            RobotInitFlag savedFlag = flagRepository.save(newFlag);
                            System.out.println("[RobotInitializer] 新标记创建成功，ID：" + savedFlag.getId());
                            return savedFlag;
                        });
                System.out.println("[RobotInitializer] 标记表查询成功，标记状态：" + flag.getInitialized());

            } catch (Exception e) {
                System.err.println("[RobotInitializer] 标记表查询/创建失败，原因：" + e.getMessage());
                e.printStackTrace(); // 打印完整堆栈，定位错误
                System.out.println("[RobotInitializer] 尝试直接创建标记表记录（假设表不存在）");
                flag = new RobotInitFlag();
                flag.setSingleRow(1);
                flag.setInitialized(false);
                flag = flagRepository.save(flag);
                System.out.println("[RobotInitializer] 标记表记录创建成功，ID：" + flag.getId());
            }

            // 2. 已初始化：重置机器人状态
            if (Boolean.TRUE.equals(flag.getInitialized())) {
                System.out.println("[RobotInitializer] 标记为已初始化，开始重置机器人状态");
                List<Robot> existingRobots = robotRepository.findAll();
                System.out.println("[RobotInitializer] 数据库中现有机器人数量：" + existingRobots.size());

                // 补充不足的机器人
                if (existingRobots.size() < TARGET_ROBOT_COUNT) {
                    int need = TARGET_ROBOT_COUNT - existingRobots.size();
                    System.out.println("[RobotInitializer] 机器人数量不足，需补充：" + need + "个");
                    for (int i = 0; i < need; i++) {
                        Robot robot = new Robot();
                        robot.setRobotStatus(Robot.STATUS_FREE); // 明确设置为空闲（0）
                        robot.setFinishedOrders(0);
                        robot.setCurrentOrder(null);
                        robot.setVersion(0);
                        existingRobots.add(robot);
                    }
                }

                // 重置状态（保留完成订单数）
                existingRobots.forEach(robot -> {
                    robot.setRobotStatus(Robot.STATUS_FREE);
                    robot.setCurrentOrder(null);
                    robot.setVersion(0);
                    System.out.println("[RobotInitializer] 重置机器人ID：" + robot.getRobotId() + "，状态：" + robot.getRobotStatus());
                });

                List<Robot> savedRobots = robotRepository.saveAll(existingRobots);
                System.out.println("[RobotInitializer] 机器人状态重置完成，共" + savedRobots.size() + "个");

                List<Robot> finalRobots = loadFinalRobots();
                initialized = true;
                System.out.println("[RobotInitializer] 初始化完成，返回前4个机器人：" + finalRobots.stream().map(Robot::getRobotId).collect(Collectors.toList()));
                return finalRobots;
            }

            // 3. 未初始化：创建4个新机器人
            System.out.println("[RobotInitializer] 标记为未初始化，开始创建新机器人");
            try {
                long count = robotRepository.count();
                System.out.println("[RobotInitializer] 数据库中现有机器人数量：" + count);
                if (count > 0) {
                    robotRepository.deleteAll();
                    System.out.println("[RobotInitializer] 已清空旧机器人数据");
                }
            } catch (Exception e) {
                System.err.println("[RobotInitializer] 清空旧机器人失败（可能表不存在），原因：" + e.getMessage());
                e.printStackTrace();
            }

            // 创建4个新机器人
            List<Robot> newRobots = new ArrayList<>();
            for (int i = 0; i < TARGET_ROBOT_COUNT; i++) {
                Robot robot = new Robot();
                robot.setRobotStatus(Robot.STATUS_FREE); // 明确赋值为0（空闲）
                robot.setFinishedOrders(0);
                robot.setCurrentOrder(null);
                robot.setVersion(0);
                newRobots.add(robot);
                System.out.println("[RobotInitializer] 创建新机器人（未保存）：状态=" + robot.getRobotStatus() + "，完成订单数=" + robot.getFinishedOrders());
            }

            List<Robot> savedRobots = robotRepository.saveAll(newRobots);
            System.out.println("[RobotInitializer] 4个新机器人创建成功，ID：" +
                    savedRobots.stream().map(Robot::getRobotId).collect(Collectors.toList()));

            // 4. 更新标记为已初始化
            flag.setInitialized(true);
            flag.setInitTime(LocalDateTime.now());
            flagRepository.save(flag);
            System.out.println("[RobotInitializer] 标记表更新为已初始化，时间：" + flag.getInitTime());

            initialized = true;
            System.out.println("[RobotInitializer] 初始化完成，返回新创建的机器人列表");
            return savedRobots;
        }
    }

    // 加载并排序前4个机器人（带日志）
    private List<Robot> loadFinalRobots() {
        System.out.println("[RobotInitializer] 开始加载前4个机器人");
        List<Robot> allRobots = robotRepository.findAll();
        System.out.println("[RobotInitializer] 数据库中所有机器人数量：" + allRobots.size());

        List<Robot> finalRobots = allRobots.stream()
                .sorted(Comparator.comparingInt(Robot::getRobotId))
                .limit(TARGET_ROBOT_COUNT)
                .collect(Collectors.toList());

        System.out.println("[RobotInitializer] 加载前4个机器人完成，ID：" +
                finalRobots.stream().map(Robot::getRobotId).collect(Collectors.toList()));
        return finalRobots;
    }

    public boolean isInitialized() {
        return initialized;
    }

    // 重置初始化（带日志）
    @Transactional
    public void reset() {
        System.out.println("[RobotInitializer] 开始重置初始化状态");
        try {
            robotRepository.deleteAll();
            System.out.println("[RobotInitializer] 已删除所有机器人数据");
        } catch (Exception e) {
            System.err.println("[RobotInitializer] 重置时删除机器人失败，原因：" + e.getMessage());
            e.printStackTrace();
        }

        flagRepository.findUniqueFlagWithLock().ifPresent(flag -> {
            flag.setInitialized(false);
            flag.setInitTime(null);
            flagRepository.save(flag);
            System.out.println("[RobotInitializer] 已重置标记表状态");
        });

        initialized = false;
        System.out.println("[RobotInitializer] 初始化状态重置完成");
    }
}