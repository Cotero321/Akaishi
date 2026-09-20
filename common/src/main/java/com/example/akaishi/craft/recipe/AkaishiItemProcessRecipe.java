package com.example.akaishi.craft.recipe;

import com.example.akaishi.api.recipe.IProcessInputCount;
import com.google.gson.JsonElement;
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

/**
 * 赤石「单输入 → 单输出」机器加工配方（打粉机 / 压缩机 / 变化器 / 植物培养机共用一类）。
 *
 * <p>数据位于 {@code data/akaishi/recipes/<机器>/*.json}：
 * <pre>
 * {
 *   "type": "akaishi:pulverizing",
 *   "ingredient": { "item": "minecraft:iron_ore" },   // 亦支持 {"tag": "..."} 或 [ ... ] 多候选
 *   "input_count": 1,                                  // 可选，缺省 1
 *   "consume_input": false,                            // 可选，缺省 true；false = 输入保留（培养机）
 *   "result": { "item": "akaishi:iron_dust", "count": 2 },
 *   "byproduct": { "item": "akaishi:exhausted_crystal", "count": 1 }   // 可选，缺省无副产（活化分馏器）
 * }
 * </pre>
 *
 * <p><b>能耗与耗时不在配方里</b>：仍由机器自身配置项（每 tick 能耗 × 耗时）决定，
 * 配方只负责描述"物料变换"，这样数值平衡依旧集中在配置系统。
 */
public class AkaishiItemProcessRecipe implements Recipe<Container>, IAkaishiMachineRecipe, IProcessInputCount {

    /** 缺省单次消耗输入数量 */
    public static final int DEFAULT_INPUT_COUNT = 1;

    private final ResourceLocation id;
    private final RegistrySupplier<RecipeType<AkaishiItemProcessRecipe>> type;
    private final RecipeSerializer<?> serializer;
    private final Ingredient ingredient;
    private final int inputCount;
    private final ItemStack result;
    /** 每批额外产出的副产物；无副产时为空栈（{@code ItemStack.EMPTY}） */
    private final ItemStack byproduct;
    private final boolean consumeInput;

    public AkaishiItemProcessRecipe(ResourceLocation id, Ingredient ingredient, int inputCount, ItemStack result,
            ItemStack byproduct, boolean consumeInput, RegistrySupplier<RecipeType<AkaishiItemProcessRecipe>> type,
            RecipeSerializer<?> serializer) {
        this.id = id;
        this.ingredient = ingredient;
        this.inputCount = Math.max(1, inputCount);
        this.result = result;
        this.byproduct = byproduct;
        this.consumeInput = consumeInput;
        this.type = type;
        this.serializer = serializer;
    }

    /** 单格原料（含标签候选） */
    @Override
    public Ingredient ingredient() {
        return ingredient;
    }

    /** 单次加工消耗的输入数量 */
    @Override
    public int inputCount() {
        return inputCount;
    }

    /** 单次加工产出的物品栈（只读语义，取用时请 {@code copy()}） */
    @Override
    public ItemStack result() {
        return result;
    }

    /**
     * 每批额外产出的副产物（无副产时为空栈）。
     * <p>副产<b>不占原料格、也不参与匹配</b>，只在一次加工成功时随主产物一起入库 ——
     * 虚拟加工侧会把它计入净产出（见 {@code VirtualCraftPlanner}），保证"报了就有"。
     */
    public ItemStack byproduct() {
        return byproduct;
    }

    /** 输入是否被消耗；false = 保留（如植物培养机的种子） */
    @Override
    public boolean consumeInput() {
        return consumeInput;
    }

    /** 本族配方不声明赤能源：能耗由机器配置（每 tick × 耗时）决定 */
    @Override
    public long energy() {
        return 0L;
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
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    /** 机器配方不进配方书、不弹解锁提示（由机器界面与 JEI 展示） */
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
    public static final class Serializer implements RecipeSerializer<AkaishiItemProcessRecipe> {

        private final RegistrySupplier<RecipeType<AkaishiItemProcessRecipe>> type;

        public Serializer(RegistrySupplier<RecipeType<AkaishiItemProcessRecipe>> type) {
            this.type = type;
        }

        @Override
        public AkaishiItemProcessRecipe fromJson(ResourceLocation id, JsonObject json) {
            if (!json.has("ingredient")) {
                throw new JsonParseException("配方 " + id + " 缺少 ingredient 字段");
            }
            Ingredient ingredient = Ingredient.fromJson(json.get("ingredient"));
            if (ingredient.isEmpty()) {
                throw new JsonParseException("配方 " + id + " 的 ingredient 为空");
            }
            int inputCount = json.has("input_count")
                    ? Math.max(1, json.get("input_count").getAsInt()) : DEFAULT_INPUT_COUNT;
            boolean consumeInput = !json.has("consume_input") || json.get("consume_input").getAsBoolean();
            ItemStack result = parseStack(json, "result", id, true);
            // 副产可省略（现有单槽族配方全都不写）
            ItemStack byproduct = json.has("byproduct")
                    ? parseStack(json, "byproduct", id, false) : ItemStack.EMPTY;
            return new AkaishiItemProcessRecipe(id, ingredient, inputCount, result, byproduct,
                    consumeInput, type, this);
        }

        /**
         * 解析 {@code {"item": ..., "count": ...}} 形状的字段。
         *
         * @param required true = 字段缺失或物品不存在时抛异常（主产物）；false = 缺失返回空栈（副产）
         */
        private static ItemStack parseStack(JsonObject json, String field, ResourceLocation id, boolean required) {
            if (!json.has(field)) {
                if (required) {
                    throw new JsonParseException("配方 " + id + " 缺少 " + field + " 字段");
                }
                return ItemStack.EMPTY;
            }
            JsonObject stackJson = json.getAsJsonObject(field);
            ResourceLocation itemId = new ResourceLocation(stackJson.get("item").getAsString());
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == Items.AIR) {
                throw new JsonParseException("配方 " + id + " 的 " + field + " 物品不存在：" + itemId);
            }
            int count = stackJson.has("count") ? Math.max(1, stackJson.get("count").getAsInt()) : 1;
            return new ItemStack(item, count);
        }

        /** 读/写顺序必须逐字对齐，错位会在客户端解码时抛 DecoderException */
        @Override
        public AkaishiItemProcessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient ingredient = Ingredient.fromNetwork(buf);
            int inputCount = buf.readVarInt();
            ItemStack result = buf.readItem();
            ItemStack byproduct = buf.readItem();
            boolean consumeInput = buf.readBoolean();
            return new AkaishiItemProcessRecipe(id, ingredient, inputCount, result, byproduct, consumeInput, type, this);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, AkaishiItemProcessRecipe recipe) {
            recipe.ingredient.toNetwork(buf);
            buf.writeVarInt(recipe.inputCount);
            buf.writeItem(recipe.result);
            buf.writeItem(recipe.byproduct);
            buf.writeBoolean(recipe.consumeInput);
        }
    }
}
