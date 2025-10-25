
package com.example.robotdelivery.controller;
import com.example.robotdelivery.pojo.Ingredient;
import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.mapper.IngredientRepository;
import com.example.robotdelivery.mapper.OrderMapper;
import com.example.robotdelivery.service.DiskSchedulerService;
import com.example.robotdelivery.vo.OrderScheduleResult;
import com.example.robotdelivery.vo.SchedulerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/order-scheduler") // 接口前缀
@CrossOrigin(origins = "*") // 允许跨域
public class OrderSchedulerController {

    private static final Logger log = LoggerFactory.getLogger(OrderSchedulerController.class); // 日志工具

    @Autowired
    private OrderMapper orderRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private DiskSchedulerService diskSchedulerService;

    /**
     * 通过订单ID查询仓库拿取食材的调度结果
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getWarehousePickResult( // 注意：返回类型改为<?>，支持错误时返回字符串
                                                     @PathVariable Integer orderId,
                                                     @RequestParam(defaultValue = "0") int initialPosition
    ) {
        // 外层大try-catch：捕获所有未预料到的异常
        try {
            log.info("开始处理订单{}的路径查询，初始位置：{}", orderId, initialPosition);

            // 1. 查询订单（已处理订单不存在的情况）
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "订单不存在（ID：" + orderId + "）"
                    ));
            log.info("成功查询到订单{}：{}", orderId, order);

            // 2. 检查订单关联的菜品是否存在
            if (order.getDish() == null) {
                String errorMsg = "订单" + orderId + "未关联任何菜品";
                log.error(errorMsg);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg);
            }

            // 3. 提取食材位置列表（处理食材为空的情况）
            List<Ingredient> ingredients = order.getDish().getIngredients();
            if (ingredients == null || ingredients.isEmpty()) {
                String errorMsg = "菜品" + order.getDish().getDishId() + "（" + order.getDish().getDishName() + "）未关联任何食材";
                log.error(errorMsg);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg);
            }

            List<Integer> ingredientPositions;
            try {
                // 提取位置时可能因Ingredient.position为null抛出NullPointerException
                ingredientPositions = ingredients.stream()
                        .map(ingredient -> {
                            if (ingredient.getPosition() == null) {
                                throw new RuntimeException("食材" + ingredient.getName() + "的位置为null");
                            }
                            return ingredient.getPosition();
                        })
                        .collect(Collectors.toList());
                log.info("订单{}的食材位置列表：{}", orderId, ingredientPositions);
            } catch (Exception e) {
                String errorMsg = "提取食材位置失败：" + e.getMessage();
                log.error(errorMsg, e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
            }

            // 4. 获取食材位置-名称映射（处理查询失败或数据异常）
            Map<Integer, String> warehouseIngredients = new HashMap<>();
            try {
                List<Map<String, Object>> positionNameList = ingredientRepository.findAllPositionToName();
                if (positionNameList == null || positionNameList.isEmpty()) {
                    log.warn("食材表中未查询到任何位置-名称映射数据");
                } else {
                    for (Map<String, Object> entry : positionNameList) {
                        // 处理map中key或value为null的情况
                        Object keyObj = entry.get("key");
                        Object nameObj = entry.get("value");
                        if (keyObj == null || nameObj == null) {
                            log.warn("跳过无效的食材映射记录：key={}, value={}", keyObj, nameObj);
                            continue;
                        }
                        // 处理key不是数字类型的情况
                        if (!(keyObj instanceof Number)) {
                            log.warn("食材位置不是数字类型：{}", keyObj);
                            continue;
                        }
                        Integer position = ((Number) keyObj).intValue();
                        String name = (String) nameObj;
                        warehouseIngredients.put(position, name);
                    }
                }
                log.info("仓库食材位置-名称映射：{}", warehouseIngredients);
            } catch (Exception e) {
                String errorMsg = "查询食材位置-名称映射失败：" + e.getMessage();
                log.error(errorMsg, e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
            }

            // 5. 生成三种算法的结果（单个算法失败不影响整体）
            Map<String, SchedulerResult> algorithmResults = new HashMap<>();
            try {
                algorithmResults.put("FCFS", generateSchedulerResult("FCFS", initialPosition, ingredientPositions, warehouseIngredients));
                algorithmResults.put("SSTF", generateSchedulerResult("SSTF", initialPosition, ingredientPositions, warehouseIngredients));
                algorithmResults.put("SCAN", generateSchedulerResult("SCAN", initialPosition, ingredientPositions, warehouseIngredients));
            } catch (Exception e) {
                log.error("生成算法结果时发生部分失败，但继续执行", e);
            }

            // 6. 组装返回结果
            OrderScheduleResult result = new OrderScheduleResult();
            result.setDishName(order.getDish().getDishName());
            result.setAlgorithmResults(algorithmResults);

            // 测试序列化（避免返回时才发现序列化失败）
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String jsonTest = objectMapper.writeValueAsString(result);
                log.info("订单{}的结果序列化测试成功：{}", orderId, jsonTest.substring(0, Math.min(200, jsonTest.length()))); // 打印前200字符
            } catch (Exception e) {
                String errorMsg = "结果序列化失败：" + e.getMessage();
                log.error(errorMsg, e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
            }

            log.info("订单{}的路径查询处理完成", orderId);
            return ResponseEntity.ok(result);

        } catch (ResponseStatusException e) {
            // 捕获已知的业务异常（如订单不存在、菜品无食材等）
            log.error("业务异常：{}", e.getReason());
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (Exception e) {
            // 捕获所有未预料到的异常
            String errorMsg = "处理订单" + orderId + "时发生未知错误：" + e.getMessage();
            log.error(errorMsg, e); // 打印完整堆栈
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMsg);
        }
    }

    /**
     * 生成单个算法的结果（单独捕获异常，避免影响其他算法）
     */
    private SchedulerResult generateSchedulerResult(
            String algorithm,
            int initialPosition,
            List<Integer> positions,
            Map<Integer, String> warehouseIngredients
    ) {
        try {
            log.info("开始生成{}算法结果，初始位置：{}，食材位置：{}", algorithm, initialPosition, positions);
            SchedulerResult result = diskSchedulerService.schedule(algorithm, initialPosition, positions);
            result.setWarehouseIngredients(warehouseIngredients);
            log.info("{}算法结果生成成功：总距离={}", algorithm, result.getTotalDistance());
            return result;
        } catch (Exception e) {
            String errorMsg = algorithm + "算法调度失败：" + e.getMessage();
            log.error(errorMsg, e);
            // 返回一个包含错误信息的默认结果，避免整个接口失败
            SchedulerResult errorResult = new SchedulerResult();
            errorResult.setAlgorithm(algorithm);
            errorResult.setTotalDistance(-1); // 用-1标识失败
            errorResult.setErrorMsg(errorMsg);
            errorResult.setWarehouseIngredients(warehouseIngredients);
            return errorResult;
        }
    }
}