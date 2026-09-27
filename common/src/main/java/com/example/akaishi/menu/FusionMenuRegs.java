package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiFusionControllerBlockEntity;
import com.example.akaishi.block.entity.AkaishiFusionItemInputPortBlockEntity;
import com.example.akaishi.block.entity.AkaishiFusionItemOutputPortBlockEntity;
import com.example.akaishi.block.entity.AkaishiFusionEnergyOutputBlockEntity;
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
 * 菜单注册 · 聚变堆族（承接 {@link ModMenus} 原注册序第 62~64 位）：
 * 聚变控制器、聚变物品输入/输出口、聚变能量输出口。
 * <p>类内注册顺序与原 {@code ModMenus.register()} 逐位一致。
 */
final class FusionMenuRegs {

    private FusionMenuRegs() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void register() {
        // ===== 聚变堆 =====
        // 控制器：4 燃料槽 + 10 散热片槽 + 16 数据槽，三页界面（散热片槽直连控制器容器）
        MenuType<AkaishiFusionControllerMenu> fusionType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            if (level.getBlockEntity(pos) instanceof AkaishiFusionControllerBlockEntity controller) {
                return new AkaishiFusionControllerMenu(syncId, inv, controller);
            }
            return new AkaishiFusionControllerMenu(syncId, inv,
                    new SimpleContainer(AkaishiFusionControllerBlockEntity.MAX_FUEL_SLOTS),
                    new SimpleContainerData(AkaishiFusionControllerBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_FUSION_CONTROLLER = (RegistrySupplier<MenuType<AkaishiFusionControllerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fusion_controller"), () -> fusionType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fusionType, AkaishiFusionControllerScreen::new));

        // 物品输入/输出口（共用菜单类型）：27 格缓冲槽，缓冲类型由方块实体传入
        MenuType<AkaishiFusionItemPortMenu> fusionPortType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            if (level.getBlockEntity(pos) instanceof AkaishiFusionItemInputPortBlockEntity port) {
                return new AkaishiFusionItemPortMenu(syncId, inv, port.buffer(), AkaishiFusionItemPortMenu.BufferKind.INPUT_RODS);
            }
            if (level.getBlockEntity(pos) instanceof AkaishiFusionItemOutputPortBlockEntity port) {
                return new AkaishiFusionItemPortMenu(syncId, inv, port.buffer(), AkaishiFusionItemPortMenu.BufferKind.OUTPUT_ASH);
            }
            return AkaishiFusionItemPortMenu.emptyMenu(syncId, inv);
        });
        ModMenus.CHISHI_FUSION_ITEM_PORT = (RegistrySupplier<MenuType<AkaishiFusionItemPortMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fusion_item_port"), () -> fusionPortType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fusionPortType, AkaishiFusionItemPortScreen::new));

        // 能量输出口：无机器槽位，能量/容量数据展示
        MenuType<AkaishiFusionEnergyOutputMenu> fusionEnergyType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            if (level.getBlockEntity(pos) instanceof AkaishiFusionEnergyOutputBlockEntity output) {
                return new AkaishiFusionEnergyOutputMenu(syncId, inv, output.data());
            }
            return AkaishiFusionEnergyOutputMenu.emptyMenu(syncId, inv);
        });
        ModMenus.CHISHI_FUSION_ENERGY_OUTPUT = (RegistrySupplier<MenuType<AkaishiFusionEnergyOutputMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_fusion_energy_output"), () -> fusionEnergyType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fusionEnergyType, AkaishiFusionEnergyOutputScreen::new));
    }
}
