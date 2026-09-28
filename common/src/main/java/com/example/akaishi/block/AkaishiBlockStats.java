package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;

/**
 * 方块挖掘手感与抗爆的统一出处（抽取自原先散落在 140+ 个方块类里的硬编码数值）。
 * <p>
 * <b>为什么集中</b>：原先每个方块类各写各的 {@code strength(...)}，实际散成 7 档
 * （0.5 / 1.5 / 3.0 / 3.5 / 4.0 / 5.0 / 6.0），同族部件互不一致，调平衡要翻一百多个文件。
 * <p>
 * <b>口径（用户拍板）</b>：
 * <ul>
 *   <li>所有机器一律 <b>铁镐可挖</b>：由基类统一加 {@code requiresCorrectToolForDrops}，
 *       配合标签 {@code mineable/pickaxe} + {@code needs_iron_tool}（数据侧，见同目录 resources）；</li>
 *   <li>挖掘时间<b>不要长</b>：铁镐挖 3.5 硬度约 0.6 秒，且刻意不随阶级递增；</li>
 *   <li><b>多方块结构件防爆</b>：结构被炸开一角就整台失效，故抗爆提到黑曜石级。</li>
 * </ul>
 */
public final class AkaishiBlockStats {

    /** 机器统一硬度：铁镐约 0.6 秒挖开（不随阶级递增，避免大机器挖起来拖沓） */
    public static final float MACHINE_HARDNESS = 3.5F;
    /** 机器统一抗爆：普通抗爆 */
    public static final float MACHINE_RESISTANCE = 6.0F;
    /** 多方块结构件抗爆：等同黑曜石级，TNT 与苦力怕无法破坏（玩家仍可正常挖掘） */
    public static final float STRUCTURE_BLAST_RESISTANCE = 1200.0F;

    /**
     * 多方块结构件的 id 前缀（<b>结构件识别的单一真源</b>）。
     * <p>
     * 这些家族的方块要么是结构外壳/玻璃，要么是结构上的端口与核心 —— 任何一个被炸掉，
     * 整台多方块都会失效，所以统一防爆。新增结构零件时把新前缀（或新家族）加到本表即可，
     * 不需要动任何方块类。
     * <p>
     * <b>为什么用前缀而不是方块标签</b>：1.20.1 的 {@code Block#getExplosionResistance} 只有无参重载，
     * 拿不到 {@code BlockState}，无法读标签；按 id 前缀判定同样是"一处集中"，且不依赖平台事件。
     */
    private static final List<String> STRUCTURE_ID_PREFIXES = List.of(
            "akaishi_fusion_",           // 聚变堆：外壳/玻璃/端口/核心/框架
            "akaishi_reactor_",          // 反应堆：外壳/玻璃/端口/核心/冷却器
            "akaishi_wireless_",         // 无线赤能源：外壳/玻璃/终端/核心/端口/桥接
            "akaishi_life_wireless_",    // 生命无线：同上
            "akaishi_item_terminal",     // 物品终端：外壳/玻璃/核心/端口
            "akaishi_item_input_port",   // 物品输入口
            "akaishi_item_output_port",  // 物品输出口
            "akaishi_purifier_matrix",   // 提纯矩阵：外壳/玻璃/控制器
            "akaishi_purifier_energy_input",
            "akaishi_purifier_item_",
            "akaishi_gen_matrix",        // 发电矩阵
            "akaishi_gen_energy_output",
            "akaishi_gen_fuel_input",
            "akaishi_life_matrix",       // 生命矩阵
            "akaishi_mini_matrix",       // 微缩矩阵
            "akaishi_miniature_terminal",
            "akaishi_miner_",            // 矿机：架构/转口/出入口（多格结构）
            "akaishi_energy_cell",       // 储存串联器（3×3×3 多方块）
            "akaishi_life_energy_cell");

    private AkaishiBlockStats() {
    }

    /**
     * 机器方块统一属性：覆盖子类自己传的 {@code strength(...)}。
     * <p>统一要求正确工具（配合 {@code mineable/pickaxe} + {@code needs_iron_tool} ⇒ 铁镐起可挖），
     * 硬度与抗爆取本类常量，保证全项目机器手感一致。
     */
    public static BlockBehaviour.Properties machine(BlockBehaviour.Properties properties) {
        return properties.requiresCorrectToolForDrops().strength(MACHINE_HARDNESS, MACHINE_RESISTANCE);
    }

    /** 该方块是否多方块结构件（按注册 id 前缀判定，见 {@link #STRUCTURE_ID_PREFIXES}） */
    public static boolean isStructureBlock(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        if (key == null || !AkaishiMod.MOD_ID.equals(key.getNamespace())) {
            return false;
        }
        String path = key.getPath();
        for (String prefix : STRUCTURE_ID_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
