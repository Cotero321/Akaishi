package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.item.AkaishiMiniatureBlockItem;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

/**
 * 微缩终端方块族注册表：把完整终端（多方块结构）连同数据与接口能力浓缩成的<b>单方块</b>。
 * <p>
 * 获取路径只有一条：<b>对已成型终端使用微缩</b>（坍缩动作消耗整套结构并原地留下本方块）。
 * 因此本族<b>有意不提供合成配方</b>，且不设独立外壳/核心等结构件——本方块即终端本体的替代形态。
 * <p>
 * 方块本体是通用壳（不认具体终端族），族差异全部由 {@code api/miniature} 的适配器承载，
 * 故后续新增终端族只加适配器，本类无需改动（OCP）。
 * 字段由 {@link #register()} 在 {@link com.example.akaishi.AkaishiMod#init()} 阶段填充，
 * 消费方须在 register() 之后访问。
 */
public final class AkaishiMiniatureBlocks {

    /** 微缩终端：通用单方块壳，承载被浓缩终端的全部数据与对外能力 */
    public static RegistrySupplier<Block> CHISHI_MINIATURE_TERMINAL = null;

    private AkaishiMiniatureBlocks() {
    }

    /** 注册全部微缩终端方块（由 AkaishiMod.init 在 ModBlockEntities.register 之前调用） */
    public static void register() {
        Registrar<Block> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
        // 物品用自定义 BlockItem：把「里面装的是哪台终端」写进悬浮文本（数据本身仍走 BlockEntityTag）
        CHISHI_MINIATURE_TERMINAL = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_miniature_terminal", AkaishiMiniatureTerminalBlock::new,
                (block, properties) -> new AkaishiMiniatureBlockItem(block, properties));
    }
}
