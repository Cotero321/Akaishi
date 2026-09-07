package com.example.akaishi.life.body;

import com.example.akaishi.config.ModConfig;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
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
    /** 排斥值上限：配置 [rejection] maxRejection 覆盖内置 100（0 = 用内置） */
    public static int maxRejection() {
        return ModConfig.maxRejection > 0 ? ModConfig.maxRejection : MAX_REJECTION;
    }
    /** 最多可吸收的基因型数量（不同生物来源各一次） */
    public static final int GENE_CAPACITY = 4;

    /** NBT 键 */
    private static final String TAG_ORGANS = "organs";
    private static final String TAG_SLOT = "slot";
    private static final String TAG_STACK = "stack";
    private static final String TAG_REJECTION = "rejection";
    private static final String TAG_INITIALIZED = "initialized";
    private static final String TAG_GENE_BONUSES = "gene_bonuses";
    private static final String TAG_GENE_ENTITY = "entity";
    private static final String TAG_GENE_BONUS = "bonus";
    private static final String TAG_BT_ENTITY = "bt_entity";
    private static final String TAG_BT_EXTRA = "bt_extra";
    private static final String TAG_BT_PCT = "bt_pct";
    private static final String TAG_BT_UNTIL = "bt_until";

    /** 槽位 → 器官物品 */
    private final Map<BodySlot, ItemStack> organs = new EnumMap<>(BodySlot.class);
    /** 槽位 → 排斥值 */
    private final Map<BodySlot, Integer> rejection = new EnumMap<>(BodySlot.class);
    /** 已吸收基因强化：生物来源 → 适配加成（插入序即吸收顺序，上限 GENE_CAPACITY） */
    private final Map<String, Integer> geneBonuses = new LinkedHashMap<>();
    /** 突破激活：单一来源（该来源器官 30 分钟内额外适配 + 基础数值百分比强化；结束后可再次激活） */
    private String btEntity = "";
    private int btExtra;
    private int btPct;
    private long btUntil = -1L;
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
                organs.put(slot, AkaishiOrganItem.createNative(slot));
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
        // 移植即产生基础排斥（原生器官为 0），并重置排异中和剂清洗额度
        setRejection(slot, AkaishiOrganItem.getBaseRejection(organ));
        AkaishiOrganItem.setWashUsed(organs.get(slot), 0);
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
            player.sendSystemMessage(Component.translatable("message.akaishi.body.extract_damage",
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
        rejection.put(slot, Math.max(0, Math.min(maxRejection(), value)));
    }

    @Override
    public void addRejection(BodySlot slot, int amount) {
        setRejection(slot, getRejection(slot) + amount);
    }

    // ===== 基因强化 =====

    @Override
    public Map<String, Integer> getGeneBonuses() {
        return Collections.unmodifiableMap(geneBonuses);
    }

    @Override
    public boolean hasGene(String entityId) {
        return entityId != null && !entityId.isEmpty() && geneBonuses.containsKey(entityId);
    }

    @Override
    public int getGeneBonus(String entityId) {
        return geneBonuses.getOrDefault(entityId, 0);
    }

    @Override
    public boolean canAddGene() {
        return geneBonuses.size() < GENE_CAPACITY;
    }

    @Override
    public boolean addGene(String entityId, int bonus) {
        if (entityId == null || entityId.isEmpty() || hasGene(entityId) || !canAddGene()) {
            return false;
        }
        geneBonuses.put(entityId, Math.max(1, bonus));
        return true;
    }

    @Override
    public boolean removeGene(String entityId) {
        if (entityId == null || geneBonuses.remove(entityId) == null) {
            return false;
        }
        // 卸载的基因正被突破激活 → 突破一并结束（该来源加成整体撤销；重新吸收后可再次突破）
        if (entityId.equals(btEntity)) {
            endBreakthrough();
        }
        return true;
    }

    // ===== 突破强化 =====

    @Override
    public boolean hasActiveBreakthrough() {
        return !btEntity.isEmpty();
    }

    @Override
    public boolean isBreakthroughActive(String entityId) {
        return entityId != null && !entityId.isEmpty() && entityId.equals(btEntity);
    }

    @Override
    public String getBreakthroughEntity() {
        return btEntity;
    }

    @Override
    public int getBreakthroughExtra() {
        return btExtra;
    }

    @Override
    public int getBreakthroughPct() {
        return btPct;
    }

    @Override
    public long getBreakthroughUntil() {
        return btUntil;
    }

    @Override
    public boolean startBreakthrough(String entityId, int extra, int pct, long untilGameTime) {
        if (entityId == null || entityId.isEmpty() || hasActiveBreakthrough()) {
            return false;
        }
        btEntity = entityId;
        btExtra = Math.max(0, extra);
        btPct = Math.max(0, pct);
        btUntil = untilGameTime;
        return true;
    }

    @Override
    public boolean endBreakthrough() {
        if (btEntity.isEmpty()) {
            return false;
        }
        btEntity = "";
        btExtra = 0;
        btPct = 0;
        btUntil = -1L;
        return true;
    }

    @Override
    public boolean tickBreakthrough(long gameTime) {
        if (btEntity.isEmpty() || btUntil < 0L) {
            return false;
        }
        if (gameTime >= btUntil) {
            endBreakthrough();
            return true;
        }
        return false;
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
        // 基因强化列表（来源 → 加成，保持吸收顺序）
        ListTag geneList = new ListTag();
        for (Map.Entry<String, Integer> entry : geneBonuses.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(TAG_GENE_ENTITY, entry.getKey());
            entryTag.putInt(TAG_GENE_BONUS, entry.getValue());
            geneList.add(entryTag);
        }
        tag.put(TAG_GENE_BONUSES, geneList);
        // 突破激活（仅激活时写入）
        if (!btEntity.isEmpty()) {
            tag.putString(TAG_BT_ENTITY, btEntity);
            tag.putInt(TAG_BT_EXTRA, btExtra);
            tag.putInt(TAG_BT_PCT, btPct);
            tag.putLong(TAG_BT_UNTIL, btUntil);
        }
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
        // 基因强化（截断到上限，防止 NBT 篡改超载）
        geneBonuses.clear();
        ListTag geneList = tag.getList(TAG_GENE_BONUSES, Tag.TAG_COMPOUND);
        for (int i = 0; i < geneList.size() && geneBonuses.size() < GENE_CAPACITY; i++) {
            CompoundTag entryTag = geneList.getCompound(i);
            String entityId = entryTag.getString(TAG_GENE_ENTITY);
            if (!entityId.isEmpty()) {
                geneBonuses.put(entityId, entryTag.getInt(TAG_GENE_BONUS));
            }
        }
        // 突破状态：激活单条（键缺失时保持默认空值；可重复激活，无一生使用记录）
        btEntity = tag.getString(TAG_BT_ENTITY);
        if (!btEntity.isEmpty()) {
            btExtra = tag.getInt(TAG_BT_EXTRA);
            btPct = tag.getInt(TAG_BT_PCT);
            btUntil = tag.getLong(TAG_BT_UNTIL);
        } else {
            btExtra = 0;
            btPct = 0;
            btUntil = -1L;
        }
        initialized = tag.getBoolean(TAG_INITIALIZED);
        // 加载后立即补位（旧存档/新玩家），保证躯体始终满位
        ensureInitialized();
    }
}
