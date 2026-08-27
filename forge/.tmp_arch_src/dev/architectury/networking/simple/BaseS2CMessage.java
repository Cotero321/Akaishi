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

package dev.architectury.networking.simple;

import net.minecraft.class_2596;
import net.minecraft.class_2818;
import net.minecraft.class_3215;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;

/**
 * The base class for server -&gt; client messages managed by a {@link SimpleNetworkManager}.
 */
public abstract class BaseS2CMessage extends Message {
    private void sendTo(class_3222 player, class_2596<?> packet) {
        if (player == null) {
            throw new NullPointerException("Unable to send packet '" + getType().getId() + "' to a 'null' player!");
        }

        player.field_13987.method_14364(packet);
    }

    /**
     * Sends this message to a player.
     *
     * @param player the player
     */
    public final void sendTo(class_3222 player) {
        sendTo(player, toPacket());
    }

    /**
     * Sends this message to multiple players.
     *
     * @param players the players
     */
    public final void sendTo(Iterable<class_3222> players) {
        class_2596<?> packet = toPacket();

        for (class_3222 player : players) {
            sendTo(player, packet);
        }
    }

    /**
     * Sends this message to all players in the server.
     *
     * @param server the server
     */
    public final void sendToAll(MinecraftServer server) {
        sendTo(server.method_3760().method_14571());
    }

    /**
     * Sends this message to all players in a level.
     *
     * @param level the level
     */
    public final void sendToLevel(class_3218 level) {
        sendTo(level.method_18456());
    }

    /**
     * Sends this message to all players listening to a chunk.
     *
     * @param chunk the listened chunk
     */
    public final void sendToChunkListeners(class_2818 chunk) {
        class_2596<?> packet = toPacket();
        ((class_3215) chunk.method_12200().method_8398()).field_17254.method_17210(chunk.method_12004(), false).forEach(e -> sendTo(e, packet));
    }
}