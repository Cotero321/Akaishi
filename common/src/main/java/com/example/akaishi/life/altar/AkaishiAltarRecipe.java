package com.example.akaishi.life.altar;

import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import com.example.akaishi.life.organ.QualityTier;
import com.example.akaishi.life.potion.AkaishiPotionItem;
import com.example.akaishi.life.sequence.AkaishiGeneSequenceItem;
import com.example.akaishi.multiblock.AkaishiGoatAltarTiersStructure;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 母神祭坛「生命融合仪式」配方表（静态表，共 5 条）。
 * <p>五条配方靠<b>巨坛供奉槽祭品</b>区分，判定无歧义：
 * <ul>
 *   <li>旧配方：赤石锭 → 生命融合锭（外圈 生命胚胎/生命灰烬/基因序列/浓缩赤石精华块，各 2 座），不限等级</li>
 *   <li>新四套：生命融合锭 → 四件禁断饰品（外圈 主题器官/基础素材/基因序列/药剂，各 2 座），需三级祭坛</li>
 * </ul>
 * <p>所有谓词与产物均以 lambda 延迟求值，避免类初始化早于 {@code ModItems.register()} 时读到空 supplier。
 */
public record AkaishiAltarRecipe(Predicate<ItemStack> hostOffering,
                                 List<Requirement> outer,
                                 Supplier<Item> output,
                                 boolean advanced,
                                 @Nullable String ritualName) {

    /** 外圈子祭坛总座数（配方须恰好占满，多一件即不成立） */
    public static final int OUTER_SLOTS = 8;
    /** 器官祭品品质门槛：≥ III 级（D136）；同时天然排除品质恒为 I 的原生壳与污染壳 */
    public static final QualityTier MIN_ORGAN_TIER = QualityTier.III;
    /** 基因序列纯度门槛：满纯度 100（D139） */
    public static final int MIN_GENE_PURITY = 100;
    /** 药剂纯度门槛：满纯度 100（D154） */
    public static final int MIN_POTION_PURITY = 100;

    /** 外圈一类祭品：{@code test} 命中的子祭坛须恰好 {@code count} 座 */
    public record Requirement(Predicate<ItemStack> test, int count) {

        public static Requirement of(int count, Predicate<ItemStack> test) {
            return new Requirement(test, count);
        }
    }

    /** 新配方目录（顺序无关，匹配靠主祭品区分） */
    private static final AkaishiAltarRecipe LEGACY = new AkaishiAltarRecipe(
            stack -> stack.is(ModItems.akaishiIngot.get()),
            List.of(
                    Requirement.of(2, stack -> stack.is(ModItems.lifeEmbryo.get())),
                    Requirement.of(2, stack -> stack.is(ModItems.lifeAsh.get())),
                    Requirement.of(2, stack -> stack.is(ModItems.geneSequence.get())
                            && AkaishiGeneSequenceItem.getPurity(stack) > AkaishiAltarRitual.MIN_GENE_PURITY),
                    Requirement.of(2, stack -> stack.is(ModBlocks.CHISHI_ESSENCE_BLOCK.get().asItem()))),
            () -> ModItems.lifeFusionIngot.get(),
            false,
            null);

    private static final AkaishiAltarRecipe LIFE_TOUCH = new AkaishiAltarRecipe(
            stack -> stack.is(ModItems.lifeFusionIngot.get()),
            List.of(
                    organ(1, BodySlot.LEFT_ARM),
                    organ(1, BodySlot.RIGHT_ARM),
                    basic(2, ModItems.lifeEmbryo),
                    gene(2),
                    potion(2)),
            () -> ModItems.lifeTouch.get(),
            true,
            "message.akaishi.altar.ritual.life_touch");

    private static final AkaishiAltarRecipe CUB_HEART = new AkaishiAltarRecipe(
            stack -> stack.is(ModItems.lifeFusionIngot.get()),
            List.of(
                    organ(2, BodySlot.HEART),
                    basic(2, ModItems.lifeEmbryo),
                    gene(2),
                    potion(2)),
            () -> ModItems.cubHeart.get(),
            true,
            "message.akaishi.altar.ritual.cub_heart");

    private static final AkaishiAltarRecipe MOTHER_SEAL = new AkaishiAltarRecipe(
            stack -> stack.is(ModItems.lifeFusionIngot.get()),
            List.of(
                    organ(2, BodySlot.EYE),
                    basic(2, ModItems.lifeAsh),
                    gene(2),
                    potion(2)),
            () -> ModItems.motherSeal.get(),
            true,
            "message.akaishi.altar.ritual.mother_seal");

    private static final AkaishiAltarRecipe FERTILITY_RING = new AkaishiAltarRecipe(
            stack -> stack.is(ModItems.lifeFusionIngot.get()),
            List.of(
                    organ(2, BodySlot.VISCERA),
                    basic(2, ModItems.lifeAsh),
                    gene(2),
                    potion(2)),
            () -> ModItems.fertilityRing.get(),
            true,
            "message.akaishi.altar.ritual.fertility_ring");

    /** 全部配方（旧配方在首位，保证旧仪式优先命中） */
    public static List<AkaishiAltarRecipe> all() {
        return List.of(LEGACY, LIFE_TOUCH, CUB_HEART, MOTHER_SEAL, FERTILITY_RING);
    }

    /** 外圈要求件数之和：须等于 {@link #OUTER_SLOTS} 才能占满外圈 */
    public int outerCount() {
        int total = 0;
        for (Requirement requirement : outer) {
            total += requirement.count();
        }
        return total;
    }

    /** 结构等级门槛：新配方需三级祭坛（读配置，D133），旧配方仅要求成型 */
    public int requiredTier() {
        return advanced ? ModConfig.altarNewRecipeTierRequired : AkaishiGoatAltarTiersStructure.FORMED_TIER;
    }

    /** 蓄能阈值：新/旧各自读配置，玩家热改配置后下一轮即生效 */
    public long progressMax() {
        return advanced ? ModConfig.altarNewRecipeProgressMax : ModConfig.altarLegacyProgressMax;
    }

    /**
     * 主题器官祭品：部位命中给定之一、品质 ≥ {@link #MIN_ORGAN_TIER}、
     * 且既非乱码（D56 侵蚀产物）也非原生壳（D25 污染空壳）。
     */
    private static Requirement organ(int count, BodySlot... slots) {
        return Requirement.of(count, stack -> {
            BodySlot actual = AkaishiOrganItem.slotOf(stack);
            if (actual == null) {
                return false;
            }
            boolean slotMatch = false;
            for (BodySlot slot : slots) {
                if (slot == actual) {
                    slotMatch = true;
                    break;
                }
            }
            if (!slotMatch) {
                return false;
            }
            QualityTier tier = AkaishiOrganItem.getTier(stack);
            return tier != null
                    && tier.ordinal() >= MIN_ORGAN_TIER.ordinal()
                    && !AkaishiOrganItem.isNative(stack)
                    && !AkaishiOrganItem.isCorrupted(stack);
        });
    }

    /** 基础素材祭品：指定物品，任意纯度/品质 */
    private static Requirement basic(int count, Supplier<Item> item) {
        return Requirement.of(count, stack -> stack.is(item.get()));
    }

    /** 基因序列祭品：满纯度 100（D139） */
    private static Requirement gene(int count) {
        return Requirement.of(count, stack -> stack.is(ModItems.geneSequence.get())
                && AkaishiGeneSequenceItem.getPurity(stack) >= MIN_GENE_PURITY);
    }

    /** 药剂祭品：满纯度 100（D154） */
    private static Requirement potion(int count) {
        return Requirement.of(count, stack -> stack.getItem() instanceof AkaishiPotionItem
                && AkaishiPotionItem.getPurity(stack) >= MIN_POTION_PURITY);
    }
}
