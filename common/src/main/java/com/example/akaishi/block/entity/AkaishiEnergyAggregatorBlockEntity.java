package com.example.akaishi.block.entity;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiEnergyAggregatorMenu;
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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 赤石能量聚合器：消耗赤能源聚合出赤石锭，或逐级升级赤石水晶母岩。
 * 配方：10M 赤能源 + 1 下界合金锭 → 1 赤石锭；10M 赤能源 + 1 母岩 → 下一等级母岩。
 * 赤能源只进不出（由管道供能）。
 */
public class AkaishiEnergyAggregatorBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider, Container, IItemPipeDevice {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 2;

    /** 聚合赤石锭单次消耗的赤能源量 */
    public static final long ENERGY_PER_INGOT = 10_000_000L;
    /** 母岩升一级消耗的赤能源量 */
    public static final long ENERGY_PER_GEODE_UPGRADE = 10_000_000L;
    /** 赤能源缓冲容量 */
    public static final long ENERGY_CAPACITY = 200_000_000L;

    /** 聚合配方：输入物品 → 输出物品 + 能量消耗 */
    private record Recipe(java.util.function.Supplier<Item> input, java.util.function.Supplier<Item> output, long energy) {
        boolean matches(ItemStack stack) {
            return !stack.isEmpty() && stack.is(input.get());
        }
    }

    /** 全部配方：赤石锭聚合 + 母岩三级升级 */
    private static final List<Recipe> RECIPES = List.of(
            new Recipe(() -> Items.NETHERITE_INGOT, () -> ModItems.akaishiIngot.get(), ENERGY_PER_INGOT),
            new Recipe(() -> ModBlocks.CHISHI_GEODE_FLAWED.get().asItem(), () -> ModBlocks.CHISHI_GEODE_NORMAL.get().asItem(), ENERGY_PER_GEODE_UPGRADE),
            new Recipe(() -> ModBlocks.CHISHI_GEODE_NORMAL.get().asItem(), () -> ModBlocks.CHISHI_GEODE_PRISTINE.get().asItem(), ENERGY_PER_GEODE_UPGRADE),
            new Recipe(() -> ModBlocks.CHISHI_GEODE_PRISTINE.get().asItem(), () -> ModBlocks.CHISHI_GEODE_PERFECT.get().asItem(), ENERGY_PER_GEODE_UPGRADE)
    );

    private final AkaishiEnergyStorage energy;
    private final SimpleContainer inventory;
    /** 同步数据：0=能量 1=容量 2=进度% 3=当前配方单次消耗 */
    private final SimpleContainerData data;

    public AkaishiEnergyAggregatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ENERGY_AGGREGATOR.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, ENERGY_CAPACITY);
        this.inventory = new SimpleContainer(SLOT_COUNT);
        this.data = new SimpleContainerData(4);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiEnergyAggregatorBlockEntity be) {
        be.tickServer();
    }

    /** 当前输入物品匹配的配方，无匹配返回 null */
    public Recipe currentRecipe() {
        ItemStack input = inventory.getItem(INPUT_SLOT);
        for (Recipe recipe : RECIPES) {
            if (recipe.matches(input)) {
                return recipe;
            }
        }
        return null;
    }

    private void tickServer() {
        Recipe recipe = currentRecipe();
        if (recipe != null && canProcess(recipe)) {
            process(recipe);
        }
        long stored = energy.getEnergyStored();
        long currentCost = recipe != null ? recipe.energy() : ENERGY_PER_INGOT;
        data.set(0, (int) stored);
        data.set(1, (int) energy.getMaxEnergy());
        // 进度 = 当次聚合的充能进度（能量 / 当前配方消耗，long 计算防溢出）
        data.set(2, (int) Math.min(100, stored * 100L / currentCost));
        data.set(3, (int) currentCost);
    }

    /** 条件：能量足够 + 输入匹配配方 + 输出可容纳产物 */
    private boolean canProcess(Recipe recipe) {
        if (energy.getEnergyStored() < recipe.energy()) {
            return false;
        }
        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        ItemStack result = new ItemStack(recipe.output().get());
        return output.isEmpty() || (output.is(result.getItem()) && output.getCount() + result.getCount() <= result.getMaxStackSize());
    }

    /** 执行聚合：消耗能量 + 1 输入 → 产出 1 配方产物 */
    private void process(Recipe recipe) {
        energy.extractEnergy(recipe.energy(), false);
        inventory.removeItem(INPUT_SLOT, 1);
        ItemStack result = new ItemStack(recipe.output().get());
        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        if (output.isEmpty()) {
            inventory.setItem(OUTPUT_SLOT, result);
        } else {
            output.grow(1);
        }
        setChanged();
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    @Override
    public IEnergyStorage getEnergyStorage(IEnergyType type) {
        return type == AkaishiEnergyType.INSTANCE ? energy : null;
    }

    @Override
    public boolean canInputEnergy(IEnergyType type) {
        return type == AkaishiEnergyType.INSTANCE;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        return false;
    }

    public ContainerData data() {
        return data;
    }

    public SimpleContainer inventory() {
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_energy_aggregator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiEnergyAggregatorMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    // ---- Container（原版容器接口，AE2 存储总线 / Mekanism 物流管道可直接访问槽位） ----

    // ---- IItemPipeDevice：物品管道精准对接——原料进 0 号输入槽，产物从 1 号输出槽取走 ----

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{INPUT_SLOT};
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

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
        tag.putLong("Energy", energy.getEnergyStored());
        tag.put("Inventory", inventory.createTag());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        inventory.fromTag(tag.getList("Inventory", net.minecraft.nbt.Tag.TAG_COMPOUND));
    }
}
