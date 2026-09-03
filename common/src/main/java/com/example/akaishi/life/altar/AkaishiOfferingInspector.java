package com.example.akaishi.life.altar;

import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Optional;

/**
 * 母神祭坛的"祭品识别"：读取供奉物 NBT，判定其是否为生命的造物（祭坛接纳标准），
 * 并提取铭刻信息（来源生物/品质/词条数/纯度）供祭坛"低语"反馈。
 * 对外预留扩展：附属可通过新增 Kind 与判定规则扩充可献祭品类（不修改祭坛本体）。
 */
public final class AkaishiOfferingInspector {

    /** 识别结果类别 */
    public enum Kind {
        /** 生命器官（含原生器官，母神垂爱） */
        ORGAN,
        /** 生命样本 */
        SAMPLE,
        /** 基因序列 */
        GENE,
        /** 生命胚胎（母神垂爱之祭品） */
        EMBRYO,
        /** 生命精华 */
        ESSENCE,
        /** 生命灰烬（生命燃烧后的余烬，母神仍予垂怜） */
        ASH,
        /** 无法识别的凡物 */
        UNKNOWN
    }

    /** 结构化识别结果：类别 + 铭刻信息（缺失字段为 0/空串） */
    public record OfferingInfo(Kind kind, String entityId, int quality, int mutations, int purity, boolean accepted) {
    }

    private static final String TYPE_KEY = "life.akaishi.altar.type.";

    private AkaishiOfferingInspector() {
    }

    /** 读取物品 NBT 并归类（只读，不修改物品） */
    public static OfferingInfo inspect(ItemStack stack) {
        if (stack.isEmpty()) {
            return new OfferingInfo(Kind.UNKNOWN, "", 0, 0, 0, false);
        }
        CompoundTag tag = stack.getTag();
        String entityId = tag != null && tag.contains("entity_id") ? tag.getString("entity_id") : "";
        // 器官：NBT 铭刻 品质/来源生物/突变词条列表
        if (stack.getItem() instanceof AkaishiOrganItem) {
            int quality = 1;
            if (tag != null && tag.contains(AkaishiOrganItem.TAG_QUALITY)) {
                quality = switch (tag.getString(AkaishiOrganItem.TAG_QUALITY)) {
                    case "II" -> 2;
                    case "III" -> 3;
                    case "IV" -> 4;
                    default -> 1;
                };
            }
            return new OfferingInfo(Kind.ORGAN, entityId, quality,
                    AkaishiOrganItem.getMutations(stack).size(), 0, true);
        }
        // 生命样本 / 基因序列：共享 来源生物(entity_id) + 纯度(purity) 字段
        int purity = tag != null && tag.contains("purity") ? tag.getInt("purity") : 0;
        if (stack.is(ModItems.lifeSample.get())) {
            return new OfferingInfo(Kind.SAMPLE, entityId, 0, 0, purity, true);
        }
        if (stack.is(ModItems.geneSequence.get())) {
            return new OfferingInfo(Kind.GENE, entityId, 0, 0, purity, true);
        }
        // 生命胚胎：尚未成形的生命，母神垂爱（无需纯度，视为上佳祭品）
        if (stack.is(ModItems.lifeEmbryo.get())) {
            return new OfferingInfo(Kind.EMBRYO, "", 0, 0, 0, true);
        }
        // 生命灰烬：生命燃烧后的余烬，母神仍予垂怜
        if (stack.is(ModItems.lifeAsh.get())) {
            return new OfferingInfo(Kind.ASH, "", 0, 0, 0, true);
        }
        // 生命精华：按注册 id 前缀识别（避免与具体物品类强耦合，便于附属扩展）
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.startsWith("akaishi_life_essence")) {
            return new OfferingInfo(Kind.ESSENCE, "", 0, 0, 0, true);
        }
        return new OfferingInfo(Kind.UNKNOWN, entityId, 0, 0, 0, false);
    }

    /**
     * 祭坛低语：把识别结果转成反馈文本。
     * 首行 = 物品种类（+ 来源生物名），次行 = 铭刻详情（品质/词条数/纯度）。
     */
    public static Component whisper(ItemStack stack) {
        OfferingInfo info = inspect(stack);
        MutableComponent line1 = Component.translatable(TYPE_KEY + info.kind().name().toLowerCase(Locale.ROOT));
        String entityName = entityName(info.entityId());
        if (!entityName.isEmpty()) {
            line1.append("（").append(entityName).append("）");
        }
        MutableComponent line2 = null;
        switch (info.kind()) {
            case ORGAN -> line2 = Component.translatable("life.akaishi.altar.detail.tier",
                            Component.translatable("life.akaishi.organ_tier." + tierKey(info.quality())))
                    .append(" · ")
                    .append(Component.translatable("life.akaishi.altar.detail.mutations", info.mutations()));
            case SAMPLE, GENE -> line2 = Component.translatable("life.akaishi.altar.detail.purity", info.purity());
            default -> { }
        }
        return line2 == null ? line1 : line1.append(Component.literal("\n")).append(line2);
    }

    /** 品质档位 → 翻译 key 后缀（I~IV → i~iv） */
    private static String tierKey(int rank) {
        return switch (rank) {
            case 2 -> "ii";
            case 3 -> "iii";
            case 4 -> "iv";
            default -> "i";
        };
    }

    /** 解析实体 id 为原版生物显示名（不存在返回空串） */
    private static String entityName(String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            return "";
        }
        try {
            ResourceLocation key = ResourceLocation.tryParse(entityId);
            if (key != null) {
                Optional<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(key);
                if (type.isPresent()) {
                    return ((net.minecraft.world.entity.EntityType<?>) type.get()).getDescription().getString();
                }
            }
        } catch (Exception ignored) {
            // 非法实体 id：忽略来源名
        }
        return "";
    }
}
