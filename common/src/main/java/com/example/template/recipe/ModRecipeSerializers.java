package com.example.template.recipe;

import com.example.template.TemplateMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 自定义配方序列化器注册（数据包 JSON 中 type 指向的 serializer）。
 */
public final class ModRecipeSerializers {

    /** 身份卡升级配方序列化器（工作台合成保留 NBT） */
    public static RegistrySupplier<RecipeSerializer<IdentityCardUpgradeRecipe>> IDENTITY_CARD_UPGRADE;

    private ModRecipeSerializers() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register() {
        Registrar<RecipeSerializer<?>> registrar = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.RECIPE_SERIALIZER);
        IDENTITY_CARD_UPGRADE = (RegistrySupplier<RecipeSerializer<IdentityCardUpgradeRecipe>>) (Object) registrar
                .register(new ResourceLocation(TemplateMod.MOD_ID, "identity_card_upgrade"),
                        () -> IdentityCardUpgradeRecipe.Serializer.INSTANCE);
    }
}
