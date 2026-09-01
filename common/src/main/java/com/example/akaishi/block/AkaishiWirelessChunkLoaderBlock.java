package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiWirelessChunkLoaderBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * 区块加载构架：无线终端多方块内腔功能件（放置在终端核心附近，内腔任意非中心格）。
 * <p>
 * 持有方块实体：每 20 tick 确认自己处于成型结构中，并对「所属结构控制器区块」施加
 * 弱加载 ticket（{@code TicketType.PORTAL}，半径 1 → 区块保持加载且方块实体 tick）。
 * 这样玩家离开后终端能量中枢仍然运转；频道内各输入口/输出口所在区块的弱加载
 * 由控制器统一管理（本构架仅负责中枢区块自持，避免玩家离开即断网）。
 * 必须放在成型结构内腔才生效；被拆时释放已锁区块。
 */
public class AkaishiWirelessChunkLoaderBlock extends Block implements EntityBlock {

    public AkaishiWirelessChunkLoaderBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_WIRELESS_CHUNK_LOADER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_WIRELESS_CHUNK_LOADER.get()) {
            return null;
        }
        // type 已确认是区块加载构架类型，安全强转
        return (l, p, s, be) -> AkaishiWirelessChunkLoaderBlockEntity.serverTick(l, p, s, (AkaishiWirelessChunkLoaderBlockEntity) be);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof AkaishiWirelessChunkLoaderBlockEntity be) {
            be.releaseChunkLoad(); // 释放已锁的中枢区块
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
