package com.example.robotdelivery.service;

import com.example.robotdelivery.mapper.DishMapper;
import com.example.robotdelivery.mapper.OrderMapper;
import com.example.robotdelivery.pojo.Dish;
import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.pojo.Order.OrderStatus;
import com.example.robotdelivery.pojo.vo.OrderVO;
import org.springframework.scheduling.annotation.Scheduled;
import com.example.robotdelivery.pojo.dto.OrderDto;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService implements IOrderService{

    @Autowired

    private final OrderMapper orderMapper;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public OrderService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;

    }

    @Override
    public void add(OrderDto order) {
        // 调用数据访问类
        Order orderPojo = new Order();
        BeanUtils.copyProperties(order, orderPojo);
        // 设置创建时间
        orderPojo.setCreateTime(LocalDateTime.now());
        orderMapper.save(orderPojo);
    }
    // ================= DTO/VO 转换逻辑 =================

    /**
     * Order 实体到 OrderVO 的转换逻辑
     */
    private OrderVO convertToOrderVO(Order order) {
        OrderVO vo = new OrderVO();

        vo.setOrderId(order.getOrderId());
        vo.setDishName(order.getDish() != null ? order.getDish().getDishName() : "未知菜品");
        vo.setPriority(order.getPriority());

        // 格式化时间
        vo.setCreateTime(order.getCreateTime().format(DATE_TIME_FORMATTER));

        // 处理状态
        String status = order.getOrderStatus().name().toLowerCase();
        vo.setStatus(status);
        vo.setOriginalStatus(order.getOrderStatus()); // 保留原始状态

        // 设置状态显示文本 (用于前端展示)
        switch (order.getOrderStatus()) {
            case PENDING:
                vo.setStatusText("待处理");
                break;
            case PROCESSING:
                // 统一显示为 "烹饪中" 或 "配送中" 的前置状态
                vo.setStatusText("烹饪中");
                break;
            case COMPLETED:
                vo.setStatusText("已完成");
                break;
            default:
                vo.setStatusText("未知状态");
        }

        // TODO: 实际应用中，机器人ID和算法信息应从其他关联实体获取
        vo.setRobotId(null);
        vo.setAlgorithmText("默认调度算法");

        return vo;
    }
    /**
     * 【新增】查询所有订单实体（用于内部逻辑或兼容原有接口）
     * 默认按创建时间降序
     */
    @Override
    public List<Order> findAll() {
        // 确保返回的列表是最新的在前面
        return orderMapper.findAllByOrderByCreateTimeDesc();
    }

    /**
     * 【新增】查询所有订单 VO（用于前端展示）
     */
    @Override
    public List<OrderVO> getAllOrderVOs() {
        List<Order> orders = this.findAll();
        return orders.stream()
                .map(this::convertToOrderVO)
                .collect(Collectors.toList());
    }

    /**
     * 【新增】查询最近的 N 条订单实体（用于内部逻辑或兼容原有接口）
     */
    @Override
    public List<Order> findRecentOrders(int limit) {
        // JpaRepository 的 findTopNBy... 方法
        return orderMapper.findTop10ByOrderByCreateTimeDesc().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 【新增】查询最近的 N 条订单 VO（用于前端展示）
     */
    @Override
    public List<OrderVO> findRecentOrderVOs(int limit) {
        List<Order> orders = this.findRecentOrders(limit);
        return orders.stream()
                .map(this::convertToOrderVO)
                .collect(Collectors.toList());
    }
    @Override
    public OrderVO getOrderVOById(Integer orderId) {
        // 1. 根据ID查询订单实体
        Optional<Order> optionalOrder = orderMapper.findById(orderId);
        // 2. 实体转VO（复用已有的convertToOrderVO方法）
        return optionalOrder.map(this::convertToOrderVO).orElse(null);
    }


    private String formatOrder(Order o) {
        return "Order{" +
                "id=" + o.getOrderId() +
                ", name=" + o.getOrderName() +
                ", priority=" + o.getPriority() +
                ", dish=" + (o.getDish() != null ? o.getDish().getDishName() : null) +
                ", createTime=" + o.getCreateTime() +
                ", completeTime=" + o.getCompleteTime() +
                ", status=" + o.getOrderStatus() +
                '}';
    }

    // 保存/更新订单
    public Order save(Order order) {
        // 设置创建时间和默认状态，如果新订单未赋值
        if (order.getCreateTime() == null) {
            order.setCreateTime(LocalDateTime.now());
        }
        if (order.getOrderStatus() == null) {
            order.setOrderStatus(OrderStatus.PENDING);
        }
        return orderMapper.save(order);
    }

    // 根据主键查
    public Optional<Order> findById(Integer id) {
        return orderMapper.findById(id);
    }

    // 根据订单名查
    public Optional<Order> findByOrderName(String name) {
        return orderMapper.findByOrderName(name);
    }


    // 时间区间查
    public List<Order> listByCreateTimeRange(LocalDateTime start, LocalDateTime end) {
        return orderMapper.findByCreateTimeBetween(start, end);
    }

    // 按创建时间升序取全部
    public List<Order> listAllOrderByCreateTimeAsc() {
        return orderMapper.findAllByOrderByCreateTimeAsc();
    }

    // 按优先级降序取全部
    public List<Order> listAllOrderByPriorityDesc() {
        return orderMapper.findAllByOrderByPriorityDesc();
    }



    /**
     * 更新订单状态
     */
    public Order updateOrderStatus(Integer orderId, OrderStatus status) {
        Optional<Order> optionalOrder = orderMapper.findById(orderId);
        if (optionalOrder.isPresent()) {
            Order order = optionalOrder.get();
            order.setOrderStatus(status);
            if (status == OrderStatus.COMPLETED) {
                order.setCompleteTime(LocalDateTime.now());
            }
            return orderMapper.save(order);
        }
        return null;
    }
}
