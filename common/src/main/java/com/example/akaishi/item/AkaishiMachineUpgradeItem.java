package com.example.akaishi.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 机器升级组件（MEK 式单格堆叠）：装入用电器升级槽，提供速度/容量加成或无线接收能力。
 * 每台用电器 3 个升级槽（速度 / 能量 / 无线接收各一格，槽位 mayPlace 互斥）；
 * 速度与能量单格最多堆叠 8 个，堆叠数即等级；无线接收升级单格 1 个即生效。
 */
public class AkaishiMachineUpgradeItem extends Item {

    public static final int MAX_STACK = 8;

    private final MachineUpgradeType type;

    public AkaishiMachineUpgradeItem(MachineUpgradeType type) {
        // 无线接收是"装/没装"的开关语义，不设等级，故封顶 1
        super(new Item.Properties().stacksTo(type == MachineUpgradeType.WIRELESS ? 1 : MAX_STACK));
        this.type = type;
    }

    public MachineUpgradeType getUpgradeType() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(switch (type) {
            case SPEED -> "gui.akaishi.upgrade.speed.hint";
            case ENERGY -> "gui.akaishi.upgrade.energy.hint";
            case WIRELESS -> "gui.akaishi.upgrade.wireless.hint";
        }));
        if (type != MachineUpgradeType.WIRELESS) {
            tooltip.add(Component.translatable("gui.akaishi.upgrade.stack_hint"));
        }
    }
}
