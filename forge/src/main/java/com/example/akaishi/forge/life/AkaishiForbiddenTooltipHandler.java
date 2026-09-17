package com.example.akaishi.forge.life;

import com.example.akaishi.item.curio.AkaishiForbiddenTooltip;
import com.example.akaishi.item.curio.AkaishiSocketCurioItem;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.List;

/**
 * 「禁忌」四件饰品 tooltip 第二页（D238 修订，仅客户端渲染时触发）。
 *
 * <p>默认页只显示单件自带说明与 Shift 提示；本类在按住 Shift 时追加扩展信息：
 * 套装说明与状态 → 侵蚀进度 → 跑满警示。件数判定复用 {@link AkaishiForbiddenCurios}，
 * 保证展示与生效口径一致。</p>
 */
public final class AkaishiForbiddenTooltipHandler {

    public static final AkaishiForbiddenTooltipHandler INSTANCE = new AkaishiForbiddenTooltipHandler();

    private AkaishiForbiddenTooltipHandler() {
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        // ItemTooltipEvent 亦可能在服务端被触发；Screen 为客户端类，先行按侧拦截避免类解析
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof AkaishiSocketCurioItem) || !Screen.hasShiftDown()) {
            return;
        }
        Player player = event.getEntity();
        if (player == null) {
            return; // 非玩家悬停上下文（如配方预览）无法取穿戴数据，跳过实时行
        }
        List<Component> tooltip = event.getToolTip();
        tooltip.add(Component.empty());
        AkaishiForbiddenTooltip.appendSetEffectLines(tooltip);
        AkaishiForbiddenTooltip.appendSetStatus(tooltip, AkaishiForbiddenCurios.countWorn(player));
        AkaishiSocketCurioItem.appendErosionInfo(tooltip, stack);
    }
}
