package com.example.akaishi.craft;

import com.example.akaishi.craft.thirdparty.ThirdPartyProcesses;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 场域「工序供给」快照：<b>一次扫描</b>的结果，之后可反复判定多张订单。
 *
 * <p>为什么要快照而不是每次现扫：加工目录构建会逐条物品调用 {@code plan}，
 * 若每条都重扫一遍场域（最多 49 区块的方块实体）开销会被放大上千倍。
 *
 * <p><b>两级准入口径</b>（用户拍板）：
 * <ul>
 *   <li><b>精确</b>：配方类型在第三方认可表里声明过 ⇒ 必须场域里真有声明方块之一（经接入器认可）；</li>
 *   <li><b>粗粒度</b>：未声明的第三方配方类型 ⇒ 只要场域里有<b>任意一台</b>已认可的第三方机器即可。</li>
 * </ul>
 *
 * <p><b>机台明细</b>（{@link #machines}）：每族有哪些机台、各自装了什么升级 ——
 * 虚拟加工的能耗与耗时按这个来算（见 {@link MachineProcessEnergy} 与 {@link MachineScheduler}）。
 *
 * @param coveredKinds          场域已覆盖的工序族（自家机台自述的 + 接入器精确声明的）
 * @param thirdPartyRecognized  场域内是否存在"已认可到第三方机器"的接入器（粗粒度准入用）
 * @param machines              工序族 → 场域内该族机台的升级配置（自家机台才有；第三方机器读不到我们的升级件）
 * @param thirdPartyMachines    场域内已认可的第三方机器台数（粗粒度档下作为"几份算力"）
 */
public record ProcessCoverage(Set<RecipeType<?>> coveredKinds, boolean thirdPartyRecognized,
                              Map<RecipeType<?>, List<MachineSpec>> machines, int thirdPartyMachines) {

    /** 空快照：无场域 / 未成型时用它，等价于"什么都跑不了" */
    public static final ProcessCoverage EMPTY = new ProcessCoverage(Set.of(), false, Map.of(), 0);

    /** 本单要跑的这些工序族，现在能不能全部顶下来 */
    public boolean covers(Set<RecipeType<?>> machineKinds) {
        if (machineKinds.isEmpty()) {
            return true;
        }
        for (RecipeType<?> kind : machineKinds) {
            if (coveredKinds.contains(kind)) {
                continue; // 自家机台覆盖，或第三方族被精确声明且那台机器就在场
            }
            if (!MachineProcessEnergy.isThirdPartyProcess(kind)) {
                return false; // 既不是自家机台族、也不是第三方族：没有任何来源
            }
            if (ThirdPartyProcesses.isDeclared(kind)) {
                // 声明过却不在场：<b>不许退化成粗粒度</b>，否则"写声明"反而比不写更松
                return false;
            }
            if (!thirdPartyRecognized) {
                return false; // 未声明的第三方族：场域里至少要有一台已认可的第三方机器
            }
        }
        return true;
    }

    /**
     * 某工序族可用的机台配置；没有明细时返回"一台无升级机台"。
     * <p>回退到基准而不是空表：第三方工序与列表预筛（没有场域信息）全靠它给出可算的成本。
     * <p><b>第三方族（未精确声明）按场域里已认可的台数给多份算力</b>：
     * 玩家摆了三台三方机器就该按三台分担，只算一台会明显高估耗时。
     * （读不到它们的升级件，故每份都是无升级的 {@link MachineSpec#BASE}。）
     */
    public List<MachineSpec> machinesOf(RecipeType<?> kind) {
        List<MachineSpec> found = machines.get(kind);
        if (found != null && !found.isEmpty()) {
            return found;
        }
        if (MachineProcessEnergy.isThirdPartyProcess(kind) && thirdPartyMachines > 1) {
            return Collections.nCopies(thirdPartyMachines, MachineSpec.BASE);
        }
        return List.of(MachineSpec.BASE);
    }
}
