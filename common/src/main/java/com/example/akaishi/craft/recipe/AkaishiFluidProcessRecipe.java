package com.example.akaishi.craft.recipe;

import com.example.akaishi.api.recipe.IFluidProcessRecipe;
import com.example.akaishi.api.recipe.IProcessInputCount;
import com.google.gson.JsonArray;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 赤石「流体加工」配方：<b>物品 / 流体 → 流体（可另产物品）</b>。
 *
 * <p>覆盖燃料链与活化链上全部"有流体参与"的机器（液化机 / 加工机 / 混合器 /
 * 聚变燃料聚合器 / 生命离心机 / 生命活化器）：它们的形状差异只是"哪几个字段填了值"，
 * 故用同一个配方类 + 同一个序列化器，新增这类机器<b>不必再改配方类</b>。
 *
 * <p>数据位于 {@code data/akaishi/recipes/<机器>/*.json}：
 * <pre>
 * {
 *   "type": "akaishi:liquefying",
 *   "ingredient": { "item": "minecraft:nether_star" },              // 可选：物品原料
 *   "input_count": 1,                                                // 可选，缺省 1（同时作为 catalyst 的消耗量）
 *   "catalyst": { "item": "akaishi:akaishi_life_essence_solid" },    // 可选：辅料槽（如液化机的生命固态物）
 *   "fluid_inputs": [ { "fluid": "akaishi:pure_fuel", "amount": 1000 } ],  // 可选：0~2 路流体输入
 *   "energy": 50000000,                                             // 可选：每批赤能源（0/缺省 = 机器配置默认）
 *   "energy_config": "xxx",                                         // 可选：选用机器哪一档配置默认
 *   "fluid_output": { "fluid": "akaishi:nether_pure_energy", "amount": 1000 }, // 可选：流体产物（amount 可省 = 由机器配置决定）
 *   "result": { "item": "...", "count": 1 },                         // 可选：物品产物
 *   "byproduct": { "item": "...", "count": 1 }                       // 可选：物品副产（随每批必产）
 * }
 * </pre>
 * <b>流体产物 / 物品产物至少有一个</b>，否则解析即报错（无产出的配方没有意义）。
 *
 * <p><b>为什么流体存 id 字符串</b>：配方在数据包加载期解析、还要经网络下发，
 * 直接持有 {@code Fluid} 实例会让"未注册流体"在两端表现不一致；存注册名后取值时再解析，
 * 两个方向都稳定（与机器侧表格的做法一致）。
 *
 * <p><b>能耗与耗时</b>：赤能源可由配方声明（{@code energy}，如液化机各档成本不同），
 * 声明值由 {@code MachineProcessEnergy} 按族读取；耗时仍由机器配置决定。
 */
public class AkaishiFluidProcessRecipe implements Recipe<Container>, IAkaishiMachineRecipe, IProcessInputCount,
        IFluidProcessRecipe {

    /** 缺省单次消耗数量 */
    public static final int DEFAULT_INPUT_COUNT = 1;
    /** 允许的流体输入路数上限（混合器 2 路，留 2 路足够） */
    public static final int MAX_FLUID_INPUTS = 2;

    /**
     * 一条流体规格：流体 + 数量（mB）。
     * <p><b>{@code amount == 0} 表示"数量由机器配置决定"</b>（如聚变燃料聚合器：7 条配方共用
     * {@code ModConfig.aggregatorProducePerCraft} 一个产量配置，配方只声明产出<b>哪种</b>流体）。
     * 若配方写了正数，则以配方为准。
     */
    public record FluidSpec(Fluid fluid, long amount) {
    }

    private final ResourceLocation id;
    private final RegistrySupplier<RecipeType<AkaishiFluidProcessRecipe>> type;
    private final RecipeSerializer<?> serializer;
    @Nullable
    private final Ingredient ingredient;
    private final int inputCount;
    /** 辅料槽（如液化机的生命固态物）；无则 null。消耗量与 {@link #inputCount} 相同 */
    @Nullable
    private final Ingredient catalyst;
    private final List<FluidSpec> fluidInputs;
    @Nullable
    private final FluidSpec fluidOutput;
    private final ItemStack result;
    private final ItemStack byproduct;
    private final long energy;
    /** 未声明 {@code energy} 时使用机器哪一档配置默认；空串 = 主机档（同 {@code AkaishiEnergyProcessRecipe}） */
    private final String costKey;

    public AkaishiFluidProcessRecipe(ResourceLocation id, @Nullable Ingredient ingredient, int inputCount,
            @Nullable Ingredient catalyst, List<FluidSpec> fluidInputs, @Nullable FluidSpec fluidOutput,
            ItemStack result, ItemStack byproduct, long energy, String costKey,
            RegistrySupplier<RecipeType<AkaishiFluidProcessRecipe>> type, RecipeSerializer<?> serializer) {
        this.id = id;
        this.ingredient = ingredient;
        this.inputCount = Math.max(1, inputCount);
        this.catalyst = catalyst;
        this.fluidInputs = List.copyOf(fluidInputs);
        this.fluidOutput = fluidOutput;
        this.result = result;
        this.byproduct = byproduct;
        this.energy = Math.max(0L, energy);
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

    /** 能耗配置档位（配方未声明 {@code energy} 时机器据此选配置默认值） */
    public String costKey() {
        return costKey;
    }

    @Override
    public boolean consumeInput() {
        return true;
    }

    /** 物品产物；纯流体配方返回空栈（此时 {@code RecipeIngredients} 不会把它当成"可合成物品"） */
    @Override
    public ItemStack result() {
        return result;
    }

    // ===== 流体 / 辅料的专属视图 =====

    /** 辅料槽的物品要求；无则 null */
    @Nullable
    public Ingredient catalyst() {
        return catalyst;
    }

    /** 流体输入（0~2 路，顺序即声明顺序） */
    public List<FluidSpec> fluidInputs() {
        return fluidInputs;
    }

    /** 本配方必须消耗流体输入 ⇒ 不参与虚拟加工（虚拟加工只处理物品物料，见 {@link IFluidProcessRecipe}） */
    @Override
    public boolean requiresFluidInput() {
        return !fluidInputs.isEmpty();
    }

    /** 流体产物；无则 null */
    @Nullable
    public FluidSpec fluidOutput() {
        return fluidOutput;
    }

    /** 物品副产；无则空栈 */
    public ItemStack byproduct() {
        return byproduct;
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

    /**
     * 物品原料格：主原料 + 辅料各占一格（辅料也是真消耗，必须让规划器看见，
     * 否则"液化末地燃料"会被算成只吃混合物）。
     */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        if (ingredient != null) {
            list.add(ingredient);
        }
        if (catalyst != null) {
            list.add(catalyst);
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
    public static final class Serializer implements RecipeSerializer<AkaishiFluidProcessRecipe> {

        private final RegistrySupplier<RecipeType<AkaishiFluidProcessRecipe>> type;

        public Serializer(RegistrySupplier<RecipeType<AkaishiFluidProcessRecipe>> type) {
            this.type = type;
        }

        @Override
        public AkaishiFluidProcessRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient = null;
            if (json.has("ingredient")) {
                ingredient = Ingredient.fromJson(json.get("ingredient"));
                if (ingredient.isEmpty()) {
                    throw new JsonParseException("配方 " + id + " 的 ingredient 为空");
                }
            }
            Ingredient catalyst = json.has("catalyst") ? Ingredient.fromJson(json.get("catalyst")) : null;
            int inputCount = json.has("input_count")
                    ? Math.max(1, json.get("input_count").getAsInt()) : DEFAULT_INPUT_COUNT;
            List<FluidSpec> fluidInputs = new ArrayList<>();
            if (json.has("fluid_inputs")) {
                JsonArray array = json.getAsJsonArray("fluid_inputs");
                if (array.size() > MAX_FLUID_INPUTS) {
                    throw new JsonParseException("配方 " + id + " 的 fluid_inputs 超过 "
                            + MAX_FLUID_INPUTS + " 路");
                }
                for (int i = 0; i < array.size(); i++) {
                    fluidInputs.add(parseFluid(array.get(i).getAsJsonObject(), id, "fluid_inputs"));
                }
            }
            FluidSpec fluidOutput = json.has("fluid_output")
                    ? parseFluid(json.getAsJsonObject("fluid_output"), id, "fluid_output") : null;
            ItemStack result = json.has("result") ? parseStack(json, "result", id) : ItemStack.EMPTY;
            ItemStack byproduct = json.has("byproduct") ? parseStack(json, "byproduct", id) : ItemStack.EMPTY;
            if (result.isEmpty() && byproduct.isEmpty() && fluidOutput == null) {
                throw new JsonParseException("配方 " + id + " 没有任何产物（result / byproduct / fluid_output 全缺）");
            }
            long energy = json.has("energy") ? Math.max(0L, json.get("energy").getAsLong()) : 0L;
            String costKey = json.has("energy_config") ? json.get("energy_config").getAsString() : "";
            return new AkaishiFluidProcessRecipe(id, ingredient, inputCount, catalyst, fluidInputs, fluidOutput,
                    result, byproduct, energy, costKey, type, this);
        }

        private static FluidSpec parseFluid(JsonObject json, ResourceLocation id, String field) {
            ResourceLocation fluidId = new ResourceLocation(json.get("fluid").getAsString());
            Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
            if (fluid == null || fluid == Fluids.EMPTY) {
                throw new JsonParseException("配方 " + id + " 的 " + field + " 流体不存在：" + fluidId);
            }
            // 不写 amount = 0 = "数量由机器配置决定"（见 FluidSpec 说明）
            long amount = json.has("amount") ? Math.max(0L, json.get("amount").getAsLong()) : 0L;
            return new FluidSpec(fluid, amount);
        }

        private static ItemStack parseStack(JsonObject json, String field, ResourceLocation id) {
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
        public AkaishiFluidProcessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            boolean hasIngredient = buf.readBoolean();
            Ingredient ingredient = hasIngredient ? Ingredient.fromNetwork(buf) : null;
            boolean hasCatalyst = buf.readBoolean();
            Ingredient catalyst = hasCatalyst ? Ingredient.fromNetwork(buf) : null;
            int inputCount = buf.readVarInt();
            int fluidInCount = buf.readVarInt();
            List<FluidSpec> fluidInputs = new ArrayList<>(fluidInCount);
            for (int i = 0; i < fluidInCount; i++) {
                fluidInputs.add(readFluid(buf));
            }
            boolean hasFluidOut = buf.readBoolean();
            FluidSpec fluidOutput = hasFluidOut ? readFluid(buf) : null;
            ItemStack result = buf.readItem();
            ItemStack byproduct = buf.readItem();
            long energy = buf.readVarLong();
            String costKey = buf.readUtf();
            return new AkaishiFluidProcessRecipe(id, ingredient, inputCount, catalyst, fluidInputs, fluidOutput,
                    result, byproduct, energy, costKey, type, this);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, AkaishiFluidProcessRecipe recipe) {
            buf.writeBoolean(recipe.ingredient != null);
            if (recipe.ingredient != null) {
                recipe.ingredient.toNetwork(buf);
            }
            buf.writeBoolean(recipe.catalyst != null);
            if (recipe.catalyst != null) {
                recipe.catalyst.toNetwork(buf);
            }
            buf.writeVarInt(recipe.inputCount);
            buf.writeVarInt(recipe.fluidInputs.size());
            for (FluidSpec spec : recipe.fluidInputs) {
                writeFluid(buf, spec);
            }
            buf.writeBoolean(recipe.fluidOutput != null);
            if (recipe.fluidOutput != null) {
                writeFluid(buf, recipe.fluidOutput);
            }
            buf.writeItem(recipe.result);
            buf.writeItem(recipe.byproduct);
            buf.writeVarLong(recipe.energy);
            buf.writeUtf(recipe.costKey);
        }

        /** 流体按注册名传输（不占用两端可能不一致的数值 id） */
        private static void writeFluid(FriendlyByteBuf buf, FluidSpec spec) {
            buf.writeResourceLocation(BuiltInRegistries.FLUID.getKey(spec.fluid()));
            buf.writeVarLong(spec.amount());
        }

        private static FluidSpec readFluid(FriendlyByteBuf buf) {
            ResourceLocation fluidId = buf.readResourceLocation();
            long amount = buf.readVarLong();
            return new FluidSpec(BuiltInRegistries.FLUID.get(fluidId), amount);
        }
    }
}
