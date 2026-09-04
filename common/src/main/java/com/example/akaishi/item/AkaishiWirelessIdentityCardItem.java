package com.example.akaishi.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * 终端身份卡（钥匙）：无线网络的认证凭证。
 * 卡片拥有唯一 UUID。UUID 只在服务端逻辑点生成（{@link #ensureUuid}）并写回物品 NBT；
 * 读取用 {@link #uuidOf}（纯只读，客户端亦安全）。禁止在客户端生成：
 * 双端随机生成会产生不一致卡号。未生成前（如客户端副本尚未同步）返回 null，调用方显示占位符。
 * <p>
 * 认证流程（参考 MEK 量子传送器「同卡配对」）：
 * 1) 在终端「安全卡认证」页把卡片 UUID 加入授权列表；
 * 2) 输入口/输出口放入同 UUID 的卡片 → 被终端识别并建立连接；
 * 3) 连接后口的区块弱加载生效；无线传输不限速（无速率上限，卡不支持升级）。
 */
public class AkaishiWirelessIdentityCardItem extends Item {

    /** 物品 NBT 根键 */
    private static final String TAG_ROOT = "IdentityCard";
    /** 卡唯一 ID */
    private static final String TAG_UUID = "CardUuid";

    public AkaishiWirelessIdentityCardItem(Properties properties) {
        super(properties);
    }

    /** 读取卡片 UUID（纯只读，不创建/修改 NBT）；尚无 UUID 返回 null */
    public static UUID uuidOf(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ROOT);
        return tag != null && tag.hasUUID(TAG_UUID) ? tag.getUUID(TAG_UUID) : null;
    }

    /**
     * 确保卡片拥有唯一 UUID（无则生成并写回），返回卡 UUID。
     * 仅在服务端逻辑调用点使用（方块右键、菜单按钮、服务端刷新等），
     * 避免依赖「物理端」环境判断在单机集成服务器下误判为客户端而拒绝生成。
     */
    public static UUID ensureUuid(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTagElement(TAG_ROOT);
        if (!tag.hasUUID(TAG_UUID)) {
            tag.putUUID(TAG_UUID, UUID.randomUUID());
        }
        return tag.getUUID(TAG_UUID);
    }

    /** 卡片 ID 短显示（前 8 位 hex；客户端未同步到 UUID 时显示占位符） */
    public static String shortId(ItemStack stack) {
        UUID u = uuidOf(stack);
        return u == null ? "----" : u.toString().substring(0, 8).toUpperCase();
    }

    /**
     * 右键交互（仅服务端）：为卡生成唯一卡号并显示（此后工具提示即可显示真实卡号）。
     * 卡不可升级（无线传输本就无速率上限）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            ItemStack stack = player.getItemInHand(hand);
            ensureUuid(stack);
            player.displayClientMessage(Component.translatable("message.akaishi.identity_card.activated",
                    shortId(stack)), false);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        // 短悬浮：卡号（编号）高亮 + 不限速提示，避免长文案撑大悬浮框
        tooltip.add(Component.translatable("item.akaishi.akaishi_wireless_identity_card.id",
                shortId(stack)));
        tooltip.add(Component.translatable("item.akaishi.akaishi_wireless_identity_card.rate_unlimited"));
    }
}
