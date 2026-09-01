package com.example.akaishi.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 机器升级组件（MEK 式单格堆叠）：装入用电器升级槽，提供速度/容量加成。
 * 每台用电器 2 个升级槽（速度/能量各一格），槽位 mayPlace 互斥，
 * 单格最多堆叠 8 个，堆叠数即等级：速度每级 +12.5%（封顶 +100%），能量每级 +50% 容量（封顶 +400%）。
 */
public class AkaishiMachineUpgradeItem extends Item {

    public static final int MAX_STACK = 8;

    private final MachineUpgradeType type;

    public AkaishiMachineUpgradeItem(MachineUpgradeType type) {
        super(new Item.Properties().stacksTo(MAX_STACK));
        this.type = type;
    }

    public MachineUpgradeType getUpgradeType() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (type == MachineUpgradeType.SPEED) {
            tooltip.add(Component.translatable("gui.akaishi.upgrade.speed.hint"));
        } else {
            tooltip.add(Component.translatable("gui.akaishi.upgrade.energy.hint"));
        }
        tooltip.add(Component.translatable("gui.akaishi.upgrade.stack_hint"));
    }
}
