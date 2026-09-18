package com.example.akaishi.block;

/**
 * 微缩矩阵终端升级组件类型（<b>以方块形式装在矩阵结构内腔</b>，控制器扫描内腔计数生效）。
 * <p>
 * 与无线终端族的内腔组件（跨维 / 区块加载 / 范围 / 损耗抑制）同范式：纯结构件、无方块实体、
 * 无自身界面；同类方块装得越多等级越高（上限见 {@link #maxCount}）。
 * <p>
 * {@code id} 同时用于注册 id（{@code akaishi_mini_matrix_upgrade_<id>}）与资源文件名，
 * <b>改名即换存档</b>，不可随意调整。
 */
public enum AkaishiMiniMatrixUpgradeType {

    /** ①无线能源操控组件：操控场域网络区域内（装了「无线接收升级」的）机器，1 块生效 */
    CONTROL("control", 1),
    /** ②无线场域升级：展开透明蓝光场域，每块 +1 区块半径，最高 3 块（半径 3 区块） */
    FIELD("field", 3),
    /** ③无线拓展升级：专用网络节点附加场域（半径 1 区块，需区块加载），最高 3 个节点 */
    EXTEND("extend", 3),
    /** ④无线加工升级：终端页列出可合成物品，按配方调度加工（消耗时间 / 赤能源 / IP） */
    CRAFT("craft", 1),
    /** ⑤无线场域联动升级：赤能源 / 生命能源 / IP 直供在场机械（IP 仅限加工） */
    LINK("link", 1);

    private final String id;
    private final int maxCount;

    AkaishiMiniMatrixUpgradeType(String id, int maxCount) {
        this.id = id;
        this.maxCount = maxCount;
    }

    /** 注册 id / 资源文件名后缀 */
    public String id() {
        return id;
    }

    /** 注册 id（方块与物品同名） */
    public String blockId() {
        return "akaishi_mini_matrix_upgrade_" + id;
    }

    /** 生效所需的最多块数（即等级上限） */
    public int maxCount() {
        return maxCount;
    }

    /** 方块名语言键 */
    public String nameKey() {
        return "block.akaishi." + blockId();
    }

    /** 悬浮说明语言键 */
    public String hintKey() {
        return "gui.akaishi.matrix.upgrade." + id + ".hint";
    }
}
