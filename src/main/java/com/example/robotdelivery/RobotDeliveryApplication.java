package com.example.robotdelivery;

import com.example.robotdelivery.mapper.IngredientMapper;
import com.example.robotdelivery.mapper.RobotRepository;
import com.example.robotdelivery.pojo.Ingredient;
import com.example.robotdelivery.pojo.Robot;
import com.example.robotdelivery.pojo.Warehouse;
import com.example.robotdelivery.service.DiskSchedulerService;
import com.example.robotdelivery.service.OrderGenerate;
import com.example.robotdelivery.service.OrderService;
import com.example.robotdelivery.service.ResourceManagerThread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableScheduling
public class RobotDeliveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(RobotDeliveryApplication.class, args);
    }

    @Autowired
    private ResourceManagerThread resourceManager;

    @Autowired
    private OrderGenerate orderGenerate;
    @Autowired
    private RobotRepository robotRepository;

    //@BeforeEach
    public void resetRobotStatus() {
        List<Robot> robots = robotRepository.findAll();
        for (Robot robot : robots) {
            robot.setRobotStatus(Robot.STATUS_FREE);
            robot.setCurrentOrder(null);
        }
        robotRepository.saveAll(robots);
        System.out.println("测试前重置所有机器人为空闲状态");
    }

    /**
     * 启动初始化：执行食材初始化、启动资源管理线程、生成初始订单
     */
    @Bean
    public CommandLineRunner initRunner(
            DiskSchedulerService schedulerService,
            OrderService orderService,
            Warehouse warehouse,
            IngredientMapper ingredientMapper
    ) {
        return args -> {
            // 1. 初始化食材（仅表为空时执行）
            try {
                if (ingredientMapper.count() == 0) {
                    System.out.println("[初始化] ingredient表为空，写入仓库默认食材...");
                    ingredientMapper.saveAll(warehouse.getAllIngredients().values());
                    System.out.println("[初始化] ingredient写入完成，总数=" + ingredientMapper.count());
                }
            } catch (Exception e) {
                System.out.println("[警告] 初始化ingredient表失败：" + e.getMessage());
            }

            // 2. 启动资源管理线程（确保线程处于运行状态）
            if (resourceManager.getState() == Thread.State.NEW) {
                System.out.println("[启动流程] 启动资源管理线程...");
                resourceManager.start();
                TimeUnit.SECONDS.sleep(2); // 等待线程完成初始化（加载机器人、工具等资源）
            } else {
                System.out.println("[启动流程] 资源管理线程已启动，状态：" + resourceManager.getState());
            }
            resetRobotStatus();

            // 3. 生成初始订单并提交给资源管理器
            System.out.println("[启动流程] 生成初始订单...");
            orderGenerate.generateRandomOrders(); // 生成订单并提交到等待队列

            // 4. 打印启动完成信息（不阻塞主线程）
            System.out.println("\n===== 应用启动完成 =====");
            System.out.println("资源管理线程状态：" + resourceManager.getState());
            System.out.println("初始订单已提交，等待处理...");
            System.out.println("========================\n");
        };
    }
}