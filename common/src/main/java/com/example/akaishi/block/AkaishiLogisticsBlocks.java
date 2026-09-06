package com.example.akaishi.block;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.fluid.FluidTankTier;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * 物流域方块注册：物品/液体/废料/等离子体四类管道与对应储罐。
 * 管道负责网络传输（按缓冲与等级区隔），储罐提供跨管道存取容量。
 * <p>
 * 从 ModBlocks 拆分出的域注册类（沿用 registerItemPipe / registerFluidTank 的
 * 「方块 + 同名 BlockItem」注册样板）。所有静态字段显式初始化为 null，由
 * {@link #register()} 在 {@link AkaishiMod#init()} 阶段填充。
 */
public final class AkaishiLogisticsBlocks {

    /** 物品管道（基础）：物流网络中继，1 个/tick */
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE = null;
    /** 物品管道（高级） */
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE_ADVANCED = null;
    /** 物品管道（精英） */
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE_ELITE = null;
    /** 物品管道（终极）：64 个/tick */
    public static RegistrySupplier<Block> CHISHI_ITEM_PIPE_ULTIMATE = null;
    /** 液体管道：传输下界能量/燃料液体，可对接 MEK 等外部液体方块 */
    public static RegistrySupplier<Block> CHISHI_FLUID_PIPE = null;
    /** 封闭性衰竭管道：废料专用（单缓冲） */
    public static RegistrySupplier<Block> CHISHI_EXHAUSTED_PIPE = null;
    /** 多流体废料管道：废料专用（多缓冲） */
    public static RegistrySupplier<Block> CHISHI_MULTI_FLUID_WASTE_PIPE = null;
    /** 等离子体管道（第三传输家族，仅传等离子体） */
    public static RegistrySupplier<Block> CHISHI_PLASMA_PIPE = null;
    /** 液体储罐（基础/高级/超级：16k/64k/256k mb，管道存取液体） */
    public static RegistrySupplier<Block> CHISHI_FLUID_TANK_BASIC = null;
    public static RegistrySupplier<Block> CHISHI_FLUID_TANK_ADVANCED = null;
    public static RegistrySupplier<Block> CHISHI_FLUID_TANK_SUPER = null;
    /** 等离子体燃料储罐（仅存储等离子体，仅等离子体管道可对接） */
    public static RegistrySupplier<Block> CHISHI_PLASMA_TANK = null;

    private AkaishiLogisticsBlocks() {
    }

    /** 注册全部物流方块（由 ModBlocks 门面在 AkaishiMod.init 阶段统一调用） */
    public static void register() {
        Registrar<Block> blockRegistrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);

        // 物品管道（4 级）：物流网络中继，传输物品到相连容器/机器，终极 64 个/tick
        CHISHI_ITEM_PIPE = registerItemPipe(blockRegistrar, "akaishi_item_pipe", AkaishiItemPipeBlock.ItemPipeTier.BASIC);
        CHISHI_ITEM_PIPE_ADVANCED = registerItemPipe(blockRegistrar, "akaishi_item_pipe_advanced", AkaishiItemPipeBlock.ItemPipeTier.ADVANCED);
        CHISHI_ITEM_PIPE_ELITE = registerItemPipe(blockRegistrar, "akaishi_item_pipe_elite", AkaishiItemPipeBlock.ItemPipeTier.ELITE);
        CHISHI_ITEM_PIPE_ULTIMATE = registerItemPipe(blockRegistrar, "akaishi_item_pipe_ultimate", AkaishiItemPipeBlock.ItemPipeTier.ULTIMATE);

        // 液体管道（单级，传输下界能量/燃料液体）
        CHISHI_FLUID_PIPE = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_fluid_pipe", AkaishiFluidPipeBlock::new);
        // 封闭性衰竭管道（废料专用，单缓冲；与普通液体管道网络隔离）
        CHISHI_EXHAUSTED_PIPE = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_exhausted_pipe", AkaishiExhaustedPipeBlock::new);
        // 多流体废料管道（废料专用，多缓冲，多种废料可混输）
        CHISHI_MULTI_FLUID_WASTE_PIPE = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_multi_fluid_waste_pipe", AkaishiMultiFluidWastePipeBlock::new);
        // 等离子体管道（第三传输家族，仅传等离子体）
        CHISHI_PLASMA_PIPE = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_plasma_pipe", AkaishiPlasmaPipeBlock::new);

        // 液体储罐（基础/高级/超级，容量递增，可被液体管道注入/抽取）
        CHISHI_FLUID_TANK_BASIC = registerFluidTank(blockRegistrar, "akaishi_fluid_tank_basic", FluidTankTier.BASIC);
        CHISHI_FLUID_TANK_ADVANCED = registerFluidTank(blockRegistrar, "akaishi_fluid_tank_advanced", FluidTankTier.ADVANCED);
        CHISHI_FLUID_TANK_SUPER = registerFluidTank(blockRegistrar, "akaishi_fluid_tank_super", FluidTankTier.SUPER);
        // 等离子体燃料储罐：仅存储等离子体（罐层拒收非等离子体液体）
        CHISHI_PLASMA_TANK = AkaishiBlockRegistrar.registerMachineBlock(blockRegistrar, "akaishi_plasma_tank", AkaishiPlasmaTankBlock::new);
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
}
