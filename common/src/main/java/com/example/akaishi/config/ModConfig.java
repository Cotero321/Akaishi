package com.example.akaishi.config;

/**
 * 模组数值配置中心：所有可调平衡的核心数值在此声明默认值，
 * 由平台加载器（ForgeConfigSpec）在配置加载/重载时覆盖写入。
 * volatile 保证配置热更新后各 tick 线程可见。
 */
public final class ModConfig {

    private ModConfig() {
    }

    // ==================== 反应堆 ====================
    /** 每个燃料槽满载（利用率 10）每 tick 产出的赤能源 */
    public static volatile long reactorEnergyPerSlot = 1_500_000L;
    /** 基础温度（无燃料热值时） */
    public static volatile int reactorBaseTemp = 300;
    /** 被动散热系数（固定 0.2） */
    public static volatile double reactorPassiveCool = 0.2;
    /** 散热片散热系数（0.7 × 散热效率） */
    public static volatile double reactorCoolerCool = 0.7;
    /** 单槽每 tick 基础燃料消耗（mb） */
    public static volatile double reactorDrainBase = 1.0 / 50.0;
    /** 燃料消耗 → 废料产出比例 */
    public static volatile double reactorWasteRatio = 0.2;
    /** 控制器废料缓冲罐容量（mb） */
    public static volatile int reactorWasteCapacity = 64_000;
    /** 温度上限：达到后启动爆炸倒计时 */
    public static volatile int reactorTempMax = 1000;
    /** 最优产率温度区间下限 */
    public static volatile int reactorTempOptMin = 400;
    /** 最优产率温度区间上限 */
    public static volatile int reactorTempOptMax = 700;
    /** 高温警告阈值：达到后降低产率并广播警告 */
    public static volatile int reactorTempWarn = 850;
    /** 满温度到爆炸的延迟（tick，默认 10 秒） */
    public static volatile int reactorExplosionDelayTicks = 200;

    // ==================== 液体管道 ====================
    /** 单段管道每 tick 最大传输量（mb） */
    public static volatile int fluidPipeRate = 4000;
    /** 单段管道缓冲罐容量（mb） */
    public static volatile int fluidPipeBufferCapacity = 8000;

    // ==================== 废品口 ====================
    /** 废品口废料缓冲罐容量（mb） */
    public static volatile int wastePortBufferCapacity = 64_000;

    // ==================== 衰竭保存桶 ====================
    /** 保存桶废料容量（mb） */
    public static volatile long exhaustedBarrelCapacity = 1_000_000L;

    // ==================== 生命活化器 ====================
    /** 活化器生命能量存储容量 */
    public static volatile long lifeActivatorLifeCapacity = 200_000L;
    /** 每转化 1mb 废料消耗的生命能量 */
    public static volatile long lifeActivatorCostPerMb = 100L;
    /** 输入罐（废料）容量（mb） */
    public static volatile long lifeActivatorInputCapacity = 8_000L;
    /** 输出罐（活化液体）容量（mb） */
    public static volatile long lifeActivatorOutputCapacity = 16_000L;
    /** 每 tick 最大转化量（mb） */
    public static volatile long lifeActivatorConvertRate = 4L;

    // ==================== 生命离心机 ====================
    /** 离心机赤能源存储容量 */
    public static volatile long lifeCentrifugeEnergyCapacity = 100_000L;
    /** 输入罐（活化燃料）容量（mb） */
    public static volatile long lifeCentrifugeInputCapacity = 64_000L;
    /** 每 tick 最大分离量（mb） */
    public static volatile long lifeCentrifugeConvertRate = 8L;
    /** 每 1mb 活化燃料分离消耗的赤能源 */
    public static volatile long lifeCentrifugeCostPerMb = 50L;

    // ==================== 物品重构仪 ====================
    /** 重构仪赤能源存储容量 */
    public static volatile long reconstructorEnergyCapacity = 100_000L;
    /** 每消耗 1 衰竭结晶重构消耗的赤能源 */
    public static volatile long reconstructorCostPerCrystal = 50L;

    // ==================== 活化分馏器 ====================
    /** 分馏器赤能源存储容量 */
    public static volatile long fractionatorEnergyCapacity = 100_000L;
    /** 每次分馏（1 活化结晶 → 1 活化成分 + 1 衰竭结晶）消耗的赤能源 */
    public static volatile long fractionatorCostPerCraft = 2_000L;
    /** 每次分馏耗时（tick） */
    public static volatile int fractionatorProcessTicks = 100;

    // ==================== 聚变燃料聚合器 ====================
    /** 聚合器赤能源存储容量 */
    public static volatile long aggregatorEnergyCapacity = 100_000L;
    /** 每次聚合（1 活化成分 → 1000mb 对应等离子体）消耗的赤能源 */
    public static volatile long aggregatorCostPerCraft = 2_000L;
    /** 每次聚合耗时（tick） */
    public static volatile int aggregatorProcessTicks = 100;
    /** 各等离子体输出罐容量（mb） */
    public static volatile long aggregatorPlasmaCapacity = 8_000L;
    /** 每次聚合产出的等离子体量（mb） */
    public static volatile long aggregatorProducePerCraft = 1_000L;

    // ==================== 离子体填装器 ====================
    /** 填装器各等离子体输入罐容量（mb） */
    public static volatile long fillerPlasmaCapacity = 8_000L;
    /** 每根反应棒灌装的等离子体量（mb） */
    public static volatile long fillerPlasmaPerRod = 1_000L;
    /** 填装一根燃料棒耗时（tick） */
    public static volatile int fillerProcessTicks = 100;

    // ==================== 赤石植物培养机 ====================
    /** 植物培养机赤能源缓冲容量 */
    public static volatile long plantCultivatorEnergyCapacity = 100_000L;
    /** 培养一次作物耗时（tick） */
    public static volatile int plantCultivatorTicks = 200;
    /** 培养每 tick 消耗的赤能源 */
    public static volatile long plantCultivatorCostPerTick = 10L;

    // ==================== 赤石压缩机 ====================
    /** 压缩机赤能源缓冲容量 */
    public static volatile long compressorEnergyCapacity = 100_000L;
    /** 压缩一次耗时（tick） */
    public static volatile int compressorTicks = 100;
    /** 压缩每 tick 消耗的赤能源 */
    public static volatile long compressorCostPerTick = 15L;

    // ==================== 赤石打粉机 ====================
    /** 打粉机赤能源缓冲容量 */
    public static volatile long pulverizerEnergyCapacity = 100_000L;
    /** 打粉一次耗时（tick） */
    public static volatile int pulverizerTicks = 100;
    /** 打粉每 tick 消耗的赤能源 */
    public static volatile long pulverizerCostPerTick = 15L;

    // ==================== 赤石变化器 ====================
    /** 变化器赤能源缓冲容量 */
    public static volatile long transformerEnergyCapacity = 100_000L;
    /** 变化一次耗时（tick） */
    public static volatile int transformerTicks = 100;
    /** 变化每 tick 消耗的赤能源 */
    public static volatile long transformerCostPerTick = 15L;

    // ==================== 赤石矿机 ====================
    /** 挖矿一次基础耗时（tick，等级倍率在此基础上加速） */
    public static volatile int minerTicksBase = 200;
    /** 挖矿每 tick 基础能耗（速度升级按比例提升） */
    public static volatile long minerCostPerTickBase = 10L;

    // ==================== 衰竭区域 ====================
    /** 衰竭区域持续时长（tick，默认 30 小时） */
    public static volatile long decayZoneDurationTicks = 30L * 60 * 60 * 20;

    // ==================== 衰变净化塔 ====================
    /** 净化塔赤能源缓冲容量 */
    public static volatile long decayPurifierEnergyCapacity = 1_000_000L;
    /** 净化作用范围（格，塔心到区域中心的欧氏距离） */
    public static volatile int decayPurifierRange = 80;
    /** 每 tick 净化消耗的赤能源 */
    public static volatile long decayPurifierCostPerTick = 2_000L;
    /** 每 tick 削减的衰竭区域剩余时间（tick，10 = 区域 10 倍速消散） */
    public static volatile long decayPurifierTicksPerTick = 10L;

    // ==================== 无线赤能源 ====================
    /** 无线区块加载能量税：每区块每 tick 消耗的赤石能量（区块加载构架生效时按加载区块数持续扣费） */
    public static volatile long wirelessChunkTaxPerChunk = 1_000L;

    // ==================== 聚变堆 ====================
    /** 效率框架增长系数：每个效率框架使产率/产热 ×该值（默认 1.15，12 个 → 约 5.35 倍） */
    public static volatile double fusionEfficiencyGrowth = 1.15;
    /** 每个散热框架的散热乘数加成（0.1 = 每框架 +10%，满载 10 个散热片总量再 ×2） */
    public static volatile double fusionCoolerFrameBonus = 0.1;
    /** 每 1% 散热效率抵消的温度（M） */
    public static volatile long fusionCoolingPerPercent = 2_000_000L;
    /** 基础温度（无燃料热值时） */
    public static volatile int fusionBaseTemp = 50_000_000;
    /** 温度硬上限（clamp 与产率线性下滑的终点，M）；实际超温停机见 fusionTempTrip */
    public static volatile int fusionTempMax = 160_000_000;
    /** 过热停机阈值：温度 ≥ 该值立即停烧（M）。略低于硬上限，避免堆子贴着极限长时间运行 */
    public static volatile int fusionTempTrip = 159_000_000;
    /** 最优产率温度区间下限 */
    public static volatile int fusionTempOptMin = 100_000_000;
    /** 最优产率温度区间上限 */
    public static volatile int fusionTempOptMax = 130_000_000;
    /** 宕机后恢复温度（上限一半） */
    public static volatile int fusionTempResume = 80_000_000;
    /** 温度渐变夹紧步长（每 tick 最大变化量，防突变） */
    public static volatile int fusionTempStep = 2_000_000;
    /** 散热片耐久消耗间隔（tick，默认 100 = 5 秒 1 点） */
    public static volatile int fusionCoolerDurabilityInterval = 100;
    /** 每消耗 1 份该能量产出 1 个生命灰烬（1000 亿） */
    public static volatile long fusionAshPerEnergy = 100_000_000_000L;
    /** 燃料棒总能量（赤能源） */
    public static volatile long fusionRodEnergy = 6_000_000_000_000L;

    // ==================== 无线赤能源 ====================
    /** 无线传输基础损耗比例（每口每次传输扣除） */
    public static volatile double wirelessBaseLoss = 0.05;
    /** 每格距离额外损耗比例 */
    public static volatile double wirelessLossPerBlock = 0.001;
    /** 损耗上限（防极端场景无意义传输） */
    public static volatile double wirelessMaxLoss = 0.5;
    /** 跨维度传输固定损耗（需终端结构含跨维组件解锁） */
    public static volatile double wirelessCrossDimLoss = 0.25;
    /** 每个损耗抑制组件削减的损耗比例（0.05 = 削减 5%，可叠加，最高削减 90%） */
    public static volatile double wirelessLossReductionPerModule = 0.05;
}
