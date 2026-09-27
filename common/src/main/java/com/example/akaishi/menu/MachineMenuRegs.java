package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import com.example.akaishi.block.entity.AbstractMechanicalMachineBlockEntity;
import com.example.akaishi.block.entity.AkaishiMaterialFuserBlockEntity;
import com.example.akaishi.block.entity.AkaishiMechanicalTemplateFactoryBlockEntity;
import com.example.akaishi.block.entity.AkaishiMechanicalProcessingFactoryBlockEntity;
import com.example.akaishi.block.entity.AkaishiMechanicalAssemblyStationBlockEntity;
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
 * 菜单注册 · 机械改造族（承接 {@link ModMenus} 原注册序第 65~67 位，另含材料融合器）：
 * 机械改造模板制造厂、加工制作厂、组装加工台、材料融合器。
 * <p>类内注册顺序与原 {@code ModMenus.register()} 逐位一致；合并母神祭坛已于 P3d 迁往 akaishi_forbidden。
 */
final class MachineMenuRegs {

    private MachineMenuRegs() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void register() {
        // ===== 机械改造三机 =====
        // 模板制造厂
        MenuType<AkaishiMechanicalTemplateFactoryMenu> mechTemplateType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            Level level = inv.player.level();
            if (level.getBlockEntity(buf.readBlockPos()) instanceof AkaishiMechanicalTemplateFactoryBlockEntity be) {
                return new AkaishiMechanicalTemplateFactoryMenu(syncId, inv, be);
            }
            return new AkaishiMechanicalTemplateFactoryMenu(syncId, inv,
                    new SimpleContainer(AkaishiMechanicalTemplateFactoryBlockEntity.SLOT_COUNT),
                    // 占位尺寸需与服务端一致：基类 10 槽 + 子类 2 槽
                    new SimpleContainerData(12), new MachineUpgradeSlots());
        });
        ModMenus.CHISHI_MECHANICAL_TEMPLATE_FACTORY = (RegistrySupplier<MenuType<AkaishiMechanicalTemplateFactoryMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_mechanical_template_factory"), () -> mechTemplateType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(mechTemplateType, AkaishiMechanicalTemplateFactoryScreen::new));
        // 加工制作厂
        MenuType<AkaishiMechanicalProcessingFactoryMenu> mechProcessType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            Level level = inv.player.level();
            if (level.getBlockEntity(buf.readBlockPos()) instanceof AkaishiMechanicalProcessingFactoryBlockEntity be) {
                return new AkaishiMechanicalProcessingFactoryMenu(syncId, inv, be);
            }
            return new AkaishiMechanicalProcessingFactoryMenu(syncId, inv,
                    new SimpleContainer(AkaishiMechanicalProcessingFactoryBlockEntity.SLOT_COUNT),
                    // 占位尺寸需与服务端一致：基类 10 槽（子类无额外槽）
                    new SimpleContainerData(AbstractMechanicalMachineBlockEntity.DATA_EXTRA_BASE), new MachineUpgradeSlots());
        });
        ModMenus.CHISHI_MECHANICAL_PROCESSING_FACTORY = (RegistrySupplier<MenuType<AkaishiMechanicalProcessingFactoryMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_mechanical_processing_factory"), () -> mechProcessType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(mechProcessType, AkaishiMechanicalProcessingFactoryScreen::new));
        // 组装加工台
        MenuType<AkaishiMechanicalAssemblyStationMenu> mechAssemblyType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            Level level = inv.player.level();
            if (level.getBlockEntity(buf.readBlockPos()) instanceof AkaishiMechanicalAssemblyStationBlockEntity be) {
                return new AkaishiMechanicalAssemblyStationMenu(syncId, inv, be);
            }
            return new AkaishiMechanicalAssemblyStationMenu(syncId, inv,
                    new SimpleContainer(AkaishiMechanicalAssemblyStationBlockEntity.SLOT_COUNT),
                    // 占位尺寸需与服务端一致：基类 10 槽（子类无额外槽）
                    new SimpleContainerData(10), new MachineUpgradeSlots());
        });
        ModMenus.CHISHI_MECHANICAL_ASSEMBLY_STATION = (RegistrySupplier<MenuType<AkaishiMechanicalAssemblyStationMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_mechanical_assembly_station"), () -> mechAssemblyType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(mechAssemblyType, AkaishiMechanicalAssemblyStationScreen::new));

        // 材料融合器（两输入 + 一输出 + 三升级槽）
        MenuType<AkaishiMaterialFuserMenu> materialFuserType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            Level level = inv.player.level();
            if (level.getBlockEntity(buf.readBlockPos()) instanceof AkaishiMaterialFuserBlockEntity be) {
                return new AkaishiMaterialFuserMenu(syncId, inv, be);
            }
            return new AkaishiMaterialFuserMenu(syncId, inv,
                    new SimpleContainer(AkaishiMaterialFuserBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiMaterialFuserBlockEntity.DATA_SLOTS), new MachineUpgradeSlots());
        });
        ModMenus.CHISHI_MATERIAL_FUSER = (RegistrySupplier<MenuType<AkaishiMaterialFuserMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_material_fuser"), () -> materialFuserType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(materialFuserType, AkaishiMaterialFuserScreen::new));

        // 合并母神祭坛随祭坛迁往 akaishi_forbidden（见 AkaishiForbiddenMenuRegs）
    }
}
