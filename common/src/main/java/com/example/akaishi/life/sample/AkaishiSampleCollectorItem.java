package com.example.akaishi.life.sample;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.menu.EnergyFormat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 样本采集器：随身携带的生命能量容器（仅用于采集）。
 * - 右键生物（Forge EntityInteract 事件驱动）：消耗生命能量抽取生命样本
 * - 右键生命能量方块（储存器/管道/提纯器等 IEnergyProvider）：从对方充能
 * 能量以 NBT 持久化，容量 100k，每次采集消耗 5k（满电可采 20 次）。
 */
public class AkaishiSampleCollectorItem extends Item {

    /** 物品 NBT 中能量存储键 */
    public static final String TAG_ENERGY = "CollectorEnergy";
    /** 容量 */
    public static final long CAPACITY = 100_000;
    /** 每次采集消耗的生命能量 */
    public static final long COST_PER_SAMPLE = 5_000;
    /** 单次右键充能抽取上限 */
    public static final long CHARGE_RATE = 10_000;

    public AkaishiSampleCollectorItem(Properties properties) {
        super(properties);
    }

    // ===== 能量存取（NBT）=====

    public static long getEnergyStored(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ENERGY);
        return tag != null ? tag.getLong("Energy") : 0;
    }

    public static void setEnergy(ItemStack stack, long amount) {
        CompoundTag tag = stack.getOrCreateTagElement(TAG_ENERGY);
        tag.putLong("Energy", Math.max(0, Math.min(amount, CAPACITY)));
    }

    private static void addEnergy(ItemStack stack, long amount) {
        setEnergy(stack, getEnergyStored(stack) + amount);
    }

    // ===== 右键方块：从生命能量设备充能 =====

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        if (level.isClientSide || player == null) {
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(ctx.getClickedPos()) instanceof IEnergyProvider provider) {
            IEnergyStorage storage = provider.getEnergyStorage(LifeEnergyType.INSTANCE);
            if (storage != null) {
                long missing = CAPACITY - getEnergyStored(stack);
                long toExtract = storage.extractEnergy(Math.min(missing, CHARGE_RATE), false);
                if (toExtract > 0) {
                    addEnergy(stack, toExtract);
                    player.sendSystemMessage(Component.translatable("message.akaishi.sample.charged",
                            EnergyFormat.format(toExtract)));
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    // ===== 采集逻辑（Forge EntityInteract 事件调用，仅服务端）=====

    /**
     * 尝试从生物抽取生命样本。
     *
     * @return 是否成功执行采集（成功则取消该次交互，如喂食）
     */
    public static boolean tryCollect(Player player, LivingEntity target, ItemStack collector) {
        if (collector.isEmpty() || !(collector.getItem() instanceof AkaishiSampleCollectorItem)) {
            return false;
        }
        SampleGroup group = SampleGroup.of(target);
        if (group == null) {
            return false;
        }
        long stored = getEnergyStored(collector);
        if (stored < COST_PER_SAMPLE) {
            player.sendSystemMessage(Component.translatable("message.akaishi.sample.no_energy"));
            return false;
        }
        // 成功率 = 分组基础值 + 血量加成（血量每低于 50% 加成 10，封顶 95）
        int rate = Math.min(95, group.getBaseCollectRate()
                + (int) (target.getHealth() < target.getMaxHealth() * 0.5 ? 10 : 0)
                + (int) (target.getHealth() < target.getMaxHealth() * 0.25 ? 10 : 0));
        // 成功：全额扣能量 + 生成样本（纯度偏斜规则见 AkaishiLifeSampleItem.createRolled）
        if (player.getRandom().nextInt(100) < rate) {
            setEnergy(collector, stored - COST_PER_SAMPLE);
            String entityId = EntityType.getKey(target.getType()).toString();
            ItemStack sample = AkaishiLifeSampleItem.createRolled(group, entityId, player.getRandom());
            if (!player.getInventory().add(sample)) {
                player.drop(sample, false);
            }
            player.sendSystemMessage(Component.translatable("message.akaishi.sample.collect",
                    Component.translatable(group.getNameKey()), AkaishiLifeSampleItem.getPurity(sample)));
        } else {
            // 失败：仅消耗一半能量，样本流失
            setEnergy(collector, stored - COST_PER_SAMPLE / 2);
            player.sendSystemMessage(Component.translatable("message.akaishi.sample.fail"));
        }
        // 无论成败都取消该次交互（防止误触发喂食/交易等默认行为）
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.akaishi.energy",
                EnergyFormat.format(getEnergyStored(stack)), EnergyFormat.format(CAPACITY)));
        tooltip.add(Component.translatable("gui.akaishi.sample_collector.hint"));
        tooltip.add(Component.translatable("gui.akaishi.sample_collector.charge_hint"));
    }
}
