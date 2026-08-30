package com.example.template.item;

import com.example.template.TemplateMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 赤石盔甲材料：基础防御与韧性均为下界合金的 1.25 倍。
 * 护甲值（靴子/护腿/胸甲/头盔）={4,7,10,4}（下界合金 {3,6,8,3}×1.25={3.75,7.5,10,3.75}，向下取整），
 * 韧性 4（3×1.25），耐久 = 原版基数 ×46（下界合金 37 的 1.25 倍）。
 */
public final class ChishiArmorMaterial {

    /** 单件护甲值（靴子/护腿/胸甲/头盔，与 getSlot().getIndex() 顺序一致：FEET=0 LEGS=1 CHEST=2 HEAD=3） */
    private static final int[] PROTECTION = {4, 7, 10, 4};

    public static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            // 原版护甲耐久基数（靴子/护腿/胸甲/头盔）× 46 ≈ 下界合金(37) 的 1.25 倍
            return new int[]{13, 15, 16, 11}[type.getSlot().getIndex()] * 46;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return PROTECTION[type.getSlot().getIndex()];
        }

        @Override
        public int getEnchantmentValue() {
            return 15;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_NETHERITE;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.chishiIngot.get());
        }

        @Override
        public String getName() {
            // 返回带命名空间的名字，使 Forge 默认 getArmorTexture 定位到
            // template_mod:textures/models/armor/chishi_layer_1/2.png
            return TemplateMod.MOD_ID + ":chishi";
        }

        @Override
        public float getToughness() {
            return 4.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.1F;
        }
    };
}
