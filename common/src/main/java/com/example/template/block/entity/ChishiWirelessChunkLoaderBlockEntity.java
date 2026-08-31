package com.example.template.block.entity;

import com.example.template.block.ChishiWirelessControllerBlock;
import com.example.template.wireless.WirelessTerminalStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 区块加载构架方块实体：无线终端内腔组件的自持中枢区块锁。
 * <p>
 * 每 20 tick 确认自己处于成型结构中，对「所属结构控制器区块」施加弱加载 ticket
 * （{@link TicketType#PORTAL}，半径 1 → 区块保持加载、方块实体 tick）。
 * 玩家离开后区块仍被 ticket 保持 → 控制器 BE 持续 tick → 频道网络离线运转；
 * 该区块 ticket 同时保持构架自身 BE tick，形成自持环。
 * 结构失效或被拆时调用 {@link #releaseChunkLoad()} 释放已锁区块，防止 ticket 泄漏。
 */
public class ChishiWirelessChunkLoaderBlockEntity extends BlockEntity {

    private static final int SCAN_INTERVAL = 20;
    /** 已锁定的控制器区块（null = 当前未锁） */
    private BlockPos targetController;
    private int cooldown;

    public ChishiWirelessChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_WIRELESS_CHUNK_LOADER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiWirelessChunkLoaderBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        if (!(level instanceof ServerLevel serverLevel) || --cooldown > 0) {
            return;
        }
        cooldown = SCAN_INTERVAL;
        BlockPos c = resolveStructureController(serverLevel);
        if (c != null) {
            if (targetController == null || !targetController.equals(c)) {
                releaseChunkLoad(); // 结构变化：先释放旧锁
                targetController = c;
            }
            ChunkPos cp = new ChunkPos(c);
            // 弱加载：PORTAL ticket 半径 1（level 32-1=31 → 区块 tick），幂等刷新
            serverLevel.getChunkSource().addRegionTicket(TicketType.PORTAL, cp, 1, cp.getWorldPosition());
        } else if (targetController != null) {
            releaseChunkLoad(); // 结构失效：释放
        }
    }

    /** 在当前区块附近（±4 格，结构边长 5 的余量）寻找成型结构且覆盖自身的控制器 */
    private BlockPos resolveStructureController(ServerLevel serverLevel) {
        for (BlockPos c : BlockPos.betweenClosed(
                worldPosition.offset(-4, -4, -4), worldPosition.offset(4, 4, 4))) {
            if (!(serverLevel.getBlockState(c).getBlock() instanceof ChishiWirelessControllerBlock)) {
                continue;
            }
            WirelessTerminalStructure.Result r = WirelessTerminalStructure.scan(serverLevel, c);
            if (r != null && worldPosition.getX() >= r.min.getX() && worldPosition.getX() <= r.max.getX()
                    && worldPosition.getY() >= r.min.getY() && worldPosition.getY() <= r.max.getY()
                    && worldPosition.getZ() >= r.min.getZ() && worldPosition.getZ() <= r.max.getZ()) {
                return c.immutable();
            }
        }
        return null;
    }

    /** 释放已锁定的中枢区块（结构失效 / 方块被拆时调用，防 ticket 泄漏） */
    public void releaseChunkLoad() {
        if (level instanceof ServerLevel serverLevel && targetController != null) {
            ChunkPos cp = new ChunkPos(targetController);
            serverLevel.getChunkSource().removeRegionTicket(TicketType.PORTAL, cp, 1, cp.getWorldPosition());
            targetController = null;
        }
    }
}
