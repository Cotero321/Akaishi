package com.example.akaishi.api.recipe;

import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

/**
 * 机台自述：本机负责哪一族加工工序。
 *
 * <p><b>为什么需要它</b>：虚拟加工要求"场域内真有这道工序的机台"（机械工序不能凭空完成），
 * 而场域扫描只认 {@code IUpgradeableMachine}，认不出这台机器究竟属于哪一族。
 * 由机台自己报名，扫描侧就不必硬编码"哪台机器对应哪个 {@link RecipeType}"，
 * 新增机器族时只要实现本接口即可自动纳入判定。
 */
public interface IMachineProcessKind {

    /** 本机台负责的配方族；null = 不参与工序族判定 */
    @Nullable
    RecipeType<?> processKind();
}
