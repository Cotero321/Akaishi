package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiItemTerminalBlockEntity;
import com.example.akaishi.block.entity.MiniatureTerminalBlockEntity;
import com.example.akaishi.block.entity.AkaishiMiniMatrixTerminalBlockEntity;
import com.example.akaishi.block.entity.AkaishiMiniMatrixNetworkNodeBlockEntity;
import com.example.akaishi.block.entity.AkaishiItemStorageUnitBlockEntity;
import com.example.akaishi.block.entity.AkaishiItemPortBlockEntity;
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
 * 菜单注册 · 终端与储存族（承接 {@link ModMenus} 原注册序第 69~74 位）：
 * 物品终端、微缩矩阵终端、网络节点、禁忌秘典、物品储存单元、储存无线输入口/输出口。
 * <p>类内注册顺序与原 {@code ModMenus.register()} 逐位一致。
 */
final class TerminalMenuRegs {

    private TerminalMenuRegs() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void register() {
        // 物品终端：库页 4×9 可视区（客户端只读虚拟槽）+ IP/单笔上限展示，服务端仅玩家背包槽
        MenuType<AkaishiItemTerminalMenu> itemTerminalType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiItemTerminalBlockEntity terminal) {
                return new AkaishiItemTerminalMenu(syncId, inv, terminal);
            }
            // 微缩件：同一套库页（宿主是微缩状态，不是终端方块实体）
            if (be instanceof MiniatureTerminalBlockEntity miniature && miniature.itemHost() != null) {
                return new AkaishiItemTerminalMenu(syncId, inv, miniature.itemHost());
            }
            // 方块实体缺失（跨维度/距离过远）时用空数据兜底：槽位与数据槽数量不变，避免索引错位
            return new AkaishiItemTerminalMenu(syncId, inv, null);
        });
        ModMenus.CHISHI_ITEM_TERMINAL = (RegistrySupplier<MenuType<AkaishiItemTerminalMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_item_terminal"), () -> itemTerminalType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(itemTerminalType, AkaishiItemTerminalScreen::new));

        // 微缩矩阵终端：芯片列表 / 升级装配 / 安全认证三页（芯片读数经 AkaishiMiniMatrixSync 快照）
        MenuType<AkaishiMiniMatrixTerminalMenu> miniMatrixType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiMiniMatrixTerminalBlockEntity matrix) {
                return new AkaishiMiniMatrixTerminalMenu(syncId, inv, matrix);
            }
            // 方块实体缺失（跨维度/距离过远）时用空数据兜底：槽位数量不变，避免索引错位
            return new AkaishiMiniMatrixTerminalMenu(syncId, inv, null);
        });
        ModMenus.CHISHI_MINI_MATRIX_TERMINAL = (RegistrySupplier<MenuType<AkaishiMiniMatrixTerminalMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_mini_matrix_terminal"), () -> miniMatrixType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(miniMatrixType, AkaishiMiniMatrixTerminalScreen::new));

        // 网络节点：只读展示绑定终端 / 归属者 / 子场域，外加本节点独立的「节点屏障」开关
        MenuType<AkaishiMiniMatrixNodeMenu> miniMatrixNodeType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiMiniMatrixNetworkNodeBlockEntity node) {
                return new AkaishiMiniMatrixNodeMenu(syncId, inv, node);
            }
            // 方块实体缺失（跨维度/距离过远）时用空数据兜底：本界面没有槽位，不存在索引错位
            return AkaishiMiniMatrixNodeMenu.empty(syncId, inv);
        });
        ModMenus.CHISHI_MINI_MATRIX_NODE = (RegistrySupplier<MenuType<AkaishiMiniMatrixNodeMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_mini_matrix_node"), () -> miniMatrixNodeType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(miniMatrixNodeType, AkaishiMiniMatrixNodeScreen::new));

        // 禁忌秘典：手持物品界面（无槽位、无方块实体），进度快照与条件求值由服务端下发
        MenuType<AkaishiCodexMenu> codexType = MenuRegistry.ofExtended(
                (syncId, inv, buf) -> new AkaishiCodexMenu(syncId, inv));
        ModMenus.CHISHI_CODEX = (RegistrySupplier<MenuType<AkaishiCodexMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_forbidden_codex"), () -> codexType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(codexType, AkaishiCodexScreen::new));

        // 物品储存单元：D18 只读视图（54 槽 + 占用/剩余 IP），无任何写入路径
        MenuType<AkaishiItemStorageUnitMenu> itemStorageUnitType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiItemStorageUnitBlockEntity unit) {
                return new AkaishiItemStorageUnitMenu(syncId, inv, unit);
            }
            return new AkaishiItemStorageUnitMenu(syncId, inv, null);
        });
        ModMenus.CHISHI_ITEM_STORAGE_UNIT = (RegistrySupplier<MenuType<AkaishiItemStorageUnitMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_item_storage_unit"),
                        () -> itemStorageUnitType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(itemStorageUnitType, AkaishiItemStorageUnitScreen::new));

        // 储存无线输入口/输出口（共用菜单类型）：7 数据槽（方向/绑定态/上次搬运/绑定身份短号拆 2 槽/
        // 赤能源不足标志/搬运失败原因），无机器槽；
        // 远程绑定走 AkaishiItemPortBindingSync（S2C 清单快照 + C2S 动作包）
        MenuType<AkaishiItemPortMenu> itemPortType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiItemPortBlockEntity port) {
                return new AkaishiItemPortMenu(syncId, inv, port);
            }
            // 方块实体缺失（如跨维度/距离过远）时用同尺寸空数据兜底，避免索引错位
            return new AkaishiItemPortMenu(syncId, inv,
                    new SimpleContainerData(AkaishiItemPortBlockEntity.DATA_SLOTS));
        });
        ModMenus.CHISHI_ITEM_PORT = (RegistrySupplier<MenuType<AkaishiItemPortMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_item_port"), () -> itemPortType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(itemPortType, AkaishiItemPortScreen::new));
    }
}
