package com.example.akaishi.forge.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * 机械部件烘焙模型占位。
 * <p>
 * 实际渲染由 BEWLR 完成，本模型只返回空 quads 列表，
 * 确保模型系统能正常注册且不报错。
 * 所有委托方法返回合理默认值。
 */
public class MechanicalPartBakedModel implements BakedModel {

    private final String partType;

    public MechanicalPartBakedModel(String partType) {
        this.partType = partType;
    }

    public String getPartType() {
        return partType;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state,
                                     @Nullable Direction direction,
                                     RandomSource random) {
        return Collections.emptyList();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state,
                                     @Nullable Direction direction,
                                     RandomSource random,
                                     ModelData data,
                                     @Nullable net.minecraft.client.renderer.RenderType renderType) {
        return Collections.emptyList();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    @Override
    public boolean isCustomRenderer() {
        return true; // 标记为自定义渲染器，让系统知道不走标准管线
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return null;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}