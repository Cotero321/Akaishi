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

package dev.architectury.fluid;

import dev.architectury.hooks.fluid.FluidStackHooks;
import dev.architectury.injectables.annotations.ExpectPlatform;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.class_2487;
import net.minecraft.class_2520;
import net.minecraft.class_2540;
import net.minecraft.class_2561;
import net.minecraft.class_3611;
import net.minecraft.class_3612;

public final class FluidStack {
    private static final FluidStackAdapter<Object> ADAPTER = adapt(FluidStack::getValue, FluidStack::new);
    private static final FluidStack EMPTY = create(class_3612.field_15906, 0);
    
    private Object value;
    
    private FluidStack(Supplier<class_3611> fluid, long amount, class_2487 tag) {
        this(ADAPTER.create(fluid, amount, tag));
    }
    
    private FluidStack(Object value) {
        this.value = Objects.requireNonNull(value);
    }
    
    private Object getValue() {
        return value;
    }
    
    @ExpectPlatform
    private static FluidStackAdapter<Object> adapt(Function<FluidStack, Object> toValue, Function<Object, FluidStack> fromValue) {
        throw new AssertionError();
    }
    
    @ApiStatus.Internal
    public interface FluidStackAdapter<T> {
        T create(Supplier<class_3611> fluid, long amount, class_2487 tag);
        
        Supplier<class_3611> getRawFluidSupplier(T object);
        
        class_3611 getFluid(T object);
        
        long getAmount(T object);
        
        void setAmount(T object, long amount);
        
        class_2487 getTag(T value);
        
        void setTag(T value, class_2487 tag);
        
        T copy(T value);
        
        int hashCode(T value);
    }
    
    public static FluidStack empty() {
        return EMPTY;
    }
    
    public static FluidStack create(class_3611 fluid, long amount, @Nullable class_2487 tag) {
        return create(() -> fluid, amount, tag);
    }
    
    public static FluidStack create(class_3611 fluid, long amount) {
        return create(fluid, amount, null);
    }
    
    public static FluidStack create(Supplier<class_3611> fluid, long amount, @Nullable class_2487 tag) {
        return new FluidStack(fluid, amount, tag);
    }
    
    public static FluidStack create(Supplier<class_3611> fluid, long amount) {
        return create(fluid, amount, null);
    }
    
    public static FluidStack create(FluidStack stack, long amount) {
        return create(stack.getRawFluidSupplier(), amount, stack.getTag());
    }
    
    public static long bucketAmount() {
        return FluidStackHooks.bucketAmount();
    }
    
    public class_3611 getFluid() {
        return isEmpty() ? class_3612.field_15906 : getRawFluid();
    }
    
    @Nullable
    public class_3611 getRawFluid() {
        return ADAPTER.getFluid(value);
    }
    
    public Supplier<class_3611> getRawFluidSupplier() {
        return ADAPTER.getRawFluidSupplier(value);
    }
    
    public boolean isEmpty() {
        return getRawFluid() == class_3612.field_15906 || ADAPTER.getAmount(value) <= 0;
    }
    
    public long getAmount() {
        return isEmpty() ? 0 : ADAPTER.getAmount(value);
    }
    
    public void setAmount(long amount) {
        ADAPTER.setAmount(value, amount);
    }
    
    public void grow(long amount) {
        setAmount(getAmount() + amount);
    }
    
    public void shrink(long amount) {
        setAmount(getAmount() - amount);
    }
    
    public boolean hasTag() {
        return getTag() != null;
    }
    
    @Nullable
    public class_2487 getTag() {
        return ADAPTER.getTag(value);
    }
    
    public void setTag(@Nullable class_2487 tag) {
        ADAPTER.setTag(value, tag);
    }
    
    public class_2487 getOrCreateTag() {
        class_2487 tag = getTag();
        if (tag == null) {
            tag = new class_2487();
            setTag(tag);
            return tag;
        }
        return tag;
    }
    
    @Nullable
    public class_2487 getChildTag(String childName) {
        class_2487 tag = getTag();
        if (tag == null)
            return null;
        return tag.method_10562(childName);
    }
    
    public class_2487 getOrCreateChildTag(String childName) {
        class_2487 tag = getOrCreateTag();
        var child = tag.method_10562(childName);
        if (!tag.method_10573(childName, class_2520.field_33260)) {
            tag.method_10566(childName, child);
        }
        return child;
    }
    
    public void removeChildTag(String childName) {
        class_2487 tag = getTag();
        if (tag != null)
            tag.method_10551(childName);
    }
    
    public class_2561 getName() {
        return FluidStackHooks.getName(this);
    }
    
    public String getTranslationKey() {
        return FluidStackHooks.getTranslationKey(this);
    }
    
    public FluidStack copy() {
        return new FluidStack(ADAPTER.copy(value));
    }
    
    @Override
    public int hashCode() {
        return ADAPTER.hashCode(value);
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FluidStack)) {
            return false;
        }
        return isFluidStackEqual((FluidStack) o);
    }
    
    public boolean isFluidStackEqual(FluidStack other) {
        return getFluid() == other.getFluid() && getAmount() == other.getAmount() && isTagEqual(other);
    }
    
    public boolean isFluidEqual(FluidStack other) {
        return getFluid() == other.getFluid();
    }
    
    public boolean isTagEqual(FluidStack other) {
        var tag = getTag();
        var otherTag = other.getTag();
        return Objects.equals(tag, otherTag);
    }
    
    public static FluidStack read(class_2540 buf) {
        return FluidStackHooks.read(buf);
    }
    
    public static FluidStack read(class_2487 tag) {
        return FluidStackHooks.read(tag);
    }
    
    public void write(class_2540 buf) {
        FluidStackHooks.write(this, buf);
    }
    
    public class_2487 write(class_2487 tag) {
        return FluidStackHooks.write(this, tag);
    }
    
    public FluidStack copyWithAmount(long amount) {
        if (isEmpty()) return this;
        return new FluidStack(getRawFluidSupplier(), amount, getTag());
    }
    
    @ApiStatus.Internal
    public static void init() {
        // classloading my beloved 😍
        // please don't use this by the way
    }
}
