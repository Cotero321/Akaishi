package com.example.akaishi.forge.life;

import com.example.akaishi.item.AkaishiLifeFusionSet;
import com.example.akaishi.item.AkaishiLifeFusionTooltip;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.ClientBodyData;
import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.body.PlayerBodyHelper;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import com.example.akaishi.life.organ.OrganEffectResolver;
import com.example.akaishi.life.sample.SampleGroup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 生命融合护甲实时状态 tooltip（仅客户端渲染时触发）：
 * 在静态效果说明之后追加 已穿件数 / 全套激活 / BOSS·龙强化条件，并以母神台词收尾。
 * 效果判定统一复用 AkaishiLifeFusionSet，保证展示与生效逻辑口径一致。
 */
public final class AkaishiLifeFusionTooltipHandler {

    public static final AkaishiLifeFusionTooltipHandler INSTANCE = new AkaishiLifeFusionTooltipHandler();

    private AkaishiLifeFusionTooltipHandler() {
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!AkaishiLifeFusionSet.isLifeFusionArmor(stack)) {
            return;
        }
        Player player = event.getEntity();
        if (player == null) {
            return; // 非玩家悬停上下文（如部分配方预览）无法取穿戴数据，跳过实时行
        }
        AkaishiLifeFusionTooltip.appendSetStatus(event.getToolTip(),
                AkaishiLifeFusionSet.countWorn(player), hasBossOrDragon(player));
    }

    /** 体内是否存在生效的 BOSS/龙族器官（服务端走躯体状态；客户端读同步镜像） */
    private static boolean hasBossOrDragon(Player player) {
        if (player instanceof ServerPlayer) {
            IPlayerBodyState state = PlayerBodyHelper.of(player);
            return AkaishiLifeFusionSet.hasBossOrDragonOrgan(player, state);
        }
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = ClientBodyData.getOrgan(slot);
            if (organ.isEmpty() || !(organ.getItem() instanceof AkaishiOrganItem) || AkaishiOrganItem.isNative(organ)) {
                continue;
            }
            // 与服务器生效判定一致：排斥满 100 的器官视为完全失效
            if (ClientBodyData.getRejection(slot) >= OrganEffectResolver.MAX_SAFE_REJECTION) {
                continue;
            }
            SampleGroup group = OrganEffectResolver.groupOf(AkaishiOrganItem.getEntityId(organ), player.level());
            if (group == SampleGroup.BOSS || group == SampleGroup.DRAGON) {
                return true;
            }
        }
        return false;
    }
}
