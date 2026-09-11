package com.example.akaishi.item;

/**
 * 动力装甲标记接口：实现该接口的护甲在 Forge 端改用原生人形动力装甲模型
 * （AkaishiMekaSuitArmorModel，128×64 UV）渲染，而非原版护甲模型。
 * <p>
 * 穿着纹理由 Forge 默认 getArmorTexture 按 ArmorMaterial#getName() 定位，
 * 即 textures/models/armor/&lt;name&gt;_layer_1|2.png，无需接口额外声明。
 * <p>
 * 附属模组实现本接口即可复用该模型，无需改动渲染 Mixin。
 */
public interface IPoweredArmor {
}
