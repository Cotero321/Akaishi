package com.example.akaishi.api.security;

import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 身份卡访问间接层（依赖倒置）：把「终端安全判定」与「身份卡物品实现」解耦。
 * <p>
 * 核心模块不认识任何具体身份卡物品；由科技本体在物品侧注册实现（
 * {@code AkaishiWirelessIdentityCardItem} 的静态初始化块）。
 * 未注册时一律按「非身份卡」处理，与"物品不属于本模组"的原判定等价。
 * <p>
 * 仅供 {@code TerminalSecurity} 读取卡上的身份与权限位使用，不含任何写入语义。
 */
public final class SecurityCardAccess {

    /** 身份卡读取实现（由科技本体注册） */
    public interface Provider {
        /** 该堆是否为身份卡 */
        boolean isCard(ItemStack stack);

        /** 卡上权限位掩码（未设置返回 0） */
        int permsOf(ItemStack stack);

        /** 卡绑定的玩家身份；未绑定返回 null（= 默认权限条目） */
        @Nullable
        UUID playerOf(ItemStack stack);

        /** 卡绑定的玩家名；未绑定返回 null */
        @Nullable
        String playerNameOf(ItemStack stack);
    }

    /** 未注册实现时的兜底：非身份卡 */
    private static final Provider ABSENT = new Provider() {
        @Override
        public boolean isCard(ItemStack stack) {
            return false;
        }

        @Override
        public int permsOf(ItemStack stack) {
            return AkaishiSecurityPermission.NONE;
        }

        @Override
        public UUID playerOf(ItemStack stack) {
            return null;
        }

        @Override
        public String playerNameOf(ItemStack stack) {
            return null;
        }
    };

    /** 当前实现（volatile：允许本体在初始化期注册、运行期只读） */
    private static volatile Provider provider = ABSENT;

    private SecurityCardAccess() {
    }

    /** 注册实现（由科技本体调用；传 null 复位为兜底） */
    public static void register(@Nullable Provider impl) {
        provider = impl == null ? ABSENT : impl;
    }

    /** 该堆是否为身份卡（null 恒 false） */
    public static boolean isCard(@Nullable ItemStack stack) {
        return stack != null && provider.isCard(stack);
    }

    /** 卡上权限位掩码 */
    public static int permsOf(ItemStack stack) {
        return provider.permsOf(stack);
    }

    /** 卡绑定的玩家身份；未绑定返回 null */
    @Nullable
    public static UUID playerOf(ItemStack stack) {
        return provider.playerOf(stack);
    }

    /** 卡绑定的玩家名；未绑定返回 null */
    @Nullable
    public static String playerNameOf(ItemStack stack) {
        return provider.playerNameOf(stack);
    }
}
