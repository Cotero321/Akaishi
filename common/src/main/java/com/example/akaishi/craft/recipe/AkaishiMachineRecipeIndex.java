package com.example.akaishi.craft.recipe;

import com.example.akaishi.value.ValueCache;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 赤石机器配方的运行时索引（覆盖全部 {@link IAkaishiMachineRecipe} 实现类）。
 *
 * <p>机器每 tick 都要查配方，若直接遍历配方表会随数据包膨胀而变慢；
 * 这里按「配方管理器 + 配方表指纹」缓存分组结果（按 {@link RecipeType} 归类），
 * 数据包重载或 {@code /reload} 后指纹变化即自动重建。
 *
 * <p>查询为组内线性匹配（同族配方通常几十条），因此<b>标签原料也能被正确命中</b>。
 */
public final class AkaishiMachineRecipeIndex {

    /** 缓存条目上限：多个存档/维度切换时防止无界增长 */
    private static final int MAX_CACHED_MANAGERS = 4;

    private static final Map<RecipeManager, Cached> CACHE = new ConcurrentHashMap<>();

    private AkaishiMachineRecipeIndex() {
    }

    private record Cached(ValueCache.Fingerprint fingerprint,
                          Map<RecipeType<?>, List<IAkaishiMachineRecipe>> byType) {
    }

    /** 按输入物品查配方；无匹配（或该配方族不消耗物品）返回 null */
    @Nullable
    public static <T extends Recipe<?> & IAkaishiMachineRecipe> T find(Level level, RecipeType<T> type, ItemStack input) {
        if (level == null || input.isEmpty()) {
            return null;
        }
        for (IAkaishiMachineRecipe recipe : group(level.getRecipeManager(), type)) {
            if (recipe.matchesInput(input)) {
                return cast(recipe);
            }
        }
        return null;
    }

    /** 取某个类型的首条配方（纯能量机器用，如生命固态物固化） */
    @Nullable
    public static <T extends Recipe<?> & IAkaishiMachineRecipe> T single(Level level, RecipeType<T> type) {
        if (level == null) {
            return null;
        }
        List<IAkaishiMachineRecipe> recipes = group(level.getRecipeManager(), type);
        return recipes.isEmpty() ? null : cast(recipes.get(0));
    }

    /** 取某个类型的全部配方（JEI 展示用；顺序即配方表顺序） */
    public static <T extends Recipe<?> & IAkaishiMachineRecipe> List<T> all(RecipeManager manager, RecipeType<T> type) {
        List<IAkaishiMachineRecipe> recipes = group(manager, type);
        List<T> out = new ArrayList<>(recipes.size());
        for (IAkaishiMachineRecipe recipe : recipes) {
            out.add(cast(recipe));
        }
        return out;
    }

    private static List<IAkaishiMachineRecipe> group(RecipeManager manager, RecipeType<?> type) {
        List<IAkaishiMachineRecipe> recipes = cached(manager).byType().get(type);
        return recipes == null ? List.of() : recipes;
    }

    /**
     * 分组按 {@link IAkaishiMachineRecipe#getType()} 建立，组内元素类型必然与查询的类型参数一致，
     * 故此处的强制转换是安全的。
     */
    @SuppressWarnings("unchecked")
    private static <T extends IAkaishiMachineRecipe> T cast(IAkaishiMachineRecipe recipe) {
        return (T) recipe;
    }

    private static Cached cached(RecipeManager manager) {
        ValueCache.Fingerprint fingerprint = ValueCache.fingerprint(manager);
        Cached cached = CACHE.get(manager);
        if (cached != null && cached.fingerprint().equals(fingerprint)) {
            return cached;
        }
        Cached built = new Cached(fingerprint, build(manager));
        if (CACHE.size() >= MAX_CACHED_MANAGERS && !CACHE.containsKey(manager)) {
            CACHE.clear();
        }
        CACHE.put(manager, built);
        return built;
    }

    private static Map<RecipeType<?>, List<IAkaishiMachineRecipe>> build(RecipeManager manager) {
        Map<RecipeType<?>, List<IAkaishiMachineRecipe>> byType = new HashMap<>();
        for (Recipe<?> recipe : manager.getRecipes()) {
            if (recipe instanceof IAkaishiMachineRecipe machine) {
                byType.computeIfAbsent(machine.getType(), key -> new ArrayList<>(8)).add(machine);
            }
        }
        // 值 List 只读语义：后续查询不做结构性修改
        return Map.copyOf(byType);
    }
}
