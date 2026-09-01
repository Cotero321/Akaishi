package com.example.template.block.entity;

import com.example.template.api.IDataCarrier;

import com.example.template.block.ChishiReactorControllerBlock;
import com.example.template.config.ModConfig;
import com.example.template.decay.DecayZoneManager;
import com.example.template.sound.ModSounds;
import com.example.template.fluid.ModFluids;
import com.example.template.fluid.MultiFluidTank;
import com.example.template.fluid.ReactorFuels;
import com.example.template.item.ChishiFuelCellItem;
import com.example.template.menu.ChishiReactorControllerMenu;
import com.example.template.reactor.ReactorStructure;
import dev.architectury.fluid.FluidStack;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反应堆控制器方块实体：多方块反应堆的主方块与状态载体。
 * <p>
 * 每 tick 委托 {@link ReactorStructure} 扫描结构（单层封闭长方体，边长 5 或 7），
 * 成型后驱动燃烧结算：燃料消耗 → 赤能源产出（分发到能量输出口）→ 废品生成（衰竭燃料）→ 温度结算。
 * 温度 = (基础 + Σ每棒热值) × (1 − 0.2 − 0.7×散热效率)：超过警告阈值进入
 * 高温警告并降低产量（不停机）；达到温度上限后启动爆炸倒计时，归零触发爆炸并产生"衰竭五"衰竭区域。
 * 满热量时被破坏/拆结构会触发普通衰竭区域。拆任意部件结构即失效，停止燃烧。
 * 所有数值参数（产率/温度/散热/废料）由 {@link com.example.template.config.ModConfig} 提供。
 */
public class ChishiReactorControllerBlockEntity extends BlockEntity implements ExtendedMenuProvider, IDataCarrier {

    // ===== 反应堆参数 =====
    /** 燃料槽上限 = 燃料棒数量上限 */
    public static final int MAX_FUEL_SLOTS = 10;

    // ===== 数据槽 =====
    public static final int DATA_TEMP = 0;
    public static final int DATA_FORMED = 1;
    public static final int DATA_ROD_COUNT = 2;
    public static final int DATA_EFFECTIVE_COOLERS = 3;
    public static final int DATA_COOLING_PERCENT = 4;
    public static final int DATA_WASTE_AMOUNT = 5;
    public static final int DATA_WASTE_CAPACITY = 6;
    public static final int DATA_WARNING = 7;
    public static final int DATA_ACTIVE_SLOTS = 8;
    public static final int DATA_ENERGY_PER_TICK = 9;
    public static final int DATA_FUEL_DRAIN = 10;
    public static final int DATA_WASTE_FULL = 11;
    public static final int DATA_COOLER_DURABILITY = 12;
    /** 结构内散热组件总数（客户端用于绘制温度页散热片槽位，≤ {@link ReactorStructure#MAX_COOLERS}） */
    public static final int DATA_COOLER_COUNT = 13;
    /** 废品罐内衰竭燃料种类数（客户端 tooltip 展示） */
    public static final int DATA_WASTE_TYPES = 14;
    public static final int DATA_SLOTS = 15;

    private final SimpleContainer fuelSlots;
    private final SimpleContainerData data;
    private final MultiFluidTank wasteTank;
    private final double[] drainAccumulator = new double[MAX_FUEL_SLOTS];

    private int temp;
    /** 满热量爆炸倒计时（tick）：-1 = 未启动，>0 = 倒计时中（启动后不可逆） */
    private int explosionCountdown = -1;
    /** 高温警告已广播标记（防止刷屏） */
    private boolean warnBroadcast;
    /** 爆炸已完成标记：清空结构时置位，阻止 onRemove 按"满热量被挖"重复触发衰竭区域 */
    private boolean exploded;
    private int activeSlots;
    private double fuelDrainPerTick;
    private long energyPerTick;
    /** 当前各燃烧槽热值总和（原始温度 = 基础温度 300 + Σ热值） */
    private double heatValueTotal;

    /** 最近一次成功扫描的结构（未成型为 null） */
    private ReactorStructure.Result structure;

    // ===== 结构扫描缓存 =====
    /** 结构扫描定时器：未成型 5 tick、成型后 20 tick 兜底重扫（方块变更事件立即失效缓存） */
    private static final int SCAN_INTERVAL = 20;
    private static final int SCAN_INTERVAL_UNFORMED = 5;
    /** 扫描缓存失效标记（方块变更事件置位） */
    private boolean structureDirty = true;
    private int scanCooldown;
    /** 运转/警告音效播放冷却（tick）：避免每 tick 播放导致音效叠加过密 */
    private int humCooldown;
    private int warnCooldown;
    /** 活跃控制器注册表（维度 → 控制器位置）：供方块变更事件定位并失效对应控制器缓存 */
    private static final Map<Level, Set<BlockPos>> ACTIVE = new ConcurrentHashMap<>();

    public ChishiReactorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_REACTOR_CONTROLLER.get(), pos, state);
        this.fuelSlots = new SimpleContainer(MAX_FUEL_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                ChishiReactorControllerBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
        this.wasteTank = new MultiFluidTank(ModConfig.reactorWasteCapacity) {
            @Override
            public long fill(FluidStack resource, boolean simulate) {
                // 只接受衰竭燃料
                if (resource == null || !ModFluids.isExhaustedFuel(resource.getFluid())) {
                    return 0;
                }
                return super.fill(resource, simulate);
            }

            @Override
            protected void onChanged() {
                setChanged();
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiReactorControllerBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 注册到活跃控制器集合（事件失效扫描缓存用）；惰性清理由 invalidateNearby 完成
        ACTIVE.computeIfAbsent(level, k -> ConcurrentHashMap.newKeySet()).add(worldPosition);

        // 结构扫描缓存：成型期间方块几乎不变，定时兜底重扫 + 方块变更事件立即失效
        ReactorStructure.Result scanned;
        if (structureDirty || --scanCooldown <= 0) {
            scanned = ReactorStructure.scan(level, worldPosition);
            structureDirty = false;
            scanCooldown = scanned != null ? SCAN_INTERVAL : SCAN_INTERVAL_UNFORMED;
        } else {
            scanned = structure;
        }
        boolean formed = scanned != null;
        boolean wasFormed = getBlockState().getValue(ChishiReactorControllerBlock.FORMED);
        if (wasFormed != formed) {
            level.setBlock(worldPosition, getBlockState().setValue(ChishiReactorControllerBlock.FORMED, formed), 3);
        }
        structure = scanned;

        if (formed) {
            // 仅在「未成型 → 成型」切换时广播一次控制器坐标，成型期间部件位置不变无需每 tick 广播
            if (!wasFormed) {
                broadcastToParts();
                // 结构成型音效（仅切换瞬间播放一次）
                level.playSound(null, worldPosition, ModSounds.MULTIBLOCK_ACTIVATE.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
            }
            tickBurning();
            // 燃烧运转声（循环）：每 15 tick 重播短音模拟持续运转
            if (energyPerTick > 0 && --humCooldown <= 0) {
                level.playSound(null, worldPosition, ModSounds.REACTOR_HUM.get(), SoundSource.BLOCKS, 0.45f, 1.0f);
                humCooldown = 15;
            }
            tickTemperature();
            // 高温警告：超过警告阈值每 25 tick 播报警
            if (temp >= ModConfig.reactorTempWarn && --warnCooldown <= 0) {
                level.playSound(null, worldPosition, ModSounds.REACTOR_WARN.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
                warnCooldown = 25;
            }
            tickOverheat();
        } else {
            // 满热量 + 此前成型（工作状态）时被拆结构 → 触发普通衰竭区域（燃料泄漏）
            if (wasFormed && temp >= ModConfig.reactorTempMax) {
                DecayZoneManager.createZone((ServerLevel) level, worldPosition, 0, false);
                setChanged();
            }
            // 结构失效：停止燃烧，爆炸倒计时作废
            explosionCountdown = -1;
            heatValueTotal = 0;
            activeSlots = 0;
            energyPerTick = 0;
            fuelDrainPerTick = 0;
            Arrays.fill(drainAccumulator, 0.0);
            tickTemperature();
        }
        updateData();
    }

    /**
     * 方块变更事件入口：变更位置落在某活跃控制器结构范围（7×7×7 + 外扩 1）内时，
     * 失效其扫描缓存；同时惰性清理已拆除的控制器（BE 不存在时）。
     * 误报无害（仅触发一次重扫），漏报会导致结构变化不生效。
     */
    public static void invalidateNearby(Level level, BlockPos changedPos) {
        Set<BlockPos> controllers = ACTIVE.get(level);
        if (controllers == null || controllers.isEmpty()) {
            return;
        }
        for (BlockPos c : controllers) {
            if (Math.abs(changedPos.getX() - c.getX()) <= 8
                    && Math.abs(changedPos.getY() - c.getY()) <= 8
                    && Math.abs(changedPos.getZ() - c.getZ()) <= 8) {
                if (level.getBlockEntity(c) instanceof ChishiReactorControllerBlockEntity be) {
                    be.structureDirty = true;
                } else {
                    controllers.remove(c);
                }
            }
        }
    }

    /** 向成型结构内的部件广播控制器坐标（部件靠坐标回查控制器） */
    private void broadcastToParts() {
        if (structure == null) {
            return;
        }
        for (BlockPos p : structure.energyPorts) {
            if (level.getBlockEntity(p) instanceof ChishiReactorEnergyOutputBlockEntity e) {
                e.setControllerPos(worldPosition);
            }
        }
        for (BlockPos p : structure.wastePorts) {
            if (level.getBlockEntity(p) instanceof ChishiReactorWastePortBlockEntity w) {
                w.setControllerPos(worldPosition);
            }
        }
        for (BlockPos p : structure.fuelPorts) {
            if (level.getBlockEntity(p) instanceof ChishiReactorFuelPortBlockEntity f) {
                f.setControllerPos(worldPosition);
            }
        }
        for (BlockPos p : structure.coolers) {
            if (level.getBlockEntity(p) instanceof ChishiReactorCoolerBlockEntity c) {
                c.setControllerPos(worldPosition);
            }
        }
    }

    // ===== 燃烧结算 =====

    private void tickBurning() {
        int rodCount = structure.rodCount;
        boolean wasteFull = wasteTank.isFull();
        int active = 0;
        double totalDrain = 0;
        long totalEnergy = 0;
        double heatValue = 0;
        double tempCoeff = temperatureCoefficient();

        for (int i = 0; i < MAX_FUEL_SLOTS && i < rodCount; i++) {
            if (wasteFull) {
                continue; // 废品满 → 跳过当前槽位（后续槽位同样跳过，等效停摆）
            }
            ItemStack cell = fuelSlots.getItem(i);
            Fluid fuelFluid = ChishiFuelCellItem.getFluid(cell);
            int util = ReactorFuels.getEnergyUtilization(fuelFluid);
            if (util <= 0 || ChishiFuelCellItem.isEmpty(cell)) {
                continue;
            }
            active++;
            // 燃料消耗恒定（与利用率无关）：利用率只决定产率与每 mb 能量密度，消耗固定每 2.5 秒 1mb（0.02 mb/tick）
            double drain = ModConfig.reactorDrainBase;
            totalDrain += drain;
            drainAccumulator[i] += drain;
            if (drainAccumulator[i] >= 1.0) {
                int mb = (int) drainAccumulator[i];
                int amount = ChishiFuelCellItem.getAmount(cell);
                int consume = Math.min(mb, amount);
                long waste = (long) (consume * ModConfig.reactorWasteRatio);
                // 废品空间不足：本槽不消耗燃料、不生成废品，避免 fill 截断导致废品静默丢失
                if (wasteTank.getCapacity() - wasteTank.getAmount() < waste) {
                    drainAccumulator[i] = 0;
                    wasteFull = true;
                    continue;
                }
                drainAccumulator[i] -= consume;
                if (consume >= amount) {
                    drainAccumulator[i] = 0;
                }
                ChishiFuelCellItem.setFluid(cell, fuelFluid, amount - consume);
                // 5mb 燃料 → 1mb 对应类型的衰竭燃料（空间已校验，fill 必定全额填入）
                wasteTank.fill(FluidStack.create(ModFluids.exhaustedFuelFor(fuelFluid), waste), false);
                if (wasteTank.isFull()) {
                    wasteFull = true;
                }
            }
            // 产率 = 每槽上限 × 能量利用率/10 × 温度系数
            long per = (long) (ModConfig.reactorEnergyPerSlot * (util / 10.0) * tempCoeff);
            totalEnergy += per;
            // 热值累加：每棒热值 = 40 × 利用率 × 产热系数（决定原始温度）
            heatValue += ReactorFuels.heatValue(fuelFluid);
        }

        activeSlots = active;
        fuelDrainPerTick = totalDrain;
        energyPerTick = totalEnergy;
        heatValueTotal = heatValue;
        distributeEnergy(totalEnergy);
        pushWaste();
        // 有效散热组件耐久消耗（每 tick 1 点；未贴邻燃料棒的无效组件不消耗）
        if (active > 0 && !wasteTank.isFull()) {
            for (BlockPos p : structure.effectiveCoolerPositions) {
                if (level.getBlockEntity(p) instanceof ChishiReactorCoolerBlockEntity c) {
                    c.consumeDurability();
                }
            }
        }
    }

    /** 温度系数：400~700℃ 为 1.0；低于 400℃ 线性提升（每低 100℃ +10%，利用率无上限），
     *  高于 700℃ 线性降至 10%（过热减产） */
    private double temperatureCoefficient() {
        if (temp <= ModConfig.reactorTempOptMin) {
            return 1.0 + (ModConfig.reactorTempOptMin - temp) * 0.001; // 强力散热压低温 → 利用率突破 1.0
        }
        if (temp <= ModConfig.reactorTempOptMax) {
            return 1.0;
        }
        return 1.0 - (temp - ModConfig.reactorTempOptMax) * 0.9 / 300.0;
    }

    /** 温度结算（静态公式）：
     *  原始温度 = 基础温度 300 + Σ每棒热值（未散热理论值）
     *  实际温度 = 原始温度 × (1 − 被动散热 0.2 − 散热片 0.7 × 散热效率)，散热效率无上限（可 >100%）
     *  燃烧停止（未成型/无燃料/废品满）时热值总和为 0 → 温度回落至 240℃ */
    private void tickTemperature() {
        double raw = ModConfig.reactorBaseTemp + heatValueTotal;
        int coolingPercent = structure == null ? 0 : structure.coolingPercent;
        double eff = coolingPercent / 100.0; // 取消 100% 封顶：满配 20 终极 = 140%
        double next = raw * (1.0 - ModConfig.reactorPassiveCool - ModConfig.reactorCoolerCool * eff);
        temp = (int) Math.max(0, Math.min(ModConfig.reactorTempMax, next));
    }

    /** 高温警告 + 满热量爆炸倒计时（不停机，仅减产与警告） */
    private void tickOverheat() {
        if (temp >= ModConfig.reactorTempWarn && !warnBroadcast) {
            warnBroadcast = true;
            broadcastWarning(Component.translatable("message.template_mod.reactor.overheat_warn", temp));
            setChanged();
        } else if (temp < ModConfig.reactorTempWarn) {
            warnBroadcast = false;
        }
        // 满热量 → 启动 10 秒爆炸倒计时（不可逆，温度回落也不取消）
        if (temp >= ModConfig.reactorTempMax && explosionCountdown < 0) {
            explosionCountdown = ModConfig.reactorExplosionDelayTicks;
            broadcastWarning(Component.translatable("message.template_mod.reactor.at_max_heat",
                    ModConfig.reactorExplosionDelayTicks / 20));
            setChanged();
        }
        if (explosionCountdown > 0) {
            explosionCountdown--;
            if (explosionCountdown == 100) {
                broadcastWarning(Component.translatable("message.template_mod.reactor.explode_soon"));
            }
            if (explosionCountdown == 0) {
                explodeReactor();
            }
        }
    }

    /** 满热量爆炸：触发"衰竭五"衰竭区域，摧毁反应堆自身（不破坏周围地形） */
    private void explodeReactor() {
        if (level.isClientSide) {
            return;
        }
        // 爆炸触发的衰竭区域施加衰竭五效果（等级 5 = amplifier 4）
        DecayZoneManager.createZone((ServerLevel) level, worldPosition, 4, true);
        // 置位后清空结构：控制器 onRemove 不再重复触发衰竭区域
        exploded = true;
        // 爆炸视觉与音效（不破坏方块，结构由下方手动清空）
        level.playSound(null, worldPosition, ModSounds.REACTOR_EXPLOSION.get(), SoundSource.BLOCKS, 1.5f, 1.0f);
        level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                6.0f, Level.ExplosionInteraction.NONE);
        // 摧毁反应堆自身（控制器 + 全部结构方块）
        ReactorStructure.Result s = structure;
        if (s != null) {
            for (BlockPos p : BlockPos.betweenClosed(s.min, s.max)) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        } else {
            level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** 全服广播警告消息 */
    private void broadcastWarning(Component msg) {
        for (Player p : level.players()) {
            p.displayClientMessage(Component.translatable("message.template_mod.reactor.prefix")
                    .append(msg), false);
        }
    }

    /** 将产出的赤能源平分到全部能量输出口（输出口缓冲满则溢出丢弃） */
    private void distributeEnergy(long amount) {
        if (amount <= 0 || structure.energyPorts.isEmpty()) {
            return;
        }
        long share = amount / structure.energyPorts.size();
        for (BlockPos p : structure.energyPorts) {
            if (level.getBlockEntity(p) instanceof ChishiReactorEnergyOutputBlockEntity e) {
                e.receiveEnergy(share);
            }
        }
    }

    /** 将废品罐中的衰竭燃料按类型逐种灌入废品输出口缓冲罐 */
    private void pushWaste() {
        if (wasteTank.isEmpty() || structure.wastePorts.isEmpty()) {
            return;
        }
        List<BlockPos> ports = structure.wastePorts;
        // 遍历液体类型快照：drain 会修改存储，快照保证循环安全
        for (Fluid fluid : wasteTank.fluidTypes()) {
            long amount = wasteTank.getAmount(fluid);
            if (amount <= 0) {
                continue;
            }
            for (BlockPos p : ports) {
                if (amount <= 0) {
                    break;
                }
                if (level.getBlockEntity(p) instanceof ChishiReactorWastePortBlockEntity w) {
                    long accepted = w.acceptWaste(FluidStack.create(fluid, amount));
                    if (accepted > 0) {
                        wasteTank.drain(fluid, accepted, false);
                        amount -= accepted;
                    }
                }
            }
        }
    }

    // ===== GUI 数据 =====

    /** 有效散热器散热片的最低剩余耐久百分比（0-100）；无散热片返回 100 */
    private int minCoolerDurability() {
        if (structure == null) {
            return 100;
        }
        int min = 100;
        for (BlockPos p : structure.effectiveCoolerPositions) {
            int d = ChishiReactorCoolerBlockEntity.getDurabilityPercentAt(level, p);
            if (d >= 0) {
                min = Math.min(min, d);
            }
        }
        return min;
    }

    private void updateData() {
        data.set(DATA_TEMP, temp);
        data.set(DATA_FORMED, getBlockState().getValue(ChishiReactorControllerBlock.FORMED) ? 1 : 0);
        data.set(DATA_ROD_COUNT, structure == null ? 0 : structure.rodCount);
        data.set(DATA_EFFECTIVE_COOLERS, structure == null ? 0 : structure.effectiveCoolers);
        data.set(DATA_COOLING_PERCENT, structure == null ? 0 : structure.coolingPercent);
        data.set(DATA_WASTE_AMOUNT, (int) wasteTank.getAmount());
        data.set(DATA_WASTE_CAPACITY, (int) wasteTank.getCapacity());
        data.set(DATA_WARNING, temp >= ModConfig.reactorTempWarn ? 1 : 0);
        data.set(DATA_ACTIVE_SLOTS, activeSlots);
        data.set(DATA_ENERGY_PER_TICK, (int) Math.min(Integer.MAX_VALUE, energyPerTick));
        data.set(DATA_FUEL_DRAIN, (int) (fuelDrainPerTick * 1000));
        data.set(DATA_WASTE_FULL, wasteTank.isFull() ? 1 : 0);
        data.set(DATA_COOLER_DURABILITY, minCoolerDurability());
        data.set(DATA_COOLER_COUNT, structure == null ? 0 : structure.coolers.size());
        data.set(DATA_WASTE_TYPES, wasteTank.typeCount());
    }

    public ContainerData data() {
        return data;
    }

    /** 最近一次结构扫描结果（未成型为 null），供菜单绑定散热组件槽位 */
    public ReactorStructure.Result getStructure() {
        return structure;
    }

    public Container fuelSlots() {
        return fuelSlots;
    }

    /** 是否已成型（部件用） */
    public boolean isFormed() {
        return getBlockState().getValue(ChishiReactorControllerBlock.FORMED);
    }

    /** 是否处于高温警告状态（温度超过警告线，产量已降低） */
    public boolean isWarning() {
        return temp >= ModConfig.reactorTempWarn;
    }

    /** 满热量爆炸倒计时剩余 tick（-1 = 未启动），供 GUI/测试展示 */
    public int getExplosionCountdown() {
        return explosionCountdown;
    }

    /** 是否已达满热量（被破坏/拆结构触发泄漏的前提） */
    public boolean isAtFullHeat() {
        return temp >= ModConfig.reactorTempMax;
    }

    /** 爆炸是否已完成（供方块 onRemove 判定是否为爆炸清空，避免重复触发区域） */
    public boolean exploded() {
        return exploded;
    }

    /** 当前温度（供方块 onRemove 判定泄漏） */
    public int temp() {
        return temp;
    }

    /** 是否正在燃烧（散热片耐久消耗的判定依据） */
    public boolean isBurning() {
        return isFormed() && activeSlots > 0 && !wasteTank.isFull();
    }

    public int getRodCount() {
        return structure == null ? 0 : structure.rodCount;
    }

    // ===== 菜单 / 序列化 =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.template_mod.chishi_reactor_controller");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ChishiReactorControllerMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    /** 挖掘保留数据：反应堆不保留内部燃料棒与废料，仅保留热量 Temp 与爆炸倒计时 */
    @Override
    public String[] excludedKeys() {
        return new String[]{"Items", "WasteTank"};
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Temp", temp);
        tag.putInt("ExplosionCountdown", explosionCountdown);
        tag.put("WasteTank", wasteTank.writeToNbt());
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
        explosionCountdown = tag.contains("ExplosionCountdown") ? tag.getInt("ExplosionCountdown") : -1;
        if (tag.contains("WasteTank")) {
            wasteTank.readFromNbt(tag.getCompound("WasteTank"));
        }
        NonNullList<ItemStack> items = NonNullList.withSize(MAX_FUEL_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < MAX_FUEL_SLOTS; i++) {
            fuelSlots.setItem(i, items.get(i));
        }
    }
}
