package com.example.robotdelivery;
import com.example.robotdelivery.mapper.IngredientMapper;
import com.example.robotdelivery.pojo.Ingredient;
import com.example.robotdelivery.pojo.Warehouse;
import com.example.robotdelivery.service.DiskSchedulerService;
import com.example.robotdelivery.service.OrderGenerate;
import com.example.robotdelivery.service.OrderService;
import com.example.robotdelivery.service.ResourceManagerThread;
import com.example.robotdelivery.vo.SchedulerResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired // 新增：注入OrderGenerate
    private OrderGenerate orderGenerate;
    /**
     * 启动初始化：若 ingredient 表为空，则从仓库写入默认食材
     */
    @Bean
    public CommandLineRunner demoRunOnce(DiskSchedulerService schedulerService, OrderService orderService, Warehouse warehouse, IngredientMapper ingredientMapper) {
        return args -> {
            // 将内存中的仓库食材持久化到数据库（仅在ingredient表为空时执行）
            try {
                if (ingredientMapper.count() == 0) {
                    System.out.println("[初始化] ingredient表为空，写入仓库默认食材...");
                    ingredientMapper.saveAll(warehouse.getAllIngredients().values());
                    System.out.println("[初始化] ingredient写入完成，总数=" + ingredientMapper.count());
                }
            } catch (Exception e) {
                System.out.println("[警告] 初始化ingredient表失败：" + e.getMessage());
            }

            // 新增：调用目标测试方法
            testOrderGenerationAndResourceScheduling();
        };
    }
    @Transactional
    //@Commit 测试时不提交数据，所以数据库中数据不会有更改。需要查看数据就将这一行加上
    public void testOrderGenerationAndResourceScheduling() throws InterruptedException {
        // 2. 主动调用OrderGenerate的generateRandomOrders()生成订单（不再等定时任务）
        System.out.println("主动调用订单生成方法...");
        orderGenerate.generateRandomOrders(); // 核心：主动生成10个订单并提交


        // 1. 启动资源管理线程（确保线程就绪）
        if (!resourceManager.isAlive()) {
            resourceManager.start();
            TimeUnit.SECONDS.sleep(1); // 等待线程初始化
        }

        //3. 等待订单处理（根据订单数量调整时间，10个订单约需10秒）
        System.out.println("等待资源管理器处理订单...");
        //TimeUnit.SECONDS.sleep(10);
        // 3. 验证资源管理器的处理状态
        System.out.println("\n===== 测试结束时的资源状态 =====");
        resourceManager.printQueue(); // 打印剩余等待队列
        System.out.println("===============================");

        // 4. 停止资源管理线程
        // resourceManager.interrupt();
    }

}
