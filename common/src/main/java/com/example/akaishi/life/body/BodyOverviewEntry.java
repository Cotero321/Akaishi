package com.example.akaishi.life.body;

/**
 * 躯体总览单条属性加成（S2C 同步载荷项）：
 * attributeKey 为属性语言键（attribute.name.*，客户端可直接翻译），value 为当前实际生效净加成。
 * 数值由服务端（forge 属性修饰聚合）计算，与玩家属性面板完全一致。
 */
public record BodyOverviewEntry(String attributeKey, double value) {
}
