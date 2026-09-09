package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.item.AkaishiMechanicalItems;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 机械域方块注册：模板制造厂、加工制作厂、组装加工台。
 * <p>
 * 所有方块通过 {@link AkaishiBlockRegistrar#registerMachineBlock} 注册（方块 + 同名 BlockItem）。
 * 门面转发由 {@link ModBlocks#register()} 完成。
 */
public final class AkaishiMechanicalBlocks {

    private AkaishiMechanicalBlocks() {}

    /** 机械改造模板制造厂：DNA + 四个部件材料 → 部件模板 */
    public static RegistrySupplier<Block> CHISHI_MECHANICAL_TEMPLATE_FACTORY;
    /** 机械改造加工制作厂：模板 + 主材料 → 加工部件 */
    public static RegistrySupplier<Block> CHISHI_MECHANICAL_PROCESSING_FACTORY;
    /** 机械改造组装加工台：四个加工部件 → 成品机械器官 */
    public static RegistrySupplier<Block> CHISHI_MECHANICAL_ASSEMBLY_STATION;

    public static void register() {
        Registrar<Block> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);

        CHISHI_MECHANICAL_TEMPLATE_FACTORY = AkaishiBlockRegistrar.registerMachineBlock(
                registrar,
                "akaishi_mechanical_template_factory",
                () -> new AkaishiMechanicalMachineBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_GRAY)
                        .strength(5.0F, 6.0F)
                        .requiresCorrectToolForDrops(),
                        () -> ModBlockEntities.CHISHI_MECHANICAL_TEMPLATE_FACTORY.get())
        );

        CHISHI_MECHANICAL_PROCESSING_FACTORY = AkaishiBlockRegistrar.registerMachineBlock(
                registrar,
                "akaishi_mechanical_processing_factory",
                () -> new AkaishiMechanicalMachineBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_GRAY)
                        .strength(5.0F, 6.0F)
                        .requiresCorrectToolForDrops(),
                        () -> ModBlockEntities.CHISHI_MECHANICAL_PROCESSING_FACTORY.get())
        );

        CHISHI_MECHANICAL_ASSEMBLY_STATION = AkaishiBlockRegistrar.registerMachineBlock(
                registrar,
                "akaishi_mechanical_assembly_station",
                () -> new AkaishiMechanicalMachineBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_GRAY)
                        .strength(5.0F, 6.0F)
                        .requiresCorrectToolForDrops(),
                        () -> ModBlockEntities.CHISHI_MECHANICAL_ASSEMBLY_STATION.get())
        );
    }
}