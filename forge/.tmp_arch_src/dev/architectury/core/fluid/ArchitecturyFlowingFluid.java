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

package dev.architectury.core.fluid;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Platform;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import net.minecraft.class_1792;
import net.minecraft.class_1802;
import net.minecraft.class_1922;
import net.minecraft.class_1936;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2404;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_2689;
import net.minecraft.class_3414;
import net.minecraft.class_3609;
import net.minecraft.class_3610;
import net.minecraft.class_3611;
import net.minecraft.class_4538;

public abstract class ArchitecturyFlowingFluid extends class_3609 {
    private final ArchitecturyFluidAttributes attributes;
    
    ArchitecturyFlowingFluid(ArchitecturyFluidAttributes attributes) {
        checkPlatform(null);
        this.attributes = attributes;
        if (Platform.isFabric()) {
            addFabricFluidAttributes(this, attributes);
        }
    }
    
    private static <T> T checkPlatform(T obj) {
        if (Platform.isForge()) {
            throw new IllegalStateException("This class should've been replaced on Forge!");
        }
        
        return obj;
    }
    
    @ExpectPlatform
    private static void addFabricFluidAttributes(class_3609 fluid, ArchitecturyFluidAttributes properties) {
        throw new AssertionError();
    }
    
    @Override
    public class_3611 method_15750() {
        return attributes.getFlowingFluid();
    }
    
    @Override
    public class_3611 method_15751() {
        return attributes.getSourceFluid();
    }
    
    @Override
    protected boolean method_15737(class_1937 level) {
        return attributes.canConvertToSource();
    }
    
    @Override
    protected void method_15730(class_1936 level, class_2338 pos, class_2680 state) {
        // Same implementation as in WaterFluid.
        class_2586 blockEntity = state.method_31709() ? level.method_8321(pos) : null;
        class_2248.method_9610(state, level, pos, blockEntity);
    }
    
    @Override
    protected int method_15733(class_4538 level) {
        return attributes.getSlopeFindDistance(level);
    }
    
    @Override
    protected int method_15739(class_4538 level) {
        return attributes.getDropOff(level);
    }
    
    @Override
    public class_1792 method_15774() {
        class_1792 item = attributes.getBucketItem();
        return item == null ? class_1802.field_8162 : item;
    }
    
    @Override
    protected boolean method_15777(class_3610 state, class_1922 level, class_2338 pos, class_3611 fluid, class_2350 direction) {
        // Same implementation as in WaterFluid.
        return direction == class_2350.field_11033 && !this.method_15780(fluid);
    }
    
    @Override
    public int method_15789(class_4538 level) {
        return attributes.getTickDelay(level);
    }
    
    @Override
    protected float method_15784() {
        return attributes.getExplosionResistance();
    }
    
    @Override
    protected class_2680 method_15790(class_3610 state) {
        class_2404 block = attributes.getBlock();
        if (block == null) return class_2246.field_10124.method_9564();
        return block.method_9564().method_11657(class_2404.field_11278, method_15741(state));
    }
    
    @NotNull
    @Override
    public Optional<class_3414> method_32359() {
        return Optional.ofNullable(attributes.getFillSound());
    }
    
    @Override
    public boolean method_15780(class_3611 fluid) {
        return fluid == method_15751() || fluid == method_15750();
    }
    
    public static class Source extends ArchitecturyFlowingFluid {
        public Source(ArchitecturyFluidAttributes attributes) {
            super(attributes);
        }
        
        @Override
        public int method_15779(class_3610 state) {
            return 8;
        }
        
        @Override
        public boolean method_15793(class_3610 state) {
            return true;
        }
    }
    
    public static class Flowing extends ArchitecturyFlowingFluid {
        public Flowing(ArchitecturyFluidAttributes attributes) {
            super(attributes);
            this.method_15781(this.method_15783().method_11664().method_11657(field_15900, 7));
        }
        
        @Override
        protected void method_15775(class_2689.class_2690<class_3611, class_3610> builder) {
            super.method_15775(builder);
            builder.method_11667(field_15900);
        }
        
        @Override
        public int method_15779(class_3610 state) {
            return state.method_11654(field_15900);
        }
        
        @Override
        public boolean method_15793(class_3610 state) {
            return false;
        }
    }
}
