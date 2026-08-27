package com.example.template.block.entity;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.block.ChishiAdvancedPurifierBlock;
import com.example.template.block.ChishiPurifierBlock;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.ChishiEnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 高级提纯构建方块实体：纯"提纯矩阵"（3×3×3）外壳方块。
 * 单放时无界面、不独立工作（无 UI 无法放入原料，避免误导）；
 * 成型后（formed=true）休眠，能量与容器访问代理到中心提纯器——
 * 中心被 26 格外壳完全包裹，管道经外壳才能给中心供能，AE2/物流管道经外壳读写中心槽位。
 */
public class ChishiAdvancedPurifierBlockEntity extends BlockEntity implements IEnergyProvider, Container {

    /** 自身能量存储：单放时无意义（无 UI/无消耗），成型后代理中心存储 */
    private final ChishiEnergyStorage energy;
    /** 本地容器：恒空，成型后所有容器访问代理到中心提纯器 */
    private final SimpleContainer inventory;
    /** 成型检测缓存失效标记（中心方块变化/自身加载时置位，每 tick 仅读缓存） */
    private boolean dirty = true;

    public ChishiAdvancedPurifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ADVANCED_PURIFIER.get(), pos, state);
        this.energy = new ChishiEnergyStorage(ChishiEnergyType.INSTANCE, 10000);
        this.inventory = new SimpleContainer(2) {
            @Override
            public void setChanged() {
                super.setChanged();
                ChishiAdvancedPurifierBlockEntity.this.setChanged();
            }
        };
    }

    /** 中心方块状态变化时触发：下次 tick 重新查找成型中心 */
    public void markDirty() {
        dirty = true;
    }

    /** 通知 3×3×3 范围内的外壳重新检测（中心方块放置/移除或成型状态变化时调用） */
    public static void notifyNearbyShells(Level level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    if (level.getBlockEntity(p) instanceof ChishiAdvancedPurifierBlockEntity shell) {
                        shell.markDirty();
                    }
                }
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiAdvancedPurifierBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 缓存化：仅当中心方块变化/自身加载时重新查找成型中心，每 tick 零方块查询
        if (!dirty) {
            return;
        }
        dirty = false;
        ChishiPurifierBlockEntity center = findMatrixCenter();
        boolean formed = center != null;
        BlockState blockState = level.getBlockState(worldPosition);
        if (formed != blockState.getValue(ChishiAdvancedPurifierBlock.FORMED)) {
            level.setBlock(worldPosition, blockState.setValue(ChishiAdvancedPurifierBlock.FORMED, formed), 3);
            setChanged();
        }
    }

    /** 在 3×3×3 范围内查找成型的提纯矩阵中心（普通提纯器） */
    public ChishiPurifierBlockEntity findMatrixCenter() {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos p = worldPosition.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof ChishiPurifierBlock
                            && s.getValue(ChishiPurifierBlock.FORMED)) {
                        if (level.getBlockEntity(p) instanceof ChishiPurifierBlockEntity center && center.isMatrixFormed()) {
                            return center;
                        }
                    }
                }
            }
        }
        return null;
    }

    public Container inventory() {
        return inventory;
    }

    /** 成型后作为外壳：容器访问动态代理到中心提纯器（AE2 存储总线 / 物流管道经外壳读写中心输入输出） */
    private Container currentContainer() {
        ChishiPurifierBlockEntity center = findMatrixCenter();
        return center != null ? center.inventory() : inventory;
    }

    // ===== Container：使 AE2 存储总线 / Mekanism 物流管道能直接读写中心机器槽位（零硬依赖） =====

    @Override
    public int getContainerSize() {
        return currentContainer().getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return currentContainer().isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return currentContainer().getItem(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return currentContainer().removeItem(index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return currentContainer().removeItemNoUpdate(index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        currentContainer().setItem(index, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return currentContainer().stillValid(player);
    }

    @Override
    public void clearContent() {
        currentContainer().clearContent();
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        // 成型后代理中心提纯器存储（中心被外壳包围，管道必须经外壳才能供能）
        ChishiPurifierBlockEntity center = findMatrixCenter();
        return center != null ? center.energy() : energy;
    }

    /** 纯消耗型机器：只接收管道输入的赤能源，不向外输出 */
    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy() {
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
    }
}
