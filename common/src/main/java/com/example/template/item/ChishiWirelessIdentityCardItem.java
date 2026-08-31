package com.example.template.item;

import com.example.template.config.ModConfig;
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
 * 卡片拥有唯一 UUID（首次读取时惰性生成）与等级（{@link IdentityCardTier}）。
 * <p>
 * 认证流程（参考 MEK 量子传送器「同卡配对」）：
 * 1) 在终端「安全卡认证」页把卡片 UUID 加入授权列表；
 * 2) 输入口/输出口放入同 UUID 的卡片 → 被终端识别并建立连接；
 * 3) 连接后按卡等级自动解锁传输速率与口区块弱加载。
 */
public class ChishiWirelessIdentityCardItem extends Item {

    /** 物品 NBT 根键 */
    private static final String TAG_ROOT = "IdentityCard";
    /** 卡唯一 ID */
    private static final String TAG_UUID = "CardUuid";
    /** 卡等级 */
    private static final String TAG_TIER = "Tier";

    public ChishiWirelessIdentityCardItem(Properties properties) {
        super(properties);
    }

    /**
     * 读取卡片 UUID。
     * 服务端首次读取时惰性生成并写回（保证每张卡唯一）；客户端只读不写——
     * 若服务端尚未生成（未同步到客户端副本）返回 null，调用方显示占位符。
     * 禁止客户端生成：客户端写 NBT 会产生与服务端不同的随机 ID，导致两端卡号不一致。
     */
    public static UUID uuidOf(ItemStack stack) {
        if (dev.architectury.platform.Platform.getEnvironment() == dev.architectury.utils.Env.CLIENT) {
            CompoundTag tag = stack.getTagElement(TAG_ROOT);
            return tag != null && tag.hasUUID(TAG_UUID) ? tag.getUUID(TAG_UUID) : null;
        }
        CompoundTag tag = stack.getOrCreateTagElement(TAG_ROOT);
        if (!tag.hasUUID(TAG_UUID)) {
            tag.putUUID(TAG_UUID, UUID.randomUUID());
        }
        return tag.getUUID(TAG_UUID);
    }

    /** 读取卡等级（缺省基础级）。客户端只读不创建 NBT（与 uuidOf 同源：防两端 tag 不一致） */
    public static IdentityCardTier tierOf(ItemStack stack) {
        if (dev.architectury.platform.Platform.getEnvironment() == dev.architectury.utils.Env.CLIENT) {
            CompoundTag tag = stack.getTagElement(TAG_ROOT);
            return tag == null ? IdentityCardTier.BASIC : IdentityCardTier.byId(tag.getInt(TAG_TIER));
        }
        CompoundTag tag = stack.getOrCreateTagElement(TAG_ROOT);
        return IdentityCardTier.byId(tag.getInt(TAG_TIER));
    }

    /** 设置卡等级（未来升级卡片的入口，当前仅基础级） */
    public static void setTier(ItemStack stack, IdentityCardTier tier) {
        stack.getOrCreateTagElement(TAG_ROOT).putInt(TAG_TIER, tier.id());
    }

    /** 卡片 ID 短显示（前 8 位 hex；客户端未同步到 UUID 时显示占位符） */
    public static String shortId(ItemStack stack) {
        UUID u = uuidOf(stack);
        return u == null ? "----" : u.toString().substring(0, 8).toUpperCase();
    }

    /** 右键激活卡片并显示身份信息（仅服务端广播） */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            ItemStack stack = player.getItemInHand(hand);
            player.displayClientMessage(Component.translatable("message.template_mod.identity_card.activated",
                    shortId(stack),
                    Component.translatable("tier.template_mod." + tierOf(stack).name().toLowerCase()),
                    ModConfig.wirelessPortTransferRate * tierOf(stack).rateMultiplier()), false);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        // 短悬浮：卡号（编号）高亮 + 等级 + 速率，避免长文案撑大悬浮框
        tooltip.add(Component.translatable("item.template_mod.chishi_wireless_identity_card.id",
                shortId(stack)));
        tooltip.add(Component.translatable("item.template_mod.chishi_wireless_identity_card.tier",
                Component.translatable("tier.template_mod." + tierOf(stack).name().toLowerCase())));
        tooltip.add(Component.translatable("item.template_mod.chishi_wireless_identity_card.rate",
                ModConfig.wirelessPortTransferRate * tierOf(stack).rateMultiplier()));
    }
}