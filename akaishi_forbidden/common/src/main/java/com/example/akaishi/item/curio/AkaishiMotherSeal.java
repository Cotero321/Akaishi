package com.example.akaishi.item.curio;

/**
 * 母神之印（akaishi_socket_3）。
 * 持有「不可名状」时随等级获得四项增益；四类伤害减半；定期自施不可名状；禁止食用素食。
 */
public class AkaishiMotherSeal extends AkaishiSocketCurioItem {

    private static final String[] EFFECT_KEYS = {
            "item.akaishi.curio.mother_seal.1",
            "item.akaishi.curio.mother_seal.2",
            "item.akaishi.curio.mother_seal.3",
            "item.akaishi.curio.mother_seal.4",
    };

    public AkaishiMotherSeal(Properties properties) {
        super(properties);
    }

    @Override
    public String socketId() {
        return "akaishi_socket_3";
    }

    @Override
    protected String whisperKey() {
        return "item.akaishi.curio.mother_seal.whisper";
    }

    @Override
    protected String[] effectKeys() {
        return EFFECT_KEYS;
    }
}
