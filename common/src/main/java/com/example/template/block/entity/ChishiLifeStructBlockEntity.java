package com.example.template.block.entity;

import com.example.template.api.IDataCarrier;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.api.energy.IEnergyType;
import com.example.template.api.item.IItemPipeDevice;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.LifeEnergyType;
import com.example.template.item.ModItems;
import com.example.template.life.body.BodySlot;
import com.example.template.life.organ.ChishiOrganItem;
import com.example.template.life.organ.OrganEffectRegistry;
import com.example.template.life.sample.SampleGroup;
import com.example.template.life.sequence.ChishiGeneSequenceItem;
import com.example.template.menu.ChishiLifeStructMenu;
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

import java.util.List;

/**
 * 生命结构台方块实体（仅服务端驱动逻辑）。
 * 将基因序列解析为完整器官：
 * - 目标槽位由界面选择（C2S 包写入），仅限该生物已注册的可用槽位
 * - 器官完整度 = 基因序列纯度 − 随机损耗(0-20)，决定品质档位（100=III、50-99=II、<50=I）
 * - 器官适配度按来源分组区间随机偏低分配（与采集逻辑一致）
 * - 无失败率：序列 + 5 固态 + 80K 生命能量齐备即成功
 * 槽位：0=基因序列，1=生命固态物，2=输出（器官）。
 */
public class ChishiLifeStructBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IItemPipeDevice, IDataCarrier {

    /** 单次构造消耗的生命能量 */
    public static final long LIFE_COST = 80_000L;
    /** 单次构造消耗的固态物数量 */
    public static final int SOLID_COST = 5;
    /** 生命能量缓冲容量（够 2 次构造） */
    public static final long LIFE_CAPACITY = 160_000L;
    /** 构造耗时（tick） */
    public static final int PROGRESS_TICKS = 120;
    /** 完整度随机损耗上限（0-20） */
    public static final int PURITY_LOSS_MAX = 20;

    public static final int INPUT_SLOT = 0;
    public static final int SOLID_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SLOT_COUNT = 3;
    /** Menu 同步数据槽：0/1=生命能量/容量 2=进度% 3=目标槽位索引 */
    public static final int DATA_SLOTS = 4;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_TARGET = 3;

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final ChishiEnergyStorage life;
    private int progress;
    /** 目标槽位（BodySlot.values() 索引） */
    private int targetSlot;

    public ChishiLifeStructBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_STRUCT.get(), pos, state);
        this.life = new ChishiEnergyStorage(LifeEnergyType.INSTANCE, LIFE_CAPACITY);
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                ChishiLifeStructBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiLifeStructBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        data.set(0, (int) life.getEnergyStored());
        data.set(1, (int) life.getMaxEnergy());

        boolean changed = false;
        refreshTarget();
        if (canProcess()) {
            progress++;
            if (progress >= PROGRESS_TICKS) {
                progress = 0;
                complete();
            }
            changed = true;
        } else {
            progress = 0;
        }
        data.set(2, progress * 100 / PROGRESS_TICKS);
        data.set(3, targetSlot);
        if (changed) {
            setChanged();
        }
    }

    /** 序列更换时，若目标槽位不可用则自动回落到第一个可用槽位 */
    private void refreshTarget() {
        ItemStack input = inventory.getItem(INPUT_SLOT);
        if (!(input.getItem() instanceof ChishiGeneSequenceItem)) {
            return;
        }
        List<BodySlot> available = OrganEffectRegistry.availableSlots(ChishiGeneSequenceItem.getEntityId(input));
        if (available.isEmpty()) {
            return;
        }
        if (!available.contains(BodySlot.values()[clampTarget()])) {
            targetSlot = available.get(0).ordinal();
        }
    }

    private int clampTarget() {
        return Math.max(0, Math.min(BodySlot.values().length - 1, targetSlot));
    }

    /** 构造条件：序列 + 目标槽位可用 + 固态/能量充足 + 输出可容纳 */
    private boolean canProcess() {
        ItemStack input = inventory.getItem(INPUT_SLOT);
        if (!(input.getItem() instanceof ChishiGeneSequenceItem) || ChishiGeneSequenceItem.getGroup(input) == null) {
            return false;
        }
        if (OrganEffectRegistry.availableSlots(ChishiGeneSequenceItem.getEntityId(input)).isEmpty()) {
            return false;
        }
        if (!availableContains(BodySlot.values()[clampTarget()])) {
            return false;
        }
        if (!hasSolid(SOLID_COST) || life.getEnergyStored() < LIFE_COST) {
            return false;
        }
        ItemStack out = inventory.getItem(OUTPUT_SLOT);
        if (out.isEmpty()) {
            return true;
        }
        return out.getItem() instanceof ChishiOrganItem && out.getCount() < out.getMaxStackSize();
    }

    private boolean availableContains(BodySlot target) {
        List<BodySlot> available = OrganEffectRegistry.availableSlots(
                ChishiGeneSequenceItem.getEntityId(inventory.getItem(INPUT_SLOT)));
        return available.contains(target);
    }

    /** 完成构造：消耗材料，产出器官（完整度 = 序列纯度 − 随机损耗） */
    private void complete() {
        ItemStack input = inventory.getItem(INPUT_SLOT);
        SampleGroup group = ChishiGeneSequenceItem.getGroup(input);
        String entityId = ChishiGeneSequenceItem.getEntityId(input);
        BodySlot target = BodySlot.values()[clampTarget()];
        int seqPurity = ChishiGeneSequenceItem.getPurity(input);

        life.extractEnergy(LIFE_COST, false);
        inventory.getItem(SOLID_SLOT).shrink(SOLID_COST);
        input.shrink(1);

        // 完整度 = 序列纯度 − 随机损耗 0-20（越低完整度越差，品质档位越低）
        int purity = Math.max(0, seqPurity - level.random.nextInt(PURITY_LOSS_MAX + 1));
        // 纯度→适配度偏置：完整度越高，适配度越接近分组区间上限（OrganLinkage.compatRoll）
        ItemStack organ = ChishiOrganItem.create(target, group, entityId, purity);
        ChishiOrganItem.setPurity(organ, purity);
        ChishiOrganItem.setTier(organ, ChishiGeneSequenceItem.tierOf(purity));

        ItemStack out = inventory.getItem(OUTPUT_SLOT);
        if (out.isEmpty()) {
            inventory.setItem(OUTPUT_SLOT, organ);
        } else {
            out.grow(1);
        }
    }

    private boolean hasSolid(int count) {
        ItemStack solid = inventory.getItem(SOLID_SLOT);
        return solid.is(ModItems.chishiLifeEssenceSolid.get()) && solid.getCount() >= count;
    }

    /** 界面选择目标槽位（服务端校验范围） */
    public void setTargetSlot(int index) {
        targetSlot = Math.max(0, Math.min(BodySlot.values().length - 1, index));
        data.set(DATA_TARGET, targetSlot);
        setChanged();
    }

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

    // ===== IItemPipeDevice：序列/固态入，器官出 =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{INPUT_SLOT, SOLID_SLOT};
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
        return Component.translatable("block.template_mod.chishi_life_struct");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ChishiLifeStructMenu(id, inv, this);
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
        tag.putInt("TargetSlot", targetSlot);
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
        targetSlot = tag.getInt("TargetSlot");
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < SLOT_COUNT; i++) {
            inventory.setItem(i, items.get(i));
        }
    }
}
