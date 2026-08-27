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

package dev.architectury.registry.client.particle;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.registry.registries.RegistrySupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1058;
import net.minecraft.class_1059;
import net.minecraft.class_2394;
import net.minecraft.class_2396;
import net.minecraft.class_4002;
import net.minecraft.class_707;
import java.util.List;

/**
 * A utility class for registering custom {@link class_707}s for particle types.
 * <p>
 * This class's methods should be invoked <b>before</b> {@link ClientLifecycleEvent#CLIENT_SETUP},
 * as doing so afterwards will result in the providers not being registered properly on Forge, causing crashes on startup.
 * <p>
 * Generally speaking, you should either listen to the registration of your particle type yourself and use either
 * {@link #register(class_2396, class_707)} or {@link #register(class_2396, DeferredParticleProvider)} to register the provider,
 * or use the helper methods {@link #register(RegistrySupplier, class_707)} and {@link #register(RegistrySupplier, DeferredParticleProvider)},
 * which will automatically handle the listening for you.
 */
@Environment(EnvType.CLIENT)
public final class ParticleProviderRegistry {
    public interface ExtendedSpriteSet extends class_4002 {
        class_1059 getAtlas();
        
        List<class_1058> getSprites();
    }
    
    public static <T extends class_2394> void register(RegistrySupplier<? extends class_2396<T>> supplier, class_707<T> provider) {
        supplier.listen(it -> register(it, provider));
    }
    
    public static <T extends class_2394> void register(RegistrySupplier<? extends class_2396<T>> supplier, DeferredParticleProvider<T> provider) {
        supplier.listen(it -> register(it, provider));
    }
    
    @ExpectPlatform
    public static <T extends class_2394> void register(class_2396<T> type, class_707<T> provider) {
        throw new AssertionError();
    }
    
    @ExpectPlatform
    public static <T extends class_2394> void register(class_2396<T> type, DeferredParticleProvider<T> provider) {
        throw new AssertionError();
    }
    
    @FunctionalInterface
    public interface DeferredParticleProvider<T extends class_2394> {
        class_707<T> create(ExtendedSpriteSet spriteSet);
    }
}
