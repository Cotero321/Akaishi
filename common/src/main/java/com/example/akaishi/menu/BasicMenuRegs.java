package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import com.example.akaishi.block.entity.AkaishiPurifierBlockEntity;
import com.example.akaishi.block.entity.AkaishiEnergyCellBlockEntity;
import com.example.akaishi.block.entity.AkaishiEnergyGeneratorBlockEntity;
import com.example.akaishi.block.entity.AkaishiEnergyCellSerializerBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeMatrixControllerBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeEnergyCellBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeEnergyCellSerializerBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeEnergyEmitterBlockEntity;
import com.example.akaishi.block.entity.AkaishiEnergyAggregatorBlockEntity;
import com.example.akaishi.block.entity.AkaishiEquipmentForgerBlockEntity;
import com.example.akaishi.block.entity.AkaishiUpgradeStationBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeFusionAnvilBlockEntity;
import com.example.akaishi.block.entity.AkaishiAutoCollectorBlockEntity;
import com.example.akaishi.block.entity.AkaishiCatalystBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifePurifierBlockEntity;
import com.example.akaishi.block.entity.AkaishiEnergyLiquefierBlockEntity;
import com.example.akaishi.block.entity.AkaishiEnergyProcessorBlockEntity;
import com.example.akaishi.block.entity.AkaishiFuelCannerBlockEntity;
import com.example.akaishi.block.entity.AkaishiFuelMixerBlockEntity;
import com.example.akaishi.block.entity.AkaishiFluidTankBlockEntity;
import com.example.akaishi.block.entity.AkaishiPlasmaTankBlockEntity;
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
 * 菜单注册 · 基础机械族（承接 {@link ModMenus} 原注册序第 1~21 位）：
 * 赤石提纯器、赤能源族（电池/发生机/储存串联器）、生命能量族（转换/储存器/串联器/发射器）、
 * 聚合器、装备打造器、升级台、生命的融合砧、自动收集器、催化器、生命提纯器、
 * 能量液化/加工、燃料装罐/混合、液体储罐与等离子储罐。
 * <p>
 * 纯机械搬迁：类内各菜单的注册顺序与原 {@code ModMenus.register()} 逐位一致；
 * 委托链见 {@code ModMenus.register()}（域间顺序 = 原顺序的分段）。
 */
final class BasicMenuRegs {

    private BasicMenuRegs() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void register() {
        // MenuType 实例可在构造期直接创建（仅封装容器工厂，不触碰注册表，此时注册表未冻结）
        MenuType<AkaishiPurifierMenu> type = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPurifierBlockEntity purifier) {
                return new AkaishiPurifierMenu(syncId, inv, purifier.inventory(), purifier.data());
            }
            // 方块实体缺失（如跨维度/距离过远）时使用空数据兜底，避免崩溃
            return new AkaishiPurifierMenu(syncId, inv,
                    new SimpleContainer(AkaishiPurifierBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiPurifierBlockEntity.DATA_SLOTS));
        });
        // 注册表延迟注册同一实例（supplier 在 RegisterEvent 时才求值）：
        // Forge 发送打开界面数据包时按注册表编码 MenuType，未注册将无法打开界面
        ModMenus.CHISHI_PURIFIER = (RegistrySupplier<MenuType<AkaishiPurifierMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_purifier"), () -> type);
        // 客户端注册界面工厂：实例已就绪，无需等待注册求值，避免构造期 get() 取值 NPE
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(type, AkaishiPurifierScreen::new));

        // 赤能源储存单元：1 便携单元充能槽 + 能量数据同步
        MenuType<AkaishiEnergyCellMenu> cellType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiEnergyCellBlockEntity cell) {
                return new AkaishiEnergyCellMenu(syncId, inv, cell.cellSlot(), cell.data());
            }
            return AkaishiEnergyCellMenu.emptyMenu(syncId, inv);
        });
        ModMenus.CHISHI_ENERGY_CELL = (RegistrySupplier<MenuType<AkaishiEnergyCellMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_cell"), () -> cellType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(cellType, AkaishiEnergyCellScreen::new));

        // 赤能源发生机：1 燃料槽 + 能量同步
        MenuType<AkaishiEnergyGeneratorMenu> genType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiEnergyGeneratorBlockEntity gen) {
                return new AkaishiEnergyGeneratorMenu(syncId, inv, gen);
            }
            return new AkaishiEnergyGeneratorMenu(syncId, inv,
                    new SimpleContainer(AkaishiEnergyGeneratorBlockEntity.SLOT_COUNT),
                    // 占位尺寸需与服务端一致：能量/燃烧/总燃烧（各 2 槽）+ 升级数 共 7 槽
                    new SimpleContainerData(AkaishiEnergyGeneratorBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_ENERGY_GENERATOR = (RegistrySupplier<MenuType<AkaishiEnergyGeneratorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_generator"), () -> genType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(genType, AkaishiEnergyGeneratorScreen::new));

        // 赤能源储存串联器：无容器槽位，同步总能量/总容量（long 4 槽）+ 结构状态
        MenuType<AkaishiEnergyCellSerializerMenu> serializerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiEnergyCellSerializerBlockEntity serializer) {
                return new AkaishiEnergyCellSerializerMenu(syncId, inv, serializer.data());
            }
            return AkaishiEnergyCellSerializerMenu.emptyMenu(syncId, inv);
        });
        ModMenus.CHISHI_ENERGY_CELL_SERIALIZER = (RegistrySupplier<MenuType<AkaishiEnergyCellSerializerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_cell_serializer"), () -> serializerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(serializerType, AkaishiEnergyCellSerializerScreen::new));

        // 生命转换（生命转换矩阵控制器）：无容器槽位，同步赤能源+生命能量+结构状态
        MenuType<AkaishiLifeConverterMenu> lifeType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifeMatrixControllerBlockEntity controller) {
                return new AkaishiLifeConverterMenu(syncId, inv, controller.data());
            }
            return AkaishiLifeConverterMenu.emptyMenu(syncId, inv);
        });
        ModMenus.CHISHI_LIFE_CONVERTER = (RegistrySupplier<MenuType<AkaishiLifeConverterMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_converter"), () -> lifeType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifeType, AkaishiLifeConverterScreen::new));

        // 生命能量储存器（单方块电池）：无机器槽位，同步生命能量/容量（long 4 槽）
        MenuType<AkaishiLifeEnergyCellMenu> lifeCellType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifeEnergyCellBlockEntity cell) {
                return new AkaishiLifeEnergyCellMenu(syncId, inv, cell.data());
            }
            return AkaishiLifeEnergyCellMenu.emptyMenu(syncId, inv);
        });
        ModMenus.CHISHI_LIFE_ENERGY_CELL = (RegistrySupplier<MenuType<AkaishiLifeEnergyCellMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_energy_cell"), () -> lifeCellType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifeCellType, AkaishiLifeEnergyCellScreen::new));

        // 生命储存串联器：无容器槽位，同步总生命能量/总容量（long 4 槽）+ 结构状态
        MenuType<AkaishiLifeEnergyCellSerializerMenu> lifeSerializerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifeEnergyCellSerializerBlockEntity serializer) {
                return new AkaishiLifeEnergyCellSerializerMenu(syncId, inv, serializer.data());
            }
            return AkaishiLifeEnergyCellSerializerMenu.emptyMenu(syncId, inv);
        });
        ModMenus.CHISHI_LIFE_ENERGY_CELL_SERIALIZER = (RegistrySupplier<MenuType<AkaishiLifeEnergyCellSerializerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_energy_cell_serializer"), () -> lifeSerializerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifeSerializerType, AkaishiLifeEnergyCellSerializerScreen::new));

        // 生命能量发射器：无机器槽位，同步生命能量/容量（long 4 槽）+ 绑定坐标
        MenuType<AkaishiLifeEnergyEmitterMenu> lifeEmitterType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifeEnergyEmitterBlockEntity emitter) {
                return new AkaishiLifeEnergyEmitterMenu(syncId, inv, emitter.data(), emitter.getTarget());
            }
            return AkaishiLifeEnergyEmitterMenu.emptyMenu(syncId, inv);
        });
        ModMenus.CHISHI_LIFE_ENERGY_EMITTER = (RegistrySupplier<MenuType<AkaishiLifeEnergyEmitterMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_energy_emitter"), () -> lifeEmitterType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifeEmitterType, AkaishiLifeEnergyEmitterScreen::new));

        // 赤石能量聚合器
        MenuType<AkaishiEnergyAggregatorMenu> aggregatorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof AkaishiEnergyAggregatorBlockEntity be
                    ? new AkaishiEnergyAggregatorMenu(syncId, inv, be)
                    : new AkaishiEnergyAggregatorMenu(syncId, inv, new net.minecraft.world.SimpleContainer(2),
                            new net.minecraft.world.inventory.SimpleContainerData(AkaishiEnergyAggregatorBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_ENERGY_AGGREGATOR = (RegistrySupplier<MenuType<AkaishiEnergyAggregatorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_aggregator"), () -> aggregatorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(aggregatorType, AkaishiEnergyAggregatorScreen::new));

        // 赤石装备打造器
        MenuType<AkaishiEquipmentForgerMenu> forgerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof AkaishiEquipmentForgerBlockEntity be
                    ? new AkaishiEquipmentForgerMenu(syncId, inv, be)
                    // 占位尺寸需与服务端一致：能量/容量各占高低两槽，共 DATA_SIZE=12 槽
                    : new AkaishiEquipmentForgerMenu(syncId, inv,
                            new net.minecraft.world.SimpleContainer(AkaishiEquipmentForgerBlockEntity.SLOT_COUNT),
                            new net.minecraft.world.inventory.SimpleContainerData(AkaishiEquipmentForgerBlockEntity.DATA_SIZE));
        });
        ModMenus.CHISHI_EQUIPMENT_FORGER = (RegistrySupplier<MenuType<AkaishiEquipmentForgerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_equipment_forger"), () -> forgerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(forgerType, AkaishiEquipmentForgerScreen::new));

        // 赤红升级台
        MenuType<AkaishiUpgradeStationMenu> upgradeType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof AkaishiUpgradeStationBlockEntity be
                    ? new AkaishiUpgradeStationMenu(syncId, inv, be)
                    : new AkaishiUpgradeStationMenu(syncId, inv, null);
        });
        ModMenus.CHISHI_UPGRADE_STATION = (RegistrySupplier<MenuType<AkaishiUpgradeStationMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_upgrade_station"), () -> upgradeType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(upgradeType, AkaishiUpgradeStationScreen::new));

        // 生命的融合砧：赤石护甲 + 融合锭 → 生命融合护甲（无能量/进度数据，纯槽位合成）
        MenuType<AkaishiLifeFusionAnvilMenu> fusionAnvilType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof AkaishiLifeFusionAnvilBlockEntity anvil
                    ? new AkaishiLifeFusionAnvilMenu(syncId, inv, anvil)
                    : new AkaishiLifeFusionAnvilMenu(syncId, inv, null);
        });
        ModMenus.CHISHI_LIFE_FUSION_ANVIL = (RegistrySupplier<MenuType<AkaishiLifeFusionAnvilMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_fusion_anvil"), () -> fusionAnvilType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fusionAnvilType, AkaishiLifeFusionAnvilScreen::new));

        // 自动收集器：27 槽存储 + 能量/进度同步
        MenuType<AkaishiAutoCollectorMenu> collectorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof AkaishiAutoCollectorBlockEntity be
                    ? new AkaishiAutoCollectorMenu(syncId, inv, be)
                    : new AkaishiAutoCollectorMenu(syncId, inv,
                            new net.minecraft.world.SimpleContainer(AkaishiAutoCollectorBlockEntity.STORAGE_SIZE),
                            new net.minecraft.world.inventory.SimpleContainerData(AkaishiAutoCollectorBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_AUTO_COLLECTOR = (RegistrySupplier<MenuType<AkaishiAutoCollectorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_auto_collector"), () -> collectorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(collectorType, AkaishiAutoCollectorScreen::new));

        // 赤石催化器：无机器槽，仅玩家背包 + 能量/工作状态数据同步
        MenuType<AkaishiCatalystMenu> catalystType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof AkaishiCatalystBlockEntity be
                    ? new AkaishiCatalystMenu(syncId, inv, be.data())
                    : new AkaishiCatalystMenu(syncId, inv,
                            new net.minecraft.world.inventory.SimpleContainerData(AkaishiCatalystBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_CATALYST = (RegistrySupplier<MenuType<AkaishiCatalystMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_catalyst"), () -> catalystType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(catalystType, AkaishiCatalystScreen::new));

        // 生命能量提纯器：1 输出槽 + 赤能源/生命能量/进度数据同步
        MenuType<AkaishiLifePurifierMenu> lifePurifierType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifePurifierBlockEntity purifier) {
                return new AkaishiLifePurifierMenu(syncId, inv, purifier.inventory(), purifier.data(),
                        purifier.getUpgradeSlots());
            }
            return new AkaishiLifePurifierMenu(syncId, inv,
                    new SimpleContainer(AkaishiLifePurifierBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiLifePurifierBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_LIFE_PURIFIER = (RegistrySupplier<MenuType<AkaishiLifePurifierMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_purifier"), () -> lifePurifierType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifePurifierType, AkaishiLifePurifierScreen::new));

        // 能量液化装置：1 输入槽（下界之星/凋零玫瑰）+ 赤能源/双液体罐/进度数据同步
        MenuType<AkaishiEnergyLiquefierMenu> liquefierType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiEnergyLiquefierBlockEntity liquefier) {
                return new AkaishiEnergyLiquefierMenu(syncId, inv, liquefier);
            }
            return new AkaishiEnergyLiquefierMenu(syncId, inv,
                    new SimpleContainer(AkaishiEnergyLiquefierBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiEnergyLiquefierBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots());
        });
        ModMenus.CHISHI_ENERGY_LIQUEFIER = (RegistrySupplier<MenuType<AkaishiEnergyLiquefierMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_liquefier"), () -> liquefierType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(liquefierType, AkaishiEnergyLiquefierScreen::new));

        // 能量加工器：1 输入槽（生命固态物）+ 赤能源/双输入罐/双输出罐/进度数据同步
        MenuType<AkaishiEnergyProcessorMenu> processorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiEnergyProcessorBlockEntity processor) {
                return new AkaishiEnergyProcessorMenu(syncId, inv, processor);
            }
            return new AkaishiEnergyProcessorMenu(syncId, inv,
                    new SimpleContainer(AkaishiEnergyProcessorBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiEnergyProcessorBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots());
        });
        ModMenus.CHISHI_ENERGY_PROCESSOR = (RegistrySupplier<MenuType<AkaishiEnergyProcessorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy_processor"), () -> processorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(processorType, AkaishiEnergyProcessorScreen::new));

        // 燃料装罐机：1 空罐输入槽 + 1 满罐输出槽 + 输入液体量数据
        MenuType<AkaishiFuelCannerMenu> cannerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            String fuelId = buf.readUtf();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiFuelCannerBlockEntity canner) {
                return new AkaishiFuelCannerMenu(syncId, inv, canner, fuelId);
            }
            return new AkaishiFuelCannerMenu(syncId, inv,
                    new SimpleContainer(AkaishiFuelCannerBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiFuelCannerBlockEntity.DATA_SLOTS), fuelId);
        });
        ModMenus.CHISHI_FUEL_CANNER = (RegistrySupplier<MenuType<AkaishiFuelCannerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fuel_canner"), () -> cannerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(cannerType, AkaishiFuelCannerScreen::new));

        // 燃料混合器：无机器槽位（纯液体调和），9 数据槽（能量/双输入/输出/进度）
        MenuType<AkaishiFuelMixerMenu> mixerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiFuelMixerBlockEntity mixer) {
                return new AkaishiFuelMixerMenu(syncId, inv, mixer);
            }
            return new AkaishiFuelMixerMenu(syncId, inv,
                    new SimpleContainerData(AkaishiFuelMixerBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots());
        });
        ModMenus.CHISHI_FUEL_MIXER = (RegistrySupplier<MenuType<AkaishiFuelMixerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fuel_mixer"), () -> mixerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(mixerType, AkaishiFuelMixerScreen::new));

        // 液体储罐：无机器槽位，仅液体量/容量数据展示
        MenuType<AkaishiFluidTankMenu> tankType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiFluidTankBlockEntity tank) {
                return new AkaishiFluidTankMenu(syncId, inv, tank.data());
            }
            return AkaishiFluidTankMenu.emptyMenu(syncId, inv);
        });
        ModMenus.CHISHI_FLUID_TANK = (RegistrySupplier<MenuType<AkaishiFluidTankMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fluid_tank"), () -> tankType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(tankType, AkaishiFluidTankScreen::new));

        // 等离子体燃料储罐：无机器槽位，复用液体储罐菜单与界面（独立菜单类型）
        MenuType<AkaishiFluidTankMenu> plasmaTankType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPlasmaTankBlockEntity tank) {
                return new AkaishiFluidTankMenu(ModMenus.CHISHI_PLASMA_TANK.get(), syncId, inv, tank.data());
            }
            return new AkaishiFluidTankMenu(ModMenus.CHISHI_PLASMA_TANK.get(), syncId, inv,
                    new SimpleContainerData(AkaishiPlasmaTankBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_PLASMA_TANK = (RegistrySupplier<MenuType<AkaishiFluidTankMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_plasma_tank"), () -> plasmaTankType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(plasmaTankType, AkaishiFluidTankScreen::new));
    }
}
