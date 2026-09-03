package com.example.akaishi.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 赤石装备升级系统。
 * 分工：
 * - 基础升级（5 属性）由赤石装备打造器提供：打造时最多分配 4 个升级点（可重复投点），
 *   装备 NBT 记录各属性次数，效果由 Forge 平台动态附加。
 * - 高级升级（4 种特殊能力 + 槽位拓展）由赤红升级台提供：消耗升级模板 + 升级槽位 + 能量。
 * 装备 tooltip 通过 appendTooltip 展示已生效升级与剩余槽位。
 */
public final class AkaishiUpgradeHelper {

    public static final String TAG = "AkaishiGear";
    public static final String TAG_SLOTS = "Slots";
    public static final String TAG_ATTACK_SPEED = "AttackSpeed";
    public static final String TAG_ATTACK_DAMAGE = "AttackDamage";
    public static final String TAG_EFFICIENCY = "Efficiency";
    public static final String TAG_MAX_HEALTH = "MaxHealth";
    public static final String TAG_ARMOR = "Armor";
    public static final String TAG_TOUGHNESS = "Toughness";
    public static final String TAG_ABILITY_LIFESTEAL = "AbilityLifesteal";
    public static final String TAG_ABILITY_KNOCKBACK = "AbilityKnockback";
    public static final String TAG_ABILITY_SPEED = "AbilitySpeed";
    public static final String TAG_ABILITY_FIRE_RESIST = "AbilityFireResist";
    public static final String TAG_ABILITY_BLAST_PROTECT = "AbilityBlastProtect";
    public static final String TAG_ABILITY_FALL_PROTECT = "AbilityFallProtect";
    // 部位专属能力（升级台，仅可升级 1 次）
    public static final String TAG_ABILITY_HASTE = "AbilityHaste";
    public static final String TAG_ABILITY_WATER_BREATHING = "AbilityWaterBreathing";
    public static final String TAG_ABILITY_RESISTANCE = "AbilityResistance";
    public static final String TAG_ABILITY_HIT_REGEN = "AbilityHitRegen";
    public static final String TAG_ABILITY_JUMP_BOOST = "AbilityJumpBoost";
    public static final String TAG_ABILITY_SWIM_SPEED = "AbilitySwimSpeed";
    public static final String TAG_ABILITY_SLOW_FALLING = "AbilitySlowFalling";
    public static final String TAG_ABILITY_BOOTS_SPEED = "AbilityBootsSpeed";
    // 工具/剑专属能力
    public static final String TAG_ABILITY_AREA_BREAK = "AbilityAreaBreak";
    public static final String TAG_ABILITY_DROP_ESSENCE = "AbilityDropEssence";

    /** 初始升级槽位 */
    public static final int INITIAL_SLOTS = 4;
    /** 打造器每件装备可分配的基础升级点数 */
    public static final int FORGE_UPGRADE_POINTS = 4;
    /** 打造器每次基础升级额外消耗的赤能源 */
    public static final long ENERGY_PER_BASE_UPGRADE = 10_000_000L;

    private AkaishiUpgradeHelper() {
    }

    /** 基础升级类型（打造器分配） */
    public enum UpgradeType {
        ATTACK_SPEED(Attributes.ATTACK_SPEED, 0.15, TAG_ATTACK_SPEED, "gui.akaishi.tooltip.attack_speed", "gui.akaishi.upgrade.attack_speed"),
        ATTACK_DAMAGE(Attributes.ATTACK_DAMAGE, 1.0, TAG_ATTACK_DAMAGE, "gui.akaishi.tooltip.attack_damage", "gui.akaishi.upgrade.attack_damage"),
        EFFICIENCY(null, 20.0, TAG_EFFICIENCY, "gui.akaishi.tooltip.efficiency", "gui.akaishi.upgrade.efficiency"),
        MAX_HEALTH(Attributes.MAX_HEALTH, 2.0, TAG_MAX_HEALTH, "gui.akaishi.tooltip.max_health", "gui.akaishi.upgrade.max_health"),
        ARMOR(Attributes.ARMOR, 1.0, TAG_ARMOR, "gui.akaishi.tooltip.armor", "gui.akaishi.upgrade.armor"),
        TOUGHNESS(Attributes.ARMOR_TOUGHNESS, 1.0, TAG_TOUGHNESS, "gui.akaishi.tooltip.toughness", "gui.akaishi.upgrade.toughness");

        /** 对应实体属性；效率无属性（挖掘速度由事件处理），此处为 null */
        public final Attribute attribute;
        public final double amount;
        public final String tagKey;
        public final String tooltipKey;
        /** 打造器按钮上的翻译 key */
        public final String buttonKey;

        UpgradeType(Attribute attribute, double amount, String tagKey, String tooltipKey, String buttonKey) {
            this.attribute = attribute;
            this.amount = amount;
            this.tagKey = tagKey;
            this.tooltipKey = tooltipKey;
            this.buttonKey = buttonKey;
        }
    }

    /** 装备类别（用于非护甲的特殊能力限定） */
    public enum GearKind {
        /** 挖掘工具（铲/斧/镐） */
        TOOL,
        /** 剑 */
        SWORD
    }

    /** 特殊能力类型（升级台高级升级，消耗槽位 + 模板） */
    public enum SpecialAbility {
        // 通用能力：任意赤石装备可升级，最多 3 级
        LIFESTEAL(null, null, 3, TAG_ABILITY_LIFESTEAL, "gui.akaishi.tooltip.ability.lifesteal", "gui.akaishi.upgrade.ability_lifesteal"),
        KNOCKBACK(null, null, 3, TAG_ABILITY_KNOCKBACK, "gui.akaishi.tooltip.ability.knockback", "gui.akaishi.upgrade.ability_knockback"),
        SPEED(null, null, 3, TAG_ABILITY_SPEED, "gui.akaishi.tooltip.ability.speed", "gui.akaishi.upgrade.ability_speed"),
        FIRE_RESIST(null, null, 3, TAG_ABILITY_FIRE_RESIST, "gui.akaishi.tooltip.ability.fire_resist", "gui.akaishi.upgrade.ability_fire"),
        BLAST_PROTECT(null, null, 3, TAG_ABILITY_BLAST_PROTECT, "gui.akaishi.tooltip.ability.blast_protect", "gui.akaishi.upgrade.ability_blast"),
        FALL_PROTECT(null, null, 3, TAG_ABILITY_FALL_PROTECT, "gui.akaishi.tooltip.ability.fall_protect", "gui.akaishi.upgrade.ability_fall"),
        // 部位专属能力：仅对应护甲部位可升级，只能升级 1 次
        HASTE(null, EquipmentSlot.HEAD, 1, TAG_ABILITY_HASTE, "gui.akaishi.tooltip.ability.haste", "gui.akaishi.upgrade.ability_haste"),
        WATER_BREATHING(null, EquipmentSlot.HEAD, 1, TAG_ABILITY_WATER_BREATHING, "gui.akaishi.tooltip.ability.water_breathing", "gui.akaishi.upgrade.ability_water_breathing"),
        RESISTANCE(null, EquipmentSlot.CHEST, 1, TAG_ABILITY_RESISTANCE, "gui.akaishi.tooltip.ability.resistance", "gui.akaishi.upgrade.ability_resistance"),
        HIT_REGEN(null, EquipmentSlot.CHEST, 1, TAG_ABILITY_HIT_REGEN, "gui.akaishi.tooltip.ability.hit_regen", "gui.akaishi.upgrade.ability_hit_regen"),
        JUMP_BOOST(null, EquipmentSlot.LEGS, 1, TAG_ABILITY_JUMP_BOOST, "gui.akaishi.tooltip.ability.jump_boost", "gui.akaishi.upgrade.ability_jump_boost"),
        SWIM_SPEED(null, EquipmentSlot.LEGS, 1, TAG_ABILITY_SWIM_SPEED, "gui.akaishi.tooltip.ability.swim_speed", "gui.akaishi.upgrade.ability_swim_speed"),
        SLOW_FALLING(null, EquipmentSlot.FEET, 1, TAG_ABILITY_SLOW_FALLING, "gui.akaishi.tooltip.ability.slow_falling", "gui.akaishi.upgrade.ability_slow_falling"),
        BOOTS_SPEED(null, EquipmentSlot.FEET, 1, TAG_ABILITY_BOOTS_SPEED, "gui.akaishi.tooltip.ability.boots_speed", "gui.akaishi.upgrade.ability_boots_speed"),
        // 工具/剑专属能力
        AREA_BREAK(GearKind.TOOL, null, 1, TAG_ABILITY_AREA_BREAK, "gui.akaishi.tooltip.ability.area_break", "gui.akaishi.upgrade.ability_area_break"),
        DROP_ESSENCE(GearKind.SWORD, null, 3, TAG_ABILITY_DROP_ESSENCE, "gui.akaishi.tooltip.ability.drop_essence", "gui.akaishi.upgrade.ability_drop_essence");

        /** 装备类别（工具/剑）；为 null 时按 slot 部位判断 */
        public final GearKind kind;
        /** 适用部位；null 表示通用或按 kind 判断 */
        public final EquipmentSlot slot;
        /** 等级上限（部位/工具专属能力为 1，通用与剑掉落为 3） */
        public final int maxLevel;
        public final String tagKey;
        public final String tooltipKey;
        /** 升级台按钮上的翻译 key */
        public final String buttonKey;

        SpecialAbility(GearKind kind, EquipmentSlot slot, int maxLevel, String tagKey, String tooltipKey, String buttonKey) {
            this.kind = kind;
            this.slot = slot;
            this.maxLevel = maxLevel;
            this.tagKey = tagKey;
            this.tooltipKey = tooltipKey;
            this.buttonKey = buttonKey;
        }

        /** 该能力是否适用于给定装备 */
        public boolean isApplicable(ItemStack gear) {
            if (kind == GearKind.TOOL) {
                return gear.getItem() instanceof AkaishiShovelItem
                        || gear.getItem() instanceof AkaishiAxeItem
                        || gear.getItem() instanceof AkaishiPickaxeItem;
            }
            if (kind == GearKind.SWORD) {
                return gear.getItem() instanceof AkaishiSwordItem;
            }
            if (slot == null) {
                return true;
            }
            return gear.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == slot;
        }
    }

    public static CompoundTag gearTag(ItemStack stack) {
        return stack.getOrCreateTagElement(TAG);
    }

    /** 是否为赤石装备（携带升级标签） */
    public static boolean isAkaishiGear(ItemStack stack) {
        return stack.getTagElement(TAG) != null;
    }

    /** 物品是否为赤石装备（护甲/剑/工具，含生命融合护甲） */
    public static boolean isAkaishiEquipment(ItemStack stack) {
        return stack.getItem() instanceof AkaishiArmorItem || stack.getItem() instanceof AkaishiSwordItem
                || stack.getItem() instanceof AkaishiShovelItem || stack.getItem() instanceof AkaishiAxeItem
                || stack.getItem() instanceof AkaishiPickaxeItem
                || stack.getItem() instanceof AkaishiLifeFusionArmorItem;
    }

    /** 是否支持效率升级的挖掘类工具（铲/斧/镐） */
    public static boolean isEfficiencyTool(ItemStack stack) {
        return stack.getItem() instanceof AkaishiShovelItem
                || stack.getItem() instanceof AkaishiAxeItem
                || stack.getItem() instanceof AkaishiPickaxeItem;
    }

    /**
     * 惰性初始化：赤石装备类物品缺标签时自动补标签（兼容 /give、创造模式直接取用，
     * 以及锻造前的旧装备）。幂等：已初始化装备不会被清空。
     */
    public static void ensureGear(ItemStack stack) {
        if (isAkaishiEquipment(stack) && !isAkaishiGear(stack)) {
            initGear(stack);
        }
    }

    /** 剩余升级槽位 */
    public static int getSlots(ItemStack stack) {
        return gearTag(stack).getInt(TAG_SLOTS);
    }

    /** 某升级项当前计数 */
    public static int getCount(ItemStack stack, String key) {
        return gearTag(stack).getInt(key);
    }

    /** 初始化一件赤石装备（锻造产出时调用）：初始 4 槽，清空所有升级 */
    public static void initGear(ItemStack stack) {
        CompoundTag tag = gearTag(stack);
        tag.putInt(TAG_SLOTS, INITIAL_SLOTS);
        for (UpgradeType type : UpgradeType.values()) {
            tag.remove(type.tagKey);
        }
        for (SpecialAbility ability : SpecialAbility.values()) {
            tag.remove(ability.tagKey);
        }
    }

    /** 打造器：为装备分配 1 次基础升级（点数由打造器管理，此处仅累加计数） */
    public static void addBaseUpgrade(ItemStack stack, UpgradeType type) {
        CompoundTag tag = gearTag(stack);
        tag.putInt(type.tagKey, tag.getInt(type.tagKey) + 1);
    }

    /**
     * 升级台：应用 1 次特殊能力。成功返回 true（等级 +1 并消耗 1 槽）；失败不消耗。
     */
    public static boolean applyAbility(ItemStack stack, SpecialAbility ability) {
        if (stack.isEmpty() || !isAkaishiGear(stack)) {
            return false;
        }
        CompoundTag tag = gearTag(stack);
        int slots = tag.getInt(TAG_SLOTS);
        if (slots <= 0) {
            return false;
        }
        int level = tag.getInt(ability.tagKey);
        if (level >= ability.maxLevel) {
            return false;
        }
        tag.putInt(ability.tagKey, level + 1);
        tag.putInt(TAG_SLOTS, slots - 1);
        return true;
    }

    /** 生成装备 tooltip：已生效的基础升级、特殊能力与剩余槽位 */
    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        if (!isAkaishiGear(stack)) {
            return;
        }
        for (UpgradeType type : UpgradeType.values()) {
            int count = getCount(stack, type.tagKey);
            if (count > 0) {
                tooltip.add(Component.translatable(type.tooltipKey, trimNum(count * type.amount)));
            }
        }
        for (SpecialAbility ability : SpecialAbility.values()) {
            int level = getCount(stack, ability.tagKey);
            if (level > 0) {
                tooltip.add(Component.translatable(ability.tooltipKey, level));
            }
        }
        tooltip.add(Component.translatable("gui.akaishi.tooltip.slots", getSlots(stack)));
    }

    /** 数字显示：整数不带小数（2.0 → 2），小数保留 1 位 */
    private static String trimNum(double d) {
        if (Math.abs(d - Math.round(d)) < 0.05) {
            return String.valueOf((long) Math.round(d));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", d);
    }
}
