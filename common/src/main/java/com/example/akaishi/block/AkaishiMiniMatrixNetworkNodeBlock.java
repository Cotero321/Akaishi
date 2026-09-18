package com.example.akaishi.block;

import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.wireless.WirelessFieldManager;
import com.example.akaishi.wireless.WirelessNodeRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 无线网络节点方块：配合矩阵内腔的「无线拓展升级」使用。
 * <p>
 * 每块节点自带 <b>1 区块半径</b>的子场域（同样走弱加载票据），由场域内的微缩矩阵终端按
 * 拓展升级数量申领（最多 3 个、就近优先）——即需求里的「让专用网络节点附加无线场域」。
 * <p>
 * 纯结构件：<b>无界面、无 tick</b>，只做两件事：放置/被拆时在
 * {@link WirelessNodeRegistry} 登记/注销；被申领后由场域服务挂票。
 * 携带一个<b>极简方块实体</b>（{@code AkaishiMiniMatrixNetworkNodeBlockEntity}），
 * 目的是把登记绑到区块生命周期上，使服务器重启后节点能随区块加载自动重新登记。
 * 未安装拓展升级（或不在任何场域内）时，节点只是普通装饰方块，不产生加载开销。
 */
public class AkaishiMiniMatrixNetworkNodeBlock extends AkaishiMachineBlock {

    /**
     * 已被矩阵申领（子场域生效中）。
     * <p>
     * 由申领方（微缩矩阵终端）在申领/释放时写入，是节点唯一的<b>可见状态</b>：
     * 方块模型据此换成"通电"款，客户端渲染器据此决定是否画它那 1 区块子场域屏障。
     * 状态写在方块状态而非方块实体，是为了让客户端在<b>方块更新包</b>里就拿到，
     * 不依赖方块实体的同步标签。
     */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public AkaishiMiniMatrixNetworkNodeBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_CYAN)
                .strength(3.0F)
                .sound(SoundType.METAL));
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    /** 只承载登记表的生命周期钩子，无 tick（基类默认 ticker 为 null） */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_MINI_MATRIX_NETWORK_NODE.get().create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** 放置即登记为候选节点（是否真的生效取决于附近矩阵的拓展升级数量；与 BE 的 onLoad 幂等互补） */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level instanceof ServerLevel serverLevel && !oldState.is(state.getBlock())) {
            WirelessNodeRegistry.register(serverLevel, pos);
        }
    }

    /** 被拆即注销，并立即释放其子场域票据（不看持有者：载体没了，全部申领者的占位一起摘） */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            WirelessNodeRegistry.unregister(serverLevel, pos);
            WirelessFieldManager.releaseAll(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.akaishi.matrix.node.hint"));
        tooltip.add(Component.translatable("gui.akaishi.matrix.node.max_hint"));
    }
}
