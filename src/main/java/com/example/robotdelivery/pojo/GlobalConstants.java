package com.example.robotdelivery.pojo;

public class GlobalConstants {
    private Integer orderComplete;//吞吐量
    private Integer waitTime;//响应时间

    public Integer getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(Integer waitTime) {
        this.waitTime = waitTime;
    }

    public Integer getOrderComplete() {
        return orderComplete;
    }

    public void setOrderComplete(Integer orderComplete) {
        this.orderComplete = orderComplete;
    }
}
