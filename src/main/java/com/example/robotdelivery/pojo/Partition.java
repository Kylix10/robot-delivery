package com.example.robotdelivery.pojo;

public class Partition {
    private int id;                 // 分区ID
    private int size;               // 分区大小
    private int startAddress;       // 分区起始地址
    private boolean isAllocated;    // 是否已分配
    private int orderId;            // 若已分配，存储订单ID（原dishId修改为orderId）
    private String dishName;        // 若已分配，存储菜肴名称

    public Partition(int id, int size, int startAddress) {
        this.id = id;
        this.size = size;
        this.startAddress = startAddress;
        this.isAllocated = false;
        this.orderId = -1; // -1 表示未分配给任何订单
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getStartAddress() { return startAddress; }
    public void setStartAddress(int startAddress) { this.startAddress = startAddress; }
    public boolean isAllocated() { return isAllocated; }
    public void setAllocated(boolean allocated) { isAllocated = allocated; }
    public int getOrderId() { return orderId; }  // 原getDishId修改为getOrderId
    public void setOrderId(int orderId) { this.orderId = orderId; }  // 原setDishId修改为setOrderId
    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }

    @Override
    public String toString() {
        if (isAllocated) {
            return String.format("分区 %d [起始地址: %d, 大小: %d, 状态: 已分配, 订单ID: %d, 菜肴: %s]",
                    id, startAddress, size, orderId, dishName);
        } else {
            return String.format("分区 %d [起始地址: %d, 大小: %d, 状态: 未分配]",
                    id, startAddress, size);
        }
    }
}