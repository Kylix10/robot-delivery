package com.example.robotdelivery.pojo.vo;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "performance_result") // 数据库表名
public class PerformanceResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 自增主键

    @Column(name = "mode_type") // 1=算法模式，2=默认模式
    private Integer modeType;

    @Column(name = "completed_count") // 完成订单数
    private Integer completedCount;

    @Column(name = "total_revenue") // 总收益
    private Double totalRevenue;

    @Column(name = "avg_response_time_ms") // 平均响应时间（毫秒）
    private Double avgResponseTimeMs;

    @Column(name = "throughput") // 吞吐量（订单/秒）
    private Double throughput;

    @Column(name = "calc_time") // 计算时间（写入数据库的时间）
    private LocalDateTime calcTime;
}