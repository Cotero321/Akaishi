package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.block.AkaishiLifeEnergyCellBlock;
import com.example.akaishi.block.AkaishiLifeEnergyCellSerializerBlock;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyCellArrayStorage;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.menu.AkaishiLifeEnergyCellSerializerMenu;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 生命储存串联器方块实体（3×3×3 多方块结构主方块）：
 * 26 台生命能量储存器环绕成型（formed=true）后，将全部单元容量聚合成单一生命能量存储，
 * 管道连接任一外壳储存器即可对整体充放能量。数据槽：0/1=总能量低/高位，2/3=总容量低/高位，4=结构状态。
 */
public class AkaishiLifeEnergyCellSerializerBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider, IDataCarrier {

    private final AkaishiEnergyStorage energy;
    private final AkaishiEnergyCellArrayStorage arrayStorage;
    private final SimpleContainerData data;

    public AkaishiLifeEnergyCellSerializerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_ENERGY_CELL_SERIALIZER.get(), pos, state);
        // 串联器自身基础容量（成型后总容量 = 该值 + 26 个外壳储存器容量之和）
        this.energy = new AkaishiEnergyStorage(LifeEnergyType.INSTANCE, ModConfig.lifeEnergyCellSerializerBaseCapacity);
        this.arrayStorage = new AkaishiEnergyCellArrayStorage(LifeEnergyType.INSTANCE, this::collectMembers);
        this.data = new SimpleContainerData(5);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiLifeEnergyCellSerializerBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 结构校验：26 台生命能量储存器环绕则成型，否则恢复为独立单体
        boolean formed = isStructureValid();
        BlockState blockState = level.getBlockState(worldPosition);
        if (formed != blockState.getValue(AkaishiLifeEnergyCellSerializerBlock.FORMED)) {
            level.setBlock(worldPosition, blockState.setValue(AkaishiLifeEnergyCellSerializerBlock.FORMED, formed), 3);
        }

        // 同步总能量/总容量（long 拆 4 槽）+ 结构状态到 GUI
        long stored = arrayStorage.getEnergyStored();
        long max = arrayStorage.getMaxEnergy();
        data.set(0, (int) stored);
        data.set(1, (int) (stored >>> 32));
        data.set(2, (int) max);
        data.set(3, (int) (max >>> 32));
        data.set(4, formed ? 1 : 0);
    }

    /** 结构校验：3×3×3 除中心外 26 个位置均为生命能量储存器（任意等级） */
    private boolean isStructureValid() {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    if (!(level.getBlockState(worldPosition.offset(dx, dy, dz)).getBlock() instanceof AkaishiLifeEnergyCellBlock)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** 收集全部成员存储：中心自身 + 26 个外壳储存器 */
    private List<IEnergyStorage> collectMembers() {
        List<IEnergyStorage> members = new ArrayList<>();
        members.add(energy);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockEntity be = level.getBlockEntity(worldPosition.offset(dx, dy, dz));
                    if (be instanceof AkaishiLifeEnergyCellBlockEntity cell) {
                        members.add(cell.energy());
                    }
                }
            }
        }
        return members;
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        return arrayStorage;
    }

    @Override
    public IEnergyStorage getEnergyStorage(IEnergyType type) {
        // 只暴露生命能量存储（赤管道通过类型匹配自动跳过本方块）
        return type == LifeEnergyType.INSTANCE ? arrayStorage : null;
    }

    @Override
    public boolean canInputEnergy(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE;
    }

    public ContainerData data() {
        return data;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_life_energy_cell_serializer");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiLifeEnergyCellSerializerMenu(id, inv, data);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
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
