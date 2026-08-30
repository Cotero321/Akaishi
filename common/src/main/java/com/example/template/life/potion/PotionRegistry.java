package com.example.template.life.potion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 药剂模板注册表（初始两种模板）：
 * - 永久药剂：饮用后已移植非原生器官适配度永久 +15，无副作用（无风险）
 * - 突破药剂：饮用后已移植非原生器官效果 ×1.5，同时放大缺点——排斥 + 随机 debuff（有风险）
 * 模板以 id 索引，药剂台界面与服务端按 id 查询。
 */
public final class PotionRegistry {

    /** 永久药剂：提升适配度（无风险） */
    public static final String PERMANENT_ID = "permanent";
    /** 突破药剂：效果增强 + 随机副作用（有风险） */
    public static final String BREAKTHROUGH_ID = "breakthrough";

    private static final Map<String, PotionTemplate> TEMPLATES = new LinkedHashMap<>();

    static {
        register(new PotionTemplate(PERMANENT_ID, "life.template_mod.potion.permanent",
                3, 20_000L, 60, false));
        register(new PotionTemplate(BREAKTHROUGH_ID, "life.template_mod.potion.breakthrough",
                5, 30_000L, 80, true));
    }

    private PotionRegistry() {
    }

    private static void register(PotionTemplate template) {
        TEMPLATES.put(template.id(), template);
    }

    /** 查询模板（无则返回 null） */
    public static PotionTemplate get(String id) {
        return TEMPLATES.get(id);
    }

    /** 全部模板（保持注册顺序，界面按钮渲染顺序一致） */
    public static List<PotionTemplate> all() {
        return List.copyOf(TEMPLATES.values());
    }
}
