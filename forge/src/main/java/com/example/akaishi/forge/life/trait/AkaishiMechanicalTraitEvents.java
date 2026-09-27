package com.example.akaishi.forge.life.trait;

import com.example.akaishi.life.mechanical.trait.MechanicalTraits;
import com.example.akaishi.life.mechanical.trait.TraitTier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Iterator;
import java.util.Map;

/**
 * 需要平台事件的材料特性处理器（4 个）：这些特性没有落在 7 个通用钩子上，
 * 必须由对应的 Forge 事件驱动。
 * <ul>
 *   <li>{@code debuff_ward} 净化滤芯：{@code MobEffectEvent.Added} 登记缩短后的截止刻，tick 到点提前移除
 *       （{@code MobEffectInstance.duration} 为私有且无 setter，故不用反射改写时长）；</li>
 *   <li>{@code excavator} 掘进模块：{@code PlayerEvent.BreakSpeed} 提升挖掘速度；</li>
 *   <li>{@code prospector} 探矿协议：{@code BlockEvent.BreakEvent} 按概率额外掉落矿物；</li>
 *   <li>{@code data_harvest} 数据收割：{@code LivingExperienceDropEvent} 提升击杀经验。</li>
 * </ul>
 * 全部数值均为<b>待调手感值</b>；等级统一走 {@link TraitSupport#effectiveLevel}（跨器官有效等级，单次取值）。
 */
public final class AkaishiMechanicalTraitEvents {

    public static final AkaishiMechanicalTraitEvents INSTANCE = new AkaishiMechanicalTraitEvents();

    /** 净化滤芯：负面时长缩减 15%/25%/35%/45% */
    private static final TraitTier DEBUFF_REDUCTION = TraitTier.of(0.15F, 0.25F, 0.35F, 0.45F);
    /** 掘进模块：挖掘速度 +8%/12%/16%/20% */
    private static final TraitTier EXCAVATE_BONUS = TraitTier.of(0.08F, 0.12F, 0.16F, 0.20F);
    /** 探矿协议：额外矿物概率 3%/5%/7%/10% */
    private static final TraitTier PROSPECT_CHANCE = TraitTier.of(0.03F, 0.05F, 0.07F, 0.10F);
    /** 数据收割：经验 +10%/15%/20%/30% */
    private static final TraitTier HARVEST_BONUS = TraitTier.of(0.10F, 0.15F, 0.20F, 0.30F);

    private AkaishiMechanicalTraitEvents() {
    }

    // ==================== 净化滤芯 ====================

    /** 负面效果被施加时登记「提前移除」截止刻（按跨器官有效等级单次缩减）。 */
    @SubscribeEvent
    public void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        MobEffectInstance added = event.getEffectInstance();
        if (added == null || added.getEffect().getCategory() != MobEffectCategory.HARMFUL) {
            return;
        }
        int duration = added.getDuration();
        if (duration <= 0) {
            return; // 无限时长不动
        }
        int level = TraitSupport.effectiveLevel(player, MechanicalTraits.DEBUFF_WARD);
        if (level <= 0) {
            return;
        }
        double retain = 1.0D - DEBUFF_REDUCTION.at(level);
        long deadline = (long) player.tickCount + (long) (duration * retain);
        MechanicalTraitRuntime.of(player.getUUID()).debuffDeadlines.put(added.getEffect(), deadline);
    }

    /** 到点清除已过缩短窗口的负面效果（效果自然结束的条目一并回收）。 */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }
        Player player = event.player;
        MechanicalTraitRuntime.State st = MechanicalTraitRuntime.peek(player.getUUID());
        if (st == null || st.debuffDeadlines.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<MobEffect, Long>> it = st.debuffDeadlines.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<MobEffect, Long> entry = it.next();
            if (!player.hasEffect(entry.getKey())) {
                it.remove();
            } else if ((long) player.tickCount >= entry.getValue()) {
                player.removeEffect(entry.getKey());
                it.remove();
            }
        }
    }

    // ==================== 掘进 / 探矿 / 数据收割 ====================

    /** 掘进模块：提升方块破坏速度。 */
    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        int level = TraitSupport.effectiveLevel(player, MechanicalTraits.EXCAVATOR);
        if (level > 0) {
            event.setNewSpeed(event.getNewSpeed() * (float) (1.0D + EXCAVATE_BONUS.at(level)));
        }
    }

    /** 探矿协议：破坏方块时按概率额外掉落该方块物品（粗略"额外矿物"口径）。（待调手感值） */
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide || player.isCreative()) {
            return;
        }
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }
        int traitLevel = TraitSupport.effectiveLevel(player, MechanicalTraits.PROSPECTOR);
        if (traitLevel <= 0) {
            return;
        }
        double chance = PROSPECT_CHANCE.at(traitLevel);
        if (player.getRandom().nextDouble() >= Math.min(chance, 1.0D)) {
            return;
        }
        ItemStack extra = new ItemStack(event.getState().getBlock().asItem());
        if (!extra.isEmpty()) {
            Block.popResource(level, event.getPos(), extra);
        }
    }

    /** 数据收割：提升击杀经验掉落。 */
    @SubscribeEvent
    public void onExperienceDrop(LivingExperienceDropEvent event) {
        Player player = event.getAttackingPlayer();
        if (player == null) {
            return;
        }
        int level = TraitSupport.effectiveLevel(player, MechanicalTraits.DATA_HARVEST);
        if (level > 0) {
            event.setDroppedExperience((int) (event.getDroppedExperience() * (1.0D + HARVEST_BONUS.at(level))));
        }
    }

    // ==================== 清理 ====================

    /** 玩家登出：丢弃该玩家的瞬时状态，避免长期钉住 UUID。 */
    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MechanicalTraitRuntime.clear(event.getEntity().getUUID());
    }
}
