package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiReactorControllerBlockEntity;
import com.example.akaishi.block.entity.AkaishiExhaustedBarrelBlockEntity;
import com.example.akaishi.block.entity.AkaishiReactorFuelPortBlockEntity;
import com.example.akaishi.block.entity.AkaishiReactorEnergyOutputBlockEntity;
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
 * 菜单注册 · 反应堆族（承接 {@link ModMenus} 原注册序第 22~25 位）：
 * 反应堆控制器、衰竭保存桶、反应堆燃料投放口、反应堆能量输出口。
 * <p>类内注册顺序与原 {@code ModMenus.register()} 逐位一致。
 */
final class ReactorMenuRegs {

    private ReactorMenuRegs() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void register() {
        // 反应堆控制器：10 燃料槽 + 13 数据槽（温度/成型/散热/废品/熔毁等）
        MenuType<AkaishiReactorControllerMenu> reactorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiReactorControllerBlockEntity controller) {
                return new AkaishiReactorControllerMenu(syncId, inv, controller);
            }
            return new AkaishiReactorControllerMenu(syncId, inv,
                    new SimpleContainer(AkaishiReactorControllerBlockEntity.MAX_FUEL_SLOTS),
                    new SimpleContainerData(AkaishiReactorControllerBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_REACTOR_CONTROLLER = (RegistrySupplier<MenuType<AkaishiReactorControllerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_reactor_controller"), () -> reactorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(reactorType, AkaishiReactorControllerScreen::new));

        // 衰竭保存桶：无机器槽位，仅液体量/容量数据展示
        MenuType<AkaishiExhaustedBarrelMenu> barrelType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiExhaustedBarrelBlockEntity barrel) {
                return new AkaishiExhaustedBarrelMenu(syncId, inv, barrel.data());
            }
            return AkaishiExhaustedBarrelMenu.emptyMenu(syncId, inv);
        });
        ModMenus.CHISHI_EXHAUSTED_BARREL = (RegistrySupplier<MenuType<AkaishiExhaustedBarrelMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_exhausted_barrel"), () -> barrelType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(barrelType, AkaishiExhaustedBarrelScreen::new));

        // 反应堆燃料投放口：27 格燃料罐缓冲槽 + 玩家背包
        MenuType<AkaishiReactorFuelPortMenu> fuelPortType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            if (level.getBlockEntity(pos) instanceof AkaishiReactorFuelPortBlockEntity port) {
                return new AkaishiReactorFuelPortMenu(syncId, inv, port.buffer());
            }
            return AkaishiReactorFuelPortMenu.emptyMenu(syncId, inv);
        });
        ModMenus.CHISHI_REACTOR_FUEL_PORT = (RegistrySupplier<MenuType<AkaishiReactorFuelPortMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_reactor_fuel_port"), () -> fuelPortType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fuelPortType, AkaishiReactorFuelPortScreen::new));

        // 反应堆能量输出口：无机器槽位，能量/容量数据展示
        MenuType<AkaishiReactorEnergyOutputMenu> energyOutputType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            if (level.getBlockEntity(pos) instanceof AkaishiReactorEnergyOutputBlockEntity output) {
                return new AkaishiReactorEnergyOutputMenu(syncId, inv, output.data());
            }
            return AkaishiReactorEnergyOutputMenu.emptyMenu(syncId, inv);
        });
        ModMenus.CHISHI_REACTOR_ENERGY_OUTPUT = (RegistrySupplier<MenuType<AkaishiReactorEnergyOutputMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_reactor_energy_output"), () -> energyOutputType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(energyOutputType, AkaishiReactorEnergyOutputScreen::new));
    }
}
