package com.example.template.forge;

import com.example.template.TemplateMod;
import com.example.template.block.ModBlocks;
import com.example.template.command.ModCommands;
import com.example.template.forge.fluid.ForgeFluidBridge;
import com.example.template.forge.fluid.ModFluidsImpl;
import com.example.template.forge.life.ChishiLifeInteraction;
import com.example.template.forge.life.PlayerBodyCapability;
import com.example.template.gametest.ChishiFuelSystemTests;
import com.example.template.item.ChishiPortableEnergyCell;
import com.example.template.item.ChishiUpgradeHelper;
import com.example.template.item.ModItems;
import dev.architectury.platform.forge.EventBuses;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Forge 平台入口。@Mod 注解将本类注册为 mods.toml 声明的 "template_mod" 的实现类。
 * 同时承载 Forge 专属事件：
 * - 赤石装备升级属性的动态附加（原版 Item 无 getAttributeModifiers(EquipmentSlot, ItemStack)）
 * - 特殊能力战斗效果（吸血 / 火焰抗性）
 */
@Mod(TemplateMod.MOD_ID)
public final class TemplateModForge {

    /** 护甲升级修饰符 UUID（固定 ID，属性以 UUID 去重） */
    private static final UUID ARMOR_HEALTH_UUID = UUID.fromString("0a1b2c3d-1001-4000-8000-000000000001");
    private static final UUID ARMOR_DAMAGE_UUID = UUID.fromString("0a1b2c3d-1001-4000-8000-000000000002");
    private static final UUID ARMOR_SPEED_UUID = UUID.fromString("0a1b2c3d-1001-4000-8000-000000000003");
    private static final UUID ARMOR_DEFENSE_UUID = UUID.fromString("0a1b2c3d-1001-4000-8000-000000000004");
    private static final UUID ARMOR_TOUGHNESS_UUID = UUID.fromString("0a1b2c3d-1001-4000-8000-000000000005");
    private static final UUID ARMOR_KNOCKBACK_UUID = UUID.fromString("0a1b2c3d-1001-4000-8000-000000000006");
    private static final UUID ARMOR_MOVE_SPEED_UUID = UUID.fromString("0a1b2c3d-1001-4000-8000-000000000007");
    /** 靴子速度提升能力修饰符 UUID（与通用移速能力区分） */
    private static final UUID ARMOR_BOOTS_SPEED_UUID = UUID.fromString("0a1b2c3d-1001-4000-8000-000000000008");
    /** 剑升级修饰符 UUID（与护甲区分，避免同属性冲突覆盖） */
    private static final UUID SWORD_DAMAGE_UUID = UUID.fromString("0a1b2c3d-2001-4000-8000-000000000001");
    private static final UUID SWORD_SPEED_UUID = UUID.fromString("0a1b2c3d-2001-4000-8000-000000000002");

    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADDITION;
    private static final AttributeModifier.Operation MULTIPLY_BASE = AttributeModifier.Operation.MULTIPLY_BASE;

    public TemplateModForge() {
        // 将 Forge 的 Mod 事件总线交给 Architectury，使内容在正确的时机加载
        EventBuses.registerModEventBus(TemplateMod.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // 注册 4 种液体（下界至纯/复合能量 + 至纯/复合燃料）到 Forge 注册表
        ModFluidsImpl.register(FMLJavaModLoadingContext.get().getModEventBus());
        // 注入外部液体访问桥（MEK 等第三方液体能力对接）
        ForgeFluidBridge.init();

        // 注册燃料系统 GameTest（dev 环境 -Dchishi.gametest=1 自动运行）
        FMLJavaModLoadingContext.get().getModEventBus()
                .addListener((RegisterGameTestsEvent event) -> event.register(ChishiFuelSystemTests.class));

        // Curios 饰品集成：通用事件（击杀/挖掘/受伤）走游戏总线；装备与每 tick 由 Curios 自动驱动
        MinecraftForge.EVENT_BUS.register(ChishiCurioIntegration.INSTANCE);

        // 玩家躯体状态（9 槽位器官/肢体）：capability 挂载 + 向 common 注入访问器
        PlayerBodyCapability.init();
        MinecraftForge.EVENT_BUS.register(PlayerBodyCapability.INSTANCE);

        // 生命科技交互：手持样本采集器右键生物抽取样本
        MinecraftForge.EVENT_BUS.register(ChishiLifeInteraction.INSTANCE);

        // 调用通用初始化逻辑
        TemplateMod.init();

        // 游戏事件总线：动态属性修饰符事件 + 特殊能力战斗事件 + 测试指令
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> ModCommands.build(event.getDispatcher()));

        // 客户端：赤石水晶簇贴图含透明像素，须注册 cutout 渲染，否则透明区域渲染成黑色块
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
    }

    /** 方块渲染类型（仅客户端触发）：透明贴图方块必须显式指定 cutout */
    private void onClientSetup(FMLClientSetupEvent event) {
        RenderTypeRegistry.register(RenderType.cutout(), ModBlocks.CHISHI_CRYSTAL_CLUSTER.get());
    }

    /**
     * 为携带升级标签的赤石装备附加升级属性。
     * 事件在 Forge 收集基础属性后触发，addModifier 追加到对应槽位属性集合。
     */
    @SubscribeEvent
    public void onAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!ChishiUpgradeHelper.isChishiGear(stack)) {
            return;
        }
        EquipmentSlot slot = event.getSlotType();
        int damage = ChishiUpgradeHelper.getCount(stack, ChishiUpgradeHelper.TAG_ATTACK_DAMAGE);
        int speed = ChishiUpgradeHelper.getCount(stack, ChishiUpgradeHelper.TAG_ATTACK_SPEED);

        if (stack.getItem() instanceof ArmorItem armorItem) {
            // 护甲：仅当事件槽位与装备实际穿戴槽位一致时生效
            if (slot != armorItem.getEquipmentSlot()) {
                return;
            }
            add(event, slot, Attributes.MAX_HEALTH, ARMOR_HEALTH_UUID,
                    ChishiUpgradeHelper.getCount(stack, ChishiUpgradeHelper.TAG_MAX_HEALTH) * 2.0);
            add(event, slot, Attributes.ATTACK_DAMAGE, ARMOR_DAMAGE_UUID, damage);
            add(event, slot, Attributes.ATTACK_SPEED, ARMOR_SPEED_UUID, speed * 0.15);
            add(event, slot, Attributes.ARMOR, ARMOR_DEFENSE_UUID,
                    ChishiUpgradeHelper.getCount(stack, ChishiUpgradeHelper.TAG_ARMOR));
            add(event, slot, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_UUID,
                    ChishiUpgradeHelper.getCount(stack, ChishiUpgradeHelper.TAG_TOUGHNESS));
            // 特殊能力：击退抗性 +0.1/级，移速 +2%/级（仅护甲穿戴时）
            int knockback = ChishiUpgradeHelper.getCount(stack, ChishiUpgradeHelper.TAG_ABILITY_KNOCKBACK);
            add(event, slot, Attributes.KNOCKBACK_RESISTANCE, ARMOR_KNOCKBACK_UUID, knockback * 0.1);
            int moveSpeed = ChishiUpgradeHelper.getCount(stack, ChishiUpgradeHelper.TAG_ABILITY_SPEED);
            add(event, slot, Attributes.MOVEMENT_SPEED, ARMOR_MOVE_SPEED_UUID, moveSpeed * 0.02, MULTIPLY_BASE);
            // 靴子专属：速度提升（+10%，仅 1 级）
            int bootsSpeed = ChishiUpgradeHelper.getCount(stack, ChishiUpgradeHelper.TAG_ABILITY_BOOTS_SPEED);
            add(event, slot, Attributes.MOVEMENT_SPEED, ARMOR_BOOTS_SPEED_UUID, bootsSpeed * 0.10, MULTIPLY_BASE);
        } else if (slot == EquipmentSlot.MAINHAND) {
            // 剑：仅主手
            add(event, slot, Attributes.ATTACK_DAMAGE, SWORD_DAMAGE_UUID, damage);
            add(event, slot, Attributes.ATTACK_SPEED, SWORD_SPEED_UUID, speed * 0.15);
        }
    }

    /**
     * 特殊能力战斗效果：
     * - 吸血：攻击者持赤石装备（主/副手）时，命中回复 1 生命/级
     * - 火焰/爆炸/摔落保护：受伤方穿戴赤石装备时，对应伤害减免 15%/级（最多 90%）
     */
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        // 吸血：攻击者为玩家
        if (event.getSource().getEntity() instanceof Player attacker) {
            int lifesteal = totalAbility(attacker, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                    ChishiUpgradeHelper.TAG_ABILITY_LIFESTEAL);
            if (lifesteal > 0) {
                attacker.heal(lifesteal);
            }
        }
        // 伤害保护：受伤者为玩家，按来源类型分别减免
        if (event.getEntity() instanceof Player victim) {
            EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            float amount = event.getAmount();
            // 1.20.1 的 DamageSource 无 isFire/isExplosion，统一用伤害类型 tag 判断
            if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                amount = applyProtection(amount, totalAbility(victim, armorSlots, ChishiUpgradeHelper.TAG_ABILITY_FIRE_RESIST));
            }
            if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
                amount = applyProtection(amount, totalAbility(victim, armorSlots, ChishiUpgradeHelper.TAG_ABILITY_BLAST_PROTECT));
            }
            if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
                amount = applyProtection(amount, totalAbility(victim, armorSlots, ChishiUpgradeHelper.TAG_ABILITY_FALL_PROTECT));
            }
            event.setAmount(amount);
            // 胸甲专属：受击时获得生命回复 I（5 秒）
            int hitRegen = totalAbility(victim, new EquipmentSlot[]{EquipmentSlot.CHEST},
                    ChishiUpgradeHelper.TAG_ABILITY_HIT_REGEN);
            if (hitRegen > 0) {
                victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false));
            }
        }
    }

    /**
     * 药水类特殊能力（部位专属）：穿戴对应护甲时持续获得原版药水效果（隐藏粒子，2 秒刷新）。
     * 急迫 / 水下呼吸 / 抗性提升 / 跳跃提升 / 水中加速（海豚的恩惠）/ 缓降。
     */
    @SubscribeEvent
    public void onAbilityTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }
        Player player = event.player;
        if (player.isDeadOrDying()) {
            return;
        }
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : armorSlots) {
            ItemStack gear = player.getItemBySlot(slot);
            if (!ChishiUpgradeHelper.isChishiGear(gear)) {
                continue;
            }
            applyPotionAbility(player, gear, ChishiUpgradeHelper.TAG_ABILITY_HASTE, MobEffects.DIG_SPEED);
            applyPotionAbility(player, gear, ChishiUpgradeHelper.TAG_ABILITY_WATER_BREATHING, MobEffects.WATER_BREATHING);
            applyPotionAbility(player, gear, ChishiUpgradeHelper.TAG_ABILITY_RESISTANCE, MobEffects.DAMAGE_RESISTANCE);
            applyPotionAbility(player, gear, ChishiUpgradeHelper.TAG_ABILITY_JUMP_BOOST, MobEffects.JUMP);
            applyPotionAbility(player, gear, ChishiUpgradeHelper.TAG_ABILITY_SWIM_SPEED, MobEffects.DOLPHINS_GRACE);
            applyPotionAbility(player, gear, ChishiUpgradeHelper.TAG_ABILITY_SLOW_FALLING, MobEffects.SLOW_FALLING);
        }
    }

    /** 若装备携带该能力且玩家当前无对应效果，则补充 2 秒效果（脱下装备后自然消退） */
    private static void applyPotionAbility(Player player, ItemStack gear, String tagKey, MobEffect effect) {
        if (ChishiUpgradeHelper.getCount(gear, tagKey) > 0 && !player.hasEffect(effect)) {
            player.addEffect(new MobEffectInstance(effect, 40, 0, false, false));
        }
    }

    /**
     * 玩家 tick：背包中任意位置的便捷赤能源储存单元都会自动为玩家身上（护甲 + 主副手）
     * 的赤石装备补充耐久，每 1 点耐久消耗 ENERGY_PER_DURABILITY 赤能源，速率受单元等级限制。
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }
        Player player = event.player;
        if (player.isDeadOrDying()) {
            return;
        }
        // 收集背包中全部便携单元（物品栏 36 格 + 副手；护甲槽无法放入非护甲）
        List<ItemStack> cells = new ArrayList<>();
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof ChishiPortableEnergyCell) {
                cells.add(s);
            }
        }
        ItemStack offhand = inv.offhand.get(0);
        if (offhand.getItem() instanceof ChishiPortableEnergyCell) {
            cells.add(offhand);
        }
        if (cells.isEmpty()) {
            return;
        }
        // 修复目标：护甲 4 件 + 主副手（便携单元放不进护甲槽，无需跳过）
        EquipmentSlot[] gearSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
        // 外层遍历单元：每个单元每 tick 有独立修复额度，多个单元可叠加供能
        for (ItemStack cellStack : cells) {
            ChishiPortableEnergyCell portable = (ChishiPortableEnergyCell) cellStack.getItem();
            if (portable.getEnergyStored(cellStack) < ChishiPortableEnergyCell.ENERGY_PER_DURABILITY) {
                continue; // 该单元能量不足，换下一个
            }
            int repairLimit = portable.tier.repairPerTick;
            for (EquipmentSlot slot : gearSlots) {
                if (repairLimit <= 0) {
                    break; // 本单元本 tick 额度用完
                }
                ItemStack gear = player.getItemBySlot(slot);
                if (gear.isEmpty() || !gear.isDamaged()) {
                    continue;
                }
                // 兼容创造模式直接取用的无标签装备，并过滤非赤石装备
                ChishiUpgradeHelper.ensureGear(gear);
                if (!ChishiUpgradeHelper.isChishiGear(gear)) {
                    continue;
                }
                int toRepair = (int) Math.min(repairLimit, Math.min(gear.getDamageValue(),
                        portable.getEnergyStored(cellStack) / ChishiPortableEnergyCell.ENERGY_PER_DURABILITY));
                if (toRepair <= 0) {
                    continue;
                }
                portable.extractEnergy(cellStack, toRepair * ChishiPortableEnergyCell.ENERGY_PER_DURABILITY, false);
                gear.setDamageValue(gear.getDamageValue() - toRepair);
                repairLimit -= toRepair;
            }
        }
    }

    /**
     * 效率升级：挖掘速度每级 +20%（挖掘速度无实体属性，用方块破坏速度事件实现）。
     * 仅主手赤石挖掘工具（铲/斧/镐）生效。
     */
    @SubscribeEvent
    public void onBlockBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        ItemStack main = player.getMainHandItem();
        if (ChishiUpgradeHelper.isChishiGear(main)) {
            int eff = ChishiUpgradeHelper.getCount(main, ChishiUpgradeHelper.TAG_EFFICIENCY);
            if (eff > 0) {
                event.setNewSpeed(event.getNewSpeed() * (1.0F + 0.2F * eff));
            }
        }
    }

    /**
     * 工具专属能力：区域破坏。破坏方块时同时破坏以目标为中心的 3×3×3 区域
     * （跳过空气与不可破坏方块），每次区域破坏额外消耗 1 + 实际破坏数/4 点耐久。
     */
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide) {
            return;
        }
        ItemStack main = player.getMainHandItem();
        if (!ChishiUpgradeHelper.isChishiGear(main)) {
            return;
        }
        if (ChishiUpgradeHelper.getCount(main, ChishiUpgradeHelper.TAG_ABILITY_AREA_BREAK) <= 0) {
            return;
        }
        event.setCanceled(true); // 取消原破坏，由下方统一处理 3×3×3
        BlockPos center = event.getPos();
        Level level = player.level();
        boolean drop = !player.isCreative();
        int broken = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.getX() - 1, center.getY() - 1, center.getZ() - 1,
                center.getX() + 1, center.getY() + 1, center.getZ() + 1)) {
            BlockState state = level.getBlockState(p);
            if (state.isAir() || state.getDestroySpeed(level, p) < 0) {
                continue; // 空气与基岩等不可破坏方块跳过
            }
            if (level.destroyBlock(p, drop)) {
                broken++;
            }
        }
        if (broken > 0) {
            main.hurtAndBreak(broken / 4 + 1, player, e -> {
            });
        }
    }

    /**
     * 剑专属能力：击杀掉落赤石精华。每级 15% 概率（3 级最多 45%），掉落 1 个赤石精华。
     */
    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (event.getSource().getEntity() instanceof Player attacker) {
            int level = totalAbility(attacker, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                    ChishiUpgradeHelper.TAG_ABILITY_DROP_ESSENCE);
            if (level > 0 && attacker.getRandom().nextFloat() < 0.15F * level) {
                Level world = attacker.level();
                net.minecraft.world.phys.Vec3 pos = event.getEntity().position();
                event.getDrops().add(new ItemEntity(world, pos.x, pos.y, pos.z,
                        new ItemStack(ModItems.chishiEssence.get(), 1)));
            }
        }
    }

    /** 按能力等级减免伤害：15%/级，最多 90% */
    private static float applyProtection(float amount, int level) {
        if (level <= 0) {
            return amount;
        }
        float reduction = Math.min(0.9F, 0.15F * level);
        return amount * (1.0F - reduction);
    }

    /** 统计指定槽位赤石装备上某能力的总等级 */
    private static int totalAbility(LivingEntity entity, EquipmentSlot[] slots, String tagKey) {
        int total = 0;
        for (EquipmentSlot slot : slots) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (ChishiUpgradeHelper.isChishiGear(stack)) {
                total += ChishiUpgradeHelper.getCount(stack, tagKey);
            }
        }
        return total;
    }

    private static int totalAbility(LivingEntity entity, EquipmentSlot slot1, EquipmentSlot slot2, String tagKey) {
        return totalAbility(entity, new EquipmentSlot[]{slot1, slot2}, tagKey);
    }

    private static void add(ItemAttributeModifierEvent event, EquipmentSlot slot,
                            net.minecraft.world.entity.ai.attributes.Attribute attribute,
                            UUID id, double amount) {
        if (amount != 0.0) {
            event.addModifier(attribute, new AttributeModifier(id, "Chishi upgrade", amount, ADD));
        }
    }

    private static void add(ItemAttributeModifierEvent event, EquipmentSlot slot,
                            net.minecraft.world.entity.ai.attributes.Attribute attribute,
                            UUID id, double amount, AttributeModifier.Operation operation) {
        if (amount != 0.0) {
            event.addModifier(attribute, new AttributeModifier(id, "Chishi ability", amount, operation));
        }
    }

    /** dev 环境燃料系统/反应堆系统 GameTest 入口（-Dchishi.gametest=1 / -Dchishi.gametest.reactor=1 时服务端启动后自动运行） */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        if (System.getProperty("chishi.gametest") != null) {
            ChishiGameTestAutoRunner.start(event.getServer());
        }
        if (System.getProperty("chishi.gametest.reactor") != null) {
            ChishiReactorGameTestAutoRunner.start(event.getServer());
        }
    }

    /** 测试运行期间每 tick 检查进度，全部完成后自动关闭服务端 */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ChishiGameTestAutoRunner.tick();
            ChishiReactorGameTestAutoRunner.tick();
        }
    }
}
