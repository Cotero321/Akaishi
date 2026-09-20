package com.example.akaishi.craft.exec;

import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.api.recipe.IMachineProcessKind;
import com.example.akaishi.block.entity.AkaishiWirelessAccessAdapterBlockEntity;
import com.example.akaishi.craft.MachineProcessEnergy;
import com.example.akaishi.craft.VirtualCraftPlanner;
import com.example.akaishi.craft.WirelessMachineScanner;
import com.example.akaishi.craft.thirdparty.ThirdPartyProcesses;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.wireless.WirelessFieldManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 场域内「工序族 → 可用机台端点」的查找（真机加工的选机处）。
 *
 * <p><b>与 {@code ProcessCoverage} 的分工</b>：那个回答"有没有人能干这些活"（<b>准入</b>，决定下单按钮亮不亮），
 * 本类回答"具体是哪几台、怎么投料"（<b>执行</b>）。两者用同一套条件（自研：
 * {@code IUpgradeableMachine + hasWirelessReceiver + IMachineProcessKind}；第三方：接入器认可 + 认可表档位），
 * 否则会出现"准入放行、执行时找不到机器"的死局。
 *
 * <p><b>自研优先</b>：同一工序族若既有自研机台又有第三方机器，先用自研的 —— 自研的能耗/耗时我们算得准，
 * 第三方只能等。
 */
public final class EndpointFinder {

    private EndpointFinder() {
    }

    /**
     * 场域内能跑该工序族的全部端点（自研在前）。
     *
     * @param distinctInputs 本节点需要的输入种类数：机台输入槽位不够装下这么多不同物品的直接排除，
     *                       否则会出现"投不进去、一直等到超时"的假死（比当场判"没机器"更难排查）
     * @return 空表 = 场域里没有能跑这道工序的机台
     */
    public static List<MachineEndpoint> find(ServerLevel level, BlockPos center, int radiusChunks,
            @Nullable RecipeType<?> kind, int distinctInputs) {
        List<MachineEndpoint> own = new ArrayList<>();
        List<MachineEndpoint> third = new ArrayList<>();
        if (kind == null) {
            return own; // 免机台工序（原版工作台那类）由终端代劳，不需要机台
        }
        WirelessMachineScanner.forEachInField(level, center, radiusChunks, (pos, be) -> {
            if (!WirelessFieldManager.inField(level, pos)) {
                return;
            }
            if (be instanceof IUpgradeableMachine machine && machine.hasWirelessReceiver()
                    && be instanceof IMachineProcessKind self && self.processKind() == kind
                    && be instanceof IItemPipeDevice device) {
                if (device.getPipeInputSlots().length >= distinctInputs) {
                    own.add(new OwnMachineEndpoint(pos, device));
                }
                return;
            }
            if (be instanceof AkaishiWirelessAccessAdapterBlockEntity adapter && serves(adapter, kind)) {
                MachineEndpoint endpoint = AdapterMachineEndpoint.of(level, adapter);
                if (endpoint != null) {
                    third.add(endpoint);
                }
            }
        });
        if (!own.isEmpty()) {
            return own;
        }
        return third;
    }

    /**
     * 给一个执行节点选机台：优先挑"输入侧还放得下料"的那台。
     * <p>全满时返回第一台（让执行器去等，而不是误判成"没有机器"）—— 两种情况在界面上的含义不同：
     * 前者等一会儿就能继续，后者要等到超时并报"没有可用机台"。
     *
     * @return null = 场域里没有能跑这道工序的机台
     */
    @Nullable
    public static MachineEndpoint pick(ServerLevel level, BlockPos center, int radiusChunks,
            VirtualCraftPlanner.Step step) {
        List<MachineEndpoint> found = find(level, center, radiusChunks,
                MachineProcessEnergy.machineKind(step.recipe().recipe()), step.picks().size());
        if (found.isEmpty()) {
            return null;
        }
        for (MachineEndpoint endpoint : found) {
            boolean roomy = true;
            for (Map.Entry<Item, Long> input : step.inputs().entrySet()) {
                if (!endpoint.hasInputRoom(input.getKey())) {
                    roomy = false;
                    break;
                }
            }
            if (roomy) {
                return endpoint;
            }
        }
        return found.get(0);
    }

    /** 该接入器能否提供这道工序：精确档要求它是认可表里声明的那台；粗粒度档任意已认可的第三方机器都算 */
    private static boolean serves(AkaishiWirelessAccessAdapterBlockEntity adapter, RecipeType<?> kind) {
        if (!adapter.isRecognized() || !MachineProcessEnergy.isThirdPartyProcess(kind)) {
            return false;
        }
        return !ThirdPartyProcesses.isDeclared(kind) || adapter.providedProcesses().contains(kind);
    }
}
