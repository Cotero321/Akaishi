package com.example.akaishi.miniature;

import com.example.akaishi.api.miniature.IMiniaturizableTerminal;
import com.example.akaishi.api.miniature.MiniatureTerminalRegistry;
import com.example.akaishi.api.security.AkaishiSecurityPermission;
import com.example.akaishi.block.AkaishiMiniatureBlocks;
import com.example.akaishi.block.entity.MiniatureTerminalBlockEntity;
import com.example.akaishi.item.AkaishiMiniatureBlockItem;
import com.example.akaishi.wireless.TerminalSecurity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 「坍缩」通用流程：把一台成型终端收成单个微缩方块。
 * <p>
 * 步骤固定为：<b>判主控权限 → 取族数据 + 记结构快照 → 消耗整套结构（不掉落）→ 原地产出微缩方块</b>。
 * 顺序是硬要求：
 * <ul>
 *   <li>快照与族数据必须<b>先取</b>再做任何销毁动作 —— 拆一半再取就残缺了；</li>
 *   <li>结构与数据是<b>互斥</b>的两份：方块被消耗、数据只进微缩方块，因此不存在"数据在方块里、原机器还在"的复制；</li>
 *   <li>用 {@code removeBlock(false)} 消耗，避免掉落物把同一批数据又吐回世界（储存单元的内容已进 payload）。</li>
 * </ul>
 * <b>解包（还原）首版不做</b>，但结构快照照写：只记「相对坐标 + 方块 id + 终端在箱体内的偏移」，
 * 成本极低，却把"以后能不能原样还原"这条路留着（不写就永久堵死）。
 * <p>
 * 快照由<b>微缩方块自己</b>持有并落盘，不塞进族 payload —— 族状态的 {@code save()} 只写自己那套键，
 * 塞进去会在第一次自动存盘时被抹掉。
 */
public final class MiniatureCollapse {

    /** 快照内的方块 id 键 */
    private static final String TAG_BLOCK = "B";
    /** 快照内的相对坐标键 */
    private static final String TAG_POS = "P";

    private MiniatureCollapse() {
    }

    /**
     * 潜行右键入口（各终端方块 {@code use()} 开头调用）。
     * <p>
     * 判定口径：<b>潜行 + 目标方块是可微缩终端</b> ⇒ 拦截本次交互并尝试坍缩（含未成型 / 无权限的提示），
     * <b>绝不退回去开界面</b> —— 否则玩家想微缩却弹出一堆界面，行为不可预期。
     * 非潜行、或目标不是可微缩终端 ⇒ 返回 false，交互照旧（开界面 / 交给方块自身逻辑）。
     *
     * @return true 表示本次交互已被本机制接管（调用方直接返回，不要再开界面）
     */
    public static boolean handleSneakUse(Level level, BlockPos pos, Player player) {
        if (player == null || !player.isShiftKeyDown()
                || !(level.getBlockEntity(pos) instanceof IMiniaturizableTerminal)) {
            return false;
        }
        if (level instanceof ServerLevel serverLevel) {
            collapse(serverLevel, pos, player);
        }
        return true;
    }

    /**
     * 尝试把 pos 处的终端坍缩为微缩方块。
     *
     * @return true 表示已完成坍缩（调用方据此决定要不要拦截本次操作）
     */
    public static boolean collapse(ServerLevel level, BlockPos pos, Player actor) {
        if (level == null || pos == null || actor == null
                || !(level.getBlockEntity(pos) instanceof IMiniaturizableTerminal terminal)) {
            return false;
        }
        BlockPos min = terminal.structureMin();
        BlockPos max = terminal.structureMax();
        if (min == null || max == null) {
            actor.displayClientMessage(Component.translatable("message.akaishi.miniature.not_formed"), true);
            return false;
        }
        if (!mayControl(actor, terminal.security())) {
            actor.displayClientMessage(Component.translatable("message.akaishi.miniature.denied"), true);
            return false;
        }
        // 装载需要一枚【空的】微缩终端当容器（用户口径）：手上有内容的芯片不能再次装载 ——
        // 一枚芯片只装一台终端；装完这枚就"有内容"了，想再装得另取一枚空的。
        ItemStack carrier = findBlankChip(actor);
        if (carrier == null) {
            actor.displayClientMessage(Component.translatable("message.akaishi.miniature.need_blank"), true);
            return false;
        }
        // 用户口径：<b>已经装了内容的芯片就不能再装载</b> —— 触发者手上那枚必须还是"空的"
        // （见上面的 findBlankChip：有内容的芯片不能当容器）
        ResourceLocation typeId = terminal.miniatureTypeId();
        UUID terminalId = terminal.terminalId();
        if (typeId == null || terminalId == null) {
            return false;
        }
        // 1) 先算清"要消耗哪些方块"，并检查里面有没有已装载的微缩终端：
        //    有 ⇒ 直接拒绝（在动手之前）。否则那枚芯片会被连数据一起抹掉（它自己也是一份终端数据）。
        List<BlockPos> extras = terminal.extraConsumedBlocks();
        Set<BlockPos> consumed = new LinkedHashSet<>();
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            consumed.add(cursor.immutable());
        }
        for (BlockPos extra : extras) {
            if (extra != null) {
                consumed.add(extra.immutable());
            }
        }
        for (BlockPos p : consumed) {
            if (level.getBlockEntity(p) instanceof MiniatureTerminalBlockEntity chip
                    && chip.isLoadedTerminal()) {
                actor.displayClientMessage(Component.translatable("message.akaishi.miniature.occupied"), true);
                return false;
            }
        }
        // 2) 取数据：族数据 + 结构快照（任何销毁动作之前）
        CompoundTag payload = terminal.captureMiniature();
        CompoundTag structure = captureStructure(level, pos, min, max);
        // 3) 消耗整套结构 + 族声明的贴装件。
        //    贴装件允许装在外侧 1 格（不在箱体内），但它的数据已经进了 payload ——
        //    若把它留在世界上，就是同一份数据的第二份（刷物品 / 刷能量），故必须一并消耗。
        for (BlockPos p : consumed) {
            if (!level.getBlockState(p).isAir()) {
                level.removeBlock(p, false);
            }
        }
        // 4) 原地产出微缩方块
        if (!level.getBlockState(pos).isAir()) {
            level.destroyBlock(pos, false);
        }
        level.setBlock(pos, AkaishiMiniatureBlocks.CHISHI_MINIATURE_TERMINAL.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(pos) instanceof MiniatureTerminalBlockEntity miniature)) {
            // 极端异常：方块实体没建出来 —— 明确失败，不假装成功（此时结构已消耗，提示玩家报错）
            actor.displayClientMessage(Component.translatable("message.akaishi.miniature.failed"), true);
            return false;
        }
        if (miniature.install(typeId, terminalId, payload)) {
            miniature.setStructureSnapshot(structure);
            miniature.syncToClient(); // 数据要下发客户端：菜单是按坐标取本地方块实体造的
            carrier.shrink(1); // 容器用掉了：这枚空芯片变成原地那枚"已装载"的方块
            actor.displayClientMessage(Component.translatable("message.akaishi.miniature.collapsed"), true);
        } else {
            // 二次覆盖被拒（该位置已有装载过的芯片，例如同方块复用了旧方块实体）：
            // 数据不能吞掉、也<b>不能覆盖旧芯片</b> —— 落成一枚带同样数据的新微缩件
            dropChip(level, pos, typeId, terminalId, payload);
            carrier.shrink(1);
            actor.displayClientMessage(Component.translatable("message.akaishi.miniature.rejected"), true);
        }
        return true;
    }

    /**
     * 找触发者手上（主手优先，其次副手）的<b>空</b>微缩终端物品。
     * <p>
     * 这就是"一艘船一个位"的那枚容器：已经装了内容的芯片不能当容器（用户口径），
     * 所以只认无 {@code BlockEntityTag} 类型标记的空壳。
     */
    private static ItemStack findBlankChip(Player player) {
        ItemStack main = player.getMainHandItem();
        if (isBlankChip(main)) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        return isBlankChip(off) ? off : null;
    }

    /** 是否为空壳微缩终端（本族物品且未带终端类型） */
    private static boolean isBlankChip(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AkaishiMiniatureBlockItem)) {
            return false;
        }
        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        return tag == null || !tag.contains(MiniatureTerminalRegistry.TAG_TYPE);
    }

    /** 兜底：把一段终端数据落成一枚微缩件物品（数据绝不静默蒸发） */
    private static void dropChip(ServerLevel level, BlockPos pos, ResourceLocation typeId,
            UUID terminalId, CompoundTag payload) {
        ItemStack chip = new ItemStack(AkaishiMiniatureBlocks.CHISHI_MINIATURE_TERMINAL.get());
        CompoundTag be = new CompoundTag();
        be.putString(MiniatureTerminalRegistry.TAG_TYPE, typeId.toString());
        be.putUUID(MiniatureTerminalRegistry.TAG_TERMINAL_ID, terminalId);
        be.put(MiniatureTerminalRegistry.TAG_PAYLOAD, payload);
        chip.getOrCreateTag().put("BlockEntityTag", be);
        Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, chip);
    }

    /**
     * 主控权限：归属者本人 / op / 权限表里显式授了「安全」档。
     * <p>
     * 不能只用 {@code security.check(uuid, SECURITY)}：项目口径是"权限表为空时全放行"，那是给
     * <b>使用</b>的宽松口径；坍缩是<b>不可逆的销毁性操作</b>，必须确认"确实是主人"，
     * 否则任何路人都能把别人的终端压成芯片。故这里额外要求「表非空且显式授予」。
     */
    private static boolean mayControl(Player actor, TerminalSecurity security) {
        if (actor.hasPermissions(4)) {
            return true;
        }
        UUID id = actor.getUUID();
        if (id.equals(security.owner())) {
            return true;
        }
        return security.entryCount() > 0 && security.check(id, AkaishiSecurityPermission.SECURITY);
    }

    /** 结构快照：逐格记「相对坐标 + 方块 id」（空气不记，还原时按空腔处理），另记终端方块自身的偏移 */
    private static CompoundTag captureStructure(ServerLevel level, BlockPos terminalPos,
            BlockPos min, BlockPos max) {
        ListTag entries = new ListTag();
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            BlockPos p = cursor.immutable();
            BlockState state = level.getBlockState(p);
            if (state.isAir()) {
                continue;
            }
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (blockId == null) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putIntArray(TAG_POS,
                    new int[] {p.getX() - min.getX(), p.getY() - min.getY(), p.getZ() - min.getZ()});
            entry.putString(TAG_BLOCK, blockId.toString());
            entries.add(entry);
        }
        CompoundTag snapshot = new CompoundTag();
        snapshot.put(MiniatureTerminalBlockEntity.TAG_STRUCTURE_BLOCKS, entries);
        snapshot.putIntArray(MiniatureTerminalBlockEntity.TAG_TERMINAL_OFFSET,
                new int[] {terminalPos.getX() - min.getX(), terminalPos.getY() - min.getY(),
                        terminalPos.getZ() - min.getZ()});
        return snapshot;
    }
}
