package com.example.akaishi.item;

import com.example.akaishi.menu.AkaishiLifeWirelessPortableTerminalMenu;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 无线生命便捷终端：手持右键打开只读遥控面板（生命族，赤版便携终端的生命镜像）。
 * <p>
 * 服务端每 tick 扫描玩家背包中的第一张身份卡，按 {@link com.example.akaishi.wireless.WirelessFamily#LIFE}
 * 族反查授权该卡的在线生命终端，把其状态（成型/储能/口统计/卡与终端短 ID）同步到界面。
 * 手持终端不传输能量，仅作状态面板；身份卡与赤能源体系共用（一张卡不可跨族命中，族隔离）。
 */
public class AkaishiLifeWirelessPortableTerminalItem extends Item {

    public AkaishiLifeWirelessPortableTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            MenuRegistry.openExtendedMenu(serverPlayer, new ExtendedMenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("item.akaishi.akaishi_life_wireless_portable_terminal");
                }

                @Override
                public void saveExtraData(FriendlyByteBuf buf) {
                    // 无额外数据：菜单直接扫玩家背包身份卡
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new AkaishiLifeWirelessPortableTerminalMenu(id, inv, p);
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.akaishi.life_wireless.portable_terminal_hint"));
    }
}
