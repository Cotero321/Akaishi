package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * 方块注册公共工具：注册一个「方块 + 同名 BlockItem」的组合。
 * <p>
 * 各机器族域注册类（如 AkaishiWirelessBlocks / AkaishiReactorBlocks / AkaishiFusionBlocks）
 * 统一经由 {@link #registerMachineBlock} 注册，避免每个域类各自复制一套 BlockItem 注册样板。
 * 所有 register 调用都发生在 {@link AkaishiMod#init()} 的注册表冻结前。
 */
public final class AkaishiBlockRegistrar {

    private AkaishiBlockRegistrar() {
    }

    /** 注册方块及其同名 BlockItem，返回方块的延迟引用 */
    public static RegistrySupplier<Block> registerMachineBlock(Registrar<Block> registrar, String id,
                                                               Supplier<Block> factory) {
        RegistrySupplier<Block> block = registrar.register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
}
