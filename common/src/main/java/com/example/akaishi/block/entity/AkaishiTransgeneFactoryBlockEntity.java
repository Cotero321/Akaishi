package com.example.akaishi.block.entity;

import java.util.List;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.sequence.AkaishiGeneSequenceItem;
import com.example.akaishi.menu.AkaishiTransgeneFactoryMenu;
import com.example.akaishi.sound.MachineHum;
import com.example.akaishi.sound.ModSounds;
import com.example.akaishi.util.LongDataSlots;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 转基因工厂方块实体（仅服务端驱动逻辑）。
 * 槽位：0=基因序列、1=缠怨藤（基底）、2=催化素材（凋零玫瑰/烈焰粉）、3=生命能量固态物（基底）、4=产物。
 * 配方见 {@link #RECIPES}：基因来源命中配方且纯度达标、催化槽与该配方 catalyst 一致 → 产出对应种子。
 */
public class AkaishiTransgeneFactoryBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, Container, IItemPipeDevice, IDataCarrier, IEnergyProvider {

    /**
     * 转基因配方：基因来源生物 + 最低纯度 + 催化素材 + 产出种子。
     * 各配方均需基底材料（缠怨藤 + 生命能量固态物），催化槽放入对应 catalyst 即锁定该配方。
     * 公共静态结构，供 forge 层 JEI 读取展示。
     */
    public record TransgeneFactoryRecipe(String geneEntity, int minPurity, Item catalyst, Item output) {
    }

    /** 配方表：凋零骷髅基因 + 凋零玫瑰 → 凋零藤种子；烈焰人基因 + 烈焰粉 → 烈焰花种 */
    public static final List<TransgeneFactoryRecipe> RECIPES = List.of(
            new TransgeneFactoryRecipe("minecraft:wither_skeleton", 50, Items.WITHER_ROSE, ModItems.akaishiWitherSeed.get()),
            new TransgeneFactoryRecipe("minecraft:blaze", 50, Items.BLAZE_POWDER, ModItems.akaishiBlazeSeed.get())
    );

    public static final int SLOT_GENE = 0;
    public static final int SLOT_VINE = 1;
    /** 催化素材槽：凋零玫瑰（凋零骷髅配方）或烈焰粉（烈焰人配方） */
    public static final int SLOT_CATALYST = 2;
    public static final int SLOT_SOLID = 3;
    public static final int SLOT_OUT = 4;
    public static final int SLOT_COUNT = 5;
    /** Menu 同步数据槽：long 各占高低两槽 0/1=生命能量 2/3=能量上限 4=进度百分比 5=是否工作中 */
    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_HIGH = 1;
    public static final int DATA_MAX = 2;
    public static final int DATA_MAX_HIGH = 3;
    public static final int DATA_PROGRESS = 4;
    public static final int DATA_WORKING = 5;
    public static final int DATA_SLOTS = 6;

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final AkaishiEnergyStorage life;
    /** 运转音播放器（本机音色） */
    private final MachineHum hum = new MachineHum(ModSounds.TRANSGENE_FACTORY_HUM, 0.4F, 1.0F);
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
        this.life = new AkaishiEnergyStorage(LifeEnergyType.INSTANCE, ModConfig.transgeneFactoryLifeCapacity);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiTransgeneFactoryBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        LongDataSlots.write(data, DATA_ENERGY, DATA_ENERGY_HIGH, life.getEnergyStored());
        LongDataSlots.write(data, DATA_MAX, DATA_MAX_HIGH, ModConfig.transgeneFactoryLifeCapacity);
        data.set(DATA_PROGRESS, progress * 100 / ModConfig.transgeneFactoryProcessTicks);
        data.set(DATA_WORKING, canProcess() ? 1 : 0);
        if (canProcess()) {
            progress++;
            hum.tick(level, worldPosition);
            if (progress >= ModConfig.transgeneFactoryProcessTicks) {
                progress = 0;
                // 先在材料消耗前锁定命中配方（消耗后基因槽可能被清空，不能再据此匹配）
                TransgeneFactoryRecipe recipe = matchingRecipe();
                if (recipe == null) {
                    return;
                }
                // 扣除一次合成所需生命能量，并消耗材料（基因/缠怨藤/催化素材/固态物各 1）
                life.extractEnergy(ModConfig.transgeneFactoryLifeCost, false);
                inventory.removeItem(SLOT_GENE, 1);
                inventory.removeItem(SLOT_VINE, 1);
                inventory.removeItem(SLOT_CATALYST, 1);
                inventory.removeItem(SLOT_SOLID, 1);
                // 按命中配方产出对应种子
                ItemStack out = inventory.getItem(SLOT_OUT);
                if (out.isEmpty()) {
                    inventory.setItem(SLOT_OUT, new ItemStack(recipe.output()));
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

    /** 合成条件：基因命中某条配方（含纯度与催化槽匹配） + 基底材料在位 + 生命能量充足 + 输出可容纳对应种子 */
    private boolean canProcess() {
        TransgeneFactoryRecipe recipe = matchingRecipe();
        if (recipe == null) {
            return false;
        }
        if (!inventory.getItem(SLOT_VINE).is(Items.TWISTING_VINES)
                || !inventory.getItem(SLOT_SOLID).is(ModItems.akaishiLifeEssenceSolid.get())) {
            return false;
        }
        if (life.getEnergyStored() < ModConfig.transgeneFactoryLifeCost) {
            return false;
        }
        ItemStack out = inventory.getItem(SLOT_OUT);
        return out.isEmpty() || (out.is(recipe.output())
                && out.getCount() < out.getMaxStackSize());
    }

    /** 匹配可执行配方：基因来源命中某条 RECIPES 且纯度达标，且催化槽物品为该条 catalyst */
    private TransgeneFactoryRecipe matchingRecipe() {
        ItemStack gene = inventory.getItem(SLOT_GENE);
        if (gene.isEmpty() || !gene.is(ModItems.geneSequence.get())) {
            return null;
        }
        int purity = AkaishiGeneSequenceItem.getPurity(gene);
        String entity = AkaishiGeneSequenceItem.getEntityId(gene);
        if (entity == null) {
            return null;
        }
        ItemStack catalyst = inventory.getItem(SLOT_CATALYST);
        for (TransgeneFactoryRecipe recipe : RECIPES) {
            if (recipe.geneEntity().equals(entity) && purity >= recipe.minPurity()
                    && !catalyst.isEmpty() && catalyst.is(recipe.catalyst())) {
                return recipe;
            }
        }
        return null;
    }

    /** 校验基因能否被工厂受理：命中任意配方的基因来源且纯度达该配方下限（供 GUI/管道拒绝无效基因） */
    public static boolean isValidGene(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ModItems.geneSequence.get())) {
            return false;
        }
        String entity = AkaishiGeneSequenceItem.getEntityId(stack);
        if (entity == null) {
            return false;
        }
        int purity = AkaishiGeneSequenceItem.getPurity(stack);
        for (TransgeneFactoryRecipe recipe : RECIPES) {
            if (recipe.geneEntity().equals(entity) && purity >= recipe.minPurity()) {
                return true;
            }
        }
        return false;
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
        // 产物槽仅输出：机器自动放入产物，禁止漏斗/第三方反注（方向语义见 getPipeOutputSlots）
        if (index == SLOT_OUT) {
            return false;
        }
        return switch (index) {
            case SLOT_GENE -> isValidGene(stack);
            case SLOT_VINE -> stack.is(Items.TWISTING_VINES);
            case SLOT_CATALYST -> stack.is(Items.WITHER_ROSE) || stack.is(Items.BLAZE_POWDER);
            case SLOT_SOLID -> stack.is(ModItems.akaishiLifeEssenceSolid.get());
            default -> false;
        };
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    // ===== IItemPipeDevice：0~3 为输入（基因/藤/催化素材/固态精华），4=产物可被第三方物流抽取 =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{SLOT_GENE, SLOT_VINE, SLOT_CATALYST, SLOT_SOLID};
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
        return ModConfig.transgeneFactoryLifeCapacity;
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
