package com.example.akaishi.miniature;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.miniature.MiniatureTerminalAdapter;
import com.example.akaishi.api.miniature.MiniatureTerminalState;
import com.example.akaishi.api.storage.IWirelessTerminalHost;
import com.example.akaishi.block.entity.MiniatureTerminalBlockEntity;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.menu.AkaishiLifeWirelessTerminalMenu;
import com.example.akaishi.menu.AkaishiWirelessTerminalMenu;
import com.example.akaishi.wireless.WirelessFamily;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import org.jetbrains.annotations.Nullable;

/**
 * 无线能量终端族的微缩适配器（赤能源 / 生命能量）。
 * <p>
 * 两族内部结构、数据与网络语义几乎一致，只差「族 + 能量类型 + 显示名 + 菜单类」，故<b>一个参数化类 +
 * 两个实例</b>（{@link #CHISHI} / {@link #LIFE}），不复制两份代码；状态类也只有一个
 * （{@link WirelessTerminalMiniatureState}）。
 * <p>
 * <b>界面零成本复用</b>：原终端菜单只依赖 {@link IWirelessTerminalHost}（数据槽 + terminalId + 安全表），
 * 两个终端方块实体与微缩状态共同实现该接口，因此微缩件直接开原菜单 —— 玩家感知不到形态变化，
 * 也不存在"两套界面各自演化"的维护负担。
 */
public final class WirelessTerminalMiniatureAdapter implements MiniatureTerminalAdapter {

    /** 赤能源族微缩终端类型 id */
    public static final ResourceLocation CHISHI_ID =
            new ResourceLocation(AkaishiMod.MOD_ID, "chishi_wireless_terminal");
    /** 生命能量族微缩终端类型 id */
    public static final ResourceLocation LIFE_ID =
            new ResourceLocation(AkaishiMod.MOD_ID, "life_wireless_terminal");

    /** 赤能源族实例（坍缩侧 {@code miniatureTypeId()} 也引用 {@link #CHISHI_ID}） */
    public static final WirelessTerminalMiniatureAdapter CHISHI = new WirelessTerminalMiniatureAdapter(
            CHISHI_ID, WirelessFamily.CHISHI, AkaishiEnergyType.INSTANCE, "block.akaishi.akaishi_wireless_terminal");
    /** 生命能量族实例 */
    public static final WirelessTerminalMiniatureAdapter LIFE = new WirelessTerminalMiniatureAdapter(
            LIFE_ID, WirelessFamily.LIFE, LifeEnergyType.INSTANCE, "block.akaishi.akaishi_life_wireless_terminal");

    private final ResourceLocation typeId;
    private final WirelessFamily family;
    private final IEnergyType energyType;
    /** 显示名复用原终端方块名（不新增语言键） */
    private final String displayKey;

    private WirelessTerminalMiniatureAdapter(ResourceLocation typeId, WirelessFamily family, IEnergyType energyType,
            String displayKey) {
        this.typeId = typeId;
        this.family = family;
        this.energyType = energyType;
        this.displayKey = displayKey;
    }

    @Override
    public ResourceLocation typeId() {
        return typeId;
    }

    @Override
    public MiniatureTerminalState createState(MiniatureTerminalBlockEntity be, CompoundTag payload) {
        return new WirelessTerminalMiniatureState(be, payload, family, energyType);
    }

    @Override
    public Component displayName(CompoundTag payload) {
        return Component.translatable(displayKey);
    }

    /**
     * 复用原终端四页菜单：宿主取微缩状态自身（它实现 {@link IWirelessTerminalHost}）。
     * 状态缺失（空壳）时返回 null —— 与 {@link #hasMenu()} 的口径一致：有界面才返回 true，
     * 方块侧仅在 hasMenu 成立时开界面，故正常路径不会拿到 null。
     */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, MiniatureTerminalBlockEntity be) {
        IWirelessTerminalHost host = be.state() instanceof IWirelessTerminalHost h ? h : null;
        if (host == null) {
            return null;
        }
        // 两族菜单类不同（各含自己的 MenuType），但依赖面完全一致 ⇒ 只在此处分派，不复制菜单
        return family == WirelessFamily.LIFE
                ? new AkaishiLifeWirelessTerminalMenu(id, inv, host)
                : new AkaishiWirelessTerminalMenu(id, inv, host);
    }

    @Override
    public boolean hasMenu() {
        return true;
    }
}
