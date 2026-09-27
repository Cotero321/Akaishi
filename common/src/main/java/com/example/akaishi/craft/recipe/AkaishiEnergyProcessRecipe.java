package com.example.akaishi.craft.recipe;

import com.example.akaishi.api.recipe.IProcessInputCount;
import com.example.akaishi.api.recipe.IProcessSlotCounts;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 赤石「能量加工」配方：物品（可省略）+ 赤能源 → 物品。
 *
 * <p>数据位于 {@code data/akaishi/recipes/<机器>/*.json}：
 * <pre>
 * {
 *   "type": "akaishi:aggregating",
 *   "ingredient": { "item": "minecraft:netherite_ingot" },  // 可省略 ⇒ 纯能量配方
 *   "input_count": 1,                                        // 可选，缺省 1
 *   "ingredient2": { "item": "akaishi:iron_dust" },          // 可选，第二原料（双原料机器如材料融合器）
 *   "input_count2": 4,                                       // 可选，缺省 1，仅 ingredient2 存在时有意义
 *   "energy": 10000000,                                      // 可选，缺省/0 = 机器配置默认（如 10M）
 *   "energy_config": "geode_upgrade",                        // 可选，选用机器哪一档配置默认（缺省=主档）
 *   "result": { "item": "akaishi:akaishi_ingot", "count": 1 }
 * }
 * </pre>
 *
 * <p><b>为什么能量可以省略</b>：能量成本天然属于配方数据（便于整合包逐条调），
 * 但保留"配方不写 ⇒ 用机器配置默认"的兜底，使现有配置项继续生效（与项目 {@code 0 = 内置默认} 口径一致）。
 *
 * <p><b>第二原料是加法式扩展</b>：{@code ingredient2/input_count2} 缺省不存在 ⇒ 既有
 * aggregating / life_purifying 单原料配方逐位不变；只有材料融合器这类双原料机台会用到。
 */
public class AkaishiEnergyProcessRecipe implements Recipe<Container>, IAkaishiMachineRecipe,
        IProcessInputCount, IProcessSlotCounts {

    /** 缺省单次消耗输入数量 */
    public static final int DEFAULT_INPUT_COUNT = 1;

    /**
     * 能耗配置档位 {@code energy_config} 的取值之一：晶洞升级。
     * <p>配方不写 {@code energy} 时用它选中「晶洞升级」那条配置，否则走机器主档（每锭）。
     * 判定集中在此常量与 {@code MachineProcessEnergy}，机器侧与规划器必须同源。
     */
    public static final String ENERGY_CONFIG_GEODE_UPGRADE = "geode_upgrade";

    private final ResourceLocation id;
    private final RegistrySupplier<RecipeType<AkaishiEnergyProcessRecipe>> type;
    private final RecipeSerializer<?> serializer;
    @Nullable
    private final Ingredient ingredient;
    @Nullable
    private final Ingredient ingredient2;
    private final int inputCount;
    private final int inputCount2;
    private final long energy;
    private final ItemStack result;
    /** 未声明 {@code energy} 时使用机器哪一档配置默认；空串 = 机器主档（见 {@link #costKey()}） */
    private final String costKey;

    /** 单原料构造（既有签名，零影响）：第二原料恒为不存在 */
    public AkaishiEnergyProcessRecipe(ResourceLocation id, @Nullable Ingredient ingredient, int inputCount,
            long energy, ItemStack result, String costKey,
            RegistrySupplier<RecipeType<AkaishiEnergyProcessRecipe>> type,
            RecipeSerializer<?> serializer) {
        this(id, ingredient, inputCount, null, DEFAULT_INPUT_COUNT, energy, result, costKey, type, serializer);
    }

    /** 完整构造（双原料机台用；{@code ingredient2 == null} 即退化为单原料语义） */
    public AkaishiEnergyProcessRecipe(ResourceLocation id, @Nullable Ingredient ingredient, int inputCount,
            @Nullable Ingredient ingredient2, int inputCount2, long energy, ItemStack result, String costKey,
            RegistrySupplier<RecipeType<AkaishiEnergyProcessRecipe>> type,
            RecipeSerializer<?> serializer) {
        this.id = id;
        this.ingredient = ingredient;
        this.ingredient2 = ingredient2;
        this.inputCount = Math.max(1, inputCount);
        this.inputCount2 = Math.max(1, inputCount2);
        this.energy = Math.max(0L, energy);
        this.result = result;
        this.costKey = costKey == null ? "" : costKey;
        this.type = type;
        this.serializer = serializer;
    }

    // ===== IAkaishiMachineRecipe =====

    @Nullable
    @Override
    public Ingredient ingredient() {
        return ingredient;
    }

    /** 第二原料；null = 单原料配方（既有配方一律为 null） */
    @Nullable
    public Ingredient ingredient2() {
        return ingredient2;
    }

    @Override
    public int inputCount() {
        return inputCount;
    }

    /** 第二原料的单次消耗数量（仅 {@link #ingredient2()} 非空时有意义） */
    public int inputCount2() {
        return inputCount2;
    }

    /** 逐格消耗数量（与 {@link #getIngredients()} 有效格同序；供虚拟加工 / 估值按格扣料） */
    @Override
    public int[] inputCounts() {
        return ingredient2 == null ? new int[]{inputCount} : new int[]{inputCount, inputCount2};
    }

    @Override
    public long energy() {
        return energy;
    }

    /**
     * 能耗配置档位：配方未声明 {@code energy} 时由机器按此键选配置默认值，
     * 使机器内多个配置项（如聚合器的「每锭」与「晶洞升级」）都能被数据包选中而不失效。
     */
    public String costKey() {
        return costKey;
    }

    @Override
    public boolean consumeInput() {
        return true;
    }

    @Override
    public ItemStack result() {
        return result;
    }

    // ===== Recipe =====

    /**
     * 容器匹配：第一格命中第一原料；存在第二原料时另需一格命中（<b>不区分槽序</b>，
     * 便于管道/端点在不保序的物流下也能投料）。
     */
    @Override
    public boolean matches(Container container, Level level) {
        if (!matchesSlot(container.getItem(0), ingredient, inputCount)) {
            return false;
        }
        if (ingredient2 == null) {
            return true;
        }
        return matchesSlot(container.getItem(1), ingredient2, inputCount2);
    }

    private static boolean matchesSlot(ItemStack stack, @Nullable Ingredient ingredient, int need) {
        return ingredient != null && !stack.isEmpty() && stack.getCount() >= need && ingredient.test(stack);
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return result.copy();
    }

    /** 有效原料格：第一原料 → 第二原料（第二原料缺省时只返回第一格） */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        if (ingredient != null) {
            list.add(ingredient);
        }
        if (ingredient2 != null) {
            list.add(ingredient2);
        }
        return list;
    }

    /** 机器配方不进配方书、不弹解锁提示 */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    @Override
    public RecipeType<?> getType() {
        return type.get();
    }

    /** 按类型参数化的通用序列化器（JSON / 网络） */
    public static final class Serializer implements RecipeSerializer<AkaishiEnergyProcessRecipe> {

        private final RegistrySupplier<RecipeType<AkaishiEnergyProcessRecipe>> type;

        public Serializer(RegistrySupplier<RecipeType<AkaishiEnergyProcessRecipe>> type) {
            this.type = type;
        }

        @Override
        public AkaishiEnergyProcessRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient = readIngredient(id, json, "ingredient");
            Ingredient ingredient2 = readIngredient(id, json, "ingredient2");
            int inputCount = json.has("input_count")
                    ? Math.max(1, json.get("input_count").getAsInt()) : DEFAULT_INPUT_COUNT;
            int inputCount2 = json.has("input_count2")
                    ? Math.max(1, json.get("input_count2").getAsInt()) : DEFAULT_INPUT_COUNT;
            long energy = json.has("energy") ? Math.max(0L, json.get("energy").getAsLong()) : 0L;
            String costKey = json.has("energy_config") ? json.get("energy_config").getAsString() : "";
            // 无 ingredient 的配方只对"纯能量机器"（如提纯机，成本由机器自身结算）有意义：
            // 聚合器按物品匹配，永远不会命中它，故不存在零成本造物；虚拟加工侧也会跳过无原料配方。
            if (!json.has("result")) {
                throw new JsonParseException("配方 " + id + " 缺少 result 字段");
            }
            JsonObject resultJson = json.getAsJsonObject("result");
            ResourceLocation itemId = new ResourceLocation(resultJson.get("item").getAsString());
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == Items.AIR) {
                throw new JsonParseException("配方 " + id + " 的产物物品不存在：" + itemId);
            }
            int count = resultJson.has("count") ? Math.max(1, resultJson.get("count").getAsInt()) : 1;
            return new AkaishiEnergyProcessRecipe(id, ingredient, inputCount, ingredient2, inputCount2, energy,
                    new ItemStack(item, count), costKey, type, this);
        }

        /** 读取一个可选原料格；不存在返回 null，存在但为空报错（与既有单原料口径一致） */
        @Nullable
        private static Ingredient readIngredient(ResourceLocation id, JsonObject json, String key) {
            if (!json.has(key)) {
                return null;
            }
            Ingredient ingredient = Ingredient.fromJson(json.get(key));
            if (ingredient.isEmpty()) {
                throw new JsonParseException("配方 " + id + " 的 " + key + " 为空");
            }
            return ingredient;
        }

        /** 读/写顺序必须逐字对齐，错位会在客户端解码时抛 DecoderException */
        @Override
        public AkaishiEnergyProcessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            boolean hasIngredient = buf.readBoolean();
            Ingredient ingredient = hasIngredient ? Ingredient.fromNetwork(buf) : null;
            int inputCount = buf.readVarInt();
            boolean hasIngredient2 = buf.readBoolean();
            Ingredient ingredient2 = hasIngredient2 ? Ingredient.fromNetwork(buf) : null;
            int inputCount2 = buf.readVarInt();
            long energy = buf.readVarLong();
            ItemStack result = buf.readItem();
            String costKey = buf.readUtf();
            return new AkaishiEnergyProcessRecipe(id, ingredient, inputCount, ingredient2, inputCount2, energy, result,
                    costKey, type, this);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, AkaishiEnergyProcessRecipe recipe) {
            buf.writeBoolean(recipe.ingredient != null);
            if (recipe.ingredient != null) {
                recipe.ingredient.toNetwork(buf);
            }
            buf.writeVarInt(recipe.inputCount);
            buf.writeBoolean(recipe.ingredient2 != null);
            if (recipe.ingredient2 != null) {
                recipe.ingredient2.toNetwork(buf);
            }
            buf.writeVarInt(recipe.inputCount2);
            buf.writeVarLong(recipe.energy);
            buf.writeItem(recipe.result);
            buf.writeUtf(recipe.costKey);
        }
    }
}
