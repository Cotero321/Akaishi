package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.block.AkaishiFusionControllerBlock;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.fusion.FusionStructure;
import com.example.akaishi.item.AkaishiFusionHeatSinkItem;
import com.example.akaishi.item.AkaishiPlasmaRodItem;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiFusionControllerMenu;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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
    /** 过热停产剩余冷却（秒，服务端写入） */
    public static final int DATA_OVERHEAT_COOLDOWN = 13;
    public static final int DATA_SLOTS = 14;

    /** 过热停产时长：超温跳闸后强制冷却 5 分钟（20 tick/秒 × 300 秒），期间无法恢复燃烧 */
    private static final int OVERHEAT_COOLDOWN_TICKS = 20 * 60 * 5;

    /** 散热片槽上限（与结构散热框架上限一致；框架数量决定实际可用槽数） */
    public static final int MAX_COOLER_SLOTS = FusionStructure.MAX_COOLER_FRAMES;

    private final SimpleContainer fuelSlots;
    /** 散热片容器：散热片统一存放于控制器，结构内散热框架仅计数解锁槽位 */
    private final SimpleContainer coolerSlots;
    private final SimpleContainerData data = new SimpleContainerData(DATA_SLOTS);

    /** 当前温度（M）：double 存储，避免 int 截断使温度逼近上限时永久卡死无法触发跳闸 */
    private double temp;
    /** 过热宕机标记：温度 ≥ fusionTempTrip 置位，降至 fusionTempResume 解除 */
    private boolean overheated;
    /** 过热停产冷却剩余 tick（超温后强制停机 5 分钟，归零且温度回落才允许重启） */
    private int overheatCooldown;
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
    private static final Map<ResourceKey<Level>, Set<BlockPos>> ACTIVE = new ConcurrentHashMap<>();

    public AkaishiFusionControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_FUSION_CONTROLLER.get(), pos, state);
        this.fuelSlots = new SimpleContainer(MAX_FUEL_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiFusionControllerBlockEntity.this.setChanged();
            }
        };
        this.coolerSlots = new SimpleContainer(MAX_COOLER_SLOTS) {
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
        ACTIVE.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(worldPosition);

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
                // 宕机：停止燃烧，仅结算温度（降温）与散热片消耗，并推进 5 分钟停产倒计时
                activeSlots = 0;
                yieldPerTick = 0;
                if (overheatCooldown > 0) {
                    overheatCooldown--;
                }
                tickTemperature();
                consumeCoolerDurability();
                // 恢复条件：温度回落至恢复线以下，且停产冷却时长已到
                if (temp <= ModConfig.fusionTempResume && overheatCooldown <= 0) {
                    overheated = false;
                }
            } else {
                tickBurning();
                tickTemperature();
                if (temp >= ModConfig.fusionTempTrip) {
                    // 超温跳闸：强制停产 5 分钟，防止反复过载
                    overheated = true;
                    overheatCooldown = OVERHEAT_COOLDOWN_TICKS;
                }
            }
        } else {
            // 结构失效：停止燃烧并重置状态（保留燃料槽，重新成型后继续使用），温度缓慢回落到基础值
            overheated = false;
            overheatCooldown = 0;
            activeSlots = 0;
            yieldPerTick = 0;
            speedPercent = 0;
            tickTemperature();
        }
        updateData();
    }

    /** 方块变更事件入口：失效邻近活跃控制器的扫描缓存（参照反应堆） */
    public static void invalidateNearby(Level level, BlockPos changedPos) {
        Set<BlockPos> controllers = ACTIVE.get(level.dimension());
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
                    // 集合清空时回收维度条目，防止陈旧 Level/坐标持续占用
                    if (controllers.isEmpty()) {
                        ACTIVE.remove(level.dimension());
                    }
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
    }

    // ===== 燃烧结算 =====

    private void tickBurning() {
        int fuelFrames = structure.fuelFrames;
        int effFrames = structure.efficiencyFrames;
        double speed = Math.pow(ModConfig.fusionEfficiencyGrowth, effFrames);
        speedPercent = (int) Math.round(speed * 100);

        // 第一遍（只读试算）：统计本 tick 计划消耗的燃料能量与产热，不修改任何槽位
        long planned = 0;
        double heatValue = 0;
        int coolingBonus = 0;
        int active = 0;
        for (int i = 0; i < MAX_FUEL_SLOTS && i < fuelFrames; i++) {
            ItemStack rod = fuelSlots.getItem(i);
            if (!(rod.getItem() instanceof AkaishiPlasmaRodItem p)) {
                continue;
            }
            active++;
            long per = (long) (p.getRodType().baseYield * speed);
            planned += Math.min(per, AkaishiPlasmaRodItem.getEnergy(rod));
            heatValue += p.getRodType().heatValue;
            coolingBonus += p.getRodType().coolingBonus;
        }
        heatValue *= speed;
        activeSlots = active;

        // 满位门控：输出储能剩余空间装不下“整 tick 计划消耗”则不点火。
        // 产出 = 消耗 × 温度系数(≤1) ≤ 消耗，因此空间足以装下消耗就一定能装下产出，
        // 杜绝“烧 1 tick→灌满→停→腾出一点空间又点火”的振荡空耗（每次翻转都会真烧燃料）。
        if (planned <= 0) {
            yieldPerTick = 0;
            return;
        }
        if (outputSpace() < planned) {
            activeSlots = 0;
            yieldPerTick = 0;
            return;
        }

        // 第二遍（实际执行）：试算通过才真正扣除燃料，与第一遍计算完全一致
        long consumed = 0;
        for (int i = 0; i < MAX_FUEL_SLOTS && i < fuelFrames; i++) {
            ItemStack rod = fuelSlots.getItem(i);
            if (!(rod.getItem() instanceof AkaishiPlasmaRodItem p)) {
                continue;
            }
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
        }

        yieldPerTick = (long) (consumed * temperatureCoefficient());
        // 温度目标：基础 + 产热 − 散热（散热 = 控制器散热片总效率 × 框架乘数 × 每%抵消 + 末地棒散热加成）
        int frameCount = structure.coolerFrames.size();
        double cooling = (activeCoolingPercent() * (1 + ModConfig.fusionCoolerFrameBonus * frameCount)
                + coolingBonus) * ModConfig.fusionCoolingPerPercent;
        tempTarget = ModConfig.fusionBaseTemp + heatValue - cooling;

        distributeEnergy(yieldPerTick);
        accumulateAsh(consumed);
        consumeCoolerDurability();
    }

    /** 温度系数：最佳稳定期 100~130M 产率 ×1.0；低温/高温线性下降（逼近 160M 时约 ×0.5） */
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
            int frameCount = structure == null ? 0 : structure.coolerFrames.size();
            double cool = (activeCoolingPercent() * (1 + ModConfig.fusionCoolerFrameBonus * frameCount))
                    * ModConfig.fusionCoolingPerPercent;
            tempTarget = ModConfig.fusionBaseTemp - cool;
        }
        double target = Math.max(0, Math.min(ModConfig.fusionTempMax, tempTarget));
        double delta = Math.max(-ModConfig.fusionTempStep,
                Math.min(ModConfig.fusionTempStep, (target - temp) * 0.02));
        // 距目标足够近时直接吸附：2% 渐近趋近在浮点精度下会停滞在目标之前，吸附保证温度能精确抵达/越过停机阈值
        if (Math.abs(target - temp) <= 0.5) {
            temp = target;
        } else {
            temp = Math.max(0, Math.min(ModConfig.fusionTempMax, temp + delta));
        }
    }

    private boolean formedState() {
        return getBlockState().getValue(AkaishiFusionControllerBlock.FORMED);
    }

    /** 散热片耐久消耗：运行或宕机期间每 100 tick 各散热片 -1 耐久（仅结算已解锁的散热片槽） */
    private void consumeCoolerDurability() {
        if (++durabilityTick < ModConfig.fusionCoolerDurabilityInterval || structure == null) {
            return;
        }
        durabilityTick = 0;
        int count = Math.min(structure.coolerFrames.size(), MAX_COOLER_SLOTS);
        for (int i = 0; i < count; i++) {
            ItemStack sink = coolerSlots.getItem(i);
            if (sink.isEmpty() || !sink.isDamageableItem()) {
                continue;
            }
            sink.setDamageValue(sink.getDamageValue() + 1);
            if (sink.getDamageValue() >= sink.getMaxDamage()) {
                coolerSlots.setItem(i, ItemStack.EMPTY); // 耐久耗尽破碎消失
            }
            setChanged();
        }
    }

    /** 全部能量输出口的剩余可装空间合计（供满位门控：空间不足整 tick 产出则停烧） */
    private long outputSpace() {
        if (structure == null) {
            return 0;
        }
        long space = 0;
        for (BlockPos p : structure.energyPorts) {
            if (level.getBlockEntity(p) instanceof AkaishiFusionEnergyOutputBlockEntity e) {
                AkaishiEnergyStorage s = e.energy();
                space += Math.max(0, s.getMaxEnergy() - s.getEnergyStored());
            }
        }
        return space;
    }

    private void distributeEnergy(long amount) {
        if (structure == null || amount <= 0 || structure.energyPorts.isEmpty()) {
            return;
        }
        // 顺序填充分配：每个口尽力接收剩余能量，实收返回；避免均分时部分满口丢弃产出
        long remaining = amount;
        for (BlockPos p : structure.energyPorts) {
            if (remaining <= 0) {
                break;
            }
            if (level.getBlockEntity(p) instanceof AkaishiFusionEnergyOutputBlockEntity e) {
                remaining -= e.receiveEnergy(remaining);
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
        data.set(DATA_TEMP, (int) temp);
        data.set(DATA_FORMED, formedState() ? 1 : 0);
        data.set(DATA_FUEL_FRAMES, structure == null ? 0 : structure.fuelFrames);
        data.set(DATA_EFFICIENCY_FRAMES, structure == null ? 0 : structure.efficiencyFrames);
        data.set(DATA_COOLER_COUNT, structure == null ? 0 : structure.coolerFrames.size());
        data.set(DATA_COOLING_PERCENT, activeCoolingPercent());
        data.set(DATA_ACTIVE_SLOTS, activeSlots);
        data.set(DATA_YIELD_LOW, (int) yieldPerTick);
        data.set(DATA_YIELD_HIGH, (int) (yieldPerTick >>> 32));
        data.set(DATA_OVERHEATED, overheated ? 1 : 0);
        data.set(DATA_COOLER_DURABILITY, lowestCoolerDurability());
        data.set(DATA_SPEED_X100, speedPercent);
        data.set(DATA_ASH_AMOUNT, (int) Math.min(Integer.MAX_VALUE, ashAmount));
        // 过热停产剩余秒（向上取整，供 GUI 倒计时显示）
        data.set(DATA_OVERHEAT_COOLDOWN, (overheatCooldown + 19) / 20);
    }

    /** 控制器散热片总冷却（%）：仅统计结构框架数解锁的前 count 个散热片槽 */
    private int activeCoolingPercent() {
        int count = structure == null ? 0 : Math.min(structure.coolerFrames.size(), MAX_COOLER_SLOTS);
        int sum = 0;
        for (int i = 0; i < count; i++) {
            ItemStack sink = coolerSlots.getItem(i);
            if (sink.getItem() instanceof AkaishiFusionHeatSinkItem h) {
                sum += h.getQuality().coolingPercent;
            }
        }
        return sum;
    }

    /** 散热片槽最低剩余耐久百分比（0-100）；无散热片返回 100 */
    private int lowestCoolerDurability() {
        int count = structure == null ? 0 : Math.min(structure.coolerFrames.size(), MAX_COOLER_SLOTS);
        int min = 100;
        boolean any = false;
        for (int i = 0; i < count; i++) {
            ItemStack sink = coolerSlots.getItem(i);
            if (sink.isEmpty() || sink.getMaxDamage() <= 0) {
                continue;
            }
            any = true;
            int rem = (int) ((long) (sink.getMaxDamage() - sink.getDamageValue()) * 100 / sink.getMaxDamage());
            min = Math.min(min, rem);
        }
        return any ? min : 100;
    }

    public ContainerData data() {
        return data;
    }

    public SimpleContainer fuelSlots() {
        return fuelSlots;
    }

    public SimpleContainer coolerSlots() {
        return coolerSlots;
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
        return new String[]{"Items", "CoolerItems", "ControllerPos"};
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("Temp", temp);
        tag.putBoolean("Overheated", overheated);
        // 持久化过热停产倒计时，防止存档重载后强制冷却被绕过
        tag.putInt("OverheatCooldown", overheatCooldown);
        tag.putLong("Ash", ashAmount);
        tag.putDouble("AshAccum", ashAccumulator);
        NonNullList<ItemStack> items = NonNullList.withSize(MAX_FUEL_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < MAX_FUEL_SLOTS; i++) {
            items.set(i, fuelSlots.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
        // 散热片槽用独立嵌套键保存，避免与燃料槽 "Items" 冲突
        NonNullList<ItemStack> coolers = NonNullList.withSize(MAX_COOLER_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < MAX_COOLER_SLOTS; i++) {
            coolers.set(i, coolerSlots.getItem(i));
        }
        CompoundTag coolerTag = new CompoundTag();
        ContainerHelper.saveAllItems(coolerTag, coolers);
        tag.put("CoolerItems", coolerTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        temp = tag.getDouble("Temp");
        overheated = tag.getBoolean("Overheated");
        overheatCooldown = tag.getInt("OverheatCooldown");
        ashAmount = tag.getLong("Ash");
        ashAccumulator = tag.getDouble("AshAccum");
        NonNullList<ItemStack> items = NonNullList.withSize(MAX_FUEL_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < MAX_FUEL_SLOTS; i++) {
            fuelSlots.setItem(i, items.get(i));
        }
        NonNullList<ItemStack> coolers = NonNullList.withSize(MAX_COOLER_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag.getCompound("CoolerItems"), coolers);
        for (int i = 0; i < MAX_COOLER_SLOTS; i++) {
            coolerSlots.setItem(i, coolers.get(i));
        }
    }
}
