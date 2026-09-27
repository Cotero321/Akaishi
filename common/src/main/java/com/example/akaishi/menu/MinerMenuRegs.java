package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiMinerControllerBlockEntity;
import com.example.akaishi.block.entity.AkaishiMinerPortBlockEntity;
import com.example.akaishi.block.entity.AkaishiMinerEnergyInputBlockEntity;
import com.example.akaishi.block.entity.AkaishiMinerItemOutputBlockEntity;
import com.example.akaishi.block.entity.AkaishiActivatedFractionatorBlockEntity;
import com.example.akaishi.block.entity.AkaishiFusionFuelAggregatorBlockEntity;
import com.example.akaishi.block.entity.AkaishiPlasmaFillerBlockEntity;
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
 * 菜单注册 · 矿机与聚变前哨族（承接 {@link ModMenus} 原注册序第 49~55 位）：
 * 赤石矿机控制器、矿机转口、矿机能量输入口、矿机物品输出口、
 * 活化分馏器、聚变燃料聚合器、离子体填装器（后三台为聚变产业链前哨机，
 * 在原文件中即位于此段，为保注册顺序逐位不变而与本族同段搬迁）。
 * <p>类内注册顺序与原 {@code ModMenus.register()} 逐位一致。
 */
final class MinerMenuRegs {

    private MinerMenuRegs() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void register() {
        // 赤石矿机控制器：产物暂存 6 槽 + 8 数据槽（能量/容量/进度/总耗时/成型/3 类升级）
        MenuType<AkaishiMinerControllerMenu> minerControllerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiMinerControllerBlockEntity controller) {
                return new AkaishiMinerControllerMenu(syncId, inv, controller);
            }
            return new AkaishiMinerControllerMenu(syncId, inv,
                    new SimpleContainer(AkaishiMinerControllerBlockEntity.OUTPUT_SLOTS),
                    new SimpleContainerData(AkaishiMinerControllerBlockEntity.DATA_COUNT));
        });
        ModMenus.CHISHI_MINER_CONTROLLER = (RegistrySupplier<MenuType<AkaishiMinerControllerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_miner_controller"), () -> minerControllerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(minerControllerType, AkaishiMinerControllerScreen::new));

        // 矿机转口：产物缓冲 27 槽 + 5 数据槽（能量/容量高低两槽 + 成型）
        MenuType<AkaishiMinerPortMenu> minerPortType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiMinerPortBlockEntity port) {
                return new AkaishiMinerPortMenu(syncId, inv, port);
            }
            return new AkaishiMinerPortMenu(syncId, inv,
                    new SimpleContainer(AkaishiMinerPortBlockEntity.BUFFER_SLOTS),
                    new SimpleContainerData(AkaishiMinerPortBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_MINER_PORT = (RegistrySupplier<MenuType<AkaishiMinerPortMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_miner_port"), () -> minerPortType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(minerPortType, AkaishiMinerPortScreen::new));

        // 矿机能量输入口：纯能量缓冲展示（无机器槽）+ 3 数据槽（能量/容量/成型）
        MenuType<AkaishiMinerEnergyInputMenu> energyInputType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiMinerEnergyInputBlockEntity port) {
                return new AkaishiMinerEnergyInputMenu(syncId, inv, port.data());
            }
            return AkaishiMinerEnergyInputMenu.emptyMenu(syncId, inv);
        });
        ModMenus.CHISHI_MINER_ENERGY_INPUT = (RegistrySupplier<MenuType<AkaishiMinerEnergyInputMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_miner_energy_input"), () -> energyInputType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(energyInputType, AkaishiMinerEnergyInputScreen::new));

        // 矿机物品输出口：产物缓冲 27 槽（只读）+ 3 数据槽（能量/容量/成型）
        MenuType<AkaishiMinerItemOutputMenu> itemOutputType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiMinerItemOutputBlockEntity port) {
                return new AkaishiMinerItemOutputMenu(syncId, inv, port);
            }
            return new AkaishiMinerItemOutputMenu(syncId, inv,
                    new SimpleContainer(AkaishiMinerItemOutputBlockEntity.BUFFER_SLOTS),
                    new SimpleContainerData(3));
        });
        ModMenus.CHISHI_MINER_ITEM_OUTPUT = (RegistrySupplier<MenuType<AkaishiMinerItemOutputMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_miner_item_output"), () -> itemOutputType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(itemOutputType, AkaishiMinerItemOutputScreen::new));

        // 活化分馏器：3 机器槽（输入活化结晶 + 2 只读输出槽）+ 3 数据槽（能量/进度）
        MenuType<AkaishiActivatedFractionatorMenu> fractionatorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiActivatedFractionatorBlockEntity fractionator) {
                return new AkaishiActivatedFractionatorMenu(syncId, inv, fractionator);
            }
            return new AkaishiActivatedFractionatorMenu(syncId, inv, new SimpleContainer(1),
                    new SimpleContainer(2), new SimpleContainerData(AkaishiActivatedFractionatorBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_ACTIVATED_FRACTIONATOR = (RegistrySupplier<MenuType<AkaishiActivatedFractionatorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_fractionator"), () -> fractionatorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fractionatorType, AkaishiActivatedFractionatorScreen::new));

        // 聚变燃料聚合器：1 活化成分输入槽 + 9 数据槽（能量/进度/3 等离子体罐量）
        MenuType<AkaishiFusionFuelAggregatorMenu> fusionAggregatorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiFusionFuelAggregatorBlockEntity aggregator) {
                return new AkaishiFusionFuelAggregatorMenu(syncId, inv, aggregator);
            }
            return new AkaishiFusionFuelAggregatorMenu(syncId, inv, new SimpleContainer(1),
                    new SimpleContainerData(AkaishiFusionFuelAggregatorBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_FUSION_FUEL_AGGREGATOR = (RegistrySupplier<MenuType<AkaishiFusionFuelAggregatorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fusion_fuel_aggregator"), () -> fusionAggregatorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fusionAggregatorType, AkaishiFusionFuelAggregatorScreen::new));

        // 离子体填装器：1 反应棒槽 + 3 只读燃料棒输出槽 + 7 数据槽（3 等离子体罐量/进度）
        MenuType<AkaishiPlasmaFillerMenu> fillerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPlasmaFillerBlockEntity filler) {
                return new AkaishiPlasmaFillerMenu(syncId, inv, filler);
            }
            return new AkaishiPlasmaFillerMenu(syncId, inv, new SimpleContainer(1), new SimpleContainer(3),
                    new SimpleContainerData(AkaishiPlasmaFillerBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_PLASMA_FILLER = (RegistrySupplier<MenuType<AkaishiPlasmaFillerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plasma_filler"), () -> fillerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fillerType, AkaishiPlasmaFillerScreen::new));
    }
}
