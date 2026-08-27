package com.example.template.block.entity;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.api.energy.IEnergyType;
import com.example.template.block.ChishiLifeAggregationConverterBlock;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.ChishiEnergyType;
import com.example.template.energy.LifeEnergyType;
import com.example.template.menu.ChishiLifeConverterMenu;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 生命聚合转换器方块实体：
 * 单方块独立工作——每 tick 消耗 10M 赤能源聚合出 10 生命能量；
 * 作为"生命转换架构"外壳（formed=true）时休眠，能量访问（getEnergyStorage(type)）
 * 与 GUI 代理到中心主方块。赤能源只进不出、生命能量只出不进。
 */
public class ChishiLifeAggregationConverterBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider {

    /** 单次聚合消耗的赤能源量 */
    public static final long CONVERSION_COST = 10_000_000L;
    /** 单次聚合产出的生命能量量 */
    public static final long CONVERSION_OUTPUT = 10L;
    /** 自身赤能源缓冲容量 */
    public static final long CHISHI_CAPACITY = 100_000_000L;
    /** 自身生命能量缓冲容量 */
    public static final long LIFE_CAPACITY = 100L;

    private final ChishiEnergyStorage chishi;
    private final ChishiEnergyStorage life;
    private final SimpleContainerData data;

    public ChishiLifeAggregationConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_AGGREGATION_CONVERTER.get(), pos, state);
        this.chishi = new ChishiEnergyStorage(ChishiEnergyType.INSTANCE, CHISHI_CAPACITY);
        this.life = new ChishiEnergyStorage(LifeEnergyType.INSTANCE, LIFE_CAPACITY);
        this.data = new SimpleContainerData(5);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiLifeAggregationConverterBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 检测是否作为生命转换架构的外壳
        ChishiLifeConversionArchitectureBlockEntity arch = findArchitecture();
        boolean formed = arch != null;
        BlockState blockState = level.getBlockState(worldPosition);
        if (formed != blockState.getValue(ChishiLifeAggregationConverterBlock.FORMED)) {
            level.setBlock(worldPosition, blockState.setValue(ChishiLifeAggregationConverterBlock.FORMED, formed), 3);
        }
        // 未成型时独立转换；成型后由中心主方块集中转换
        if (!formed) {
            convert();
        }
        // 同步自身数据到 GUI（成型后右键代理打开的是中心界面，此处仅单方块时使用）
        data.set(0, (int) chishi.getEnergyStored());
        data.set(1, (int) chishi.getMaxEnergy());
        data.set(2, (int) life.getEnergyStored());
        data.set(3, (int) life.getMaxEnergy());
        data.set(4, formed ? 1 : 0);
    }

    /** 单次转换：赤能源充足且生命能量未满时执行 */
    private boolean convert() {
        if (chishi.getEnergyStored() >= CONVERSION_COST
                && life.getEnergyStored() + CONVERSION_OUTPUT <= life.getMaxEnergy()) {
            chishi.extractEnergy(CONVERSION_COST, false);
            life.addEnergy(CONVERSION_OUTPUT, false);
            setChanged();
            return true;
        }
        return false;
    }

    /** 在 3×3×3 范围内查找成型的生命转换架构中心 */
    public ChishiLifeConversionArchitectureBlockEntity findArchitecture() {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    if (level.getBlockEntity(worldPosition.offset(dx, dy, dz))
                            instanceof ChishiLifeConversionArchitectureBlockEntity arch && arch.isStructureValid()) {
                        return arch;
                    }
                }
            }
        }
        return null;
    }

    public ContainerData data() {
        return data;
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        return chishi;
    }

    @Override
    public IEnergyStorage getEnergyStorage(IEnergyType type) {
        // 成型后代理到中心主方块的存储；未成型使用自身存储
        ChishiLifeConversionArchitectureBlockEntity arch = findArchitecture();
        if (type == ChishiEnergyType.INSTANCE) {
            return arch != null ? arch.chishiStorage() : chishi;
        }
        if (type == LifeEnergyType.INSTANCE) {
            return arch != null ? arch.lifeStorage() : life;
        }
        return null;
    }

    @Override
    public boolean canInputEnergy() {
        return true;
    }

    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy(IEnergyType type) {
        // 赤能源只进（原料），生命能量只出不进
        return type == ChishiEnergyType.INSTANCE;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.template_mod.chishi_life_aggregation_converter");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ChishiLifeConverterMenu(id, inv, data);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("ChishiEnergy", chishi.getEnergyStored());
        tag.putLong("LifeEnergy", life.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        chishi.setEnergy(tag.getLong("ChishiEnergy"));
        life.setEnergy(tag.getLong("LifeEnergy"));
    }
}
