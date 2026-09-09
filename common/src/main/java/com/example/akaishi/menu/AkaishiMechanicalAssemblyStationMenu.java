package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiMechanicalAssemblyStationBlockEntity;
import com.example.akaishi.item.AkaishiMechanicalItems;
import com.example.akaishi.item.MechanicalPartItem;
import com.example.akaishi.life.mechanical.MechanicalPartType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.example.akaishi.upgrade.MachineUpgradeSlots;

/**
 * 组装加工台菜单：升级槽 + 四部件输入（核心/模块/外壳/散热，须加工件）+ 成品输出 + 双能源/进度数据。
 * 槽位：0=核心(20,56)、1=模块(38,56)、2=外壳(56,56)、3=散热(74,56)、4=输出(116,56)。
 */
public class AkaishiMechanicalAssemblyStationMenu extends AbstractMechanicalMachineMenu {

    public static final int MACHINE_SLOTS = 5;

    private final BlockPos pos;

    public AkaishiMechanicalAssemblyStationMenu(int id, Inventory inv, AkaishiMechanicalAssemblyStationBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots(), be.getBlockPos());
    }

    AkaishiMechanicalAssemblyStationMenu(int id, Inventory inv, Container container, ContainerData data, Container upgrades) {
        this(id, inv, container, data, upgrades, null);
    }

    AkaishiMechanicalAssemblyStationMenu(int id, Inventory inv, Container container, ContainerData data, Container upgrades, BlockPos pos) {
        super(ModMenus.CHISHI_MECHANICAL_ASSEMBLY_STATION.get(), id, container, data, upgrades, MACHINE_SLOTS, inv);
        this.pos = pos;
    }

    /** 所在方块位置（C2S 制作请求包用），BE 缺失兜底时返回 null */
    public BlockPos getBlockPos() {
        return pos;
    }

    @Override
    protected void addMachineSlots() {
        addPartSlot(0, 20, MechanicalPartType.CORE);
        addPartSlot(1, 38, MechanicalPartType.MODULE);
        addPartSlot(2, 56, MechanicalPartType.SHELL);
        addPartSlot(3, 74, MechanicalPartType.COOLING);
        // 输出：成品器官（只出不进）
        addSlot(new Slot(container(), AkaishiMechanicalAssemblyStationBlockEntity.SLOT_OUTPUT, 116, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    /** 部件槽只收对应类型的加工件 */
    private void addPartSlot(int index, int x, MechanicalPartType part) {
        addSlot(new Slot(container(), index, x, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(AkaishiMechanicalItems.mechanicalProcessedPart.get())
                        && MechanicalPartItem.isProcessed(stack)
                        && MechanicalPartItem.getPartType(stack) == part;
            }
        });
    }
}