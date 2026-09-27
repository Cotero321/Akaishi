package com.example.akaishi.api.life;

import net.minecraft.nbt.CompoundTag;

/**
 * 躯体 capability 承载的「可选子系统状态段」最小契约（P3b 倒置层）。
 *
 * <p><b>为什么需要它</b>：某些子系统（如禁忌模块的理智系统）把自身状态借住在
 * 本体的玩家躯体 capability 里，以复用其"落盘 + 死亡即时快照 + 重生/换维度克隆"链路。
 * 但禁忌模块不可被本体反向依赖，故把"状态段"抽象成这个只有存取语义的接口：
 * 本体只按段名原样保存/加载，不认识任何具体子系统的字段。
 *
 * <p>实现由对应内容模块提供并在其模块初始化时经 {@link BodySubStateFactory#install} 注册；
 * 未安装该模块时使用内置空实现（不动存档、不报错）。
 */
public interface IBodySubState {

    /** 序列化整段状态（本体把返回值原样写进躯体 NBT 的对应子段） */
    CompoundTag save();

    /** 反序列化整段状态（本体从躯体 NBT 的对应子段取出的值，缺段时为空气值） */
    void load(CompoundTag tag);

    /**
     * 清除"瞬态"标记：死亡克隆等场景下调用。
     * 无瞬态语义的实现留空即可。
     */
    void clearTemporaries();
}
