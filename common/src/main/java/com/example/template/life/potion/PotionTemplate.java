package com.example.template.life.potion;

/**
 * 药剂模板：定义药剂台可制作的一种药剂。
 * 与 OrganTemplate 同构——模板只声明"配方 + 类型"，具体功效由饮用分发逻辑实现，
 * 新增药剂 = 注册新模板，无需新增物品。
 *
 * @param id            模板 id（写入药剂物品 NBT）
 * @param nameKey       药剂类型显示名翻译键
 * @param solidCost     制作消耗固态物数量
 * @param lifeCost      制作消耗生命能量
 * @param ticks         制作耗时（tick）
 * @param breakthrough  是否突破药剂（有副作用风险）
 */
public record PotionTemplate(
        String id,
        String nameKey,
        int solidCost,
        long lifeCost,
        int ticks,
        boolean breakthrough) {
}
