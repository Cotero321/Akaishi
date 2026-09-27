package com.example.akaishi.item;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.AkaishiCrystalBlocks;
import com.example.akaishi.block.AkaishiDecayBlocks;
import com.example.akaishi.block.AkaishiEnergyBlocks;
import com.example.akaishi.block.AkaishiOreDef;
import com.example.akaishi.block.AkaishiFusionBlocks;
import com.example.akaishi.block.AkaishiLifeBlocks;
import com.example.akaishi.block.AkaishiMinerBlocks;
import com.example.akaishi.block.AkaishiMiniMatrixBlocks;
import com.example.akaishi.block.AkaishiMiniatureBlocks;
import com.example.akaishi.block.AkaishiMatrixBlocks;
import com.example.akaishi.block.AkaishiReactorBlocks;
import com.example.akaishi.block.AkaishiItemTerminalBlocks;
import com.example.akaishi.block.AkaishiWirelessBlocks;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.block.AkaishiMechanicalBlocks;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 创造模式物品栏分类（P4 重排为 2 栏，含禁忌栏合计 3 栏）：
 * <ol>
 *   <li>{@link #MATERIALS_TAB_ID} 赤石-材料：所有非方块物品 + 矿石/自然方块白名单（帕秋莉手册引用该 id）</li>
 *   <li>{@link #MACHINERY_TAB_ID} 赤石机械：其余全部方块（机器与结构）</li>
 * </ol>
 * <p>原 4 栏（主栏 / 机械改造 / 生命科技 / 机器与结构）合并为上述 2 栏：两栏各自复用同一组
 * {@code add*Items} 策展方法一次，由类型谓词（非方块物品或白名单方块 → 材料；其余方块 → 机械）分流，
 * 两栏并集与原 4 栏并集逐项一致（不重不漏），入栏顺序仍为原有策展顺序。
 * <p>原第 5 栏「禁忌」随禁忌秘典一并迁往 {@code akaishi_forbidden}（未装禁忌包即无该栏）。
 */
public final class ModCreativeTabs {

    /** 材料栏 id（复用原主栏 id；帕秋莉手册 book.json 的 creative_tab 也引用该 id） */
    public static final String MATERIALS_TAB_ID = "akaishi";
    /** 机械栏 id（复用原机器与结构栏 id） */
    public static final String MACHINERY_TAB_ID = "akaishi_machines";

    private ModCreativeTabs() {
    }

    public static void register() {
        var tabs = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.CREATIVE_MODE_TAB);

        // 1. 材料栏：所有非方块物品 + 矿石/自然方块白名单（原 4 栏并集的「材料」子集）
        tabs.register(new ResourceLocation(AkaishiMod.MOD_ID, MATERIALS_TAB_ID),
                () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemGroup.akaishi.materials"))
                        .icon(() -> new ItemStack(ModBlocks.get(ModBlocks.ALL_ORES.get(0))))
                        .displayItems((params, output) -> fillMaterialItems(output))
                        .build());

        // 2. 机械栏：其余全部方块（机器与结构，原 4 栏并集的「机械」子集）
        tabs.register(new ResourceLocation(AkaishiMod.MOD_ID, MACHINERY_TAB_ID),
                () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
                        .title(Component.translatable("itemGroup.akaishi.machinery"))
                        .icon(() -> new ItemStack(AkaishiReactorBlocks.CHISHI_REACTOR_CONTROLLER.get()))
                        .displayItems((params, output) -> fillMachineryItems(output))
                        .build());
    }

    // ==================== 两栏分流：复用同一组策展方法，按类型谓词拆分 ====================

    /**
     * 材料栏内容：依次跑完原 4 栏的策展方法（顺序不变），仅保留「非方块物品」与「白名单矿石/自然方块」。
     * <p>与原 4 栏并集的关系：材料栏 ≡ {非方块物品} ∪ {白名单方块}，机械栏 ≡ {其余方块}，二者互斥且并集为全集。
     */
    private static void fillMaterialItems(CreativeModeTab.Output output) {
        Set<Item> natural = naturalOrOreBlocks();
        CreativeModeTab.Output filtered = new FilteredOutput(output, stack -> isMaterial(stack, natural));
        addMainItems(filtered);
        addMechanicalItems(filtered);
        addLifeItems(filtered);
        addMachinesItems(filtered);
    }

    /** 机械栏内容：同 {@link #fillMaterialItems} 的策展顺序，仅保留「非白名单方块」。 */
    private static void fillMachineryItems(CreativeModeTab.Output output) {
        Set<Item> natural = naturalOrOreBlocks();
        CreativeModeTab.Output filtered = new FilteredOutput(output, stack -> !isMaterial(stack, natural));
        addMainItems(filtered);
        addMechanicalItems(filtered);
        addLifeItems(filtered);
        addMachinesItems(filtered);
    }

    /** 材料栏判据：非方块物品一律入材料栏；方块只有命中矿石/自然方块白名单才入材料栏，其余归机械栏。 */
    private static boolean isMaterial(ItemStack stack, Set<Item> naturalBlocks) {
        Item item = stack.getItem();
        return !(item instanceof BlockItem) || naturalBlocks.contains(item);
    }

    /**
     * 矿石/自然方块白名单（显式列出，不用名字匹配）：4 环境赤石矿簇、粗制赤石块、赤石精华块、
     * 4 级晶洞母岩、赤石水晶簇、赤石水晶块。其余方块（催化器/收集器/机器/结构件）一律归机械栏。
     */
    private static Set<Item> naturalOrOreBlocks() {
        Set<Item> set = new HashSet<>();
        for (AkaishiOreDef def : ModBlocks.ALL_ORES) {
            addBlockItem(set, ModBlocks.get(def));
        }
        addBlockItem(set, ModBlocks.RAW_CHISHI_BLOCK);
        addBlockItem(set, ModBlocks.CHISHI_ESSENCE_BLOCK);
        addBlockItem(set, AkaishiCrystalBlocks.CHISHI_GEODE_FLAWED);
        addBlockItem(set, AkaishiCrystalBlocks.CHISHI_GEODE_NORMAL);
        addBlockItem(set, AkaishiCrystalBlocks.CHISHI_GEODE_PRISTINE);
        addBlockItem(set, AkaishiCrystalBlocks.CHISHI_GEODE_PERFECT);
        addBlockItem(set, AkaishiCrystalBlocks.CHISHI_CRYSTAL_CLUSTER);
        addBlockItem(set, AkaishiCrystalBlocks.CHISHI_CRYSTAL_BLOCK);
        return set;
    }

    private static void addBlockItem(Set<Item> set, Block block) {
        if (block != null) {
            set.add(block.asItem());
        }
    }

    private static void addBlockItem(Set<Item> set, RegistrySupplier<? extends Block> supplier) {
        if (supplier != null) {
            addBlockItem(set, supplier.get());
        }
    }

    /** 类型过滤的 Output 包装器：仅把满足谓词的 ItemStack 转发给真实 output，其余丢弃。 */
    private static final class FilteredOutput implements CreativeModeTab.Output {
        private final CreativeModeTab.Output delegate;
        private final Predicate<ItemStack> keep;

        private FilteredOutput(CreativeModeTab.Output delegate, Predicate<ItemStack> keep) {
            this.delegate = delegate;
            this.keep = keep;
        }

        @Override
        public void accept(ItemStack stack, CreativeModeTab.TabVisibility visibility) {
            if (keep.test(stack)) {
                delegate.accept(stack, visibility);
            }
        }
    }

    // ==================== 栏 1：主栏（通用材料 / 装备 / 工具 / 采集体系） ====================

    private static void addMainItems(CreativeModeTab.Output output) {
        // 赤石矿簇方块（4 个环境 × 中浓度）
        for (AkaishiOreDef def : ModBlocks.ALL_ORES) {
            output.accept(new ItemStack(ModBlocks.get(def)));
        }
        // 赤石晶
        accept(output, ModItems.akaishiCrystal);
        // 粗制赤石块
        accept(output, ModBlocks.RAW_CHISHI_BLOCK);
        // 赤石精华 + 浓缩精华 + 精华块
        accept(output, ModItems.akaishiEssence);
        accept(output, ModItems.akaishiEssenceCompressed);
        accept(output, ModBlocks.CHISHI_ESSENCE_BLOCK);
        // 赤石锭 + 升级模板
        accept(output, ModItems.akaishiIngot);
        accept(output, ModItems.akaishiUpgradeTemplate);
        // 通用机器组件
        accept(output, ModItems.akaishiMachineComponent);
        accept(output, ModItems.akaishiAdvancedComponent);
        // 升级组件（能源产生/机器通用）
        accept(output, ModItems.akaishiSpeedUpgrade);
        accept(output, ModItems.machineSpeedUpgrade);
        accept(output, ModItems.machineEnergyUpgrade);
        accept(output, ModItems.machineWirelessReceiverUpgrade);
        // 调试工具
        accept(output, ModItems.akaishiDebugTool);
        // 赤石装备（头盔/胸甲/护腿/靴子/剑/镐/铲/斧）
        accept(output, ModItems.akaishiHelmet);
        accept(output, ModItems.akaishiChestplate);
        accept(output, ModItems.akaishiLeggings);
        accept(output, ModItems.akaishiBoots);
        accept(output, ModItems.akaishiSword);
        accept(output, ModItems.akaishiPickaxe);
        accept(output, ModItems.akaishiShovel);
        accept(output, ModItems.akaishiAxe);
        // 便捷赤能源储存单元（初级/中级/高级）
        accept(output, ModItems.portableCellBasic);
        accept(output, ModItems.portableCellAdvanced);
        accept(output, ModItems.portableCellSuper);
        // 赤石水晶体系：4 级母岩 + 水晶簇 + 水晶块
        accept(output, AkaishiCrystalBlocks.CHISHI_GEODE_FLAWED);
        accept(output, AkaishiCrystalBlocks.CHISHI_GEODE_NORMAL);
        accept(output, AkaishiCrystalBlocks.CHISHI_GEODE_PRISTINE);
        accept(output, AkaishiCrystalBlocks.CHISHI_GEODE_PERFECT);
        accept(output, AkaishiCrystalBlocks.CHISHI_CRYSTAL_CLUSTER);
        accept(output, AkaishiCrystalBlocks.CHISHI_CRYSTAL_BLOCK);
        // 赤石催化器（4 级）
        accept(output, AkaishiCrystalBlocks.CHISHI_CATALYST_BASIC);
        accept(output, AkaishiCrystalBlocks.CHISHI_CATALYST_MEDIUM);
        accept(output, AkaishiCrystalBlocks.CHISHI_CATALYST_ADVANCED);
        accept(output, AkaishiCrystalBlocks.CHISHI_CATALYST_ULTIMATE);
        // 自动收集器（4 级）
        accept(output, AkaishiCrystalBlocks.CHISHI_COLLECTOR_BASIC);
        accept(output, AkaishiCrystalBlocks.CHISHI_COLLECTOR_MEDIUM);
        accept(output, AkaishiCrystalBlocks.CHISHI_COLLECTOR_ADVANCED);
        accept(output, AkaishiCrystalBlocks.CHISHI_COLLECTOR_ULTIMATE);
        // 赤石饰品（Curios）
        accept(output, ModItems.satiationCharm);
        accept(output, ModItems.huntingRing);
        accept(output, ModItems.gatheringBracelet);
        accept(output, ModItems.fireNecklace);
        accept(output, ModItems.blastCharm);
        accept(output, ModItems.antidoteBracelet);
        accept(output, ModItems.witherCharm);
        // 散热片（5 品质 + 终极）
        accept(output, ModItems.heatSinkPoor);
        accept(output, ModItems.heatSinkNormal);
        accept(output, ModItems.heatSinkGood);
        accept(output, ModItems.heatSinkFine);
        accept(output, ModItems.heatSinkExquisite);
        accept(output, ModItems.heatSinkUltimate);
        // 粉末体系（打粉机产物 / 压缩机原料 / 变化器原料）
        accept(output, ModItems.akaishiDust);
        accept(output, ModItems.coalDust);
        accept(output, ModItems.ironDust);
        accept(output, ModItems.copperDust);
        accept(output, ModItems.goldDust);
        accept(output, ModItems.lapisDust);
        accept(output, ModItems.diamondDust);
        accept(output, ModItems.emeraldDust);
        accept(output, ModItems.quartzDust);
        accept(output, ModItems.obsidianDust);
        // 基底体系（变化器产物）
        accept(output, ModItems.coolingBase);
        accept(output, ModItems.coalOreBase);
        accept(output, ModItems.ironOreBase);
        accept(output, ModItems.copperOreBase);
        accept(output, ModItems.goldOreBase);
        accept(output, ModItems.redstoneOreBase);
        accept(output, ModItems.lapisOreBase);
        accept(output, ModItems.diamondOreBase);
        accept(output, ModItems.emeraldOreBase);
        accept(output, ModItems.quartzOreBase);
        accept(output, ModItems.netheriteOreBase);
        accept(output, ModItems.akaishiOreBase);
        // 山羊头旗帜图案（织布机图案物）
        accept(output, ModItems.goatSkullBannerPattern);
        // 手册
        accept(output, ModItems.akaishiDiary);
        accept(output, ModItems.lifeBook);
        accept(output, ModItems.geneBook);
    }

    // ==================== 栏 2：机械改造 ====================

    private static void addMechanicalItems(CreativeModeTab.Output output) {
        // 生产链三台机器
        accept(output, AkaishiMechanicalBlocks.CHISHI_MECHANICAL_TEMPLATE_FACTORY);
        accept(output, AkaishiMechanicalBlocks.CHISHI_MECHANICAL_PROCESSING_FACTORY);
        accept(output, AkaishiMechanicalBlocks.CHISHI_MECHANICAL_ASSEMBLY_STATION);
        // 材料融合器（9 种机械材料的唯一获取途径：两原料 + 赤能源）
        accept(output, AkaishiMechanicalBlocks.CHISHI_MATERIAL_FUSER);
        // 机械器官（9 槽位）
        accept(output, ModItems.mechanicalEye);
        accept(output, ModItems.mechanicalHeart);
        accept(output, ModItems.mechanicalLungs);
        accept(output, ModItems.mechanicalViscera);
        accept(output, ModItems.mechanicalKidneys);
        accept(output, ModItems.mechanicalLeftArm);
        accept(output, ModItems.mechanicalRightArm);
        accept(output, ModItems.mechanicalLeftLeg);
        accept(output, ModItems.mechanicalRightLeg);
        // 通用部件塑形模板 + 部位模板 + 加工部件
        accept(output, ModItems.genericPartMould);
        accept(output, ModItems.mechanicalPartTemplate);
        accept(output, ModItems.mechanicalProcessedPart);
        // 机械材料（9 种）
        accept(output, ModItems.redstoneAlloyIngot);
        accept(output, ModItems.ceramicCompositePlate);
        accept(output, ModItems.resistantSteelIngot);
        accept(output, ModItems.precisionAlloyIngot);
        accept(output, ModItems.polymerizedRedstoneCore);
        accept(output, ModItems.bioCeramicPlate);
        accept(output, ModItems.refinedCore);
        accept(output, ModItems.alloySteelIngot);
        accept(output, ModItems.psionicCompositeIngot);
    }

    // ==================== 栏 3：生命科技 ====================

    private static void addLifeItems(CreativeModeTab.Output output) {
        // 生物器官（9 槽位）
        accept(output, ModItems.akaishiOrganEye);
        accept(output, ModItems.akaishiOrganHeart);
        accept(output, ModItems.akaishiOrganLungs);
        accept(output, ModItems.akaishiOrganViscera);
        accept(output, ModItems.akaishiOrganKidneys);
        accept(output, ModItems.akaishiOrganLeftArm);
        accept(output, ModItems.akaishiOrganRightArm);
        accept(output, ModItems.akaishiOrganLeftLeg);
        accept(output, ModItems.akaishiOrganRightLeg);
        // 生命能量体系：管道（基础/中级/高级/超级）+ 聚合转换器 + 转换架构
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_PIPE);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_PIPE_ADVANCED);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_PIPE_ELITE);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_PIPE_ULTIMATE);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_PIPE_INFINITE);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_AGGREGATION_CONVERTER);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_CONVERSION_ARCHITECTURE);
        // 生命能量储存器（基础/高级/超级）+ 串联器
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_CELL);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_CELL_ADVANCED);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_CELL_SUPER);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_CELL_SERIALIZER);
        // 生命能量发射器 + 权杖（权杖两步式绑定发射目标）
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_EMITTER);
        accept(output, ModItems.lifeEnergyWand);
        // 生命能量无线终端族
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_SHELL);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_CORE);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_CONTROLLER);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_STRUCTURE_GLASS);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_TERMINAL);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_INPUT_PORT);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_OUTPUT_PORT);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_DIM_BRIDGE);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_CHUNK_LOADER);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_CHUNK_RANGE);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_INPUT_LOSS);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_OUTPUT_LOSS);
        accept(output, ModItems.akaishiLifeWirelessPortableTerminal);
        // 生命能量提纯器 + 固态物
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_PURIFIER);
        accept(output, ModItems.akaishiLifeEssenceSolid);
        // 生命处理机器族
        accept(output, ModBlocks.CHISHI_LIFE_ACTIVATOR);
        accept(output, ModBlocks.CHISHI_LIFE_CENTRIFUGE);
        accept(output, ModBlocks.CHISHI_ITEM_RECONSTRUCTOR);
        accept(output, ModBlocks.CHISHI_ACTIVATED_FRACTIONATOR);
        // 生命科技核心机器
        accept(output, AkaishiLifeBlocks.CHISHI_BODY_SCANNER);
        accept(output, AkaishiLifeBlocks.CHISHI_GENE_MANAGER);
        accept(output, AkaishiLifeBlocks.CHISHI_GENE_ANALYZER);
        accept(output, AkaishiLifeBlocks.CHISHI_TRANSGENE_FACTORY);
        accept(output, AkaishiLifeBlocks.CHISHI_CULTIVATOR);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_STRUCT);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_BREEDER);
        accept(output, AkaishiLifeBlocks.CHISHI_TRAIT_REFORGER);
        accept(output, AkaishiLifeBlocks.CHISHI_SURGERY);
        accept(output, AkaishiLifeBlocks.CHISHI_POTION_TABLE);
        // 药剂
        accept(output, ModItems.akaishiPotion);
        accept(output, ModItems.rejectionSerum);
        // 金西瓜（理智食补食物：30s 共回 10 SAN + 5s 生命回复 I，首用上限 +5）
        accept(output, ModItems.goldenMelonSlice);
        // 仓储
        accept(output, AkaishiLifeBlocks.CHISHI_ORGAN_VAULT);
        accept(output, AkaishiLifeBlocks.CHISHI_SAMPLE_VAULT);
        accept(output, AkaishiLifeBlocks.CHISHI_POTION_CABINET);
        // 母神祭坛体系随祭坛迁往禁忌栏 akaishi_forbidden（母神祭坛 / 祭坛石 / 猩红哭泣黑曜石）
        // 样本 / 基因 / 胚胎
        accept(output, ModItems.sampleCollector);
        accept(output, ModItems.lifeSample);
        accept(output, ModItems.lifeEmbryo);
        accept(output, ModItems.geneSequence);
        // 转基因/培育产物
        accept(output, ModItems.akaishiWitherSeed);
        accept(output, ModItems.akaishiWitherCondensate);
        accept(output, ModItems.akaishiBlazeSeed);
        accept(output, ModItems.akaishiBlazeCondensate);
        accept(output, ModItems.akaishiCurseVineSeed);
        accept(output, ModItems.akaishiCurseBlossom);
        accept(output, ModItems.akaishiEchoSeed);
        accept(output, ModItems.akaishiEchoFruit);
        accept(output, ModItems.akaishiEnderSeed);
        accept(output, ModItems.akaishiEnderFruit);
        // 离心结晶 + 活化成分
        accept(output, ModItems.exhaustedCrystal);
        accept(output, ModItems.activatedSculkCrystal);
        accept(output, ModItems.activatedNetherCompoundCrystal);
        accept(output, ModItems.activatedEndMixtureCrystal);
        accept(output, ModItems.activatedAdvancedMixtureCrystal);
        accept(output, ModItems.activatedPureCrystal);
        accept(output, ModItems.activatedDragonCrystal);
        accept(output, ModItems.activatedUltimateMixtureCrystal);
        accept(output, ModItems.activatedSculkComponent);
        accept(output, ModItems.activatedNetherCompoundComponent);
        accept(output, ModItems.activatedEndMixtureComponent);
        accept(output, ModItems.activatedAdvancedMixtureComponent);
        accept(output, ModItems.activatedPureComponent);
        accept(output, ModItems.activatedDragonComponent);
        accept(output, ModItems.activatedUltimateMixtureComponent);
        // 生命融合体系（融合锭 / 融合砧 / 生命融合护甲）随生命融合迁往禁忌栏 akaishi_forbidden
    }

    // ==================== 栏 4：机器与结构 ====================

    private static void addMachinesItems(CreativeModeTab.Output output) {
        // 矿石→锭基础机器
        accept(output, ModBlocks.CHISHI_PURIFIER);
        accept(output, ModBlocks.CHISHI_ADVANCED_PURIFIER);
        // 单槽处理机器（植物培养机/压缩机/打粉机/变化器）
        accept(output, ModBlocks.CHISHI_PLANT_CULTIVATOR);
        accept(output, ModBlocks.CHISHI_COMPRESSOR);
        accept(output, ModBlocks.CHISHI_PULVERIZER);
        accept(output, ModBlocks.CHISHI_TRANSFORMER);
        // 赤能源体系：电池 3 档 + 管道 4 档
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_CELL_BASIC);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_CELL_ADVANCED);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_CELL_SUPER);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_PIPE);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_PIPE_ADVANCED);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_PIPE_ELITE);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_PIPE_ULTIMATE);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_PIPE_INFINITE);
        // 赤能源发生机 + 小型组合结构 + 聚合器 + 串联器 + 超级发生器架构核心
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_GENERATOR);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_ASSEMBLY);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_AGGREGATOR);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_CELL_SERIALIZER);
        accept(output, AkaishiEnergyBlocks.CHISHI_SUPER_GENERATOR_CORE);
        // 物品管道（4 级）
        accept(output, ModBlocks.CHISHI_ITEM_PIPE);
        accept(output, ModBlocks.CHISHI_ITEM_PIPE_ADVANCED);
        accept(output, ModBlocks.CHISHI_ITEM_PIPE_ELITE);
        accept(output, ModBlocks.CHISHI_ITEM_PIPE_ULTIMATE);
        accept(output, ModBlocks.CHISHI_ITEM_PIPE_INFINITE);
        // 液体管道 + 废料管道 + 多重废液管道 + 储罐 3 档
        accept(output, ModBlocks.CHISHI_FLUID_PIPE);
        accept(output, ModBlocks.CHISHI_EXHAUSTED_PIPE);
        accept(output, ModBlocks.CHISHI_MULTI_FLUID_WASTE_PIPE);
        accept(output, ModBlocks.CHISHI_FLUID_TANK_BASIC);
        accept(output, ModBlocks.CHISHI_FLUID_TANK_ADVANCED);
        accept(output, ModBlocks.CHISHI_FLUID_TANK_SUPER);
        // 能源加工链：液化 + 加工器 + 装罐机 + 混合器 + 燃料罐
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_LIQUEFIER);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_PROCESSOR);
        accept(output, ModBlocks.CHISHI_FUEL_CANNER);
        accept(output, ModBlocks.CHISHI_FUEL_MIXER);
        accept(output, ModItems.fuelCell);
        // 燃料原料
        accept(output, ModItems.endMixture);
        accept(output, ModItems.dragonMixture);
        accept(output, ModItems.sculkLifeform);
        // 反应堆体系
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_SHELL);
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_STRUCTURE_GLASS);
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_CONTROLLER);
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_FUEL_PORT);
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_ENERGY_OUTPUT);
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_WASTE_PORT);
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_FUEL_ROD);
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_COOLER);
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_CORE);
        accept(output, AkaishiReactorBlocks.CHISHI_EXHAUSTED_BARREL);
        // 聚变燃料体系
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_FUEL_AGGREGATOR);
        accept(output, ModBlocks.CHISHI_PLASMA_FILLER);
        accept(output, ModBlocks.CHISHI_PLASMA_PIPE);
        accept(output, ModBlocks.CHISHI_PLASMA_TANK);
        accept(output, ModItems.fusionRod);
        accept(output, ModItems.mixedPlasmaRod);
        accept(output, ModItems.netherPlasmaRod);
        accept(output, ModItems.endPlasmaRod);
        // 发生器矩阵 / 提纯矩阵 / 生命转换矩阵
        accept(output, AkaishiMatrixBlocks.CHISHI_GEN_MATRIX_CASING);
        accept(output, AkaishiMatrixBlocks.CHISHI_GEN_MATRIX_STRUCTURE_GLASS);
        accept(output, AkaishiMatrixBlocks.CHISHI_GEN_MATRIX_CONTROLLER_BASIC);
        accept(output, AkaishiMatrixBlocks.CHISHI_GEN_MATRIX_CONTROLLER_ADVANCED);
        accept(output, AkaishiMatrixBlocks.CHISHI_GEN_ENERGY_OUTPUT);
        accept(output, AkaishiMatrixBlocks.CHISHI_GEN_FUEL_INPUT);
        accept(output, AkaishiMatrixBlocks.CHISHI_PURIFIER_MATRIX_CASING);
        accept(output, AkaishiMatrixBlocks.CHISHI_PURIFIER_MATRIX_STRUCTURE_GLASS);
        accept(output, AkaishiMatrixBlocks.CHISHI_PURIFIER_MATRIX_CONTROLLER);
        accept(output, AkaishiMatrixBlocks.CHISHI_PURIFIER_ENERGY_INPUT);
        accept(output, AkaishiMatrixBlocks.CHISHI_PURIFIER_ITEM_INPUT);
        accept(output, AkaishiMatrixBlocks.CHISHI_PURIFIER_ITEM_OUTPUT);
        accept(output, AkaishiMatrixBlocks.CHISHI_LIFE_MATRIX_CASING);
        accept(output, AkaishiMatrixBlocks.CHISHI_LIFE_MATRIX_STRUCTURE_GLASS);
        accept(output, AkaishiMatrixBlocks.CHISHI_LIFE_MATRIX_CONTROLLER);
        accept(output, AkaishiMatrixBlocks.CHISHI_LIFE_MATRIX_ENERGY_INPUT);
        accept(output, AkaishiMatrixBlocks.CHISHI_LIFE_MATRIX_ENERGY_OUTPUT);
        // 微缩矩阵（P1a：外壳 / 结构玻璃 / 控制器）
        accept(output, AkaishiMiniMatrixBlocks.CHISHI_MINI_MATRIX_CASING);
        accept(output, AkaishiMiniMatrixBlocks.CHISHI_MINI_MATRIX_STRUCTURE_GLASS);
        accept(output, AkaishiMiniMatrixBlocks.CHISHI_MINI_MATRIX_TERMINAL);
        // 微缩矩阵内腔升级组件（5 类方块，装在箱体内腔生效）
        accept(output, AkaishiMiniMatrixBlocks.CHISHI_MINI_MATRIX_UPGRADE_CONTROL);
        accept(output, AkaishiMiniMatrixBlocks.CHISHI_MINI_MATRIX_UPGRADE_FIELD);
        accept(output, AkaishiMiniMatrixBlocks.CHISHI_MINI_MATRIX_UPGRADE_EXTEND);
        accept(output, AkaishiMiniMatrixBlocks.CHISHI_MINI_MATRIX_UPGRADE_CRAFT);
        accept(output, AkaishiMiniMatrixBlocks.CHISHI_MINI_MATRIX_UPGRADE_LINK);
        accept(output, AkaishiMiniMatrixBlocks.CHISHI_MINI_MATRIX_NETWORK_NODE);
        accept(output, AkaishiMiniMatrixBlocks.CHISHI_WIRELESS_ACCESS_ADAPTER);
        // 无线赤能源体系（方块族 + 便捷件）
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_SHELL);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_STRUCTURE_GLASS);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_TERMINAL);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_SECURITY);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_CORE);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_CONTROLLER);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_INPUT_PORT);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_OUTPUT_PORT);
        // 储存无线输入/输出口（物品终端 IP 库的远程物品口，与能量口同排）
        accept(output, AkaishiItemTerminalBlocks.CHISHI_ITEM_INPUT_PORT);
        accept(output, AkaishiItemTerminalBlocks.CHISHI_ITEM_OUTPUT_PORT);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_DIM_BRIDGE);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_CHUNK_LOADER);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_CHUNK_RANGE);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_INPUT_LOSS);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_OUTPUT_LOSS);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_TRANSMIT_FRAME);
        accept(output, ModItems.akaishiWirelessComponent);
        accept(output, ModItems.akaishiWirelessPortableTerminal);
        accept(output, ModItems.akaishiWirelessIdentityCard);
        // 物品终端体系（IP 物品库：终端本体 + 三阶储存单元 + 赤能源接入口）
        accept(output, AkaishiItemTerminalBlocks.CHISHI_ITEM_TERMINAL);
        // 微缩终端：完整终端坍缩成的单方块（无合成配方，仅由坍缩动作产出）
        accept(output, AkaishiMiniatureBlocks.CHISHI_MINIATURE_TERMINAL);
        accept(output, AkaishiItemTerminalBlocks.CHISHI_ITEM_STORAGE_UNIT_BASIC);
        accept(output, AkaishiItemTerminalBlocks.CHISHI_ITEM_STORAGE_UNIT_ADVANCED);
        accept(output, AkaishiItemTerminalBlocks.CHISHI_ITEM_STORAGE_UNIT_SUPER);
        accept(output, AkaishiItemTerminalBlocks.CHISHI_ITEM_TERMINAL_ENERGY_INPUT);
        accept(output, AkaishiItemTerminalBlocks.CHISHI_ITEM_TERMINAL_SHELL);
        accept(output, AkaishiItemTerminalBlocks.CHISHI_ITEM_TERMINAL_STRUCTURE_GLASS);
        accept(output, AkaishiItemTerminalBlocks.CHISHI_ITEM_TERMINAL_CORE);
        accept(output, AkaishiItemTerminalBlocks.CHISHI_ITEM_TERMINAL_BUFFER_MODULE);
        accept(output, AkaishiItemTerminalBlocks.CHISHI_ITEM_TERMINAL_FEE_MODULE);
        accept(output, AkaishiItemTerminalBlocks.CHISHI_ITEM_TERMINAL_CHUNK_LOADER);
        accept(output, AkaishiItemTerminalBlocks.CHISHI_ITEM_TERMINAL_CHUNK_RANGE);
        // 聚变堆体系
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_SHELL);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_STRUCTURE_GLASS);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_INSULATION);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_CONTROLLER);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_CORE);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_COOLER_FRAME);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_FUEL_FRAME);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_EFFICIENCY_FRAME);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_ENERGY_OUTPUT);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_ITEM_INPUT);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_ITEM_OUTPUT);
        accept(output, ModItems.lifeAsh);
        accept(output, ModItems.fusionHeatSinkTier1);
        accept(output, ModItems.fusionHeatSinkTier2);
        accept(output, ModItems.fusionHeatSinkTier3);
        accept(output, ModItems.fusionHeatSinkTier4);
        accept(output, ModItems.fusionHeatSinkTier5);
        accept(output, ModItems.fusionHeatSinkLife);
        // 矿机体系
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_CONTROLLER_BASIC);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_CONTROLLER_ADVANCED);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_CONTROLLER_SUPER);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_CONTROLLER_ULTIMATE);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_FRAME);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_UPGRADE_FRAME);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_PORT);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_DRILL_BIT);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_FRAME_EXTERNAL);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_ENERGY_INPUT);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_ITEM_OUTPUT);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_SPEED_UPGRADE_BLOCK);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_FORTUNE_UPGRADE_BLOCK);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_STORAGE_UPGRADE_BLOCK);
        // 衰竭净化塔 + 衰竭全家桶（岩石/木/地表组）
        accept(output, AkaishiDecayBlocks.CHISHI_DECAY_PURIFIER);
        accept(output, ModBlocks.CHISHI_DECAY_SOIL);
        accept(output, ModBlocks.CHISHI_DECAY_LOG);
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_STONE.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_COBBLESTONE.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_STONE_BRICKS.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_STONE_BRICK_STAIRS.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_STONE_BRICK_SLAB.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_STONE_BRICK_WALL.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_PLANKS.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_STAIRS.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_SLAB.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_FENCE.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_FENCE_GATE.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_DOOR.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_TRAPDOOR.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_BUTTON.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_PRESSURE_PLATE.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_SAND.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_GRAVEL.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_GRASS_BLOCK.get()));
        // 创造模式能量源（测试用）
        accept(output, AkaishiEnergyBlocks.CHISHI_CREATIVE_ENERGY_CELL);
        accept(output, AkaishiLifeBlocks.CHISHI_CREATIVE_LIFE_CELL);
    }

    /** 判空后把注册内容（物品/方块，均实现 ItemLike）放入创造标签（注册完成前为 null，防御性跳过） */
    private static void accept(CreativeModeTab.Output output, RegistrySupplier<? extends ItemLike> supplier) {
        if (supplier != null) {
            output.accept(new ItemStack(supplier.get()));
        }
    }
}