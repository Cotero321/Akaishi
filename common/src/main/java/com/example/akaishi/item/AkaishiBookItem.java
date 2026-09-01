package com.example.akaishi.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import vazkii.patchouli.api.PatchouliAPI;

/**
 * Patchouli 手册物品：右键时在服务端请求打开对应书籍。
 * Patchouli 为可选依赖（mods.toml 中 mandatory=false），
 * 未安装时类加载抛 LinkageError，经捕获降级为普通物品，不导致崩溃。
 */
public class AkaishiBookItem extends Item {

    private static final Logger LOGGER = LogManager.getLogger();

    /** 目标手册 id（patchouli_books 目录名），如 akaishi:akaishi_diary */
    private final ResourceLocation bookId;

    public AkaishiBookItem(ResourceLocation bookId, Properties properties) {
        super(properties);
        this.bookId = bookId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            try {
                // Patchouli 官方 API 自带 Stub 兜底；此处防御整个 mod 未安装时的类缺失
                PatchouliAPI.get().openBookGUI(serverPlayer, bookId);
            } catch (LinkageError e) {
                LOGGER.warn("Patchouli 未安装，无法打开手册 {}", bookId);
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
