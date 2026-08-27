package com.example.template.block.entity;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.block.ModBlocks;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.ChishiEnergyType;
import com.example.template.item.ModItems;
import com.example.template.menu.ChishiPurifierMenu;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 赤石提纯器方块实体（仅服务端驱动逻辑，客户端通过 Menu 数据展示）。
 * 槽位：0=燃料，1=输入（粗制赤石块 / 赤石水晶块），2=输出（赤石精华）。
 * 逻辑：燃烧赤石晶/粗制块产生赤石能量 → 消耗能量提纯输入为赤石精华。
 * 配方：粗制赤石块 → 1 精华；赤石水晶块 → 4 精华。
 */
public class ChishiPurifierBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider, Container {

    public static final int FUEL_SLOT = 0;
    public static final int INPUT_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SLOT_COUNT = 3;

    /** 最大能量存储 */
    public static final int MAX_ENERGY = 10000;
    /** 提纯所需总进度（tick） */
    public static final int MAX_PROGRESS = 100;
    /** 每 tick 提纯消耗能量（需求减半后 10→5，燃烧产能 10/tick 可净积累 5/tick） */
    public static final int ENERGY_PER_TICK = 5;
    /** 每 tick 燃烧产能（产能减半后 20→10，与提纯消耗持平，可配合管道外部供能缓冲） */
    private static final int BURN_RATE = 10;
    /** 燃料能量：赤石晶 */
    public static final int FUEL_CRYSTAL = 200;
    /** 燃料能量：粗制赤石块 */
    public static final int FUEL_RAW_BLOCK = 2000;

    private final SimpleContainer inventory;
    /** 与 Menu 同步的数据缓存：0=能量 1=燃烧时间 2=进度 3=燃烧总时间 */
    private final SimpleContainerData data;
    private ChishiEnergyStorage energy;

    private int burnTime;
    private int burnTimeTotal;
    private int progress;

    public ChishiPurifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_PURIFIER.get(), pos, state);
        this.energy = new ChishiEnergyStorage(ChishiEnergyType.INSTANCE, MAX_ENERGY);
        // 容器变更时标记方块保存，防止进度/物品丢失
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                ChishiPurifierBlockEntity.this.setChanged();
            }
        };
        // 数据缓存：服务端每 tick 写入，客户端经 Menu 同步 set 覆盖，GUI 据此绘制
        this.data = new SimpleContainerData(4);
    }

    /** 服务端 tick：燃烧产能 + 提纯 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiPurifierBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        boolean changed = false;
        // 写入数据缓存：Menu 的 broadcastChanges 每 tick 据此同步到客户端 GUI
        data.set(0, (int) energy.getEnergyStored());
        data.set(1, burnTime);
        data.set(2, progress);
        data.set(3, burnTimeTotal);

        // 1) 燃烧燃料产生赤石能量
        if (energy.getEnergyStored() < MAX_ENERGY) {
            if (burnTime <= 0) {
                int fuel = getFuelEnergy(inventory.getItem(FUEL_SLOT));
                if (fuel > 0) {
                    inventory.removeItem(FUEL_SLOT, 1);
                    // 燃烧时长 = 燃料能量 / 100（燃料消耗节奏加快 100 倍）
                    burnTimeTotal = fuel / 100;
                    burnTime = burnTimeTotal;
                    changed = true;
                }
            }
            if (burnTime > 0) {
                burnTime--;
                energy.addEnergy(BURN_RATE, false);
                changed = true;
            }
        }

        // 2) 消耗能量提纯输入（能量不足时进度暂停，不清零）
        if (canProcess()) {
            if (energy.getEnergyStored() >= ENERGY_PER_TICK) {
                progress++;
                energy.extractEnergy(ENERGY_PER_TICK, false);
                if (progress >= MAX_PROGRESS) {
                    progress = 0;
                    inventory.removeItem(INPUT_SLOT, 1);
                    ItemStack out = inventory.getItem(OUTPUT_SLOT);
                    if (out.isEmpty()) {
                        inventory.setItem(OUTPUT_SLOT, new ItemStack(ModItems.chishiEssence.get(), outputPerInput()));
                    } else {
                        out.grow(outputPerInput());
                    }
                }
                changed = true;
            }
        } else {
            // 无有效输入或输出已满：重置进度
            progress = 0;
        }

        if (changed) {
            setChanged();
        }
    }

    /** 是否具备提纯条件：输入有效 + 输出可容纳（能量检查在 tick 内做，不足时暂停而非清零） */
    private boolean canProcess() {
        ItemStack input = inventory.getItem(INPUT_SLOT);
        if (input.isEmpty() || !isValidInput(input)) {
            return false;
        }
        ItemStack out = inventory.getItem(OUTPUT_SLOT);
        if (out.isEmpty()) {
            return true;
        }
        return out.is(ModItems.chishiEssence.get()) && out.getCount() + outputPerInput() <= out.getMaxStackSize();
    }

    /** 有效输入：粗制赤石块 或 赤石水晶块 */
    private boolean isValidInput(ItemStack stack) {
        return stack.is(ModBlocks.RAW_CHISHI_BLOCK.get().asItem())
                || stack.is(ModBlocks.CHISHI_CRYSTAL_BLOCK.get().asItem());
    }

    /** 单次提纯产出的精华数：赤石水晶块 4 个，粗制赤石块 1 个 */
    private int outputPerInput() {
        return inventory.getItem(INPUT_SLOT).is(ModBlocks.CHISHI_CRYSTAL_BLOCK.get().asItem()) ? 4 : 1;
    }

    /** 燃料能量值，非燃料返回 0 */
    private static int getFuelEnergy(ItemStack stack) {
        if (stack.is(ModItems.chishiCrystal.get())) {
            return FUEL_CRYSTAL;
        }
        if (stack.is(ModBlocks.RAW_CHISHI_BLOCK.get().asItem())) {
            return FUEL_RAW_BLOCK;
        }
        return 0;
    }

    public Container inventory() {
        return inventory;
    }

    // ===== Container：使 AE2 存储总线 / Mekanism 物流管道能直接读写机器槽位（零硬依赖） =====

    @Override
    public int getContainerSize() {
        return inventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return inventory.getItem(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return inventory.removeItem(index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return inventory.removeItemNoUpdate(index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        inventory.setItem(index, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    public ContainerData data() {
        return data;
    }

    public ChishiEnergyStorage energy() {
        return energy;
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    /** 提纯器是纯消耗型机器：只接收管道输入的赤能源，不向外输出 */
    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    /** 允许管道向提纯器注入赤能源（可接入能量网络） */
    @Override
    public boolean canInputEnergy() {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.template_mod.chishi_purifier");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ChishiPurifierMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTimeTotal", burnTimeTotal);
        tag.putInt("Progress", progress);
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // 复用既有 energy 实例恢复，避免反复重建 storage 导致外部引用失效、能量不累积
        energy.setEnergy(tag.getLong("Energy"));
        burnTime = tag.getInt("BurnTime");
        burnTimeTotal = tag.getInt("BurnTimeTotal");
        progress = tag.getInt("Progress");
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < SLOT_COUNT; i++) {
            inventory.setItem(i, items.get(i));
        }
    }
}
