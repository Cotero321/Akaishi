package com.example.akaishi.api.recipe;

import net.minecraft.world.item.crafting.RecipeType;

import java.util.Set;

/**
 * 场域内的「工序供给点」：接入器认可到第三方机器后，替它提供其工序族。
 *
 * <p><b>为什么放在 api 而不是让扫描侧直接认接入器类</b>：场域扫描属于 {@code craft} 包，
 * 若它直接引用 {@code block.entity} 就形成 craft ↔ block.entity 的双向依赖。
 * 这里只暴露"供给"这一件事，接入器实现它即可被扫描到（ISP）。
 */
public interface IProcessSource {

    /** 是否已认可到一台第三方机器（未认可 = 这个供给点不存在） */
    boolean isRecognized();

    /** 该机器在「第三方认可表」里声明的工序族；未声明返回空集（只走粗粒度准入） */
    Set<RecipeType<?>> providedProcesses();
}
