package com.example.akaishi.life.body;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 可安装器官契约：手术仓 9 槽体系对所有可植入物品统一以此判定。
 * <p>生物器官（{@code AkaishiOrganItem}）与机械义体（{@code MechanicalOrganItem}）均实现本接口，
 * 使手术三件套（Menu / BlockEntity / Screen）无需依赖具体物品类型。</p>
 */
public interface IInstallableOrgan {

    /** 该物品栈绑定的躯体槽位（类型不确定时返回 null） */
    @Nullable
    BodySlot bodySlot(ItemStack stack);

    /**
     * 安装时的初始整合度：生物器官恒为 0，机械义体按材料决定。
     * 由 {@code PlayerBodyState.implantOrgan} 在安装时写入整合度数据。
     */
    default int initialIntegration(ItemStack stack) {
        return 0;
    }

    /** 物品栈 → 槽位（不可安装物品返回 null） */
    @Nullable
    static BodySlot slotOf(ItemStack stack) {
        return stack.getItem() instanceof IInstallableOrgan organ ? organ.bodySlot(stack) : null;
    }
}
