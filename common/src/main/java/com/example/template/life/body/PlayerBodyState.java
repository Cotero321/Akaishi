package com.example.template.life.body;

import com.example.template.life.organ.ChishiOrganItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * 玩家躯体状态默认实现：9 槽位器官 + 每部位排斥值。
 * - 初始状态：首次加载自动为 9 个槽位填充"原生器官"（适配度 100，无效果），
 *   保证躯体始终满位——空槽会触发生命上限惩罚。
 * - 移植：槽位必须为空，成功即占用，并按器官品质写入初始排斥值。
 * - 摘除：立即对玩家造成无视护甲的固定伤害（创造模式豁免），排斥值随之清零。
 * - 排斥值：0-100 钳制，由基因系统在移植/装配时累加；达到 100 该器官失效。
 */
public class PlayerBodyState implements IPlayerBodyState {

    /** 排斥值上限（达到上限器官失效） */
    public static final int MAX_REJECTION = 100;

    /** NBT 键 */
    private static final String TAG_ORGANS = "organs";
    private static final String TAG_SLOT = "slot";
    private static final String TAG_STACK = "stack";
    private static final String TAG_REJECTION = "rejection";
    private static final String TAG_INITIALIZED = "initialized";

    /** 槽位 → 器官物品 */
    private final Map<BodySlot, ItemStack> organs = new EnumMap<>(BodySlot.class);
    /** 槽位 → 排斥值 */
    private final Map<BodySlot, Integer> rejection = new EnumMap<>(BodySlot.class);
    /** 是否已完成原生器官填充（旧存档无此标记时自动补位） */
    private boolean initialized;

    /** 首次访问前确保 9 槽位已填充原生器官（新玩家/旧存档自动补位） */
    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        for (BodySlot slot : BodySlot.values()) {
            if (!organs.containsKey(slot) || organs.get(slot).isEmpty()) {
                organs.put(slot, ChishiOrganItem.createNative(slot));
            }
        }
    }

    @Override
    public ItemStack getOrgan(BodySlot slot) {
        ensureInitialized();
        return organs.getOrDefault(slot, ItemStack.EMPTY);
    }

    @Override
    public boolean isOccupied(BodySlot slot) {
        return !getOrgan(slot).isEmpty();
    }

    @Override
    public boolean implantOrgan(BodySlot slot, ItemStack organ) {
        // 槽位被占用时禁止直接覆盖，必须先摘除（承受代价）
        if (isOccupied(slot) || organ == null || organ.isEmpty()) {
            return false;
        }
        organs.put(slot, organ.copy());
        // 移植即产生基础排斥（原生器官为 0）
        setRejection(slot, ChishiOrganItem.getBaseRejection(organ));
        return true;
    }

    @Override
    public ItemStack extractOrgan(Player player, BodySlot slot) {
        ensureInitialized();
        ItemStack removed = organs.remove(slot);
        if (removed == null || removed.isEmpty()) {
            return ItemStack.EMPTY;
        }
        // 摘除后排斥清零，槽位恢复为空（触发空槽惩罚）
        rejection.remove(slot);
        // 摘除原部件造成生命值损失（仅服务端结算，创造模式豁免）
        if (player != null && !player.isCreative() && !player.isSpectator()) {
            if (!player.level().isClientSide) {
                player.hurt(player.damageSources().magic(), slot.getExtractDamage());
            }
            player.sendSystemMessage(Component.translatable("message.template_mod.body.extract_damage",
                    Component.translatable(slot.getNameKey())));
        }
        return removed;
    }

    @Override
    public int getRejection(BodySlot slot) {
        return rejection.getOrDefault(slot, 0);
    }

    @Override
    public void setRejection(BodySlot slot, int value) {
        rejection.put(slot, Math.max(0, Math.min(MAX_REJECTION, value)));
    }

    @Override
    public void addRejection(BodySlot slot, int amount) {
        setRejection(slot, getRejection(slot) + amount);
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        // 器官列表
        ListTag organList = new ListTag();
        for (Map.Entry<BodySlot, ItemStack> entry : organs.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(TAG_SLOT, entry.getKey().getId());
            entryTag.put(TAG_STACK, entry.getValue().save(new CompoundTag()));
            organList.add(entryTag);
        }
        tag.put(TAG_ORGANS, organList);
        // 排斥值列表
        ListTag rejectionList = new ListTag();
        for (Map.Entry<BodySlot, Integer> entry : rejection.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(TAG_SLOT, entry.getKey().getId());
            entryTag.putInt(TAG_REJECTION, entry.getValue());
            rejectionList.add(entryTag);
        }
        tag.put(TAG_REJECTION, rejectionList);
        tag.putBoolean(TAG_INITIALIZED, initialized);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        organs.clear();
        rejection.clear();
        ListTag organList = tag.getList(TAG_ORGANS, Tag.TAG_COMPOUND);
        for (int i = 0; i < organList.size(); i++) {
            CompoundTag entryTag = organList.getCompound(i);
            BodySlot slot = BodySlot.byId(entryTag.getString(TAG_SLOT));
            ItemStack stack = ItemStack.of(entryTag.getCompound(TAG_STACK));
            if (slot != null && !stack.isEmpty()) {
                organs.put(slot, stack);
            }
        }
        ListTag rejectionList = tag.getList(TAG_REJECTION, Tag.TAG_COMPOUND);
        for (int i = 0; i < rejectionList.size(); i++) {
            CompoundTag entryTag = rejectionList.getCompound(i);
            BodySlot slot = BodySlot.byId(entryTag.getString(TAG_SLOT));
            if (slot != null) {
                rejection.put(slot, entryTag.getInt(TAG_REJECTION));
            }
        }
        initialized = tag.getBoolean(TAG_INITIALIZED);
        // 加载后立即补位（旧存档/新玩家），保证躯体始终满位
        ensureInitialized();
    }
}
