package com.example.robotdelivery.pojo;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 菜品类（JPA实体，对应数据库dish表）
 * 贴合文档“订单定制菜品”需求：A/B/C三种菜品，各需不同工具和工作区空间
 */
@Entity
@Table(name = "dish") // 与原有数据库表名一致，不冲突
public class Dish {
    // 1. 文档需求：A/B/C菜品类型枚举（映射到数据库字段dish_type）
    public enum DishType {
        A, // 文档场景：需烤箱+20空间
        B, // 文档场景：需烤箱+煎锅+60空间
        C  // 文档场景：需煎锅+40空间
    }

    // 2. 原有JPA主键与基础字段（保留，确保数据库交互不冲突）
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dish_id", nullable = false)
    private Integer dishId;

    @Column(name = "dish_name", nullable = false, length = 100)
    private String dishName; // 如“A类菜品-烤箱专属”“B类菜品-烤箱+煎锅”

    // 3. 新增：文档需求字段（映射到数据库，持久化存储）
    @Column(name = "dish_type", nullable = false) // 数据库存储枚举名（A/B/C）
    @Enumerated(EnumType.STRING) // 枚举以字符串形式存储（避免数字枚举值混乱）
    private DishType dishType; // 菜品类型（A/B/C，文档核心需求）

    @Column(name = "dish_space", nullable = false) // 复用原有dish_space字段，存储所需工作区空间
    private Integer requiredSpace; // 文档“工作区内存分配”需求：菜品所需空间

    @Column(name = "need_oven", nullable = false) // 新增数据库字段：是否需要烤箱（0=否，1=是）
    private Boolean needOven; // 文档“资源申请”需求：是否需要烤箱

    @Column(name = "need_fry_pan", nullable = false) // 新增数据库字段：是否需要煎锅（0=否，1=是）
    private Boolean needFryPan; // 文档“资源申请”需求：是否需要煎锅

    // 4. 原有食材字段（保留，适配数据库）
    @Column(name = "dish_ingredient")
    private Integer ingredients;

    // 5. 构造方法1：无参构造（JPA实体必须，用于数据库查询映射）
    public Dish() {}

    // 6. 构造方法2：按菜品类型初始化（贴合文档场景，简化业务代码）
    public Dish(DishType dishType) {
        this.dishType = dishType;
        // 按文档场景初始化：A/B/C菜品的资源需求
        switch (dishType) {
            case A:
                this.dishName = "A类菜品（烤箱专属）";
                this.requiredSpace = 20; // 文档设定：A需20空间
                this.needOven = true;    // 文档设定：A需烤箱
                this.needFryPan = false; // 文档设定：A无需煎锅
                break;
            case B:
                this.dishName = "B类菜品（烤箱+煎锅）";
                this.requiredSpace = 30; // 文档设定：B需30空间
                this.needOven = true;    // 文档设定：B需烤箱
                this.needFryPan = true;  // 文档设定：B需煎锅
                break;
            case C:
                this.dishName = "C类菜品（煎锅专属）";
                this.requiredSpace = 40; // 文档设定：C需40空间
                this.needOven = false;   // 文档设定：C无需烤箱
                this.needFryPan = true;  // 文档设定：C需煎锅
                break;
        }
    }

    // Getter 和 Setter 方法
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

    public DishType getDishType() {
        return dishType;
    }

    public void setDishType(DishType dishType) {
        this.dishType = dishType;
    }

    public Integer getRequiredSpace() {
        return requiredSpace;
    }

    public void setRequiredSpace(Integer requiredSpace) {
        this.requiredSpace = requiredSpace;
    }

    public Boolean getNeedOven() {
        return needOven;
    }

    public void setNeedOven(Boolean needOven) {
        this.needOven = needOven;
    }

    public Boolean getNeedFryPan() {
        return needFryPan;
    }

    public void setNeedFryPan(Boolean needFryPan) {
        this.needFryPan = needFryPan;
    }

    public Integer getIngredients() {
        return ingredients;
    }

    public void setIngredients(Integer ingredients) {
        this.ingredients = ingredients;
    }
}