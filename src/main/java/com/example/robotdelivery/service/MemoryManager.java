package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Dish;
import com.example.robotdelivery.pojo.GlobalConstants; 
import com.example.robotdelivery.pojo.Partition;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class MemoryManager {
    private List<Partition> partitions;
    private final int totalWorkbenchSize; // 总工作台空间大小（固定）

    // 构造器：初始化总空间和初始分区
    public MemoryManager(int totalWorkbenchSize) {
        this.totalWorkbenchSize = totalWorkbenchSize;
        this.partitions = new ArrayList<>();
        // 初始化为一个完整的未分配分区
        this.partitions.add(new Partition(1, totalWorkbenchSize, 0));
    }

    // 辅助方法：重新设置所有分区的ID，以保证连续性
    private void reassignPartitionIds() {
        for (int i = 0; i < partitions.size(); i++) {
            partitions.get(i).setId(i + 1);
        }
    }

    // 检查是否已分配该菜肴
    private boolean isDishAllocated(int dishId) {
        for (Partition part : partitions) {
            if (part.isAllocated() && part.getDishId() == dishId) {
                return true;
            }
        }
        return false;
    }

    // 最佳适应算法分配工作台（带总空间检查）
    public boolean allocateForDish(Dish dish) {
        int dishId = dish.getDishId();
        int requiredSize = dish.getDishSpace();

        // 校验1：菜肴已分配
        if (isDishAllocated(dishId)) {
            System.out.println("错误：菜肴ID " + dishId + " 已占用工作台，无法重复分配");
            return false;
        }

        // 校验2：需求空间是否超过总空间
        if (requiredSize > totalWorkbenchSize) {
            System.out.println("错误：菜肴所需空间 " + requiredSize + " 超过总工作台空间 " + totalWorkbenchSize);
            return false;
        }

        // 寻找最佳适配分区
        int bestFitIndex = -1;
        int minExtraSpace = Integer.MAX_VALUE;

        for (int i = 0; i < partitions.size(); i++) {
            Partition part = partitions.get(i);
            if (!part.isAllocated() && part.getSize() >= requiredSize) {
                int extraSpace = part.getSize() - requiredSize;
                if (extraSpace < minExtraSpace) {
                    minExtraSpace = extraSpace;
                    bestFitIndex = i;
                }
            }
        }

        // 处理分配结果
        if (bestFitIndex != -1) {
            Partition bestFitPart = partitions.get(bestFitIndex);

            // 分割分区（如果有剩余空间）
            if (minExtraSpace > 0) {
                // 创建新的未分配分区
                Partition newPart = new Partition(
                        partitions.size() + 1, // 临时ID
                        minExtraSpace,
                        bestFitPart.getStartAddress() + requiredSize // 新的起始地址
                );
                partitions.add(bestFitIndex + 1, newPart);
                bestFitPart.setSize(requiredSize); // 调整原分区大小
                reassignPartitionIds(); // 每次添加新分区后，重新调整ID
            }

            // 标记分配状态
            bestFitPart.setAllocated(true);
            bestFitPart.setDishId(dishId);
            return true;
        } else {
            // 所有分区空间不足时的处理
            handleInsufficientSpace(dish);
            return false;
        }
    }

    // 处理空间不足的情况
    private void handleInsufficientSpace(Dish dish) {
        int requiredSize = dish.getDishSpace();
        int totalFreeSpace = calculateTotalFreeSpace();

        System.out.println("\n===== 空间不足处理 =====");
        System.out.println("当前任务需求：" + requiredSize + "，总可用空间：" + totalFreeSpace);

        // 1. 检查是否是碎片导致的空间不足（总可用空间足够但分散）
        if (totalFreeSpace >= requiredSize) {
            System.out.println("原因：可用空间分散（碎片过多），尝试整理...");
            defragmentSpace(); // 执行碎片整理
            System.out.println("碎片整理完成，重新尝试分配...");
            printMemoryStatus(); // 打印整理后的状态以便观察

            // 整理后再次尝试分配 (注意：这里必须避免递归调用，否则可能陷入无限循环
            // 但在这个上下文，重新尝试是合理的，因为分区状态已经改变)
            // 必须传入一个新的Dish对象或确保上一个操作没有影响到Dish对象，
            // 但由于Dish只是数据POJO，这里直接传入即可。

            // 重新调用 allocateForDish，它会再次执行最佳适应算法
            if (allocateForDish(dish)) {
                System.out.println("整理后分配成功！");
            } else {
                System.out.println("整理后仍无法分配，可能是整理逻辑有误或需求过大");
            }
        } else {
            // 2. 总空间确实不足
            System.out.println("原因：总可用空间不足，建议：");
            System.out.println("- 优先处理已分配任务以释放空间");
            System.out.println("- 检查任务需求是否合理（当前总空间：" + totalWorkbenchSize + "）");
        }
        System.out.println("=======================\n");
    }

    // 计算总可用空间
    private int calculateTotalFreeSpace() {
        int freeSpace = 0;
        for (Partition part : partitions) {
            if (!part.isAllocated()) {
                freeSpace += part.getSize();
            }
        }
        return freeSpace;
    }

    /**
     * 【修复】碎片整理（合并所有未分配分区，并更新已分配分区的地址）
     */
    private void defragmentSpace() {
        List<Partition> newPartitions = new ArrayList<>();
        List<Partition> allocatedParts = new ArrayList<>();

        // 1. 收集已分配分区
        for (Partition part : partitions) {
            if (part.isAllocated()) {
                allocatedParts.add(part);
            }
        }

        // 2. 按起始地址排序已分配分区
        // 保证按顺序移动
        allocatedParts.sort(Comparator.comparingInt(Partition::getStartAddress));

        // 3. 重新构建分区列表，将已分配分区紧密排列
        int currentAddress = 0;

        for (Partition allocated : allocatedParts) {
            // 插入已分配分区前的空闲空间 (如果存在，在碎片整理时应该不存在，但为了健壮性保留)
            // 实际上，正确的碎片整理是将所有已分配的都挪到前面，不留空隙。

            // 3.1. 核心修复：更新已分配分区的起始地址
            allocated.setStartAddress(currentAddress);

            // 3.2. 插入已分配分区
            newPartitions.add(allocated);

            // 3.3. 更新下一个分区的起始地址
            currentAddress += allocated.getSize();
        }

        // 4. 插入末尾的空闲分区（合并了所有碎片空间）
        if (currentAddress < totalWorkbenchSize) {
            int freeSize = totalWorkbenchSize - currentAddress;
            // 创建新的空闲分区，起始地址紧跟在最后一个已分配分区之后
            newPartitions.add(new Partition(
                    newPartitions.size() + 1, // 临时ID
                    freeSize,
                    currentAddress
            ));
        }

        this.partitions = newPartitions;
        // 5. 重新分配所有分区的 ID
        reassignPartitionIds();
    }

    // 释放菜肴占用的分区
    public boolean releaseDishPartition(int dishId) {
        for (int i = 0; i < partitions.size(); i++) {
            Partition part = partitions.get(i);
            if (part.isAllocated() && part.getDishId() == dishId) {
                part.setAllocated(false);
                part.setDishId(-1);

                // 尝试与前一个分区合并
                if (i > 0) {
                    Partition prevPart = partitions.get(i - 1);
                    if (!prevPart.isAllocated()) {
                        prevPart.setSize(prevPart.getSize() + part.getSize());
                        // 地址不变，大小增加
                        partitions.remove(i);
                        i--; // 列表元素向前移动，索引回退
                    }
                }

                // 尝试与后一个分区合并
                if (i < partitions.size() - 1) { // 检查 i 是否仍然有效
                    Partition nextPart = partitions.get(i + 1);
                    if (!nextPart.isAllocated()) {
                        // 当前分区 part 是空闲的（合并前的 prevPart 或刚刚释放的）
                        // 它的地址保持不变，大小加上 nextPart 的大小
                        part = partitions.get(i); // 获取合并后的当前分区
                        part.setSize(part.getSize() + nextPart.getSize());
                        partitions.remove(i + 1);
                    }
                }

                // 修复：每次添加或删除分区后，重新调整 ID
                reassignPartitionIds();
                return true;
            }
        }
        return false;
    }

    // 打印当前工作台状态
    public void printMemoryStatus() {
        System.out.println("\n===== 工作台状态 =====");
        System.out.println("总空间：" + totalWorkbenchSize + "，已使用：" +
                (totalWorkbenchSize - calculateTotalFreeSpace()) +
                "，可用：" + calculateTotalFreeSpace());

        int totalSizeCheck = 0;
        for (Partition part : partitions) {
            System.out.println(part);
            totalSizeCheck += part.getSize();
        }

        // 打印额外的校验信息，帮助调试
        if (totalSizeCheck != totalWorkbenchSize) {
            System.err.println("!!! 警告：分区总大小 (" + totalSizeCheck + ") 不等于总工作台大小 (" + totalWorkbenchSize + ") !!!");
        }

        System.out.println("======================\n");
    }

    // Getters
    public int getTotalWorkbenchSize() {
        return totalWorkbenchSize;
    }
}