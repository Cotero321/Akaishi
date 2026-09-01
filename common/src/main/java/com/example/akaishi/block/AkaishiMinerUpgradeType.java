package com.example.akaishi.block;

/**
 * 矿机升级模块类型（以方块形式安装在矿机结构的升级框架位置上）。
 * 速度：提高挖矿速率（+12.5%/个，上限 10）；时运：增加产物数量（+1/个，上限 4）；
 * 储能：提高控制器能量容量（+50%/个，上限 10）。
 */
public enum AkaishiMinerUpgradeType {

    SPEED("speed"),
    FORTUNE("fortune"),
    STORAGE("storage");

    /** 注册 id / 语言键后缀（akaishi_miner_<suffix>_upgrade_block） */
    public final String suffix;

    AkaishiMinerUpgradeType(String suffix) {
        this.suffix = suffix;
    }
}
