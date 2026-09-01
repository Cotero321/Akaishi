package com.example.template.config;

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

    // ==================== 衰竭区域 ====================
    /** 衰竭区域持续时长（tick，默认 30 小时） */
    public static volatile long decayZoneDurationTicks = 30L * 60 * 60 * 20;

    // ==================== 无线赤能源 ====================
    /** 无线传输基础损耗比例（每口每次传输扣除） */
    public static volatile double wirelessBaseLoss = 0.05;
    /** 每格距离额外损耗比例 */
    public static volatile double wirelessLossPerBlock = 0.001;
    /** 损耗上限（防极端场景无意义传输） */
    public static volatile double wirelessMaxLoss = 0.5;
    /** 输入口/输出口每 tick 基础传输上限（按身份卡等级倍率提升） */
    public static volatile long wirelessPortTransferRate = 4096L;
    /** 跨维度传输固定损耗（需终端结构含跨维组件解锁） */
    public static volatile double wirelessCrossDimLoss = 0.25;
    /** 每个损耗抑制组件削减的损耗比例（0.05 = 削减 5%，可叠加，最高削减 90%） */
    public static volatile double wirelessLossReductionPerModule = 0.05;
}
