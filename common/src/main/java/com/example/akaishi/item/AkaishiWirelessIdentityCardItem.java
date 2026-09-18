package com.example.akaishi.item;

import com.example.akaishi.api.security.AkaishiSecurityPermission;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * 终端身份卡（钥匙）：终端安全系统的权限载体，口径对齐 AE2 生物识别卡。
 * <p>
 * 卡上存三样东西：① 唯一卡号（物理卡序号，仅用于区分同身份的卡）；② 绑定的玩家身份
 * （UUID + 名字，未绑定即为 AE2 的"默认权限条目"）；③ 权限位掩码（五档见
 * {@link AkaishiSecurityPermission}）。卡号只在服务端生成（{@link #ensureUuid}）并写回 NBT，
 * 双端随机会产生不一致卡号。
 * <p>
 * 交互：<b>shift + 右键</b>把自己编码进卡（卡上已是自己则解除）；<b>右键其他玩家</b>把对方写入卡；
 * 普通右键只提示卡信息。权限位本身在终端「安全认证」页勾选（服务端写入卡 NBT）。
 */
public class AkaishiWirelessIdentityCardItem extends Item {

    /** 物品 NBT 根键 */
    private static final String TAG_ROOT = "IdentityCard";
    /** 卡唯一 ID（物理卡序号） */
    private static final String TAG_UUID = "CardUuid";
    /** 绑定玩家 UUID（未绑定 = 默认权限条目） */
    private static final String TAG_PLAYER = "PlayerUuid";
    /** 绑定玩家名（仅显示用；以 UUID 为准） */
    private static final String TAG_PLAYER_NAME = "PlayerName";
    /** 权限位掩码 */
    private static final String TAG_PERMS = "Perms";

    public AkaishiWirelessIdentityCardItem(Properties properties) {
        super(properties);
    }

    // ===== 卡号（物理卡序号） =====

    /** 读取卡号（纯只读，不创建/修改 NBT）；尚无卡号返回 null */
    public static UUID uuidOf(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ROOT);
        return tag != null && tag.hasUUID(TAG_UUID) ? tag.getUUID(TAG_UUID) : null;
    }

    /**
     * 确保卡片拥有唯一卡号（无则生成并写回），返回卡号。
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

    /** 卡号短显示（前 8 位 hex；客户端未同步到卡号时显示占位符） */
    public static String shortId(ItemStack stack) {
        UUID u = uuidOf(stack);
        return u == null ? "----" : u.toString().substring(0, 8).toUpperCase();
    }

    // ===== 身份绑定 =====

    /** 卡绑定的玩家 UUID；未绑定返回 null（= 默认权限条目） */
    public static UUID playerOf(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ROOT);
        return tag != null && tag.hasUUID(TAG_PLAYER) ? tag.getUUID(TAG_PLAYER) : null;
    }

    /** 卡绑定的玩家名；未绑定返回 null */
    public static String playerNameOf(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ROOT);
        return tag != null && tag.contains(TAG_PLAYER_NAME) ? tag.getString(TAG_PLAYER_NAME) : null;
    }

    /** 是否已绑定玩家身份 */
    public static boolean isBound(ItemStack stack) {
        return playerOf(stack) != null;
    }

    /** 绑定玩家身份（覆盖旧身份） */
    public static void bindPlayer(ItemStack stack, UUID player, String name) {
        CompoundTag tag = stack.getOrCreateTagElement(TAG_ROOT);
        tag.putUUID(TAG_PLAYER, player);
        tag.putString(TAG_PLAYER_NAME, name == null ? "" : name);
    }

    /** 解除身份绑定（此卡即成为默认权限条目） */
    public static void clearPlayer(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ROOT);
        if (tag != null) {
            tag.remove(TAG_PLAYER);
            tag.remove(TAG_PLAYER_NAME);
        }
    }

    // ===== 权限位 =====

    /** 权限掩码（未设置返回 0） */
    public static int permsOf(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ROOT);
        return tag == null ? AkaishiSecurityPermission.NONE : tag.getInt(TAG_PERMS);
    }

    /** 覆写权限掩码（掩码为 0 时清除键，保持 NBT 干净） */
    public static void setPerms(ItemStack stack, int mask) {
        CompoundTag tag = stack.getOrCreateTagElement(TAG_ROOT);
        int normalized = mask & AkaishiSecurityPermission.ALL;
        if (normalized == AkaishiSecurityPermission.NONE) {
            tag.remove(TAG_PERMS);
        } else {
            tag.putInt(TAG_PERMS, normalized);
        }
    }

    /** 本卡是否含某权限 */
    public static boolean hasPerm(ItemStack stack, AkaishiSecurityPermission permission) {
        return permission.granted(permsOf(stack));
    }

    // ===== 交互 =====

    /**
     * 右键空气：普通右键 = 提示卡信息；<b>shift + 右键</b> = 把自己编码进卡（卡上已是自己则解除）。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        ensureUuid(stack);
        if (player.isShiftKeyDown()) {
            if (player.getUUID().equals(playerOf(stack))) {
                clearPlayer(stack);
                player.displayClientMessage(
                        Component.translatable("message.akaishi.identity_card.cleared"), false);
            } else {
                bindPlayer(stack, player.getUUID(), player.getGameProfile().getName());
                player.displayClientMessage(Component.translatable("message.akaishi.identity_card.bound",
                        player.getGameProfile().getName()), false);
            }
            return InteractionResultHolder.success(stack);
        }
        player.displayClientMessage(Component.translatable("message.akaishi.identity_card.activated",
                shortId(stack)), false);
        return InteractionResultHolder.success(stack);
    }

    /**
     * 右键其他玩家：把对方身份写入卡（用于登记席位，AE2 口径）。
     * <p>
     * <b>必须由对方潜行表示同意</b>：卡上的身份就是运行期身份，能被 BUILD / INJECT / EXTRACT / CRAFT 判定直接采信，
     * 若允许"随手右键即绑定他人"，任何人只要右键一次归属者就能拿到一张冒用其身份的卡，
     * 进而把他人的终端储能/库存通过自己的端口搬走 —— 那是明确的安全漏洞。
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
            InteractionHand hand) {
        if (!(target instanceof Player other) || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide) {
            if (!other.isShiftKeyDown()) {
                player.displayClientMessage(Component.translatable(
                        "message.akaishi.identity_card.need_consent", other.getGameProfile().getName()), false);
            } else {
                ItemStack card = player.getItemInHand(hand);
                ensureUuid(card);
                bindPlayer(card, other.getUUID(), other.getGameProfile().getName());
                player.displayClientMessage(Component.translatable("message.akaishi.identity_card.bound",
                        other.getGameProfile().getName()), false);
            }
        }
        player.swing(hand);
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.akaishi.akaishi_wireless_identity_card.id", shortId(stack)));
        String name = playerNameOf(stack);
        tooltip.add(name == null || name.isEmpty()
                ? Component.translatable("item.akaishi.akaishi_wireless_identity_card.unbound")
                : Component.translatable("item.akaishi.akaishi_wireless_identity_card.bound", name));
        int perms = permsOf(stack);
        tooltip.add(perms == AkaishiSecurityPermission.NONE
                ? Component.translatable("item.akaishi.akaishi_wireless_identity_card.no_perms")
                : Component.translatable("item.akaishi.akaishi_wireless_identity_card.perms", describePerms(perms)));
        tooltip.add(Component.translatable("item.akaishi.akaishi_wireless_identity_card.rate_unlimited"));
    }

    /** 权限掩码 → 可读权限名列表（用半角逗号连接，双语通吃） */
    private static Component describePerms(int mask) {
        MutableComponent text = Component.empty();
        boolean first = true;
        for (AkaishiSecurityPermission p : AkaishiSecurityPermission.values()) {
            if (!p.granted(mask)) {
                continue;
            }
            if (!first) {
                text.append(", ");
            }
            text.append(p.displayName().copy().withStyle(ChatFormatting.GREEN));
            first = false;
        }
        return text;
    }
}
