/*
 * This file is part of architectury.
 * Copyright (C) 2020, 2021, 2022 architectury
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package dev.architectury.mixin.inject;

import dev.architectury.extensions.injected.InjectedItemExtension;
import dev.architectury.impl.ItemPropertiesExtensionImpl;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredSupplier;
import net.minecraft.class_1761;
import net.minecraft.class_1792;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_1792.class)
public class MixinItem implements InjectedItemExtension {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(class_1792.class_1793 properties, CallbackInfo ci) {
        class_1761 tab = ((ItemPropertiesExtensionImpl) properties).arch$getTab();
        if (tab != null) {
            CreativeTabRegistry.appendBuiltin(tab, (class_1792) (Object) this);
            return;
        }
        DeferredSupplier<class_1761> tabSupplier = ((ItemPropertiesExtensionImpl) properties).arch$getTabSupplier();
        if (tabSupplier != null) {
            CreativeTabRegistry.append(tabSupplier, (class_1792) (Object) this);
        }
    }
}
