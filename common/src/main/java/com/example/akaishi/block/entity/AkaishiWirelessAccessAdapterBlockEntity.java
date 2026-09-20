package com.example.akaishi.block.entity;

import com.example.akaishi.api.recipe.IProcessSource;
import com.example.akaishi.api.transfer.IItemAccess;
import com.example.akaishi.api.transfer.ItemAccessHolder;
import com.example.akaishi.craft.thirdparty.ThirdPartyProcesses;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * 无线接入器方块实体：把<b>相邻的第三方机器</b>接入无线网络。
 *
 * <p><b>为什么需要它</b>：场域成员的准入依赖我们自己的升级组件（{@code hasWirelessReceiver()}），
 * 而第三方机器（MEK/AE2/Create…）不可能持有我们的物品 —— 于是它们永远进不了网络。
 * 本方块是我们自己的物件，替那台机器"背书"：它就是那台机器在网络里的代表。
 *
 * <p><b>识别方式（用户口径：贴相邻，自动识别）</b>：按固定面序扫 6 个相邻格，
 * 取第一个暴露标准物品能力（forge {@code ITEM_HANDLER}，或原版 {@code Container}）的方块实体。
 * 排除自家机台（本来就能入网）与其它接入器（不串接）。
 *
 * <p><b>不缓存</b>：每次都现场解析 —— 机器被拆/被换掉后立刻反映为"未认可"，
 * 不会出现"记着一个已经不在的坐标"。代价是每次查询几次能力查询（很便宜）。
 */
public class AkaishiWirelessAccessAdapterBlockEntity extends BlockEntity implements IProcessSource {

    /** 已认可的邻居：位置 + 访问面（访问面 = 邻居朝向我们这一侧的面，与标准能力取面约定一致） */
    public record Target(BlockPos pos, Direction side) {
    }

    public AkaishiWirelessAccessAdapterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_WIRELESS_ACCESS_ADAPTER.get(), pos, state);
    }

    /** 当前认可的机器；无则 null */
    @Nullable
    public Target target() {
        if (level == null) {
            return null;
        }
        IItemAccess access = ItemAccessHolder.get();
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = worldPosition.relative(dir);
            if (!level.isLoaded(neighbor)) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be == null || be instanceof IUpgradeableMachine
                    || be instanceof AkaishiWirelessAccessAdapterBlockEntity) {
                continue;
            }
            // 邻居朝我们这一面 = dir 的反面
            Direction side = dir.getOpposite();
            if (access.hasItemCapability(level, neighbor, side)) {
                return new Target(neighbor.immutable(), side);
            }
        }
        return null;
    }

    /** 是否已认可到机器（外观/回执用） */
    public boolean linked() {
        return target() != null;
    }

    /**
     * 被认可机器在「第三方认可表」里声明的工序；未声明返回 null（= 只走粗粒度，不提供精确族别）。
     */
    @Nullable
    public ThirdPartyProcesses.Entry recognizedProcess() {
        Target target = target();
        if (target == null || level == null) {
            return null;
        }
        return ThirdPartyProcesses.entryOfBlock(level.getBlockState(target.pos()).getBlock());
    }

    /** 已认可机器的方块（回执显示用；无则 null） */
    @Nullable
    public Block targetBlock() {
        Target target = target();
        return target == null || level == null ? null : level.getBlockState(target.pos()).getBlock();
    }

    // ===== IProcessSource（场域扫描的消费面） =====

    @Override
    public boolean isRecognized() {
        return target() != null;
    }

    /**
     * 认可的机器在第三方认可表里声明的工序；未声明返回空集。
     * <p>空集不等于"没有能力"——未声明的第三方工序走<b>粗粒度</b>准入（场域里有任意一台已认可的机器即可），
     * 这里只提供"精确到族"的那一档。
     */
    @Override
    public Set<RecipeType<?>> providedProcesses() {
        ThirdPartyProcesses.Entry entry = recognizedProcess();
        return entry == null ? Set.of() : Set.of(entry.process());
    }
}
