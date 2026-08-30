package com.example.template.life.sample;

import com.example.template.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 生命样本：采集器从活体生物抽取的遗传物质。
 * NBT 记录来源分组（sample_group）、纯度（purity 0-100）与具体生物（entity_id），
 * 是生命分析台解析为基因序列片段的原料；具体生物用于区分不同生物的器官效果。
 */
public class ChishiLifeSampleItem extends Item {

    public static final String TAG_GROUP = "sample_group";
    public static final String TAG_PURITY = "purity";
    public static final String TAG_ENTITY = "entity_id";

    public ChishiLifeSampleItem(Properties properties) {
        super(properties);
    }

    /** 构造一个生命样本物品 */
    public static ItemStack create(SampleGroup group, int purity, String entityId) {
        ItemStack stack = new ItemStack(ModItems.lifeSample.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_GROUP, group.getId());
        tag.putInt(TAG_PURITY, Math.max(0, Math.min(100, purity)));
        tag.putString(TAG_ENTITY, entityId);
        return stack;
    }

    /**
     * 按分组随机生成样本（采集专用）：纯度偏斜低值——60% 落在 0-49、25% 落在 50-74、
     * 15% 落在 75-100，保证高纯度样本稀有。随机纯度规则统一收敛在此处。
     */
    public static ItemStack createRolled(SampleGroup group, String entityId, RandomSource random) {
        int r = random.nextInt(100);
        int purity;
        if (r < 60) {
            purity = random.nextInt(50);
        } else if (r < 85) {
            purity = 50 + random.nextInt(25);
        } else {
            purity = 75 + random.nextInt(26);
        }
        return create(group, purity, entityId);
    }

    /** 样本来源分组（无 NBT 返回 null） */
    public static SampleGroup getGroup(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? SampleGroup.byId(tag.getString(TAG_GROUP)) : null;
    }

    /** 样本纯度（0-100，无 NBT 默认 0） */
    public static int getPurity(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(TAG_PURITY) : 0;
    }

    /** 设置样本纯度（培养舱提纯调用，钳制 0-100） */
    public static void setPurity(ItemStack stack, int purity) {
        stack.getOrCreateTag().putInt(TAG_PURITY, Math.max(0, Math.min(100, purity)));
    }

    /** 具体生物 id（如 "minecraft:cow"，无 NBT 返回 null） */
    public static String getEntityId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString(TAG_ENTITY) : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        SampleGroup group = getGroup(stack);
        if (group != null) {
            // 来源分组 + 纯度（绿色显示纯度）
            tooltip.add(Component.translatable("gui.template_mod.life_sample.info",
                    Component.translatable(group.getNameKey()), getPurity(stack)));
            // 具体生物（反查原版实体翻译键）
            String entityId = getEntityId(stack);
            if (entityId != null && !entityId.isEmpty()) {
                ResourceLocation rl = ResourceLocation.tryParse(entityId);
                EntityType<?> type = rl != null ? BuiltInRegistries.ENTITY_TYPE.get(rl) : null;
                if (type != null) {
                    tooltip.add(Component.translatable("gui.template_mod.life_sample.entity",
                            Component.translatable(type.getDescriptionId())));
                }
            }
        }
    }
}
