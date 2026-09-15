package com.example.akaishi.item;

import com.example.akaishi.block.entity.AkaishiLifeEnergyEmitterBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 生命能量权杖：给能量发射器绑定目标坐标的两步式工具。
 * ① 右键发射器 → 把该发射器坐标存入权杖 NBT（选定）；
 * ② 再右键任意目标方块 → 把该方块坐标写入选定发射器（转动头部指向它）。
 * 潜行 + 右键发射器 → 解除绑定。未选定发射器时右键普通方块不接管交互（PASS）。
 */
public class AkaishiLifeEnergyWandItem extends Item {

    private static final String TAG_EMITTER = "EmitterPos";
    private static final String TAG_HAS_EMITTER = "HasEmitter";

    public AkaishiLifeEnergyWandItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        Player player = ctx.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockPos clicked = ctx.getClickedPos();
        BlockEntity be = level.getBlockEntity(clicked);

        // ① 点击发射器：选定（潜行则解除绑定）
        if (be instanceof AkaishiLifeEnergyEmitterBlockEntity emitter) {
            ItemStack stack = ctx.getItemInHand();
            if (player.isShiftKeyDown()) {
                clearSelection(stack);
                emitter.setTarget(null);
                player.displayClientMessage(Component.translatable("message.akaishi.wand.cleared"), true);
            } else {
                setSelection(stack, clicked);
                player.displayClientMessage(Component.translatable("message.akaishi.wand.selected",
                        clicked.getX(), clicked.getY(), clicked.getZ()), true);
            }
            return InteractionResult.sidedSuccess(false);
        }

        // ② 点击普通方块：把坐标绑定给已选定的发射器（接管该方块的右键交互）
        ItemStack stack = ctx.getItemInHand();
        BlockPos emitterPos = getSelection(stack);
        if (emitterPos == null) {
            return InteractionResult.PASS;
        }
        BlockEntity emitterBe = level.getBlockEntity(emitterPos);
        if (!(emitterBe instanceof AkaishiLifeEnergyEmitterBlockEntity emitter)) {
            clearSelection(stack);
            player.displayClientMessage(Component.translatable("message.akaishi.wand.missing"), true);
            return InteractionResult.sidedSuccess(false);
        }
        emitter.setTarget(clicked);
        player.displayClientMessage(Component.translatable("message.akaishi.wand.bound",
                clicked.getX(), clicked.getY(), clicked.getZ()), true);
        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        BlockPos emitterPos = getSelection(stack);
        if (emitterPos == null) {
            tooltip.add(Component.translatable("tooltip.akaishi.life_energy_wand").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.akaishi.life_energy_wand.selected",
                    emitterPos.getX(), emitterPos.getY(), emitterPos.getZ()).withStyle(ChatFormatting.GREEN));
        }
    }

    private static void setSelection(ItemStack stack, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong(TAG_EMITTER, pos.asLong());
        tag.putBoolean(TAG_HAS_EMITTER, true);
    }

    private static void clearSelection(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_EMITTER);
            tag.remove(TAG_HAS_EMITTER);
        }
    }

    @Nullable
    private static BlockPos getSelection(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean(TAG_HAS_EMITTER)) {
            return null;
        }
        return BlockPos.of(tag.getLong(TAG_EMITTER));
    }
}
