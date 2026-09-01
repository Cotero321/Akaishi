package com.example.akaishi.life.body;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 玩家躯体状态（capability 接口）：
 * 9 个槽位的器官植入状态 + 每部位排斥值。
 * 移植需槽位为空；摘除造成生命值损失（代价模型：装仿生部件前先承受摘除伤害）。
 * 数据经 NBT 持久化到玩家，排斥值由后续基因系统驱动。
 * common 不依赖 Forge，NBT 序列化方法由平台 capability 包装适配。
 */
public interface IPlayerBodyState {

    /** 当前槽位移植的器官（无则为空物品） */
    ItemStack getOrgan(BodySlot slot);

    /** 槽位是否已被占用 */
    boolean isOccupied(BodySlot slot);

    /** 移植器官：槽位必须为空才可移植，成功返回 true */
    boolean implantOrgan(BodySlot slot, ItemStack organ);

    /**
     * 摘除器官：返回被摘除的器官（空物品表示无）。
     * 摘除瞬间对玩家造成无视护甲的生命值损失（创造模式豁免）。
     */
    ItemStack extractOrgan(Player player, BodySlot slot);

    /** 该部位的排斥值（0-100，越高越危险） */
    int getRejection(BodySlot slot);

    void setRejection(BodySlot slot, int value);

    /** 累加排斥值，超出 0-100 自动钳制 */
    void addRejection(BodySlot slot, int amount);

    /** 持久化为 NBT（玩家存档用） */
    CompoundTag save();

    /** 从 NBT 恢复 */
    void load(CompoundTag tag);
}
