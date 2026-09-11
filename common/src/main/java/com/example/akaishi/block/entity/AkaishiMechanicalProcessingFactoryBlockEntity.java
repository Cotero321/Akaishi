package com.example.akaishi.block.entity;

import com.example.akaishi.item.AkaishiMechanicalItems;
import com.example.akaishi.item.MechanicalPartItem;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.mechanical.MechanicalDnaProfile;
import com.example.akaishi.life.mechanical.MechanicalMachineCosts;
import com.example.akaishi.life.mechanical.MechanicalMaterial;
import com.example.akaishi.life.mechanical.MechanicalOrganType;
import com.example.akaishi.life.mechanical.MechanicalPartType;
import com.example.akaishi.menu.AkaishiMechanicalProcessingFactoryMenu;
import com.example.akaishi.sound.ModSounds;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 加工制作厂：部位模板 + 材料（按部件份数）+ 固态物 + 双能源 → 加工件（注入材料）。
 * 费用随模板的器官×部件从配置表读取（器官基价 × 部件系数），材料消耗份数随部件固定、与材料等级无关。
 * 槽位：0=部位模板输入、1=材料输入、2=固态物输入、3=输出。
 */
public class AkaishiMechanicalProcessingFactoryBlockEntity extends AbstractMechanicalMachineBlockEntity {

    public static final int SLOT_TEMPLATE = 0;
    public static final int SLOT_MATERIAL = 1;
    public static final int SLOT_SOLID = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int SLOT_COUNT = 4;

    public AkaishiMechanicalProcessingFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_MECHANICAL_PROCESSING_FACTORY.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{SLOT_TEMPLATE, SLOT_MATERIAL, SLOT_SOLID};
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{SLOT_OUTPUT};
    }

    @Override
    protected int extraDataSlots() {
        return 0;
    }

    @Override
    protected void populateExtraData(SimpleContainerData data) {
    }

    @Override
    protected Component getMachineName() {
        return Component.translatable("block.akaishi.akaishi_mechanical_processing_factory");
    }

    @Override
    protected RegistrySupplier<SoundEvent> humSound() {
        return ModSounds.MECHANICAL_PROCESSING_HUM;
    }

    @Override
    protected AbstractContainerMenu createMenuServer(int id, Inventory inv) {
        return new AkaishiMechanicalProcessingFactoryMenu(id, inv, this);
    }

    /** 模板器官（无模板返回 null） */
    public MechanicalOrganType templateOrgan() {
        return MechanicalPartItem.getOrganType(getItem(SLOT_TEMPLATE));
    }

    /** 模板部件（无模板返回 null） */
    public MechanicalPartType templatePart() {
        return MechanicalPartItem.getPartType(getItem(SLOT_TEMPLATE));
    }

    @Override
    protected long chishiCostTotal() {
        MechanicalOrganType organ = templateOrgan();
        MechanicalPartType part = templatePart();
        return organ != null && part != null
                ? MechanicalMachineCosts.processChishiCost(organ, part)
                : 1L;
    }

    @Override
    protected long lifeCostTotal() {
        MechanicalOrganType organ = templateOrgan();
        MechanicalPartType part = templatePart();
        return organ != null && part != null
                ? MechanicalMachineCosts.processLifeCost(organ, part)
                : 1L;
    }

    @Override
    protected int processTicksTotal() {
        MechanicalOrganType organ = templateOrgan();
        MechanicalPartType part = templatePart();
        return organ != null && part != null ? MechanicalMachineCosts.processTicks(organ, part) : 1;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return switch (index) {
            case SLOT_TEMPLATE -> stack.is(AkaishiMechanicalItems.mechanicalPartTemplate.get())
                    && !MechanicalPartItem.isProcessed(stack);
            case SLOT_MATERIAL -> AkaishiMechanicalItems.materialIdOf(stack.getItem()) != null;
            case SLOT_SOLID -> stack.is(ModItems.akaishiLifeEssenceSolid.get());
            default -> false;
        };
    }

    @Override
    protected boolean canProcess() {
        ItemStack template = getItem(SLOT_TEMPLATE);
        // 物流能力可绕过菜单槽位限制，因此底层必须校验物品本体，不能只信任 NBT。
        if (!template.is(AkaishiMechanicalItems.mechanicalPartTemplate.get())) return false;
        MechanicalOrganType organ = MechanicalPartItem.getOrganType(template);
        MechanicalPartType part = MechanicalPartItem.getPartType(template);
        if (organ == null || part == null) return false;
        // 模板必须未加工（processed=false）
        if (MechanicalPartItem.isProcessed(template)) return false;

        ItemStack material = getItem(SLOT_MATERIAL);
        if (material.isEmpty() || AkaishiMechanicalItems.materialIdOf(material.getItem()) == null) return false;
        // 材料份数按部件固定（核心3/模块2/外壳4/散热2），与材料等级无关
        if (material.getCount() < MechanicalMachineCosts.materialCount(part)) return false;

        ItemStack solid = getItem(SLOT_SOLID);
        if (solid.isEmpty() || !solid.is(ModItems.akaishiLifeEssenceSolid.get())) return false;

        ItemStack out = getItem(SLOT_OUTPUT);
        return out.isEmpty() || (out.is(AkaishiMechanicalItems.mechanicalProcessedPart.get())
                && out.getCount() < out.getMaxStackSize());
    }

    @Override
    protected void onProgressCompleted() {
        // tick 内从上一帧的条件进入完成分支前，输入可能被物流修改；再次校验避免无模板产出。
        if (!canProcess()) {
            return;
        }
        ItemStack template = getItem(SLOT_TEMPLATE);
        MechanicalOrganType organ = MechanicalPartItem.getOrganType(template);
        MechanicalPartType part = MechanicalPartItem.getPartType(template);
        String dnaId = MechanicalPartItem.getDnaProfileId(template);
        String materialId = AkaishiMechanicalItems.materialIdOf(getItem(SLOT_MATERIAL).getItem());
        int count = MechanicalMachineCosts.materialCount(part);

        // 统一经 Container 移除：确保空栈写回槽位并通知菜单同步，避免直接 shrink 留下空对象。
        removeItem(SLOT_MATERIAL, count);
        removeItem(SLOT_SOLID, 1);
        removeItem(SLOT_TEMPLATE, 1);

        // 未知材料/脏 NBT 回退铁材料；未知 DNA 回退无调校，避免 createProcessed 内 id() 空指针
        MechanicalMaterial material = MechanicalMaterial.get(materialId);
        if (material == null) {
            material = MechanicalMaterial.get("akaishi:iron");
        }
        MechanicalDnaProfile dna = dnaId != null ? MechanicalDnaProfile.get(dnaId) : null;
        if (dna == null) {
            dna = MechanicalDnaProfile.get(MechanicalDnaProfile.NONE_ID);
        }
        ItemStack result = MechanicalPartItem.createProcessed(organ, part, material, dna);
        ItemStack out = getItem(SLOT_OUTPUT);
        if (out.isEmpty()) {
            setItem(SLOT_OUTPUT, result);
        } else {
            out.grow(1);
        }
    }
}