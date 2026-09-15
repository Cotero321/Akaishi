package com.example.akaishi.life.altar;

import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.block.entity.AkaishiMotherAltarBlockEntity;
import com.example.akaishi.effect.ModEffects;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.sequence.AkaishiGeneSequenceItem;
import com.example.akaishi.multiblock.AkaishiGoatAltarTiersStructure;
import com.example.akaishi.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 母神祭坛"生命融合仪式"：进度蓄满后校验祭品配方，齐备即消耗全部祭品、凝出生命融合锭。
 * <p>配方（共 9 件，须恰好占满 1 巨坛 + 8 外圈子祭坛）：
 * <ul>
 *   <li>巨坛供奉槽：赤石锭 ×1</li>
 *   <li>外圈 8 座各 1 件：生命胚胎 ×2、生命灰烬 ×2、纯度 &gt; {@link #MIN_GENE_PURITY} 的基因序列 ×2、
 *       浓缩赤石精华块 ×2</li>
 * </ul>
 * <p>产物放回巨坛供奉槽（沿用悬浮旋转展示），玩家经界面或空手右键取回。
 * <p>注能期间每推进 10% 落一道纯视觉雷（子祭坛轮流，进度满时落巨坛中央），见 {@link #strikeProgressBolts}。
 */
public final class AkaishiAltarRitual {

    /** 基因纯度门槛（不含）：低于或等于该值的序列不被仪式接受 */
    public static final int MIN_GENE_PURITY = 50;
    /** 外圈子祭坛座数（配方恰好占满，多一件即不成立） */
    private static final int OUTER_SLOTS = 8;
    /** 每种祭品的要求数量 */
    private static final int PER_KIND = 2;
    /** 完成反馈的广播半径（格） */
    private static final double ANNOUNCE_RANGE = 64.0D;
    /** 仪式落雷档数：进度每推进 1/10 落一道，共 10 档（第 10 档即进度满） */
    private static final long BOLT_MILESTONES = 10L;
    /** 单档进度步长（8K = 10%），跨过其整数倍即落雷 */
    private static final long BOLT_STEP = AkaishiMotherAltarBlockEntity.PROGRESS_MAX / BOLT_MILESTONES;
    /** 完成爆发时「不可名状」的时长（tick）：15s，足够玩家看完一轮强化表现 */
    private static final int BURST_DURATION = 300;
    /** 完成爆发时「不可名状」的等级：II 级（放大器 1），画面扭曲与呓语随之加剧 */
    private static final int BURST_AMPLIFIER = 1;

    private AkaishiAltarRitual() {
    }

    /**
     * 进度推进时的仪式落雷（仅服务端调用）。
     * <p>每跨过一个 10% 档位落一道纯视觉雷：第 1~9 档依次击中外圈 8 座子祭坛（超出按 {@code size} 循环），
     * 第 10 档（进度满）击中巨坛正中央，呼应"能量注入的节拍"。
     * <p>档位直接由进度推导，进度单调递增、结算成功后清零，故无需额外落雷状态，也不会重复触发。
     *
     * @param before 本次注入前的进度
     * @param after  本次注入后的进度
     */
    public static void strikeProgressBolts(ServerLevel level, BlockPos hostPos, long before, long after) {
        long first = before / BOLT_STEP + 1;
        long last = after / BOLT_STEP;
        if (first > last) {
            return;
        }
        BlockPos origin = AkaishiGoatAltarTiersStructure.findOrigin(level, hostPos);
        if (origin == null) {
            return;
        }
        List<BlockPos> outer = AkaishiGoatAltarTiersStructure.collectOuterAltars(origin);
        for (long milestone = first; milestone <= last; milestone++) {
            if (milestone >= BOLT_MILESTONES) {
                // 第 10 档：进度满，雷落巨坛 2×2 交界处正上方
                strike(level, origin.getX() + 8.0D, origin.getY() + 2.0D, origin.getZ() + 8.0D);
            } else if (!outer.isEmpty()) {
                BlockPos target = outer.get((int) ((milestone - 1) % outer.size()));
                strike(level, target.getX() + 0.5D, target.getY() + 1.0D, target.getZ() + 0.5D);
            }
        }
    }

    /** 落一道纯视觉雷电：有雷柱与雷声，但不点燃方块、不伤害实体，避免破坏祭坛结构或伤害玩家 */
    private static void strike(ServerLevel level, double x, double y, double z) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(x, y, z);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    /**
     * 只读校验祭品配方（无任何消耗/产出副作用），供界面与氛围音判定"是否正在合成"。
     *
     * @return 配方齐备时返回外圈 8 座坐标（供结算时消耗），否则返回 {@code null}
     */
    private static List<BlockPos> findReadyOuter(ServerLevel level, BlockPos hostPos, AkaishiMotherAltarBlockEntity host) {
        if (!host.getOffering().is(ModItems.akaishiIngot.get())) {
            return null;
        }
        BlockPos origin = AkaishiGoatAltarTiersStructure.findOrigin(level, hostPos);
        if (origin == null) {
            return null;
        }
        List<BlockPos> outer = AkaishiGoatAltarTiersStructure.collectOuterAltars(origin);
        if (outer.size() != OUTER_SLOTS) {
            return null;
        }
        int embryo = 0;
        int ash = 0;
        int gene = 0;
        int essence = 0;
        for (BlockPos pos : outer) {
            if (!(level.getBlockEntity(pos) instanceof AkaishiMotherAltarBlockEntity altar) || !altar.hasOffering()) {
                return null;
            }
            ItemStack stack = altar.getOffering();
            if (stack.is(ModItems.lifeEmbryo.get())) {
                embryo++;
            } else if (stack.is(ModItems.lifeAsh.get())) {
                ash++;
            } else if (stack.is(ModItems.geneSequence.get())
                    && AkaishiGeneSequenceItem.getPurity(stack) > MIN_GENE_PURITY) {
                gene++;
            } else if (stack.is(ModBlocks.CHISHI_ESSENCE_BLOCK.get().asItem())) {
                essence++;
            } else {
                return null;
            }
        }
        if (embryo != PER_KIND || ash != PER_KIND || gene != PER_KIND || essence != PER_KIND) {
            return null;
        }
        return outer;
    }

    /**
     * 只读预检：祭品配方是否齐备（含巨坛供奉槽的赤石锭与外圈 8 件）。
     * 仅做判定，不消耗也不产出，供界面进度条显示与氛围音"工作中"档位使用。
     */
    public static boolean isRecipeReady(ServerLevel level, BlockPos hostPos, AkaishiMotherAltarBlockEntity host) {
        return findReadyOuter(level, hostPos, host) != null;
    }

    /**
     * 尝试结算仪式（仅服务端调用）。
     *
     * @param hostPos 巨坛主座坐标（结构原点 offset(7,0,7)）
     * @return true = 配方齐备并已完成消耗与产出；false = 配方不齐，进度保持满值等待补齐
     */
    public static boolean tryComplete(ServerLevel level, BlockPos hostPos, AkaishiMotherAltarBlockEntity host) {
        List<BlockPos> outer = findReadyOuter(level, hostPos, host);
        if (outer == null) {
            return false;
        }
        // 消耗外圈 8 件，巨坛供奉槽换为产物
        for (BlockPos pos : outer) {
            if (level.getBlockEntity(pos) instanceof AkaishiMotherAltarBlockEntity altar) {
                altar.takeOffering();
            }
        }
        host.setOffering(new ItemStack(ModItems.lifeFusionIngot.get()));
        announce(level, hostPos);
        return true;
    }

    /** 仪式完成反馈：向巨坛附近的玩家施加强化「不可名状」爆发并散出粒子（无聊天文本） */
    private static void announce(ServerLevel level, BlockPos hostPos) {
        double cx = hostPos.getX() + 0.5D;
        double cy = hostPos.getY() + 0.5D;
        double cz = hostPos.getZ() + 0.5D;
        double rangeSqr = ANNOUNCE_RANGE * ANNOUNCE_RANGE;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(cx, cy, cz) > rangeSqr) {
                continue;
            }
            // 完成爆发：叠加 II 级「不可名状」，客户端画面扭曲/噪点/低语文字随之加剧
            player.addEffect(new MobEffectInstance(ModEffects.UNNAMEABLE.get(),
                    BURST_DURATION, BURST_AMPLIFIER, true, false, true));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.UNNAMEABLE_WHISPER.get(), SoundSource.PLAYERS, 1.0F, 0.85F);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                hostPos.getX() + 0.5D, hostPos.getY() + 1.5D, hostPos.getZ() + 0.5D,
                48, 1.0D, 0.6D, 1.0D, 0.04D);
    }
}
