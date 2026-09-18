package com.example.akaishi.api.miniature;

import com.example.akaishi.block.entity.MiniatureTerminalBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import org.jetbrains.annotations.Nullable;

/**
 * 终端微缩适配器（各族一支，注册进 {@link MiniatureTerminalRegistry}）。
 * <p>
 * 微缩机制本身<b>通用</b>：方块、持久化、能力转发、掉落保留数据全在通用层；
 * 各族只提供三件事 —— ① 用存盘数据造出本族的状态对象；② 本族微缩方块的界面入口；
 * ③ 本族的运行逻辑（如需）。因此新增终端族只需加一支适配器，通用层零改动（OCP）。
 * <p>
 * <b>只对终端开放</b>：普通机器不注册适配器，也就无法被微缩。
 */
public interface MiniatureTerminalAdapter {

    /** 类型 id（存进微缩方块 NBT；决定用哪支适配器还原状态） */
    ResourceLocation typeId();

    /** 从存盘数据还原本族状态（脏数据不允许抛异常拖垮方块加载，实现方需自行兜底） */
    MiniatureTerminalState createState(MiniatureTerminalBlockEntity be, CompoundTag payload);

    /** 微缩方块的显示名（界面标题 / 方块名后缀无关，仅用于容器名） */
    Component displayName(CompoundTag payload);

    /** 右键界面；不提供界面的族返回 null */
    @Nullable
    default AbstractContainerMenu createMenu(int id, Inventory inv, MiniatureTerminalBlockEntity be) {
        return null;
    }

    /**
     * 本族是否有右键界面。
     * <p>
     * 必须与本族 {@link #createMenu} 的实际能力一致：方块据此决定是否打开菜单，
     * 若此处返回 true 而 {@code createMenu} 返回 null，会在菜单打开路径上踩到空指针。
     */
    default boolean hasMenu() {
        return false;
    }
}
