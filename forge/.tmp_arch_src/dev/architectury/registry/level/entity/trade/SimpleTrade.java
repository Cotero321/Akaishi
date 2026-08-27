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

package dev.architectury.registry.level.entity.trade;

import net.minecraft.class_1297;
import net.minecraft.class_1799;
import net.minecraft.class_1914;
import net.minecraft.class_3853;
import net.minecraft.class_5819;
import org.jetbrains.annotations.Nullable;

/**
 * This class is the easiest implementation of a trade object.
 * All trades added by vanilla do have custom classes like {@link class_3853.class_4161}, but they aren't accessible.
 * <p>
 * Instead of widening the access of those classes or recreating them, this class was added to serve a basic trading implementation.
 * To register a trade, just call
 * {@link TradeRegistry#registerVillagerTrade(net.minecraft.class_3852, int, class_3853.class_1652...)}
 * or
 * {@link TradeRegistry#registerTradeForWanderingTrader(boolean, class_3853.class_1652...)}.
 */
public record SimpleTrade(class_1799 primaryPrice, class_1799 secondaryPrice,
                          class_1799 sale, int maxTrades, int experiencePoints,
                          float priceMultiplier) implements class_3853.class_1652 {
    /**
     * Constructor for creating the trade.
     * You can take a look at all the values the vanilla game uses right here {@link class_3853#field_17067}.
     *
     * @param primaryPrice     The first price a player has to pay to get the 'sale' stack.
     * @param secondaryPrice   A optional, secondary price to pay as well as the primary one. If not needed just use {@link class_1799#field_8037}.
     * @param sale             The ItemStack which a player can purchase in exchange for the two prices.
     * @param maxTrades        The amount of trades one villager or wanderer can do. When the amount is surpassed, the trade can't be purchased anymore.
     * @param experiencePoints How much experience points does the player get, when trading. Vanilla uses between 2 and 30 for this.
     * @param priceMultiplier  How much should the price rise, after the trade is used. It is added to the stack size of the primary price. Vanilla uses between 0.05 and 0.2.
     */
    public SimpleTrade {
    }
    
    @Nullable
    @Override
    public class_1914 method_7246(class_1297 entity, class_5819 random) {
        return new class_1914(this.primaryPrice, this.secondaryPrice, this.sale, this.maxTrades, this.experiencePoints, this.priceMultiplier);
    }
}
