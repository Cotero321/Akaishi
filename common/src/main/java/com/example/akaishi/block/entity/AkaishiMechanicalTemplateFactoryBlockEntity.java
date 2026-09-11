package com.example.akaishi.block.entity;

import com.example.akaishi.api.life.ISampleGroup;
import com.example.akaishi.item.AkaishiMechanicalItems;
import com.example.akaishi.item.MechanicalPartItem;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.mechanical.MechanicalDnaProfile;
import com.example.akaishi.life.mechanical.MechanicalMachineCosts;
import com.example.akaishi.life.mechanical.MechanicalMaterial;
import com.example.akaishi.life.mechanical.MechanicalOrganType;
import com.example.akaishi.life.mechanical.MechanicalPartType;
import com.example.akaishi.life.mechanical.MechanicalPartWeight;
import com.example.akaishi.life.sample.AkaishiLifeSampleItem;
import com.example.akaishi.life.sequence.AkaishiGeneSequenceItem;
import com.example.akaishi.menu.AkaishiMechanicalTemplateFactoryMenu;
import com.example.akaishi.sound.ModSounds;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 模板制造厂：通用部件塑形模板 + 生命固态物 + 基因序列/生命样本 → 部位模板（organ×part）。
 * DNA 槽可放基因序列片段或生命样本，按 gene_source 分组 / entity_id 生物推导 DNA 调校；
 * 槽位留空则产出无调校模板（akaishi:none）。
 * 目标模板在 GUI 内选（organ/part 选择存 BE，data 槽 6/7 同步；C2S 包更新）。
 * 槽位：0=通用模板输入、1=生命固态物输入、2=DNA输入、3=输出。
 */
public class AkaishiMechanicalTemplateFactoryBlockEntity extends AbstractMechanicalMachineBlockEntity {

    public static final int SLOT_MOULD = 0;
    public static final int SLOT_SOLID = 1;
    public static final int SLOT_DNA = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int SLOT_COUNT = 4;
    public static final int DATA_SELECTED_ORGAN = DATA_CRAFT + 1;
    public static final int DATA_SELECTED_PART = DATA_CRAFT + 2;

    private int selectedOrgan; // MechanicalOrganType.ordinal()
    private int selectedPart;  // MechanicalPartType.ordinal()

    public AkaishiMechanicalTemplateFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_MECHANICAL_TEMPLATE_FACTORY.get(), pos, state, SLOT_COUNT);
        this.selectedOrgan = MechanicalOrganType.EYE.ordinal();
        this.selectedPart = MechanicalPartType.CORE.ordinal();
    }

    /** GUI 选择目标模板（C2S 包调用，仅服务端） */
    public void setSelection(int organOrd, int partOrd) {
        if (organOrd >= 0 && organOrd < MechanicalOrganType.values().length
                && partOrd >= 0 && partOrd < MechanicalPartType.values().length
                && (organOrd != selectedOrgan || partOrd != selectedPart)) {
            this.selectedOrgan = organOrd;
            this.selectedPart = partOrd;
            setChanged();
        }
    }

    public int getSelectedOrgan() {
        return selectedOrgan;
    }

    public int getSelectedPart() {
        return selectedPart;
    }

    public MechanicalOrganType selectedOrganType() {
        return MechanicalOrganType.values()[selectedOrgan];
    }

    public MechanicalPartType selectedPartType() {
        return MechanicalPartType.values()[selectedPart];
    }

    /** 输入槽（管道可插入）：通用模板 + 固态物 + DNA */
    @Override
    public int[] getPipeInputSlots() {
        return new int[]{SLOT_MOULD, SLOT_SOLID, SLOT_DNA};
    }

    /** 输出槽（管道可抽取）：部位模板 */
    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{SLOT_OUTPUT};
    }

    @Override
    protected int extraDataSlots() {
        return 2;
    }

    @Override
    protected void populateExtraData(SimpleContainerData data) {
        data.set(DATA_SELECTED_ORGAN, selectedOrgan);
        data.set(DATA_SELECTED_PART, selectedPart);
    }

    @Override
    protected void saveExtraNbt(CompoundTag tag) {
        tag.putInt("SelOrgan", selectedOrgan);
        tag.putInt("SelPart", selectedPart);
    }

    @Override
    protected void loadExtraNbt(CompoundTag tag) {
        selectedOrgan = tag.contains("SelOrgan") ? tag.getInt("SelOrgan") : MechanicalOrganType.EYE.ordinal();
        selectedPart = tag.contains("SelPart") ? tag.getInt("SelPart") : MechanicalPartType.CORE.ordinal();
    }

    @Override
    protected Component getMachineName() {
        return Component.translatable("block.akaishi.akaishi_mechanical_template_factory");
    }

    @Override
    protected RegistrySupplier<SoundEvent> humSound() {
        return ModSounds.MECHANICAL_TEMPLATE_FACTORY_HUM;
    }

    @Override
    protected AbstractContainerMenu createMenuServer(int id, Inventory inv) {
        return new AkaishiMechanicalTemplateFactoryMenu(id, inv, this);
    }

    @Override
    protected long chishiCostTotal() {
        return MechanicalMachineCosts.templateChishiCost();
    }

    @Override
    protected long lifeCostTotal() {
        return MechanicalMachineCosts.templateLifeCost();
    }

    @Override
    protected int processTicksTotal() {
        return MechanicalMachineCosts.templateTicks();
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return switch (index) {
            case SLOT_MOULD -> stack.is(AkaishiMechanicalItems.genericPartMould.get());
            case SLOT_SOLID -> stack.is(ModItems.akaishiLifeEssenceSolid.get());
            case SLOT_DNA -> isDnaSource(stack);
            default -> false;
        };
    }

    /** DNA 槽载体：基因序列片段或生命样本（两者均带分组与生物来源） */
    public static boolean isDnaSource(ItemStack stack) {
        return stack.is(ModItems.geneSequence.get()) || stack.is(ModItems.lifeSample.get());
    }

    @Override
    protected boolean canProcess() {
        ItemStack mould = getItem(SLOT_MOULD);
        ItemStack solid = getItem(SLOT_SOLID);
        if (mould.isEmpty() || !mould.is(AkaishiMechanicalItems.genericPartMould.get())) return false;
        if (solid.isEmpty() || !solid.is(ModItems.akaishiLifeEssenceSolid.get())) return false;
        // 物流可绕过菜单槽位限制，非空 DNA 槽必须是基因序列/生命样本，否则拒绝制作
        ItemStack dnaStack = getItem(SLOT_DNA);
        if (!dnaStack.isEmpty() && !isDnaSource(dnaStack)) return false;
        ItemStack out = getItem(SLOT_OUTPUT);
        return out.isEmpty() || (out.is(AkaishiMechanicalItems.mechanicalPartTemplate.get())
                && out.getCount() < out.getMaxStackSize());
    }

    /** 读取 DNA 槽推导调校模板；槽位为空或来源缺失时回退无调校 */
    private MechanicalDnaProfile resolveDna() {
        ItemStack dnaStack = getItem(SLOT_DNA);
        if (dnaStack.isEmpty()) {
            return MechanicalDnaProfile.resolveForSample(null, null);
        }
        String groupId = null;
        String entityId = null;
        if (dnaStack.is(ModItems.geneSequence.get())) {
            ISampleGroup group = AkaishiGeneSequenceItem.getGroup(dnaStack);
            groupId = group != null ? group.getId() : null;
            entityId = AkaishiGeneSequenceItem.getEntityId(dnaStack);
        } else if (dnaStack.is(ModItems.lifeSample.get())) {
            ISampleGroup group = AkaishiLifeSampleItem.getGroup(dnaStack);
            groupId = group != null ? group.getId() : null;
            entityId = AkaishiLifeSampleItem.getEntityId(dnaStack);
        }
        return MechanicalDnaProfile.resolveForSample(groupId, entityId);
    }

    @Override
    protected void onProgressCompleted() {
        if (!canProcess()) {
            return;
        }
        getItem(SLOT_MOULD).shrink(1);
        getItem(SLOT_SOLID).shrink(1);
        ItemStack dnaStack = getItem(SLOT_DNA);
        MechanicalDnaProfile dna = resolveDna();
        if (!dnaStack.isEmpty()) {
            dnaStack.shrink(1);
        }
        ItemStack result = MechanicalPartItem.createTemplate(
                selectedOrganType(), selectedPartType(),
                MechanicalMaterial.get("akaishi:iron"), dna);
        ItemStack out = getItem(SLOT_OUTPUT);
        if (out.isEmpty()) {
            setItem(SLOT_OUTPUT, result);
        } else {
            out.grow(1);
        }
    }
}