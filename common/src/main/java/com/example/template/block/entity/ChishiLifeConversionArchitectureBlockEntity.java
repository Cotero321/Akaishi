package com.example.template.block.entity;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.api.energy.IEnergyType;
import com.example.template.block.ChishiLifeAggregationConverterBlock;
import com.example.template.block.ChishiLifeConversionArchitectureBlock;
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
 * 生命转换架构方块实体（3×3×3 多方块结构主方块）：
 * 26 台生命聚合转换器环绕成型（formed=true）后，以 45 倍速率集中转换——
 * 每 tick 最多 45 次，每次消耗 10M 赤能源、产出 10 生命能量（450M → 450/tick）。
 * 赤能源经外壳代理注入，生命能量经外壳代理输出。
 */
public class ChishiLifeConversionArchitectureBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider {

    /** 成型后每 tick 转换次数（= 45 倍单台速率） */
    public static final int CONVERSIONS_PER_TICK = 45;
    /** 单次转换消耗的赤能源量 */
    public static final long CONVERSION_COST = ChishiLifeAggregationConverterBlockEntity.CONVERSION_COST;
    /** 单次转换产出的生命能量量 */
    public static final long CONVERSION_OUTPUT = ChishiLifeAggregationConverterBlockEntity.CONVERSION_OUTPUT;
    /** 中心赤能源缓冲容量（支持一轮 45 次转换后仍有余量） */
    public static final long CHISHI_CAPACITY = 500_000_000L;
    /** 中心生命能量存储容量 */
    public static final long LIFE_CAPACITY = 5000L;

    private final ChishiEnergyStorage chishi;
    private final ChishiEnergyStorage life;
    private final SimpleContainerData data;

    public ChishiLifeConversionArchitectureBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_CONVERSION_ARCHITECTURE.get(), pos, state);
        this.chishi = new ChishiEnergyStorage(ChishiEnergyType.INSTANCE, CHISHI_CAPACITY);
        this.life = new ChishiEnergyStorage(LifeEnergyType.INSTANCE, LIFE_CAPACITY);
        this.data = new SimpleContainerData(5);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiLifeConversionArchitectureBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        boolean formed = isStructureValid();
        BlockState blockState = level.getBlockState(worldPosition);
        if (formed != blockState.getValue(ChishiLifeConversionArchitectureBlock.FORMED)) {
            level.setBlock(worldPosition, blockState.setValue(ChishiLifeConversionArchitectureBlock.FORMED, formed), 3);
        }
        // 成型后以 45 倍速率集中转换
        if (formed) {
            for (int i = 0; i < CONVERSIONS_PER_TICK; i++) {
                if (!convert()) {
                    break;
                }
            }
        }
        // 同步数据到 GUI
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

    /** 结构校验：3×3×3 除中心外 26 个位置均为生命聚合转换器 */
    public boolean isStructureValid() {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    if (!(level.getBlockState(worldPosition.offset(dx, dy, dz)).getBlock() instanceof ChishiLifeAggregationConverterBlock)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** 供外壳代理访问的赤能源存储 */
    public ChishiEnergyStorage chishiStorage() {
        return chishi;
    }

    /** 供外壳代理访问的生命能量存储 */
    public ChishiEnergyStorage lifeStorage() {
        return life;
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
        if (type == ChishiEnergyType.INSTANCE) {
            return chishi;
        }
        if (type == LifeEnergyType.INSTANCE) {
            return life;
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
        return Component.translatable("block.template_mod.chishi_life_conversion_architecture");
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
