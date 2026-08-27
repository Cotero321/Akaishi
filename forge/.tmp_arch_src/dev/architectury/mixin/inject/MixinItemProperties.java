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

import dev.architectury.extensions.injected.InjectedItemPropertiesExtension;
import dev.architectury.impl.ItemPropertiesExtensionImpl;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredSupplier;
import net.minecraft.class_1761;
import net.minecraft.class_1792;
import net.minecraft.class_5321;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(class_1792.class_1793.class)
public class MixinItemProperties implements InjectedItemPropertiesExtension, ItemPropertiesExtensionImpl {
    @Unique
    private class_1761 tab;
    @Unique
    private DeferredSupplier<class_1761> tabSupplier;
    
    @Override
    public class_1792.class_1793 arch$tab(class_1761 tab) {
        this.tab = tab;
        this.tabSupplier = null;
        return (class_1792.class_1793) (Object) this;
    }
    
    @Override
    public class_1792.class_1793 arch$tab(DeferredSupplier<class_1761> tab) {
        this.tab = null;
        this.tabSupplier = tab;
        return (class_1792.class_1793) (Object) this;
    }
    
    @Override
    public class_1792.class_1793 arch$tab(class_5321<class_1761> tab) {
        this.tab = null;
        this.tabSupplier = CreativeTabRegistry.defer(tab);
        return (class_1792.class_1793) (Object) this;
    }
    
    @Override
    @Nullable
    public class_1761 arch$getTab() {
        return tab;
    }
    
    @Override
    @Nullable
    public DeferredSupplier<class_1761> arch$getTabSupplier() {
        return tabSupplier;
    }
}
