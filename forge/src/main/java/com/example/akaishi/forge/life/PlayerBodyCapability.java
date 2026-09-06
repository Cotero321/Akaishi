package com.example.akaishi.forge.life;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.life.body.BodyOverviewEntry;
import com.example.akaishi.life.body.BodyOverviewResult;
import com.example.akaishi.life.body.BodyPassiveEntry;
import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.body.PlayerBodyHelper;
import com.example.akaishi.life.body.PlayerBodyState;
import com.example.akaishi.life.body.PlayerBodySync;
import com.example.akaishi.life.organ.OrganEffectResolver;
import com.example.akaishi.life.organ.OrganPassive;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.ForgeMod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家躯体状态 capability（Forge 平台）：
 * - 定义 IPlayerBodyState capability，仅挂载到玩家实体
 * - 通过 ICapabilitySerializable 随玩家存档自动持久化（9 槽位器官 + 排斥值）
 * - 启动时向 common 的 PlayerBodyHelper 注入访问器
 */
public final class PlayerBodyCapability {

    /** capability 实例（单例） */
    public static final Capability<IPlayerBodyState> PLAYER_BODY =
            CapabilityManager.get(new CapabilityToken<IPlayerBodyState>() {
            });

    /** 单例（作为游戏事件总线监听器） */
    public static final PlayerBodyCapability INSTANCE = new PlayerBodyCapability();

    /** 玩家扩展 NBT 中躯体状态快照键：死亡即时写入，重生/克隆时优先恢复，保证跨死亡状态不丢 */
    private static final String SNAPSHOT_KEY = "akaishi_body_snapshot";

    private PlayerBodyCapability() {
    }

    /** 初始化：注册挂载事件 + 注入 common 访问器（Forge 构造阶段调用一次） */
    public static void init() {
        PlayerBodyHelper.registerProvider(player ->
                player.getCapability(PLAYER_BODY).orElse(null));
        // 躯体总览：身体系统当前实际生效的属性净加成（forge 读取玩家属性修饰聚合，随检查仪同步包下发）
        PlayerBodySync.registerOverviewProvider(PlayerBodyCapability::overviewOf);
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) {
            return;
        }
        PlayerBodyState state = new PlayerBodyState();
        event.addCapability(new ResourceLocation(AkaishiMod.MOD_ID, "player_body"),
                new ICapabilitySerializable<CompoundTag>() {
                    @Override
                    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
                        return PLAYER_BODY.orEmpty(cap, LazyOptional.of(() -> state));
                    }

                    @Override
                    public CompoundTag serializeNBT() {
                        return state.save();
                    }

                    @Override
                    public void deserializeNBT(CompoundTag nbt) {
                        state.load(nbt);
                    }
                });
    }

    /** 玩家死亡即时快照：把当前躯体状态（移植器官/排斥/基因/突破）写入玩家扩展 NBT，
     * 随玩家存档持久化；重生克隆走快照恢复，任何内存链路异常都不再导致器官重置为原生 */
    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            player.getCapability(PLAYER_BODY).ifPresent(state ->
                    player.getPersistentData().put(SNAPSHOT_KEY, state.save()));
        }
    }

    /** 玩家重生克隆（含死亡/维度切换）：移植器官跨死亡保留——
     * 死亡重生优先从死亡快照恢复（authoritative）；维度切换直接复制旧实体 capability
     * （避免以登出前旧快照覆盖刚变更的状态）。两种路径均把最新状态写回快照 */
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        Player original = event.getOriginal();
        if (event.isWasDeath()) {
            CompoundTag snapshot = original.getPersistentData().getCompound(SNAPSHOT_KEY);
            if (!snapshot.isEmpty()) {
                player.getCapability(PLAYER_BODY).ifPresent(next -> next.load(snapshot));
            } else {
                original.getCapability(PLAYER_BODY).ifPresent(old ->
                        player.getCapability(PLAYER_BODY).ifPresent(next -> next.load(old.save())));
            }
        } else {
            original.getCapability(PLAYER_BODY).ifPresent(old ->
                    player.getCapability(PLAYER_BODY).ifPresent(next -> next.load(old.save())));
        }
        // 快照同步到新玩家：其后任何一次死亡/维度切换均以该快照为恢复源
        player.getCapability(PLAYER_BODY).ifPresent(state ->
                player.getPersistentData().put(SNAPSHOT_KEY, state.save()));
        // 重生/换维度后立即重建：属性修饰与被动不等待重生实体首个 tick（区块加载前 tick 不跑，
        // 等 tick 会造成死亡→重生的数值空窗），克隆瞬间即恢复全部数值
        AkaishiBodyPassiveHandler.resyncAfterClone(player);
    }

    /** 躯体总览优先级（生命/防御/攻击/速度在前，保证面板阅读顺序稳定） */
    private static final Attribute[] OVERVIEW_ORDER = {
            Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS,
            Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED, Attributes.MOVEMENT_SPEED,
            Attributes.KNOCKBACK_RESISTANCE, Attributes.LUCK, Attributes.JUMP_STRENGTH
    };

    /**
     * 躯体总览计算：读取玩家身上由身体系统挂载的临时属性修饰（"Akaishi organ*" 器官加成与
     * "Akaishi long reach" 长臂），按属性求和 → 实际生效净加成；并附被动叠加计数。
     * 固定优先级属性先逐个取实例采集（覆盖全部身体属性，不依赖同步清单顺序），与属性面板数值一致。
     */
    private static BodyOverviewResult overviewOf(ServerPlayer player) {
        Map<Attribute, Double> merged = new LinkedHashMap<>();
        // 固定优先级属性逐个取实例（保证其修饰必然被采集，攻击/速度等不因同步清单缺失而漏列）
        for (Attribute attr : OVERVIEW_ORDER) {
            collect(player.getAttribute(attr), merged);
        }
        for (AttributeInstance inst : player.getAttributes().getSyncableAttributes()) {
            collect(inst, merged);
        }
        collect(player.getAttribute(ForgeMod.ENTITY_REACH.get()), merged); // 长臂为 Forge 专属属性，单独补充
        List<BodyOverviewEntry> out = new ArrayList<>();
        // 固定顺序输出
        for (Attribute attr : OVERVIEW_ORDER) {
            Double value = merged.remove(attr);
            if (value != null && Math.abs(value) > 1.0E-6) {
                out.add(new BodyOverviewEntry(attr.getDescriptionId(), value));
            }
        }
        for (Map.Entry<Attribute, Double> rest : merged.entrySet()) {
            if (Math.abs(rest.getValue()) > 1.0E-6) {
                out.add(new BodyOverviewEntry(rest.getKey().getDescriptionId(), rest.getValue()));
            }
        }
        // 被动叠加计数（跨器官来源数，供总览页显示叠加强度/罗马级）
        List<BodyPassiveEntry> passives = new ArrayList<>();
        IPlayerBodyState state = PlayerBodyHelper.of(player);
        if (state != null) {
            for (OrganPassive passive : OrganPassive.values()) {
                int count = OrganEffectResolver.countPassive(state, passive);
                if (count > 0) {
                    passives.add(new BodyPassiveEntry(passive.getId(), count));
                }
            }
        }
        return new BodyOverviewResult(out, passives);
    }

    /** 汇总单个属性实例上属于身体系统的修饰值 */
    private static void collect(AttributeInstance inst, Map<Attribute, Double> merged) {
        if (inst == null) {
            return;
        }
        for (AttributeModifier mod : inst.getModifiers()) {
            String name = mod.getName();
            if (name.startsWith("Akaishi organ") || name.equals("Akaishi long reach")) {
                merged.merge(inst.getAttribute(), mod.getAmount(), Double::sum);
            }
        }
    }
}
