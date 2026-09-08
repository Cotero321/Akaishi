package com.example.akaishi.block.entity;

import com.example.akaishi.block.AkaishiLifeWirelessControllerBlock;
import com.example.akaishi.wireless.LifeWirelessStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 生命无线区块加载构架的自持中枢区块锁。 */
public class AkaishiLifeWirelessChunkLoaderBlockEntity extends BlockEntity {

    private static final int SCAN_INTERVAL = 20;
    private BlockPos targetController;
    private int cooldown;

    public AkaishiLifeWirelessChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_WIRELESS_CHUNK_LOADER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  AkaishiLifeWirelessChunkLoaderBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        if (!(level instanceof ServerLevel serverLevel) || --cooldown > 0) {
            return;
        }
        cooldown = SCAN_INTERVAL;
        BlockPos controller = resolveStructureController(serverLevel);
        if (controller != null) {
            if (targetController == null || !targetController.equals(controller)) {
                releaseChunkLoad();
                targetController = controller;
            }
            ChunkPos chunk = new ChunkPos(controller);
            serverLevel.getChunkSource().addRegionTicket(TicketType.PORTAL, chunk, 1, chunk.getWorldPosition());
        } else if (targetController != null) {
            releaseChunkLoad();
        }
    }

    private BlockPos resolveStructureController(ServerLevel serverLevel) {
        for (BlockPos controller : BlockPos.betweenClosed(
                worldPosition.offset(-4, -4, -4), worldPosition.offset(4, 4, 4))) {
            if (!(serverLevel.getBlockState(controller).getBlock() instanceof AkaishiLifeWirelessControllerBlock)) {
                continue;
            }
            LifeWirelessStructure.Result structure = LifeWirelessStructure.scan(serverLevel, controller);
            if (structure != null && worldPosition.getX() >= structure.min.getX()
                    && worldPosition.getX() <= structure.max.getX()
                    && worldPosition.getY() >= structure.min.getY()
                    && worldPosition.getY() <= structure.max.getY()
                    && worldPosition.getZ() >= structure.min.getZ()
                    && worldPosition.getZ() <= structure.max.getZ()) {
                return controller.immutable();
            }
        }
        return null;
    }

    public void releaseChunkLoad() {
        if (level instanceof ServerLevel serverLevel && targetController != null) {
            ChunkPos chunk = new ChunkPos(targetController);
            serverLevel.getChunkSource().removeRegionTicket(TicketType.PORTAL, chunk, 1, chunk.getWorldPosition());
            targetController = null;
        }
    }
}
