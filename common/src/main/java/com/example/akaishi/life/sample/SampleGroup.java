package com.example.akaishi.life.sample;

import com.example.akaishi.config.ModConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Shulker;

/**
 * 生命样本生物分组：决定样本的基因来源与基础纯度。
 * 采集器对生物右键时按优先级匹配（龙 > 首领 > 爆炸 > 异变 > 末影 > 亡灵 > 温血），
 * 未命中的生物（如村民）视为无样本价值，不可采集。
 */
public enum SampleGroup {

    /** 温血生物（动物/水生） */
    WARM_BLOODED("warm_blooded", 45, 85, 75, 95, 0.8, "life.akaishi.sample_group.warm_blooded"),
    /** 亡灵生物（僵尸/骷髅等） */
    UNDEAD("undead", 55, 70, 60, 85, 1.0, "life.akaishi.sample_group.undead"),
    /** 爆炸性生物（苦力怕） */
    EXPLOSIVE("explosive", 60, 55, 45, 70, 1.1, "life.akaishi.sample_group.explosive"),
    /** 异变体（蜘蛛/史莱姆/烈焰人/守卫者/铁傀儡/悦灵等非自然强敌） */
    ABERRATION("aberration", 65, 45, 45, 70, 1.15, "life.akaishi.sample_group.aberration"),
    /** 末影生物（末影人/末影螨/潜影贝） */
    ENDER("ender", 70, 40, 35, 60, 1.3, "life.akaishi.sample_group.ender"),
    /** 首领级（凋灵）：Boss 基因，仅次龙族 */
    BOSS("boss", 85, 20, 30, 55, 1.5, "life.akaishi.sample_group.boss"),
    /** 龙族（末影龙，顶级样本） */
    DRAGON("dragon", 90, 25, 25, 50, 1.6, "life.akaishi.sample_group.dragon");

    /** 注册 id（NBT/网络共用） */
    private final String id;
    /** 基础纯度（0-100），采集时附加随机波动 */
    private final int basePurity;
    /** 基础采集成功率（0-100），生物血量越低加成越高，封顶 95 */
    private final int baseCollectRate;
    /** 器官适配度区间下限（越强生物越难契合，原生器官固定 100） */
    private final int compatMin;
    /** 器官适配度区间上限 */
    private final int compatMax;
    /**
     * 排斥系数：基因强度 → 身体负担倍数（温血 0.8 最契合，龙 1.6 最难契合）。
     * 参与移植基础排斥与排斥增长速率，由 OrganLinkage 统一消费。
     */
    private final double rejectionFactor;
    /** 显示名翻译键 */
    private final String nameKey;

    SampleGroup(String id, int basePurity, int baseCollectRate, int compatMin, int compatMax,
                double rejectionFactor, String nameKey) {
        this.id = id;
        this.basePurity = basePurity;
        this.baseCollectRate = baseCollectRate;
        this.compatMin = compatMin;
        this.compatMax = compatMax;
        this.rejectionFactor = rejectionFactor;
        this.nameKey = nameKey;
    }

    public int getBaseCollectRate() {
        return baseCollectRate;
    }

    /** 适配度区间下限（0-100） */
    public int getCompatMin() {
        return compatMin;
    }

    /** 适配度区间上限（0-100） */
    public int getCompatMax() {
        return compatMax;
    }

    public String getId() {
        return id;
    }

    public int getBasePurity() {
        return basePurity;
    }

    /** 排斥系数（基因强度 → 身体负担倍数，值越低越契合）——
     *  可由配置 akaishi-common.toml [sample_groups] 列表覆盖（按枚举序数，0 = 内置默认） */
    public double getRejectionFactor() {
        int i = ordinal();
        return i < ModConfig.groupRejectionFactor.length && ModConfig.groupRejectionFactor[i] > 0
                ? ModConfig.groupRejectionFactor[i] : rejectionFactor;
    }

    public String getNameKey() {
        return nameKey;
    }

    /** 按 id 反查分组（NBT 反序列化用） */
    public static SampleGroup byId(String id) {
        for (SampleGroup group : values()) {
            if (group.id.equals(id)) {
                return group;
            }
        }
        return null;
    }

    /** 匹配实体所属分组，无样本价值返回 null */
    public static SampleGroup of(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        if (entity instanceof EnderDragon) {
            return DRAGON;
        }
        // 首领级 Boss：凋灵（mobType 为 UNDEAD）与循声守卫（隐藏于远古城市的顶级强敌）
        if (type == EntityType.WITHER || type == EntityType.WARDEN) {
            return BOSS;
        }
        if (entity instanceof Creeper) {
            return EXPLOSIVE;
        }
        // 异变体：非自然强敌（节肢/软泥/元素/构装/精灵/下界族/灾厄施法者），无法归入常规生态组
        if (type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER || type == EntityType.SLIME
                || type == EntityType.MAGMA_CUBE || type == EntityType.BLAZE
                || type == EntityType.GUARDIAN || type == EntityType.ELDER_GUARDIAN || type == EntityType.IRON_GOLEM
                || type == EntityType.SNOW_GOLEM || type == EntityType.ALLAY
                || type == EntityType.GHAST || type == EntityType.VINDICATOR
                // 下界系（1.16+ 火狱生态）：猪灵/蛮兵（人形贪婪）、疣猪兽（冲撞兽）、女巫（灾厄炼药者）
                || type == EntityType.PIGLIN || type == EntityType.PIGLIN_BRUTE
                || type == EntityType.HOGLIN || type == EntityType.WITCH) {
            return ABERRATION;
        }
        if (entity instanceof EnderMan || entity instanceof Endermite || entity instanceof Shulker) {
            return ENDER;
        }
        // 亡灵：mobType 判定之外，幻翼（夜航幽魂）与僵尸疣猪兽（疣猪兽感染体）同为不死生态
        if (type == EntityType.PHANTOM || type == EntityType.ZOGLIN || entity.getMobType() == MobType.UNDEAD) {
            return UNDEAD;
        }
        if (entity instanceof Animal || entity instanceof AmbientCreature || entity instanceof WaterAnimal) {
            return WARM_BLOODED;
        }
        return null;
    }
}
