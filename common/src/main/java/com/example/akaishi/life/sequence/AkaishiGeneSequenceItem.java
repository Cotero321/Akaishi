package com.example.akaishi.life.sequence;

import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.organ.QualityTier;
import com.example.akaishi.life.sample.AkaishiLifeSampleItem;
import com.example.akaishi.life.sample.SampleGroup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 基因序列片段：纯度 25 及以上的生命样本在生命分析台解构的产物。
 * NBT 记录来源分组（gene_source）、具体生物（entity_id）与纯度（purity，继承样本纯度），
 * 是生命结构台解析为完整基因图谱（器官）的原料——结构台造器官时纯度会随机损耗 0-20。
 */
public class AkaishiGeneSequenceItem extends Item {

    public static final String TAG_GROUP = "gene_source";
    public static final String TAG_ENTITY = "entity_id";
    /** 纯度（继承自样本，0-100）：决定结构台产出器官的初始品质档位 */
    public static final String TAG_PURITY = "purity";

    public AkaishiGeneSequenceItem(Properties properties) {
        super(properties);
    }

    /** 从生命样本构造序列片段（复制分组、生物与纯度信息） */
    public static ItemStack createFromSample(ItemStack sample) {
        ItemStack stack = new ItemStack(ModItems.geneSequence.get());
        CompoundTag tag = stack.getOrCreateTag();
        SampleGroup group = AkaishiLifeSampleItem.getGroup(sample);
        if (group != null) {
            tag.putString(TAG_GROUP, group.getId());
        }
        String entityId = AkaishiLifeSampleItem.getEntityId(sample);
        if (entityId != null && !entityId.isEmpty()) {
            tag.putString(TAG_ENTITY, entityId);
        }
        tag.putInt(TAG_PURITY, AkaishiLifeSampleItem.getPurity(sample));
        return stack;
    }

    /** 来源分组（无 NBT 返回 null） */
    public static SampleGroup getGroup(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? SampleGroup.byId(tag.getString(TAG_GROUP)) : null;
    }

    /** 具体生物 id（如 "minecraft:cow"，无 NBT 返回 null） */
    public static String getEntityId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_ENTITY) ? tag.getString(TAG_ENTITY) : null;
    }

    /** 纯度（0-100），无 NBT 返回 0 */
    public static int getPurity(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_PURITY) ? tag.getInt(TAG_PURITY) : 0;
    }

    /** 纯度 → 品质档位：100=III、50-99=II、<50=I（与器官完整度映射一致） */
    public static QualityTier tierOf(int purity) {
        if (purity >= 100) {
            return QualityTier.III;
        }
        return purity >= 50 ? QualityTier.II : QualityTier.I;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        SampleGroup group = getGroup(stack);
        if (group != null) {
            tooltip.add(Component.translatable("gui.akaishi.gene_sequence.group",
                    Component.translatable(group.getNameKey())));
        }
        String entityId = getEntityId(stack);
        if (entityId != null && !entityId.isEmpty()) {
            ResourceLocation rl = ResourceLocation.tryParse(entityId);
            EntityType<?> type = rl != null ? BuiltInRegistries.ENTITY_TYPE.get(rl) : null;
            if (type != null) {
                tooltip.add(Component.translatable("gui.akaishi.gene_sequence.entity",
                        Component.translatable(type.getDescriptionId())));
            }
        }
        // 纯度 + 对应品质档位（结构台造器官时纯度会随机损耗 0-20）
        int purity = getPurity(stack);
        tooltip.add(Component.translatable("gui.akaishi.gene_sequence.purity", purity));
        tooltip.add(Component.translatable("gui.akaishi.gene_sequence.tier",
                Component.translatable("life.akaishi.organ_tier." + tierOf(purity).name().toLowerCase())));
        tooltip.add(Component.translatable("gui.akaishi.gene_sequence.hint"));
    }
}
