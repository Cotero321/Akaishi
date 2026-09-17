package com.example.akaishi.value;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;

/**
 * 配方原料提取：把任意加载器 / 任意模组的配方归一成 {@link RecipeCost}。
 *
 * <p>优先走原版 {@link Recipe#getIngredients()}；若第三方配方未暴露原料，
 * 则反射扫描其 {@link Ingredient} 字段兜底。流体原料仅按<b>类型名</b>反射识别，
 * 因此不会在通用模块内引用 Forge 类。
 */
public final class RecipeIngredients {

    /** 流体原料：按类型名反射识别后归一 */
    public record FluidAmount(Fluid fluid, long mb) {
    }

    /** 一张配方的估价视图：产物 + 每格候选物品 + 流体原料 */
    public record RecipeCost(Item resultItem, int outputCount, List<Item[]> ingredients, List<FluidAmount> fluidsIn) {
    }

    /** 单张配方的 Ingredient 字段缓存，避免重复扫描继承链 */
    private static final Map<Class<?>, List<Field>> INGREDIENT_FIELDS = new ConcurrentHashMap<>();

    /** 单张配方的流体字段缓存 */
    private static final Map<Class<?>, List<Field>> FLUID_FIELDS = new ConcurrentHashMap<>();

    private RecipeIngredients() {
    }

    /** 收集全部可用配方（产物非空）的估价视图 */
    public static List<RecipeCost> collect(RecipeManager manager, RegistryAccess access) {
        List<RecipeCost> costs = new ArrayList<>();
        for (Recipe<?> recipe : manager.getRecipes()) {
            RecipeCost cost = describe(recipe, access);
            if (cost != null) {
                costs.add(cost);
            }
        }
        return costs;
    }

    private static RecipeCost describe(Recipe<?> recipe, RegistryAccess access) {
        ItemStack result;
        try {
            result = recipe.getResultItem(access);
        } catch (RuntimeException e) {
            // 个别模组配方在缺少上下文时会抛异常，跳过即可，不影响整体估值
            return null;
        }
        if (result == null || result.isEmpty()) {
            return null;
        }
        List<Ingredient> ingredients = new ArrayList<>();
        NonNullList<Ingredient> declared = recipe.getIngredients();
        if (declared != null) {
            for (Ingredient ingredient : declared) {
                if (ingredient != null && !ingredient.isEmpty()) {
                    ingredients.add(ingredient);
                }
            }
        }
        if (ingredients.isEmpty()) {
            ingredients.addAll(reflectIngredients(recipe));
        }
        List<Item[]> slots = new ArrayList<>(ingredients.size());
        for (Ingredient ingredient : ingredients) {
            Item[] candidates = candidateItems(ingredient);
            if (candidates.length > 0) {
                slots.add(candidates);
            }
        }
        List<FluidAmount> fluids = reflectFluids(recipe);
        return new RecipeCost(result.getItem(), Math.max(1, result.getCount()), List.copyOf(slots), fluids);
    }

    /** 取一格的候选物品；空标签会被原版填成屏障，需剔除 */
    private static Item[] candidateItems(Ingredient ingredient) {
        ItemStack[] stacks;
        try {
            stacks = ingredient.getItems();
        } catch (RuntimeException e) {
            return new Item[0];
        }
        List<Item> items = new ArrayList<>(stacks.length);
        for (ItemStack stack : stacks) {
            if (stack.isEmpty() || stack.is(Items.BARRIER)) {
                continue;
            }
            Item item = stack.getItem();
            if (!items.contains(item)) {
                items.add(item);
            }
        }
        return items.toArray(new Item[0]);
    }

    /** 反射兜底：读取配方实例上的 Ingredient / Ingredient[] / Collection 字段 */
    private static List<Ingredient> reflectIngredients(Recipe<?> recipe) {
        List<Field> fields = INGREDIENT_FIELDS.computeIfAbsent(recipe.getClass(), RecipeIngredients::scanIngredientFields);
        if (fields.isEmpty()) {
            return List.of();
        }
        List<Ingredient> ingredients = new ArrayList<>();
        for (Field field : fields) {
            try {
                collectIngredients(field.get(recipe), ingredients);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // 单个字段不可读不影响整张配方
            }
        }
        return ingredients;
    }

    private static void collectIngredients(Object value, List<Ingredient> out) {
        if (value instanceof Ingredient ingredient) {
            if (!ingredient.isEmpty()) {
                out.add(ingredient);
            }
        } else if (value instanceof Ingredient[] array) {
            for (Ingredient ingredient : array) {
                if (ingredient != null && !ingredient.isEmpty()) {
                    out.add(ingredient);
                }
            }
        } else if (value instanceof Collection<?> collection) {
            for (Object element : collection) {
                collectIngredients(element, out);
            }
        }
    }

    private static List<Field> scanIngredientFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                Class<?> fieldType = field.getType();
                boolean candidate = Ingredient.class.isAssignableFrom(fieldType)
                        || Ingredient[].class.isAssignableFrom(fieldType)
                        || Collection.class.isAssignableFrom(fieldType);
                if (candidate && makeAccessible(field)) {
                    fields.add(field);
                }
            }
        }
        return List.copyOf(fields);
    }

    /** 反射读取流体原料（仅按类型名判定，避免通用模块依赖平台类） */
    private static List<FluidAmount> reflectFluids(Recipe<?> recipe) {
        List<Field> fields = FLUID_FIELDS.computeIfAbsent(recipe.getClass(), RecipeIngredients::scanFluidFields);
        if (fields.isEmpty()) {
            return List.of();
        }
        List<FluidAmount> fluids = new ArrayList<>();
        for (Field field : fields) {
            try {
                collectFluids(field.get(recipe), fluids);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // 忽略单个字段读取失败
            }
        }
        return fluids;
    }

    private static void collectFluids(Object value, List<FluidAmount> out) {
        if (value == null) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object element : collection) {
                collectFluids(element, out);
            }
            return;
        }
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            for (Object element : array) {
                collectFluids(element, out);
            }
            return;
        }
        try {
            Method getFluid = value.getClass().getMethod("getFluid");
            Method getAmount = value.getClass().getMethod("getAmount");
            Object fluid = getFluid.invoke(value);
            Object amount = getAmount.invoke(value);
            if (fluid instanceof Fluid resolved && amount instanceof Number number && number.longValue() > 0) {
                out.add(new FluidAmount(resolved, number.longValue()));
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // 不是流体原料对象，忽略
        }
    }

    private static List<Field> scanFluidFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                if (mentionsFluid(field) && makeAccessible(field)) {
                    fields.add(field);
                }
            }
        }
        return List.copyOf(fields);
    }

    private static boolean mentionsFluid(Field field) {
        if (field.getType().getName().contains("Fluid")) {
            return true;
        }
        Type generic = field.getGenericType();
        return generic instanceof ParameterizedType parameterized
                && parameterized.getActualTypeArguments().length == 1
                && parameterized.getActualTypeArguments()[0].getTypeName().contains("Fluid");
    }

    private static boolean makeAccessible(Field field) {
        try {
            field.setAccessible(true);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
