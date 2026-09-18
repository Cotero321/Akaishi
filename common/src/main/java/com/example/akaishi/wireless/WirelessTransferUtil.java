package com.example.akaishi.wireless;

import com.example.akaishi.api.security.AkaishiSecurityPermission;
import com.example.akaishi.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * 无线传输工具：损耗计算与终端解析（赤能源 / 生命能量双族共用）。
 * <p>
 * 损耗规则：同维度按欧氏距离线性增长（基础 + 每格额外），封顶 {@link ModConfig#wirelessMaxLoss}；
 * 跨维度无距离概念，使用固定跨维损耗 {@link ModConfig#wirelessCrossDimLoss}（需终端已解锁跨维度）。
 * {@link #resolveTerminal} 按端口记录的终端 ID 直达目标终端，并校验绑定身份在终端安全表里的方向权限：
 * 同维度直查，异维度需终端解锁跨维度，并经服务器在目标维度查询
 * （终端所在区块由区块加载构架弱加载保证在线）。
 * 族感知：终端实体校验限定 {@link WirelessFamily}，跨族终端不可命中。
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
    public static double lossRatio(Level level, BlockPos portPos, IWirelessTerminal terminal, boolean isOutput) {
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
     * <b>远程绑定解析</b>（新路径）：按端口记录的终端 ID 直达终端，并校验「绑定身份」在目标终端安全表里的
     * 方向权限（输入口 {@code INJECT}、输出口 {@code EXTRACT}）——不再依赖身份卡白名单。
     * <p>
     * 归属者在安全表里恒为全权限；未登记任何条目的终端默认全放行，因此不配置安全的单机场景行为不变。
     *
     * @return 成型终端，或 null（未绑定 / 权限不足 / 未解锁跨维度 / 终端区块未加载）
     */
    public static IWirelessTerminal resolveTerminal(Level level, UUID terminalId, WirelessFamily family,
            UUID identity, AkaishiSecurityPermission required) {
        if (terminalId == null || !WirelessNetworkManager.hasPermission(terminalId, identity, required)) {
            return null;
        }
        WirelessFamily f = family == null ? WirelessFamily.CHISHI : family;
        WirelessNetworkManager.TerminalRef tr = WirelessNetworkManager.terminalOf(terminalId);
        if (tr == null) {
            return null;
        }
        if (tr.dimension().equals(level.dimension())) {
            return terminalAt(level, tr.pos(), f);
        }
        // 跨维度：需终端解锁跨维组件
        if (level.getServer() == null || !crossDimUnlocked(level, tr, f)) {
            return null;
        }
        ServerLevel target = level.getServer().getLevel(tr.dimension());
        return target == null ? null : terminalAt(target, tr.pos(), f);
    }

    /** 读取位置处成型且属指定族的终端（额外实体级校验，防注册表与实体状态不一致） */
    private static IWirelessTerminal terminalAt(Level level, BlockPos pos, WirelessFamily family) {
        return level.getBlockEntity(pos) instanceof IWirelessTerminal t && t.isFormed() && t.family() == family
                ? t : null;
    }

    /** 目标终端是否已解锁跨维度且属指定族（异维口发起传输前的校验） */
    private static boolean crossDimUnlocked(Level level, WirelessNetworkManager.TerminalRef tr, WirelessFamily family) {
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
        IWirelessTerminal t = terminalAt(target, tr.pos(), family);
        return t != null && t.isCrossDim();
    }
}
