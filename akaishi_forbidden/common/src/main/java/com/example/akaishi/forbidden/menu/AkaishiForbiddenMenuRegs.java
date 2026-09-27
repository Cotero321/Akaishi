package com.example.akaishi.forbidden.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.forbidden.block.entity.AkaishiLifeFusionAnvilBlockEntity;
import com.example.akaishi.forbidden.block.entity.AkaishiMotherAltarBlockEntity;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 菜单注册 · 禁忌模块（P3d 随生命融合 / 母神祭坛从本体 {@code BasicMenuRegs} / {@code MachineMenuRegs} 迁入）。
 *
 * <p>菜单类型字段原在本体门面 {@code ModMenus.CHISHI_LIFE_FUSION_ANVIL} / {@code CHISHI_MOTHER_ALTAR}，
 * 因禁忌模块不可反向依赖本体，故随注册一并迁到本类；注册 id 与 screen factory 与迁前逐字一致
 * （{@code akaishi:akaishi_life_fusion_anvil} / {@code akaishi:akaishi_mother_altar}，
 * 命名空间仍为三模块共用的 {@code akaishi:}）。
 */
public final class AkaishiForbiddenMenuRegs {

    /** 生命的融合砧菜单类型（赤石护甲 + 融合锭 → 生命融合护甲） */
    public static RegistrySupplier<MenuType<AkaishiLifeFusionAnvilMenu>> CHISHI_LIFE_FUSION_ANVIL;

    /** 合并母神祭坛菜单类型（结构等级展示 + 单物品供奉槽） */
    public static RegistrySupplier<MenuType<AkaishiMotherAltarMenu>> CHISHI_MOTHER_ALTAR;

    private AkaishiForbiddenMenuRegs() {
    }

    /** 注册菜单类型 + 客户端 screen factory（由 {@code AkaishiForbiddenMod.init()} 调用） */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register() {
        // 生命的融合砧：赤石护甲 + 融合锭 → 生命融合护甲（无能量/进度数据，纯槽位合成）
        MenuType<AkaishiLifeFusionAnvilMenu> fusionAnvilType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            return level.getBlockEntity(pos) instanceof AkaishiLifeFusionAnvilBlockEntity anvil
                    ? new AkaishiLifeFusionAnvilMenu(syncId, inv, anvil)
                    : new AkaishiLifeFusionAnvilMenu(syncId, inv, null);
        });
        CHISHI_LIFE_FUSION_ANVIL = (RegistrySupplier<MenuType<AkaishiLifeFusionAnvilMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_fusion_anvil"), () -> fusionAnvilType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(fusionAnvilType, AkaishiLifeFusionAnvilScreen::new));

        // 合并母神祭坛：同步结构等级（1 槽）+ 单物品供奉槽
        MenuType<AkaishiMotherAltarMenu> motherAltarType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiMotherAltarBlockEntity altar) {
                return new AkaishiMotherAltarMenu(syncId, inv, altar.altarSlot(), altar.data());
            }
            return AkaishiMotherAltarMenu.emptyMenu(syncId, inv);
        });
        CHISHI_MOTHER_ALTAR = (RegistrySupplier<MenuType<AkaishiMotherAltarMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_mother_altar"), () -> motherAltarType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(motherAltarType, AkaishiMotherAltarScreen::new));
    }
}
