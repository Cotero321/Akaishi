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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

import java.util.function.Supplier;

/**
 * 衰竭域方块注册（衰竭全家桶）：衰竭区域内对应原版方块被污染后的终态变体。
 * <p>
 * 按材质族划分四组：衰竭岩石组（石/圆石/石砖及其楼梯台阶墙）、衰竭木完整组
 * （木板/楼梯/台阶/栅栏/栅栏门/门/活板门/按钮/压力板）、衰竭地表组
 * （沙/砾石/草方块）、衰竭区域治理与终态方块（衰变净化塔/衰竭土壤/衰竭木）。
 * 属性一律沿袭各自原版方块，保证挖掘音效/硬度/工具一致，
 * 仅在视觉上呈现灰紫腐化。
 * <p>
 * 所有静态字段显式初始化为 null，由 {@link #register()} 在 {@link AkaishiMod#init()}
 * 阶段填充；任何消费方都须在 register() 之后访问，否则会触发 NPE。
 */
public final class AkaishiDecayBlocks {

    // ===== 衰竭岩石组 =====
    public static RegistrySupplier<Block> CHISHI_DECAY_STONE = null;
    public static RegistrySupplier<Block> CHISHI_DECAY_COBBLESTONE = null;
    public static RegistrySupplier<Block> CHISHI_DECAY_STONE_BRICKS = null;
    public static RegistrySupplier<Block> CHISHI_DECAY_STONE_BRICK_STAIRS = null;
    public static RegistrySupplier<Block> CHISHI_DECAY_STONE_BRICK_SLAB = null;
    public static RegistrySupplier<Block> CHISHI_DECAY_STONE_BRICK_WALL = null;

    // ===== 衰竭木完整组 =====
    public static RegistrySupplier<Block> CHISHI_DECAY_PLANKS = null;
    public static RegistrySupplier<Block> CHISHI_DECAY_STAIRS = null;
    public static RegistrySupplier<Block> CHISHI_DECAY_SLAB = null;
    public static RegistrySupplier<Block> CHISHI_DECAY_FENCE = null;
    public static RegistrySupplier<Block> CHISHI_DECAY_FENCE_GATE = null;
    public static RegistrySupplier<Block> CHISHI_DECAY_DOOR = null;
    public static RegistrySupplier<Block> CHISHI_DECAY_TRAPDOOR = null;
    public static RegistrySupplier<Block> CHISHI_DECAY_BUTTON = null;
    public static RegistrySupplier<Block> CHISHI_DECAY_PRESSURE_PLATE = null;

    // ===== 衰竭地表组 =====
    public static RegistrySupplier<Block> CHISHI_DECAY_SAND = null;
    public static RegistrySupplier<Block> CHISHI_DECAY_GRAVEL = null;
    public static RegistrySupplier<Block> CHISHI_DECAY_GRASS_BLOCK = null;

    // ===== 衰竭区域治理与终态方块 =====
    /** 衰变净化塔：清除衰竭地表并将其转化为衰竭土壤的治理机器 */
    public static RegistrySupplier<Block> CHISHI_DECAY_PURIFIER = null;
    /** 衰竭土壤：衰竭区域地表经净化后的终态（不可蔓延） */
    public static RegistrySupplier<Block> CHISHI_DECAY_SOIL = null;
    /** 衰竭木：衰竭区域树木被污染后的终态 */
    public static RegistrySupplier<Block> CHISHI_DECAY_LOG = null;

    private AkaishiDecayBlocks() {
    }

    /** 注册全部衰竭方块（供 AkaishiMod.init 调用，与 ModBlocks 注册顺序无关） */
    public static void register() {
        Registrar<Block> blocks = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.BLOCK);

        // ---- 衰竭岩石组：硬度/音效/工具沿用原版石头系 ----
        CHISHI_DECAY_STONE = register(blocks, "akaishi_decay_stone",
                () -> new Block(Block.Properties.copy(Blocks.STONE)));
        CHISHI_DECAY_COBBLESTONE = register(blocks, "akaishi_decay_cobblestone",
                () -> new Block(Block.Properties.copy(Blocks.COBBLESTONE)));
        CHISHI_DECAY_STONE_BRICKS = register(blocks, "akaishi_decay_stone_bricks",
                () -> new Block(Block.Properties.copy(Blocks.STONE_BRICKS)));
        CHISHI_DECAY_STONE_BRICK_STAIRS = register(blocks, "akaishi_decay_stone_brick_stairs",
                () -> new StairBlock(Blocks.STONE_BRICKS.defaultBlockState(),
                        Block.Properties.copy(Blocks.STONE_BRICKS)));
        CHISHI_DECAY_STONE_BRICK_SLAB = register(blocks, "akaishi_decay_stone_brick_slab",
                () -> new SlabBlock(Block.Properties.copy(Blocks.STONE_BRICKS)));
        CHISHI_DECAY_STONE_BRICK_WALL = register(blocks, "akaishi_decay_stone_brick_wall",
                () -> new WallBlock(Block.Properties.copy(Blocks.STONE_BRICKS)));

        // ---- 衰竭木完整组：沿用原版橡木系属性（木门类走 BlockSetType.OAK 交互）----
        CHISHI_DECAY_PLANKS = register(blocks, "akaishi_decay_planks",
                () -> new Block(Block.Properties.copy(Blocks.OAK_PLANKS)));
        CHISHI_DECAY_STAIRS = register(blocks, "akaishi_decay_stairs",
                () -> new StairBlock(Blocks.OAK_PLANKS.defaultBlockState(),
                        Block.Properties.copy(Blocks.OAK_PLANKS)));
        CHISHI_DECAY_SLAB = register(blocks, "akaishi_decay_slab",
                () -> new SlabBlock(Block.Properties.copy(Blocks.OAK_PLANKS)));
        CHISHI_DECAY_FENCE = register(blocks, "akaishi_decay_fence",
                () -> new FenceBlock(Block.Properties.copy(Blocks.OAK_PLANKS)));
        CHISHI_DECAY_FENCE_GATE = register(blocks, "akaishi_decay_fence_gate",
                () -> new FenceGateBlock(Block.Properties.copy(Blocks.OAK_PLANKS), WoodType.OAK));
        CHISHI_DECAY_DOOR = register(blocks, "akaishi_decay_door",
                () -> new DoorBlock(Block.Properties.copy(Blocks.OAK_DOOR), BlockSetType.OAK));
        CHISHI_DECAY_TRAPDOOR = register(blocks, "akaishi_decay_trapdoor",
                () -> new TrapDoorBlock(Block.Properties.copy(Blocks.OAK_TRAPDOOR), BlockSetType.OAK));
        CHISHI_DECAY_BUTTON = register(blocks, "akaishi_decay_button",
                () -> new ButtonBlock(Block.Properties.copy(Blocks.OAK_BUTTON), BlockSetType.OAK, 30, true));
        CHISHI_DECAY_PRESSURE_PLATE = register(blocks, "akaishi_decay_pressure_plate",
                () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING,
                        Block.Properties.copy(Blocks.OAK_PRESSURE_PLATE), BlockSetType.OAK));

        // ---- 衰竭地表组：沙/砾石承重力与挖掘方式沿用原版；草块取草块属性但不做蔓延 ----
        CHISHI_DECAY_SAND = register(blocks, "akaishi_decay_sand",
                () -> new Block(Block.Properties.copy(Blocks.SAND)));
        CHISHI_DECAY_GRAVEL = register(blocks, "akaishi_decay_gravel",
                () -> new Block(Block.Properties.copy(Blocks.GRAVEL)));
        CHISHI_DECAY_GRASS_BLOCK = register(blocks, "akaishi_decay_grass_block",
                () -> new Block(Block.Properties.copy(Blocks.GRASS_BLOCK)));

        // ---- 衰竭区域治理与终态方块：净化塔（机器）与污染产物块（均带 BlockItem）----
        CHISHI_DECAY_PURIFIER = register(blocks, "akaishi_decay_purifier", AkaishiDecayPurifierBlock::new);
        CHISHI_DECAY_SOIL = register(blocks, "akaishi_decay_soil", AkaishiDecaySoilBlock::new);
        CHISHI_DECAY_LOG = register(blocks, "akaishi_decay_log", AkaishiDecayLogBlock::new);
    }

    /** 注册方块 + 同名 BlockItem */
    private static RegistrySupplier<Block> register(Registrar<Block> registrar, String id,
                                                    Supplier<Block> factory) {
        RegistrySupplier<Block> block = registrar.register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id),
                        () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
}
