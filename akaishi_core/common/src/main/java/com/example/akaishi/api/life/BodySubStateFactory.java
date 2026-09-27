package com.example.akaishi.api.life;

import net.minecraft.nbt.CompoundTag;

import java.util.function.Supplier;

/**
 * 躯体「可选子系统状态段」的创建入口（P3b 倒置层，见 {@link IBodySubState}）。
 *
 * <p>本体在构造玩家躯体 capability 时经 {@link #create()} 取得一个状态段实例；
 * 具体实现由内容模块在初始化阶段经 {@link #install} 注入。未注入时使用空实现，
 * 保证本体在没有该可选模块时依旧可运行（读写均为空操作）。
 */
public final class BodySubStateFactory {

    private static volatile Supplier<IBodySubState> factory = EmptySubState::new;

    private BodySubStateFactory() {
    }

    /** 由内容模块注入实现（null 忽略，保持上一次有效值） */
    public static void install(Supplier<IBodySubState> supplier) {
        if (supplier != null) {
            factory = supplier;
        }
    }

    /** 创建一个状态段实例（每次调用返回新实例） */
    public static IBodySubState create() {
        return factory.get();
    }

    /** 未安装可选模块时的空实现：不读不写，绝不改变存档 */
    static final class EmptySubState implements IBodySubState {

        @Override
        public CompoundTag save() {
            return new CompoundTag();
        }

        @Override
        public void load(CompoundTag tag) {
            // 无实现：不消费任何字段
        }

        @Override
        public void clearTemporaries() {
            // 无实现
        }
    }
}
