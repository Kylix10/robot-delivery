package com.example.robotdelivery.pojo;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class Memory {

    private final Integer workbenchId = 1;
    private final Integer totalSpace = 100;
    private volatile Integer usedSpace = 0;
    private volatile Integer occupiedByRobotId;
    private List<Partition> partitions = new ArrayList<>();  // 新增分区列表

    // Getter和Setter方法
    public Integer getWorkbenchId() {
        return workbenchId;
    }

    public Integer getTotalSpace() {
        return totalSpace;
    }

    public Integer getUsedSpace() {
        return usedSpace;
    }

    public synchronized void setUsedSpace(Integer usedSpace) {
        this.usedSpace = Math.max(0, Math.min(usedSpace, totalSpace));
    }

    public Integer getOccupiedByRobotId() {
        return occupiedByRobotId;
    }

    public synchronized void setOccupiedByRobotId(Integer occupiedByRobotId) {
        this.occupiedByRobotId = occupiedByRobotId;
    }

    public synchronized Integer getFreeSpace() {
        return totalSpace - usedSpace;
    }

    // 新增分区列表的Getter和Setter
    public List<Partition> getPartitions() {
        return partitions;
    }

    public void setPartitions(List<Partition> partitions) {
        this.partitions = partitions;
    }
}