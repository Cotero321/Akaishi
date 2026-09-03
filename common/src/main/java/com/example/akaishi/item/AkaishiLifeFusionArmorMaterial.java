package com.example.akaishi.item;

import com.example.akaishi.AkaishiMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 生命融合护甲材料：赤石护甲的 2 倍基础数值。
 * 护甲值（靴子/护腿/胸甲/头盔）={8,14,20,8}（赤石 {4,7,10,4}×2），
 * 韧性 8（赤石 4×2），耐久 = 原版基数 ×92（赤石 ×46 的 2 倍）。
 */
public final class AkaishiLifeFusionArmorMaterial {

    /** 单件护甲值（靴子/护腿/胸甲/头盔，与 getSlot().getIndex() 顺序一致：FEET=0 LEGS=1 CHEST=2 HEAD=3） */
    private static final int[] PROTECTION = {8, 14, 20, 8};

    public static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return new int[]{13, 15, 16, 11}[type.getSlot().getIndex()] * 92;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return PROTECTION[type.getSlot().getIndex()];
        }

        @Override
        public int getEnchantmentValue() {
            return 25;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_NETHERITE;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.lifeFusionIngot.get());
        }

        @Override
        public String getName() {
            // 定位到 akaishi:textures/models/armor/life_fusion_layer_1/2.png
            return AkaishiMod.MOD_ID + ":life_fusion";
        }

        @Override
        public float getToughness() {
            return 8.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.2F;
        }
    };
}
