package com.example.robotdelivery.pojo;

import com.example.robotdelivery.pojo.Ingredient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class Warehouse {
    private final Map<Integer, Ingredient> positionToIngredient = new HashMap<>();

    // 初始化仓库（模拟食材分布）
    public Warehouse() {
        putIngredient("汉堡胚", 10);
        putIngredient("生菜", 25);
        putIngredient("芝士", 5);
        putIngredient("洋葱", 30);
        putIngredient("酸黄瓜", 15);
        putIngredient("酱料", 20);
        putIngredient("牛肉饼", 8);
        putIngredient("火腿", 40);
        putIngredient("煎蛋", 56);
        putIngredient("鸡肉", 34);
        putIngredient("淀粉", 60);
    }

    private void putIngredient(String name, Integer position) {
        Ingredient ing = new Ingredient();
        ing.setName(name);
        ing.setPosition(position);
        positionToIngredient.put(position, ing);
    }

     /** 按位置取食材 */
     public Optional<Ingredient> getIngredientByPosition(Integer position) {
        return Optional.ofNullable(positionToIngredient.get(position));
    }

    /** 返回仓库所有食材（副本） */
    public Map<Integer, Ingredient> getAllIngredients() {
        return new HashMap<>(positionToIngredient);
    }

    /** 根据食材名称查找其在仓库中的位置 */
    public Optional<Integer> getPositionByIngredientName(String name) {
        return positionToIngredient.entrySet().stream()
                .filter(e -> e.getValue().getName().equals(name))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    /** 打印仓库分布（调试用） */
    public void printWarehouseMap() {
        System.out.println("📦 仓库食材布局：");
        positionToIngredient.forEach((pos, ing) ->
                System.out.println("位置 " + pos + " → " + ing.getName()));
    }
    
}