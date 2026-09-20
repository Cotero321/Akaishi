package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiWirelessAccessAdapterBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.craft.thirdparty.ThirdPartyProcesses;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 无线接入器：贴在第三方机器旁边，把它接入无线场域，使它能参与加工链路的准入判定。
 *
 * <p><b>准入</b>（用户口径）：贴相邻、自动识别，<b>不需要</b>额外升级件 —— 接入器本身就是网络设备。
 * 识别对象 = 相邻 6 面中第一个暴露标准物品能力的方块实体（排除自家机台与其它接入器）。
 *
 * <p><b>可见性</b>：{@link #LINKED} 方块状态决定外观（未认可款 / 已认可款），
 * 右键给出准确回执（现场重算，含"认到了哪台、它能提供哪些工序"）。
 * 方块状态只是外观：所有判定都走方块实体的现场解析，因此外观偶尔滞后不影响功能正确性。
 */
public class AkaishiWirelessAccessAdapterBlock extends AkaishiMachineBlock {

    /** 已认可到相邻第三方机器（外观用；权威判定在方块实体里现场解析） */
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");

    public AkaishiWirelessAccessAdapterBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_CYAN)
                .strength(3.5F)
                .sound(SoundType.METAL));
        registerDefaultState(stateDefinition.any().setValue(LINKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LINKED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_WIRELESS_ACCESS_ADAPTER.get().create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        refreshLinkedState(level, pos);
    }

    /** 邻居变化（旁边放上/拆掉机器）即刷新外观 */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
            boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        refreshLinkedState(level, pos);
    }

    /** 右键回执：认到了哪台机器、它能提供哪些工序（现场重算，不用外观状态） */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof AkaishiWirelessAccessAdapterBlockEntity be)) {
            return InteractionResult.PASS;
        }
        AkaishiWirelessAccessAdapterBlockEntity.Target target = be.target();
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.akaishi.adapter.no_target"), true);
            return InteractionResult.CONSUME;
        }
        Block machine = be.targetBlock();
        Component name = machine == null ? Component.literal("?") : machine.getName();
        ThirdPartyProcesses.Entry entry = be.recognizedProcess();
        player.displayClientMessage(entry == null
                ? Component.translatable("message.akaishi.adapter.linked_generic", name,
                        String.format("%d %d %d", target.pos().getX(), target.pos().getY(), target.pos().getZ()))
                : Component.translatable("message.akaishi.adapter.linked_exact", name,
                        entry.process().toString()), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.akaishi.adapter.hint"));
        tooltip.add(Component.translatable("gui.akaishi.adapter.hint_exact"));
    }

    /** 按现场解析结果刷新外观（相同则不写，避免多余方块更新） */
    private static void refreshLinkedState(Level level, BlockPos pos) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof AkaishiWirelessAccessAdapterBlockEntity be)) {
            return;
        }
        boolean linked = be.linked();
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(LINKED) || state.getValue(LINKED) == linked) {
            return;
        }
        level.setBlock(pos, state.setValue(LINKED, linked), Block.UPDATE_ALL);
    }
}
