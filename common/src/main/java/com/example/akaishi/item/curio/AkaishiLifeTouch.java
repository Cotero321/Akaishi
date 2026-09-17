package com.example.akaishi.item.curio;

/**
 * 生命之触（akaishi_socket_1）。
 * 攻击距离 +2；命中可连打复刻上一击伤害；命中伴随自伤与饥饱的随机波动。
 */
public class AkaishiLifeTouch extends AkaishiSocketCurioItem {

    private static final String[] EFFECT_KEYS = {
            "item.akaishi.curio.life_touch.1",
            "item.akaishi.curio.life_touch.2",
            "item.akaishi.curio.life_touch.3",
            "item.akaishi.curio.life_touch.4",
    };

    public AkaishiLifeTouch(Properties properties) {
        super(properties);
    }

    @Override
    public String socketId() {
        return "akaishi_socket_1";
    }

    @Override
    protected String whisperKey() {
        return "item.akaishi.curio.life_touch.whisper";
    }

    @Override
    protected String[] effectKeys() {
        return EFFECT_KEYS;
    }
}
