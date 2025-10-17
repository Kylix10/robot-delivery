package com.example.robotdelivery;
import com.example.robotdelivery.mapper.IngredientMapper;
import com.example.robotdelivery.pojo.Ingredient;
import com.example.robotdelivery.pojo.Warehouse;
import com.example.robotdelivery.service.DiskSchedulerService;
import com.example.robotdelivery.service.OrderService;
import com.example.robotdelivery.vo.SchedulerResult;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
@EnableScheduling
public class RobotDeliveryApplication {
    public static void main(String[] args) {
        SpringApplication.run(RobotDeliveryApplication.class, args);
    }

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
        };
    }

}
