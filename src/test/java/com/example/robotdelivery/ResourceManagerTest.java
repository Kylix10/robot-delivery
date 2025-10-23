package com.example.robotdelivery;

import com.example.robotdelivery.service.OrderGenerate; // 导入OrderGenerate
import com.example.robotdelivery.service.ResourceManagerThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import com.example.robotdelivery.vo.OrderScheduleResult;
import org.springframework.test.annotation.Commit;

import com.example.robotdelivery.mapper.RobotRepository;
import com.example.robotdelivery.pojo.*;
import java.util.List;

import java.util.concurrent.TimeUnit;

@SpringBootTest
public class ResourceManagerTest {

    @Autowired
    private RobotRepository robotRepository;

    // 测试方法执行前，重置所有机器人为空闲
    @BeforeEach
    @Commit // 关键：强制提交事务，确保修改写入数据库
    public void resetRobotStatus() {
        List<Robot> robots = robotRepository.findAll();
        for (Robot robot : robots) {
            robot.setRobotStatus(Robot.STATUS_FREE);
            robot.setCurrentOrder(null);
        }
        robotRepository.saveAll(robots);
        System.out.println("测试前重置所有机器人为空闲状态");
    }

    @Autowired
    private ResourceManagerThread resourceManager;

    @Autowired // 新增：注入OrderGenerate
    private OrderGenerate orderGenerate;

    @Test
    @Transactional
    @Commit //测试时不提交数据，所以数据库中数据不会有更改。需要查看数据就将这一行加上
    public void testOrderGenerationAndResourceScheduling() throws InterruptedException {


        // 2. 主动调用OrderGenerate的generateRandomOrders()生成订单（不再等定时任务）
        System.out.println("主动调用订单生成方法...");
        orderGenerate.generateRandomOrders(); // 核心：主动生成10个订单并提交


        // 只对“新建状态”的线程调用 start()
        if (resourceManager.getState() == Thread.State.NEW) {
            resourceManager.start();
            TimeUnit.SECONDS.sleep(1); // 等待线程初始化
        } else {
            System.out.println("资源管理线程已启动，状态：" + resourceManager.getState());
        }


        //3. 等待订单处理（根据订单数量调整时间，10个订单约需10秒）
        System.out.println("等待资源管理器处理订单...");
        TimeUnit.SECONDS.sleep(30);
        // 3. 验证资源管理器的处理状态
        System.out.println("\n===== 测试结束时的资源状态 =====");
        resourceManager.printQueue(); // 打印剩余等待队列
        System.out.println("===============================");

        // 4. 停止资源管理线程
        resourceManager.interrupt();
    }
}