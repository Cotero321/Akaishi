package com.example.akaishi.item.curio;

/**
 * 幼崽之心（akaishi_socket_2）。
 * 攻击伤害抽取一成转为黄心；按节流刷新亢奋（攻速）与目标的减速、伤害增减等随机效果。
 */
public class AkaishiCubHeart extends AkaishiSocketCurioItem {

    private static final String[] EFFECT_KEYS = {
            "item.akaishi.curio.cub_heart.1",
            "item.akaishi.curio.cub_heart.2",
            "item.akaishi.curio.cub_heart.3",
            "item.akaishi.curio.cub_heart.4",
            "item.akaishi.curio.cub_heart.5",
    };

    public AkaishiCubHeart(Properties properties) {
        super(properties);
    }

    @Override
    public String socketId() {
        return "akaishi_socket_2";
    }

    @Override
    protected String whisperKey() {
        return "item.akaishi.curio.cub_heart.whisper";
    }

    @Override
    protected String[] effectKeys() {
        return EFFECT_KEYS;
    }
}
