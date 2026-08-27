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

package dev.architectury.hooks.item;

import Z;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.class_1542;
import net.minecraft.class_1799;
import net.minecraft.class_3222;
import net.minecraft.class_3417;
import net.minecraft.class_3419;

public final class ItemStackHooks {
    private ItemStackHooks() {
    }
    
    public static class_1799 copyWithCount(class_1799 stack, int count) {
        var copy = stack.method_7972();
        copy.method_7939(count);
        return copy;
    }
    
    public static void giveItem(class_3222 player, class_1799 stack) {
        var bl = player.method_31548().method_7394(stack);
        if (bl && stack.method_7960()) {
            stack.method_7939(1);
            var entity = player.method_7328(stack, false);
            if (entity != null) {
                entity.method_6987();
            }
            
            player.method_37908().method_43128(null, player.method_23317(), player.method_23318(), player.method_23321(), class_3417.field_15197, class_3419.field_15248, 0.2F, ((player.method_6051().method_43057() - player.method_6051().method_43057()) * 0.7F + 1.0F) * 2.0F);
            player.field_7498.method_7623();
        } else {
            var entity = player.method_7328(stack, false);
            if (entity != null) {
                entity.method_6975();
                entity.method_48349(player.method_5667());
            }
        }
    }
    
    /**
     * Returns whether the given item stack has a remaining item after crafting.
     * This method is stack-aware only on Forge.
     *
     * @param stack the item stack
     * @return whether the given item stack has a remaining item after crafting
     */
    @ExpectPlatform
    public static boolean hasCraftingRemainingItem(class_1799 stack) {
        throw new AssertionError();
    }
    
    /**
     * Returns the remaining item for a given item stack after crafting.
     * This method is stack-aware only on Forge.
     *
     * @param stack the item stack
     * @return the remaining item for a given item stack after crafting
     */
    @ExpectPlatform
    public static class_1799 getCraftingRemainingItem(class_1799 stack) {
        throw new AssertionError();
    }
}
