package com.example.robotdelivery.mapper;

import com.example.robotdelivery.pojo.Robot;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import org.springframework.data.repository.query.Param;
import static org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.QueryHints.HINT_CACHEABLE;

// JpaRepository<实体类, 主键类型>：提供默认的增删改查方法
@Repository
public interface RobotRepository extends JpaRepository<Robot, Integer> {
    // 禁用缓存，强制从数据库读取最新数据
    @QueryHints(value = {
            @QueryHint(name = HINT_FETCH_SIZE, value = "1"),
            @QueryHint(name = HINT_CACHEABLE, value = "false") // 禁用二级缓存
    })
    @Query("SELECT r FROM Robot r WHERE r.robotStatus = :status")
    List<Robot> findByRobotStatus(@Param("status") Integer status);

}


