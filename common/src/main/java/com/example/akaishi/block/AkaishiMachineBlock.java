package com.example.akaishi.block;

import com.example.akaishi.api.DataCarrierHelper;
import com.example.akaishi.api.IDataCarrier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

/**
 * 数据保留机器方块基类（仿 MEK ISustainedData 的掉落保存模式）。
 * 覆写 {@link #getDrops(BlockState, LootParams.Builder)}：方块被破坏（玩家/爆炸/活塞）掉落时，
 * loot 上下文携带方块实体，若其实现了 {@link IDataCarrier}，则把 BE 的 NBT
 * （排除各 BE 声明的键，如已随方块掉落的内部物品）写入掉落物 BlockEntityTag；
 * 重新放置后原版 BlockItem 自动读取并恢复能量/进度/温度等。
 * 同时覆写 {@link #getCloneItemStack}：创造模式中键拾取同样写入数据，两个入口行为一致。
 */
public abstract class AkaishiMachineBlock extends BaseEntityBlock {

    public AkaishiMachineBlock(Properties properties) {
        // 统一挖掘手感：所有机器一律铁镐可挖、硬度/抗爆取自 AkaishiBlockStats。
        // 放在基类而不是 89 个子类里各写各的 —— 子类即便传了 strength(...) 也会被这里覆盖。
        super(AkaishiBlockStats.machine(properties));
    }

    /**
     * 抗爆：多方块结构件（按注册 id 前缀识别，见 {@link AkaishiBlockStats#isStructureBlock}）
     * 提到黑曜石级，避免结构被 TNT / 苦力怕炸开一角而整台失效；其余机器走原版属性值。
     * <p>1.20.1 的 {@code Block#getExplosionResistance} 只有本无参重载（带 state 的重载是更高版本才有）。
     */
    @Override
    public float getExplosionResistance() {
        return AkaishiBlockStats.isStructureBlock(this)
                ? AkaishiBlockStats.STRUCTURE_BLAST_RESISTANCE
                : super.getExplosionResistance();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof IDataCarrier carrier) {
            for (ItemStack drop : drops) {
                if (drop.is(state.getBlock().asItem())) {
                    DataCarrierHelper.saveToItem(be, drop, carrier.excludedKeys());
                }
            }
        }
        return drops;
    }

    /** 创造模式中键拾取：与掉落一致地保留 BE 数据 */
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IDataCarrier carrier) {
            DataCarrierHelper.saveToItem(be, stack, carrier.excludedKeys());
        }
        return stack;
    }
}
