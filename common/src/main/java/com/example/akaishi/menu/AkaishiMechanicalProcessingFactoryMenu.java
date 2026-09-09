package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiMechanicalProcessingFactoryBlockEntity;
import com.example.akaishi.item.AkaishiMechanicalItems;
import com.example.akaishi.item.MechanicalPartItem;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.mechanical.MechanicalMachineCosts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.example.akaishi.upgrade.MachineUpgradeSlots;

/**
 * 加工制作厂菜单：升级槽 + 部位模板/材料/固态物输入 + 加工件输出 + 双能源/进度数据。
 * 槽位：0=部位模板(26,56)、1=材料(44,56)、2=固态物(62,56)、3=输出(116,56)。
 * 费用随模板的器官×部件自动计算（显示端读取统一 MechanicalMachineCosts）。
 */
public class AkaishiMechanicalProcessingFactoryMenu extends AbstractMechanicalMachineMenu {

    public static final int MACHINE_SLOTS = 4;

    private final BlockPos pos;

    public AkaishiMechanicalProcessingFactoryMenu(int id, Inventory inv, AkaishiMechanicalProcessingFactoryBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots(), be.getBlockPos());
    }

    AkaishiMechanicalProcessingFactoryMenu(int id, Inventory inv, Container container, ContainerData data, Container upgrades) {
        this(id, inv, container, data, upgrades, null);
    }

    AkaishiMechanicalProcessingFactoryMenu(int id, Inventory inv, Container container, ContainerData data, Container upgrades, BlockPos pos) {
        super(ModMenus.CHISHI_MECHANICAL_PROCESSING_FACTORY.get(), id, container, data, upgrades, MACHINE_SLOTS, inv);
        this.pos = pos;
    }

    /** 所在方块位置（C2S 制作请求包用），BE 缺失兜底时返回 null */
    public BlockPos getBlockPos() {
        return pos;
    }

    @Override
    protected void addMachineSlots() {
        // 输入：部位模板（未加工）
        addSlot(new Slot(container(), 0, 26, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(AkaishiMechanicalItems.mechanicalPartTemplate.get())
                        && !MechanicalPartItem.isProcessed(stack);
            }
        });
        // 输入：机械材料（任意 9 种，可堆叠）
        addSlot(new Slot(container(), 1, 44, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AkaishiMechanicalItems.materialIdOf(stack.getItem()) != null;
            }
        });
        // 输入：生命固态物
        addSlot(new Slot(container(), 2, 62, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.akaishiLifeEssenceSolid.get());
            }
        });
        // 输出：加工件（只出不进）
        addSlot(new Slot(container(), 3, 116, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    /** GUI 显示当前模板成本（材料份数/费用），客户端读服务端同步的配置 */
    public int materialCount(ItemStack template) {
        var part = MechanicalPartItem.getPartType(template);
        return part != null ? MechanicalMachineCosts.materialCount(part) : 0;
    }
}