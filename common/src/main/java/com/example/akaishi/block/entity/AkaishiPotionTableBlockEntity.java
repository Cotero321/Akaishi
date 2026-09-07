package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.potion.AkaishiPotionItem;
import com.example.akaishi.life.potion.PotionRegistry;
import com.example.akaishi.life.potion.PotionTemplate;
import com.example.akaishi.life.sample.AkaishiLifeSampleItem;
import com.example.akaishi.menu.AkaishiPotionTableMenu;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 药剂台方块实体（仅服务端驱动逻辑）：
 * - 消耗：1 生命样本（纯度 ≥25）+ 模板固态物 + 模板生命能量
 * - 产出：药剂物品（模板 id + 样本纯度写入 NBT，纯度影响突破药剂副作用强度）
 * - 界面选择模板（永久/突破）后自动加工，无失败风险
 * 槽位：0=样本 1=固态 2=药剂输出。
 */
public class AkaishiPotionTableBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IItemPipeDevice, IDataCarrier, IUpgradeableMachine {

    /** 样本解构门槛：纯度 ≥25 才可用于药剂制作（与分析台一致） */
    public static final int MIN_PURITY = 25;

    public static final int SAMPLE_SLOT = 0;
    public static final int SOLID_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SLOT_COUNT = 3;
    /** Menu 同步数据槽：0/1=生命能量/容量 2=进度% 3=模板索引（-1 未选择） */
    public static final int DATA_SLOTS = 4;
    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_TEMPLATE = 3;

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final AkaishiEnergyStorage life;
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    private int progress;
    /** 速度升级小数余量（避免 (int) 截断使 1~7 级升级无效） */
    private float speedAccum;
    /** 当前选中的模板 id（"" 表示未选择） */
    private String selectedTemplate = "";

    public AkaishiPotionTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_POTION_TABLE.get(), pos, state);
        this.life = new AkaishiEnergyStorage(LifeEnergyType.INSTANCE, ModConfig.potionTableLifeCapacity);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiPotionTableBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiPotionTableBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 动态扩容：能量升级组件生效时按倍率提升生命能量上限
        life.setMaxEnergy((long) (ModConfig.potionTableLifeCapacity * getEnergyCapacityMultiplier()));
        data.set(DATA_ENERGY, (int) life.getEnergyStored());
        data.set(DATA_CAPACITY, (int) life.getMaxEnergy());
        data.set(DATA_TEMPLATE, templateIndex());

        PotionTemplate template = PotionRegistry.get(selectedTemplate);
        if (canProcess(template)) {
            // 速度升级：每 tick 进度按倍率累加（小数余量累积避免截断）
            speedAccum += getSpeedMultiplier();
            int delta = (int) speedAccum;
            if (delta > 0) {
                speedAccum -= delta;
                progress += delta;
            }
            if (progress >= template.ticks()) {
                complete(template);
            }
        } else {
            progress = 0;
            speedAccum = 0;
        }
        data.set(DATA_PROGRESS, template != null ? progress * 100 / template.ticks() : 0);
        setChanged();
    }

    /**
     * 制作条件：选中模板 + 样本纯度 ≥25 + 固态/能量足够 + 输出可收纳。
     * 输出须与本次产出同栈（同模板同纯度同来源，NBT 一致方可堆叠）且未满，
     * 否则停机——杜绝"不可叠加却持续消耗材料"的空转/覆盖。
     */
    private boolean canProcess(PotionTemplate template) {
        if (template == null) {
            return false;
        }
        ItemStack sample = inventory.getItem(SAMPLE_SLOT);
        if (!(sample.getItem() instanceof AkaishiLifeSampleItem)
                || AkaishiLifeSampleItem.getPurity(sample) < MIN_PURITY) {
            return false;
        }
        ItemStack solid = inventory.getItem(SOLID_SLOT);
        if (!solid.is(ModItems.akaishiLifeEssenceSolid.get())
                || solid.getCount() < template.solidCost()
                || life.getEnergyStored() < template.lifeCost()) {
            return false;
        }
        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        if (!output.is(ModItems.akaishiPotion.get())
                || output.getCount() >= output.getMaxStackSize()) {
            return false;
        }
        return ItemStack.isSameItemSameTags(output,
                AkaishiPotionItem.create(new ItemStack(output.getItem()),
                        template.id(), AkaishiLifeSampleItem.getPurity(sample),
                        AkaishiLifeSampleItem.getEntityId(sample)));
    }

    private void complete(PotionTemplate template) {
        ItemStack sample = inventory.getItem(SAMPLE_SLOT);
        int purity = AkaishiLifeSampleItem.getPurity(sample);
        // 生物来源随样本写入药剂 NBT（生物药剂差异化依据）
        String entityId = AkaishiLifeSampleItem.getEntityId(sample);
        // 每次产出构建独立新瓶：空槽放入，可堆叠（同 NBT 且未满）时 +1；
        // 异源/已满时中止本单制作，绝不覆盖既有产出或空耗材料（canProcess 已先行拦截，此处兜底）
        ItemStack result = AkaishiPotionItem.create(
                new ItemStack(ModItems.akaishiPotion.get()), template.id(), purity, entityId);
        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        if (output.isEmpty()) {
            inventory.setItem(OUTPUT_SLOT, result);
        } else if (ItemStack.isSameItemSameTags(output, result)
                && output.getCount() < output.getMaxStackSize()) {
            output.grow(1);
        } else {
            // 兜底：中止本单制作并归零进度，避免进度卡满反复进入 complete
            progress = 0;
            speedAccum = 0;
            setChanged();
            return;
        }
        // 扣消耗：1 样本 + 模板固态物 + 模板生命能量
        sample.shrink(1);
        inventory.getItem(SOLID_SLOT).shrink(template.solidCost());
        life.extractEnergy(template.lifeCost(), false);
        progress = 0;
        setChanged();
    }

    /** 服务端选择模板（C2S 包入口）；id 无效则忽略 */
    public void selectTemplate(Player player, int templateIndex) {
        if (level == null || level.isClientSide) {
            return;
        }
        List<PotionTemplate> all = PotionRegistry.all();
        if (templateIndex < 0 || templateIndex >= all.size()) {
            return;
        }
        selectedTemplate = all.get(templateIndex).id();
        progress = 0;
        speedAccum = 0;
        setChanged();
    }

    private int templateIndex() {
        List<PotionTemplate> all = PotionRegistry.all();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id().equals(selectedTemplate)) {
                return i;
            }
        }
        return -1;
    }

    // ===== 界面 =====

    public Container inventory() {
        return inventory;
    }

    public ContainerData data() {
        return data;
    }

    // ===== Container：漏斗 / 物品管道直接读写 =====

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

    // ===== IItemPipeDevice：样本/固态输入，药剂输出 =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{SAMPLE_SLOT, SOLID_SLOT};
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

    // ===== IEnergyProvider：仅生命能量输入 =====

    @Override
    public IEnergyStorage getEnergyStorage() {
        return life;
    }

    @Override
    public IEnergyStorage getEnergyStorage(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE ? life : null;
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
        return type == LifeEnergyType.INSTANCE;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_potion_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiPotionTableMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("LifeEnergy", life.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.putString("SelectedTemplate", selectedTemplate);
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        life.setEnergy(tag.getLong("LifeEnergy"));
        progress = tag.getInt("Progress");
        selectedTemplate = tag.getString("SelectedTemplate");
        if (tag.contains("Upgrades")) {
            upgradeSlots.load(tag.getCompound("Upgrades"));
        }
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < SLOT_COUNT; i++) {
            inventory.setItem(i, items.get(i));
        }
    }

    @Override
    public MachineUpgradeSlots getUpgradeSlots() {
        return upgradeSlots;
    }
}
