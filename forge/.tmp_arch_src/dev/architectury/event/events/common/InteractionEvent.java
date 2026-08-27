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

package dev.architectury.event.events.common;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;
import net.minecraft.class_1268;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2680;

public interface InteractionEvent {
    /**
     * @see LeftClickBlock#click(class_1657, class_1268, class_2338, class_2350)
     */
    Event<LeftClickBlock> LEFT_CLICK_BLOCK = EventFactory.createEventResult();
    /**
     * @see RightClickBlock#click(class_1657, class_1268, class_2338, class_2350)
     */
    Event<RightClickBlock> RIGHT_CLICK_BLOCK = EventFactory.createEventResult();
    /**
     * @see RightClickItem#click(class_1657, class_1268)
     */
    Event<RightClickItem> RIGHT_CLICK_ITEM = EventFactory.createCompoundEventResult();
    /**
     * @see ClientLeftClickAir#click(class_1657, class_1268)
     */
    Event<ClientLeftClickAir> CLIENT_LEFT_CLICK_AIR = EventFactory.createLoop();
    /**
     * @see ClientRightClickAir#click(class_1657, class_1268)
     */
    Event<ClientRightClickAir> CLIENT_RIGHT_CLICK_AIR = EventFactory.createLoop();
    /**
     * @see InteractEntity#interact(class_1657, class_1297, class_1268)
     */
    Event<InteractEntity> INTERACT_ENTITY = EventFactory.createEventResult();
    /**
     * @see FarmlandTrample#trample(class_1937, class_2338, class_2680, float, class_1297)
     */
    Event<FarmlandTrample> FARMLAND_TRAMPLE = EventFactory.createEventResult();
    
    interface RightClickBlock {
        /**
         * Invoked whenever a player right clicks a block.
         * Equivalent to Forge's {@code PlayerInteractEvent.RightClickBlock} event and Fabric's {@code UseBlockCallback}.
         *
         * @param player The player right clicking the block.
         * @param hand   The hand that is used.
         * @param pos    The position of the block in the level.
         * @param face   The face of the block clicked.
         * @return A {@link EventResult} determining the outcome of the event,
         * the action may be cancelled by the result.
         */
        EventResult click(class_1657 player, class_1268 hand, class_2338 pos, class_2350 face);
    }
    
    interface LeftClickBlock {
        /**
         * Invoked whenever a player left clicks a block.
         * Equivalent to Forge's {@code PlayerInteractEvent.LeftClickBlock} event and Fabric's {@code AttackBlockCallback}.
         *
         * @param player The player left clicking the block.
         * @param hand   The hand that is used.
         * @param pos    The position of the block in the level. Use {@link class_1657#method_5770()} to get the level.
         * @param face   The face of the block clicked.
         * @return A {@link EventResult} determining the outcome of the event,
         * the action may be cancelled by the result.
         */
        EventResult click(class_1657 player, class_1268 hand, class_2338 pos, class_2350 face);
    }
    
    interface RightClickItem {
        /**
         * Invoked whenever a player uses an item on a block.
         * Equivalent to Forge's {@code PlayerInteractEvent.RightClickItem} event and Fabric's {@code UseItemCallback}.
         *
         * @param player The player right clicking the block.
         * @param hand   The hand that is used.
         * @return A {@link EventResult} determining the outcome of the event,
         * the action may be cancelled by the result.
         */
        CompoundEventResult<class_1799> click(class_1657 player, class_1268 hand);
    }
    
    interface ClientRightClickAir {
        /**
         * Invoked whenever a player right clicks the air.
         * This only occurs on the client.
         * Equivalent to Forge's {@code PlayerInteractEvent.RightClickEmpty} event.
         *
         * @param player The player. Always {@link net.minecraft.class_746}
         * @param hand   The hand used.
         */
        void click(class_1657 player, class_1268 hand);
    }
    
    interface ClientLeftClickAir {
        /**
         * Invoked whenever a player left clicks the air.
         * This only occurs on the client.
         * Equivalent to Forge's {@code PlayerInteractEvent.LeftClickEmpty} event.
         *
         * @param player The player. Always {@link net.minecraft.class_746}
         * @param hand   The hand used.
         */
        void click(class_1657 player, class_1268 hand);
    }
    
    interface InteractEntity {
        /**
         * Invoked whenever a player right clicks an entity.
         * Equivalent to Forge's {@code PlayerInteractEvent.EntityInteract} event.
         *
         * @param player The player clicking the entity.
         * @param entity Then entity the player clicks.
         * @param hand   The used hand.
         * @return A {@link EventResult} determining the outcome of the event,
         * the action may be cancelled by the result.
         */
        EventResult interact(class_1657 player, class_1297 entity, class_1268 hand);
    }
    
    interface FarmlandTrample {
        /**
         * Invoked when an entity attempts to trample farmland.
         * Equivalent to Forge's {@code BlockEvent.FarmlandTrampleEvent} event.
         *
         * @param world    The level where the block and the player are located in.
         * @param pos      The position of the block.
         * @param state    The state of the block.
         * @param distance The distance of the player to the block.
         * @param entity   The entity trampling.
         * @return A {@link EventResult} determining the outcome of the event,
         * the action may be cancelled by the result.
         */
        EventResult trample(class_1937 world, class_2338 pos, class_2680 state, float distance, class_1297 entity);
    }
}
