package com.example.robotdelivery;


import com.example.robotdelivery.pojo.Dish;
import com.example.robotdelivery.pojo.Memory;
import com.example.robotdelivery.service.MemoryManager;

import java.util.Scanner;

public class MemoryTest {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 初始化总工作台空间
        Memory memory=new Memory();
        MemoryManager memoryManager = new MemoryManager(memory);
        memoryManager.printMemoryStatus();

        // 业务交互循环
        while (true) {
            System.out.println("请选择操作：1-分配工作台  2-释放工作台  3-查看状态  4-退出");
            int choice = scanner.nextInt();

            if (choice == 1) {
                System.out.println("请输入菜肴ID（数据库主键）：");
                int dishId = scanner.nextInt();
                System.out.println("请输入该菜肴所需工作台大小：");
                int dishSpace = scanner.nextInt();

                Dish dish = new Dish();
                dish.setDishId(dishId);
                dish.setDishSpace(dishSpace);

                memoryManager.allocateForDish(dish);
            } else if (choice == 2) {
                System.out.println("请输入要释放的菜肴ID：");
                int dishId = scanner.nextInt();
                boolean isReleased = memoryManager.releaseDishPartition(dishId);
                if (isReleased) {
                    System.out.println("释放成功！");
                } else {
                    System.out.println("未找到该菜肴的分配记录！");
                }
                memoryManager.printMemoryStatus();
            } else if (choice == 3) {
                memoryManager.printMemoryStatus();
            } else if (choice == 4) {
                System.out.println("程序退出。");
                break;
            } else {
                System.out.println("无效选择，请重新输入！");
            }
        }
        scanner.close();
    }
}
