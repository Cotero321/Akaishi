package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.sequence.AkaishiGeneSequenceItem;
import com.example.akaishi.menu.AkaishiTransgeneFactoryMenu;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 转基因工厂方块实体（仅服务端驱动逻辑）。
 * 槽位：0=基因序列（须为凋零骷髅来源且纯度≥50）、1=缠怨藤、2=凋零玫瑰、3=生命能量固态物、4=产物。
 * 配方（转基因判定）：凋零骷髅基因 → 凋零藤。判定条件抽成配方记录，便于后续扩充其它转基因。
 */
public class AkaishiTransgeneFactoryBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, Container, IItemPipeDevice, IDataCarrier, IEnergyProvider {

    /** 配方记录：基因来源生物 + 最低纯度（产物固化在产出步骤，扩展时按基因新增产出分支） */
    private record GeneRecipe(String geneEntity, int minPurity) {
    }

    /** 凋零藤配方：凋零骷髅基因纯度≥50 + 缠怨藤 + 凋零玫瑰 + 固态物 → 凋零藤 */
    private static final GeneRecipe WITHER_VINE_RECIPE = new GeneRecipe("minecraft:wither_skeleton", 50);

    /** 合成耗时（tick，5 秒） */
    public static final int PROGRESS_TICKS = 100;
    /** 每次合成消耗的生命能量（5k） */
    public static final long LIFE_COST = 5_000;
    /** 内部生命能量上限（可连续加工 2 次） */
    public static final long LIFE_CAPACITY = 10_000;

    public static final int SLOT_GENE = 0;
    public static final int SLOT_VINE = 1;
    public static final int SLOT_ROSE = 2;
    public static final int SLOT_SOLID = 3;
    public static final int SLOT_OUT = 4;
    public static final int SLOT_COUNT = 5;
    /** Menu 同步数据槽：0=能量、1=能量上限、2=进度百分比、3=是否工作中 */
    public static final int DATA_SLOTS = 4;
    public static final int DATA_ENERGY = 0;
    public static final int DATA_MAX = 1;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_WORKING = 3;

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final AkaishiEnergyStorage life;
    /** 当前合成进度（tick） */
    private int progress;

    public AkaishiTransgeneFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_TRANSGENE_FACTORY.get(), pos, state);
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiTransgeneFactoryBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
        this.life = new AkaishiEnergyStorage(LifeEnergyType.INSTANCE, LIFE_CAPACITY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiTransgeneFactoryBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        data.set(DATA_ENERGY, (int) Math.min(Integer.MAX_VALUE, life.getEnergyStored()));
        data.set(DATA_MAX, (int) Math.min(Integer.MAX_VALUE, LIFE_CAPACITY));
        data.set(DATA_PROGRESS, progress * 100 / PROGRESS_TICKS);
        data.set(DATA_WORKING, canProcess() ? 1 : 0);
        if (canProcess()) {
            progress++;
            if (progress >= PROGRESS_TICKS) {
                progress = 0;
                // 扣除一次合成所需生命能量（5k），并消耗材料
                life.extractEnergy(LIFE_COST, false);
                inventory.removeItem(SLOT_GENE, 1);
                inventory.removeItem(SLOT_VINE, 1);
                inventory.removeItem(SLOT_ROSE, 1);
                inventory.removeItem(SLOT_SOLID, 1);
                ItemStack out = inventory.getItem(SLOT_OUT);
                if (out.isEmpty()) {
                    inventory.setItem(SLOT_OUT, new ItemStack(ModItems.akaishiWitherSeed.get()));
                } else {
                    out.grow(1);
                }
            }
            setChanged();
        } else {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
        }
    }

    /** 合成条件：基因匹配当前配方 + 三种材料在位 + 生命能量充足 + 输出可容纳 */
    private boolean canProcess() {
        GeneRecipe recipe = matchingRecipe();
        if (recipe == null) {
            return false;
        }
        if (!inventory.getItem(SLOT_VINE).is(Items.TWISTING_VINES)
                || !inventory.getItem(SLOT_ROSE).is(Items.WITHER_ROSE)
                || !inventory.getItem(SLOT_SOLID).is(ModItems.akaishiLifeEssenceSolid.get())) {
            return false;
        }
        if (life.getEnergyStored() < LIFE_COST) {
            return false;
        }
        ItemStack out = inventory.getItem(SLOT_OUT);
        return out.isEmpty() || (out.is(ModItems.akaishiWitherSeed.get())
                && out.getCount() < out.getMaxStackSize());
    }

    /** 匹配可执行配方的凋零系基因（凋零骷髅 + 纯度达标） */
    private GeneRecipe matchingRecipe() {
        ItemStack gene = inventory.getItem(SLOT_GENE);
        if (gene.isEmpty() || !gene.is(ModItems.geneSequence.get())) {
            return null;
        }
        int purity = AkaishiGeneSequenceItem.getPurity(gene);
        String entity = AkaishiGeneSequenceItem.getEntityId(gene);
        if (entity == null || purity < WITHER_VINE_RECIPE.minPurity()
                || !entity.equals(WITHER_VINE_RECIPE.geneEntity())) {
            return null;
        }
        return WITHER_VINE_RECIPE;
    }

    /** 校验基因是否"凋零骷髅且纯度≥50"（供 GUI/管道拒绝无效基因） */
    public static boolean isWitherSkeletonGene(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.geneSequence.get())
                && "minecraft:wither_skeleton".equals(AkaishiGeneSequenceItem.getEntityId(stack))
                && AkaishiGeneSequenceItem.getPurity(stack) >= 50;
    }

    public Container inventory() {
        return inventory;
    }

    public ContainerData data() {
        return data;
    }

    // ===== Container：使漏斗 / 物品管道可直接读写槽位（放料规则同 GUI 槽） =====

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
    public void setChanged() {
        super.setChanged();
        inventory.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (index == SLOT_OUT) {
            return false;
        }
        return switch (index) {
            case SLOT_GENE -> isWitherSkeletonGene(stack);
            case SLOT_VINE -> stack.is(Items.TWISTING_VINES);
            case SLOT_ROSE -> stack.is(Items.WITHER_ROSE);
            case SLOT_SOLID -> stack.is(ModItems.akaishiLifeEssenceSolid.get());
            default -> false;
        };
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    // ===== IItemPipeDevice：0~3 为输入（基因/藤/玫瑰/固态精华），4=产物可被第三方物流抽取 =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{SLOT_GENE, SLOT_VINE, SLOT_ROSE, SLOT_SOLID};
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{SLOT_OUT};
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_transgene_factory");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiTransgeneFactoryMenu(id, inv, inventory, data);
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
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < SLOT_COUNT; i++) {
            inventory.setItem(i, items.get(i));
        }
    }

    // ===== 生命能量：只进不出（由生命能量管道/能量单元注入） =====

    public long getLifeEnergy() {
        return life.getEnergyStored();
    }

    public long getLifeMax() {
        return LIFE_CAPACITY;
    }

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
}
