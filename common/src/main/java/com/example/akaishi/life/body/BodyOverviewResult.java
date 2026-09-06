package com.example.akaishi.life.body;

import java.util.List;

/**
 * 躯体总览聚合载荷：属性净加成 + 被动叠加计数（服务端随检查仪同步包一次性下发）。
 */
public record BodyOverviewResult(List<BodyOverviewEntry> attributes, List<BodyPassiveEntry> passives) {
}
