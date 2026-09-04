package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.energy.EnergyCellTier;
import com.example.akaishi.energy.EnergyPipeTier;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.fluid.FluidTankTier;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 赤石矿石块注册表。
 * 循环注册 16 个方块（4 浓度 × 4 环境）及其 BlockItem。
 */
public final class ModBlocks {

    /** 全部 16 个矿石组合定义 */
    public static final List<AkaishiOreDef> ALL_ORES = buildAllOres();

    /** 组合定义 → 方块延迟注册引用 */
    private static final Map<AkaishiOreDef, RegistrySupplier<Block>> BLOCK_BY_DEF = new ConcurrentHashMap<>();

    /** 粗制赤石块（9 赤石晶合成，提纯器原料） */
    public static RegistrySupplier<Block> RAW_CHISHI_BLOCK;
    /** 赤石提纯器（消耗赤能源提纯粗制块/水晶块，可作提纯矩阵中心） */
    public static RegistrySupplier<Block> CHISHI_PURIFIER;
    /** 高级提纯构建方块（单方块直接消耗赤能源提纯，提纯矩阵 3×3×3 外壳） */
    public static RegistrySupplier<Block> CHISHI_ADVANCED_PURIFIER;
    /** 赤能源储存单元（基础级） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_CELL_BASIC;
    /** 赤能源储存单元（高级） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_CELL_ADVANCED;
    /** 赤能源储存单元（超级） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_CELL_SUPER;
    /** 赤能源管道（基础，能量网络中继） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_PIPE;
    /** 赤能源管道（高级） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_PIPE_ADVANCED;
    /** 赤能源管道（精英） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_PIPE_ELITE;
    /** 赤能源管道（终极） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_PIPE_ULTIMATE;
    /** 浓缩赤石精华块（9 个浓缩精华压缩，装饰与储备用） */
    public static RegistrySupplier<Block> CHISHI_ESSENCE_BLOCK;
    /** 赤能源发生机（燃烧赤石材料产赤能源，单方块 / 多方块外壳） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_GENERATOR;
    /** 小型赤能源组合结构（多方块主方块） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_ASSEMBLY;
    /** 赤能源储存串联器（多方块主方块） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_CELL_SERIALIZER;
    /** 超级发生器架构核心（5×5×5 多方块主方块） */
    public static RegistrySupplier<Block> CHISHI_SUPER_GENERATOR_CORE;
    /** 生命能量管道（传输生命能量类型） */
    public static RegistrySupplier<Block> CHISHI_LIFE_ENERGY_PIPE;
    /** 生命聚合转换器（消耗赤能源聚合生命能量，单方块 / 生命转换架构外壳） */
    public static RegistrySupplier<Block> CHISHI_LIFE_AGGREGATION_CONVERTER;
    /** 生命转换架构（3×3×3 多方块主方块） */
    public static RegistrySupplier<Block> CHISHI_LIFE_CONVERSION_ARCHITECTURE;
    /** 生命能量储存器（纯生命能量存储） */
    public static RegistrySupplier<Block> CHISHI_LIFE_ENERGY_CELL;
    /** 赤石能量聚合器（赤能源 + 下界合金锭 → 赤石锭） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_AGGREGATOR;
    /** 赤石装备打造器（赤能源 + 赤石锭 → 赤石装备） */
    public static RegistrySupplier<Block> CHISHI_EQUIPMENT_FORGER;
    /** 赤红升级台（模板 + 槽位 → 升级赤石装备） */
    public static RegistrySupplier<Block> CHISHI_UPGRADE_STATION;
    /** 生命的融合砧（赤石护甲 + 生命的融合锭 → 生命融合护甲） */
    public static RegistrySupplier<Block> CHISHI_LIFE_FUSION_ANVIL;
    /** 创造赤能源储存原件（无限输出测试方块） */
    public static RegistrySupplier<Block> CHISHI_CREATIVE_ENERGY_CELL;
    /** 创造生命能量储存原件（无限输出测试方块） */
    public static RegistrySupplier<Block> CHISHI_CREATIVE_LIFE_CELL;
    /** 赤石水晶母岩（瑕疵）：晶洞外层自然生成，可生长水晶簇，可在聚合器升级 */
    public static RegistrySupplier<Block> CHISHI_GEODE_FLAWED;
    /** 赤石水晶母岩（普通） */
    public static RegistrySupplier<Block> CHISHI_GEODE_NORMAL;
    /** 赤石水晶母岩（完好） */
    public static RegistrySupplier<Block> CHISHI_GEODE_PRISTINE;
    /** 赤石水晶母岩（完美） */
    public static RegistrySupplier<Block> CHISHI_GEODE_PERFECT;
    /** 赤石水晶簇：母岩生长/晶洞生成，破坏掉落赤石精华 */
    public static RegistrySupplier<Block> CHISHI_CRYSTAL_CLUSTER;
    /** 赤石水晶块：9 簇合成，提纯器提纯成精华 */
    public static RegistrySupplier<Block> CHISHI_CRYSTAL_BLOCK;
    /** 赤石催化器（初级）：催生范围内母岩生长水晶簇 */
    public static RegistrySupplier<Block> CHISHI_CATALYST_BASIC;
    /** 赤石催化器（中级） */
    public static RegistrySupplier<Block> CHISHI_CATALYST_MEDIUM;
    /** 赤石催化器（高级） */
    public static RegistrySupplier<Block> CHISHI_CATALYST_ADVANCED;
    /** 赤石催化器（终极） */
    public static RegistrySupplier<Block> CHISHI_CATALYST_ULTIMATE;
    /** 自动收集器（初级）：自动收获范围内水晶簇 */
    public static RegistrySupplier<Block> CHISHI_COLLECTOR_BASIC;
    /** 自动收集器（中级） */
    public static RegistrySupplier<Block> CHISHI_COLLECTOR_MEDIUM;
    /** 自动收集器（高级） */
    public static RegistrySupplier<Block> CHISHI_COLLECTOR_ADVANCED;
    /** 自动收集器（终极） */
    public static RegistrySupplier<Block> CHISHI_COLLECTOR_ULTIMATE;
    /** 物品管道（基础）：物流网络中继，1 个/tick */
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE;
    /** 物品管道（高级） */
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE_ADVANCED;
    /** 物品管道（精英） */
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE_ELITE;
    /** 物品管道（终极）：64 个/tick */
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE_ULTIMATE;
    /** 生命能量提纯器（赤能源驱动，1000 生命能量 + 10M 赤能源 → 1 生命能量固态物） */
    public static RegistrySupplier<Block> CHISHI_LIFE_PURIFIER;
    /** 液体管道：传输下界能量/燃料液体，可对接 MEK 等外部液体方块 */
    public static RegistrySupplier<Block> CHISHI_FLUID_PIPE;
    /** 封闭性衰竭管道：废料专用（单缓冲） */
    public static RegistrySupplier<Block> CHISHI_EXHAUSTED_PIPE;
    /** 多流体废料管道：废料专用（多缓冲） */
    public static RegistrySupplier<Block> CHISHI_MULTI_FLUID_WASTE_PIPE;
    /** 能量液化装置（赤能源驱动，下界之星/凋零玫瑰 → 下界能量液体） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_LIQUEFIER;
    /** 能量加工器（赤能源驱动，生命固态物 + 下界能量液体 → 反应堆燃料） */
    public static RegistrySupplier<Block> CHISHI_ENERGY_PROCESSOR;
    /** 燃料装罐机：液体燃料灌装进 10L 燃料罐 */
    public static RegistrySupplier<Block> CHISHI_FUEL_CANNER;
    /** 燃料混合器：两种燃料液体 1:1:1 调和为高阶混合燃料 */
    public static RegistrySupplier<Block> CHISHI_FUEL_MIXER;
    /** 生命活化器：消耗生命能量缓慢无害化衰竭燃料（废料进、活化液出） */
    public static RegistrySupplier<Block> CHISHI_LIFE_ACTIVATOR;
    /** 生命离心机：分离活化燃料为活化结晶 + 衰竭结晶 */
    public static RegistrySupplier<Block> CHISHI_LIFE_CENTRIFUGE;
    /** 物品重构仪：以衰竭结晶为代价嬗变物品 */
    public static RegistrySupplier<Block> CHISHI_ITEM_RECONSTRUCTOR;
    /** 赤石植物培养机：消耗赤能源培养植物（种子保留） */
    public static RegistrySupplier<Block> CHISHI_PLANT_CULTIVATOR;
    /** 赤石压缩机：粉末 → 块、赤石精华 → 浓缩赤石精华 */
    public static RegistrySupplier<Block> CHISHI_COMPRESSOR;
    /** 赤石打粉机：矿物/赤石/黑曜石 → 粉末 */
    public static RegistrySupplier<Block> CHISHI_PULVERIZER;
    /** 赤石变化器：青金石粉 → 冷却基底、矿物 → 矿石基底 */
    public static RegistrySupplier<Block> CHISHI_TRANSFORMER;
    /** 赤石矿机控制器（基础级，9×9×3 多方块主方块） */
    public static RegistrySupplier<Block> CHISHI_MINER_CONTROLLER_BASIC;
    /** 赤石矿机控制器（进阶级） */
    public static RegistrySupplier<Block> CHISHI_MINER_CONTROLLER_ADVANCED;
    /** 赤石矿机控制器（高级级） */
    public static RegistrySupplier<Block> CHISHI_MINER_CONTROLLER_SUPER;
    /** 赤石矿机控制器（终极级） */
    public static RegistrySupplier<Block> CHISHI_MINER_CONTROLLER_ULTIMATE;
    /** 矿机架构（结构框架方块） */
    public static RegistrySupplier<Block> CHISHI_MINER_FRAME;
    /** 矿机架构【矿机升级】（升级模块的安装位置，纯结构件） */
    public static RegistrySupplier<Block> CHISHI_MINER_UPGRADE_FRAME;
    /** 矿机转口（产物输出 + 赤能源输入） */
    public static RegistrySupplier<Block> CHISHI_MINER_PORT;
    /** 矿机速度升级模块（方块形式，安装于升级框架位置） */
    public static RegistrySupplier<Block> CHISHI_MINER_SPEED_UPGRADE_BLOCK;
    /** 矿机时运升级模块（方块形式，安装于升级框架位置） */
    public static RegistrySupplier<Block> CHISHI_MINER_FORTUNE_UPGRADE_BLOCK;
    /** 矿机储能升级模块（方块形式，安装于升级框架位置） */
    public static RegistrySupplier<Block> CHISHI_MINER_STORAGE_UPGRADE_BLOCK;
    /** 活化分馏器（活化结晶深度拆分：活化成分 + 衰竭结晶） */
    public static RegistrySupplier<Block> CHISHI_ACTIVATED_FRACTIONATOR;
    /** 聚变燃料聚合器（活化成分 → 等离子体） */
    public static RegistrySupplier<Block> CHISHI_FUSION_FUEL_AGGREGATOR;
    /** 离子体填装器（等离子体 + 反应棒 → 燃料棒） */
    public static RegistrySupplier<Block> CHISHI_PLASMA_FILLER;
    /** 等离子体管道（第三传输家族，仅传等离子体） */
    public static RegistrySupplier<Block> CHISHI_PLASMA_PIPE;
    /** 液体储罐（基础/高级/超级：16k/64k/256k mb，管道存取液体） */
    public static RegistrySupplier<Block> CHISHI_FLUID_TANK_BASIC;
    public static RegistrySupplier<Block> CHISHI_FLUID_TANK_ADVANCED;
    public static RegistrySupplier<Block> CHISHI_FLUID_TANK_SUPER;
    /** 等离子体燃料储罐（仅存储等离子体，仅等离子体管道可对接） */
    public static RegistrySupplier<Block> CHISHI_PLASMA_TANK;
    /** 反应堆外壳：多方块外壁（控制器/投放口/输出口/废品口也属外壁），右键打开控制器 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_SHELL;
    /** 反应堆结构玻璃：半透明观察窗，可替代反应堆外壳 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_STRUCTURE_GLASS;
    /** 反应堆控制器：主方块，持有全部反应堆状态，右键打开控制界面 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_CONTROLLER;
    /** 燃料投放口：燃料罐物品输入（管道+手动），自动分配到控制器空燃料槽 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_FUEL_PORT;
    /** 能量输出口：赤能源输出（纯发电，管道只可抽取） */
    public static RegistrySupplier<Block> CHISHI_REACTOR_ENERGY_OUTPUT;
    /** 废品输出口：衰竭燃料输出（液体管道只可抽取） */
    public static RegistrySupplier<Block> CHISHI_REACTOR_WASTE_PORT;
    /** 燃料棒组件：每根解锁 1 个燃料槽 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_FUEL_ROD;
    /** 散热组件：装入散热片，贴邻燃料棒才有效 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_COOLER;
    /** 反应核心：燃烧结算中心 */
    public static RegistrySupplier<Block> CHISHI_REACTOR_CORE;
    /** 衰竭保存桶：专储衰竭的生命燃料，带 GUI 液位 */
    public static RegistrySupplier<Block> CHISHI_EXHAUSTED_BARREL;
    /** 躯体检查仪：展示玩家躯体状态（9 槽位器官/肢体 + 排斥值） */
    public static RegistrySupplier<Block> CHISHI_BODY_SCANNER;
    /** 基因管理器：管理已吸收基因强化（最多 4 种来源，可卸载） */
    public static RegistrySupplier<Block> CHISHI_GENE_MANAGER;
    /** 生命分析台：纯度 100 样本解构为基因序列片段（有失败率） */
    public static RegistrySupplier<Block> CHISHI_GENE_ANALYZER;
    /** 部件培养舱：样本提纯 + 器官品质升级（双模式） */
    public static RegistrySupplier<Block> CHISHI_CULTIVATOR;
    /** 生命结构台：基因序列解析为指定槽位器官 */
    public static RegistrySupplier<Block> CHISHI_LIFE_STRUCT;
    /** 生命培育器：器官 + 同源序列 + 衰竭结晶 → 突变器官（成功率由纯度决定） */
    public static RegistrySupplier<Block> CHISHI_LIFE_BREEDER;
    /** 词条重铸仪：衰竭结晶 + 生命能量 → 原位替换指定第 N 条突变词条（确定性必成） */
    public static RegistrySupplier<Block> CHISHI_TRAIT_REFORGER;
    /** 手术仓：移植/摘除玩家躯体器官（消耗固态 + 生命能量，带进度） */
    public static RegistrySupplier<Block> CHISHI_SURGERY;
    /** 药剂台：样本（纯度 ≥25）+ 固态 + 生命能量 → 永久/突破药剂 */
    public static RegistrySupplier<Block> CHISHI_POTION_TABLE;
    /** 器官储藏库：按躯体槽位分页的器官仓库（生命能量维持活性） */
    public static RegistrySupplier<Block> CHISHI_ORGAN_VAULT;
    /** 药剂库：大容量药剂仓库（同 NBT 自动合并，按模板筛选） */
    public static RegistrySupplier<Block> CHISHI_POTION_CABINET;
    /** 样本库：大容量生命样本仓库（同 NBT 自动合并，机器联动存取） */
    public static RegistrySupplier<Block> CHISHI_SAMPLE_VAULT;
    /** 衰变净化塔：消耗赤能源净化范围内衰竭区域（加速区域消散） */
    public static RegistrySupplier<Block> CHISHI_DECAY_PURIFIER;
    /** 母神祭坛：生命线终局多方块祭坛核心（黑山羊之母，NBT 献祭识别 + 悬浮供奉） */
    public static RegistrySupplier<Block> CHISHI_MOTHER_ALTAR;
    /** 母神祭坛石：祭坛结构件（5×5 底座 + 四角柱，铺设成型后母神驻留） */
    public static RegistrySupplier<Block> CHISHI_ALTAR_STONE;
    /** 发生器矩阵外壳：类反应堆式矩阵外壁（端口可替代外壳） */
    public static RegistrySupplier<Block> CHISHI_GEN_MATRIX_CASING;
    /** 发生器矩阵结构玻璃：半透明观察窗，可替代发生器矩阵外壳 */
    public static RegistrySupplier<Block> CHISHI_GEN_MATRIX_STRUCTURE_GLASS;
    /** 发生器矩阵控制器（低级 3×3×3，45 倍，沿用组合结构数据） */
    public static RegistrySupplier<Block> CHISHI_GEN_MATRIX_CONTROLLER_BASIC;
    /** 发生器矩阵控制器（高级 5×5×5，200 倍，沿用超级架构数据） */
    public static RegistrySupplier<Block> CHISHI_GEN_MATRIX_CONTROLLER_ADVANCED;
    /** 发生器矩阵能量输出口（纯发电，仅管道抽取） */
    public static RegistrySupplier<Block> CHISHI_GEN_ENERGY_OUTPUT;
    /** 发生器矩阵燃料输入口（燃料物品输入，管道/漏斗） */
    public static RegistrySupplier<Block> CHISHI_GEN_FUEL_INPUT;
    /** 提纯矩阵外壳：类反应堆式矩阵外壁（端口可替代外壳） */
    public static RegistrySupplier<Block> CHISHI_PURIFIER_MATRIX_CASING;
    /** 提纯矩阵结构玻璃：半透明观察窗，可替代提纯矩阵外壳 */
    public static RegistrySupplier<Block> CHISHI_PURIFIER_MATRIX_STRUCTURE_GLASS;
    /** 提纯矩阵控制器：主方块，结构成型后集中提纯 */
    public static RegistrySupplier<Block> CHISHI_PURIFIER_MATRIX_CONTROLLER;
    /** 提纯矩阵能量输入口（赤能源输入，仅管道供能） */
    public static RegistrySupplier<Block> CHISHI_PURIFIER_ENERGY_INPUT;
    /** 提纯矩阵物品输入口（提纯原料输入，管道/漏斗） */
    public static RegistrySupplier<Block> CHISHI_PURIFIER_ITEM_INPUT;
    /** 提纯矩阵物品输出口（提纯产物输出，仅管道抽取） */
    public static RegistrySupplier<Block> CHISHI_PURIFIER_ITEM_OUTPUT;
    /** 生命转换矩阵外壳：类反应堆式矩阵外壁（端口可替代外壳） */
    public static RegistrySupplier<Block> CHISHI_LIFE_MATRIX_CASING;
    /** 生命转换矩阵结构玻璃：半透明观察窗，可替代生命转换矩阵外壳 */
    public static RegistrySupplier<Block> CHISHI_LIFE_MATRIX_STRUCTURE_GLASS;
    /** 生命转换矩阵控制器：主方块，结构成型后 45 倍集中转换 */
    public static RegistrySupplier<Block> CHISHI_LIFE_MATRIX_CONTROLLER;
    /** 生命转换矩阵能量输入口（赤能源输入，仅管道供能） */
    public static RegistrySupplier<Block> CHISHI_LIFE_MATRIX_ENERGY_INPUT;
    /** 生命转换矩阵能量输出口（生命能量输出，仅管道抽取） */
    public static RegistrySupplier<Block> CHISHI_LIFE_MATRIX_ENERGY_OUTPUT;
    /** 无线赤能源终端外壳：无线终端多方块（5×5×5）墙面填充方块 */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_SHELL;
    /** 无线终端结构玻璃：半透明观察窗，可替代无线赤能源终端外壳 */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_STRUCTURE_GLASS;
    /** 无线赤能源终端方块：外墙主方块（GUI 入口，成型后为网络能量中枢） */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_TERMINAL;
    /** 无线赤能源终端安全方块：外墙方块 + 安全卡认证页直达入口 */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_SECURITY;
    /** 无线赤能源终端核心：内腔中心方块（恰 1 个），拆掉结构即失效 */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_CORE;
    /** 无线赤能源控制器：外墙纯结构件（无 GUI 无 BE） */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_CONTROLLER;
    /** 无线赤能源输入口：能量管道 → 无线频道的发送端 */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_INPUT_PORT;
    /** 无线赤能源输出口：无线频道 → 能量管道的接收端 */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_OUTPUT_PORT;
    /** 终端跨维组件：内腔 ≥1 个解锁跨维度传输 */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_DIM_BRIDGE;
    /** 区块加载构架：内腔 ≥1 个使网络区块弱加载（离线运转） */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_CHUNK_LOADER;
    /** 区块加载扩展组件：内腔 ≥1 个使弱加载范围扩为 3×3 区块（终端与口） */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_CHUNK_RANGE;
    /** 输入损耗抑制组件：内腔每个降低输入口方向损耗（可叠加） */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_INPUT_LOSS;
    /** 输出损耗抑制组件：内腔每个降低输出口方向损耗（可叠加） */
    public static RegistrySupplier<Block> CHISHI_WIRELESS_OUTPUT_LOSS;
    /** 耐高温聚变外壳：聚变堆多方块外壁（控制器/能量输出口/物品输入口/物品输出口也属外壁） */
    public static RegistrySupplier<Block> CHISHI_FUSION_SHELL;
    /** 聚变结构玻璃：半透明观察窗，可替代耐高温聚变外壳（仅外壳层，隔热层不可替代） */
    public static RegistrySupplier<Block> CHISHI_FUSION_STRUCTURE_GLASS;
    /** 聚变隔热层：外壳与框架层之间的第二层，必须全部填充 */
    public static RegistrySupplier<Block> CHISHI_FUSION_INSULATION;
    /** 聚变控制器：主方块，持有全部聚变状态，右键打开三页界面 */
    public static RegistrySupplier<Block> CHISHI_FUSION_CONTROLLER;
    /** 聚变能量输出口：产出赤能源的墙面缓冲口（纯发电，管道只可抽取） */
    public static RegistrySupplier<Block> CHISHI_FUSION_ENERGY_OUTPUT;
    /** 聚变物品输入口：燃料棒投放口（管道/漏斗/手动投料） */
    public static RegistrySupplier<Block> CHISHI_FUSION_ITEM_INPUT;
    /** 聚变物品输出口：生命灰烬输出口（管道/手动收取） */
    public static RegistrySupplier<Block> CHISHI_FUSION_ITEM_OUTPUT;
    /** 聚变核心：结构中心方块（恰 1 个），拆掉结构即失效 */
    public static RegistrySupplier<Block> CHISHI_FUSION_CORE;
    /** 聚变散热框架：框架层纯结构件，为控制器解锁散热片槽（上限 10） */
    public static RegistrySupplier<Block> CHISHI_FUSION_COOLER_FRAME;
    /** 聚变燃料框架：框架层结构件，每框架解锁 1 个燃料槽（上限 4） */
    public static RegistrySupplier<Block> CHISHI_FUSION_FUEL_FRAME;
    /** 聚变效率框架：框架层结构件，每个使产率/产热 ×1.15（上限 12） */
    public static RegistrySupplier<Block> CHISHI_FUSION_EFFICIENCY_FRAME;

    private ModBlocks() {
    }

    public static void register() {
        for (AkaishiOreDef def : ALL_ORES) {
            ResourceLocation id = new ResourceLocation(AkaishiMod.MOD_ID, def.id());

            // 方块实例必须延迟到注册事件中创建：new Block 会创建侵入式 Holder，
            // 若在注册表冻结后执行将抛 "Registry is already frozen"
            Registrar<Block> blockRegistrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
            RegistrySupplier<Block> block = blockRegistrar.register(id, AkaishiOreBlock::new);
            // 方块物品一并注册，方便玩家在创造模式取用
            RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                    .register(id, () -> new BlockItem(block.get(), new Item.Properties()));

            BLOCK_BY_DEF.put(def, block);
        }

        // 粗制赤石块 + 赤石提纯器（含各自 BlockItem）
        Registrar<Block> blockRegistrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
        RAW_CHISHI_BLOCK = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "raw_akaishi_block"), AkaishiBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "raw_akaishi_block"),
                        () -> new BlockItem(RAW_CHISHI_BLOCK.get(), new Item.Properties()));

        CHISHI_PURIFIER = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_purifier"), AkaishiPurifierBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_purifier"),
                        () -> new BlockItem(CHISHI_PURIFIER.get(), new Item.Properties()));

        // 高级提纯构建方块：单方块直接消耗赤能源提纯，作为"提纯矩阵"（3×3×3）外壳
        CHISHI_ADVANCED_PURIFIER = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_advanced_purifier"), AkaishiAdvancedPurifierBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_advanced_purifier"),
                        () -> new BlockItem(CHISHI_ADVANCED_PURIFIER.get(), new Item.Properties()));

        // 赤能源储存单元（基础/高级/超级）+ 赤能源管道，均为含方块实体的参数化/独立方块
        CHISHI_ENERGY_CELL_BASIC = registerCell(blockRegistrar, "akaishi_energy_cell_basic", EnergyCellTier.BASIC);
        CHISHI_ENERGY_CELL_ADVANCED = registerCell(blockRegistrar, "akaishi_energy_cell_advanced", EnergyCellTier.ADVANCED);
        CHISHI_ENERGY_CELL_SUPER = registerCell(blockRegistrar, "akaishi_energy_cell_super", EnergyCellTier.SUPER);

        CHISHI_ENERGY_PIPE = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_pipe"),
                () -> new AkaishiEnergyPipeBlock(EnergyPipeTier.BASIC));
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_pipe"),
                        () -> new BlockItem(CHISHI_ENERGY_PIPE.get(), new Item.Properties()));

        CHISHI_ENERGY_PIPE_ADVANCED = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_pipe_advanced"),
                () -> new AkaishiEnergyPipeBlock(EnergyPipeTier.ADVANCED));
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_pipe_advanced"),
                        () -> new BlockItem(CHISHI_ENERGY_PIPE_ADVANCED.get(), new Item.Properties()));

        CHISHI_ENERGY_PIPE_ELITE = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_pipe_elite"),
                () -> new AkaishiEnergyPipeBlock(EnergyPipeTier.ELITE));
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_pipe_elite"),
                        () -> new BlockItem(CHISHI_ENERGY_PIPE_ELITE.get(), new Item.Properties()));

        CHISHI_ENERGY_PIPE_ULTIMATE = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_pipe_ultimate"),
                () -> new AkaishiEnergyPipeBlock(EnergyPipeTier.ULTIMATE));
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_pipe_ultimate"),
                        () -> new BlockItem(CHISHI_ENERGY_PIPE_ULTIMATE.get(), new Item.Properties()));

        // 浓缩赤石精华块（普通方块 + BlockItem）
        CHISHI_ESSENCE_BLOCK = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_essence_block"),
                () -> new Block(Block.Properties.of().strength(3.0F, 6.0F).requiresCorrectToolForDrops()));
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_essence_block"),
                        () -> new BlockItem(CHISHI_ESSENCE_BLOCK.get(), new Item.Properties()));

        // 赤能源发生机 + 小型赤能源组合结构（含各自 BlockItem）
        CHISHI_ENERGY_GENERATOR = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_generator"), AkaishiEnergyGeneratorBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_generator"),
                        () -> new BlockItem(CHISHI_ENERGY_GENERATOR.get(), new Item.Properties()));

        CHISHI_ENERGY_ASSEMBLY = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_assembly"), AkaishiEnergyAssemblyBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_assembly"),
                        () -> new BlockItem(CHISHI_ENERGY_ASSEMBLY.get(), new Item.Properties()));

        // 赤能源储存串联器（3×3×3 多方块主方块，26 个储存单元环绕成型）
        CHISHI_ENERGY_CELL_SERIALIZER = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_cell_serializer"),
                AkaishiEnergyCellSerializerBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_cell_serializer"),
                        () -> new BlockItem(CHISHI_ENERGY_CELL_SERIALIZER.get(), new Item.Properties()));

        // 超级发生器架构核心（5×5×5 多方块主方块，124 台发生机环绕成型）
        CHISHI_SUPER_GENERATOR_CORE = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_super_generator_core"),
                AkaishiSuperGeneratorCoreBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_super_generator_core"),
                        () -> new BlockItem(CHISHI_SUPER_GENERATOR_CORE.get(), new Item.Properties()));

        // 生命能量管道（独立能量类型，与赤能源管道互不连通）
        CHISHI_LIFE_ENERGY_PIPE = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_energy_pipe"),
                AkaishiLifeEnergyPipeBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_energy_pipe"),
                        () -> new BlockItem(CHISHI_LIFE_ENERGY_PIPE.get(), new Item.Properties()));

        // 生命聚合转换器（单方块独立转换 / 生命转换架构外壳）+ 生命转换架构（3×3×3 多方块主方块）
        CHISHI_LIFE_AGGREGATION_CONVERTER = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_aggregation_converter"),
                AkaishiLifeAggregationConverterBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_aggregation_converter"),
                        () -> new BlockItem(CHISHI_LIFE_AGGREGATION_CONVERTER.get(), new Item.Properties()));

        CHISHI_LIFE_CONVERSION_ARCHITECTURE = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_conversion_architecture"),
                AkaishiLifeConversionArchitectureBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_conversion_architecture"),
                        () -> new BlockItem(CHISHI_LIFE_CONVERSION_ARCHITECTURE.get(), new Item.Properties()));

        // 生命能量储存器（纯生命能量存储，单方块）
        CHISHI_LIFE_ENERGY_CELL = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_energy_cell"),
                AkaishiLifeEnergyCellBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_energy_cell"),
                        () -> new BlockItem(CHISHI_LIFE_ENERGY_CELL.get(), new Item.Properties()));

        // 赤石能量聚合器（10M 赤能源 + 下界合金锭 → 赤石锭）
        CHISHI_ENERGY_AGGREGATOR = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_aggregator"),
                AkaishiEnergyAggregatorBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_aggregator"),
                        () -> new BlockItem(CHISHI_ENERGY_AGGREGATOR.get(), new Item.Properties()));

        // 赤石装备打造器（赤能源 + 赤石锭 + 下界合金装备 → 赤石装备）
        CHISHI_EQUIPMENT_FORGER = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_equipment_forger"),
                AkaishiEquipmentForgerBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_equipment_forger"),
                        () -> new BlockItem(CHISHI_EQUIPMENT_FORGER.get(), new Item.Properties()));

        // 赤红升级台（模板 + 槽位 + 赤能源 → 升级赤石装备）
        CHISHI_UPGRADE_STATION = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_upgrade_station"),
                AkaishiUpgradeStationBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_upgrade_station"),
                        () -> new BlockItem(CHISHI_UPGRADE_STATION.get(), new Item.Properties()));

        // 生命的融合砧（赤石护甲 + 生命的融合锭 → 生命融合护甲，保留升级数据）
        CHISHI_LIFE_FUSION_ANVIL = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_fusion_anvil"),
                AkaishiLifeFusionAnvilBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_fusion_anvil"),
                        () -> new BlockItem(CHISHI_LIFE_FUSION_ANVIL.get(), new Item.Properties()));

        // 赤石水晶母岩（4 级）：晶洞外层自然生成，放置后生长水晶簇，聚合器可升级
        CHISHI_GEODE_FLAWED = registerGeode(blockRegistrar, "akaishi_geode_flawed", AkaishiGeodeBlock.GeodeTier.FLAWED, MapColor.COLOR_LIGHT_GRAY);
        CHISHI_GEODE_NORMAL = registerGeode(blockRegistrar, "akaishi_geode_normal", AkaishiGeodeBlock.GeodeTier.NORMAL, MapColor.COLOR_RED);
        CHISHI_GEODE_PRISTINE = registerGeode(blockRegistrar, "akaishi_geode_pristine", AkaishiGeodeBlock.GeodeTier.PRISTINE, MapColor.GOLD);
        CHISHI_GEODE_PERFECT = registerGeode(blockRegistrar, "akaishi_geode_perfect", AkaishiGeodeBlock.GeodeTier.PERFECT, MapColor.COLOR_PURPLE);

        // 赤石水晶簇（破坏掉落精华）
        CHISHI_CRYSTAL_CLUSTER = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_crystal_cluster"),
                AkaishiCrystalClusterBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_crystal_cluster"),
                        () -> new BlockItem(CHISHI_CRYSTAL_CLUSTER.get(), new Item.Properties()));

        // 赤石水晶块（提纯器提纯成精华）
        CHISHI_CRYSTAL_BLOCK = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_crystal_block"),
                AkaishiCrystalBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_crystal_block"),
                        () -> new BlockItem(CHISHI_CRYSTAL_BLOCK.get(), new Item.Properties()));

        // 赤石催化器（4 级）：催生范围内母岩生长水晶簇，消耗赤能源
        CHISHI_CATALYST_BASIC = registerCatalyst(blockRegistrar, "akaishi_catalyst_basic", AkaishiCatalystBlock.CatalystTier.BASIC);
        CHISHI_CATALYST_MEDIUM = registerCatalyst(blockRegistrar, "akaishi_catalyst_medium", AkaishiCatalystBlock.CatalystTier.MEDIUM);
        CHISHI_CATALYST_ADVANCED = registerCatalyst(blockRegistrar, "akaishi_catalyst_advanced", AkaishiCatalystBlock.CatalystTier.ADVANCED);
        CHISHI_CATALYST_ULTIMATE = registerCatalyst(blockRegistrar, "akaishi_catalyst_ultimate", AkaishiCatalystBlock.CatalystTier.ULTIMATE);

        // 自动收集器（4 级）：自动收获范围内水晶簇，精华存入内部 27 槽容器
        CHISHI_COLLECTOR_BASIC = registerCollector(blockRegistrar, "akaishi_collector_basic", AkaishiAutoCollectorBlock.CollectorTier.BASIC);
        CHISHI_COLLECTOR_MEDIUM = registerCollector(blockRegistrar, "akaishi_collector_medium", AkaishiAutoCollectorBlock.CollectorTier.MEDIUM);
        CHISHI_COLLECTOR_ADVANCED = registerCollector(blockRegistrar, "akaishi_collector_advanced", AkaishiAutoCollectorBlock.CollectorTier.ADVANCED);
        CHISHI_COLLECTOR_ULTIMATE = registerCollector(blockRegistrar, "akaishi_collector_ultimate", AkaishiAutoCollectorBlock.CollectorTier.ULTIMATE);

        // 物品管道（4 级）：物流网络中继，传输物品到相连容器/机器，终极 64 个/tick
        CHISHI_ITEM_PIPE = registerItemPipe(blockRegistrar, "akaishi_item_pipe", AkaishiItemPipeBlock.ItemPipeTier.BASIC);
        CHISHI_ITEM_PIPE_ADVANCED = registerItemPipe(blockRegistrar, "akaishi_item_pipe_advanced", AkaishiItemPipeBlock.ItemPipeTier.ADVANCED);
        CHISHI_ITEM_PIPE_ELITE = registerItemPipe(blockRegistrar, "akaishi_item_pipe_elite", AkaishiItemPipeBlock.ItemPipeTier.ELITE);
        CHISHI_ITEM_PIPE_ULTIMATE = registerItemPipe(blockRegistrar, "akaishi_item_pipe_ultimate", AkaishiItemPipeBlock.ItemPipeTier.ULTIMATE);

        // 生命能量提纯器（双能量输入：赤能源驱动 + 生命能量原料，输出生命能量固态物）
        CHISHI_LIFE_PURIFIER = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_purifier"),
                AkaishiLifePurifierBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_purifier"),
                        () -> new BlockItem(CHISHI_LIFE_PURIFIER.get(), new Item.Properties()));

        // 液体管道（单级，传输下界能量/燃料液体）
        CHISHI_FLUID_PIPE = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fluid_pipe"),
                AkaishiFluidPipeBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fluid_pipe"),
                        () -> new BlockItem(CHISHI_FLUID_PIPE.get(), new Item.Properties()));

        // 封闭性衰竭管道（废料专用，单缓冲；与普通液体管道网络隔离）
        CHISHI_EXHAUSTED_PIPE = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_exhausted_pipe"),
                AkaishiExhaustedPipeBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_exhausted_pipe"),
                        () -> new BlockItem(CHISHI_EXHAUSTED_PIPE.get(), new Item.Properties()));

        // 多流体废料管道（废料专用，多缓冲，多种废料可混输）
        CHISHI_MULTI_FLUID_WASTE_PIPE = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_multi_fluid_waste_pipe"),
                AkaishiMultiFluidWastePipeBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_multi_fluid_waste_pipe"),
                        () -> new BlockItem(CHISHI_MULTI_FLUID_WASTE_PIPE.get(), new Item.Properties()));

        // 能量液化装置（赤能源驱动，下界之星 → 至纯能量 / 凋零玫瑰 → 复合能量）
        CHISHI_ENERGY_LIQUEFIER = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_liquefier"),
                AkaishiEnergyLiquefierBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_liquefier"),
                        () -> new BlockItem(CHISHI_ENERGY_LIQUEFIER.get(), new Item.Properties()));

        // 能量加工器（赤能源驱动，生命固态物 + 下界能量液体 → 反应堆燃料）
        CHISHI_ENERGY_PROCESSOR = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_processor"),
                AkaishiEnergyProcessorBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_processor"),
                        () -> new BlockItem(CHISHI_ENERGY_PROCESSOR.get(), new Item.Properties()));

        // 燃料装罐机（液体燃料 → 10L 燃料罐）
        CHISHI_FUEL_CANNER = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fuel_canner"),
                AkaishiFuelCannerBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fuel_canner"),
                        () -> new BlockItem(CHISHI_FUEL_CANNER.get(), new Item.Properties()));

        // 燃料混合器（燃料液体 1:1:1 调和 → 高级/终极混合燃料）
        CHISHI_FUEL_MIXER = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fuel_mixer"),
                AkaishiFuelMixerBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fuel_mixer"),
                        () -> new BlockItem(CHISHI_FUEL_MIXER.get(), new Item.Properties()));

        // 生命活化器（生命能量无害化衰竭燃料：废料管道进、普通管道抽活化液）
        CHISHI_LIFE_ACTIVATOR = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_activator"),
                AkaishiLifeActivatorBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_activator"),
                        () -> new BlockItem(CHISHI_LIFE_ACTIVATOR.get(), new Item.Properties()));

        // 生命离心机（赤能源分离活化燃料：活化结晶 + 衰竭结晶）
        CHISHI_LIFE_CENTRIFUGE = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_centrifuge"),
                AkaishiLifeCentrifugeBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_centrifuge"),
                        () -> new BlockItem(CHISHI_LIFE_CENTRIFUGE.get(), new Item.Properties()));

        // 物品重构仪（以衰竭结晶为代价嬗变物品）
        CHISHI_ITEM_RECONSTRUCTOR = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_item_reconstructor"),
                AkaishiItemReconstructorBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_item_reconstructor"),
                        () -> new BlockItem(CHISHI_ITEM_RECONSTRUCTOR.get(), new Item.Properties()));

        // 赤石植物培养机（消耗赤能源培养植物，种子保留不消耗）
        CHISHI_PLANT_CULTIVATOR = registerReactorBlock(blockRegistrar, "akaishi_plant_cultivator", AkaishiPlantCultivatorBlock::new);
        // 赤石压缩机（粉末 → 块、赤石精华 → 浓缩赤石精华）
        CHISHI_COMPRESSOR = registerReactorBlock(blockRegistrar, "akaishi_compressor", AkaishiCompressorBlock::new);
        // 赤石打粉机（矿物/赤石/黑曜石 → 粉末）
        CHISHI_PULVERIZER = registerReactorBlock(blockRegistrar, "akaishi_pulverizer", AkaishiPulverizerBlock::new);
        // 赤石变化器（青金石粉 → 冷却基底、矿物 → 矿石基底）
        CHISHI_TRANSFORMER = registerReactorBlock(blockRegistrar, "akaishi_transformer", AkaishiTransformerBlock::new);

        // 赤石矿机体系：4 级控制器（等级由方块实例决定）+ 架构框架 + 升级框架 + 转口
        CHISHI_MINER_CONTROLLER_BASIC = registerReactorBlock(blockRegistrar, "akaishi_miner_controller_basic",
                () -> new AkaishiMinerControllerBlock(AkaishiMinerTier.BASIC));
        CHISHI_MINER_CONTROLLER_ADVANCED = registerReactorBlock(blockRegistrar, "akaishi_miner_controller_advanced",
                () -> new AkaishiMinerControllerBlock(AkaishiMinerTier.ADVANCED));
        CHISHI_MINER_CONTROLLER_SUPER = registerReactorBlock(blockRegistrar, "akaishi_miner_controller_super",
                () -> new AkaishiMinerControllerBlock(AkaishiMinerTier.SUPER));
        CHISHI_MINER_CONTROLLER_ULTIMATE = registerReactorBlock(blockRegistrar, "akaishi_miner_controller_ultimate",
                () -> new AkaishiMinerControllerBlock(AkaishiMinerTier.ULTIMATE));
        CHISHI_MINER_FRAME = registerReactorBlock(blockRegistrar, "akaishi_miner_frame", AkaishiMinerFrameBlock::new);
        CHISHI_MINER_UPGRADE_FRAME = registerReactorBlock(blockRegistrar, "akaishi_miner_upgrade_frame",
                AkaishiMinerUpgradeFrameBlock::new);
        CHISHI_MINER_PORT = registerReactorBlock(blockRegistrar, "akaishi_miner_port", AkaishiMinerPortBlock::new);
        // 矿机升级模块（方块形式，替换升级框架安装，上限：速度 10 / 时运 4 / 储能 10）
        CHISHI_MINER_SPEED_UPGRADE_BLOCK = registerReactorBlock(blockRegistrar, "akaishi_miner_speed_upgrade_block",
                () -> new AkaishiMinerUpgradeBlock(AkaishiMinerUpgradeType.SPEED));
        CHISHI_MINER_FORTUNE_UPGRADE_BLOCK = registerReactorBlock(blockRegistrar, "akaishi_miner_fortune_upgrade_block",
                () -> new AkaishiMinerUpgradeBlock(AkaishiMinerUpgradeType.FORTUNE));
        CHISHI_MINER_STORAGE_UPGRADE_BLOCK = registerReactorBlock(blockRegistrar, "akaishi_miner_storage_upgrade_block",
                () -> new AkaishiMinerUpgradeBlock(AkaishiMinerUpgradeType.STORAGE));

        // 活化分馏器（活化结晶深度拆分：活化成分 + 衰竭结晶）
        CHISHI_ACTIVATED_FRACTIONATOR = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_fractionator"),
                AkaishiActivatedFractionatorBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_fractionator"),
                        () -> new BlockItem(CHISHI_ACTIVATED_FRACTIONATOR.get(), new Item.Properties()));

        // 聚变燃料聚合器（活化成分 → 等离子体）
        CHISHI_FUSION_FUEL_AGGREGATOR = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fusion_fuel_aggregator"),
                AkaishiFusionFuelAggregatorBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fusion_fuel_aggregator"),
                        () -> new BlockItem(CHISHI_FUSION_FUEL_AGGREGATOR.get(), new Item.Properties()));

        // 离子体填装器（等离子体 + 反应棒 → 燃料棒）
        CHISHI_PLASMA_FILLER = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plasma_filler"),
                AkaishiPlasmaFillerBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plasma_filler"),
                        () -> new BlockItem(CHISHI_PLASMA_FILLER.get(), new Item.Properties()));

        // 等离子体管道（第三传输家族，仅传等离子体）
        CHISHI_PLASMA_PIPE = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plasma_pipe"),
                AkaishiPlasmaPipeBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plasma_pipe"),
                        () -> new BlockItem(CHISHI_PLASMA_PIPE.get(), new Item.Properties()));

        // 液体储罐（基础/高级/超级，容量递增，可被液体管道注入/抽取）
        CHISHI_FLUID_TANK_BASIC = registerFluidTank(blockRegistrar, "akaishi_fluid_tank_basic", FluidTankTier.BASIC);
        CHISHI_FLUID_TANK_ADVANCED = registerFluidTank(blockRegistrar, "akaishi_fluid_tank_advanced", FluidTankTier.ADVANCED);
        CHISHI_FLUID_TANK_SUPER = registerFluidTank(blockRegistrar, "akaishi_fluid_tank_super", FluidTankTier.SUPER);

        // 等离子体燃料储罐：仅存储等离子体（罐层拒收非等离子体液体）
        CHISHI_PLASMA_TANK = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plasma_tank"),
                AkaishiPlasmaTankBlock::new);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plasma_tank"),
                        () -> new BlockItem(CHISHI_PLASMA_TANK.get(), new Item.Properties()));

        // 创造模式能量源（测试用，无限输出）：赤能源 / 生命能量
        CHISHI_CREATIVE_ENERGY_CELL = blockRegistrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, "creative_akaishi_energy_cell"),
                () -> new CreativeEnergySourceBlock(AkaishiEnergyType.INSTANCE));
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "creative_akaishi_energy_cell"),
                        () -> new BlockItem(CHISHI_CREATIVE_ENERGY_CELL.get(), new Item.Properties()));
        CHISHI_CREATIVE_LIFE_CELL = blockRegistrar.register(
                new ResourceLocation(AkaishiMod.MOD_ID, "creative_life_energy_cell"),
                () -> new CreativeEnergySourceBlock(LifeEnergyType.INSTANCE));
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "creative_life_energy_cell"),
                        () -> new BlockItem(CHISHI_CREATIVE_LIFE_CELL.get(), new Item.Properties()));

        // ===== 反应堆体系（9 方块）=====
        CHISHI_REACTOR_SHELL = registerReactorBlock(blockRegistrar, "akaishi_reactor_shell", AkaishiReactorShellBlock::new);
        CHISHI_REACTOR_STRUCTURE_GLASS = registerReactorBlock(blockRegistrar, "akaishi_reactor_structure_glass", AkaishiStructureGlassBlock::new);
        CHISHI_REACTOR_CONTROLLER = registerReactorBlock(blockRegistrar, "akaishi_reactor_controller", AkaishiReactorControllerBlock::new);
        CHISHI_REACTOR_FUEL_PORT = registerReactorBlock(blockRegistrar, "akaishi_reactor_fuel_port", AkaishiReactorFuelPortBlock::new);
        CHISHI_REACTOR_ENERGY_OUTPUT = registerReactorBlock(blockRegistrar, "akaishi_reactor_energy_output", AkaishiReactorEnergyOutputBlock::new);
        CHISHI_REACTOR_WASTE_PORT = registerReactorBlock(blockRegistrar, "akaishi_reactor_waste_port", AkaishiReactorWastePortBlock::new);
        CHISHI_REACTOR_FUEL_ROD = registerReactorBlock(blockRegistrar, "akaishi_reactor_fuel_rod", AkaishiReactorFuelRodBlock::new);
        CHISHI_REACTOR_COOLER = registerReactorBlock(blockRegistrar, "akaishi_reactor_cooler", AkaishiReactorCoolerBlock::new);
        CHISHI_REACTOR_CORE = registerReactorBlock(blockRegistrar, "akaishi_reactor_core", AkaishiReactorCoreBlock::new);
        CHISHI_EXHAUSTED_BARREL = registerReactorBlock(blockRegistrar, "akaishi_exhausted_barrel", AkaishiExhaustedBarrelBlock::new);

        // ===== 生命科技 =====
        CHISHI_BODY_SCANNER = registerReactorBlock(blockRegistrar, "akaishi_body_scanner", AkaishiBodyScannerBlock::new);
        // 基因管理器（生命科技：已吸收基因强化管理）
        CHISHI_GENE_MANAGER = registerReactorBlock(blockRegistrar, "akaishi_gene_manager", AkaishiGeneManagerBlock::new);
        CHISHI_GENE_ANALYZER = registerReactorBlock(blockRegistrar, "akaishi_gene_analyzer", AkaishiGeneAnalyzerBlock::new);
        CHISHI_CULTIVATOR = registerReactorBlock(blockRegistrar, "akaishi_cultivator", AkaishiCultivatorBlock::new);
        CHISHI_LIFE_STRUCT = registerReactorBlock(blockRegistrar, "akaishi_life_struct", AkaishiLifeStructBlock::new);
        CHISHI_LIFE_BREEDER = registerReactorBlock(blockRegistrar, "akaishi_life_breeder", AkaishiLifeBreederBlock::new);
        CHISHI_TRAIT_REFORGER = registerReactorBlock(blockRegistrar, "akaishi_trait_reforger", AkaishiTraitReforgerBlock::new);
        CHISHI_SURGERY = registerReactorBlock(blockRegistrar, "akaishi_surgery", AkaishiSurgeryBlock::new);
        CHISHI_POTION_TABLE = registerReactorBlock(blockRegistrar, "akaishi_potion_table", AkaishiPotionTableBlock::new);
        CHISHI_ORGAN_VAULT = registerReactorBlock(blockRegistrar, "akaishi_organ_vault", AkaishiOrganVaultBlock::new);
        CHISHI_POTION_CABINET = registerReactorBlock(blockRegistrar, "akaishi_potion_cabinet", AkaishiPotionCabinetBlock::new);
        CHISHI_SAMPLE_VAULT = registerReactorBlock(blockRegistrar, "akaishi_sample_vault", AkaishiSampleVaultBlock::new);
        CHISHI_DECAY_PURIFIER = registerReactorBlock(blockRegistrar, "akaishi_decay_purifier", AkaishiDecayPurifierBlock::new);

        // ===== 母神祭坛（生命线终局多方块：黑山羊之母）=====
        CHISHI_MOTHER_ALTAR = registerReactorBlock(blockRegistrar, "akaishi_mother_altar", AkaishiMotherAltarBlock::new);
        // 母神祭坛石（结构件：5×5 底座 + 四角柱，铺设成型后母神驻留）
        CHISHI_ALTAR_STONE = registerReactorBlock(blockRegistrar, "akaishi_altar_stone", () -> new Block(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN)));

        // ===== 发生器矩阵（类反应堆式：立方体外壁成型，端口可替代外壳）=====
        CHISHI_GEN_MATRIX_CASING = registerReactorBlock(blockRegistrar, "akaishi_gen_matrix_casing", AkaishiGenMatrixCasingBlock::new);
        CHISHI_GEN_MATRIX_STRUCTURE_GLASS = registerReactorBlock(blockRegistrar, "akaishi_gen_matrix_structure_glass", AkaishiStructureGlassBlock::new);
        CHISHI_GEN_MATRIX_CONTROLLER_BASIC = registerGenMatrixController(
                blockRegistrar, "akaishi_gen_matrix_controller_basic", AkaishiGenMatrixTier.BASIC);
        CHISHI_GEN_MATRIX_CONTROLLER_ADVANCED = registerGenMatrixController(
                blockRegistrar, "akaishi_gen_matrix_controller_advanced", AkaishiGenMatrixTier.ADVANCED);
        CHISHI_GEN_ENERGY_OUTPUT = registerReactorBlock(blockRegistrar, "akaishi_gen_energy_output", AkaishiGenEnergyOutputPortBlock::new);
        CHISHI_GEN_FUEL_INPUT = registerReactorBlock(blockRegistrar, "akaishi_gen_fuel_input", AkaishiGenFuelInputPortBlock::new);

        // ===== 提纯矩阵（类反应堆式：立方体外壁成型，端口可替代外壳）=====
        CHISHI_PURIFIER_MATRIX_CASING = registerReactorBlock(blockRegistrar, "akaishi_purifier_matrix_casing", AkaishiPurifierMatrixCasingBlock::new);
        CHISHI_PURIFIER_MATRIX_STRUCTURE_GLASS = registerReactorBlock(blockRegistrar, "akaishi_purifier_matrix_structure_glass", AkaishiStructureGlassBlock::new);
        CHISHI_PURIFIER_MATRIX_CONTROLLER = registerReactorBlock(blockRegistrar, "akaishi_purifier_matrix_controller", AkaishiPurifierMatrixControllerBlock::new);
        CHISHI_PURIFIER_ENERGY_INPUT = registerReactorBlock(blockRegistrar, "akaishi_purifier_energy_input", AkaishiPurifierEnergyInputPortBlock::new);
        CHISHI_PURIFIER_ITEM_INPUT = registerReactorBlock(blockRegistrar, "akaishi_purifier_item_input", AkaishiPurifierItemInputPortBlock::new);
        CHISHI_PURIFIER_ITEM_OUTPUT = registerReactorBlock(blockRegistrar, "akaishi_purifier_item_output", AkaishiPurifierItemOutputPortBlock::new);

        // ===== 生命转换矩阵（类反应堆式：立方体外壁成型，端口可替代外壳）=====
        CHISHI_LIFE_MATRIX_CASING = registerReactorBlock(blockRegistrar, "akaishi_life_matrix_casing", AkaishiLifeMatrixCasingBlock::new);
        CHISHI_LIFE_MATRIX_STRUCTURE_GLASS = registerReactorBlock(blockRegistrar, "akaishi_life_matrix_structure_glass", AkaishiStructureGlassBlock::new);
        CHISHI_LIFE_MATRIX_CONTROLLER = registerReactorBlock(blockRegistrar, "akaishi_life_matrix_controller", AkaishiLifeMatrixControllerBlock::new);
        CHISHI_LIFE_MATRIX_ENERGY_INPUT = registerReactorBlock(blockRegistrar, "akaishi_life_matrix_energy_input", AkaishiLifeMatrixEnergyInputPortBlock::new);
        CHISHI_LIFE_MATRIX_ENERGY_OUTPUT = registerReactorBlock(blockRegistrar, "akaishi_life_matrix_energy_output", AkaishiLifeMatrixEnergyOutputPortBlock::new);

        // ===== 无线赤能源（无线终端多方块体系）=====
        CHISHI_WIRELESS_SHELL = registerReactorBlock(blockRegistrar, "akaishi_wireless_shell", AkaishiWirelessShellBlock::new);
        CHISHI_WIRELESS_STRUCTURE_GLASS = registerReactorBlock(blockRegistrar, "akaishi_wireless_structure_glass", AkaishiStructureGlassBlock::new);
        CHISHI_WIRELESS_TERMINAL = registerReactorBlock(blockRegistrar, "akaishi_wireless_terminal", AkaishiWirelessTerminalBlock::new);
        CHISHI_WIRELESS_SECURITY = registerReactorBlock(blockRegistrar, "akaishi_wireless_security", AkaishiWirelessSecurityBlock::new);
        CHISHI_WIRELESS_CORE = registerReactorBlock(blockRegistrar, "akaishi_wireless_core", AkaishiWirelessCoreBlock::new);
        CHISHI_WIRELESS_CONTROLLER = registerReactorBlock(blockRegistrar, "akaishi_wireless_controller", AkaishiWirelessControllerBlock::new);
        CHISHI_WIRELESS_INPUT_PORT = registerReactorBlock(blockRegistrar, "akaishi_wireless_input_port", AkaishiWirelessInputPortBlock::new);
        CHISHI_WIRELESS_OUTPUT_PORT = registerReactorBlock(blockRegistrar, "akaishi_wireless_output_port", AkaishiWirelessOutputPortBlock::new);
        CHISHI_WIRELESS_DIM_BRIDGE = registerReactorBlock(blockRegistrar, "akaishi_wireless_dim_bridge", AkaishiWirelessDimBridgeBlock::new);
        CHISHI_WIRELESS_CHUNK_LOADER = registerReactorBlock(blockRegistrar, "akaishi_wireless_chunk_loader", AkaishiWirelessChunkLoaderBlock::new);
        CHISHI_WIRELESS_CHUNK_RANGE = registerReactorBlock(blockRegistrar, "akaishi_wireless_chunk_range", AkaishiWirelessChunkRangeBlock::new);
        CHISHI_WIRELESS_INPUT_LOSS = registerReactorBlock(blockRegistrar, "akaishi_wireless_input_loss", AkaishiWirelessInputLossBlock::new);
        CHISHI_WIRELESS_OUTPUT_LOSS = registerReactorBlock(blockRegistrar, "akaishi_wireless_output_loss", AkaishiWirelessOutputLossBlock::new);

        // ===== 聚变堆（10 方块）=====
        CHISHI_FUSION_SHELL = registerReactorBlock(blockRegistrar, "akaishi_fusion_shell", AkaishiFusionShellBlock::new);
        CHISHI_FUSION_STRUCTURE_GLASS = registerReactorBlock(blockRegistrar, "akaishi_fusion_structure_glass", AkaishiStructureGlassBlock::new);
        CHISHI_FUSION_INSULATION = registerReactorBlock(blockRegistrar, "akaishi_fusion_insulation", AkaishiFusionInsulationBlock::new);
        CHISHI_FUSION_CONTROLLER = registerReactorBlock(blockRegistrar, "akaishi_fusion_controller", AkaishiFusionControllerBlock::new);
        CHISHI_FUSION_ENERGY_OUTPUT = registerReactorBlock(blockRegistrar, "akaishi_fusion_energy_output", AkaishiFusionEnergyOutputBlock::new);
        CHISHI_FUSION_ITEM_INPUT = registerReactorBlock(blockRegistrar, "akaishi_fusion_item_input", AkaishiFusionItemInputPortBlock::new);
        CHISHI_FUSION_ITEM_OUTPUT = registerReactorBlock(blockRegistrar, "akaishi_fusion_item_output", AkaishiFusionItemOutputPortBlock::new);
        CHISHI_FUSION_CORE = registerReactorBlock(blockRegistrar, "akaishi_fusion_core", AkaishiFusionCoreBlock::new);
        CHISHI_FUSION_COOLER_FRAME = registerReactorBlock(blockRegistrar, "akaishi_fusion_cooler_frame", AkaishiFusionCoolerFrameBlock::new);
        CHISHI_FUSION_FUEL_FRAME = registerReactorBlock(blockRegistrar, "akaishi_fusion_fuel_frame", AkaishiFusionFuelFrameBlock::new);
        CHISHI_FUSION_EFFICIENCY_FRAME = registerReactorBlock(blockRegistrar, "akaishi_fusion_efficiency_frame", AkaishiFusionEfficiencyFrameBlock::new);
    }

    /** 注册一个指定等级的赤能源储存单元及其 BlockItem */
    private static RegistrySupplier<Block> registerCell(Registrar<Block> blockRegistrar, String id, EnergyCellTier tier) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                () -> new AkaishiEnergyCellBlock(tier));
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 注册一个指定等级的液体储罐及其 BlockItem */
    private static RegistrySupplier<Block> registerFluidTank(Registrar<Block> blockRegistrar, String id, FluidTankTier tier) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                () -> new AkaishiFluidTankBlock(tier));
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 注册一个指定等级的赤石水晶母岩及其 BlockItem */
    private static RegistrySupplier<Block> registerGeode(Registrar<Block> blockRegistrar, String id,
                                                         AkaishiGeodeBlock.GeodeTier tier, MapColor color) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                () -> new AkaishiGeodeBlock(tier, color));
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 注册一个指定等级的赤石催化器及其 BlockItem */
    private static RegistrySupplier<Block> registerCatalyst(Registrar<Block> blockRegistrar, String id,
                                                            AkaishiCatalystBlock.CatalystTier tier) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                () -> new AkaishiCatalystBlock(tier));
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 注册一个指定等级的自动收集器及其 BlockItem */
    private static RegistrySupplier<Block> registerCollector(Registrar<Block> blockRegistrar, String id,
                                                             AkaishiAutoCollectorBlock.CollectorTier tier) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                () -> new AkaishiAutoCollectorBlock(tier));
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 注册一个指定等级的物品管道及其 BlockItem */
    private static RegistrySupplier<Block> registerItemPipe(Registrar<Block> blockRegistrar, String id,
                                                            AkaishiItemPipeBlock.ItemPipeTier tier) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                () -> new AkaishiItemPipeBlock(tier));
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 注册一个反应堆方块及其 BlockItem（无参数构造） */
    private static RegistrySupplier<Block> registerReactorBlock(Registrar<Block> blockRegistrar, String id,
                                                                java.util.function.Supplier<Block> factory) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 注册一个指定等级的发生器矩阵控制器及其 BlockItem（低级/高级共用方块类，等级由实例决定） */
    private static RegistrySupplier<Block> registerGenMatrixController(Registrar<Block> blockRegistrar, String id,
                                                                       AkaishiGenMatrixTier tier) {
        RegistrySupplier<Block> block = blockRegistrar.register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                () -> new AkaishiGenMatrixControllerBlock(tier));
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** 获取对应组合定义的方块（注册完成后可用） */
    public static Block get(AkaishiOreDef def) {
        return BLOCK_BY_DEF.get(def).get();
    }

    /** 生成 4 × 4 全部组合 */
    private static List<AkaishiOreDef> buildAllOres() {
        List<AkaishiOreDef> defs = new ArrayList<>(16);
        for (AkaishiOreEnvironment env : AkaishiOreEnvironment.values()) {
            for (AkaishiOreTier tier : AkaishiOreTier.values()) {
                defs.add(new AkaishiOreDef(tier, env));
            }
        }
        return List.copyOf(defs);
    }
}
