package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiLifeFusionAnvilMenu;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 生命的融合砧：将赤石护甲与生命的融合锭融合为生命融合护甲。
 * 消耗 1 枚融合锭，完整复制原装备 NBT（保留升级数据），不消耗能量。
 * 仅支持赤石护甲 4 件（头盔/胸甲/护腿/靴子）。
 */
public class AkaishiLifeFusionAnvilBlockEntity extends BlockEntity implements ExtendedMenuProvider, Container, IDataCarrier {

    public static final int INPUT_GEAR_SLOT = 0;
    public static final int INPUT_INGOT_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SLOT_COUNT = 3;

    private final SimpleContainer inventory = new SimpleContainer(SLOT_COUNT);

    public AkaishiLifeFusionAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_FUSION_ANVIL.get(), pos, state);
    }

    /** 查找赤石护甲对应的融合目标（非赤石护甲返回 null） */
    public static Item fusionTargetFor(ItemStack gear) {
        Item item = gear.getItem();
        if (item == ModItems.akaishiHelmet.get()) {
            return ModItems.lifeFusionHelmet.get();
        }
        if (item == ModItems.akaishiChestplate.get()) {
            return ModItems.lifeFusionChestplate.get();
        }
        if (item == ModItems.akaishiLeggings.get()) {
            return ModItems.lifeFusionLeggings.get();
        }
        if (item == ModItems.akaishiBoots.get()) {
            return ModItems.lifeFusionBoots.get();
        }
        return null;
    }

    /** 是否为可融合的赤石护甲 */
    public static boolean isFusionGear(ItemStack gear) {
        return fusionTargetFor(gear) != null;
    }

    /** 可融合：赤石护甲 + 1 枚融合锭 + 输出为空 */
    private boolean canFuse() {
        ItemStack gear = inventory.getItem(INPUT_GEAR_SLOT);
        if (!isFusionGear(gear)) {
            return false;
        }
        ItemStack ingot = inventory.getItem(INPUT_INGOT_SLOT);
        if (!ingot.is(ModItems.lifeFusionIngot.get()) || ingot.getCount() < 1) {
            return false;
        }
        return inventory.getItem(OUTPUT_SLOT).isEmpty();
    }

    /** 玩家点击融合按钮时调用：条件满足则执行融合 */
    public void tryFuse() {
        if (canFuse()) {
            fuse();
        }
    }

    /** 执行融合：消耗赤石护甲 + 1 融合锭 → 产出保留升级数据的生命融合护甲 */
    private void fuse() {
        ItemStack gear = inventory.getItem(INPUT_GEAR_SLOT);
        Item target = fusionTargetFor(gear);
        if (target == null) {
            return;
        }
        inventory.removeItem(INPUT_GEAR_SLOT, 1);
        inventory.removeItem(INPUT_INGOT_SLOT, 1);
        ItemStack result = new ItemStack(target);
        // 复制原装备完整 NBT（含 AkaishiGear 升级子标签），保留升级数据
        CompoundTag tag = gear.getTag();
        if (tag != null) {
            result.setTag(tag.copy());
        }
        inventory.setItem(OUTPUT_SLOT, result);
        setChanged();
    }

    public SimpleContainer inventory() {
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_life_fusion_anvil");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiLifeFusionAnvilMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    // ---- Container（漏斗 / 物流管道可直接访问槽位） ----

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
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.createTag());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.fromTag(tag.getList("Inventory", net.minecraft.nbt.Tag.TAG_COMPOUND));
    }
}
