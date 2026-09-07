package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.block.AkaishiEnergyCellBlock;
import com.example.akaishi.block.AkaishiEnergyCellSerializerBlock;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyCellArrayStorage;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.menu.AkaishiEnergyCellSerializerMenu;
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
 * 赤能源储存串联器方块实体（3×3×3 多方块结构主方块）：
 * 26 个赤能源储存单元环绕成型（formed=true）后，将全部单元容量聚合为单一存储，
 * 管道连接任一外壳单元即可对整体充放能量。数据槽：0/1=总能量低/高位，2/3=总容量低/高位，4=结构状态。
 */
public class AkaishiEnergyCellSerializerBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider, IDataCarrier {

    private final AkaishiEnergyStorage energy;
    private final AkaishiEnergyCellArrayStorage arrayStorage;
    private final SimpleContainerData data;

    public AkaishiEnergyCellSerializerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ENERGY_CELL_SERIALIZER.get(), pos, state);
        // 串联器自身基础容量（成型后总容量 = 该值 + 26 个外壳单元容量之和）
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, ModConfig.energyCellSerializerBaseCapacity);
        this.arrayStorage = new AkaishiEnergyCellArrayStorage(AkaishiEnergyType.INSTANCE, this::collectMembers);
        this.data = new SimpleContainerData(5);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiEnergyCellSerializerBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 结构校验：26 个储存单元环绕则成型，否则恢复为独立单体
        boolean formed = isStructureValid();
        BlockState blockState = level.getBlockState(worldPosition);
        if (formed != blockState.getValue(AkaishiEnergyCellSerializerBlock.FORMED)) {
            level.setBlock(worldPosition, blockState.setValue(AkaishiEnergyCellSerializerBlock.FORMED, formed), 3);
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

    /** 结构校验：3×3×3 除中心外 26 个位置均为赤能源储存单元（任意等级） */
    private boolean isStructureValid() {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    if (!(level.getBlockState(worldPosition.offset(dx, dy, dz)).getBlock() instanceof AkaishiEnergyCellBlock)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** 收集全部成员存储：中心自身 + 26 个外壳储存单元 */
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
                    if (be instanceof AkaishiEnergyCellBlockEntity cell) {
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

    public ContainerData data() {
        return data;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_energy_cell_serializer");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiEnergyCellSerializerMenu(id, inv, data);
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
