package com.example.robotdelivery.pojo;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "dish",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "dish_type")
        }
)
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dish_id", nullable = false)
    private Integer dishId;

    @Column(name = "dish_name", nullable = false, length = 100)
    private String dishName;

    @Column(name = "dish_type", nullable = false, unique = true, length = 50)
    private String dishType;

    @Column(name = "dish_space", nullable = false)
    private Integer requiredSpace;

    @Column(name = "need_oven", nullable = false)
    private Boolean needOven;

    @Column(name = "need_fry_pan", nullable = false)
    private Boolean needFryPan;

    @Column(name = "need_fry_pot", nullable = false)
    private Boolean needFryPot;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.MERGE})
    @JoinTable(
            name = "dish_ingredient_mapping",
            joinColumns = @JoinColumn(name = "dish_id"),
            inverseJoinColumns = @JoinColumn(name = "ingredient_id")
    )
    private List<Ingredient> ingredients = new ArrayList<>();

    public Dish() {}

    // 可选：构造器初始化名字、类型和工具
    public Dish(String dishName, String dishType, Boolean needOven, Boolean needFryPan, Boolean needFryPot) {
        this.dishName = dishName;
        this.dishType = dishType;
        this.needOven = needOven;
        this.needFryPan = needFryPan;
        this.needFryPot = needFryPot;
    }

    // getters & setters方法
    public Integer getDishId() { return dishId; }
    public void setDishId(Integer dishId) { this.dishId = dishId; }

    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }

    public String getDishType() { return dishType; }
    public void setDishType(String dishType) { this.dishType = dishType; }

    public Integer getRequiredSpace() { return requiredSpace; }
    public void setRequiredSpace(Integer requiredSpace) { this.requiredSpace = requiredSpace; }

    public Boolean getNeedOven() { return needOven; }
    public void setNeedOven(Boolean needOven) { this.needOven = needOven; }

    public Boolean getNeedFryPan() { return needFryPan; }
    public void setNeedFryPan(Boolean needFryPan) { this.needFryPan = needFryPan; }

    public Boolean getNeedFryPot() { return needFryPot; }
    public void setNeedFryPot(Boolean needFryPot) { this.needFryPot = needFryPot; }

    public List<Ingredient> getIngredients() { return ingredients; }
    public void setIngredients(List<Ingredient> ingredients) { this.ingredients = ingredients; }
}
