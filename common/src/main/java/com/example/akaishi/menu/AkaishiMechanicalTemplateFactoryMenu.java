package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiMechanicalTemplateFactoryBlockEntity;
import com.example.akaishi.item.AkaishiMechanicalItems;
import com.example.akaishi.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 模板制造厂菜单：升级槽 + 通用模板/固态物/DNA 输入 + 部位模板输出 + 双能源/进度/选择数据。
 * 槽位：0=通用模板(26,56)、1=固态物(44,56)、2=DNA(62,56)、3=输出(116,56)，只读输出。
 * 选择数据：data 5=器官序数、data 6=部件序数（C2S 包更新）。
 */
public class AkaishiMechanicalTemplateFactoryMenu extends AbstractMechanicalMachineMenu {

    /** 机器槽数（输入 3 + 输出 1） */
    public static final int MACHINE_SLOTS = 4;

    private final BlockPos pos;

    public AkaishiMechanicalTemplateFactoryMenu(int id, Inventory inv, AkaishiMechanicalTemplateFactoryBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots(), be.getBlockPos());
    }

    AkaishiMechanicalTemplateFactoryMenu(int id, Inventory inv, Container container, ContainerData data, Container upgrades) {
        this(id, inv, container, data, upgrades, null);
    }

    AkaishiMechanicalTemplateFactoryMenu(int id, Inventory inv, Container container, ContainerData data, Container upgrades, BlockPos pos) {
        super(ModMenus.CHISHI_MECHANICAL_TEMPLATE_FACTORY.get(), id, container, data, upgrades, MACHINE_SLOTS, inv);
        this.pos = pos;
    }

    /** 所在方块位置（C2S 选择包用），BE 缺失兜底时返回 null */
    public BlockPos getBlockPos() {
        return pos;
    }

    @Override
    protected void addMachineSlots() {
        // 输入：通用部件塑形模板 / 生命固态物 / DNA 来源
        addSlot(new Slot(container(), 0, 26, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(AkaishiMechanicalItems.genericPartMould.get());
            }
        });
        addSlot(new Slot(container(), 1, 44, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.akaishiLifeEssenceSolid.get());
            }
        });
        addSlot(new Slot(container(), 2, 62, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AkaishiMechanicalTemplateFactoryBlockEntity.isDnaSource(stack);
            }
        });
        // 输出：部位模板（只出不进）
        addSlot(new Slot(container(), 3, 116, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    public int getSelectedOrgan() {
        return data.get(AkaishiMechanicalTemplateFactoryBlockEntity.DATA_SELECTED_ORGAN);
    }

    public int getSelectedPart() {
        return data.get(AkaishiMechanicalTemplateFactoryBlockEntity.DATA_SELECTED_PART);
    }
}