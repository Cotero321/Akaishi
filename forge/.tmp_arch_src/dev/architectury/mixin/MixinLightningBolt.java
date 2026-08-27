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

package dev.architectury.mixin;

import dev.architectury.event.events.common.LightningEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1538;
import net.minecraft.class_1937;

@Mixin(class_1538.class)
public abstract class MixinLightningBolt extends class_1297 {
    
    public MixinLightningBolt(class_1299<?> type, class_1937 level) {
        super(type, level);
        throw new IllegalStateException();
    }
    
    @Inject(method = "tick", at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            ordinal = 1,
            shift = At.Shift.BY,
            by = 1
    ), locals = LocalCapture.CAPTURE_FAILHARD)
    public void handleLightning(CallbackInfo ci, List<class_1297> list) {
        if (this.method_31481() || this.method_37908().field_9236) {
            return;
        }
        
        LightningEvent.STRIKE.invoker().onStrike((class_1538) (Object) this, this.method_37908(), this.method_19538(), list);
    }
    
}
