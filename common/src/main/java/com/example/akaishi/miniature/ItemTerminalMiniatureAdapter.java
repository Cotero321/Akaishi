package com.example.akaishi.miniature;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.api.miniature.MiniatureTerminalAdapter;
import com.example.akaishi.api.miniature.MiniatureTerminalState;
import com.example.akaishi.api.storage.IItemTerminalHost;
import com.example.akaishi.block.entity.MiniatureTerminalBlockEntity;
import com.example.akaishi.menu.AkaishiItemTerminalMenu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

/**
 * 物品终端族的微缩适配器：把「物品终端」折进一个方块。
 * <p>
 * 界面直接复用原终端的库页菜单 —— 菜单只依赖 {@link IItemTerminalHost}，
 * 而微缩状态自己就是宿主，因此玩家感知不到形态变化（用户口径：界面不要割裂、开发量别翻倍）。
 */
public final class ItemTerminalMiniatureAdapter implements MiniatureTerminalAdapter {

    /** 族 id（存进微缩方块 NBT） */
    public static final ResourceLocation ID = new ResourceLocation(AkaishiMod.MOD_ID, "item_terminal");

    @Override
    public ResourceLocation typeId() {
        return ID;
    }

    @Override
    public MiniatureTerminalState createState(BlockEntity be, CompoundTag payload) {
        // 通用层按 BlockEntity 传参；唯一调用方是 MiniatureTerminalBlockEntity 自身（见其 apply()）
        return be instanceof MiniatureTerminalBlockEntity terminal
                ? new ItemTerminalMiniatureState(terminal, payload) : null;
    }

    @Override
    public Component displayName(CompoundTag payload) {
        return Component.translatable("block.akaishi.akaishi_item_terminal");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, BlockEntity be) {
        IItemTerminalHost host = be instanceof MiniatureTerminalBlockEntity terminal && terminal.state() != null
                ? terminal.state().itemHost() : null;
        return host == null ? null : new AkaishiItemTerminalMenu(id, inv, host);
    }

    @Override
    public boolean hasMenu() {
        return true;
    }
}
