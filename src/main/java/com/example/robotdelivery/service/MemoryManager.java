package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Dish;
import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.pojo.Partition;
import com.example.robotdelivery.pojo.Memory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

@Service
public class MemoryManager {
    private final Memory memory;

    public MemoryManager(Memory memory) {
        this.memory = memory;
        // 初始化分区
        if (memory.getPartitions().isEmpty()) {
            memory.getPartitions().add(new Partition(1, memory.getTotalSpace(), 0));
        }
    }

    /*private void 重新设置所有分区的ID，保证连续性
     */
    private void reassignPartitionIds(List<Partition> partitions) {
        for (int i = 0; i < partitions.size(); i++) {
            partitions.get(i).setId(i + 1);
        }
    }

    /**
     * 检查订单是否已分配该订单
     */
    private boolean isOrderAllocated(int orderId) {
        for (Partition part : memory.getPartitions()) {
            if (part.isAllocated() && part.getOrderId() == orderId) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算总可用空间，并更新Memory的usedSpace
     */
    private int calculateTotalFreeSpace() {
        int usedSpace = 0;
        for (Partition part : memory.getPartitions()) {
            if (part.isAllocated()) {
                usedSpace += part.getSize();
            }
        }
        memory.setUsedSpace(usedSpace);
        return memory.getFreeSpace();
    }

    /**
     * 根据订单ID分配空间
     */
    public boolean allocateForOrder(Order order) {
        int orderId = order.getOrderId();
        Dish dish = order.getDish();
        if (dish == null) {
            System.out.println("错误：订单" + orderId + "没有关联菜品");
            return false;
        }

        int requiredSize = dish.getRequiredSpace();
        int totalWorkbenchSize = memory.getTotalSpace();

        // 校验1：订单已分配
        if (isOrderAllocated(orderId)) {
            System.out.println("错误：订单ID " + orderId + " 已占用工作台，无法重复分配");
            return false;
        }

        // 校验2：需求空间是否超过总空间
        if (requiredSize > totalWorkbenchSize) {
            System.out.println("错误：订单" + orderId + "所需空间 " + requiredSize + " 超过总工作台空间 " + totalWorkbenchSize);
            return false;
        }

        List<Partition> partitions = memory.getPartitions();

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
            List<Partition> newPartitions = new ArrayList<>(partitions);
            Partition bestFitPart = newPartitions.get(bestFitIndex);

            // 分割分区（如果有剩余空间）
            if (minExtraSpace > 0) {
                Partition newPart = new Partition(
                        partitions.size() + 1,
                        minExtraSpace,
                        bestFitPart.getStartAddress() + requiredSize
                );
                newPartitions.add(bestFitIndex + 1, newPart);
                bestFitPart.setSize(requiredSize);
                reassignPartitionIds(newPartitions);
            }

            // 标记分配状态
            bestFitPart.setAllocated(true);
            bestFitPart.setOrderId(orderId);
            bestFitPart.setDishName(dish.getDishName());

            // 更新Memory中的分区列表
            memory.setPartitions(newPartitions);
            calculateTotalFreeSpace();

            return true;
        } else {
            // 所有分区空间不足时的处理
            handleInsufficientSpace(order);
            return false;
        }
    }

    /**
     * 处理空间不足的情况
     */
    private void handleInsufficientSpace(Order order) {
        Dish dish = order.getDish();
        if (dish == null) return;

        int requiredSize = dish.getRequiredSpace();
        int totalFreeSpace = calculateTotalFreeSpace();
        int totalWorkbenchSize = memory.getTotalSpace();

        System.out.println("\n===== 空间不足处理 =====");
        System.out.println("订单" + order.getOrderId() + "需求：" + requiredSize + "，总可用空间：" + totalFreeSpace);

        // 检查是否是碎片导致的空间不足
        if (totalFreeSpace >= requiredSize) {
            System.out.println("原因：可用空间分散（碎片过多），尝试整理...");
            defragmentSpace();
            calculateTotalFreeSpace();

            System.out.println("碎片整理完成，重新尝试分配...");
            printMemoryStatus();

            if (allocateForOrder(order)) {
                System.out.println("整理后分配成功！");
            } else {
                System.out.println("整理后仍无法分配，可能是整理逻辑有误或需求过大");
            }
        } else {
            // 总空间确实不足
            System.out.println("原因：总可用空间不足，建议：");
            System.out.println("- 优先处理已分配任务以释放空间");
            System.out.println("- 检查任务需求是否合理（当前总空间：" + totalWorkbenchSize + "）");
        }
        System.out.println("=======================\n");
    }

    /**
     * 碎片整理（合并所有未分配分区，并更新已分配分区的地址）
     */
    private void defragmentSpace() {
        List<Partition> partitions = memory.getPartitions();
        List<Partition> newPartitions = new ArrayList<>();
        List<Partition> allocatedParts = new ArrayList<>();
        int totalWorkbenchSize = memory.getTotalSpace();

        // 收集已分配分区
        for (Partition part : partitions) {
            if (part.isAllocated()) {
                allocatedParts.add(part);
            }
        }

        // 按起始地址排序已分配分区
        allocatedParts.sort(Comparator.comparingInt(Partition::getStartAddress));

        // 重新构建分区列表，将已分配分区紧密排列
        int currentAddress = 0;
        for (Partition allocated : allocatedParts) {
            allocated.setStartAddress(currentAddress);
            newPartitions.add(allocated);
            currentAddress += allocated.getSize();
        }

        // 插入末尾的空闲分区（合并了所有碎片空间）
        if (currentAddress < totalWorkbenchSize) {
            newPartitions.add(new Partition(
                    newPartitions.size() + 1,
                    totalWorkbenchSize - currentAddress,
                    currentAddress
            ));
        }

        reassignPartitionIds(newPartitions);
        memory.setPartitions(newPartitions);
    }

    /**
     * 释放订单占用的分区
     */
    public boolean releaseOrderPartition(int orderId) {
        List<Partition> partitions = memory.getPartitions();
        for (int i = 0; i < partitions.size(); i++) {
            Partition part = partitions.get(i);
            if (part.isAllocated() && part.getOrderId() == orderId) {
                part.setAllocated(false);
                part.setOrderId(-1);
                part.setDishName(null);

                // 尝试与前一个分区合并
                if (i > 0) {
                    Partition prevPart = partitions.get(i - 1);
                    if (!prevPart.isAllocated()) {
                        prevPart.setSize(prevPart.getSize() + part.getSize());
                        partitions.remove(i);
                        i--;
                        part = prevPart;
                    }
                }

                // 尝试与后一个分区合并
                if (i < partitions.size() - 1) {
                    Partition nextPart = partitions.get(i + 1);
                    if (!nextPart.isAllocated()) {
                        part.setSize(part.getSize() + nextPart.getSize());
                        partitions.remove(i + 1);
                    }
                }

                reassignPartitionIds(partitions);
                memory.setPartitions(partitions);
                calculateTotalFreeSpace();

                return true;
            }
        }
        return false;
    }

    /**
     * 打印当前工作台状态
     */
    public void printMemoryStatus() {
        int totalWorkbenchSize = memory.getTotalSpace();

        System.out.println("\n===== 工作台状态 =====");
        System.out.println("总空间：" + totalWorkbenchSize +
                "，已使用：" + memory.getUsedSpace() +
                "，可用：" + memory.getFreeSpace());

        int totalSizeCheck = 0;
        for (Partition part : memory.getPartitions()) {
            System.out.println(part);
            totalSizeCheck += part.getSize();
        }

        if (totalSizeCheck != totalWorkbenchSize) {
            System.err.println("!!! 警告：分区总大小 (" + totalSizeCheck + ") 不等于总工作台大小 (" + totalWorkbenchSize + ") !!!");
        }

        System.out.println("======================\n");
    }

    // Getters
    public int getTotalWorkbenchSize() {
        return memory.getTotalSpace();
    }

    public Memory getMemory() {
        return memory;
    }

    public List<Partition> getPartitions() {
        return memory.getPartitions();
    }
}