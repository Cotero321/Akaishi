package com.example.template.block.entity;

import com.example.template.api.IDataCarrier;

import com.example.template.block.ChishiReactorControllerBlock;
import com.example.template.decay.DecayZoneManager;
import com.example.template.fluid.FluidTank;
import com.example.template.fluid.ModFluids;
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

import java.util.Arrays;

/**
 * 反应堆控制器方块实体：多方块反应堆的主方块与状态载体。
 * <p>
 * 每 tick 委托 {@link ReactorStructure} 扫描结构（单层封闭长方体，边长 5 或 7），
 * 成型后驱动燃烧结算：燃料消耗 → 赤能源产出（分发到能量输出口）→ 废品生成（衰竭燃料）→ 温度演化。
 * 温度超过 {@link #TEMP_WARN} 进入高温警告并降低产量（不停机）；热量达到上限 {@link #TEMP_MAX}
 * 后启动 {@link #EXPLOSION_DELAY_TICKS} tick 爆炸倒计时，归零触发爆炸并产生"衰竭五"衰竭区域。
 * 满热量时被破坏/拆结构会触发普通衰竭区域。拆任意部件结构即失效，停止燃烧。
 */
public class ChishiReactorControllerBlockEntity extends BlockEntity implements ExtendedMenuProvider, IDataCarrier {

    // ===== 反应堆参数 =====
    /** 燃料槽上限 = 燃料棒数量上限 */
    public static final int MAX_FUEL_SLOTS = 10;
    /** 废品罐容量（mb），满后停摆 */
    public static final int WASTE_CAPACITY = 64_000;
    /** 温度上限与最优区间 */
    public static final int TEMP_MAX = 1000, TEMP_OPT_MIN = 400, TEMP_OPT_MAX = 700;
    /** 高温警告阈值（℃）：达到后广播警告并降低产量（不停机） */
    public static final int TEMP_WARN = 850;
    /** 满热量（{@link #TEMP_MAX}）到爆炸的延迟（tick，10 秒） */
    public static final int EXPLOSION_DELAY_TICKS = 200;
    /** 每槽最大产率（赤能源/tick，能量利用率 10 且温度系数 1.0；满配 10 槽约 1500 万/tick） */
    public static final long ENERGY_PER_SLOT = 1_500_000L;
    /** 每槽产热系数（heat = 系数 × 能量利用率） */
    public static final double SLOT_HEAT = 4.0;
    /** 被动散热与散热片散热系数 */
    public static final double PASSIVE_COOL = 0.2, COOLER_COOL = 0.5;
    /** 温度变化速率 */
    public static final double TEMP_RATE = 0.1;
    /** 燃料消耗恒定速率：10 秒消耗 1mb（与燃料能量利用率无关）；利用率只影响产率 → 每 mb 能量密度 = 3亿 × 利用率/10 */
    public static final double DRAIN_BASE = 1.0 / 200.0;
    /** 废品比：1mb 燃料 → 3mb 衰竭燃料 */
    public static final int WASTE_RATIO = 3;

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
    public static final int DATA_SLOTS = 14;

    private final SimpleContainer fuelSlots;
    private final SimpleContainerData data;
    private final FluidTank wasteTank;
    private final double[] drainAccumulator = new double[MAX_FUEL_SLOTS];

    private int temp;
    /** 满热量爆炸倒计时（tick）：-1 = 未启动，>0 = 倒计时中（启动后不可逆） */
    private int explosionCountdown = -1;
    /** 高温警告已广播标记（防止刷屏） */
    private boolean warnBroadcast;
    private int activeSlots;
    private double fuelDrainPerTick;
    private long energyPerTick;
    private double heatInTotal;

    /** 最近一次成功扫描的结构（未成型为 null） */
    private ReactorStructure.Result structure;

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
        this.wasteTank = new FluidTank(WASTE_CAPACITY) {
            @Override
            public long fill(FluidStack resource, boolean simulate) {
                // 只接受衰竭的生命燃料
                if (resource == null || !isExhaustedFuel(resource.getFluid())) {
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

    /** 判断液体是否为衰竭的生命燃料 */
    public static boolean isExhaustedFuel(net.minecraft.world.level.material.Fluid fluid) {
        return fluid != null && fluid == ModFluids.get(ModFluids.EXHAUSTED_LIFE_FUEL_ID);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiReactorControllerBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        ReactorStructure.Result scanned = ReactorStructure.scan(level, worldPosition);
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
            }
            tickBurning();
            tickTemperature();
            tickOverheat();
        } else {
            // 满热量 + 此前成型（工作状态）时被拆结构 → 触发普通衰竭区域（燃料泄漏）
            if (wasFormed && temp >= TEMP_MAX) {
                DecayZoneManager.createZone((ServerLevel) level, worldPosition, 0, false);
                setChanged();
            }
            // 结构失效：停止燃烧，爆炸倒计时作废
            explosionCountdown = -1;
            heatInTotal = 0;
            activeSlots = 0;
            energyPerTick = 0;
            fuelDrainPerTick = 0;
            Arrays.fill(drainAccumulator, 0.0);
            tickTemperature();
        }
        updateData();
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
        double heatIn = 0;
        double tempCoeff = temperatureCoefficient();

        for (int i = 0; i < MAX_FUEL_SLOTS && i < rodCount; i++) {
            if (wasteFull) {
                continue; // 废品满 → 跳过当前槽位（后续槽位同样跳过，等效停摆）
            }
            ItemStack cell = fuelSlots.getItem(i);
            int util = ReactorFuels.getEnergyUtilization(ChishiFuelCellItem.getFluid(cell));
            if (util <= 0 || ChishiFuelCellItem.isEmpty(cell)) {
                continue;
            }
            active++;
            // 燃料消耗恒定（与利用率无关）：利用率只决定产率与每 mb 能量密度，消耗固定 10 秒 1mb
            double drain = DRAIN_BASE;
            totalDrain += drain;
            drainAccumulator[i] += drain;
            if (drainAccumulator[i] >= 1.0) {
                int mb = (int) drainAccumulator[i];
                int amount = ChishiFuelCellItem.getAmount(cell);
                int consume = Math.min(mb, amount);
                long waste = (long) consume * WASTE_RATIO;
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
                ChishiFuelCellItem.setFluid(cell, ChishiFuelCellItem.getFluid(cell), amount - consume);
                // 1mb 燃料 → 3mb 衰竭燃料（空间已校验，fill 必定全额填入）
                wasteTank.fill(FluidStack.create(ModFluids.get(ModFluids.EXHAUSTED_LIFE_FUEL_ID), waste), false);
                if (wasteTank.isFull()) {
                    wasteFull = true;
                }
            }
            // 产率 = 每槽上限 × 能量利用率/10 × 温度系数
            long per = (long) (ENERGY_PER_SLOT * (util / 10.0) * tempCoeff);
            totalEnergy += per;
            heatIn += SLOT_HEAT * util;
        }

        activeSlots = active;
        fuelDrainPerTick = totalDrain;
        energyPerTick = totalEnergy;
        heatInTotal = heatIn;
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

    /** 温度系数：400~700℃ 为 1.0，低于 400 线性 50%→100%，高于 700 线性 100%→10% */
    private double temperatureCoefficient() {
        if (temp <= TEMP_OPT_MIN) {
            return 0.5 + temp / 800.0;
        }
        if (temp <= TEMP_OPT_MAX) {
            return 1.0;
        }
        return 1.0 - (temp - TEMP_OPT_MAX) * 0.9 / 300.0;
    }

    /** 温度演化：产热 − 被动散热 − 散热片散热，按速率趋近平衡 */
    private void tickTemperature() {
        int coolingPercent = structure == null ? 0 : structure.coolingPercent;
        double heatOut = temp * (PASSIVE_COOL + COOLER_COOL * coolingPercent / 100.0);
        double next = temp + (heatInTotal - heatOut) * TEMP_RATE;
        temp = (int) Math.max(0, Math.min(TEMP_MAX, next));
    }

    /** 高温警告 + 满热量爆炸倒计时（不停机，仅减产与警告） */
    private void tickOverheat() {
        if (temp >= TEMP_WARN && !warnBroadcast) {
            warnBroadcast = true;
            broadcastWarning(Component.translatable("message.template_mod.reactor.overheat_warn", temp));
            setChanged();
        } else if (temp < TEMP_WARN) {
            warnBroadcast = false;
        }
        // 满热量 → 启动 10 秒爆炸倒计时（不可逆，温度回落也不取消）
        if (temp >= TEMP_MAX && explosionCountdown < 0) {
            explosionCountdown = EXPLOSION_DELAY_TICKS;
            broadcastWarning(Component.translatable("message.template_mod.reactor.at_max_heat",
                    EXPLOSION_DELAY_TICKS / 20));
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
        // 爆炸视觉与音效（不破坏方块，结构由下方手动清空）
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

    /** 将废品罐中的衰竭燃料灌入废品输出口缓冲罐 */
    private void pushWaste() {
        if (wasteTank.isEmpty() || structure.wastePorts.isEmpty()) {
            return;
        }
        long amount = wasteTank.getAmount();
        for (BlockPos p : structure.wastePorts) {
            if (amount <= 0) {
                break;
            }
            if (level.getBlockEntity(p) instanceof ChishiReactorWastePortBlockEntity w) {
                long accepted = w.acceptWaste(amount);
                if (accepted > 0) {
                    wasteTank.drain(accepted, false);
                    amount -= accepted;
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
        data.set(DATA_WARNING, temp >= TEMP_WARN ? 1 : 0);
        data.set(DATA_ACTIVE_SLOTS, activeSlots);
        data.set(DATA_ENERGY_PER_TICK, (int) Math.min(Integer.MAX_VALUE, energyPerTick));
        data.set(DATA_FUEL_DRAIN, (int) (fuelDrainPerTick * 1000));
        data.set(DATA_WASTE_FULL, wasteTank.isFull() ? 1 : 0);
        data.set(DATA_COOLER_DURABILITY, minCoolerDurability());
        data.set(DATA_COOLER_COUNT, structure == null ? 0 : structure.coolers.size());
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
        return temp >= TEMP_WARN;
    }

    /** 满热量爆炸倒计时剩余 tick（-1 = 未启动），供 GUI/测试展示 */
    public int getExplosionCountdown() {
        return explosionCountdown;
    }

    /** 是否已达满热量（被破坏/拆结构触发泄漏的前提） */
    public boolean isAtFullHeat() {
        return temp >= TEMP_MAX;
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
