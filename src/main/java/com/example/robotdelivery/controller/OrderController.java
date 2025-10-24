package com.example.robotdelivery.controller;

import com.example.robotdelivery.pojo.dto.OrderDto;
import com.example.robotdelivery.pojo.vo.OrderVO;
import com.example.robotdelivery.service.IOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器：提供前端所需的订单查询、创建接口，统一返回 OrderVO（前端展示专用）
 */
@RestController
@RequestMapping("/api/orders") // 调整路径为 /api/orders，符合前后端分离接口规范
public class OrderController {

    @Autowired
    private IOrderService orderService;

    /**
     * 1. 创建新订单
     * 前端调用：POST /api/orders
     * @param orderDto 前端传递的订单参数（包含菜品、优先级等）
     * @return 统一响应格式（状态码、消息）
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addOrder(@RequestBody OrderDto orderDto) {
        try {
            orderService.add(orderDto);
            // 统一返回成功响应（包含状态码、消息）
            return ResponseEntity.ok(new ApiResponse<>(200, "订单创建成功", null));
        } catch (Exception e) {
            // 异常时返回错误响应（包含错误消息）
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "订单创建失败：" + e.getMessage(), null));
        }
    }

    /**
     * 2. 查询所有订单（前端“订单管理”页面专用）
     * 前端调用：GET /api/orders
     * @return 所有订单的 VO 列表（包含格式化后的展示字段）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderVO>>> getAllOrders() {
        try {
            List<OrderVO> allOrderVOs = orderService.getAllOrderVOs();
            return ResponseEntity.ok(new ApiResponse<>(200, "查询所有订单成功", allOrderVOs));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "查询所有订单失败：" + e.getMessage(), null));
        }
    }

    /**
     * 3. 查询最近N条订单（前端“仪表盘”或“快速查看”功能专用）
     * 前端调用：GET /api/orders/recent?limit=10（limit默认10，可自定义）
     * @param limit 要查询的“最近订单”数量
     * @return 最近N条订单的 VO 列表
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<OrderVO>>> getRecentOrders(
            @RequestParam(defaultValue = "10", required = false) int limit) {
        try {
            // 校验参数：避免limit过大导致性能问题
            if (limit <= 0 || limit > 100) {
                limit = 10; // 超出范围时默认返回10条
            }
            List<OrderVO> recentOrderVOs = orderService.findRecentOrderVOs(limit);
            return ResponseEntity.ok(new ApiResponse<>(200, "查询最近订单成功", recentOrderVOs));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "查询最近订单失败：" + e.getMessage(), null));
        }
    }

    /**
     * 4. （可选）根据订单ID查询单个订单详情
     * 前端调用：GET /api/orders/1（1为订单ID）
     * @param orderId 订单ID
     * @return 单个订单的 VO 详情
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderVO>> getOrderById(@PathVariable Integer orderId) {
        try {
            OrderVO orderVO = orderService.getOrderVOById(orderId);
            if (orderVO == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(404, "订单不存在（ID：" + orderId + "）", null));
            }
            return ResponseEntity.ok(new ApiResponse<>(200, "查询订单详情成功", orderVO));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "查询订单详情失败：" + e.getMessage(), null));
        }
    }

    /**
     * 统一响应格式类（内部静态类，避免前端处理不同格式的响应）
     *
     * @param <T>     响应数据类型
     * @param code    状态码（200=成功，404=未找到，500=服务器错误等）
     * @param message 提示消息（成功/错误描述）
     * @param data    响应数据（成功时返回，错误时为null）
     */
        public record ApiResponse<T>(int code, String message, T data) {
    }
}