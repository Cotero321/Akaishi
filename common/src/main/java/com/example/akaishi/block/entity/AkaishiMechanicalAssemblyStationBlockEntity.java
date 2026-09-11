package com.example.akaishi.block.entity;

import com.example.akaishi.item.AkaishiMechanicalItems;
import com.example.akaishi.item.MechanicalOrganItem;
import com.example.akaishi.item.MechanicalPartItem;
import com.example.akaishi.life.mechanical.MechanicalAssembledStats;
import com.example.akaishi.life.mechanical.MechanicalDnaProfile;
import com.example.akaishi.life.mechanical.MechanicalMachineCosts;
import com.example.akaishi.life.mechanical.MechanicalMaterial;
import com.example.akaishi.life.mechanical.MechanicalOrganResolver;
import com.example.akaishi.life.mechanical.MechanicalOrganType;
import com.example.akaishi.life.mechanical.MechanicalPartTemplate;
import com.example.akaishi.life.mechanical.MechanicalPartType;
import com.example.akaishi.menu.AkaishiMechanicalAssemblyStationMenu;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 组装加工台：四个加工件（核心/模块/外壳/散热，须同器官）→ 成品机械器官。
 * 固定双能源费用（与器官/部件无关），组装时按部件顺序聚合权重 + 协同加成。
 * 槽位：0=核心、1=模块、2=外壳、3=散热、4=输出。
 */
public class AkaishiMechanicalAssemblyStationBlockEntity extends AbstractMechanicalMachineBlockEntity {

    public static final int SLOT_CORE = 0;
    public static final int SLOT_MODULE = 1;
    public static final int SLOT_SHELL = 2;
    public static final int SLOT_COOLING = 3;
    public static final int SLOT_OUTPUT = 4;
    public static final int SLOT_COUNT = 5;

    public AkaishiMechanicalAssemblyStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_MECHANICAL_ASSEMBLY_STATION.get(), pos, state, SLOT_COUNT);
    }

    /** 组装台输入槽全部是加工件（管道可插入） */
    @Override
    public int[] getPipeInputSlots() {
        return new int[]{SLOT_CORE, SLOT_MODULE, SLOT_SHELL, SLOT_COOLING};
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
        return Component.translatable("block.akaishi.akaishi_mechanical_assembly_station");
    }

    @Override
    protected RegistrySupplier<SoundEvent> humSound() {
        return ModSounds.MECHANICAL_ASSEMBLY_HUM;
    }

    @Override
    protected AbstractContainerMenu createMenuServer(int id, Inventory inv) {
        return new AkaishiMechanicalAssemblyStationMenu(id, inv, this);
    }

    @Override
    protected long chishiCostTotal() {
        return MechanicalMachineCosts.assemblyChishiCost();
    }

    @Override
    protected long lifeCostTotal() {
        return MechanicalMachineCosts.assemblyLifeCost();
    }

    @Override
    protected int processTicksTotal() {
        return MechanicalMachineCosts.assemblyTicks();
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (index < SLOT_CORE || index > SLOT_COOLING) {
            return false;
        }
        return stack.is(AkaishiMechanicalItems.mechanicalProcessedPart.get())
                && MechanicalPartItem.isProcessed(stack)
                && MechanicalPartItem.getPartType(stack) == partForSlot(index);
    }

    /** 槽位对应部件类型 */
    private static MechanicalPartType partForSlot(int slot) {
        return switch (slot) {
            case SLOT_CORE -> MechanicalPartType.CORE;
            case SLOT_MODULE -> MechanicalPartType.MODULE;
            case SLOT_SHELL -> MechanicalPartType.SHELL;
            default -> MechanicalPartType.COOLING;
        };
    }

    @Override
    protected boolean canProcess() {
        ItemStack out = getItem(SLOT_OUTPUT);
        if (!out.isEmpty()) return false;

        MechanicalOrganType common = null;
        for (int slot = SLOT_CORE; slot <= SLOT_COOLING; slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) return false;
            if (!MechanicalPartItem.isProcessed(stack)) return false;
            if (MechanicalPartItem.getPartType(stack) != partForSlot(slot)) return false;
            MechanicalOrganType organ = MechanicalPartItem.getOrganType(stack);
            if (organ == null) return false;
            if (common == null) {
                common = organ;
            } else if (common != organ) {
                return false; // 四种部件须同器官
            }
        }
        return true;
    }

    @Override
    protected void onProgressCompleted() {
        // 从四个加工件 NBT 重建部件模板（保留材料/DNA，权重自动重算）
        List<MechanicalPartTemplate> templates = new ArrayList<>(4);
        List<String> materialIds = new ArrayList<>(4);
        for (int slot = SLOT_CORE; slot <= SLOT_COOLING; slot++) {
            ItemStack stack = getItem(slot);
            MechanicalOrganType organ = MechanicalPartItem.getOrganType(stack);
            MechanicalPartType part = MechanicalPartItem.getPartType(stack);
            String materialId = MechanicalPartItem.getMaterialId(stack);
            String dnaId = MechanicalPartItem.getDnaProfileId(stack);
            // 未知材料/脏 NBT 回退铁材料，避免模板构造 material.distribution() 空指针
            MechanicalMaterial material = MechanicalMaterial.get(materialId != null ? materialId : "akaishi:iron");
            if (material == null) {
                material = MechanicalMaterial.get("akaishi:iron");
            }
            MechanicalDnaProfile dna = dnaId != null ? MechanicalDnaProfile.get(dnaId) : null;
            if (dna == null) {
                dna = MechanicalDnaProfile.get(MechanicalDnaProfile.NONE_ID);
            }
            templates.add(new MechanicalPartTemplate(organ, part, material, dna));
            materialIds.add(material != null ? material.id() : "akaishi:iron");
        }

        // DNA 同源标记：四部件来源完全一致且非空源才写入成品（运行时同源协同 / 整合加速据此判定）
        String dnaId = null;
        for (MechanicalPartTemplate t : templates) {
            String id = t.dnaProfile().id();
            if (MechanicalDnaProfile.NONE_ID.equals(id) || (dnaId != null && !dnaId.equals(id))) {
                dnaId = null;
                break;
            }
            dnaId = id;
        }

        MechanicalOrganType organ = templates.get(0).organType();
        MechanicalAssembledStats stats = MechanicalOrganResolver.resolve(organ, templates);
        ItemStack result = MechanicalOrganItem.create(stats, materialIds);
        MechanicalOrganItem.setDnaProfileId(result, dnaId);
        setItem(SLOT_OUTPUT, result);
        for (int slot = SLOT_CORE; slot <= SLOT_COOLING; slot++) {
            setItem(slot, ItemStack.EMPTY);
        }
    }
}