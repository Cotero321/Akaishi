package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.api.storage.IStorageVault;
import com.example.akaishi.life.sample.AkaishiLifeSampleItem;
import com.example.akaishi.menu.AkaishiSampleVaultMenu;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 样本库（样本冷藏柜）：大容量生命样本仓库。
 * - 容量：54 格（6 行 × 9 列）
 * - 自动合并：周期扫描同 NBT（来源分组 + 纯度 + 生物）的样本自动合并堆叠，
 *   解决"NBT 不同不堆叠"导致占格的问题
 * - 归类：canPlaceItem 仅接受生命样本（纯度任意，分析台/药剂台按需取用）
 * - 管道：输入输出全槽，只收样本
 * 实现 {@link IStorageVault}：供 3 格内机器（分析台等）联动浮层存取。
 */
public class AkaishiSampleVaultBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IItemPipeDevice, IStorageVault, IDataCarrier {

    public static final int SAMPLE_SLOTS = 54;
    /** 合并扫描节流（tick） */
    public static final int MERGE_INTERVAL = 10;

    private final SimpleContainer inventory;
    private int mergeTick;

    public AkaishiSampleVaultBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_SAMPLE_VAULT.get(), pos, state);
        this.inventory = new SimpleContainer(SAMPLE_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiSampleVaultBlockEntity.this.setChanged();
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiSampleVaultBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        if (++mergeTick % MERGE_INTERVAL != 0) {
            return;
        }
        mergeStacks();
    }

    /** 同 NBT 样本自动合并堆叠（合并后调用方只保留一份，减少占格） */
    private void mergeStacks() {
        boolean changed = false;
        for (int i = 0; i < SAMPLE_SLOTS; i++) {
            ItemStack a = inventory.getItem(i);
            if (a.isEmpty() || a.getCount() >= a.getMaxStackSize()) {
                continue;
            }
            for (int j = i + 1; j < SAMPLE_SLOTS; j++) {
                ItemStack b = inventory.getItem(j);
                if (b.isEmpty()) {
                    continue;
                }
                if (ItemStack.isSameItemSameTags(a, b)) {
                    int move = Math.min(b.getCount(), a.getMaxStackSize() - a.getCount());
                    if (move > 0) {
                        a.grow(move);
                        b.shrink(move);
                        if (b.isEmpty()) {
                            inventory.setItem(j, ItemStack.EMPTY);
                        }
                        changed = true;
                        if (a.getCount() >= a.getMaxStackSize()) {
                            break;
                        }
                    }
                }
            }
        }
        if (changed) {
            setChanged();
        }
    }

    // ===== 放置校验 =====

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return stack.getItem() instanceof AkaishiLifeSampleItem;
    }

    // ===== 界面 =====

    public Container inventory() {
        return inventory;
    }

    // ===== Container：管道 / 漏斗读写 =====

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
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    // ===== IItemPipeDevice：只收样本，输入输出全槽 =====

    @Override
    public int[] getPipeInputSlots() {
        return allSlots();
    }

    @Override
    public int[] getPipeOutputSlots() {
        return allSlots();
    }

    private int[] allSlots() {
        int[] slots = new int[SAMPLE_SLOTS];
        for (int i = 0; i < SAMPLE_SLOTS; i++) {
            slots[i] = i;
        }
        return slots;
    }

    // ===== IStorageVault：机器联动浮层存取 =====

    @Override
    public String getVaultNameKey() {
        return "block.akaishi.akaishi_sample_vault";
    }

    @Override
    public Container getVaultContainer() {
        return inventory;
    }

    // ===== 界面工厂 =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_sample_vault");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiSampleVaultMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        NonNullList<ItemStack> items = NonNullList.withSize(SAMPLE_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < SAMPLE_SLOTS; i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        NonNullList<ItemStack> items = NonNullList.withSize(SAMPLE_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < SAMPLE_SLOTS; i++) {
            inventory.setItem(i, items.get(i));
        }
    }
}
