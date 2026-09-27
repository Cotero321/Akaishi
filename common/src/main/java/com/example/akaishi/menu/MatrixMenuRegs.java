package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.AkaishiGenMatrixTier;
import com.example.akaishi.block.entity.AkaishiGenMatrixControllerBlockEntity;
import com.example.akaishi.block.entity.AkaishiPurifierMatrixControllerBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeActivatorBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeCentrifugeBlockEntity;
import com.example.akaishi.block.entity.AkaishiItemReconstructorBlockEntity;
import com.example.akaishi.block.entity.AkaishiSingleSlotMachineBlockEntity;
import com.example.akaishi.block.entity.AkaishiPlantCultivatorBlockEntity;
import com.example.akaishi.block.entity.AkaishiCompressorBlockEntity;
import com.example.akaishi.block.entity.AkaishiPulverizerBlockEntity;
import com.example.akaishi.block.entity.AkaishiTransformerBlockEntity;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 菜单注册 · 矩阵与加工机族（承接 {@link ModMenus} 原注册序第 40~48 位）：
 * 发生器矩阵控制器、提纯矩阵控制器、生命活化器、生命离心机、物品重构仪、
 * 赤石植物培养机、赤石压缩机、赤石打粉机、赤石变化器。
 * <p>类内注册顺序与原 {@code ModMenus.register()} 逐位一致。
 */
final class MatrixMenuRegs {

    private MatrixMenuRegs() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void register() {
        // 发生器矩阵控制器：1 燃料槽 + 10 升级槽 + 8 数据槽（能量/燃烧/总量各低高两槽、成型、升级数）
        MenuType<AkaishiGenMatrixControllerMenu> genMatrixType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiGenMatrixControllerBlockEntity controller) {
                return new AkaishiGenMatrixControllerMenu(syncId, inv, controller);
            }
            return new AkaishiGenMatrixControllerMenu(syncId, inv,
                    new SimpleContainer(AkaishiGenMatrixControllerBlockEntity.TOTAL_SLOTS),
                    new SimpleContainerData(AkaishiGenMatrixControllerBlockEntity.DATA_SLOTS), AkaishiGenMatrixTier.BASIC);
        });
        ModMenus.CHISHI_GEN_MATRIX_CONTROLLER = (RegistrySupplier<MenuType<AkaishiGenMatrixControllerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_gen_matrix_controller"), () -> genMatrixType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(genMatrixType, AkaishiGenMatrixControllerScreen::new));

        // 提纯矩阵控制器：2 槽（输入/输出）+ 3 数据槽（能量/进度/成型）
        MenuType<AkaishiPurifierMatrixControllerMenu> purifierMatrixType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPurifierMatrixControllerBlockEntity controller) {
                return new AkaishiPurifierMatrixControllerMenu(syncId, inv, controller);
            }
            return new AkaishiPurifierMatrixControllerMenu(syncId, inv,
                    new SimpleContainer(AkaishiPurifierMatrixControllerBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiPurifierMatrixControllerBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_PURIFIER_MATRIX_CONTROLLER = (RegistrySupplier<MenuType<AkaishiPurifierMatrixControllerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_purifier_matrix_controller"), () -> purifierMatrixType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(purifierMatrixType, AkaishiPurifierMatrixControllerScreen::new));

        // 生命活化器：无机器槽位（纯液体无害化），7 数据槽（生命能量/输入/输出/累计活化量）
        MenuType<AkaishiLifeActivatorMenu> activatorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifeActivatorBlockEntity activator) {
                return new AkaishiLifeActivatorMenu(syncId, inv, activator);
            }
            return new AkaishiLifeActivatorMenu(syncId, inv,
                    new SimpleContainerData(AkaishiLifeActivatorBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_LIFE_ACTIVATOR = (RegistrySupplier<MenuType<AkaishiLifeActivatorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_activator"), () -> activatorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(activatorType, AkaishiLifeActivatorScreen::new));

        // 生命离心机：2 机器输出槽 + 5 数据槽（能量/罐量/进度）
        MenuType<AkaishiLifeCentrifugeMenu> centrifugeType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifeCentrifugeBlockEntity centrifuge) {
                return new AkaishiLifeCentrifugeMenu(syncId, inv, centrifuge);
            }
            return new AkaishiLifeCentrifugeMenu(syncId, inv, new SimpleContainer(2),
                    new SimpleContainerData(AkaishiLifeCentrifugeBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_LIFE_CENTRIFUGE = (RegistrySupplier<MenuType<AkaishiLifeCentrifugeMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_centrifuge"), () -> centrifugeType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(centrifugeType, AkaishiLifeCentrifugeScreen::new));

        // 物品重构仪：3 机器槽（原料/结晶/产物）+ 5 数据槽（能量/进度/所需/结晶数）
        MenuType<AkaishiItemReconstructorMenu> reconstructorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiItemReconstructorBlockEntity reconstructor) {
                return new AkaishiItemReconstructorMenu(syncId, inv, reconstructor);
            }
            return new AkaishiItemReconstructorMenu(syncId, inv, new SimpleContainer(3),
                    new SimpleContainerData(AkaishiItemReconstructorBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_ITEM_RECONSTRUCTOR = (RegistrySupplier<MenuType<AkaishiItemReconstructorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_item_reconstructor"), () -> reconstructorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(reconstructorType, AkaishiItemReconstructorScreen::new));

        // 赤石植物培养机：2 机器槽（输入/输出）+ 4 数据槽（能量/容量/进度/总耗时）
        MenuType<AkaishiPlantCultivatorMenu> plantCultivatorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPlantCultivatorBlockEntity cultivator) {
                return new AkaishiPlantCultivatorMenu(syncId, inv, cultivator);
            }
            return new AkaishiPlantCultivatorMenu(syncId, inv, new SimpleContainer(AkaishiSingleSlotMachineBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiSingleSlotMachineBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_PLANT_CULTIVATOR = (RegistrySupplier<MenuType<AkaishiPlantCultivatorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plant_cultivator"), () -> plantCultivatorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(plantCultivatorType, AkaishiPlantCultivatorScreen::new));

        // 赤石压缩机：2 机器槽（输入/输出）+ 4 数据槽（能量/容量/进度/总耗时）
        MenuType<AkaishiCompressorMenu> compressorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiCompressorBlockEntity compressor) {
                return new AkaishiCompressorMenu(syncId, inv, compressor);
            }
            return new AkaishiCompressorMenu(syncId, inv, new SimpleContainer(AkaishiSingleSlotMachineBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiSingleSlotMachineBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_COMPRESSOR = (RegistrySupplier<MenuType<AkaishiCompressorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_compressor"), () -> compressorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(compressorType, AkaishiCompressorScreen::new));

        // 赤石打粉机：2 机器槽（输入/输出）+ 4 数据槽（能量/容量/进度/总耗时）
        MenuType<AkaishiPulverizerMenu> pulverizerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPulverizerBlockEntity pulverizer) {
                return new AkaishiPulverizerMenu(syncId, inv, pulverizer);
            }
            return new AkaishiPulverizerMenu(syncId, inv, new SimpleContainer(AkaishiSingleSlotMachineBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiSingleSlotMachineBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_PULVERIZER = (RegistrySupplier<MenuType<AkaishiPulverizerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_pulverizer"), () -> pulverizerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(pulverizerType, AkaishiPulverizerScreen::new));

        // 赤石变化器：2 机器槽（输入/输出）+ 4 数据槽（能量/容量/进度/总耗时）
        MenuType<AkaishiTransformerMenu> transformerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiTransformerBlockEntity transformer) {
                return new AkaishiTransformerMenu(syncId, inv, transformer);
            }
            return new AkaishiTransformerMenu(syncId, inv, new SimpleContainer(AkaishiSingleSlotMachineBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiSingleSlotMachineBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_TRANSFORMER = (RegistrySupplier<MenuType<AkaishiTransformerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_transformer"), () -> transformerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(transformerType, AkaishiTransformerScreen::new));
    }
}
