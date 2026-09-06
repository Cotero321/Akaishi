package com.example.akaishi.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * 管道碰撞箱：与细管模型（中心 6×6 方核 + 各连接方向延伸段）逐像素一致，
 * 消除整方块碰撞箱造成的「空气墙」，玩家可自由穿行管身间隙。
 * 能量/流体/物品三类管道共用；64 种连接组合静态缓存，避免每 tick 重复构建。
 */
public final class PipeShapes {

    /** 中心方核（5..11，即 6/16 管径） */
    private static final VoxelShape CENTER = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
    private static final VoxelShape ARM_NORTH = Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 5.0);
    private static final VoxelShape ARM_EAST = Block.box(11.0, 5.0, 5.0, 16.0, 11.0, 11.0);
    private static final VoxelShape ARM_SOUTH = Block.box(5.0, 5.0, 11.0, 11.0, 11.0, 16.0);
    private static final VoxelShape ARM_WEST = Block.box(0.0, 5.0, 5.0, 5.0, 11.0, 11.0);
    private static final VoxelShape ARM_UP = Block.box(5.0, 11.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape ARM_DOWN = Block.box(5.0, 0.0, 5.0, 11.0, 5.0, 11.0);

    /** 位序：bit0 north / bit1 east / bit2 south / bit3 west / bit4 up / bit5 down */
    private static final VoxelShape[] CACHE = new VoxelShape[64];

    static {
        for (int mask = 0; mask < CACHE.length; mask++) {
            List<VoxelShape> parts = new ArrayList<>(7);
            parts.add(CENTER);
            if ((mask & 1) != 0) {
                parts.add(ARM_NORTH);
            }
            if ((mask & 2) != 0) {
                parts.add(ARM_EAST);
            }
            if ((mask & 4) != 0) {
                parts.add(ARM_SOUTH);
            }
            if ((mask & 8) != 0) {
                parts.add(ARM_WEST);
            }
            if ((mask & 16) != 0) {
                parts.add(ARM_UP);
            }
            if ((mask & 32) != 0) {
                parts.add(ARM_DOWN);
            }
            VoxelShape combined = parts.get(0);
            for (int i = 1; i < parts.size(); i++) {
                combined = Shapes.or(combined, parts.get(i));
            }
            CACHE[mask] = combined;
        }
    }

    private PipeShapes() {
    }

    /** 按 6 向连接属性组装细管形状（属性顺序：north/east/south/west/up/down） */
    public static VoxelShape build(BlockState state, BooleanProperty north, BooleanProperty east,
                                   BooleanProperty south, BooleanProperty west,
                                   BooleanProperty up, BooleanProperty down) {
        int mask = (state.getValue(north) ? 1 : 0) | (state.getValue(east) ? 2 : 0)
                | (state.getValue(south) ? 4 : 0) | (state.getValue(west) ? 8 : 0)
                | (state.getValue(up) ? 16 : 0) | (state.getValue(down) ? 32 : 0);
        return CACHE[mask];
    }
}
