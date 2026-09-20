package com.example.akaishi.craft;

/**
 * 场域的「工序供给」契约：虚拟加工要跑机械工序时，向场域要一份供给快照。
 *
 * <p>由微缩矩阵终端实现（它是场域的持有者）；判定方只依赖本接口，
 * 因此菜单、任务、列表高亮三处能共用同一条准入口径（DIP）。
 */
public interface IProcessSupply {

    /**
     * 扫一次场域，返回工序供给快照。
     * <p><b>同一批判定请复用同一份快照</b>（目录构建会逐条物品判定，逐条重扫会被放大上千倍）。
     */
    ProcessCoverage processCoverage();

    /**
     * 场域能否把能量送到机台上。
     * <p><b>为什么它属于"供给"</b>：真机加工里机台必须真的转起来，光有工序不够 —— 自研机台靠场域无线直供
     * 取电（终端每 40 tick 推一次，前提是「操控」+「联动」两件升级在位且场域半径 &gt; 0）。
     * 少任何一条，机台节点就会一直等到超时，于是出现"列表亮着、点下去干等"。
     * <p>判定条件必须与调用点<b>逐条同源</b>（见 {@code AkaishiMiniMatrixTerminalBlockEntity#pushWirelessEnergy}）。
     */
    boolean canPowerMachines();
}
