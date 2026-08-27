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

package dev.architectury.registry;

import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import net.minecraft.class_1761;
import net.minecraft.class_1799;
import net.minecraft.class_1935;

@ApiStatus.NonExtendable
@ApiStatus.Experimental
public interface CreativeTabOutput extends class_1761.class_7704 {
    void acceptAfter(class_1799 after, class_1799 stack, class_1761.class_7705 visibility);
    
    void acceptBefore(class_1799 before, class_1799 stack, class_1761.class_7705 visibility);
    
    @Override
    default void method_45417(class_1799 stack, class_1761.class_7705 visibility) {
        acceptAfter(class_1799.field_8037, stack, visibility);
    }
    
    default void acceptAfter(class_1799 after, class_1799 stack) {
        this.acceptAfter(after, stack, class_1761.class_7705.field_40191);
    }
    
    default void acceptAfter(class_1799 after, class_1935 item, class_1761.class_7705 visibility) {
        this.acceptAfter(after, new class_1799(item), visibility);
    }
    
    default void acceptAfter(class_1799 after, class_1935 item) {
        this.acceptAfter(after, new class_1799(item), class_1761.class_7705.field_40191);
    }
    
    default void acceptAllAfter(class_1799 after, Collection<class_1799> stacks, class_1761.class_7705 visibility) {
        stacks.forEach((stack) -> acceptAfter(after, stack, visibility));
    }
    
    default void acceptAllAfter(class_1799 after, Collection<class_1799> stacks) {
        this.acceptAllAfter(after, stacks, class_1761.class_7705.field_40191);
    }
    
    default void acceptAfter(class_1935 after, class_1799 stack) {
        this.acceptAfter(new class_1799(after), stack);
    }
    
    default void acceptAfter(class_1935 after, class_1935 item, class_1761.class_7705 visibility) {
        this.acceptAfter(new class_1799(after), item, visibility);
    }
    
    default void acceptAfter(class_1935 after, class_1935 item) {
        this.acceptAfter(new class_1799(after), item);
    }
    
    default void acceptAllAfter(class_1935 after, Collection<class_1799> stacks, class_1761.class_7705 visibility) {
        acceptAllAfter(new class_1799(after), stacks, visibility);
    }
    
    default void acceptAllAfter(class_1935 after, Collection<class_1799> stacks) {
        acceptAllAfter(new class_1799(after), stacks);
    }
    
    default void acceptBefore(class_1799 before, class_1799 stack) {
        this.acceptBefore(before, stack, class_1761.class_7705.field_40191);
    }
    
    default void acceptBefore(class_1799 before, class_1935 item, class_1761.class_7705 visibility) {
        this.acceptBefore(before, new class_1799(item), visibility);
    }
    
    default void acceptBefore(class_1799 before, class_1935 item) {
        this.acceptBefore(before, new class_1799(item), class_1761.class_7705.field_40191);
    }
    
    default void acceptAllBefore(class_1799 before, Collection<class_1799> stacks, class_1761.class_7705 visibility) {
        stacks.forEach((stack) -> acceptBefore(before, stack, visibility));
    }
    
    default void acceptAllBefore(class_1799 before, Collection<class_1799> stacks) {
        this.acceptAllBefore(before, stacks, class_1761.class_7705.field_40191);
    }
    
    default void acceptBefore(class_1935 before, class_1799 stack) {
        this.acceptBefore(new class_1799(before), stack);
    }
    
    default void acceptBefore(class_1935 before, class_1935 item, class_1761.class_7705 visibility) {
        this.acceptBefore(new class_1799(before), item, visibility);
    }
    
    default void acceptBefore(class_1935 before, class_1935 item) {
        this.acceptBefore(new class_1799(before), item);
    }
    
    default void acceptAllBefore(class_1935 before, Collection<class_1799> stacks, class_1761.class_7705 visibility) {
        acceptAllBefore(new class_1799(before), stacks, visibility);
    }
    
    default void acceptAllBefore(class_1935 before, Collection<class_1799> stacks) {
        acceptAllBefore(new class_1799(before), stacks);
    }
}
