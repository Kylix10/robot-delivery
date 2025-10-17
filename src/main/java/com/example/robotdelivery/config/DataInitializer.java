package com.example.robotdelivery.config;

import com.example.robotdelivery.mapper.DishRepository;
import com.example.robotdelivery.mapper.IngredientRepository;
import com.example.robotdelivery.pojo.Dish;
import com.example.robotdelivery.pojo.Ingredient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DataInitializer implements ApplicationRunner {

    private final IngredientRepository ingredientRepository;
    private final DishRepository dishRepository;

    public DataInitializer(IngredientRepository ingredientRepository, DishRepository dishRepository) {
        this.ingredientRepository = ingredientRepository;
        this.dishRepository = dishRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        // 1️⃣ 初始化食材
        List<String> ingredientNames = Arrays.asList(
                "汉堡胚", "生菜", "芝士", "洋葱", "酸黄瓜", "酱料",
                "牛肉饼", "火腿", "煎蛋", "鸡肉", "淀粉"
        );

        Map<String, Ingredient> nameToIngredient = ingredientRepository.findAll().stream()
                .collect(Collectors.toMap(Ingredient::getName, Function.identity(), (a,b)->a));

        List<Ingredient> toSave = new ArrayList<>();
        int pos = 1;
        for (String name : ingredientNames) {
            if (!nameToIngredient.containsKey(name)) {
                Ingredient ing = new Ingredient();
                ing.setName(name);
                ing.setPosition(pos++);
                toSave.add(ing);
            }
        }
        ingredientRepository.saveAll(toSave).forEach(i -> nameToIngredient.put(i.getName(), i));
        System.out.println("✅ 食材初始化完成");

        // 2️⃣ 初始化菜品
        Map<String, List<String>> dishToIngredients = new LinkedHashMap<>();
        Map<String, Set<String>> dishToTools = new LinkedHashMap<>();
        Map<String, Integer> dishToSpace = new LinkedHashMap<>();

        dishToIngredients.put("麦辣鸡腿堡", Arrays.asList("汉堡胚", "鸡肉", "生菜", "酱料"));
        dishToTools.put("麦辣鸡腿堡", Set.of("烤箱"));
        dishToSpace.put("麦辣鸡腿堡", 20);

        dishToIngredients.put("巨无霸汉堡", Arrays.asList("汉堡胚", "牛肉饼", "芝士", "生菜", "洋葱", "酸黄瓜", "酱料"));
        dishToTools.put("巨无霸汉堡", Set.of("炸锅","烤箱"));
        dishToSpace.put("巨无霸汉堡", 30);

        dishToIngredients.put("双层吉士汉堡", Arrays.asList("汉堡胚", "牛肉饼", "芝士", "洋葱", "酸黄瓜", "酱料"));
        dishToTools.put("双层吉士汉堡", Set.of("煎锅","烤箱"));
        dishToSpace.put("双层吉士汉堡", 25);

        dishToIngredients.put("汉堡包", Arrays.asList("汉堡胚","牛肉饼","洋葱","酸黄瓜","酱料"));
        dishToTools.put("汉堡包", Set.of("炸锅","煎锅"));
        dishToSpace.put("汉堡包", 15);

        dishToIngredients.put("麦乐鸡", Arrays.asList("鸡肉","淀粉","酱料"));
        dishToTools.put("麦乐鸡", Set.of("炸锅"));
        dishToSpace.put("麦乐鸡", 10);

        dishToIngredients.put("吉士蛋麦满分", Arrays.asList("汉堡胚","火腿","煎蛋","芝士","酱料"));
        dishToTools.put("吉士蛋麦满分", Set.of("炸锅","煎锅","烤箱"));
        dishToSpace.put("吉士蛋麦满分", 40);


        for (String name : dishToIngredients.keySet()) {
            Dish dish = dishRepository.findByDishName(name);
            if (dish == null) {
                dish = new Dish();
                dish.setDishName(name);
                dish.setDishType(name); // 唯一标识
            }

            // 绑定食材
            dish.getIngredients().clear();
            dish.getIngredients().addAll(
                    dishToIngredients.get(name).stream()
                            .map(nameToIngredient::get)
                            .filter(Objects::nonNull)
                            .toList()
            );

            Set<String> tools = dishToTools.getOrDefault(name, Set.of());
            dish.setNeedOven(tools.contains("烤箱"));
            dish.setNeedFryPan(tools.contains("煎锅"));
            dish.setNeedFryPot(tools.contains("炸锅"));
            dish.setRequiredSpace(dishToSpace.getOrDefault(name, 30));

            if (dish.getRequiredSpace() == null) dish.setRequiredSpace(30);
            dishRepository.save(dish);
            System.out.println("✅ 已初始化菜品: " + name);
        }

        System.out.println("🌟 数据初始化完成");
    }
}
