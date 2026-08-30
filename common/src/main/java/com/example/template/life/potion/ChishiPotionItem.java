package com.example.template.life.potion;

import com.example.template.life.body.BodySlot;
import com.example.template.life.body.IPlayerBodyState;
import com.example.template.life.body.PlayerBodyHelper;
import com.example.template.life.organ.ChishiOrganItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 药剂物品（单物品 + 模板 NBT，新增药剂无需新物品）：
 * - 永久药剂：已移植非原生器官适配度永久 +N（N 按生物来源差异化），无副作用
 * - 突破药剂：已移植非原生器官效果 ×1.5，同时放大缺点——槽位排斥永久增加 + 生物差异化随机副作用
 * 副作用强度与样本纯度负相关：纯度越高副作用越轻（由药剂台制作时继承样本纯度与生物来源）。
 */
public class ChishiPotionItem extends Item {

    public static final String TAG_TEMPLATE = "potion_template";
    public static final String TAG_PURITY = "purity";
    /** 生物来源（药剂台制作时从样本继承，生物药剂差异化依据） */
    public static final String TAG_ENTITY = "entity_id";

    /** 突破药剂：效果倍率 */
    public static final double BREAKTHROUGH_MULTIPLIER = 1.5;

    public ChishiPotionItem(Properties properties) {
        super(properties);
    }

    // ===== NBT 读写 =====

    /** 药剂台产出：写入模板 id + 样本纯度 + 生物来源 */
    public static ItemStack create(ItemStack stack, String templateId, int purity, String entityId) {
        stack.getOrCreateTag().putString(TAG_TEMPLATE, templateId);
        stack.getOrCreateTag().putInt(TAG_PURITY, Math.max(0, Math.min(100, purity)));
        if (entityId != null && !entityId.isEmpty()) {
            stack.getOrCreateTag().putString(TAG_ENTITY, entityId);
        }
        return stack;
    }

    public static String getTemplateId(ItemStack stack) {
        return stack.getTag() != null ? stack.getTag().getString(TAG_TEMPLATE) : "";
    }

    public static int getPurity(ItemStack stack) {
        return stack.getTag() != null ? stack.getTag().getInt(TAG_PURITY) : 0;
    }

    public static String getEntityId(ItemStack stack) {
        return stack.getTag() != null ? stack.getTag().getString(TAG_ENTITY) : "";
    }

    // ===== 饮用效果 =====

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        PotionTemplate template = PotionRegistry.get(getTemplateId(stack));
        if (template == null) {
            return InteractionResultHolder.fail(stack);
        }
        IPlayerBodyState state = PlayerBodyHelper.of(player);
        if (state == null) {
            return InteractionResultHolder.fail(stack);
        }
        boolean applied = template.breakthrough()
                ? applyBreakthrough(player, state, getPurity(stack), getEntityId(stack))
                : applyPermanent(player, state, getEntityId(stack));
        if (!applied) {
            return InteractionResultHolder.fail(stack); // 无可作用器官：不消耗
        }
        stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    /** 永久药剂：已移植非原生器官适配度 +N（加成按生物来源差异化，钳制上限） */
    private boolean applyPermanent(Player player, IPlayerBodyState state, String entityId) {
        int bonus = PotionEffectRegistry.compatBonusOf(entityId);
        int affected = 0;
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            if (!(organ.getItem() instanceof ChishiOrganItem) || ChishiOrganItem.isNative(organ)) {
                continue;
            }
            int before = ChishiOrganItem.getCompat(organ);
            ChishiOrganItem.addCompat(organ, bonus);
            if (ChishiOrganItem.getCompat(organ) > before) {
                affected++;
            }
        }
        if (affected == 0) {
            player.displayClientMessage(Component.translatable("message.template_mod.potion.no_target"), true);
            return false;
        }
        player.displayClientMessage(Component.translatable("message.template_mod.potion.permanent", affected,
                bonus), true);
        return true;
    }

    /** 突破药剂：效果 ×1.5 + 槽位排斥增加 + 生物副作用（副作用与纯度负相关） */
    private boolean applyBreakthrough(Player player, IPlayerBodyState state, int purity, String entityId) {
        int affected = 0;
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            if (!(organ.getItem() instanceof ChishiOrganItem) || ChishiOrganItem.isNative(organ)) {
                continue;
            }
            if (ChishiOrganItem.getBoost(organ) >= BREAKTHROUGH_MULTIPLIER) {
                continue; // 已强化，不重复生效
            }
            ChishiOrganItem.setBoost(organ, BREAKTHROUGH_MULTIPLIER);
            // 缺点放大：排斥永久增加（纯度越低涨得越多）
            state.addRejection(slot, rejectionGain(purity));
            affected++;
        }
        if (affected == 0) {
            player.displayClientMessage(Component.translatable("message.template_mod.potion.already_boosted"), true);
            return false;
        }
        player.displayClientMessage(Component.translatable("message.template_mod.potion.breakthrough", affected,
                formatValue(BREAKTHROUGH_MULTIPLIER)), true);
        applySideEffect(player, purity, entityId);
        return true;
    }

    /** 排斥增加量：5 + (100-纯度)/5，纯度 100 → 5，纯度 25 → 20 */
    private static int rejectionGain(int purity) {
        return 5 + (100 - Math.max(0, Math.min(100, purity))) / 5;
    }

    /** 随机 debuff（副作用池按生物来源差异化）：持续时间与等级随纯度降低而增强 */
    private void applySideEffect(Player player, int purity, String entityId) {
        MobEffect[] pool = PotionEffectRegistry.sideEffectsOf(entityId);
        MobEffect effect = pool[player.getRandom().nextInt(pool.length)];
        int seconds = 20 + (100 - Math.max(0, Math.min(100, purity))) / 5;
        int amplifier = purity < 50 ? 1 : 0;
        player.addEffect(new MobEffectInstance(effect, seconds * 20, amplifier, false, false));
        player.displayClientMessage(Component.translatable("message.template_mod.potion.side_effect",
                Component.translatable(effect.getDescriptionId()), seconds), true);
    }

    private static String formatValue(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.1f", value);
    }

    // ===== 显示 =====

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        String templateId = getTemplateId(stack);
        PotionTemplate template = PotionRegistry.get(templateId);
        if (template == null) {
            tooltip.add(Component.translatable("gui.template_mod.potion.undefined"));
            return;
        }
        tooltip.add(Component.translatable("gui.template_mod.potion.type",
                Component.translatable(template.nameKey())));
        String entityId = getEntityId(stack);
        if (!entityId.isEmpty()) {
            // 生物来源（EntityType 自带翻译）
            EntityType.byString(entityId).ifPresent(type ->
                    tooltip.add(Component.translatable("gui.template_mod.potion.source",
                            type.getDescription())));
        }
        tooltip.add(Component.translatable("gui.template_mod.potion.purity", getPurity(stack)));
        tooltip.add(Component.translatable("gui.template_mod.potion.hint"));
    }
}
