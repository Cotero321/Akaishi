package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import com.example.akaishi.block.entity.AkaishiGeneAnalyzerBlockEntity;
import com.example.akaishi.block.entity.AkaishiCultivatorBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeStructBlockEntity;
import com.example.akaishi.block.entity.AkaishiLifeBreederBlockEntity;
import com.example.akaishi.block.entity.AkaishiTraitReforgerBlockEntity;
import com.example.akaishi.block.entity.AkaishiTransgeneFactoryBlockEntity;
import com.example.akaishi.block.entity.AkaishiSurgeryBlockEntity;
import com.example.akaishi.block.entity.AkaishiPotionTableBlockEntity;
import com.example.akaishi.block.entity.AkaishiOrganVaultBlockEntity;
import com.example.akaishi.block.entity.AkaishiPotionCabinetBlockEntity;
import com.example.akaishi.block.entity.AkaishiSampleVaultBlockEntity;
import com.example.akaishi.block.entity.AkaishiDecayPurifierBlockEntity;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 菜单注册 · 生命器官族（承接 {@link ModMenus} 原注册序第 26~39 位）：
 * 躯体检查仪、基因管理器、生命分析台、部件培养舱、生命结构台、生命培育器、词条重铸仪、
 * 转基因工厂、手术仓、药剂台、器官储藏库、药剂库、样本库、衰变净化塔。
 * <p>类内注册顺序与原 {@code ModMenus.register()} 逐位一致。
 */
final class LifeMenuRegs {

    private LifeMenuRegs() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void register() {
        // 躯体检查仪：无机器槽位，纯展示玩家躯体状态（数据由 S2C 同步包推送）
        MenuType<AkaishiBodyScannerMenu> bodyScannerType = MenuRegistry.ofExtended((syncId, inv, buf) ->
                new AkaishiBodyScannerMenu(syncId, inv));
        ModMenus.CHISHI_BODY_SCANNER = (RegistrySupplier<MenuType<AkaishiBodyScannerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_body_scanner"), () -> bodyScannerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(bodyScannerType, AkaishiBodyScannerScreen::new));

        // 基因管理器：无机器槽位，展示/卸载已吸收基因强化（数据 S2C 推送，卸载走 C2S）
        // extraData 携带方块坐标（BE.saveExtraData 写入），客户端据此还原菜单绑定方块
        MenuType<AkaishiGeneManagerMenu> geneManagerType = MenuRegistry.ofExtended((syncId, inv, buf) ->
                new AkaishiGeneManagerMenu(syncId, inv, buf.readBlockPos()));
        ModMenus.CHISHI_GENE_MANAGER = (RegistrySupplier<MenuType<AkaishiGeneManagerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_gene_manager"), () -> geneManagerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(geneManagerType, AkaishiGeneManagerScreen::new));

        // 生命分析台：输入（纯度 100 样本）+ 输出（基因序列片段）+ 生命能量/进度数据
        MenuType<AkaishiGeneAnalyzerMenu> geneAnalyzerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiGeneAnalyzerBlockEntity analyzer) {
                return new AkaishiGeneAnalyzerMenu(syncId, inv, analyzer.inventory(), analyzer.data(), analyzer.getUpgradeSlots(), pos);
            }
            return new AkaishiGeneAnalyzerMenu(syncId, inv,
                    new SimpleContainer(AkaishiGeneAnalyzerBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiGeneAnalyzerBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots(), pos);
        });
        ModMenus.CHISHI_GENE_ANALYZER = (RegistrySupplier<MenuType<AkaishiGeneAnalyzerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_gene_analyzer"), () -> geneAnalyzerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(geneAnalyzerType, AkaishiGeneAnalyzerScreen::new));

        // 部件培养舱：输入（样本/器官）+ 材料（固态物）+ 生命能量/进度/模式数据
        MenuType<AkaishiCultivatorMenu> cultivatorType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiCultivatorBlockEntity cultivator) {
                return new AkaishiCultivatorMenu(syncId, inv, cultivator.inventory(), cultivator.data(),
                        cultivator.getUpgradeSlots(), pos);
            }
            return new AkaishiCultivatorMenu(syncId, inv,
                    new SimpleContainer(AkaishiCultivatorBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiCultivatorBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots(), pos);
        });
        ModMenus.CHISHI_CULTIVATOR = (RegistrySupplier<MenuType<AkaishiCultivatorMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_cultivator"), () -> cultivatorType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(cultivatorType, AkaishiCultivatorScreen::new));

        // 生命结构台：基因序列 + 固态物 → 器官（目标槽位界面选择）
        MenuType<AkaishiLifeStructMenu> lifeStructType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifeStructBlockEntity struct) {
                return new AkaishiLifeStructMenu(syncId, inv, struct.inventory(), struct.data(),
                        struct.getUpgradeSlots(), struct.getBlockPos());
            }
            return new AkaishiLifeStructMenu(syncId, inv,
                    new SimpleContainer(AkaishiLifeStructBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiLifeStructBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots(), null);
        });
        ModMenus.CHISHI_LIFE_STRUCT = (RegistrySupplier<MenuType<AkaishiLifeStructMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_struct"), () -> lifeStructType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(lifeStructType, AkaishiLifeStructScreen::new));

        // 生命培育器：器官 + 同源基因序列 + 衰竭结晶 → 突变器官（纯度决定成功率）
        MenuType<AkaishiLifeBreederMenu> breederType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiLifeBreederBlockEntity breeder) {
                return new AkaishiLifeBreederMenu(syncId, inv, breeder.inventory(), breeder.data(),
                        breeder.getUpgradeSlots(), breeder.getBlockPos());
            }
            return new AkaishiLifeBreederMenu(syncId, inv,
                    new SimpleContainer(AkaishiLifeBreederBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiLifeBreederBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots(), pos);
        });
        ModMenus.CHISHI_LIFE_BREEDER = (RegistrySupplier<MenuType<AkaishiLifeBreederMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_life_breeder"), () -> breederType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(breederType, AkaishiLifeBreederScreen::new));

        // 词条重铸仪：器官 + 衰竭结晶 → 原位替换指定第 N 条突变词条（确定性必成）
        MenuType<AkaishiTraitReforgerMenu> traitReforgerType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiTraitReforgerBlockEntity reforger) {
                return new AkaishiTraitReforgerMenu(syncId, inv, reforger.inventory(), reforger.data(),
                        reforger.getUpgradeSlots(), reforger.getBlockPos());
            }
            return new AkaishiTraitReforgerMenu(syncId, inv,
                    new SimpleContainer(AkaishiTraitReforgerBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiTraitReforgerBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots(), pos);
        });
        ModMenus.CHISHI_TRAIT_REFORGER = (RegistrySupplier<MenuType<AkaishiTraitReforgerMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_trait_reforger"), () -> traitReforgerType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(traitReforgerType, AkaishiTraitReforgerScreen::new));

        // 转基因工厂：凋零骷髅基因（纯度≥50）+ 缠怨藤 + 凋零玫瑰 + 固态物 → 凋零藤
        MenuType<AkaishiTransgeneFactoryMenu> transgeneFactoryType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiTransgeneFactoryBlockEntity factory) {
                return new AkaishiTransgeneFactoryMenu(syncId, inv, factory.inventory(), factory.data());
            }
            return new AkaishiTransgeneFactoryMenu(syncId, inv);
        });
        ModMenus.CHISHI_TRANSGENE_FACTORY = (RegistrySupplier<MenuType<AkaishiTransgeneFactoryMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_transgene_factory"), () -> transgeneFactoryType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(transgeneFactoryType, AkaishiTransgeneFactoryScreen::new));

        // 手术仓：器官移植/摘除（消耗固态 + 生命能量，带进度）
        MenuType<AkaishiSurgeryMenu> surgeryType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiSurgeryBlockEntity surgery) {
                return new AkaishiSurgeryMenu(syncId, inv, surgery.inventory(), surgery.data(),
                        surgery.getUpgradeSlots(), surgery.getBlockPos());
            }
            return new AkaishiSurgeryMenu(syncId, inv,
                    new SimpleContainer(AkaishiSurgeryBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiSurgeryBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots(), pos);
        });
        ModMenus.CHISHI_SURGERY = (RegistrySupplier<MenuType<AkaishiSurgeryMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_surgery"), () -> surgeryType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(surgeryType, AkaishiSurgeryScreen::new));

        // 药剂台：样本+固态+生命能量 → 永久/突破药剂（模板选择走 C2S 包）
        MenuType<AkaishiPotionTableMenu> potionTableType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPotionTableBlockEntity potionTable) {
                return new AkaishiPotionTableMenu(syncId, inv, potionTable.inventory(), potionTable.data(),
                        potionTable.getUpgradeSlots(), potionTable.getBlockPos());
            }
            return new AkaishiPotionTableMenu(syncId, inv,
                    new SimpleContainer(AkaishiPotionTableBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiPotionTableBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots(), pos);
        });
        ModMenus.CHISHI_POTION_TABLE = (RegistrySupplier<MenuType<AkaishiPotionTableMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_potion_table"), () -> potionTableType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(potionTableType, AkaishiPotionTableScreen::new));

        // 器官储藏库：按躯体槽位分页的器官仓库（选页为客户端本地状态，无需网络包）
        MenuType<AkaishiOrganVaultMenu> organVaultType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiOrganVaultBlockEntity vault) {
                return new AkaishiOrganVaultMenu(syncId, inv, vault);
            }
            return new AkaishiOrganVaultMenu(syncId, inv,
                    new SimpleContainer(AkaishiOrganVaultBlockEntity.SLOT_COUNT),
                    new SimpleContainerData(AkaishiOrganVaultBlockEntity.DATA_SLOTS), pos);
        });
        ModMenus.CHISHI_ORGAN_VAULT = (RegistrySupplier<MenuType<AkaishiOrganVaultMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_organ_vault"), () -> organVaultType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(organVaultType, AkaishiOrganVaultScreen::new));

        // 药剂库：大容量药剂仓库（筛选为客户端本地状态，无需网络包）
        MenuType<AkaishiPotionCabinetMenu> potionCabinetType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPotionCabinetBlockEntity cabinet) {
                return new AkaishiPotionCabinetMenu(syncId, inv, cabinet);
            }
            return new AkaishiPotionCabinetMenu(syncId, inv,
                    new SimpleContainer(AkaishiPotionCabinetBlockEntity.CABINET_SLOTS));
        });
        ModMenus.CHISHI_POTION_CABINET = (RegistrySupplier<MenuType<AkaishiPotionCabinetMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_potion_cabinet"), () -> potionCabinetType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(potionCabinetType, AkaishiPotionCabinetScreen::new));

        // 样本库：大容量样本仓库（同 NBT 自动合并）
        MenuType<AkaishiSampleVaultMenu> sampleVaultType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiSampleVaultBlockEntity vault) {
                return new AkaishiSampleVaultMenu(syncId, inv, vault);
            }
            return new AkaishiSampleVaultMenu(syncId, inv,
                    new SimpleContainer(AkaishiSampleVaultBlockEntity.SAMPLE_SLOTS));
        });
        ModMenus.CHISHI_SAMPLE_VAULT = (RegistrySupplier<MenuType<AkaishiSampleVaultMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_sample_vault"), () -> sampleVaultType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(sampleVaultType, AkaishiSampleVaultScreen::new));

        // 衰变净化塔：无机器槽位，升级槽（速度/能量）+ 4 数据槽（能量/容量/净化中/区域数）
        MenuType<AkaishiDecayPurifierMenu> decayPurifierType = MenuRegistry.ofExtended((syncId, inv, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Level level = inv.player.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiDecayPurifierBlockEntity purifier) {
                return new AkaishiDecayPurifierMenu(syncId, inv, purifier.data(), purifier.getUpgradeSlots());
            }
            return new AkaishiDecayPurifierMenu(syncId, inv,
                    new SimpleContainerData(AkaishiDecayPurifierBlockEntity.DATA_SLOTS),
                    new MachineUpgradeSlots());
        });
        ModMenus.CHISHI_DECAY_PURIFIER = (RegistrySupplier<MenuType<AkaishiDecayPurifierMenu>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.MENU)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_decay_purifier"), () -> decayPurifierType);
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                MenuRegistry.registerScreenFactory(decayPurifierType, AkaishiDecayPurifierScreen::new));
    }
}
