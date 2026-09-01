package com.example.akaishi.forge.life;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.body.PlayerBodyHelper;
import com.example.akaishi.life.body.PlayerBodyState;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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

    private PlayerBodyCapability() {
    }

    /** 初始化：注册挂载事件 + 注入 common 访问器（Forge 构造阶段调用一次） */
    public static void init() {
        PlayerBodyHelper.registerProvider(player ->
                player.getCapability(PLAYER_BODY).orElse(null));
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
}
