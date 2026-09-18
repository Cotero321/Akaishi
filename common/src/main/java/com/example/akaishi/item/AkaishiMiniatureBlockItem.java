package com.example.akaishi.item;

import com.example.akaishi.api.miniature.MiniatureTerminalAdapter;
import com.example.akaishi.api.miniature.MiniatureTerminalRegistry;
import com.example.akaishi.wireless.WirelessNetworkManager;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 微缩终端的方块物品（可携带形态，即"U 盘"）。
 * <p>
 * 存在的唯一理由是把「这枚芯片里装的是哪台终端」告诉玩家 —— 数据本身由 {@code BlockEntityTag}
 * 承载（拆方块自动写入、放置自动还原），物品侧只负责把它读出来展示。
 * <p>
 * 交互上不做任何特殊处理：右键 = 放置（含蹲下），右键已放置的方块才开界面。
 */
public class AkaishiMiniatureBlockItem extends BlockItem {

    public AkaishiMiniatureBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag == null || !tag.contains(MiniatureTerminalRegistry.TAG_TYPE)) {
            // 空壳：还没装过终端数据（例如创造栏直接拿的）
            tooltip.add(Component.translatable("item.akaishi.miniature.empty").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("item.akaishi.miniature.hint").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        ResourceLocation typeId = ResourceLocation.tryParse(tag.getString(MiniatureTerminalRegistry.TAG_TYPE));
        MiniatureTerminalAdapter adapter = MiniatureTerminalRegistry.adapter(typeId);
        CompoundTag payload = tag.getCompound(MiniatureTerminalRegistry.TAG_PAYLOAD);
        // 适配器缺失（族被移除）时给出明确提示，而不是显示裸 id
        tooltip.add(Component.translatable("item.akaishi.miniature.type",
                adapter == null ? Component.translatable("item.akaishi.miniature.unknown")
                        : adapter.displayName(payload)).withStyle(ChatFormatting.GRAY));
        if (tag.hasUUID(MiniatureTerminalRegistry.TAG_TERMINAL_ID)) {
            UUID id = tag.getUUID(MiniatureTerminalRegistry.TAG_TERMINAL_ID);
            tooltip.add(Component.translatable("item.akaishi.miniature.id",
                    WirelessNetworkManager.shortId(id)).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("item.akaishi.miniature.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
