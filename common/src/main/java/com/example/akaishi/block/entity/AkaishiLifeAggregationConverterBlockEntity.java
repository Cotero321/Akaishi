package com.example.akaishi.block.entity;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.menu.AkaishiLifeConverterMenu;
import com.example.akaishi.util.LongDataSlots;
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
 * 生命聚合转换器：单方块独立转换器（赤能源 → 生命能量）。
 * 每 tick 恒定转换 1 次（复用旧转换配置），右键打开 GUI。
 * 数据槽：0/1=赤能源低/高，2/3=赤容量低/高，4/5=生命能量低/高，6/7=生命容量低/高，
 * 8=预留(0)，9=独立转换标记(1)。
 */
public class AkaishiLifeAggregationConverterBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider {

    public static final int DATA_SLOTS = 10;

    /** 独立转换标记槽（AkaishiLifeConverterMenu 据此显示"单台 · 独立转换"） */
    private static final int DATA_STANDALONE = 9;

    private final AkaishiEnergyStorage akaishi;
    private final AkaishiEnergyStorage life;
    private final SimpleContainerData data = new SimpleContainerData(DATA_SLOTS);

    public AkaishiLifeAggregationConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_AGGREGATION_CONVERTER.get(), pos, state);
        this.akaishi = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, ModConfig.lifeAggregationChishiCapacity);
        this.life = new AkaishiEnergyStorage(LifeEnergyType.INSTANCE, ModConfig.lifeAggregationLifeCapacity);
        data.set(DATA_STANDALONE, 1);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiLifeAggregationConverterBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        boolean changed = convert();
        // long 拆高低 32 位（SimpleContainerData 仅支持 int）
        LongDataSlots.write(data, 0, 1, akaishi.getEnergyStored());
        LongDataSlots.write(data, 2, 3, akaishi.getMaxEnergy());
        LongDataSlots.write(data, 4, 5, life.getEnergyStored());
        LongDataSlots.write(data, 6, 7, life.getMaxEnergy());
        if (changed) {
            setChanged();
        }
    }

    /** 单次转换：赤能源充足且生命能量未满时执行（每 tick 恒 1 次） */
    private boolean convert() {
        if (akaishi.getEnergyStored() >= ModConfig.lifeAggregationConversionCost
                && life.getEnergyStored() + ModConfig.lifeAggregationConversionOutput <= life.getMaxEnergy()) {
            akaishi.extractEnergy(ModConfig.lifeAggregationConversionCost, false);
            life.addEnergy(ModConfig.lifeAggregationConversionOutput, false);
            return true;
        }
        return false;
    }

    public ContainerData data() {
        return data;
    }

    // ===== IEnergyProvider：赤能源只进（原料），生命能量只出不进 =====

    @Override
    public IEnergyStorage getEnergyStorage() {
        return akaishi;
    }

    @Override
    public IEnergyStorage getEnergyStorage(IEnergyType type) {
        if (type == AkaishiEnergyType.INSTANCE) {
            return akaishi;
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
        return type == AkaishiEnergyType.INSTANCE;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE;
    }

    // ===== ExtendedMenuProvider（复用旧生命转换菜单） =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_life_aggregation_converter");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiLifeConverterMenu(id, inv, data);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("AkaishiEnergy", akaishi.getEnergyStored());
        tag.putLong("LifeEnergy", life.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        akaishi.setEnergy(tag.getLong("AkaishiEnergy"));
        life.setEnergy(tag.getLong("LifeEnergy"));
    }
}
