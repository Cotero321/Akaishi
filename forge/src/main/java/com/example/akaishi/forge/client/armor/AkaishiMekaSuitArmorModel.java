package com.example.akaishi.forge.client.armor;

import com.example.akaishi.AkaishiMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * 赤石动力装甲的原生人形模型。
 * 各模块直接挂在原版人形骨骼上，使行走、潜行、骑乘与手臂动作沿用原版动画。
 */
public final class AkaishiMekaSuitArmorModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(AkaishiMod.MOD_ID, "mekasuit_armor"), "main");

    private final ModelPart headShell;
    private final ModelPart torsoShell;
    private final ModelPart waistCarrier;
    private final ModelPart bioCore;
    private final ModelPart rightArmShell;
    private final ModelPart leftArmShell;
    private final ModelPart rightLegShell;
    private final ModelPart leftLegShell;
    private final ModelPart rightBootShell;
    private final ModelPart leftBootShell;

    public AkaishiMekaSuitArmorModel(ModelPart root) {
        super(root);
        headShell = head.getChild("shell");
        torsoShell = body.getChild("torso_shell");
        waistCarrier = body.getChild("waist_carrier");
        bioCore = body.getChild("bio_core");
        rightArmShell = rightArm.getChild("shell");
        leftArmShell = leftArm.getChild("shell");
        rightLegShell = rightLeg.getChild("upper_shell");
        leftLegShell = leftLeg.getChild("upper_shell");
        rightBootShell = rightLeg.getChild("boot_shell");
        leftBootShell = leftLeg.getChild("boot_shell");
    }

    public void prepareForSlot(HumanoidModel<?> original, EquipmentSlot slot) {
        attackTime = original.attackTime;
        riding = original.riding;
        young = original.young;
        crouching = original.crouching;
        rightArmPose = original.rightArmPose;
        leftArmPose = original.leftArmPose;
        head.copyFrom(original.head);
        hat.copyFrom(original.hat);
        body.copyFrom(original.body);
        rightArm.copyFrom(original.rightArm);
        leftArm.copyFrom(original.leftArm);
        rightLeg.copyFrom(original.rightLeg);
        leftLeg.copyFrom(original.leftLeg);

        setAllVisible(false);
        switch (slot) {
            case HEAD -> {
                head.visible = true;
                headShell.visible = true;
            }
            case CHEST -> {
                body.visible = true;
                rightArm.visible = true;
                leftArm.visible = true;
                torsoShell.visible = true;
                bioCore.visible = true;
                rightArmShell.visible = true;
                leftArmShell.visible = true;
            }
            case LEGS -> {
                body.visible = true;
                rightLeg.visible = true;
                leftLeg.visible = true;
                waistCarrier.visible = true;
                rightLegShell.visible = true;
                leftLegShell.visible = true;
            }
            case FEET -> {
                rightLeg.visible = true;
                leftLeg.visible = true;
                rightBootShell.visible = true;
                leftBootShell.visible = true;
            }
            default -> {
            }
        }
    }

    @Override
    public void setAllVisible(boolean visible) {
        head.visible = visible;
        hat.visible = false;
        body.visible = visible;
        rightArm.visible = visible;
        leftArm.visible = visible;
        rightLeg.visible = visible;
        leftLeg.visible = visible;
        headShell.visible = visible;
        torsoShell.visible = visible;
        waistCarrier.visible = visible;
        bioCore.visible = visible;
        leftArmShell.visible = visible;
        rightLegShell.visible = visible;
        leftLegShell.visible = visible;
        rightBootShell.visible = visible;
        leftBootShell.visible = visible;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation plate = new CubeDeformation(0.45F);
        CubeDeformation frame = new CubeDeformation(0.7F);

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("shell", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.5F, -8.5F, -4.5F, 9, 9, 9, plate)
                        .texOffs(36, 0).addBox(-3.0F, -5.8F, -5.15F, 6, 2, 1, frame)
                        .texOffs(50, 0).addBox(-4.9F, -6.0F, -2.4F, 1, 4, 5, plate)
                        .texOffs(50, 9).addBox(3.9F, -6.0F, -2.4F, 1, 4, 5, plate)
                        .texOffs(0, 18).addBox(-2.5F, -9.1F, -1.8F, 5, 1, 4, frame),
                PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        body.addOrReplaceChild("torso_shell", CubeListBuilder.create()
                        .texOffs(18, 18).addBox(-4.7F, -0.4F, -2.8F, 9.4F, 11.5F, 1.8F, plate)
                        .texOffs(0, 31).addBox(-3.2F, 1.0F, -4.0F, 6.4F, 7.2F, 1.4F, frame)
                        .texOffs(0, 42).addBox(-1.6F, 2.3F, -4.65F, 3.2F, 3.6F, 1.0F, frame)
                        .texOffs(18, 31).addBox(-5.7F, 0.2F, -2.5F, 1.6F, 4.5F, 4.6F, frame)
                        .texOffs(30, 31).addBox(4.1F, 0.2F, -2.5F, 1.6F, 4.5F, 4.6F, frame)
                        .texOffs(40, 18).addBox(-3.6F, 1.0F, 2.2F, 7.2F, 8.8F, 1.2F, plate),
                PartPose.ZERO);
        body.addOrReplaceChild("waist_carrier", CubeListBuilder.create()
                        .texOffs(40, 31).addBox(-4.8F, 9.2F, -2.8F, 9.6F, 2.5F, 5.6F, frame)
                        .texOffs(0, 46).addBox(-1.7F, 9.4F, -3.65F, 3.4F, 2.1F, 1.0F, frame),
                PartPose.ZERO);

        body.addOrReplaceChild("bio_core", CubeListBuilder.create()
                        .texOffs(32, 42).addBox(-1.4F, 3.2F, -4.9F, 2.8F, 2.8F, 0.6F, new CubeDeformation(0.05F))
                        .texOffs(40, 42).addBox(-0.6F, 6.4F, -4.45F, 1.2F, 2.0F, 0.5F, new CubeDeformation(0.02F)),
                PartPose.ZERO);

        addArm(root, "right_arm", -5.0F, true, plate, frame);
        addArm(root, "left_arm", 5.0F, false, plate, frame);
        addLeg(root, "right_leg", -1.9F, true, plate, frame);
        addLeg(root, "left_leg", 1.9F, false, plate, frame);
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void addArm(PartDefinition root, String name, float x, boolean right,
                               CubeDeformation plate, CubeDeformation frame) {
        PartDefinition arm = root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(x, 2.0F, 0));
        float shoulderX = right ? -3.2F : 1.2F;
        float sideX = right ? -3.1F : 2.1F;
        arm.addOrReplaceChild("shell", CubeListBuilder.create()
                        .texOffs(60, 0).addBox(-3.0F, -2.7F, -2.7F, 6, 3.1F, 5.4F, frame)
                        .texOffs(60, 9).addBox(-2.7F, 0.0F, -2.7F, 5.4F, 7.5F, 5.4F, plate)
                        .texOffs(82, 0).addBox(sideX, 1.2F, -2.1F, 1.0F, 4.0F, 4.2F, frame)
                        .texOffs(82, 9).addBox(-2.9F, 7.1F, -2.8F, 5.8F, 2.0F, 5.6F, frame)
                        .texOffs(94, 0).addBox(shoulderX, -2.1F, -2.2F, 2.0F, 1.0F, 4.4F, plate),
                PartPose.ZERO);
    }

    private static void addLeg(PartDefinition root, String name, float x, boolean right,
                               CubeDeformation plate, CubeDeformation frame) {
        PartDefinition leg = root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(x, 12.0F, 0));
        float sideX = right ? -2.9F : 1.9F;
        leg.addOrReplaceChild("upper_shell", CubeListBuilder.create()
                        .texOffs(60, 22).addBox(-2.65F, -0.3F, -2.65F, 5.3F, 8.4F, 5.3F, plate)
                        .texOffs(82, 18).addBox(-2.25F, 1.0F, -3.45F, 4.5F, 4.8F, 1.1F, frame)
                        .texOffs(94, 10).addBox(sideX, 1.4F, -1.8F, 1.0F, 4.8F, 3.6F, frame),
                PartPose.ZERO);
        leg.addOrReplaceChild("boot_shell", CubeListBuilder.create()
                        .texOffs(60, 37).addBox(-2.8F, 7.2F, -2.9F, 5.6F, 5.8F, 5.8F, frame)
                        .texOffs(82, 28).addBox(-2.4F, 9.0F, -3.8F, 4.8F, 3.2F, 1.2F, plate)
                        .texOffs(94, 20).addBox(-3.0F, 12.0F, -3.3F, 6.0F, 1.2F, 6.4F, frame),
                PartPose.ZERO);
    }
}
