package com.example.akaishi.craft.recipe;

import com.example.akaishi.api.recipe.IProcessInputCount;
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
 *   "energy": 10000000,                                      // 可选，缺省/0 = 机器配置默认（如 10M）
 *   "energy_config": "geode_upgrade",                        // 可选，选用机器哪一档配置默认（缺省=主档）
 *   "result": { "item": "akaishi:akaishi_ingot", "count": 1 }
 * }
 * </pre>
 *
 * <p><b>为什么能量可以省略</b>：能量成本天然属于配方数据（便于整合包逐条调），
 * 但保留"配方不写 ⇒ 用机器配置默认"的兜底，使现有配置项继续生效（与项目 {@code 0 = 内置默认} 口径一致）。
 */
public class AkaishiEnergyProcessRecipe implements Recipe<Container>, IAkaishiMachineRecipe, IProcessInputCount {

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
    private final int inputCount;
    private final long energy;
    private final ItemStack result;
    /** 未声明 {@code energy} 时使用机器哪一档配置默认；空串 = 机器主档（见 {@link #costKey()}） */
    private final String costKey;

    public AkaishiEnergyProcessRecipe(ResourceLocation id, @Nullable Ingredient ingredient, int inputCount,
            long energy, ItemStack result, String costKey,
            RegistrySupplier<RecipeType<AkaishiEnergyProcessRecipe>> type,
            RecipeSerializer<?> serializer) {
        this.id = id;
        this.ingredient = ingredient;
        this.inputCount = Math.max(1, inputCount);
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

    @Override
    public int inputCount() {
        return inputCount;
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

    @Override
    public boolean matches(Container container, Level level) {
        return matchesInput(container.getItem(0));
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

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredient == null ? NonNullList.create() : NonNullList.of(Ingredient.EMPTY, ingredient);
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
            Ingredient ingredient = null;
            if (json.has("ingredient")) {
                ingredient = Ingredient.fromJson(json.get("ingredient"));
                if (ingredient.isEmpty()) {
                    throw new JsonParseException("配方 " + id + " 的 ingredient 为空");
                }
            }
            int inputCount = json.has("input_count")
                    ? Math.max(1, json.get("input_count").getAsInt()) : DEFAULT_INPUT_COUNT;
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
            return new AkaishiEnergyProcessRecipe(id, ingredient, inputCount, energy,
                    new ItemStack(item, count), costKey, type, this);
        }

        /** 读/写顺序必须逐字对齐，错位会在客户端解码时抛 DecoderException */
        @Override
        public AkaishiEnergyProcessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            boolean hasIngredient = buf.readBoolean();
            Ingredient ingredient = hasIngredient ? Ingredient.fromNetwork(buf) : null;
            int inputCount = buf.readVarInt();
            long energy = buf.readVarLong();
            ItemStack result = buf.readItem();
            String costKey = buf.readUtf();
            return new AkaishiEnergyProcessRecipe(id, ingredient, inputCount, energy, result, costKey, type, this);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, AkaishiEnergyProcessRecipe recipe) {
            buf.writeBoolean(recipe.ingredient != null);
            if (recipe.ingredient != null) {
                recipe.ingredient.toNetwork(buf);
            }
            buf.writeVarInt(recipe.inputCount);
            buf.writeVarLong(recipe.energy);
            buf.writeItem(recipe.result);
            buf.writeUtf(recipe.costKey);
        }
    }
}
