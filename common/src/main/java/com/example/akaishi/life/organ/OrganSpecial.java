package com.example.akaishi.life.organ;

/**
 * 器官独特机制类型：无法用属性/被动表达的专属效果（随器官移植生效）。
 * 每个特殊效果在移植生效层（PlayerBodyHandler）中单独实现逻辑。
 */
public enum OrganSpecial {

    /** 牛胃：可以直接吃小麦恢复饥饿，但不能吃肉类食物 */
    COW_STOMACH("cow_stomach"),
    /** 末影核心：接触水源时瞬移离开并损失生命值（怕水） */
    ENDER_WATER_FEAR("ender_water_fear"),
    /** 凋灵核心（满级）：攻击时发射凋零骷髅头弹射物（不破坏方块，伤害较低） */
    WITHER_SKULL("wither_skull"),
    /** 龙之心（满级）：攻击时喷吐龙息弹射物（伤害较低） */
    DRAGON_BREATH("dragon_breath"),
    /** 监守者核心（满级）：攻击时沿视线释放直线穿透音爆（无视护甲） */
    SONIC_BOOM("sonic_boom");

    private final String id;

    OrganSpecial(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
