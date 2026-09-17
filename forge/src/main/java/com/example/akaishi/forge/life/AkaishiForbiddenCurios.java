package com.example.akaishi.forge.life;

import com.example.akaishi.item.ModItems;
import com.example.akaishi.item.curio.AkaishiForbiddenTooltip;
import com.example.akaishi.item.curio.AkaishiSocketCurioItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 「禁忌」四件的 Curios 访问辅助。
 *
 * <p>槽位已在 {@link AkaishiSocketCurioItem#canEquip} 中严格隔离（D38：一件只认自己那一槽），
 * 因此「按物品定位」与「按槽位定位」等价，这里统一用物品定位，避免槽位字面量重复。</p>
 */
final class AkaishiForbiddenCurios {

    private AkaishiForbiddenCurios() {
    }

    /** 定位佩戴中的禁忌件（未佩戴返回空） */
    static Optional<ItemStack> find(Player player, Item item) {
        return CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(handler -> handler.findFirstCurio(item))
                .map(slot -> slot.stack());
    }

    static boolean worn(Player player, Item item) {
        return find(player, item).isPresent();
    }

    static boolean hasLifeTouch(Player player) {
        return worn(player, ModItems.lifeTouch.get());
    }

    static boolean hasCubHeart(Player player) {
        return worn(player, ModItems.cubHeart.get());
    }

    static boolean hasMotherSeal(Player player) {
        return worn(player, ModItems.motherSeal.get());
    }

    static boolean hasFertilityRing(Player player) {
        return worn(player, ModItems.fertilityRing.get());
    }

    /**
     * 已佩戴的四件禁忌饰品快照（按固定顺序，未佩戴的跳过）。
     * 走 {@link #find} 取 Curios 槽内实际 Stack 引用，调用方写入的 NBT 可直接落盘（侵蚀进度依赖此点）。
     */
    static List<ItemStack> wornPieces(Player player) {
        List<ItemStack> pieces = new ArrayList<>(AkaishiForbiddenTooltip.SET_PIECES);
        collect(player, ModItems.lifeTouch.get(), pieces);
        collect(player, ModItems.cubHeart.get(), pieces);
        collect(player, ModItems.motherSeal.get(), pieces);
        collect(player, ModItems.fertilityRing.get(), pieces);
        return pieces;
    }

    private static void collect(Player player, Item item, List<ItemStack> out) {
        find(player, item).ifPresent(out::add);
    }

    /** 已佩戴的禁忌件数（0-4），供套装 tooltip 与激活判定共用 */
    static int countWorn(Player player) {
        ICuriosItemHandler handler = CuriosApi.getCuriosInventory(player).resolve().orElse(null);
        if (handler == null) {
            return 0;
        }
        IItemHandlerModifiable curios = handler.getEquippedCurios();
        int worn = 0;
        for (int i = 0; i < curios.getSlots(); i++) {
            if (curios.getStackInSlot(i).getItem() instanceof AkaishiSocketCurioItem) {
                worn++;
            }
        }
        return worn;
    }

    /** 四件集齐（套装激活，D1） */
    static boolean isFullSet(Player player) {
        return countWorn(player) >= AkaishiForbiddenTooltip.SET_PIECES;
    }
}
