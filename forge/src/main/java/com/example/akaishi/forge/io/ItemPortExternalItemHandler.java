package com.example.akaishi.forge.io;

import com.example.akaishi.block.entity.AkaishiItemPortBlockEntity;
import com.example.akaishi.wireless.ItemPortTransfer;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 储存无线输入/输出口的 Forge 物品能力视图（第三方物流直达：原版漏斗、MEK 管道、AE2/RS 等）。
 * <p>
 * <b>为什么不用通用 {@link ForgeItemHandler}：</b>那个适配层假定机器有<b>真实槽位</b>——
 * 插入按 {@code min(堆量, 最大堆)} 记账后调 {@code setItem}，抽取按"读到的槽内容"原样回报。
 * 而本口是<b>无内部容器、纯转发</b>的「终端储存远程接口面」：真正能收多少取决于终端空间、
 * 费用闸门与绑定态。套用通用适配层会把"转发未成功"当成功（丢物）、把"未抽出"当抽出（复制）。
 * 故这里按 forge 侧既有能力注册范式补一个只做转发的实现，一切搬运与拒绝语义都下沉到
 * {@link AkaishiItemPortBlockEntity#externalInsert}/{@code externalExtract}/{@code externalCanAccept}，
 * 能力层只负责槽位契约与方向硬约束（与 {@code RULES §5} 的"接口声明方向、适配层逐槽遵守"同构）。
 * <p>
 * <b>单向硬约束</b>：输入口 {@code insertItem} 正常转发进终端、{@code extractItem} 恒空；
 * 输出口 {@code extractItem} 正常从终端取出、{@code insertItem} 恒原样退回。
 * 单次上限 1 组（{@link ItemPortTransfer#BATCH_LIMIT}），避免一次巨量塞入绕过节流。
 */
public final class ItemPortExternalItemHandler implements IItemHandler, ICapabilityProvider {

    /** 虚拟转发槽下标（不存物） */
    private static final int SLOT = 0;

    private final AkaishiItemPortBlockEntity port;
    private final LazyOptional<IItemHandler> self = LazyOptional.of(() -> this);

    public ItemPortExternalItemHandler(AkaishiItemPortBlockEntity port) {
        this.port = port;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == ForgeCapabilities.ITEM_HANDLER ? self.cast() : LazyOptional.empty();
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return slot == SLOT ? port.externalPreview() : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == SLOT ? ItemPortTransfer.BATCH_LIMIT : 0;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return slot == SLOT && port.externalCanAccept(stack);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return slot == SLOT ? port.externalInsert(stack, simulate) : stack;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return slot == SLOT ? port.externalExtract(amount, simulate) : ItemStack.EMPTY;
    }
}
