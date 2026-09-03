package com.example.akaishi.life.potion;

import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.body.PlayerBodyHelper;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 药剂物品（单物品 + 模板 NBT，新增药剂无需新物品）：
 * - 永久药剂：将该生物基因型吸收进身体 → 该来源所有器官"有效适配度"+N（纯度五档 2/4/6/8/10），
 *   每种基因型只能吸收一次，最多同时 4 种；无副作用，可由基因管理器卸载（卸载后可再次吸收）
 * - 突破药剂：对已吸收的基因型发动临时激活（30 分钟，同一时间最多 1 种，到期/卸载后可再次激活）：
 *   该来源器官基础数值 +10%~40%（纯度四档）+ 额外适配 +2~+8，负基础值属性词条激活期内暂时失效
 * 数值随样本纯度分档（由药剂台制作时继承样本纯度与生物来源）。
 */
public class AkaishiPotionItem extends Item {

    public static final String TAG_TEMPLATE = "potion_template";
    public static final String TAG_PURITY = "purity";
    /** 生物来源（药剂台制作时从样本继承，生物药剂差异化依据） */
    public static final String TAG_ENTITY = "entity_id";

    /** 突破药剂激活时长：30 分钟（tick） */
    public static final long BREAKTHROUGH_DURATION_TICKS = 30L * 60L * 20L;

    public AkaishiPotionItem(Properties properties) {
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
                : applyPermanent(player, state, getPurity(stack), getEntityId(stack));
        if (!applied) {
            return InteractionResultHolder.fail(stack); // 无可作用器官：不消耗
        }
        stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    /** 永久药剂：将该生物基因型吸收进身体（该来源所有器官有效适配 +N）——
     *  每种基因型只能吸收一次，最多同时 4 种（基因管理器可卸载，卸载后可再次吸收） */
    private boolean applyPermanent(Player player, IPlayerBodyState state, int purity, String entityId) {
        if (entityId.isEmpty()) {
            noSource(player);
            return false;
        }
        if (state.hasGene(entityId)) {
            player.displayClientMessage(Component.translatable("message.akaishi.potion.already_gene"), true);
            return false;
        }
        if (!state.canAddGene()) {
            player.displayClientMessage(Component.translatable("message.akaishi.potion.gene_full"), true);
            return false;
        }
        int bonus = compatBonusByPurity(purity);
        state.addGene(entityId, bonus);
        Component sourceName = EntityType.byString(entityId)
                .map(type -> (Component) type.getDescription())
                .orElse(Component.literal(entityId));
        player.displayClientMessage(Component.translatable("message.akaishi.potion.permanent",
                sourceName, bonus), true);
        return true;
    }

    /** 突破药剂：须先经永久药剂吸收该来源基因型。激活后 30 分钟内该来源器官获得——
     *  额外适配（纯度四档 2/4/6/8，叠加于身体基因加成）+ 基础数值百分比强化（10/20/30/40%），
     *  负基础值属性词条激活期内暂时失效。同一时间最多 1 种激活；到期/卸载后可再次激活同一来源 */
    private boolean applyBreakthrough(Player player, IPlayerBodyState state, int purity, String entityId) {
        if (entityId.isEmpty()) {
            noSource(player);
            return false;
        }
        // 前置：突破强化的是身体基因，须先吸收该来源基因型
        if (!state.hasGene(entityId)) {
            player.displayClientMessage(Component.translatable("message.akaishi.potion.need_gene"), true);
            return false;
        }
        // 前置：须至少移植一枚该来源的非原生器官，激活才有效果（避免 30 分钟空转）
        if (!hasSameSourceOrgan(state, entityId)) {
            player.displayClientMessage(Component.translatable("message.akaishi.potion.need_organ"), true);
            return false;
        }
        if (state.isBreakthroughActive(entityId)) {
            // 同源突破仍激活中：等待本次结束（30 分钟到/卸载）后再喝即可再次激活
            player.displayClientMessage(Component.translatable("message.akaishi.potion.bt_running"), true);
            return false;
        }
        if (state.hasActiveBreakthrough()) {
            player.displayClientMessage(Component.translatable("message.akaishi.potion.bt_active"), true);
            return false;
        }
        int extra = extraCompatByPurity(purity); // 2/4/6/8
        long until = player.level().getGameTime() + BREAKTHROUGH_DURATION_TICKS;
        if (!state.startBreakthrough(entityId, extra, extra * 5, until)) {
            return false; // 兜底（前置已校验，正常不可达）：不消耗
        }
        Component sourceName = EntityType.byString(entityId)
                .map(type -> (Component) type.getDescription())
                .orElse(Component.literal(entityId));
        player.displayClientMessage(Component.translatable("message.akaishi.potion.breakthrough_ok",
                sourceName, extra, extra * 5), true);
        return true;
    }

    /** 突破药剂额外适配（纯度四档 1-25/26-50/51-75/76-100 → 2/4/6/8） */
    public static int extraCompatByPurity(int purity) {
        int clamped = Math.max(1, Math.min(100, purity));
        return ((clamped + 24) / 25) * 2;
    }

    /** 身上是否已移植该生物来源的非原生器官（原生器官不受基因加成，不计入） */
    private static boolean hasSameSourceOrgan(IPlayerBodyState state, String entityId) {
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            if (organ.getItem() instanceof AkaishiOrganItem
                    && !AkaishiOrganItem.isNative(organ)
                    && entityId.equals(AkaishiOrganItem.getEntityId(organ))) {
                return true;
            }
        }
        return false;
    }

    /** 缺少生物来源提示（两种药剂都需要生物来源才能生效） */
    private static void noSource(Player player) {
        player.displayClientMessage(Component.translatable("message.akaishi.potion.no_source"), true);
    }

    /** 永久药剂加成（纯度五档）：纯度每 20 分一档 → 加成 2/4/6/8/10（纯度 81+ 到顶 +10） */
    public static int compatBonusByPurity(int purity) {
        int clamped = Math.max(1, Math.min(100, purity));
        return ((clamped + 19) / 20) * 2;
    }

    // ===== 显示 =====

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        String templateId = getTemplateId(stack);
        PotionTemplate template = PotionRegistry.get(templateId);
        if (template == null) {
            tooltip.add(Component.translatable("gui.akaishi.potion.undefined"));
            return;
        }
        tooltip.add(Component.translatable("gui.akaishi.potion.type",
                Component.translatable(template.nameKey())));
        String entityId = getEntityId(stack);
        if (!entityId.isEmpty()) {
            // 生物来源（EntityType 自带翻译）
            EntityType.byString(entityId).ifPresent(type ->
                    tooltip.add(Component.translatable("gui.akaishi.potion.source",
                            type.getDescription())));
        }
        tooltip.add(Component.translatable("gui.akaishi.potion.purity", getPurity(stack)));
        if (template.breakthrough()) {
            // 突破药剂：需先吸收该来源基因；激活期内强化基础数值，负基础值词条暂时失效
            tooltip.add(Component.translatable("gui.akaishi.potion.hint_breakthrough"));
            int extra = extraCompatByPurity(getPurity(stack));
            tooltip.add(Component.translatable("gui.akaishi.potion.bt_bonus", extra, extra * 5));
        } else {
            // 永久药剂：直接显示本次适配度加成 + 吸收规则
            tooltip.add(Component.translatable("gui.akaishi.potion.compat_bonus",
                    compatBonusByPurity(getPurity(stack))));
            tooltip.add(Component.translatable("gui.akaishi.potion.hint_permanent"));
        }
    }
}
