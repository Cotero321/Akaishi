package com.example.akaishi.life.mechanical.trait;

/**
 * 材料特性的「四档数值表」轻量辅助结构（企划 §4 的 Lv1~Lv4）。
 * <p>
 * 只承载数值，不含任何语义分支，供各特性处理器按等级取值，避免每档写重复 switch。
 * 所有数值均为<b>待调手感值</b>。
 */
public record TraitTier(float lv1, float lv2, float lv3, float lv4) {

    /** 取指定档位的值（等级越界时钳制到 1~4）。 */
    public float at(int level) {
        return switch (Math.max(1, Math.min(4, level))) {
            case 1 -> lv1;
            case 2 -> lv2;
            case 3 -> lv3;
            default -> lv4;
        };
    }

    /** 取指定档位的整数值（用于计数/时长/等级等整型档位）。 */
    public int atInt(int level) {
        return Math.round(at(level));
    }

    public static TraitTier of(float lv1, float lv2, float lv3, float lv4) {
        return new TraitTier(lv1, lv2, lv3, lv4);
    }
}
