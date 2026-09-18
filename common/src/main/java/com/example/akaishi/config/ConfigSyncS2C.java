package com.example.akaishi.config;

import com.example.akaishi.AkaishiMod;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 配置 S2C 同步：服务端权威配置值 → 客户端。
 * 客户端界面会直接读取 ModConfig 显示（排斥上限、反应堆/聚变温度标尺、
 * 培养机成功率、加工耗时等）；专用服务器上服务端与客户端的 common.toml
 * 各自独立，必须在玩家登录与配置热重载时推送服务端值覆盖客户端本地值，
 * 否则界面标尺/成功率与实际服务端行为不一致。
 * 仅同步"客户端界面会显示"的字段，其余纯服务端数值无需网络开销。
 */
public final class ConfigSyncS2C {

    public static final ResourceLocation CHANNEL = new ResourceLocation(AkaishiMod.MOD_ID, "config_sync");

    private ConfigSyncS2C() {
    }

    /** 客户端注册接收器（AkaishiMod.init 的 Env.CLIENT 分支调用） */
    public static void registerClient() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, CHANNEL, (buf, context) -> {
            // 网络线程读取后调度到客户端主线程应用（volatile 字段原子覆盖）
            int maxRejection = buf.readVarInt();
            int reactorTempMax = buf.readVarInt();
            int reactorTempWarn = buf.readVarInt();
            int reactorTempOptMin = buf.readVarInt();
            int reactorTempOptMax = buf.readVarInt();
            int fusionTempTrip = buf.readVarInt();
            int fusionTempOptMin = buf.readVarInt();
            int fusionTempOptMax = buf.readVarInt();
            int aggregatorProcessTicks = buf.readVarInt();
            int fillerProcessTicks = buf.readVarInt();
            int[] cultivatorUpgradeSuccess = readArray(buf);
            int[] cultivatorPurifySuccess = readArray(buf);
            long surgeryImplantLifeCost = buf.readVarLong();
            long surgeryExtractLifeCost = buf.readVarLong();
            int surgeryImplantSolidCost = buf.readVarInt();
            int surgeryExtractSolidCost = buf.readVarInt();
            long lifeBreederLifeCost = buf.readVarLong();
            int lifeBreederCrystalCost = buf.readVarInt();
            long traitReforgerLifeCost = buf.readVarLong();
            long transgeneFactoryLifeCost = buf.readVarLong();
            long equipmentForgerEnergyPerForge = buf.readVarLong();
            long[] cultivatorPurifyEnergy = readLongArray(buf);
            int[] cultivatorPurifySolid = readArray(buf);
            int[] cultivatorPurifyTicks = readArray(buf);
            int cultivatorPurifyGain = buf.readVarInt();
            long cultivatorLifeCapacity = buf.readVarLong();
            int[] cultivatorUpgradeEnergy = readArray(buf);
            int[] cultivatorUpgradeSolid = readArray(buf);
            int[] cultivatorUpgradeTicks = readArray(buf);
            int cultivatorUpgradeCompatBonus = buf.readVarInt();
            long[] mechProcessChishiBase = readLongArray(buf);
            long[] mechProcessLifeBase = readLongArray(buf);
            int[] mechProcessTicksBase = readArray(buf);
            int[] mechProcessPartFactor = readArray(buf);
            int[] mechProcessMaterialCount = readArray(buf);
            long mechTemplateChishiCost = buf.readVarLong();
            long mechTemplateLifeCost = buf.readVarLong();
            int mechTemplateTicks = buf.readVarInt();
            long mechAssemblyChishiCost = buf.readVarLong();
            long mechAssemblyLifeCost = buf.readVarLong();
            int mechAssemblyTicks = buf.readVarInt();
            long mechanicalChishiCapacity = buf.readVarLong();
            long mechanicalLifeCapacity = buf.readVarLong();
            double mechBodyHealthScale = buf.readDouble();
            double mechBodyAttackScale = buf.readDouble();
            double mechBodyAttackSpeedScale = buf.readDouble();
            double mechBodyMovementSpeedScale = buf.readDouble();
            double mechBodyArmorScale = buf.readDouble();
            double mechBodyCritChanceScale = buf.readDouble();
            double mechBodyCritDamageScale = buf.readDouble();
            double mechBodyRangeScale = buf.readDouble();
            double mechBodyDodgeScale = buf.readDouble();
            boolean combatCritEnabled = buf.readBoolean();
            boolean combatDodgeEnabled = buf.readBoolean();
            double combatCritChanceCap = buf.readDouble();
            double combatCritDamageCap = buf.readDouble();
            double combatDodgeChanceCap = buf.readDouble();
            // 场域屏障可见性：消费端是纯客户端渲染（WirelessFieldRenderer），
            // COMMON 配置不会自动下发，必须随包同步服务端权威值
            boolean wirelessFieldOwnerOnly = buf.readBoolean();
            Minecraft.getInstance().execute(() -> {
                ModConfig.maxRejection = maxRejection;
                ModConfig.reactorTempMax = reactorTempMax;
                ModConfig.reactorTempWarn = reactorTempWarn;
                ModConfig.reactorTempOptMin = reactorTempOptMin;
                ModConfig.reactorTempOptMax = reactorTempOptMax;
                ModConfig.fusionTempTrip = fusionTempTrip;
                ModConfig.fusionTempOptMin = fusionTempOptMin;
                ModConfig.fusionTempOptMax = fusionTempOptMax;
                ModConfig.aggregatorProcessTicks = aggregatorProcessTicks;
                ModConfig.fillerProcessTicks = fillerProcessTicks;
                ModConfig.cultivatorUpgradeSuccess = cultivatorUpgradeSuccess;
                ModConfig.cultivatorPurifySuccess = cultivatorPurifySuccess;
                ModConfig.surgeryImplantLifeCost = surgeryImplantLifeCost;
                ModConfig.surgeryExtractLifeCost = surgeryExtractLifeCost;
                ModConfig.surgeryImplantSolidCost = surgeryImplantSolidCost;
                ModConfig.surgeryExtractSolidCost = surgeryExtractSolidCost;
                ModConfig.lifeBreederLifeCost = lifeBreederLifeCost;
                ModConfig.lifeBreederCrystalCost = lifeBreederCrystalCost;
                ModConfig.traitReforgerLifeCost = traitReforgerLifeCost;
                ModConfig.transgeneFactoryLifeCost = transgeneFactoryLifeCost;
                ModConfig.equipmentForgerEnergyPerForge = equipmentForgerEnergyPerForge;
                ModConfig.cultivatorPurifyEnergy = cultivatorPurifyEnergy;
                ModConfig.cultivatorPurifySolid = cultivatorPurifySolid;
                ModConfig.cultivatorPurifyTicks = cultivatorPurifyTicks;
                ModConfig.cultivatorPurifyGain = cultivatorPurifyGain;
                ModConfig.cultivatorLifeCapacity = cultivatorLifeCapacity;
                ModConfig.cultivatorUpgradeEnergy = cultivatorUpgradeEnergy;
                ModConfig.cultivatorUpgradeSolid = cultivatorUpgradeSolid;
                ModConfig.cultivatorUpgradeTicks = cultivatorUpgradeTicks;
                ModConfig.cultivatorUpgradeCompatBonus = cultivatorUpgradeCompatBonus;
                ModConfig.mechProcessChishiBase = mechProcessChishiBase;
                ModConfig.mechProcessLifeBase = mechProcessLifeBase;
                ModConfig.mechProcessTicksBase = mechProcessTicksBase;
                ModConfig.mechProcessPartFactor = mechProcessPartFactor;
                ModConfig.mechProcessMaterialCount = mechProcessMaterialCount;
                ModConfig.mechTemplateChishiCost = mechTemplateChishiCost;
                ModConfig.mechTemplateLifeCost = mechTemplateLifeCost;
                ModConfig.mechTemplateTicks = mechTemplateTicks;
                ModConfig.mechAssemblyChishiCost = mechAssemblyChishiCost;
                ModConfig.mechAssemblyLifeCost = mechAssemblyLifeCost;
                ModConfig.mechAssemblyTicks = mechAssemblyTicks;
                ModConfig.mechanicalChishiCapacity = mechanicalChishiCapacity;
                ModConfig.mechanicalLifeCapacity = mechanicalLifeCapacity;
                ModConfig.mechBodyHealthScale = mechBodyHealthScale;
                ModConfig.mechBodyAttackScale = mechBodyAttackScale;
                ModConfig.mechBodyAttackSpeedScale = mechBodyAttackSpeedScale;
                ModConfig.mechBodyMovementSpeedScale = mechBodyMovementSpeedScale;
                ModConfig.mechBodyArmorScale = mechBodyArmorScale;
                ModConfig.mechBodyCritChanceScale = mechBodyCritChanceScale;
                ModConfig.mechBodyCritDamageScale = mechBodyCritDamageScale;
                ModConfig.mechBodyRangeScale = mechBodyRangeScale;
                ModConfig.mechBodyDodgeScale = mechBodyDodgeScale;
                ModConfig.combatCritEnabled = combatCritEnabled;
                ModConfig.combatDodgeEnabled = combatDodgeEnabled;
                ModConfig.combatCritChanceCap = combatCritChanceCap;
                ModConfig.combatCritDamageCap = combatCritDamageCap;
                ModConfig.combatDodgeChanceCap = combatDodgeChanceCap;
                ModConfig.wirelessFieldOwnerOnly = wirelessFieldOwnerOnly;
            });
        });
    }

    /** 服务端：推送当前配置值给指定玩家（登录时 / 配置重载后全服） */
    public static void sendToPlayer(ServerPlayer player) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(ModConfig.maxRejection);
        buf.writeVarInt(ModConfig.reactorTempMax);
        buf.writeVarInt(ModConfig.reactorTempWarn);
        buf.writeVarInt(ModConfig.reactorTempOptMin);
        buf.writeVarInt(ModConfig.reactorTempOptMax);
        buf.writeVarInt(ModConfig.fusionTempTrip);
        buf.writeVarInt(ModConfig.fusionTempOptMin);
        buf.writeVarInt(ModConfig.fusionTempOptMax);
        buf.writeVarInt(ModConfig.aggregatorProcessTicks);
        buf.writeVarInt(ModConfig.fillerProcessTicks);
        writeArray(buf, ModConfig.cultivatorUpgradeSuccess);
        writeArray(buf, ModConfig.cultivatorPurifySuccess);
        buf.writeVarLong(ModConfig.surgeryImplantLifeCost);
        buf.writeVarLong(ModConfig.surgeryExtractLifeCost);
        buf.writeVarInt(ModConfig.surgeryImplantSolidCost);
        buf.writeVarInt(ModConfig.surgeryExtractSolidCost);
        buf.writeVarLong(ModConfig.lifeBreederLifeCost);
        buf.writeVarInt(ModConfig.lifeBreederCrystalCost);
        buf.writeVarLong(ModConfig.traitReforgerLifeCost);
        buf.writeVarLong(ModConfig.transgeneFactoryLifeCost);
        buf.writeVarLong(ModConfig.equipmentForgerEnergyPerForge);
        writeLongArray(buf, ModConfig.cultivatorPurifyEnergy);
        writeArray(buf, ModConfig.cultivatorPurifySolid);
        writeArray(buf, ModConfig.cultivatorPurifyTicks);
        buf.writeVarInt(ModConfig.cultivatorPurifyGain);
        buf.writeVarLong(ModConfig.cultivatorLifeCapacity);
        writeArray(buf, ModConfig.cultivatorUpgradeEnergy);
        writeArray(buf, ModConfig.cultivatorUpgradeSolid);
        writeArray(buf, ModConfig.cultivatorUpgradeTicks);
        buf.writeVarInt(ModConfig.cultivatorUpgradeCompatBonus);
        writeLongArray(buf, ModConfig.mechProcessChishiBase);
        writeLongArray(buf, ModConfig.mechProcessLifeBase);
        writeArray(buf, ModConfig.mechProcessTicksBase);
        writeArray(buf, ModConfig.mechProcessPartFactor);
        writeArray(buf, ModConfig.mechProcessMaterialCount);
        buf.writeVarLong(ModConfig.mechTemplateChishiCost);
        buf.writeVarLong(ModConfig.mechTemplateLifeCost);
        buf.writeVarInt(ModConfig.mechTemplateTicks);
        buf.writeVarLong(ModConfig.mechAssemblyChishiCost);
        buf.writeVarLong(ModConfig.mechAssemblyLifeCost);
        buf.writeVarInt(ModConfig.mechAssemblyTicks);
        buf.writeVarLong(ModConfig.mechanicalChishiCapacity);
        buf.writeVarLong(ModConfig.mechanicalLifeCapacity);
        buf.writeDouble(ModConfig.mechBodyHealthScale);
        buf.writeDouble(ModConfig.mechBodyAttackScale);
        buf.writeDouble(ModConfig.mechBodyAttackSpeedScale);
        buf.writeDouble(ModConfig.mechBodyMovementSpeedScale);
        buf.writeDouble(ModConfig.mechBodyArmorScale);
        buf.writeDouble(ModConfig.mechBodyCritChanceScale);
        buf.writeDouble(ModConfig.mechBodyCritDamageScale);
        buf.writeDouble(ModConfig.mechBodyRangeScale);
        buf.writeDouble(ModConfig.mechBodyDodgeScale);
        buf.writeBoolean(ModConfig.combatCritEnabled);
        buf.writeBoolean(ModConfig.combatDodgeEnabled);
        buf.writeDouble(ModConfig.combatCritChanceCap);
        buf.writeDouble(ModConfig.combatCritDamageCap);
        buf.writeDouble(ModConfig.combatDodgeChanceCap);
        // 与读端最后一个字段严格对称
        buf.writeBoolean(ModConfig.wirelessFieldOwnerOnly);
        NetworkManager.sendToPlayer(player, CHANNEL, buf);
    }

    private static void writeArray(FriendlyByteBuf buf, int[] arr) {
        buf.writeVarInt(arr.length);
        for (int v : arr) {
            buf.writeVarInt(v);
        }
    }

    /** 写入 long[] 数组（长度 + 逐元素 VarLong） */
    private static void writeLongArray(FriendlyByteBuf buf, long[] arr) {
        buf.writeVarInt(arr.length);
        for (long v : arr) {
            buf.writeVarLong(v);
        }
    }

    private static int[] readArray(FriendlyByteBuf buf) {
        int n = Math.max(0, Math.min(16, buf.readVarInt()));
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) {
            arr[i] = buf.readVarInt();
        }
        return arr;
    }

    /** 读取 long[] 数组（长度防溢出上限 16） */
    private static long[] readLongArray(FriendlyByteBuf buf) {
        int n = Math.max(0, Math.min(16, buf.readVarInt()));
        long[] arr = new long[n];
        for (int i = 0; i < n; i++) {
            arr[i] = buf.readVarLong();
        }
        return arr;
    }
}
