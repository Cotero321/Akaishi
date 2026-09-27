package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

/**
 * 禁忌模块·方块注册（P3d 随生命融合 / 母神祭坛从本体迁入，注册 id 与迁前逐字一致）。
 *
 * <p>本类只承载「不属于某一独立域类」的禁忌方块；母神祭坛体系仍由
 * {@link AkaishiMotherAltarBlocks} 自行注册（同本体拆分范式）。
 * 未安装本模块时这些方块根本不注册（注册命名空间仍为三模块共用的 {@code akaishi:}）。
 */
public final class AkaishiForbiddenBlocks {

    /** 生命的融合砧（赤石护甲 + 生命的融合锭 → 生命融合护甲，保留升级数据） */
    public static RegistrySupplier<Block> CHISHI_LIFE_FUSION_ANVIL;

    private AkaishiForbiddenBlocks() {
    }

    /** 由 {@code AkaishiForbiddenMod.init()} 调用（须先于方块实体类型注册） */
    public static void register() {
        Registrar<Block> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
        CHISHI_LIFE_FUSION_ANVIL = AkaishiBlockRegistrar.registerMachineBlock(registrar,
                "akaishi_life_fusion_anvil", AkaishiLifeFusionAnvilBlock::new);
    }
}
