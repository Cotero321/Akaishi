package com.example.akaishi.life.body;

/**
 * 躯体总览·被动条目：被动 id + 当前生效来源数（跨器官叠加，≥2 表示强度升级）。
 */
public record BodyPassiveEntry(String passiveId, int count) {
}
