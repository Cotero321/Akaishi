package com.example.template.block.entity;

import com.example.template.api.IDataCarrier;
import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.api.energy.IEnergyType;
import com.example.template.block.ChishiLifeMatrixCasingBlock;
import com.example.template.block.ChishiLifeMatrixControllerBlock;
import com.example.template.block.ChishiLifeMatrixEnergyInputPortBlock;
import com.example.template.block.ChishiLifeMatrixEnergyOutputPortBlock;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.ChishiEnergyType;
import com.example.template.energy.LifeEnergyType;
import com.example.template.menu.ChishiLifeConverterMenu;
import com.example.template.multiblock.MatrixStructure;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 生命转换矩阵控制器：类反应堆式矩阵主方块（3×3×3）。
 * 结构成型后以 45 倍速率集中转换：每 tick 最多 45 次，每次消耗 10M 赤能源、产出 10 生命能量。
 * 赤能源经能量输入口（或直接管道）注入，生命能量经能量输出口（仅管道抽取）输出。
 * 数据槽：0=赤能源，1=赤能源容量，2=生命能量，3=生命能量容量，4=结构状态（与旧菜单共用布局）。
 */
public class ChishiLifeMatrixControllerBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider, IDataCarrier {

    /** 成型后每 tick 转换次数（= 45 倍单台速率） */
    public static final int CONVERSIONS_PER_TICK = 45;
    /** 单次转换消耗的赤能源量 */
    public static final long CONVERSION_COST = 10_000_000L;
    /** 单次转换产出的生命能量量 */
    public static final long CONVERSION_OUTPUT = 10L;
    /** 中心赤能源缓冲容量（支持一轮 45 次转换后仍有余量） */
    public static final long CHISHI_CAPACITY = 500_000_000L;
    /** 中心生命能量存储容量 */
    public static final long LIFE_CAPACITY = 5000L;
    public static final int DATA_SLOTS = 5;

    private final ChishiEnergyStorage chishi;
    private final ChishiEnergyStorage life;
    private final SimpleContainerData data = new SimpleContainerData(DATA_SLOTS);
    /** 最近一次成型的箱体范围（解除端口关联时使用） */
    private BlockPos boxMin, boxMax;

    public ChishiLifeMatrixControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_MATRIX_CONTROLLER.get(), pos, state);
        this.chishi = new ChishiEnergyStorage(ChishiEnergyType.INSTANCE, CHISHI_CAPACITY);
        this.life = new ChishiEnergyStorage(LifeEnergyType.INSTANCE, LIFE_CAPACITY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiLifeMatrixControllerBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        boolean changed = false;
        boolean formed = getBlockState().getValue(ChishiLifeMatrixControllerBlock.FORMED);
        // 成型后每 10 tick 校验一次即可（结构变化不频繁，减少扫描开销）
        boolean checked = !formed || ++structureTick % 10 == 0;
        MatrixStructure.Result scan = checked
                ? MatrixStructure.scan(level, worldPosition, 3, this::isWall)
                : null;
        boolean valid = checked ? scan != null : formed;
        if (formed != valid) {
            setFormed(valid, scan);
            if (valid) {
                boxMin = scan.min;
                boxMax = scan.max;
            } else {
                boxMin = null;
                boxMax = null;
            }
            formed = valid;
            changed = true;
        }
        // 成型后以 45 倍速率集中转换
        if (formed) {
            for (int i = 0; i < CONVERSIONS_PER_TICK; i++) {
                if (!convert()) {
                    break;
                }
                changed = true;
            }
        }
        // 同步数据到 GUI（与 ChishiLifeConverterMenu 共用布局）
        data.set(0, (int) chishi.getEnergyStored());
        data.set(1, (int) chishi.getMaxEnergy());
        data.set(2, (int) life.getEnergyStored());
        data.set(3, (int) life.getMaxEnergy());
        data.set(4, formed ? 1 : 0);

        if (changed) {
            setChanged();
        }
    }

    /** 单次转换：赤能源充足且生命能量未满时执行 */
    private boolean convert() {
        if (chishi.getEnergyStored() >= CONVERSION_COST
                && life.getEnergyStored() + CONVERSION_OUTPUT <= life.getMaxEnergy()) {
            chishi.extractEnergy(CONVERSION_COST, false);
            life.addEnergy(CONVERSION_OUTPUT, false);
            return true;
        }
        return false;
    }

    /** 墙块判定：矩阵外壳 / 两种能量端口 / 控制器自身（控制器在墙面上） */
    private boolean isWall(Block b) {
        return b instanceof ChishiLifeMatrixCasingBlock
                || b instanceof ChishiLifeMatrixEnergyInputPortBlock
                || b instanceof ChishiLifeMatrixEnergyOutputPortBlock
                || b instanceof ChishiLifeMatrixControllerBlock;
    }

    /** 结构检测节流计数（成型后每 10 tick 校验一次） */
    private int structureTick;

    /** 切换结构状态：同步自身 FORMED 标记，并建立/解除端口与控制器的关联 */
    private void setFormed(boolean formed, MatrixStructure.Result scan) {
        level.setBlock(worldPosition, getBlockState().setValue(ChishiLifeMatrixControllerBlock.FORMED, formed), 3);
        // 解除关联时遍历上次成型的箱体范围
        BlockPos min = formed ? scan.min : boxMin;
        BlockPos max = formed ? scan.max : boxMax;
        if (min == null || max == null) {
            return;
        }
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            BlockEntity be = level.getBlockEntity(p);
            BlockPos link = formed ? worldPosition : null;
            if (be instanceof ChishiLifeMatrixEnergyInputPortBlockEntity in) {
                in.setControllerPos(link);
            } else if (be instanceof ChishiLifeMatrixEnergyOutputPortBlockEntity out) {
                out.setControllerPos(link);
            }
        }
    }

    /** 结构是否成型（端口用） */
    public boolean isFormed() {
        return getBlockState().getValue(ChishiLifeMatrixControllerBlock.FORMED);
    }

    public ChishiEnergyStorage chishiStorage() {
        return chishi;
    }

    public ChishiEnergyStorage lifeStorage() {
        return life;
    }

    public ContainerData data() {
        return data;
    }

    // ===== IEnergyProvider：赤能源只进（原料），生命能量只出不进 =====

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
        return type == ChishiEnergyType.INSTANCE;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE;
    }

    // ===== ExtendedMenuProvider（复用旧生命转换菜单） =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.template_mod.chishi_life_matrix_controller");
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
