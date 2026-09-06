package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.item.AkaishiUpgradeHelper;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiEquipmentForgerMenu;
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

import java.util.Map;
import java.util.function.Supplier;

/**
 * 赤石装备打造器：消耗赤能源 + 赤石锭，将下界合金装备重铸为赤石装备。
 * 基础升级点（5 属性，可重复投点，总计 ≤4）为可选：不分配也能直接锻造，
 * 每次基础升级额外消耗 ENERGY_PER_BASE_UPGRADE 赤能源，锻造费用按实际选点数动态计算。
 * 产出装备携带已选基础升级并初始拥有 4 个升级槽位（高级升级由赤红升级台提供）。
 * 重铸配方：下界合金装备 → 对应赤石装备 + 消耗锭数（头盔 5 / 胸甲 8 / 护腿 7 / 靴子 4 / 剑 2）。
 */
public class AkaishiEquipmentForgerBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider, Container, IItemPipeDevice, IDataCarrier {

    public static final int INPUT_GEAR_SLOT = 0;
    public static final int INPUT_INGOT_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SLOT_COUNT = 3;

    /** 重铸基础消耗的赤能源量 */
    public static final long ENERGY_PER_FORGE = 50_000_000L;
    /** 赤能源缓冲容量（满能可承担 4 点满配重铸：50M + 4×10M = 90M） */
    public static final long ENERGY_CAPACITY = 100_000_000L;

    /** 当前锻造总能耗 = 基础 + 已选升级点数 × 单点（动态计算） */
    public long getCurrentCost() {
        long points = 0;
        for (int c : baseCounts) {
            points += c;
        }
        return ENERGY_PER_FORGE + points * AkaishiUpgradeHelper.ENERGY_PER_BASE_UPGRADE;
    }

    /** data 布局：0=能量 1=最大 2=充能进度 3=剩余升级点 4-9=6 种属性已选次数 */
    public static final int DATA_SIZE = 10;

    /** 重铸配方：下界合金装备 → (赤石装备, 消耗锭数) */
    private static final Map<Item, ForgeRecipe> FORGE_RECIPES = Map.of(
            Items.NETHERITE_HELMET, new ForgeRecipe(() -> ModItems.akaishiHelmet.get(), 5),
            Items.NETHERITE_CHESTPLATE, new ForgeRecipe(() -> ModItems.akaishiChestplate.get(), 8),
            Items.NETHERITE_LEGGINGS, new ForgeRecipe(() -> ModItems.akaishiLeggings.get(), 7),
            Items.NETHERITE_BOOTS, new ForgeRecipe(() -> ModItems.akaishiBoots.get(), 4),
            Items.NETHERITE_SWORD, new ForgeRecipe(() -> ModItems.akaishiSword.get(), 2),
            Items.NETHERITE_PICKAXE, new ForgeRecipe(() -> ModItems.akaishiPickaxe.get(), 3),
            Items.NETHERITE_SHOVEL, new ForgeRecipe(() -> ModItems.akaishiShovel.get(), 1),
            Items.NETHERITE_AXE, new ForgeRecipe(() -> ModItems.akaishiAxe.get(), 3));

    private final AkaishiEnergyStorage energy;
    private final SimpleContainer inventory;
    private final SimpleContainerData data;

    /** 剩余基础升级点（每件装备 FORGE_UPGRADE_POINTS 个） */
    private int upgradePoints = AkaishiUpgradeHelper.FORGE_UPGRADE_POINTS;
    /** 5 种基础升级已选次数（与 UpgradeType 顺序一致） */
    private final int[] baseCounts = new int[AkaishiUpgradeHelper.UpgradeType.values().length];
    /** 上一 tick 输入槽装备（用于检测更换装备 → 重置选点） */
    private ItemStack lastGear = ItemStack.EMPTY;
    /** 上一 tick 输出槽是否有物品（用于检测产物被取走 → 重置选点） */
    private boolean lastOutputNonEmpty;

    public AkaishiEquipmentForgerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_EQUIPMENT_FORGER.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, ENERGY_CAPACITY);
        this.inventory = new SimpleContainer(SLOT_COUNT);
        this.data = new SimpleContainerData(DATA_SIZE);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiEquipmentForgerBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 更换输入装备 / 取走产物 → 重置升级选点，让玩家重新选择
        ItemStack gear = inventory.getItem(INPUT_GEAR_SLOT);
        if (!ItemStack.isSameItemSameTags(gear, lastGear)) {
            resetUpgradePoints();
            lastGear = gear.copy();
        }
        boolean outputNonEmpty = !inventory.getItem(OUTPUT_SLOT).isEmpty();
        if (lastOutputNonEmpty && !outputNonEmpty) {
            resetUpgradePoints();
        }
        lastOutputNonEmpty = outputNonEmpty;

        long stored = energy.getEnergyStored();
        data.set(0, (int) stored);
        data.set(1, (int) energy.getMaxEnergy());
        // 充能进度（long 计算防溢出，费用按当前选点动态计算）
        long cost = getCurrentCost();
        data.set(2, (int) Math.min(100, stored * 100L / cost));
        data.set(3, upgradePoints);
        for (int i = 0; i < baseCounts.length; i++) {
            data.set(4 + i, baseCounts[i]);
        }
    }

    /** 重置升级选点（更换输入装备或取走产物时调用） */
    private void resetUpgradePoints() {
        upgradePoints = AkaishiUpgradeHelper.FORGE_UPGRADE_POINTS;
        java.util.Arrays.fill(baseCounts, 0);
        setChanged();
    }

    /** 玩家点击锻造按钮时调用：条件满足则执行重铸 */
    public void tryForge() {
        if (canForge()) {
            forge();
        }
    }

    /** 分配 1 个升级点到指定属性（GUI 按钮调用）。返回是否成功。 */
    public boolean addBaseUpgrade(int typeId) {
        if (upgradePoints <= 0 || typeId < 0 || typeId >= baseCounts.length
                || baseCounts[typeId] >= AkaishiUpgradeHelper.FORGE_UPGRADE_POINTS) {
            return false;
        }
        // 效率升级仅对挖掘类工具（铲/斧/镐）开放
        if (typeId == AkaishiUpgradeHelper.UpgradeType.EFFICIENCY.ordinal()
                && !AkaishiUpgradeHelper.isEfficiencyTool(inventory.getItem(INPUT_GEAR_SLOT))) {
            return false;
        }
        baseCounts[typeId]++;
        upgradePoints--;
        setChanged();
        return true;
    }

    /** 撤销 1 个升级点（GUI 按钮调用）。返回是否成功。 */
    public boolean removeBaseUpgrade(int typeId) {
        if (typeId < 0 || typeId >= baseCounts.length || baseCounts[typeId] <= 0
                || upgradePoints >= AkaishiUpgradeHelper.FORGE_UPGRADE_POINTS) {
            return false;
        }
        baseCounts[typeId]--;
        upgradePoints++;
        setChanged();
        return true;
    }

    /** 查找下界合金装备对应的重铸配方，无则 null */
    private ForgeRecipe findRecipe(ItemStack stack) {
        return FORGE_RECIPES.get(stack.getItem());
    }

    /** 下界合金装备 → 重铸消耗锭数（非配方装备返回 0）。供 Menu/Screen 判断锻造是否就绪。 */
    public static int ingotCostFor(ItemStack stack) {
        if (stack.is(Items.NETHERITE_HELMET)) {
            return 5;
        }
        if (stack.is(Items.NETHERITE_CHESTPLATE)) {
            return 8;
        }
        if (stack.is(Items.NETHERITE_LEGGINGS)) {
            return 7;
        }
        if (stack.is(Items.NETHERITE_BOOTS)) {
            return 4;
        }
        if (stack.is(Items.NETHERITE_SWORD)) {
            return 2;
        }
        if (stack.is(Items.NETHERITE_PICKAXE)) {
            return 3;
        }
        if (stack.is(Items.NETHERITE_SHOVEL)) {
            return 1;
        }
        if (stack.is(Items.NETHERITE_AXE)) {
            return 3;
        }
        return 0;
    }

    /** 可锻造：升级点可留空（不选也能锻造）、能量（按当前选点计费）/锭/装备满足、输出为空 */
    private boolean canForge() {
        ItemStack gear = inventory.getItem(INPUT_GEAR_SLOT);
        ForgeRecipe recipe = findRecipe(gear);
        if (recipe == null || energy.getEnergyStored() < getCurrentCost()) {
            return false;
        }
        ItemStack ingot = inventory.getItem(INPUT_INGOT_SLOT);
        if (!ingot.is(ModItems.akaishiIngot.get()) || ingot.getCount() < recipe.ingotCost) {
            return false;
        }
        return inventory.getItem(OUTPUT_SLOT).isEmpty();
    }

    /** 执行重铸：消耗赤能源 + 装备 + 锭 → 产出带所选基础升级的赤石装备，重置点数 */
    private void forge() {
        ForgeRecipe recipe = findRecipe(inventory.getItem(INPUT_GEAR_SLOT));
        if (recipe == null) {
            return;
        }
        energy.extractEnergy(getCurrentCost(), false);
        inventory.removeItem(INPUT_GEAR_SLOT, 1);
        inventory.removeItem(INPUT_INGOT_SLOT, recipe.ingotCost);
        ItemStack result = new ItemStack(recipe.result.get());
        AkaishiUpgradeHelper.initGear(result);
        for (int i = 0; i < baseCounts.length; i++) {
            for (int n = 0; n < baseCounts[i]; n++) {
                AkaishiUpgradeHelper.addBaseUpgrade(result, AkaishiUpgradeHelper.UpgradeType.values()[i]);
            }
        }
        inventory.setItem(OUTPUT_SLOT, result);
        upgradePoints = AkaishiUpgradeHelper.FORGE_UPGRADE_POINTS;
        java.util.Arrays.fill(baseCounts, 0);
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
        return Component.translatable("block.akaishi.akaishi_equipment_forger");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiEquipmentForgerMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    // ===== IItemPipeDevice：装备/赤石锭槽可入、锻造结果槽仅出 =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{INPUT_GEAR_SLOT, INPUT_INGOT_SLOT};
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

    // ---- Container（AE2 存储总线 / Mekanism 物流管道可直接访问槽位） ----

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
        tag.putInt("UpgradePoints", upgradePoints);
        int[] counts = new int[baseCounts.length];
        System.arraycopy(baseCounts, 0, counts, 0, baseCounts.length);
        tag.putIntArray("BaseCounts", counts);
        tag.put("Inventory", inventory.createTag());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        upgradePoints = tag.getInt("UpgradePoints");
        int[] saved = tag.getIntArray("BaseCounts");
        if (saved.length == baseCounts.length) {
            System.arraycopy(saved, 0, baseCounts, 0, baseCounts.length);
        }
        inventory.fromTag(tag.getList("Inventory", net.minecraft.nbt.Tag.TAG_COMPOUND));
    }

    /** 重铸配方：目标赤石装备 + 消耗锭数 */
    private record ForgeRecipe(Supplier<Item> result, int ingotCost) {
    }
}
