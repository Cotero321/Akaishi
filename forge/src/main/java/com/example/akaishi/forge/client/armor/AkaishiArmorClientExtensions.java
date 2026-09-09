package com.example.akaishi.forge.client.armor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Forge 客户端护甲扩展：为赤石护甲提供原生分件模型。 */
public final class AkaishiArmorClientExtensions implements IClientItemExtensions {
    public static final AkaishiArmorClientExtensions INSTANCE = new AkaishiArmorClientExtensions();

    private AkaishiMekaSuitArmorModel model;

    private AkaishiArmorClientExtensions() {
    }

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                   EquipmentSlot slot, HumanoidModel<?> original) {
        if (model == null) {
            model = new AkaishiMekaSuitArmorModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(AkaishiMekaSuitArmorModel.LAYER));
        }
        model.prepareForSlot(original, slot);
        return model;
    }
}
