package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.block.AkaishiFusionControllerBlock;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.fusion.FusionStructure;
import com.example.akaishi.item.AkaishiPlasmaRodItem;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiFusionControllerMenu;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聚变控制器方块实体：聚变堆多方块结构主方块与状态载体。
 * <p>
 * 每 tick 委托 {@link FusionStructure} 扫描（边长 7 洋葱式结构），成型后驱动燃烧结算：
 * 燃料棒消耗（能量 NBT）→ 赤能源产出（分发能量输出口）→ 生命灰烬积累（物品输出口推出）→ 温度结算。
 * <p>
 * 温度模型（渐进式）：目标温度 = 基础 50M + Σ热值×效率系数 − 散热，每 tick 按 2% 向目标趋近。
 * 最佳稳定期 100~130M 产率 ×1.0，低温/高温产率下降；达到上限 160M 过热宕机（停止燃烧），
 * 温度降至上限一半（80M）自动恢复；降温期间散热片持续消耗耐久（每 100 tick 1 点）。
 */
public class AkaishiFusionControllerBlockEntity extends BlockEntity implements ExtendedMenuProvider, IDataCarrier {

    /** 燃料槽上限 = 燃料框架上限 */
    public static final int MAX_FUEL_SLOTS = FusionStructure.MAX_FUEL_FRAMES;

    // ===== 数据槽 =====
    public static final int DATA_TEMP = 0;
    public static final int DATA_FORMED = 1;
    public static final int DATA_FUEL_FRAMES = 2;
    public static final int DATA_EFFICIENCY_FRAMES = 3;
    public static final int DATA_COOLER_COUNT = 4;
    public static final int DATA_COOLING_PERCENT = 5;
    public static final int DATA_ACTIVE_SLOTS = 6;
    public static final int DATA_YIELD_LOW = 7;
    public static final int DATA_YIELD_HIGH = 8;
    public static final int DATA_OVERHEATED = 9;
    public static final int DATA_COOLER_DURABILITY = 10;
    public static final int DATA_SPEED_X100 = 11;
    public static final int DATA_ASH_AMOUNT = 12;
    public static final int DATA_SLOTS = 13;

    private final SimpleContainer fuelSlots;
    private final SimpleContainerData data = new SimpleContainerData(DATA_SLOTS);

    /** 当前温度（M） */
    private int temp;
    /** 过热宕机标记：温度 ≥ 上限置位，降至一半解除 */
    private boolean overheated;
    /** 灰烬计数（物品，输出口按个取出） */
    private long ashAmount;
    /** 灰烬累计器（double 精度累加） */
    private double ashAccumulator;
    /** 散热片耐久消耗节拍器（每 100 tick 1 点） */
    private int durabilityTick;

    private int activeSlots;
    private long yieldPerTick;
    /** 当前效率系数（1.15^效率框架数，×100 整数存数据槽） */
    private int speedPercent;

    /** 最近一次成功扫描的结构（未成型为 null） */
    private FusionStructure.Result structure;

    // ===== 结构扫描缓存 =====
    private static final int SCAN_INTERVAL = 20;
    private static final int SCAN_INTERVAL_UNFORMED = 5;
    private boolean structureDirty = true;
    private int scanCooldown;
    /** 活跃控制器注册表（维度 → 控制器位置）：供方块变更事件失效缓存 */
    private static final Map<Level, Set<BlockPos>> ACTIVE = new ConcurrentHashMap<>();

    public AkaishiFusionControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_FUSION_CONTROLLER.get(), pos, state);
        this.fuelSlots = new SimpleContainer(MAX_FUEL_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiFusionControllerBlockEntity.this.setChanged();
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiFusionControllerBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        ACTIVE.computeIfAbsent(level, k -> ConcurrentHashMap.newKeySet()).add(worldPosition);

        FusionStructure.Result scanned;
        if (structureDirty || --scanCooldown <= 0) {
            scanned = FusionStructure.scan(level, worldPosition);
            structureDirty = false;
            scanCooldown = scanned != null ? SCAN_INTERVAL : SCAN_INTERVAL_UNFORMED;
        } else {
            scanned = structure;
        }
        boolean formed = scanned != null;
        boolean wasFormed = getBlockState().getValue(AkaishiFusionControllerBlock.FORMED);
        if (wasFormed != formed) {
            level.setBlock(worldPosition, getBlockState().setValue(AkaishiFusionControllerBlock.FORMED, formed), 3);
        }
        structure = scanned;

        if (formed) {
            if (!wasFormed) {
                broadcastToParts();
            }
            if (overheated) {
                // 宕机：停止燃烧，仅结算温度（降温）与散热片消耗
                activeSlots = 0;
                yieldPerTick = 0;
                tickTemperature();
                consumeCoolerDurability();
                if (temp <= ModConfig.fusionTempResume) {
                    overheated = false;
                }
            } else {
                tickBurning();
                tickTemperature();
                if (temp >= ModConfig.fusionTempMax) {
                    overheated = true;
                }
            }
        } else {
            // 结构失效：停止燃烧并重置状态（保留燃料槽，重新成型后继续使用），温度缓慢回落到基础值
            overheated = false;
            activeSlots = 0;
            yieldPerTick = 0;
            speedPercent = 0;
            tickTemperature();
        }
        updateData();
    }

    /** 方块变更事件入口：失效邻近活跃控制器的扫描缓存（参照反应堆） */
    public static void invalidateNearby(Level level, BlockPos changedPos) {
        Set<BlockPos> controllers = ACTIVE.get(level);
        if (controllers == null || controllers.isEmpty()) {
            return;
        }
        for (BlockPos c : controllers) {
            if (Math.abs(changedPos.getX() - c.getX()) <= 8
                    && Math.abs(changedPos.getY() - c.getY()) <= 8
                    && Math.abs(changedPos.getZ() - c.getZ()) <= 8) {
                if (level.getBlockEntity(c) instanceof AkaishiFusionControllerBlockEntity be) {
                    be.structureDirty = true;
                } else {
                    controllers.remove(c);
                }
            }
        }
    }

    /** 向成型结构内的部件广播控制器坐标 */
    private void broadcastToParts() {
        if (structure == null) {
            return;
        }
        for (BlockPos p : structure.energyPorts) {
            if (level.getBlockEntity(p) instanceof AkaishiFusionEnergyOutputBlockEntity e) {
                e.setControllerPos(worldPosition);
            }
        }
        for (BlockPos p : structure.itemInputPorts) {
            if (level.getBlockEntity(p) instanceof AkaishiFusionItemInputPortBlockEntity i) {
                i.setControllerPos(worldPosition);
            }
        }
        for (BlockPos p : structure.itemOutputPorts) {
            if (level.getBlockEntity(p) instanceof AkaishiFusionItemOutputPortBlockEntity o) {
                o.setControllerPos(worldPosition);
            }
        }
        for (BlockPos p : structure.coolerFrames) {
            if (level.getBlockEntity(p) instanceof AkaishiFusionCoolerFrameBlockEntity c) {
                c.setControllerPos(worldPosition);
            }
        }
    }

    // ===== 燃烧结算 =====

    private void tickBurning() {
        int fuelFrames = structure.fuelFrames;
        int effFrames = structure.efficiencyFrames;
        double speed = Math.pow(ModConfig.fusionEfficiencyGrowth, effFrames);
        speedPercent = (int) Math.round(speed * 100);

        int active = 0;
        long consumed = 0;
        long heatValue = 0;
        int coolingBonus = 0;
        for (int i = 0; i < MAX_FUEL_SLOTS && i < fuelFrames; i++) {
            ItemStack rod = fuelSlots.getItem(i);
            if (!(rod.getItem() instanceof AkaishiPlasmaRodItem p)) {
                continue;
            }
            active++;
            // 消耗 = 单棒基础产率 × 效率系数（100% 转化；宕机时不燃烧不消耗）
            long per = (long) (p.getRodType().baseYield * speed);
            long energy = AkaishiPlasmaRodItem.getEnergy(rod);
            long actual = Math.min(per, energy);
            consumed += actual;
            if (energy <= per) {
                // 棒烧尽：清空槽位（不返还空反应棒）
                fuelSlots.setItem(i, ItemStack.EMPTY);
            } else {
                AkaishiPlasmaRodItem.setEnergy(rod, energy - actual);
            }
            heatValue += p.getRodType().heatValue;
            coolingBonus += p.getRodType().coolingBonus;
        }

        activeSlots = active;
        yieldPerTick = (long) (consumed * temperatureCoefficient());
        heatValue *= speed; // 产热随效率系数同步放大
        // 温度目标：基础 + 产热 − 散热（散热 = 片效率 × 框架乘数 × 每%抵消 + 末地棒散热加成）
        double cooling = (structure.coolingPercent * (1 + ModConfig.fusionCoolerFrameBonus * structure.coolerFrames.size())
                + coolingBonus) * ModConfig.fusionCoolingPerPercent;
        tempTarget = ModConfig.fusionBaseTemp + heatValue - cooling;

        distributeEnergy(yieldPerTick);
        accumulateAsh(consumed);
        consumeCoolerDurability();
    }

    /** 温度系数：最佳稳定期 100~130M 产率 ×1.0；低温/高温线性下降（150M 时约 ×0.5） */
    private double temperatureCoefficient() {
        if (temp <= ModConfig.fusionTempOptMin) {
            return 0.5 + 0.5 * temp / (double) Math.max(1, ModConfig.fusionTempOptMin);
        }
        if (temp <= ModConfig.fusionTempOptMax) {
            return 1.0;
        }
        int span = Math.max(1, ModConfig.fusionTempMax - ModConfig.fusionTempOptMax);
        return 1.0 - (temp - ModConfig.fusionTempOptMax) * 0.5 / span;
    }

    /** 温度目标（tickBurning 计算，tickTemperature 使用） */
    private double tempTarget;

    /** 温度结算（渐进式）：每 tick 向目标趋近 2%，并夹紧 ±2M 防止突变 */
    private void tickTemperature() {
        if (!formedState() || overheated) {
            // 未成型或过热宕机：产热归零，目标 = 基础 − 散热（散热片继续生效降温）
            int cooling = structure == null ? 0 : structure.coolingPercent;
            double eff = (structure == null ? 0 : structure.coolerFrames.size());
            double cool = (cooling * (1 + ModConfig.fusionCoolerFrameBonus * eff)) * ModConfig.fusionCoolingPerPercent;
            tempTarget = ModConfig.fusionBaseTemp - cool;
        }
        double target = Math.max(0, Math.min(ModConfig.fusionTempMax, tempTarget));
        double delta = (target - temp) * 0.02;
        delta = Math.max(-ModConfig.fusionTempStep, Math.min(ModConfig.fusionTempStep, delta));
        temp = (int) Math.max(0, Math.min(ModConfig.fusionTempMax, temp + delta));
    }

    private boolean formedState() {
        return getBlockState().getValue(AkaishiFusionControllerBlock.FORMED);
    }

    /** 散热片耐久消耗：运行或宕机期间每 100 tick 全体散热片 -1 耐久 */
    private void consumeCoolerDurability() {
        if (++durabilityTick < ModConfig.fusionCoolerDurabilityInterval || structure == null) {
            return;
        }
        durabilityTick = 0;
        for (BlockPos p : structure.coolerFrames) {
            if (level.getBlockEntity(p) instanceof AkaishiFusionCoolerFrameBlockEntity c) {
                c.consumeDurability();
            }
        }
    }

    /** 将产出的赤能源平分到全部能量输出口（输出口缓冲满则溢出丢弃） */
    private void distributeEnergy(long amount) {
        if (structure == null || amount <= 0 || structure.energyPorts.isEmpty()) {
            return;
        }
        long per = amount / structure.energyPorts.size();
        for (BlockPos p : structure.energyPorts) {
            if (level.getBlockEntity(p) instanceof AkaishiFusionEnergyOutputBlockEntity e) {
                e.receiveEnergy(per);
            }
        }
    }

    /** 生命灰烬累计：每消耗 fusionAshPerEnergy 能量产出 1 个灰烬 */
    private void accumulateAsh(long consumed) {
        if (consumed <= 0) {
            return;
        }
        ashAccumulator += consumed;
        long per = Math.max(1L, ModConfig.fusionAshPerEnergy);
        while (ashAccumulator >= per) {
            ashAccumulator -= per;
            ashAmount++;
            setChanged();
        }
    }

    /** 物品输出口调用：取 1 个灰烬（无则返回空） */
    public ItemStack takeAsh() {
        if (ashAmount <= 0) {
            return ItemStack.EMPTY;
        }
        ashAmount--;
        setChanged();
        return new ItemStack(ModItems.lifeAsh.get());
    }

    // ===== 数据同步 =====

    private void updateData() {
        data.set(DATA_TEMP, temp);
        data.set(DATA_FORMED, formedState() ? 1 : 0);
        data.set(DATA_FUEL_FRAMES, structure == null ? 0 : structure.fuelFrames);
        data.set(DATA_EFFICIENCY_FRAMES, structure == null ? 0 : structure.efficiencyFrames);
        data.set(DATA_COOLER_COUNT, structure == null ? 0 : structure.coolerFrames.size());
        data.set(DATA_COOLING_PERCENT, structure == null ? 0 : structure.coolingPercent);
        data.set(DATA_ACTIVE_SLOTS, activeSlots);
        data.set(DATA_YIELD_LOW, (int) yieldPerTick);
        data.set(DATA_YIELD_HIGH, (int) (yieldPerTick >>> 32));
        data.set(DATA_OVERHEATED, overheated ? 1 : 0);
        data.set(DATA_COOLER_DURABILITY, lowestCoolerDurability());
        data.set(DATA_SPEED_X100, speedPercent);
        data.set(DATA_ASH_AMOUNT, (int) Math.min(Integer.MAX_VALUE, ashAmount));
    }

    /** 结构内散热片最低耐久百分比（0-100）；无散热片返回 100 */
    private int lowestCoolerDurability() {
        if (structure == null || structure.coolerFrames.isEmpty()) {
            return 100;
        }
        int min = 100;
        for (BlockPos p : structure.coolerFrames) {
            int d = AkaishiFusionCoolerFrameBlockEntity.getDurabilityPercentAt(level, p);
            if (d >= 0) {
                min = Math.min(min, d);
            }
        }
        return min;
    }

    public ContainerData data() {
        return data;
    }

    public SimpleContainer fuelSlots() {
        return fuelSlots;
    }

    public int getFuelSlotCount() {
        return structure == null ? 0 : structure.fuelFrames;
    }

    public FusionStructure.Result getStructure() {
        return structure;
    }

    public boolean isFormed() {
        return formedState();
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_fusion_controller");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiFusionControllerMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    // ===== NBT =====

    @Override
    public String[] excludedKeys() {
        return new String[]{"Items", "ControllerPos"};
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Temp", temp);
        tag.putBoolean("Overheated", overheated);
        tag.putLong("Ash", ashAmount);
        tag.putDouble("AshAccum", ashAccumulator);
        NonNullList<ItemStack> items = NonNullList.withSize(MAX_FUEL_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < MAX_FUEL_SLOTS; i++) {
            items.set(i, fuelSlots.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        temp = tag.getInt("Temp");
        overheated = tag.getBoolean("Overheated");
        ashAmount = tag.getLong("Ash");
        ashAccumulator = tag.getDouble("AshAccum");
        NonNullList<ItemStack> items = NonNullList.withSize(MAX_FUEL_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < MAX_FUEL_SLOTS; i++) {
            fuelSlots.setItem(i, items.get(i));
        }
    }
}
