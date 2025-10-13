package com.example.robotdelivery.pojo;

import jakarta.persistence.*;
import java.util.List;

// 标记这是一个JPA实体类，对应数据库中的表
@Entity
// 指定对应的数据库表名
@Table(name = "dish")
public class Dish {

    // 主键字段
    @Id
    // 主键生成策略：自增（依赖数据库支持）
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 映射到数据库表中的字段名
    @Column(name = "dish_id", nullable = false)
    private Integer dishId;

    // 菜品名称
    @Column(name = "dish_name", nullable = false, length = 100)
    private String dishName;

    // 菜品所需食材（多对多关系）
    // 实际项目中推荐使用中间表维护多对多关系

    @Column(name = "dish_ingredient")
    private Integer ingredients;

    // 菜品所需空间
    @Column(name = "dish_space")
    private Integer dishSpace;

    // getter 和 setter 方法
    public Integer getDishId() {
        return dishId;
    }

    public void setDishId(Integer dishId) {
        this.dishId = dishId;
    }

    public String getDishName() {
        return dishName;
    }

    public void setDishName(String dishName) {
        this.dishName = dishName;
    }



    public void setIngredients(Integer ingredients) {
        this.ingredients = ingredients;
    }

    public Integer getDishSpace() {
        return dishSpace;
    }

    public void setDishSpace(Integer dishSpace) {
        this.dishSpace = dishSpace;
    }
}
