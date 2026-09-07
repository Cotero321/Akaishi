package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.item.AkaishiUpgradeHelper;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiUpgradeStationMenu;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 赤红升级台：为赤石装备应用高级升级（特殊能力）。
 * 消耗 1 升级模板 + 20M 赤能源 + 1 槽位，所选能力等级 +1（每种能力 ≤3 级）：
 * 吸血 / 击退抗性 / 移动速度 / 火焰抗性 / 爆炸伤害保护 / 摔落伤害保护。
 * 升级选项由 GUI 按钮选择（clickMenuButton），执行由按钮触发。
 */
public class AkaishiUpgradeStationBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider, Container, IItemPipeDevice, IDataCarrier {

    public static final int INPUT_GEAR_SLOT = 0;
    public static final int INPUT_TEMPLATE_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SLOT_COUNT = 3;

    private final AkaishiEnergyStorage energy;
    private final SimpleContainer inventory;
    private final SimpleContainerData data;

    /** 当前选择的特殊能力下标（SpecialAbility.values() 顺序） */
    private int selectedType;

    public AkaishiUpgradeStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_UPGRADE_STATION.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, ModConfig.upgradeStationEnergyCapacity);
        this.inventory = new SimpleContainer(SLOT_COUNT);
        this.data = new SimpleContainerData(5);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiUpgradeStationBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        data.set(0, (int) energy.getEnergyStored());
        data.set(1, (int) energy.getMaxEnergy());
        data.set(2, selectedType);
        ItemStack gear = inventory.getItem(INPUT_GEAR_SLOT);
        // 惰性初始化：give/创造模式直接取用的赤石装备无标签，放入后自动补齐
        AkaishiUpgradeHelper.ensureGear(gear);
        data.set(3, AkaishiUpgradeHelper.isAkaishiGear(gear) ? AkaishiUpgradeHelper.getSlots(gear) : 0);
        data.set(4, AkaishiUpgradeHelper.isAkaishiGear(gear) ? 1 : 0);
        // 当前选中能力不适用于装备部位时，自动重置为第一个可用能力
        AkaishiUpgradeHelper.SpecialAbility[] abilities = AkaishiUpgradeHelper.SpecialAbility.values();
        if (selectedType < 0 || selectedType >= abilities.length || !abilities[selectedType].isApplicable(gear)) {
            for (int i = 0; i < abilities.length; i++) {
                if (abilities[i].isApplicable(gear)) {
                    selectedType = i;
                    break;
                }
            }
        }
    }

    public void setSelectedType(int type) {
        if (type >= 0 && type < AkaishiUpgradeHelper.SpecialAbility.values().length) {
            this.selectedType = type;
            setChanged();
        }
    }

    /** 尝试执行一次升级：条件满足时应用并消耗资源 */
    public void tryUpgrade() {
        ItemStack gear = inventory.getItem(INPUT_GEAR_SLOT);
        ItemStack template = inventory.getItem(INPUT_TEMPLATE_SLOT);
        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        AkaishiUpgradeHelper.ensureGear(gear);
        if (energy.getEnergyStored() < ModConfig.upgradeStationEnergyPerUpgrade
                || !AkaishiUpgradeHelper.isAkaishiGear(gear)
                || !template.is(ModItems.akaishiUpgradeTemplate.get())
                || !output.isEmpty()) {
            return;
        }
        AkaishiUpgradeHelper.SpecialAbility[] abilities = AkaishiUpgradeHelper.SpecialAbility.values();
        if (selectedType < 0 || selectedType >= abilities.length) {
            return;
        }
        // 服务端校验：所选能力必须适用于当前装备部位（防 GUI 欺骗）
        if (!abilities[selectedType].isApplicable(gear)) {
            return;
        }
        if (!AkaishiUpgradeHelper.applyAbility(gear, abilities[selectedType])) {
            return;
        }
        energy.extractEnergy(ModConfig.upgradeStationEnergyPerUpgrade, false);
        inventory.removeItem(INPUT_TEMPLATE_SLOT, 1);
        inventory.setItem(OUTPUT_SLOT, gear.copy());
        inventory.removeItem(INPUT_GEAR_SLOT, 1);
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
        return Component.translatable("block.akaishi.akaishi_upgrade_station");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiUpgradeStationMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
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
        tag.putInt("SelectedType", selectedType);
        tag.put("Inventory", inventory.createTag());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        selectedType = tag.getInt("SelectedType");
        inventory.fromTag(tag.getList("Inventory", net.minecraft.nbt.Tag.TAG_COMPOUND));
    }
}
