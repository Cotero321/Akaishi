package com.example.akaishi.forge.client.model;

import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import java.util.function.Function;

/**
 * 机械部件未烘焙几何体。
 * 存储模型 JSON 中的 {@code part_type} 值，烘焙时创建 {@link MechanicalPartBakedModel}。
 * 实际渲染由 BEWLR 完成，本几何体只提供模型占位。
 */
public class MechanicalPartGeometry implements IUnbakedGeometry<MechanicalPartGeometry> {

    private final String partType;

    public MechanicalPartGeometry(String partType) {
        this.partType = partType;
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context,
                           ModelBaker baker,
                           Function<Material, TextureAtlasSprite> spriteGetter,
                           ModelState modelState,
                           ItemOverrides overrides,
                           ResourceLocation modelLocation) {
        return new MechanicalPartBakedModel(partType);
    }
}