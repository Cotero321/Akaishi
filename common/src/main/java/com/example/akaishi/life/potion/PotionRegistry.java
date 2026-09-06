package com.example.akaishi.life.potion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 药剂模板注册表（四种模板，突破按方向拆成三种模式）：
 * - 永久药剂：将该生物基因型吸收进身体 → 该来源所有器官有效适配 +3~15（纯度五档），无副作用
 * - 突破药剂·均衡：对已吸收基因型发动 30 分钟临时激活——基础数值 +10~40%（纯度四档）+
 *   额外适配 +2~+8，负基础值词条期内失效（原突破药剂数值，标准方向）
 * - 突破药剂·狂涌：同机制但 15 分钟高爆发（+20~80%），适合短窗口极限输出
 * - 突破药剂·深沉：同机制但 60 分钟长效低幅（+6~24%），适合常驻构筑
 * 同一时间最多激活 1 种，到期/卸载后可再次激活。
 * 模板以 id 索引，药剂台界面与服务端按 id 查询；列表顺序 = 界面顺序（0 恒为永久药剂）。
 */
public final class PotionRegistry {

    /** 永久药剂：吸收基因型提升适配度（无风险） */
    public static final String PERMANENT_ID = "permanent";
    /** 突破药剂·均衡（标准方向） */
    public static final String BALANCE_ID = "balance";
    /** 突破药剂·狂涌（爆发方向） */
    public static final String SURGE_ID = "surge";
    /** 突破药剂·深沉（持久方向） */
    public static final String ENDURE_ID = "endure";

    private static final Map<String, PotionTemplate> TEMPLATES = new LinkedHashMap<>();

    static {
        // 0 号恒为永久药剂：药剂台界面把 0 号行固定映射到「永久」，其余为突破家族
        register(new PotionTemplate(PERMANENT_ID, "life.akaishi.potion.permanent",
                3, 20_000L, 60, BreakthroughMode.NONE));
        register(new PotionTemplate(BALANCE_ID, "life.akaishi.potion.breakthrough_balance",
                5, 30_000L, 80, BreakthroughMode.BALANCE));
        register(new PotionTemplate(SURGE_ID, "life.akaishi.potion.breakthrough_surge",
                7, 50_000L, 100, BreakthroughMode.SURGE));
        register(new PotionTemplate(ENDURE_ID, "life.akaishi.potion.breakthrough_endure",
                4, 25_000L, 120, BreakthroughMode.ENDURE));
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

    /** 突破药剂家族（排除 0 号永久药剂；药剂台第二行在家族内切换模式） */
    public static List<PotionTemplate> breakthroughs() {
        return all().stream().filter(t -> t.breakthrough()).toList();
    }
}
