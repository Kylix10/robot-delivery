package com.example.robotdelivery.mapper;

import com.example.robotdelivery.pojo.Robot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.QueryByExampleExecutor;

import org.springframework.data.repository.query.Param;

import java.util.List;

// 继承必要的接口以支持基础CRUD和扩展查询
public interface RobotRepository extends JpaRepository<Robot, Integer>, QueryByExampleExecutor<Robot> {

    //以对robot实例的sql操作为例，需要什么操作后续在此处添加

    
    // 1. 简单条件查询（方法名规则）
    // 按状态查询
    List<Robot> findByRobot_status(Integer robotStatus);

    // 按位置查询
    List<Robot> findByRobotLcation(Integer robotLocation);

    // 按状态和位置组合查询（且关系）
    List<Robot> findByRobot_statusAndRobotLcation(Integer robotStatus, Integer robotLocation);

    // 按状态或位置组合查询（或关系）
    List<Robot> findByRobot_statusOrRobotLcation(Integer robotStatus, Integer robotLocation);

    // 按位置范围查询（大于指定值）
    List<Robot> findByRobotLcationGreaterThan(Integer location);

    // 按位置范围查询（小于等于指定值）
    List<Robot> findByRobotLcationLessThanOrEqualTo(Integer location);


    // 2. 复杂条件查询（@Query注解）
    // 模糊查询（假设location是字符串类型时使用，这里仅作示例）
    @Query("SELECT r FROM Robot r WHERE CAST(r.robotLcation AS string) LIKE %:keyword%")
    List<Robot> findByLocationLike(@Param("keyword") String keyword);

    // 多条件动态SQL（参数为null时忽略该条件）
    @Query("SELECT r FROM Robot r WHERE " +
            "(:status IS NULL OR r.robot_status = :status) AND " +
            "(:location IS NULL OR r.robotLcation = :location)")
    List<Robot> findByDynamicParams(
            @Param("status") Integer status,
            @Param("location") Integer location
    );

    // 原生SQL查询（多表关联示例，假设关联订单表）
    @Query(value = "SELECT r.* FROM tb_robot r " +
            "JOIN tb_order o ON r.robot_id = o.robot_id " +
            "WHERE o.order_status = :orderStatus",
            nativeQuery = true)
    List<Robot> findRobotsWithOrders(@Param("orderStatus") Integer orderStatus);


    // 3. 分页与排序查询
    // 按状态分页查询
    Page<Robot> findByRobot_status(Integer robotStatus, Pageable pageable);

    // 无条件分页查询
    @Override
    Page<Robot> findAll(Pageable pageable);

}
