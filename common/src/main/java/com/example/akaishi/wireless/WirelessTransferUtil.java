package com.example.akaishi.wireless;

import com.example.akaishi.block.entity.AkaishiWirelessTerminalBlockEntity;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.item.IdentityCardTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * 无线传输工具：损耗计算与终端解析。
 * <p>
 * 损耗规则：同维度按欧氏距离线性增长（基础 + 每格额外），封顶 {@link ModConfig#wirelessMaxLoss}；
 * 跨维度无距离概念，使用固定跨维损耗 {@link ModConfig#wirelessCrossDimLoss}（需终端已解锁跨维度）。
 * {@link #resolveTerminal} 按身份卡认证反查授权终端：同维度直查，异维度需终端解锁跨维度，
 * 并经服务器在目标维度查询（终端所在区块由区块加载构架弱加载保证在线）。
 */
public final class WirelessTransferUtil {

    private WirelessTransferUtil() {
    }

    /**
     * 计算损耗率（0-1；无终端返回 1 表示完全损耗不可传）。
     * 同维度按距离线性增长（基础 + 每格），跨维度固定损耗（resolveTerminal 已校验跨维解锁）；
     * 最终损耗再按方向乘以削减系数（1 - 终端内腔对应方向的损耗抑制比例）。
     *
     * @param isOutput true=输出口方向（储能→缓冲），false=输入口方向（缓冲→储能）
     */
    public static double lossRatio(Level level, BlockPos portPos, AkaishiWirelessTerminalBlockEntity terminal, boolean isOutput) {
        if (terminal == null || terminal.getLevel() == null) {
            return 1.0;
        }
        double base;
        if (terminal.getLevel().dimension().equals(level.dimension())) {
            double dist = Math.sqrt(portPos.distSqr(terminal.getBlockPos()));
            base = Math.min(ModConfig.wirelessMaxLoss,
                    ModConfig.wirelessBaseLoss + dist * ModConfig.wirelessLossPerBlock);
        } else {
            base = ModConfig.wirelessCrossDimLoss;
        }
        double reduction = isOutput ? terminal.outputLossReduction() : terminal.inputLossReduction();
        return Math.max(0.0, base * (1.0 - reduction));
    }

    /**
     * 解析绑定卡可用的成型终端：按卡 UUID 反查网络注册表（参考 MEK 同卡配对）；
     * 同维度直查；异维度需终端已解锁跨维度，并经服务器在目标维度查询
     * （终端所在区块由区块加载构架弱加载保证在线）。
     *
     * @return 成型终端方块实体，或 null（卡未授权 / 无在线终端 / 未解锁跨维度 / 终端区块未加载）
     */
    public static AkaishiWirelessTerminalBlockEntity resolveTerminal(Level level, UUID boundCard) {
        UUID terminalId = WirelessNetworkManager.findTerminalForCard(boundCard);
        if (terminalId == null) {
            return null;
        }
        WirelessNetworkManager.TerminalRef tr = WirelessNetworkManager.terminalOf(terminalId);
        if (tr == null) {
            return null;
        }
        if (tr.dimension().equals(level.dimension())) {
            return level.getBlockEntity(tr.pos()) instanceof AkaishiWirelessTerminalBlockEntity t && t.isFormed()
                    ? t : null;
        }
        // 跨维度：需终端解锁跨维组件
        if (level.getServer() == null || !crossDimUnlocked(level, tr)) {
            return null;
        }
        ServerLevel target = level.getServer().getLevel(tr.dimension());
        if (target == null) {
            return null;
        }
        return target.getBlockEntity(tr.pos()) instanceof AkaishiWirelessTerminalBlockEntity t && t.isFormed()
                ? t : null;
    }

    /** 目标终端是否已解锁跨维度（异维口发起传输前的校验） */
    private static boolean crossDimUnlocked(Level level, WirelessNetworkManager.TerminalRef tr) {
        if (tr.dimension().equals(level.dimension())) {
            return true;
        }
        if (level.getServer() == null) {
            return false;
        }
        ServerLevel target = level.getServer().getLevel(tr.dimension());
        if (target == null) {
            return false;
        }
        return target.getBlockEntity(tr.pos()) instanceof AkaishiWirelessTerminalBlockEntity t && t.isCrossDim();
    }

    /** 口每 tick 传输上限 = 基础速率 × 卡等级倍率（按卡自动解锁，当前仅基础级 1×） */
    public static long transferRate(IdentityCardTier tier) {
        return ModConfig.wirelessPortTransferRate * tier.rateMultiplier();
    }
}
