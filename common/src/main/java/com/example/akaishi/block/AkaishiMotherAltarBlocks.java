package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 母神祭坛方块族注册表。
 * <p>
 * 从 ModBlocks 拆分出的域注册类：母神祭坛核心 + 母神祭坛石（结构件）。
 * 字段由 {@link #register()} 在 {@link AkaishiMod#init()} 阶段填充，消费方须在
 * register() 之后访问，否则会触发 NPE。
 */
public final class AkaishiMotherAltarBlocks {

    /** 母神祭坛：生命线终局多方块祭坛核心（黑山羊之母，NBT 献祭识别 + 悬浮供奉） */
    public static RegistrySupplier<Block> CHISHI_MOTHER_ALTAR = null;
    /** 母神祭坛石：祭坛结构件（5×5 底座 + 四角柱，铺设成型后母神驻留） */
    public static RegistrySupplier<Block> CHISHI_ALTAR_STONE = null;

    private AkaishiMotherAltarBlocks() {
    }

    /** 注册全部母神祭坛方块（由 AkaishiMod.init 调用） */
    public static void register() {
        Registrar<Block> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);
        CHISHI_MOTHER_ALTAR = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_mother_altar", AkaishiMotherAltarBlock::new);
        CHISHI_ALTAR_STONE = AkaishiBlockRegistrar.registerMachineBlock(registrar, "akaishi_altar_stone",
                () -> new Block(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN)));
    }
}
