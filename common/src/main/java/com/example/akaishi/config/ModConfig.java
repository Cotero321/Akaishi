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
    public static volatile long minerCostPerTickBase = 2000L;
    /** 精准模式时运折算除数：生效时运升级数 = 时运升级数 ÷ 该值向下取整（3 = 时运 1/3 生效） */
    public static volatile int minerPreciseFortuneDivisor = 3;
    /** 物品标签 #akaishi:miner/minerals 扩展矿物的抽奖权重（0 = 关闭标签扩展，只挖默认十种） */
    public static volatile int minerExtraOreWeight = 1;

    // ==================== 衰竭区域 ====================
    /** 衰竭区域持续时长（tick，默认 30 小时） */
    public static volatile long decayZoneDurationTicks = 30L * 60 * 60 * 20;
    /** 每区域每 tick 腐化方块采样次数（256=旧速；8192=旧速 32 倍；上限 65536=256 倍） */
    public static volatile int decayZoneEnvSamplesPerTick = 8192;

    // ==================== 衰变净化塔 ====================
    /** 净化塔赤能源缓冲容量 */
    public static volatile long decayPurifierEnergyCapacity = 1_000_000L;
    /** 净化作用范围（格，塔心到区域中心的欧氏距离） */
    public static volatile int decayPurifierRange = 80;
    /** 每 tick 净化消耗的赤能源 */
    public static volatile long decayPurifierCostPerTick = 2_000L;
    /** 每 tick 削减的衰竭区域剩余时间（tick，10 = 区域 10 倍速消散） */
    public static volatile long decayPurifierTicksPerTick = 10L;

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

    // ==================== 器官·品质曲线（override：0 = 不覆盖，用 QualityTier 内置默认） ====================
    /** 品质 I~IV 属性加成倍率 override（索引 = 品质序号） */
    public static volatile double[] organTierMultiplier = {0.0, 0.0, 0.0, 0.0};
    /** 品质 I~IV 移植基础排斥 override */
    public static volatile int[] organTierBaseRejection = {0, 0, 0, 0};
    /** 品质 I~IV 排斥增长间隔（秒）override */
    public static volatile int[] organTierGrowthInterval = {0, 0, 0, 0};

    // ==================== 基因来源组（override：0 = 不覆盖，用 SampleGroup 内置排斥系数） ====================
    /** 七组排斥系数 override（索引 = SampleGroup 序号：温血/亡灵/爆炸/异变/末影/Boss/龙） */
    public static volatile double[] groupRejectionFactor = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

    // ==================== 纯度联动（override：0 = 不覆盖，用 OrganLinkage 内置常量） ====================
    /** 完整度对排斥的最大削减比例 */
    public static volatile double purityRejectionCap = 0.0;
    /** 纯度对适配度偏置的最大权重 */
    public static volatile double purityCompatWeight = 0.0;

    // ==================== 排斥·标尺与阈值 ====================
    /** 排斥值上限 override（0 = 用 PlayerBodyState.MAX_REJECTION=100；达上限器官失效） */
    public static volatile int maxRejection = 0;
    /** 排斥负面效果触发阈值（≥ 该值开始掷中毒/虚弱） */
    public static volatile int rejectionWarning = 60;
    /** 排斥中毒阈值 */
    public static volatile int rejectionPoison = 80;
    /** 排斥增速翻倍线：有效适配度 < 该值排斥速率 ×2 */
    public static volatile int compatSevereThreshold = 60;
    /** 部位 debuff 豁免线：有效适配度 ≥ 该值无部位负面 */
    public static volatile int slotDebuffCleanThreshold = 70;
    /** 部位 debuff 重度线：有效适配度 < 该值部位负面升 II 级 */
    public static volatile int slotDebuffSevereThreshold = 45;
    /** 排斥增长间隔下限（tick，15s/点） */
    public static volatile int growthIntervalMinTicks = 300;
    /** 天敌反噬周期（tick） */
    public static volatile int conflictPunishIntervalTicks = 100;
    /** 天敌反噬每次自伤伤害（爆炸伤害源） */
    public static volatile double conflictPunishDamage = 5.0;
    /** 躯体超载 I 线：全身总排斥 ≥ 该值 → 缓慢 I */
    public static volatile int overloadLight = 320;
    /** 躯体超载 II 线：全身总排斥 ≥ 该值 → 缓慢 II + 虚弱 */
    public static volatile int overloadHeavy = 450;

    // ==================== 排异中和剂（血清） ====================
    /** 每瓶每个可洗器官的排斥下降量 */
    public static volatile int serumWashReduce = 12;
    /** 每个器官每次移植可被清洗的次数上限 */
    public static volatile int serumWashLimit = 6;
    /** 饮用冷却（tick） */
    public static volatile int serumCooldownTicks = 300;

    // ==================== 突变词条 ====================
    /** 良性 : 畸变(双刃) 的良性占比（roll 时先滚良性池） */
    public static volatile double traitBenignRatio = 0.7;
    /** 词条稀有度 3 档纯度门槛（纯度 ≥ 该值可出稀有度 3 词条） */
    public static volatile int traitRarityHighThreshold = 85;
    /** 词条稀有度 2 档纯度门槛 */
    public static volatile int traitRarityMidThreshold = 60;

    // ==================== 培养机·品质升级（override：0 = 用内置 UPGRADE_TIERS） ====================
    /** I→II / II→III / III→IV 成功率（百分比） */
    public static volatile int[] cultivatorUpgradeSuccess = {0, 0, 0};
    /** 三段升级生命能量消耗 */
    public static volatile int[] cultivatorUpgradeEnergy = {0, 0, 0};
    /** 三段升级固态物消耗 */
    public static volatile int[] cultivatorUpgradeSolid = {0, 0, 0};
    /** 三段升级耗时（tick） */
    public static volatile int[] cultivatorUpgradeTicks = {0, 0, 0};
    /** 升级成功额外适配加成 override（0 = 用内置 +8） */
    public static volatile int cultivatorUpgradeCompatBonus = 0;

    // ==================== 机器全局倍率 ====================
    /** 全部可升级加工机器的工作速度倍率（含速度升级，1.0 = 不变） */
    public static volatile double machineWorkSpeed = 1.0;
    /** 持续耗能机器的运行能耗倍率（1.0 = 不变） */
    public static volatile double machineCostMultiplier = 1.0;

    // ==================== 机制开关 ====================
    /** 衰竭区域生成（管道/桶/反应堆泄漏等触发） */
    public static volatile boolean decayZoneEnabled = true;
    /** 日光自燃负面被动（亡灵速腿/幻翼肺的代价） */
    public static volatile boolean sunlightBurnEnabled = true;
    /** 躯体超载 debuff（全身总排斥预算惩罚） */
    public static volatile boolean overloadEnabled = true;

    // ==================== 生命研究机器 ====================
    /** 基因分析仪：解构一次消耗的生命能量 */
    public static volatile long geneAnalyzerLifeCost = 5_000L;
    /** 基因分析仪：生命能量缓冲容量 */
    public static volatile long geneAnalyzerLifeCapacity = 10_000L;
    /** 基因分析仪：解构耗时（tick） */
    public static volatile int geneAnalyzerProcessTicks = 100;
    /** 基因分析仪：最低成功率（纯度 25） */
    public static volatile double geneAnalyzerMinSuccessRate = 0.70;
    /** 基因分析仪：最高成功率（纯度 100） */
    public static volatile double geneAnalyzerMaxSuccessRate = 0.95;
    /** 生命结构台：构造一次消耗的生命能量 */
    public static volatile long lifeStructLifeCost = 80_000L;
    /** 生命结构台：构造一次消耗的固态生命精华 */
    public static volatile int lifeStructSolidCost = 5;
    /** 生命结构台：生命能量缓冲容量 */
    public static volatile long lifeStructLifeCapacity = 160_000L;
    /** 生命结构台：构造耗时（tick） */
    public static volatile int lifeStructProcessTicks = 120;
    /** 生命培育器：培育一次消耗的生命能量 */
    public static volatile long lifeBreederLifeCost = 60_000L;
    /** 生命培育器：培育一次消耗的赤水晶 */
    public static volatile int lifeBreederCrystalCost = 2;
    /** 生命培育器：生命能量缓冲容量 */
    public static volatile long lifeBreederLifeCapacity = 120_000L;
    /** 生命培育器：培育耗时（tick） */
    public static volatile int lifeBreederProcessTicks = 1000;
    /** 生命培育器：最低成功率 */
    public static volatile double lifeBreederMinSuccessRate = 0.35;
    /** 生命培育器：最高成功率 */
    public static volatile double lifeBreederMaxSuccessRate = 0.70;
    /** 词条重铸仪：重铸一次消耗的生命能量 */
    public static volatile long traitReforgerLifeCost = 120_000L;
    /** 词条重铸仪：生命能量缓冲容量 */
    public static volatile long traitReforgerLifeCapacity = 240_000L;
    /** 词条重铸仪：重铸耗时（tick） */
    public static volatile int traitReforgerProcessTicks = 600;
    /** 词条重铸仪：每级稀有度消耗的赤水晶 */
    public static volatile int traitReforgerCrystalPerRarity = 2;
    /** 转基因工厂：加工一次消耗的生命能量 */
    public static volatile long transgeneFactoryLifeCost = 5_000L;
    /** 转基因工厂：生命能量缓冲容量 */
    public static volatile long transgeneFactoryLifeCapacity = 10_000L;
    /** 转基因工厂：加工耗时（tick） */
    public static volatile int transgeneFactoryProcessTicks = 100;
    /** 手术仓：移植消耗的固态生命精华 */
    public static volatile int surgeryImplantSolidCost = 3;
    /** 手术仓：移植消耗的生命能量 */
    public static volatile long surgeryImplantLifeCost = 20_000L;
    /** 手术仓：摘除消耗的固态生命精华 */
    public static volatile int surgeryExtractSolidCost = 1;
    /** 手术仓：摘除消耗的生命能量 */
    public static volatile long surgeryExtractLifeCost = 5_000L;
    /** 手术仓：生命能量缓冲容量 */
    public static volatile long surgeryLifeCapacity = 100_000L;
    /** 手术仓：单次手术耗时（tick） */
    public static volatile int surgeryProcessTicks = 80;
    /** 器官储藏库：生命能量缓冲容量 */
    public static volatile long organVaultLifeCapacity = 100_000L;
    /** 器官储藏库：每 tick 保育消耗（有器官时） */
    public static volatile long organVaultKeepCostPerTick = 1L;
    /** 药剂台：生命能量缓冲容量 */
    public static volatile long potionTableLifeCapacity = 100_000L;

    // ==================== 能量机器 ====================
    /** 能量加工机：每 tick 抽取赤能源上限 */
    public static volatile long energyProcessorChishiRate = 1_000_000L;
    /** 能量加工机：赤能源池容量 */
    public static volatile long energyProcessorChishiCapacity = 20_000_000L;
    /** 能量加工机：各液体罐容量（mb） */
    public static volatile long energyProcessorTankCapacity = 16_000L;
    /** 能量加工机：每次加工消耗的赤能源 */
    public static volatile long energyProcessorChishiCost = 5_000_000L;
    /** 能量液化器：每 tick 抽取赤能源上限 */
    public static volatile long energyLiquefierChishiRate = 1_000_000L;
    /** 能量液化器：赤能源池容量 */
    public static volatile long energyLiquefierChishiCapacity = 100_000_000L;
    /** 能量液化器：液体罐容量（mb） */
    public static volatile long energyLiquefierTankCapacity = 16_000L;
    /** 燃料混合器：每 tick 抽取赤能源上限 */
    public static volatile long fuelMixerChishiRate = 1_000_000L;
    /** 燃料混合器：赤能源池容量 */
    public static volatile long fuelMixerChishiCapacity = 100_000_000L;
    /** 燃料混合器：每次混合消耗的赤能源 */
    public static volatile long fuelMixerChishiCost = 2_000_000L;
    /** 燃料混合器：液体罐容量（mb） */
    public static volatile long fuelMixerTankCapacity = 16_000L;
    /** 燃料灌装机：液体罐容量（mb） */
    public static volatile long fuelCannerTankCapacity = 16_000L;
    /** 燃料灌装机：每 tick 灌装量（mb） */
    public static volatile long fuelCannerFillRate = 1_000L;
    /** 能量聚合器：每颗赤石粉聚合消耗的赤能源 */
    public static volatile long energyAggregatorEnergyPerIngot = 10_000_000L;
    /** 能量聚合器：晶洞升级一次消耗的赤能源 */
    public static volatile long energyAggregatorEnergyPerGeodeUpgrade = 10_000_000L;
    /** 能量聚合器：赤能源存储容量 */
    public static volatile long energyAggregatorEnergyCapacity = 200_000_000L;
    /** 能量发电机：每 tick 发电量 */
    public static volatile int energyGeneratorGenerateRate = 75;
    /** 能量组装机：每 tick 发电量 */
    public static volatile int energyAssemblyGenerateRate = 3375;
    /** 超级发电机核心：每 tick 发电量 */
    public static volatile int superGeneratorCoreGenerateRate = 15_000;
    /** 能量池：基础容量 */
    public static volatile long energyCellSerializerBaseCapacity = 1_000_000_000L;
    /** 升级工作台：每次升级消耗的赤能源 */
    public static volatile long upgradeStationEnergyPerUpgrade = 20_000_000L;
    /** 升级工作台：赤能源存储容量 */
    public static volatile long upgradeStationEnergyCapacity = 40_000_000L;
    /** 装备锻造台：每次锻造消耗的赤能源 */
    public static volatile long equipmentForgerEnergyPerForge = 50_000_000L;
    /** 装备锻造台：赤能源存储容量 */
    public static volatile long equipmentForgerEnergyCapacity = 100_000_000L;

    // ==================== 净化与矩阵 ====================
    /** 净化塔：每 tick 消耗的赤能源 */
    public static volatile int purifierEnergyPerTick = 5;
    /** 净化塔：燃料燃烧速率（每点产能 tick 数） */
    public static volatile int purifierBurnRate = 10;
    /** 净化塔：单次提纯消耗（生命能量） */
    public static volatile long purifierTotalCost = 500L;
    /** 净化塔：成型后每 tick 提纯量 */
    public static volatile long purifierRateFormed = 150L;
    /** 净化矩阵：单次提纯消耗（生命能量） */
    public static volatile long purifierMatrixTotalCost = 500L;
    /** 净化矩阵：成型后每 tick 提纯量 */
    public static volatile long purifierMatrixRateFormed = 150L;
    /** 生命净化机：每 tick 抽取赤能源上限 */
    public static volatile long lifePurifierChishiRate = 1_000_000L;
    /** 生命净化机：单次固化消耗的赤能源 */
    public static volatile long lifePurifierTotalCost = 10_000_000L;
    /** 生命净化机：单次固化消耗的生命能量 */
    public static volatile long lifePurifierLifeCost = 1_000L;
    /** 生命净化机：赤能源池容量 */
    public static volatile long lifePurifierChishiCapacity = 20_000_000L;
    /** 生命净化机：生命能量缓冲容量 */
    public static volatile long lifePurifierLifeCapacity = 5_000L;
    /** 生命矩阵：每 tick 转化次数 */
    public static volatile int lifeMatrixConversionsPerTick = 45;
    /** 生命矩阵：单次转化消耗的赤能源 */
    public static volatile long lifeMatrixConversionCost = 10_000_000L;
    /** 生命矩阵：赤能源池容量 */
    public static volatile long lifeMatrixChishiCapacity = 500_000_000L;
    /** 生命矩阵：生命能量缓冲容量 */
    public static volatile long lifeMatrixLifeCapacity = 5_000L;
    /** 生命转化架构【外接】：每 tick 转化次数 */
    public static volatile int lifeConversionConversionsPerTick = 45;
    /** 生命转化架构【外接】：赤能源池容量 */
    public static volatile long lifeConversionChishiCapacity = 500_000_000L;
    /** 生命转化架构【外接】：生命能量缓冲容量 */
    public static volatile long lifeConversionLifeCapacity = 5_000L;
    /** 生命聚合转化器：单次转化消耗的赤能源 */
    public static volatile long lifeAggregationConversionCost = 10_000_000L;
    /** 生命聚合转化器：单次转化产出的生命能量 */
    public static volatile long lifeAggregationConversionOutput = 10L;
    /** 生命聚合转化器：赤能源池容量 */
    public static volatile long lifeAggregationChishiCapacity = 100_000_000L;
    /** 生命聚合转化器：生命能量缓冲容量 */
    public static volatile long lifeAggregationLifeCapacity = 100L;

    // ==================== 端口与电池缓冲 ====================
    /** 生命矩阵能量输入口缓冲容量 */
    public static volatile long lifeMatrixInputPortBufferCapacity = 100_000_000L;
    /** 生命矩阵能量输出口缓冲容量 */
    public static volatile long lifeMatrixOutputPortBufferCapacity = 5_000L;
    /** 净化矩阵能量输入口缓冲容量 */
    public static volatile long purifierEnergyInputPortBufferCapacity = 1_000_000L;
    /** 矿机物品输出口缓冲（物品格数语义不变，仅能量/液体缓冲类） */
    public static volatile long minerPortBufferCapacity = 10_000_000L;
    /** 矿机能量输入口缓冲容量 */
    public static volatile long minerEnergyInputBufferCapacity = 10_000_000L;
    /** 无线能量输入口缓冲容量 */
    public static volatile long wirelessInputPortBufferCapacity = 100_000_000L;
    /** 无线能量输出口缓冲容量 */
    public static volatile long wirelessOutputPortBufferCapacity = 100_000_000L;
    /** 生命矩阵结构【外接】能量输出口缓冲容量 */
    public static volatile long genEnergyOutputPortBufferCapacity = 100_000_000L;
    /** 聚变能量输出口缓冲容量 */
    public static volatile long fusionEnergyOutputBufferCapacity = 20_000_000_000L;
    /** 反应堆能量输出口缓冲容量 */
    public static volatile long reactorEnergyOutputBufferCapacity = 5_000_000_000L;
    /** 生命能量电池：容量 */
    public static volatile long lifeEnergyCellLifeCapacity = 1_000_000L;
    /** 等离子储罐：容量（mb） */
    public static volatile long plasmaTankCapacity = 16_000L;

    // ==================== 培养机提纯与分馏机 ====================
    /** 培养机：生命能量缓冲容量 */
    public static volatile long cultivatorLifeCapacity = 500_000L;
    /** 培养机：提纯成功率（%）按纯度区间 [0, 25, 50, 75]；0 = 用内置默认 */
    public static volatile int[] cultivatorPurifySuccess = {0, 0, 0, 0};
    /** 培养机：提纯生命能量消耗按纯度区间 [0, 25, 50, 75]；0 = 用内置默认 */
    public static volatile long[] cultivatorPurifyEnergy = {0, 0, 0, 0};
    /** 培养机：提纯固态生命精华消耗按纯度区间 [0, 25, 50, 75]；0 = 用内置默认 */
    public static volatile int[] cultivatorPurifySolid = {0, 0, 0, 0};
    /** 培养机：提纯耗时 (tick) 按纯度区间 [0, 25, 50, 75]；0 = 用内置默认 */
    public static volatile int[] cultivatorPurifyTicks = {0, 0, 0, 0};
    /** 培养机：单次提纯增加的纯度 */
    public static volatile int cultivatorPurifyGain = 10;
}
