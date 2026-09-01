package com.example.akaishi.recipe;

import com.example.akaishi.item.AkaishiWirelessIdentityCardItem;
import com.example.akaishi.item.IdentityCardTier;
import com.example.akaishi.item.ModItems;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/**
 * 身份卡升级配方（工作台合成，保留 NBT）：
 * 身份卡 + 对应活化结晶 → 升级后的身份卡（复制输入卡全部 NBT，仅提升等级，卡号 UUID 不变）。
 * 匹配按卡当前等级动态判定：基础卡+活化下界复合→中级，中级+活化至纯→高级，高级+活化终极混合→超级；
 * 已达到超级的卡无下一级，不再匹配。合成网格最多 1 卡 + 1 结晶，多余物品不匹配。
 * 继承 ShapelessRecipe 复用其序列化/类型/合成网格逻辑，仅覆写匹配与产物（保留 NBT 的核心）。
 */
public class IdentityCardUpgradeRecipe extends ShapelessRecipe {

    public IdentityCardUpgradeRecipe(ResourceLocation id) {
        super(id, "", CraftingBookCategory.MISC,
                new ItemStack(ModItems.akaishiWirelessIdentityCard.get()),
                NonNullList.of(Ingredient.EMPTY,
                        Ingredient.of(ModItems.akaishiWirelessIdentityCard.get()),
                        Ingredient.of(ModItems.activatedNetherCompoundCrystal.get(),
                                ModItems.activatedPureCrystal.get(),
                                ModItems.activatedUltimateMixtureCrystal.get())));
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        ItemStack card = ItemStack.EMPTY;
        ItemStack crystal = ItemStack.EMPTY;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            if (s.is(ModItems.akaishiWirelessIdentityCard.get())) {
                if (!card.isEmpty()) {
                    return false; // 多张卡
                }
                card = s;
            } else {
                if (!crystal.isEmpty()) {
                    return false; // 多于 1 种非卡物品
                }
                crystal = s;
            }
        }
        if (card.isEmpty() || crystal.isEmpty()) {
            return false;
        }
        // 校验该卡当前等级的升级材料：逐级对应，不可跳级/错料
        IdentityCardTier tier = AkaishiWirelessIdentityCardItem.tierOf(card);
        AkaishiWirelessIdentityCardItem.UpgradeRequirement next = AkaishiWirelessIdentityCardItem.nextUpgrade(tier);
        return next != null && crystal.is(next.crystal().get());
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
        ItemStack card = ItemStack.EMPTY;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems.akaishiWirelessIdentityCard.get())) {
                card = s;
                break;
            }
        }
        IdentityCardTier tier = AkaishiWirelessIdentityCardItem.tierOf(card);
        IdentityCardTier next = AkaishiWirelessIdentityCardItem.nextUpgrade(tier).nextTier();
        ItemStack out = card.copy(); // 复制全部 NBT（含卡号 UUID）
        AkaishiWirelessIdentityCardItem.setTier(out, next);
        return out;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.IDENTITY_CARD_UPGRADE.get();
    }

    /** 序列化器：继承 ShapelessRecipe 序列化逻辑，覆写反序列化返回本类型（数据仅 type，无额外字段） */
    public static class Serializer extends ShapelessRecipe.Serializer {

        public static final Serializer INSTANCE = new Serializer();

        @Override
        public IdentityCardUpgradeRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new IdentityCardUpgradeRecipe(id);
        }

        @Override
        public IdentityCardUpgradeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new IdentityCardUpgradeRecipe(id);
        }
    }
}
