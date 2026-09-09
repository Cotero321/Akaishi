package com.example.akaishi.mixin;

import com.example.akaishi.forge.client.armor.AkaishiArmorClientExtensions;
import com.example.akaishi.forge.client.mechanical.MechanicalPartRenderer;
import com.example.akaishi.item.AkaishiArmorItem;
import com.example.akaishi.item.MechanicalOrganItem;
import com.example.akaishi.item.MechanicalPartItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * 为 common 层物品注入 Forge 专属客户端扩展。
 * 机械部件使用 BEWLR，赤石护甲使用原生人形动力装甲模型。
 */
@Mixin(Item.class)
public class MechanicalItemRendererMixin {

    @Inject(method = "initializeClient", at = @At("HEAD"), remap = false, cancellable = true)
    public void onInitializeClient(Consumer<IClientItemExtensions> consumer, CallbackInfo ci) {
        Item self = (Item) (Object) this;
        if (self instanceof AkaishiArmorItem) {
            consumer.accept(AkaishiArmorClientExtensions.INSTANCE);
            ci.cancel();
        }
        if (self instanceof MechanicalPartItem || self instanceof MechanicalOrganItem) {
            consumer.accept(MechanicalPartRenderer.INSTANCE);
            ci.cancel();
        }
    }
}
