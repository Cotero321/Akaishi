package com.example.akaishi.forge.client.mechanical;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.item.MechanicalOrganItem;
import com.example.akaishi.item.MechanicalPartItem;
import com.example.akaishi.life.mechanical.MechanicalMaterial;
import com.example.akaishi.life.mechanical.MechanicalOrganType;
import com.example.akaishi.life.mechanical.MechanicalPartType;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/**
 * 机械部件/器官的 BEWLR 渲染器（Forge 侧）。
 * <p>
 * 改进点：
 * 1. 细腻合成：HSL 混合算法，保留材料纹理细节，形状纹理提供 alpha 遮罩 + 亮度细节
 * 2. 类型差异化：不同部件/器官类型有不同的渲染参数（发光、厚度、旋转方式）
 * 3. 材料驱动：材料纹理是主色源，形状纹理决定轮廓和细节
 * 4. 动态渲染：3D 物品渲染，NBT 驱动纹理选择，多材料器官混合
 * 5. 32x32 形状纹理支持
 */
public class MechanicalPartRenderer extends BlockEntityWithoutLevelRenderer
        implements IClientItemExtensions {

    /** 单例 */
    public static final MechanicalPartRenderer INSTANCE = new MechanicalPartRenderer();

    private static final String SHAPE_PATH = "/assets/akaishi/textures/mechanical_part/shape/%s.png";
    private static final String MATERIAL_PATH = "/assets/%s/textures/mechanical_part/material/%s.png";

    /** 合成纹理缓存 */
    private static final Map<String, ResourceLocation> COMPOSITE_CACHE = new ConcurrentHashMap<>();

    /** 部件类型 → 渲染参数 */
    private static final Map<MechanicalPartType, PartRenderParams> PART_PARAMS = new EnumMap<>(MechanicalPartType.class);

    private static volatile boolean initialized = false;

    private MechanicalPartRenderer() {
        super(null, null);
    }

    /** 每种部件类型的渲染参数 */
    private record PartRenderParams(
            float thickness,
            float glowIntensity,
            float rotationSpeed,
            boolean hasOverlay,
            int[] overlayColor
    ) {}

    static {
        PART_PARAMS.put(MechanicalPartType.CORE, new PartRenderParams(0.6f, 0.8f, 0.5f, true, new int[]{255, 200, 80, 40}));
        PART_PARAMS.put(MechanicalPartType.MODULE, new PartRenderParams(0.4f, 0.3f, 1.0f, true, new int[]{100, 150, 255, 30}));
        PART_PARAMS.put(MechanicalPartType.SHELL, new PartRenderParams(0.7f, 0.0f, 0.0f, false, new int[]{0, 0, 0, 0}));
        PART_PARAMS.put(MechanicalPartType.COOLING, new PartRenderParams(0.3f, 0.4f, 2.0f, true, new int[]{150, 220, 255, 35}));
    }

    // ==================== 初始化 ====================

    public static void initialize() {
        if (initialized) return;
        initialized = true;

        // 预合成部件形状 × 材料
        for (MechanicalPartType partType : MechanicalPartType.values()) {
            String partName = partType.name().toLowerCase();
            BufferedImage shapeImage = loadTexture(String.format(SHAPE_PATH, partName));
            if (shapeImage == null) continue;

            for (MechanicalMaterial material : MechanicalMaterial.getAll()) {
                BufferedImage materialImage = loadMaterialTexture(material.id());
                if (materialImage == null) continue;

                String key = getCompositeKey(partName, material.id());
                BufferedImage composite = composite(shapeImage, materialImage, null);
                if (composite != null) registerComposite(key, composite);
            }
        }

        // 预合成器官形状 × 材料
        String[] organShapes = {"eye", "heart", "lung", "viscera", "kidney",
                "left_arm", "right_arm", "left_leg", "right_leg"};
        for (String organName : organShapes) {
            String shapeName = "organ_" + organName;
            BufferedImage shapeImage = loadTexture(String.format(SHAPE_PATH, shapeName));
            if (shapeImage == null) continue;

            for (MechanicalMaterial material : MechanicalMaterial.getAll()) {
                BufferedImage materialImage = loadMaterialTexture(material.id());
                if (materialImage == null) continue;

                String key = getCompositeKey(shapeName, material.id());
                BufferedImage composite = composite(shapeImage, materialImage, null);
                if (composite != null) registerComposite(key, composite);
            }
        }
    }

    private static String getCompositeKey(String shapeName, String materialId) {
        return shapeName + "_" + materialId.replace(':', '_');
    }

    // ==================== 纹理获取 ====================

    public static ResourceLocation getCompositeTexture(String shapeName, String materialId) {
        String key = getCompositeKey(shapeName, materialId);
        ResourceLocation existing = COMPOSITE_CACHE.get(key);
        if (existing != null) return existing;

        BufferedImage shapeImage = loadTexture(String.format(SHAPE_PATH, shapeName));
        BufferedImage materialImage = loadMaterialTexture(materialId);
        if (shapeImage == null || materialImage == null) {
            return new ResourceLocation(AkaishiMod.MOD_ID, "textures/mechanical_part/shape/" + shapeName + ".png");
        }
        BufferedImage composite = composite(shapeImage, materialImage, null);
        if (composite == null) {
            return new ResourceLocation(AkaishiMod.MOD_ID, "textures/mechanical_part/shape/" + shapeName + ".png");
        }
        registerComposite(key, composite);
        ResourceLocation result = COMPOSITE_CACHE.get(key);
        return result != null ? result :
                new ResourceLocation(AkaishiMod.MOD_ID, "textures/mechanical_part/shape/" + shapeName + ".png");
    }

    public static ResourceLocation getCompositeTextureMultiMaterial(String shapeName, List<String> materialIds) {
        if (materialIds.isEmpty()) return getCompositeTexture(shapeName, "akaishi:iron");
        if (materialIds.size() == 1) return getCompositeTexture(shapeName, materialIds.get(0));

        // 材质 id 含命名空间冒号，拼进 ResourceLocation 路径前必须清洗（与 getCompositeKey 同口径）
        String key = shapeName + "_multi_" + String.join("_",
                materialIds.stream().map(id -> id.replace(':', '_')).toList());
        ResourceLocation existing = COMPOSITE_CACHE.get(key);
        if (existing != null) return existing;

        BufferedImage shapeImage = loadTexture(String.format(SHAPE_PATH, shapeName));
        if (shapeImage == null) {
            return new ResourceLocation(AkaishiMod.MOD_ID, "textures/mechanical_part/shape/" + shapeName + ".png");
        }

        List<BufferedImage> materialImages = new ArrayList<>();
        for (String mid : materialIds) {
            BufferedImage img = loadMaterialTexture(mid);
            if (img != null) materialImages.add(img);
        }
        if (materialImages.isEmpty()) return getCompositeTexture(shapeName, "akaishi:iron");

        BufferedImage blendedMaterial = blendMaterials(materialImages);
        BufferedImage composite = composite(shapeImage, blendedMaterial, null);
        if (composite != null) registerComposite(key, composite);

        ResourceLocation result = COMPOSITE_CACHE.get(key);
        return result != null ? result : getCompositeTexture(shapeName, materialIds.get(0));
    }

    // ==================== BEWLR 渲染 ====================

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType,
                              PoseStack poseStack, MultiBufferSource buffer,
                              int combinedLight, int combinedOverlay) {
        if (!initialized) initialize();

        String shapeName;
        String materialId;
        boolean isOrgan = false;
        List<String> materialIds = List.of();
        MechanicalPartType partType = null;

        if (stack.getItem() instanceof MechanicalOrganItem) {
            MechanicalOrganType organType = MechanicalOrganItem.getOrganType(stack);
            shapeName = "organ_" + (organType != null ? organType.name().toLowerCase() : "default");
            materialIds = MechanicalOrganItem.getMaterialIds(stack);
            materialId = materialIds.isEmpty() ? "akaishi:iron" : materialIds.get(0);
            isOrgan = true;
        } else {
            partType = MechanicalPartItem.getPartType(stack);
            shapeName = partType != null ? partType.name().toLowerCase() : "core";
            String mid = MechanicalPartItem.getMaterialId(stack);
            materialId = mid != null ? mid : "akaishi:iron";
            materialIds = List.of(materialId);
        }

        // 变换
        setupTransform(poseStack, transformType, partType, isOrgan);

        // 纹理
        ResourceLocation texture;
        if (isOrgan && materialIds.size() > 1) {
            texture = getCompositeTextureMultiMaterial(shapeName, materialIds);
        } else {
            texture = getCompositeTexture(shapeName, materialId);
        }

        PartRenderParams params = (partType != null) ? PART_PARAMS.get(partType) : null;

        renderItem3D(poseStack, buffer, combinedLight, combinedOverlay, texture, params, isOrgan);
    }

    // ==================== 3D 变换 ====================

    private void setupTransform(PoseStack poseStack, ItemDisplayContext transformType,
                                 @Nullable MechanicalPartType partType, boolean isOrgan) {
        poseStack.pushPose();

        if (transformType == ItemDisplayContext.GUI) {
            poseStack.translate(0.5D, 0.5D, 0.5D);
        } else if (transformType == ItemDisplayContext.GROUND) {
            poseStack.translate(0.5D, 0.15D, 0.5D);
            poseStack.scale(0.6f, 0.6f, 0.6f);
        } else if (transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ||
                   transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            poseStack.translate(0.5D, 0.5D, 0.5D);
            poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(-30)));
            poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(45)));
            if (isOrgan) poseStack.scale(0.7f, 0.7f, 0.7f);
        } else {
            poseStack.translate(0.5D, 0.5D, 0.5D);
        }

        if (partType != null) {
            PartRenderParams p = PART_PARAMS.get(partType);
            if (p != null) {
                if (p.rotationSpeed > 0 && transformType == ItemDisplayContext.GUI) {
                    float angle = (System.currentTimeMillis() % 5000) / 5000.0f * 360.0f * p.rotationSpeed;
                    poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(angle)));
                }
                float thicknessScale = 0.8f + p.thickness * 0.4f;
                poseStack.scale(1.0f, 1.0f, thicknessScale);
            }
        }
    }

    // ==================== 3D 渲染 ====================

    private void renderItem3D(PoseStack poseStack, MultiBufferSource buffer,
                               int combinedLight, int combinedOverlay,
                               ResourceLocation texture,
                               @Nullable PartRenderParams params, boolean isOrgan) {
        Matrix4f matrix = poseStack.last().pose();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float size = 0.5f;
        float thickness = (params != null) ? params.thickness * 0.15f : 0.05f;

        // 正面
        renderQuad(matrix, -size, -size, size, size, 0, thickness, 255, 255, 255, 255);
        // 背面
        renderQuad(matrix, size, -size, -size, size, 0, -thickness, 200, 200, 200, 255);

        // 发光层
        if (params != null && params.glowIntensity > 0) {
            int glow = (int) (params.glowIntensity * 255);
            renderQuad(matrix, -size * 0.8f, -size * 0.8f, size * 0.8f, size * 0.8f,
                    0.001f, thickness, 255, 255, 255, glow);
        }

        // 叠加层
        if (params != null && params.hasOverlay) {
            int[] oc = params.overlayColor;
            float overlaySize = size * 0.85f;
            renderQuad(matrix, -overlaySize, -overlaySize, overlaySize, overlaySize,
                    0.002f, thickness, oc[0], oc[1], oc[2], oc[3]);
        }

        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private void renderQuad(Matrix4f matrix, float x1, float y1, float x2, float y2,
                             float zOffset, float thickness, int r, int g, int b, int a) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        float z1 = -thickness + zOffset;
        float z2 = thickness + zOffset;

        // 前面
        builder.vertex(matrix, x1, y1, z1).uv(0, 1).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y1, z1).uv(1, 1).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y2, z1).uv(1, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x1, y2, z1).uv(0, 0).color(r, g, b, a).endVertex();
        // 背面
        builder.vertex(matrix, x1, y2, z2).uv(0, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y2, z2).uv(1, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y1, z2).uv(1, 1).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x1, y1, z2).uv(0, 1).color(r, g, b, a).endVertex();
        // 顶边
        builder.vertex(matrix, x1, y1, z1).uv(0, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y1, z1).uv(1, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y1, z2).uv(1, 1).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x1, y1, z2).uv(0, 1).color(r, g, b, a).endVertex();
        // 底边
        builder.vertex(matrix, x1, y2, z1).uv(0, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y2, z1).uv(1, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y2, z2).uv(1, 1).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x1, y2, z2).uv(0, 1).color(r, g, b, a).endVertex();
        // 左边
        builder.vertex(matrix, x1, y1, z1).uv(0, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x1, y2, z1).uv(1, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x1, y2, z2).uv(1, 1).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x1, y1, z2).uv(0, 1).color(r, g, b, a).endVertex();
        // 右边
        builder.vertex(matrix, x2, y1, z1).uv(0, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y2, z1).uv(1, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y2, z2).uv(1, 1).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y1, z2).uv(0, 1).color(r, g, b, a).endVertex();

        BufferUploader.drawWithShader(builder.end());
    }

    // ==================== IClientItemExtensions ====================

    @Nullable
    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return this;
    }

    // ==================== 纹理合成 ====================

    private static BufferedImage composite(BufferedImage shape, BufferedImage material,
                                            @Nullable float[] tintColor) {
        int w = shape.getWidth();
        int h = shape.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int mw = material.getWidth();
        int mh = material.getHeight();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int shapePixel = shape.getRGB(x, y);
                int shapeAlpha = (shapePixel >> 24) & 0xFF;
                if (shapeAlpha == 0) { result.setRGB(x, y, 0); continue; }

                int mx = x % mw;
                int my = y % mh;
                int materialPixel = material.getRGB(mx, my);
                int mr = (materialPixel >> 16) & 0xFF;
                int mg = (materialPixel >> 8) & 0xFF;
                int mb = materialPixel & 0xFF;
                int sr = (shapePixel >> 16) & 0xFF;
                int sg = (shapePixel >> 8) & 0xFF;
                int sb = shapePixel & 0xFF;

                float shapeBrightness = (sr + sg + sb) / (3.0f * 255.0f);
                float brightnessFactor = 0.3f + 0.7f * shapeBrightness;

                int r = clamp((int) (mr * brightnessFactor));
                int g = clamp((int) (mg * brightnessFactor));
                int b = clamp((int) (mb * brightnessFactor));

                // 高光/阴影增强
                if (sr > 200 && sg > 200 && sb > 200) {
                    r = clamp((int) (r * 1.3f));
                    g = clamp((int) (g * 1.3f));
                    b = clamp((int) (b * 1.3f));
                } else if (sr < 50 && sg < 50 && sb < 50) {
                    r = clamp((int) (r * 0.6f));
                    g = clamp((int) (g * 0.6f));
                    b = clamp((int) (b * 0.6f));
                }

                if (tintColor != null) {
                    r = clamp((int) (r * (1 - tintColor[3]) + tintColor[0] * tintColor[3] * 255));
                    g = clamp((int) (g * (1 - tintColor[3]) + tintColor[1] * tintColor[3] * 255));
                    b = clamp((int) (b * (1 - tintColor[3]) + tintColor[2] * tintColor[3] * 255));
                }

                result.setRGB(x, y, (shapeAlpha << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }

    private static BufferedImage blendMaterials(List<BufferedImage> materials) {
        if (materials.isEmpty()) return null;
        if (materials.size() == 1) return materials.get(0);
        int w = materials.get(0).getWidth();
        int h = materials.get(0).getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int count = materials.size();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int totalR = 0, totalG = 0, totalB = 0, totalA = 0;
                for (BufferedImage mat : materials) {
                    int mx = x % mat.getWidth();
                    int my = y % mat.getHeight();
                    int pixel = mat.getRGB(mx, my);
                    totalR += (pixel >> 16) & 0xFF;
                    totalG += (pixel >> 8) & 0xFF;
                    totalB += pixel & 0xFF;
                    totalA += (pixel >> 24) & 0xFF;
                }
                result.setRGB(x, y, (clamp(totalA / count) << 24) |
                        (clamp(totalR / count) << 16) |
                        (clamp(totalG / count) << 8) |
                        clamp(totalB / count));
            }
        }
        return result;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    @Nullable
    private static BufferedImage loadMaterialTexture(String materialId) {
        ResourceLocation id = new ResourceLocation(materialId);
        return loadTexture(String.format(MATERIAL_PATH, id.getNamespace(), id.getPath()));
    }

    @Nullable
    private static BufferedImage loadTexture(String path) {
        try (InputStream is = MechanicalPartRenderer.class.getResourceAsStream(path)) {
            if (is == null) return null;
            return ImageIO.read(is);
        } catch (IOException e) {
            return null;
        }
    }

    private static void registerComposite(String key, BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] pixels = new int[w * h];
        image.getRGB(0, 0, w, h, pixels, 0, w);

        NativeImage nativeImage = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = pixels[y * w + x];
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                nativeImage.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }

        DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
        ResourceLocation location = new ResourceLocation(AkaishiMod.MOD_ID,
                "mechanical_part/composited/" + key);
        Minecraft.getInstance().getTextureManager().register(location, dynamicTexture);
        COMPOSITE_CACHE.put(key, location);
    }

    public static void clearCache() {
        COMPOSITE_CACHE.clear();
        initialized = false;
    }
}