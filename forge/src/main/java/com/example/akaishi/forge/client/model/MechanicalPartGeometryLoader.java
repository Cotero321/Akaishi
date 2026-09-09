package com.example.akaishi.forge.client.model;

import com.example.akaishi.AkaishiMod;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryLoader;

/**
 * 机械部件模型的 IGeometryLoader（方案4 入口）。
 * <p>
 * 模型 JSON 引用 {@code "loader": "akaishi:mechanical_part"} 时触发此加载器。
 * 实际渲染由 {@link com.example.akaishi.client.mechanical.MechanicalPartRenderer} 的 BEWLR 完成，
 * 本加载器只负责解析 JSON 并创建几何体占位，使模型系统能正常注册。
 *
 * <p>模型 JSON 示例：
 * <pre>{@code
 * {
 *   "loader": "akaishi:mechanical_part",
 *   "part_type": "template"
 * }
 * }</pre>
 */
public class MechanicalPartGeometryLoader implements IGeometryLoader<MechanicalPartGeometry> {

    public static final ResourceLocation ID = new ResourceLocation(AkaishiMod.MOD_ID, "mechanical_part");

    @Override
    public MechanicalPartGeometry read(JsonObject json, JsonDeserializationContext context)
            throws JsonParseException {
        String partType = "template"; // 默认值
        if (json.has("part_type")) {
            partType = json.get("part_type").getAsString();
        }
        return new MechanicalPartGeometry(partType);
    }
}