package com.example.akaishi.forge;

import com.example.akaishi.config.ModConfig;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.UUID;

/**
 * 赤石饰品扩展槽解锁器。
 *
 * <p>四个扩展槽在数据包中声明为 size=0（不直接开槽），由本类按玩家在赤石进度链上已完成的
 * 节点数，逐槽下发 ADDITION 型「永久」槽位修饰符各 +1 完成开槽。永久修饰符由 Curios 序列化进
 * 玩家 NBT（PersistentModifiers），重登/重载后不丢失，因此只在达成时下发一次即可。</p>
 *
 * <p>触发点：玩家登录 + 获得任意进度。二者均只在服务端执行。</p>
 */
public final class AkaishiCurioSlotUnlocker {

    /** 四个扩展槽 id，下标顺序与配置阈值 [槽1..槽4] 一一对应 */
    public static final String[] SLOT_IDS = {
            "akaishi_socket_1", "akaishi_socket_2", "akaishi_socket_3", "akaishi_socket_4"
    };

    /** 进度链：按推进顺序排列，玩家完成其中的节点数即为解锁进度 */
    private static final ResourceLocation[] PROGRESS_CHAIN = {
            new ResourceLocation("akaishi", "akaishi_root"),
            new ResourceLocation("akaishi", "first_ore"),
            new ResourceLocation("akaishi", "first_refine"),
            new ResourceLocation("akaishi", "first_purifier"),
            new ResourceLocation("akaishi", "first_item_pipe"),
            new ResourceLocation("akaishi", "first_aggregator"),
            new ResourceLocation("akaishi", "first_life_energy")
    };

    /** 每槽固定修饰符 UUID：Curios 以 UUID 判重，固定值保证同一槽只下发一次、不会逐 tick 叠加 */
    private static final UUID[] MODIFIER_UUIDS = {
            UUID.fromString("0a1b2c3d-3001-4000-8000-000000000001"),
            UUID.fromString("0a1b2c3d-3001-4000-8000-000000000002"),
            UUID.fromString("0a1b2c3d-3001-4000-8000-000000000003"),
            UUID.fromString("0a1b2c3d-3001-4000-8000-000000000004")
    };

    private static final String MODIFIER_NAME = "akaishi_socket_unlock";

    private AkaishiCurioSlotUnlocker() {
    }

    /** 按玩家当前进度补齐已达标的扩展槽（幂等，可重复调用） */
    public static void sync(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> grantReached(handler, progressNodes(player)));
    }

    /** 统计进度链上已完成的节点数 */
    private static int progressNodes(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }
        int done = 0;
        for (ResourceLocation id : PROGRESS_CHAIN) {
            Advancement advancement = server.getAdvancements().getAdvancement(id);
            // 进度缺失（数据包被移除等）按未完成处理，不阻断其余节点统计
            if (advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                done++;
            }
        }
        return done;
    }

    /** 逐槽判断：达标且尚未下发时，收集进一次性批量修饰符请求 */
    private static void grantReached(ICuriosItemHandler handler, int progress) {
        Multimap<String, AttributeModifier> pending = LinkedHashMultimap.create();
        for (int i = 0; i < SLOT_IDS.length; i++) {
            if (progress < threshold(i) || hasModifier(handler, SLOT_IDS[i], MODIFIER_UUIDS[i])) {
                continue;
            }
            pending.put(SLOT_IDS[i], new AttributeModifier(MODIFIER_UUIDS[i], MODIFIER_NAME, 1.0D,
                    AttributeModifier.Operation.ADDITION));
        }
        if (!pending.isEmpty()) {
            handler.addPermanentSlotModifiers(pending);
        }
    }

    /** 第 index 个槽的开槽门槛；关闭解锁门槛开关时全部视为 0（无条件开启） */
    private static int threshold(int index) {
        if (!ModConfig.curioSlotUnlockRequired) {
            return 0;
        }
        int[] thresholds = ModConfig.curioSlotUnlockThresholds;
        // 配置越界/缺失时回退到「槽位序号」内置默认（槽1需 1 个节点，依次递增）
        if (thresholds == null || index >= thresholds.length) {
            return index + 1;
        }
        return Math.max(0, thresholds[index]);
    }

    /** 该槽是否已下发过本模组的开槽修饰符 */
    private static boolean hasModifier(ICuriosItemHandler handler, String slotId, UUID uuid) {
        for (AttributeModifier modifier : handler.getModifiers().get(slotId)) {
            if (modifier.getId().equals(uuid)) {
                return true;
            }
        }
        return false;
    }
}
