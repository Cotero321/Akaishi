package com.example.akaishi.api.miniature;

import com.example.akaishi.wireless.TerminalSecurity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 可微缩终端契约：各族终端方块实体实现它，即可被「坍缩」通用流程收成单方块。
 * <p>
 * 通用流程（{@link com.example.akaishi.miniature.MiniatureCollapse}）只依赖本接口：
 * 判主控权限 → 记结构快照 → 取族数据 → 消耗整套结构 → 产出微缩方块。
 * 因此新增终端族只要实现这几个方法，微缩机制零改动（OCP）；<b>非终端机器不实现本接口</b>，
 * 也就天然无法被微缩（用户口径：通用但仅限终端）。
 */
public interface IMiniaturizableTerminal {

    /** 终端族 id（必须与 {@link MiniatureTerminalRegistry} 里注册的适配器 id 一致） */
    ResourceLocation miniatureTypeId();

    /** 终端唯一 ID（微缩后由微缩方块继承，端口绑定不失效） */
    UUID terminalId();

    /** 安全表（主控权限判定真源） */
    TerminalSecurity security();

    /** 结构箱体最小角；<b>未成型返回 null</b>（未成型不允许微缩） */
    @Nullable
    BlockPos structureMin();

    /** 结构箱体最大角；未成型返回 null */
    @Nullable
    BlockPos structureMax();

    /**
     * 导出本族数据（微缩方块读回的 payload）。
     * <p>
     * 只写族自有内容：终端 ID 与类型由通用层写，结构快照也由通用层追加，
     * 族不需要（也不该）自己处理这两项。
     * <p>
     * <b>不变量</b>：凡是被本方法写进 payload 的方块（贴装储存单元 / 贴身储能单元等），
     * 其方块位置必须一并出现在 {@link #extraConsumedBlocks()} 里 —— 否则数据会在
     * "payload 一份 + 世界方块一份"之间变成复制。
     */
    CompoundTag captureMiniature();

    /**
     * 除结构箱体之外、<b>必须一并消耗</b>的方块位置（贴装件）。
     * <p>
     * 这些方块不在 5×5×5 箱体内（贴装件允许装在外侧 1 格），若留下而数据已进 payload，
     * 就是同一份数据的第二份 ⇒ 玩家刷物品/刷能量。默认空（无贴装件的族）。
     */
    default List<BlockPos> extraConsumedBlocks() {
        return List.of();
    }
}
