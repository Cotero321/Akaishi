package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.wireless.IWirelessPortHost;
import com.example.akaishi.block.entity.AkaishiWirelessTerminalBlockEntity;
import com.example.akaishi.block.entity.MiniatureTerminalBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeWirelessTerminalBlockEntity;
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
 * 菜单注册 · 无线终端族（承接 {@link ModMenus} 原注册序第 56~61 位）：
 * 无线赤能源终端、无线赤能源端口、无线生命终端、无线生命端口、
 * 无线便捷终端、无线生命便捷终端（生命侧为赤版镜像）。
 * <p>类内注册顺序与原 {@code ModMenus.register()} 逐位一致。
 */
final class WirelessMenuRegs {

    private WirelessMenuRegs() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void register() {
        // ===== 无线赤能源 =====
        // 终端（外墙主方块）：15 数据槽（储能 long/成型/口统计/授权卡数/组件状态/终端ID），
        // 1 授权槽（仅安全页显示）；网络缓冲 = 方块坐标 + 初始页（安全方块直达安全卡认证页）
        MenuType<AkaishiWirelessTerminalMenu> terminalType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            // 微缩终端的扩展数据只写坐标（无页号）⇒ 缺页号时回退运行页，避免缓冲读越界
            int page = buf.isReadable() ? buf.readInt() : 0;
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            AkaishiWirelessTerminalMenu menu = be instanceof AkaishiWirelessTerminalBlockEntity t
                    ? new AkaishiWirelessTerminalMenu(syncId, inv, t)
                    : be instanceof MiniatureTerminalBlockEntity miniature && miniature.wirelessHost() != null
                            ? new AkaishiWirelessTerminalMenu(syncId, inv, miniature.wirelessHost())
                            : AkaishiWirelessTerminalMenu.emptyMenu(syncId, inv);
            menu.setInitialPage(page);
            return menu;
        });
        ModMenus.CHISHI_WIRELESS_TERMINAL = (RegistrySupplier<MenuType<AkaishiWirelessTerminalMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_wireless_terminal"), () -> terminalType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(terminalType, AkaishiWirelessTerminalScreen::new));

        // 端口（输入口/输出口共用）：10 数据槽（缓冲储能 long + 卡/终端短 ID 各拆 2 槽 + 认证态 + 方向），无机器槽
        MenuType<AkaishiWirelessPortMenu> wirelessPortType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IWirelessPortHost host) {
                return new AkaishiWirelessPortMenu(syncId, inv, host);
            }
            return new AkaishiWirelessPortMenu(syncId, inv,
                    new SimpleContainerData(com.example.akaishi.block.entity.AkaishiWirelessInputPortBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_WIRELESS_PORT = (RegistrySupplier<MenuType<AkaishiWirelessPortMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_wireless_port"), () -> wirelessPortType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(wirelessPortType, AkaishiWirelessPortScreen::new));

        // ===== 无线生命能量（赤版生命镜像，布局/槽位与赤版无线一致） =====
        // 终端（外墙主方块）：数据槽布局与赤版无线终端相同；网络缓冲 = 方块坐标 + 初始页
        MenuType<AkaishiLifeWirelessTerminalMenu> lifeTerminalType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            // 微缩终端的扩展数据只写坐标（无页号）⇒ 缺页号时回退运行页，避免缓冲读越界
            int page = buf.isReadable() ? buf.readInt() : 0;
            Level level = inv.player.level();
            BlockEntity lifeBe = level.getBlockEntity(pos);
            AkaishiLifeWirelessTerminalMenu menu = lifeBe instanceof AkaishiLifeWirelessTerminalBlockEntity t
                    ? new AkaishiLifeWirelessTerminalMenu(syncId, inv, t)
                    : lifeBe instanceof MiniatureTerminalBlockEntity miniature && miniature.wirelessHost() != null
                            ? new AkaishiLifeWirelessTerminalMenu(syncId, inv, miniature.wirelessHost())
                            : AkaishiLifeWirelessTerminalMenu.emptyMenu(syncId, inv);
            menu.setInitialPage(page);
            return menu;
        });
        ModMenus.CHISHI_LIFE_WIRELESS_TERMINAL = (RegistrySupplier<MenuType<AkaishiLifeWirelessTerminalMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_wireless_terminal"), () -> lifeTerminalType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifeTerminalType, AkaishiLifeWirelessTerminalScreen::new));

        // 端口（输入口/输出口共用）：数据槽/无机器槽布局与赤版口一致，无额外缓冲
        MenuType<AkaishiLifeWirelessPortMenu> lifeWirelessPortType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IWirelessPortHost host) {
                return new AkaishiLifeWirelessPortMenu(syncId, inv, host);
            }
            return new AkaishiLifeWirelessPortMenu(syncId, inv,
                    new SimpleContainerData(com.example.akaishi.block.entity.AkaishiLifeWirelessInputPortBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_LIFE_WIRELESS_PORT = (RegistrySupplier<MenuType<AkaishiLifeWirelessPortMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_wireless_port"), () -> lifeWirelessPortType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifeWirelessPortType, AkaishiLifeWirelessPortScreen::new));

        // 便捷终端（手持物品）：无方块实体，服务端每 tick broadcastChanges 扫背包身份卡刷新数据槽
        MenuType<AkaishiWirelessPortableTerminalMenu> portableType = MenuRegistry.ofExtended((syncId, inv, buf) ->
                new AkaishiWirelessPortableTerminalMenu(syncId, inv, inv.player));
        ModMenus.CHISHI_WIRELESS_PORTABLE_TERMINAL = (RegistrySupplier<MenuType<AkaishiWirelessPortableTerminalMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_wireless_portable_terminal"), () -> portableType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(portableType, AkaishiWirelessPortableTerminalScreen::new));

        // 生命便捷终端（手持物品，赤版生命镜像）：无方块实体，服务端每 tick 按生命族扫背包身份卡刷新数据槽
        MenuType<AkaishiLifeWirelessPortableTerminalMenu> lifePortableType = MenuRegistry.ofExtended((syncId, inv, buf) ->
                new AkaishiLifeWirelessPortableTerminalMenu(syncId, inv, inv.player));
        ModMenus.CHISHI_LIFE_WIRELESS_PORTABLE_TERMINAL = (RegistrySupplier<MenuType<AkaishiLifeWirelessPortableTerminalMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_wireless_portable_terminal"), () -> lifePortableType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifePortableType, AkaishiLifeWirelessPortableTerminalScreen::new));
    }
}
