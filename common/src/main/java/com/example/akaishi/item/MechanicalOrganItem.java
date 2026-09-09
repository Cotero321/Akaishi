package com.example.akaishi.item;

import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.IInstallableOrgan;
import com.example.akaishi.life.mechanical.MechanicalAssembledStats;
import com.example.akaishi.life.mechanical.MechanicalIntegrationService;
import com.example.akaishi.life.mechanical.MechanicalMaterial;
import com.example.akaishi.life.mechanical.MechanicalOrganType;
import com.example.akaishi.life.mechanical.MechanicalProperty;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.List;

/**
 * 机械器官成品物品。
 * <p>
 * 由组装加工台将四个加工部件组装而成，存储最终聚合属性。
 * 渲染由 BEWLR 根据四个材料的纹理动态合成最终外观。
 */
public class MechanicalOrganItem extends Item implements IInstallableOrgan {

    private static final DecimalFormat DF = new DecimalFormat("#.##");

    // NBT 键
    public static final String TAG_ORGAN_TYPE = "mech_organ_type";
    public static final String TAG_OVERALL_MULTIPLIER = "mech_om";
    public static final String TAG_HEALTH = "mech_hp";
    public static final String TAG_ATTACK_DAMAGE = "mech_ad";
    public static final String TAG_ATTACK_SPEED = "mech_as";
    public static final String TAG_MOVEMENT_SPEED = "mech_ms";
    /** 四个材料 ID，用于渲染合成 */
    public static final String TAG_MATERIALS = "mech_materials";
    /** 协同加成键列表 */
    public static final String TAG_SYNERGIES = "mech_synergies";

    public MechanicalOrganItem(Properties properties) {
        super(properties);
    }

    // ==================== 工厂方法 ====================

    /** 创建机械器官成品 ItemStack */
    public static ItemStack create(MechanicalAssembledStats stats,
                                   List<String> materialIds) {
        ItemStack stack = new ItemStack(getOrganItem(stats.organType()).get());
        CompoundTag tag = stack.getOrCreateTag();

        tag.putString(TAG_ORGAN_TYPE, stats.organType().name());
        tag.putDouble(TAG_OVERALL_MULTIPLIER, stats.overallMultiplier());
        tag.putDouble(TAG_HEALTH, stats.health());
        tag.putDouble(TAG_ATTACK_DAMAGE, stats.attackDamage());
        tag.putDouble(TAG_ATTACK_SPEED, stats.attackSpeed());
        tag.putDouble(TAG_MOVEMENT_SPEED, stats.movementSpeed());

        // 存储材料 ID 列表
        tag.putInt(TAG_MATERIALS + "_count", materialIds.size());
        for (int i = 0; i < materialIds.size(); i++) {
            tag.putString(TAG_MATERIALS + "_" + i, materialIds.get(i));
        }

        // 协同加成
        List<String> synergies = stats.synergyBonuses();
        tag.putInt(TAG_SYNERGIES + "_count", synergies.size());
        for (int i = 0; i < synergies.size(); i++) {
            tag.putString(TAG_SYNERGIES + "_" + i, synergies.get(i));
        }

        return stack;
    }

    /** 根据器官类型获取对应物品的注册器引用 */
    private static RegistrySupplier<Item> getOrganItem(MechanicalOrganType type) {
        return switch (type) {
            case EYE -> AkaishiMechanicalItems.mechanicalEye;
            case HEART -> AkaishiMechanicalItems.mechanicalHeart;
            case LUNG -> AkaishiMechanicalItems.mechanicalLungs;
            case VISCERA -> AkaishiMechanicalItems.mechanicalViscera;
            case KIDNEY -> AkaishiMechanicalItems.mechanicalKidneys;
            case LEFT_ARM -> AkaishiMechanicalItems.mechanicalLeftArm;
            case RIGHT_ARM -> AkaishiMechanicalItems.mechanicalRightArm;
            case LEFT_LEG -> AkaishiMechanicalItems.mechanicalLeftLeg;
            case RIGHT_LEG -> AkaishiMechanicalItems.mechanicalRightLeg;
        };
    }

    // ==================== 读取器 ====================

    @Nullable
    public static MechanicalOrganType getOrganType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ORGAN_TYPE)) return null;
        try { return MechanicalOrganType.valueOf(tag.getString(TAG_ORGAN_TYPE)); }
        catch (IllegalArgumentException e) { return null; }
    }

    /** 获取用于渲染的四个材料 ID */
    public static List<String> getMaterialIds(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return List.of();
        int count = tag.getInt(TAG_MATERIALS + "_count");
        if (count <= 0) return List.of();
        java.util.ArrayList<String> ids = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(tag.getString(TAG_MATERIALS + "_" + i));
        }
        return ids;
    }

    /** 从成品 NBT 还原聚合属性（缺器官类型时返回 null）；协同/效果列表不还原（生效层只用五维） */
    @Nullable
    public static MechanicalAssembledStats getStats(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return null;
        MechanicalOrganType type = getOrganType(stack);
        if (type == null) return null;
        return new MechanicalAssembledStats(type,
                tag.getDouble(TAG_OVERALL_MULTIPLIER),
                tag.getDouble(TAG_HEALTH),
                tag.getDouble(TAG_ATTACK_DAMAGE),
                tag.getDouble(TAG_ATTACK_SPEED),
                tag.getDouble(TAG_MOVEMENT_SPEED),
                List.of(), List.of());
    }

    // ==================== 可安装器官契约 ====================

    /** 机械器官类型 → 躯体槽位（注意 LUNG/KIDNEY 与 BodySlot 的 LUNGS/KIDNEYS 复数差异） */
    @Override
    public BodySlot bodySlot(ItemStack stack) {
        MechanicalOrganType type = getOrganType(stack);
        if (type == null) return null;
        return switch (type) {
            case EYE -> BodySlot.EYE;
            case HEART -> BodySlot.HEART;
            case LUNG -> BodySlot.LUNGS;
            case VISCERA -> BodySlot.VISCERA;
            case KIDNEY -> BodySlot.KIDNEYS;
            case LEFT_ARM -> BodySlot.LEFT_ARM;
            case RIGHT_ARM -> BodySlot.RIGHT_ARM;
            case LEFT_LEG -> BodySlot.LEFT_LEG;
            case RIGHT_LEG -> BodySlot.RIGHT_LEG;
        };
    }

    /** 安装初始整合度：由首个材料决定（生命陶瓷 20 / 灵能复合 10 / 其余 0） */
    @Override
    public int initialIntegration(ItemStack stack) {
        List<String> materials = getMaterialIds(stack);
        if (materials.isEmpty()) return 0;
        MechanicalMaterial material = MechanicalMaterial.get(materials.get(0));
        return material != null ? MechanicalIntegrationService.getInitialIntegration(material) : 0;
    }

    // ==================== 覆写 ====================

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag tag = stack.getTag();
        if (tag == null) return;

        // 器官类型
        MechanicalOrganType organType = getOrganType(stack);
        if (organType != null) {
            tooltip.add(Component.translatable("tooltip.akaishi.mechanical.organ_type",
                    Component.translatable("mechanical.organ." + organType.name().toLowerCase())));
        }

        // 五维属性
        double om = tag.getDouble(TAG_OVERALL_MULTIPLIER);
        double hp = tag.getDouble(TAG_HEALTH);
        double ad = tag.getDouble(TAG_ATTACK_DAMAGE);
        double as = tag.getDouble(TAG_ATTACK_SPEED);
        double ms = tag.getDouble(TAG_MOVEMENT_SPEED);

        tooltip.add(Component.translatable("tooltip.akaishi.mechanical.om",
                Component.literal(DF.format(om))));
        tooltip.add(Component.translatable("tooltip.akaishi.mechanical.hp",
                Component.literal(DF.format(hp))));
        tooltip.add(Component.translatable("tooltip.akaishi.mechanical.ad",
                Component.literal(DF.format(ad))));
        tooltip.add(Component.translatable("tooltip.akaishi.mechanical.as",
                Component.literal(DF.format(as))));
        tooltip.add(Component.translatable("tooltip.akaishi.mechanical.ms",
                Component.literal(DF.format(ms))));

        // 协同加成
        int synergyCount = tag.getInt(TAG_SYNERGIES + "_count");
        for (int i = 0; i < synergyCount; i++) {
            String key = tag.getString(TAG_SYNERGIES + "_" + i);
            if (!key.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.akaishi.mechanical.synergy",
                        Component.translatable(key)));
            }
        }
    }
}