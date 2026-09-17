package com.example.akaishi.item.curio;

/**
 * 孕育之环（akaishi_socket_4）。
 * 受伤概率回血并换取攻速；肉类不减速且额外回血、饱食满仍可进食；饥饿伤害三倍且可致死。
 */
public class AkaishiFertilityRing extends AkaishiSocketCurioItem {

    private static final String[] EFFECT_KEYS = {
            "item.akaishi.curio.fertility_ring.1",
            "item.akaishi.curio.fertility_ring.2",
            "item.akaishi.curio.fertility_ring.3",
    };

    public AkaishiFertilityRing(Properties properties) {
        super(properties);
    }

    @Override
    public String socketId() {
        return "akaishi_socket_4";
    }

    @Override
    protected String whisperKey() {
        return "item.akaishi.curio.fertility_ring.whisper";
    }

    @Override
    protected String[] effectKeys() {
        return EFFECT_KEYS;
    }
}
