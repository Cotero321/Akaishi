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

package dev.architectury.networking;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.networking.transformers.PacketCollector;
import dev.architectury.networking.transformers.PacketSink;
import dev.architectury.networking.transformers.PacketTransformer;
import dev.architectury.networking.transformers.SinglePacketCollector;
import dev.architectury.utils.Env;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_2540;
import net.minecraft.class_2596;
import net.minecraft.class_2602;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collections;
import java.util.List;

public final class NetworkManager {
    public static void registerReceiver(Side side, class_2960 id, NetworkReceiver receiver) {
        registerReceiver(side, id, Collections.emptyList(), receiver);
    }
    
    @ExpectPlatform
    @ApiStatus.Experimental
    public static void registerReceiver(Side side, class_2960 id, List<PacketTransformer> packetTransformers, NetworkReceiver receiver) {
        throw new AssertionError();
    }
    
    @Deprecated
    @ApiStatus.ScheduledForRemoval
    public static class_2596<?> toPacket(Side side, class_2960 id, class_2540 buf) {
        SinglePacketCollector sink = new SinglePacketCollector(null);
        collectPackets(sink, side, id, buf);
        return sink.getPacket();
    }
    
    @Deprecated
    @ApiStatus.ScheduledForRemoval
    public static List<class_2596<?>> toPackets(Side side, class_2960 id, class_2540 buf) {
        PacketCollector sink = new PacketCollector(null);
        collectPackets(sink, side, id, buf);
        return sink.collect();
    }
    
    @ExpectPlatform
    public static void collectPackets(PacketSink sink, Side side, class_2960 id, class_2540 buf) {
        throw new AssertionError();
    }
    
    public static void sendToPlayer(class_3222 player, class_2960 id, class_2540 buf) {
        collectPackets(PacketSink.ofPlayer(player), serverToClient(), id, buf);
    }
    
    public static void sendToPlayers(Iterable<class_3222> players, class_2960 id, class_2540 buf) {
        collectPackets(PacketSink.ofPlayers(players), serverToClient(), id, buf);
    }
    
    @Environment(EnvType.CLIENT)
    public static void sendToServer(class_2960 id, class_2540 buf) {
        collectPackets(PacketSink.client(), clientToServer(), id, buf);
    }
    
    @Environment(EnvType.CLIENT)
    @ExpectPlatform
    public static boolean canServerReceive(class_2960 id) {
        throw new AssertionError();
    }
    
    @ExpectPlatform
    public static boolean canPlayerReceive(class_3222 player, class_2960 id) {
        throw new AssertionError();
    }
    
    /**
     * Easy to use utility method to create an entity spawn packet.
     * This packet is needed everytime any mod adds a non-living entity.
     * The entity should override {@link class_1297#method_18002()} to point to this method!
     * <p>
     * Additionally, entities may implement {@link dev.architectury.extensions.network.EntitySpawnExtension}
     * to load / save additional data to the client.
     *
     * @param entity The entity which should be spawned.
     * @return The ready to use packet to spawn the entity on the client.
     * @see class_1297#method_18002()
     */
    @ExpectPlatform
    public static class_2596<class_2602> createAddEntityPacket(class_1297 entity) {
        throw new AssertionError();
    }
    
    @FunctionalInterface
    public interface NetworkReceiver {
        void receive(class_2540 buf, PacketContext context);
    }
    
    public interface PacketContext {
        class_1657 getPlayer();
        
        void queue(Runnable runnable);
        
        Env getEnvironment();
        
        default EnvType getEnv() {
            return getEnvironment().toPlatform();
        }
    }
    
    public static Side s2c() {
        return Side.S2C;
    }
    
    public static Side c2s() {
        return Side.C2S;
    }
    
    public static Side serverToClient() {
        return Side.S2C;
    }
    
    public static Side clientToServer() {
        return Side.C2S;
    }
    
    public enum Side {
        S2C,
        C2S
    }
}
