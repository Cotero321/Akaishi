package com.example.akaishi.item;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.life.mechanical.MechanicalOrganType;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * 机械域物品注册：机械器官（9 槽位）、部件模板、加工部件、机械材料。
 * <p>
 * 注册实现按功能域拆分到此域类，禁止向 ModItems 堆积注册逻辑。
 * 门面转发由 {@link ModItems#register()} 完成。
 * <p>
 * 机械器官使用 {@link MechanicalOrganItem}（BEWLR 动态纹理渲染），
 * 部件模板/加工件使用 {@link MechanicalPartItem}（NBT 驱动 + BEWLR 渲染）。
 */
public final class AkaishiMechanicalItems {

    private AkaishiMechanicalItems() {}

    // ==================== 机械器官（9 槽位，各一） ====================
    public static RegistrySupplier<Item> mechanicalEye;
    public static RegistrySupplier<Item> mechanicalHeart;
    public static RegistrySupplier<Item> mechanicalLungs;
    public static RegistrySupplier<Item> mechanicalViscera;
    public static RegistrySupplier<Item> mechanicalKidneys;
    public static RegistrySupplier<Item> mechanicalLeftArm;
    public static RegistrySupplier<Item> mechanicalRightArm;
    public static RegistrySupplier<Item> mechanicalLeftLeg;
    public static RegistrySupplier<Item> mechanicalRightLeg;

    // ==================== 机械部件模板 ====================
    /** 通用部件塑形模板：工作台合成（需求生命固态物），模板制造厂将其塑形成部位模板（NBT 驱动） */
    public static RegistrySupplier<Item> genericPartMould;
    /** 模板制造厂产出：NBT 存储部件/器官/DNA 信息，BEWLR 动态渲染（未注入材料，铁色占位） */
    public static RegistrySupplier<Item> mechanicalPartTemplate;
    /** 加工制作厂产出：NBT 驱动，BEWLR 动态渲染（带"已加工"标记 + 注入材料） */
    public static RegistrySupplier<Item> mechanicalProcessedPart;

    // ==================== 机械材料 ====================
    public static RegistrySupplier<Item> redstoneAlloyIngot;
    public static RegistrySupplier<Item> ceramicCompositePlate;
    public static RegistrySupplier<Item> resistantSteelIngot;
    public static RegistrySupplier<Item> precisionAlloyIngot;
    public static RegistrySupplier<Item> polymerizedRedstoneCore;
    public static RegistrySupplier<Item> bioCeramicPlate;
    public static RegistrySupplier<Item> refinedCore;
    public static RegistrySupplier<Item> alloySteelIngot;
    public static RegistrySupplier<Item> psionicCompositeIngot;

    public static void register() {
        // 机械器官（9 槽位，不可堆叠，使用 MechanicalOrganItem）
        mechanicalEye = item("akaishi_mechanical_eye", () -> new MechanicalOrganItem(stacks(1)));
        mechanicalHeart = item("akaishi_mechanical_heart", () -> new MechanicalOrganItem(stacks(1)));
        mechanicalLungs = item("akaishi_mechanical_lungs", () -> new MechanicalOrganItem(stacks(1)));
        mechanicalViscera = item("akaishi_mechanical_viscera", () -> new MechanicalOrganItem(stacks(1)));
        mechanicalKidneys = item("akaishi_mechanical_kidneys", () -> new MechanicalOrganItem(stacks(1)));
        mechanicalLeftArm = item("akaishi_mechanical_left_arm", () -> new MechanicalOrganItem(stacks(1)));
        mechanicalRightArm = item("akaishi_mechanical_right_arm", () -> new MechanicalOrganItem(stacks(1)));
        mechanicalLeftLeg = item("akaishi_mechanical_left_leg", () -> new MechanicalOrganItem(stacks(1)));
        mechanicalRightLeg = item("akaishi_mechanical_right_leg", () -> new MechanicalOrganItem(stacks(1)));

        // 通用部件塑形模板 + 机械部件模板 + 加工部件（通用模板为普通物品；后两者 NBT 驱动，BEWLR 动态渲染）
        genericPartMould = item("akaishi_generic_part_mould", () -> new Item(stacks(64)));
        mechanicalPartTemplate = item("akaishi_mechanical_part_template",
                () -> new MechanicalPartItem(stacks(1)));
        mechanicalProcessedPart = item("akaishi_mechanical_processed_part",
                () -> new MechanicalPartItem(stacks(1)));

        // 机械材料（9 种，普通 Item）
        redstoneAlloyIngot = item("akaishi_redstone_alloy_ingot");
        ceramicCompositePlate = item("akaishi_ceramic_composite_plate");
        resistantSteelIngot = item("akaishi_resistant_steel_ingot");
        precisionAlloyIngot = item("akaishi_precision_alloy_ingot");
        polymerizedRedstoneCore = item("akaishi_polymerized_redstone_core");
        bioCeramicPlate = item("akaishi_bio_ceramic_plate");
        refinedCore = item("akaishi_refined_core");
        alloySteelIngot = item("akaishi_alloy_steel_ingot");
        psionicCompositeIngot = item("akaishi_psionic_composite_ingot");
    }

    private static Item.Properties stacks(int max) {
        return new Item.Properties().stacksTo(max);
    }

    private static RegistrySupplier<Item> item(String id) {
        return item(id, () -> new Item(new Item.Properties()));
    }

    private static RegistrySupplier<Item> item(String id, Supplier<Item> factory) {
        return RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
    }

    // ==================== 材料映射 ====================

    /** 机械材料物品 → MechanicalMaterial.id（加工厂材料槽据此判定材料种类），非机械材料返回 null */
    public static String materialIdOf(net.minecraft.world.item.Item item) {
        if (item == redstoneAlloyIngot.get()) return "akaishi:redstone_alloy";
        if (item == ceramicCompositePlate.get()) return "akaishi:ceramic_composite";
        if (item == resistantSteelIngot.get()) return "akaishi:resistant_steel";
        if (item == precisionAlloyIngot.get()) return "akaishi:precision_alloy";
        if (item == polymerizedRedstoneCore.get()) return "akaishi:polymerized_redstone";
        if (item == bioCeramicPlate.get()) return "akaishi:bio_ceramic";
        if (item == refinedCore.get()) return "akaishi:refined_core";
        if (item == alloySteelIngot.get()) return "akaishi:alloy_steel";
        if (item == psionicCompositeIngot.get()) return "akaishi:psionic_composite";
        return null;
    }
}