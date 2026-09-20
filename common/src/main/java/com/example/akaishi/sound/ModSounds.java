package com.example.akaishi.sound;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * 模组音效注册表。音效资源位于 assets/akaishi/sounds/*.ogg（自合成），
 * 由 sounds.json 关联事件名。注册时机与方块一致：注册事件期间延迟求值。
 * <p>
 * 机器运转音一律以 {@code *_hum} 结尾，并由 {@link MachineHum} 在各机器 tickServer 中播放。
 * 祭坛氛围音（{@code VOID_*}）为整段无缝循环素材，同样经 {@link MachineHum} 播放，
 * 只是重播间隔取音效时长以实现连续不断（见 {@code AkaishiMotherAltarBlockEntity}）。
 */
public final class ModSounds {

    private ModSounds() {
    }

    // ==================== 反应堆 / 结构事件 ====================

    /** 反应堆燃烧运转（循环） */
    public static final RegistrySupplier<SoundEvent> REACTOR_HUM = reg("reactor_hum");
    /** 反应堆高温警告 */
    public static final RegistrySupplier<SoundEvent> REACTOR_WARN = reg("reactor_warn");
    /** 反应堆爆炸 */
    public static final RegistrySupplier<SoundEvent> REACTOR_EXPLOSION = reg("reactor_explosion");
    /** 生命活化器液体活化（循环） */
    public static final RegistrySupplier<SoundEvent> ACTIVATOR_BUBBLE = reg("activator_bubble");
    /** 多方块结构成型 */
    public static final RegistrySupplier<SoundEvent> MULTIBLOCK_ACTIVATE = reg("multiblock_activate");
    /** 衰竭区域泄漏警示 */
    public static final RegistrySupplier<SoundEvent> DECAY_LEAK = reg("decay_leak");

    // ==================== 赤能机械 ====================

    /** 能量发生器运转 */
    public static final RegistrySupplier<SoundEvent> ENERGY_GENERATOR_HUM = reg("energy_generator_hum");
    /** 能量处理器运转 */
    public static final RegistrySupplier<SoundEvent> ENERGY_PROCESSOR_HUM = reg("energy_processor_hum");
    /** 能量液化器运转 */
    public static final RegistrySupplier<SoundEvent> ENERGY_LIQUEFIER_HUM = reg("energy_liquefier_hum");
    /** 能量聚合器运转 */
    public static final RegistrySupplier<SoundEvent> ENERGY_AGGREGATOR_HUM = reg("energy_aggregator_hum");
    /** 发电矩阵控制器运转 */
    public static final RegistrySupplier<SoundEvent> GEN_MATRIX_HUM = reg("gen_matrix_hum");
    /** 压缩机运转 */
    public static final RegistrySupplier<SoundEvent> COMPRESSOR_HUM = reg("compressor_hum");
    /** 粉碎机运转 */
    public static final RegistrySupplier<SoundEvent> PULVERIZER_HUM = reg("pulverizer_hum");
    /** 变压器运转 */
    public static final RegistrySupplier<SoundEvent> TRANSFORMER_HUM = reg("transformer_hum");
    /** 催化剂室运转 */
    public static final RegistrySupplier<SoundEvent> CATALYST_HUM = reg("catalyst_hum");
    /** 燃料混合器运转 */
    public static final RegistrySupplier<SoundEvent> FUEL_MIXER_HUM = reg("fuel_mixer_hum");
    /** 燃料装罐机运转 */
    public static final RegistrySupplier<SoundEvent> FUEL_CANNER_HUM = reg("fuel_canner_hum");

    // ==================== 生命 / 净化 ====================

    /** 净化器运转 */
    public static final RegistrySupplier<SoundEvent> PURIFIER_HUM = reg("purifier_hum");
    /** 衰变净化器运转 */
    public static final RegistrySupplier<SoundEvent> DECAY_PURIFIER_HUM = reg("decay_purifier_hum");
    /** 生命净化器运转 */
    public static final RegistrySupplier<SoundEvent> LIFE_PURIFIER_HUM = reg("life_purifier_hum");
    /** 净化矩阵控制器运转 */
    public static final RegistrySupplier<SoundEvent> PURIFIER_MATRIX_HUM = reg("purifier_matrix_hum");
    /** 生命培育器运转 */
    public static final RegistrySupplier<SoundEvent> LIFE_BREEDER_HUM = reg("life_breeder_hum");
    /** 生命离心机运转 */
    public static final RegistrySupplier<SoundEvent> LIFE_CENTRIFUGE_HUM = reg("life_centrifuge_hum");
    /** 生命聚合转换器运转 */
    public static final RegistrySupplier<SoundEvent> LIFE_AGGREGATION_HUM = reg("life_aggregation_hum");
    /** 生命融合砧运转 */
    public static final RegistrySupplier<SoundEvent> LIFE_FUSION_ANVIL_HUM = reg("life_fusion_anvil_hum");
    /** 生命矩阵控制器运转 */
    public static final RegistrySupplier<SoundEvent> LIFE_MATRIX_HUM = reg("life_matrix_hum");

    // ==================== 聚变 / 等离子 ====================

    /** 聚变控制器运转 */
    public static final RegistrySupplier<SoundEvent> FUSION_CONTROLLER_HUM = reg("fusion_controller_hum");
    /** 聚变燃料聚合器运转 */
    public static final RegistrySupplier<SoundEvent> FUSION_FUEL_AGGREGATOR_HUM = reg("fusion_fuel_aggregator_hum");
    /** 等离子填充器运转 */
    public static final RegistrySupplier<SoundEvent> PLASMA_FILLER_HUM = reg("plasma_filler_hum");
    /** 活化分馏塔运转 */
    public static final RegistrySupplier<SoundEvent> ACTIVATED_FRACTIONATOR_HUM = reg("activated_fractionator_hum");

    // ==================== 采矿 / 基因 ====================

    /** 采矿控制器运转 */
    public static final RegistrySupplier<SoundEvent> MINER_HUM = reg("miner_hum");
    /** 基因分析仪运转 */
    public static final RegistrySupplier<SoundEvent> GENE_ANALYZER_HUM = reg("gene_analyzer_hum");
    /** 转基因工厂运转 */
    public static final RegistrySupplier<SoundEvent> TRANSGENE_FACTORY_HUM = reg("transgene_factory_hum");
    /** 培育器运转 */
    public static final RegistrySupplier<SoundEvent> CULTIVATOR_HUM = reg("cultivator_hum");
    /** 植物培育器运转 */
    public static final RegistrySupplier<SoundEvent> PLANT_CULTIVATOR_HUM = reg("plant_cultivator_hum");
    /** 手术台运转 */
    public static final RegistrySupplier<SoundEvent> SURGERY_HUM = reg("surgery_hum");

    // ==================== 机械改造 ====================

    /** 机械模板工厂运转 */
    public static final RegistrySupplier<SoundEvent> MECHANICAL_TEMPLATE_FACTORY_HUM = reg("mechanical_template_factory_hum");
    /** 机械加工厂运转 */
    public static final RegistrySupplier<SoundEvent> MECHANICAL_PROCESSING_HUM = reg("mechanical_processing_hum");
    /** 机械装配站运转 */
    public static final RegistrySupplier<SoundEvent> MECHANICAL_ASSEMBLY_HUM = reg("mechanical_assembly_hum");
    /** 升级站运转 */
    public static final RegistrySupplier<SoundEvent> UPGRADE_STATION_HUM = reg("upgrade_station_hum");
    /** 装备锻造机运转 */
    public static final RegistrySupplier<SoundEvent> EQUIPMENT_FORGER_HUM = reg("equipment_forger_hum");
    /** 物品重构仪运转 */
    public static final RegistrySupplier<SoundEvent> ITEM_RECONSTRUCTOR_HUM = reg("item_reconstructor_hum");
    /** 词条重铸机运转 */
    public static final RegistrySupplier<SoundEvent> TRAIT_REFORGER_HUM = reg("trait_reforger_hum");
    /** 药水台运转 */
    public static final RegistrySupplier<SoundEvent> POTION_TABLE_HUM = reg("potion_table_hum");
    /** 自动收集器运转 */
    public static final RegistrySupplier<SoundEvent> AUTO_COLLECTOR_HUM = reg("auto_collector_hum");

    // ==================== 祭坛氛围音 ====================

    /** 母神祭坛成型后未工作的氛围音：虚空的心跳声（5s 无缝循环） */
    public static final RegistrySupplier<SoundEvent> VOID_HEARTBEAT = reg("void_heartbeat");
    /** 母神祭坛仪式进行中的氛围音：虚空呓语声（8s 无缝循环） */
    public static final RegistrySupplier<SoundEvent> VOID_WHISPER = reg("void_whisper");

    // ==================== 不可名状 ====================

    /** 「不可名状」减益的耳中呓语：以玩家自身为音源播放的一次性低语（8s） */
    public static final RegistrySupplier<SoundEvent> UNNAMEABLE_WHISPER = reg("unnameable_whisper");

    // ==================== BOSS 战斗音乐 ====================

    /**
     * 阿盖托洛丝战斗音乐（53s 无缝循环素材）。
     * <p>
     * 与机器 {@code *_hum} 不同，本条<b>不由服务端播放</b>：它是位置音效且需要"BOSS 一死立刻停"，
     * 只有客户端的循环 SoundInstance 能做到（服务端 {@code playSound} 播出去就收不回来），
     * 故实际消费方是 forge 客户端 {@code AgaitolosMusicHandler} / {@code AgaitolosThemeSound}。
     */
    public static final RegistrySupplier<SoundEvent> AGAITOLOS_THEME = reg("agaitolos_theme");

    /** 强制类加载：确保 SoundEvent 在注册事件前完成注册（游戏启动阶段由 AkaishiMod.init 调用） */
    public static void touch() {
    }

    private static RegistrySupplier<SoundEvent> reg(String name) {
        Registrar<SoundEvent> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.SOUND_EVENT);
        ResourceLocation id = new ResourceLocation(AkaishiMod.MOD_ID, name);
        return registrar.register(id, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
