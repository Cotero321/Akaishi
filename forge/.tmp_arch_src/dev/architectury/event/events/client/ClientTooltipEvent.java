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

package dev.architectury.event.events.client;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;
import dev.architectury.impl.TooltipAdditionalContextsImpl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_1836;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_5684;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Environment(EnvType.CLIENT)
public interface ClientTooltipEvent {
    /**
     * @see Item#append(class_1799, List, class_1836)
     */
    Event<Item> ITEM = EventFactory.createLoop();
    /**
     * @see Render#renderTooltip(class_332, List, int, int)
     */
    Event<Render> RENDER_PRE = EventFactory.createEventResult();
    /**
     * @see RenderModifyPosition#renderTooltip(class_332, PositionContext)
     */
    Event<RenderModifyPosition> RENDER_MODIFY_POSITION = EventFactory.createLoop();
    /**
     * @see RenderModifyColor#renderTooltip(class_332, int, int, ColorContext)
     */
    Event<RenderModifyColor> RENDER_MODIFY_COLOR = EventFactory.createLoop();
    
    static AdditionalContexts additionalContexts() {
        return TooltipAdditionalContextsImpl.get();
    }
    
    @ApiStatus.NonExtendable
    interface AdditionalContexts {
        @Nullable
        class_1799 getItem();
        
        void setItem(@Nullable class_1799 stack);
    }
    
    @Environment(EnvType.CLIENT)
    interface Item {
        /**
         * Invoked whenever an item tooltip is rendered.
         * Equivalent to Forge's {@code ItemTooltipEvent} event and
         * Fabric's {@code ItemTooltipCallback}.
         *
         * @param stack The rendered stack.
         * @param lines The mutable list of tooltip components.
         * @param flag  A flag indicating if advanced mode is active.
         */
        void append(class_1799 stack, List<class_2561> lines, class_1836 flag);
    }
    
    @Environment(EnvType.CLIENT)
    interface Render {
        /**
         * Invoked before the tooltip for a tooltip is rendered.
         *
         * @param graphics The graphics context.
         * @param texts    The mutable list of components that are rendered.
         * @param x        The x-coordinate of the tooltip.
         * @param y        The y-coordinate of the tooltip.
         * @return A {@link EventResult} determining the outcome of the event,
         * the execution of the vanilla tooltip rendering may be cancelled by the result.
         */
        EventResult renderTooltip(class_332 graphics, List<? extends class_5684> texts, int x, int y);
    }
    
    @Environment(EnvType.CLIENT)
    interface RenderModifyPosition {
        /**
         * Event to manipulate the position of the tooltip.
         *
         * @param graphics The graphics context.
         * @param context  The current position context.
         */
        void renderTooltip(class_332 graphics, PositionContext context);
    }
    
    @Environment(EnvType.CLIENT)
    interface RenderModifyColor {
        /**
         * Event to manipulate the color of the tooltip.
         *
         * @param graphics The graphics context.
         * @param x        The x-coordinate of the tooltip.
         * @param y        The y-coordinate of the tooltip.
         * @param context  The current color context.
         */
        void renderTooltip(class_332 graphics, int x, int y, ColorContext context);
    }
    
    @Environment(EnvType.CLIENT)
    interface PositionContext {
        int getTooltipX();
        
        void setTooltipX(int x);
        
        int getTooltipY();
        
        void setTooltipY(int y);
    }
    
    @Environment(EnvType.CLIENT)
    interface ColorContext {
        int getBackgroundColor();
        
        void setBackgroundColor(int color);
        
        int getOutlineGradientTopColor();
        
        void setOutlineGradientTopColor(int color);
        
        int getOutlineGradientBottomColor();
        
        void setOutlineGradientBottomColor(int color);
    }
}
