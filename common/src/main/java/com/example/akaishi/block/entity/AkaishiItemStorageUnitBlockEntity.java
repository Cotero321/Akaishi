package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.storage.IItemStorageUnit;
import com.example.akaishi.block.AkaishiItemStorageUnitBlock;
import com.example.akaishi.block.ItemStorageUnitTier;
import com.example.akaishi.menu.AkaishiItemStorageUnitMenu;
import com.example.akaishi.storage.ItemStorageUnitData;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 物品储存单元方块实体：以 IP 计量「占用量」的贴装式仓库（D5）。
 * <p>
 * <b>双轨记账</b>（D3）：物品本体（含 NBT）落容器，占用 IP 落入同槽位的账本，
 * 二者在唯一写入口内同步变更，因此「物品 ↔ 账本」永不脱节（D10 无偏差）。
 * <p>
 * <b>入账锁定</b>：折算只在存入瞬间执行一次，取出只扣账本值，价值表重载不漂移。
 * <p>
 * <b>被动存储</b>：不实现 {@code IItemPipeDevice} / 不暴露 {@code ITEM_HANDLER}，
 * 外部物流无法直写容器（否则会绕过账本）。物品进出仅由物品终端调用
 * {@link #insert} / {@link #extract} / {@link #setItemAt} 完成。
 * <p>
 * 数据与逻辑落在 {@link ItemStorageUnitData}（不依赖方块实体，便于微缩与复用），
 * 本类只做生命周期、菜单与持久化转发；数据经 {@link AkaishiItemStorageUnitBlock} 继承的打包链路
 * 随掉落物保留，容器内容与账本一律经 {@code saveAdditional} 交由数据对象落盘，
 * 禁止调用 {@code Containers#dropContents}（否则双份复制）。
 */
public class AkaishiItemStorageUnitBlockEntity extends BlockEntity
        implements IDataCarrier, IItemStorageUnit, ExtendedMenuProvider {

    /** 槽位数：单页大箱 54（6×9），与项目存储库单页口径一致 */
    public static final int SLOTS = 54;

    /** 单元数据本体（等阶 + 槽内容 + 账本），变更回调直连本方块实体的 setChanged */
    private final ItemStorageUnitData data;

    public AkaishiItemStorageUnitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ITEM_STORAGE_UNIT.get(), pos, state);
        // 构造期直接用入参 state 取阶（与 tier() 同口径），避免依赖基类方块状态缓存
        this.data = new ItemStorageUnitData(tierOf(state.getBlock()), SLOTS, this::setChanged);
    }

    /** 本机等阶（由方块决定；异常方块退回基础档，避免空指针） */
    private ItemStorageUnitTier tier() {
        return tierOf(getBlockState().getBlock());
    }

    /** 等阶解析：异常方块退回基础档 */
    private static ItemStorageUnitTier tierOf(Block block) {
        return block instanceof AkaishiItemStorageUnitBlock unit ? unit.getTier() : ItemStorageUnitTier.BASIC;
    }

    // ===== 契约（终端只读聚合）：一律转发数据本体 =====

    @Override
    public long getIpCapacity() {
        return data.getIpCapacity();
    }

    @Override
    public long getStoredIp() {
        return data.getStoredIp();
    }

    /** 剩余可用 IP（界面展示「容量还剩多少」，用户口径） */
    public long getRemainingIp() {
        return data.getRemainingIp();
    }

    /** 槽位数（只读视图用，不暴露容器本体，避免外部直写绕过账本） */
    @Override
    public int slots() {
        return data.slots();
    }

    /** 只读取槽（界面 / 终端浏览用） */
    @Override
    public ItemStack getItem(int slot) {
        return data.getItem(slot);
    }

    /** 单槽已占用 IP（只读，供物品库按占用排序，无需重查价值表） */
    @Override
    public long getSlotIp(int slot) {
        return data.getSlotIp(slot);
    }

    // ===== 唯一写入口（三法：insert / extract / setItemAt） =====

    @Override
    public int acceptable(ItemStack stack) {
        return data.acceptable(stack);
    }

    @Override
    public int insert(ItemStack stack) {
        return data.insert(stack);
    }

    @Override
    public ItemStack extract(int slot, int amount) {
        return data.extract(slot, amount);
    }

    public ItemStack setItemAt(int slot, ItemStack stack) {
        return data.setItemAt(slot, stack);
    }

    // ===== NBT 持久化（容器 + 账本同批落盘） =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        data.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // 等阶随方块，先同步再读内容：自愈按本阶容量口径折算
        data.setTier(tier());
        data.load(tag);
    }

    // ===== 菜单入口（ExtendedMenuProvider：右键打开只读视图，按钮位置经 saveExtraData 下发） =====

    @Override
    public Component getDisplayName() {
        // 直接取方块名：三阶单元的双语名随方块解析，无需在此分支
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiItemStorageUnitMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }
}
