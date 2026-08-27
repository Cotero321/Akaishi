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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import java.util.function.Supplier;
import net.minecraft.class_2172;
import net.minecraft.class_241;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_638;
import net.minecraft.class_7157;
import net.minecraft.class_746;

public interface ClientCommandRegistrationEvent {
    /**
     * @see ClientCommandRegistrationEvent#register(CommandDispatcher, class_7157)
     */
    Event<ClientCommandRegistrationEvent> EVENT = EventFactory.createLoop();
    
    /**
     * This event is invoked after the client initializes.
     * Equivalent to Forge's {@code RegisterClientCommandsEvent} and Fabric's {@code ClientCommandManager}.
     *
     * @param dispatcher The command dispatcher to register commands to.
     * @param context    The command build context.
     */
    void register(CommandDispatcher<ClientCommandSourceStack> dispatcher, class_7157 context);
    
    static LiteralArgumentBuilder<ClientCommandSourceStack> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }
    
    static <T> RequiredArgumentBuilder<ClientCommandSourceStack, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }
    
    interface ClientCommandSourceStack extends class_2172 {
        void arch$sendSuccess(Supplier<class_2561> message, boolean broadcastToAdmins);
        
        void arch$sendFailure(class_2561 message);
        
        class_746 arch$getPlayer();
        
        class_243 arch$getPosition();
        
        class_241 arch$getRotation();
        
        class_638 arch$getLevel();
    }
}
