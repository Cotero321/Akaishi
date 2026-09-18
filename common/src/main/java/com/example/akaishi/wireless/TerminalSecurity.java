package com.example.akaishi.wireless;

import com.example.akaishi.api.security.AkaishiSecurityPermission;
import com.example.akaishi.item.AkaishiWirelessIdentityCardItem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 终端安全状态：归属者 + 权限表（身份 → 权限位）+ 默认权限条目，口径对齐 AE2 安全终端。
 * <p>
 * 数据模型的三个角色：
 * <ul>
 *   <li><b>身份卡</b>（{@link AkaishiWirelessIdentityCardItem}）= 权限载体：卡上带绑定的玩家身份与权限位；
 *       在安全页「登记」即把卡上的身份与权限位写进权限表（未绑定身份的卡 ⇒ 默认权限条目）；</li>
 *   <li><b>本类</b> = 终端侧权威权限表（NBT 持久化），三个终端（赤能源无线、生命无线、物品终端）各持一份；</li>
 *   <li><b>{@link WirelessNetworkManager}</b> = 权限表镜像：给"手上没有卡的入口"（无线口、便携终端）
 *       与客户端 UI 查询用，镜像由终端推送（{@link #pushTo(UUID)}）。</li>
 * </ul>
 * 判定规则（{@link #allows}）是唯一真源，本地判定与镜像判定都走它：未启用 ⇒ 放行 →
 * 归属者 ⇒ 放行 → 命中条目 ⇒ 按位判 → 未命中 ⇒ 继承默认条目 → 连默认条目都没有 ⇒ 放行。
 */
public final class TerminalSecurity {

    /** NBT 根键（寄生在终端 BE 的根 tag 下） */
    private static final String TAG_ROOT = "Security";
    /** 权限表条数上限（安全页一屏 5 行身份 + 1 行默认条目，行高 9px 刚好落在背包之上） */
    public static final int MAX_ENTRIES = 5;

    private final Runnable onChanged;

    /** 变更计数：GUI 同步包据此判断是否需要重推（仅比较，不参与判定） */
    private int revision;

    /** 归属者（放置终端的玩家，恒全权限）；null = 未知 */
    private UUID owner;
    private String ownerName = "";
    /** 权限表：玩家 → 权限位掩码（LinkedHashMap 保序，GUI 列表稳定） */
    private final Map<UUID, Integer> perms = new LinkedHashMap<>();
    /** 权限表玩家名（仅显示用） */
    private final Map<UUID, String> names = new LinkedHashMap<>();
    /** 默认权限条目（未绑定身份的卡） */
    private boolean hasDefault;
    private int defaultPerms;

    public TerminalSecurity(Runnable onChanged) {
        this.onChanged = onChanged == null ? () -> {
        } : onChanged;
    }

    // ===== 判定（唯一真源） =====

    /**
     * 权限判定：未启用放行 → 归属者放行 → 命中条目按位判 → 未命中继承默认条目 → 无默认条目放行。
     *
     * @param player 判定主体；null（控制台/无主）放行
     */
    public static boolean allows(UUID owner, Map<UUID, Integer> perms, boolean hasDefault, int defaultPerms,
                                 UUID player, AkaishiSecurityPermission perm) {
        if (perm == null) {
            return true;
        }
        if (perms.isEmpty() && !hasDefault) {
            return true; // 一条都没登记 = 安全系统未启用（AE2 口径：没装安全终端 ⇒ 全放行）
        }
        if (player == null) {
            return true;
        }
        if (player.equals(owner)) {
            return true;
        }
        Integer bits = perms.get(player);
        if (bits != null) {
            return perm.granted(bits);
        }
        return hasDefault && perm.granted(defaultPerms);
    }

    /** 本地判定（终端自己用，不依赖注册表镜像是否已推送） */
    public boolean check(UUID player, AkaishiSecurityPermission perm) {
        return allows(owner, perms, hasDefault, defaultPerms, player, perm);
    }

    /** 安全系统是否已启用（登记了任何条目才算） */
    public boolean enabled() {
        return !perms.isEmpty() || hasDefault;
    }

    // ===== 归属者 =====

    /** 记录归属者（结构主方块放置时由方块调用） */
    public void setOwner(UUID owner, String name) {
        this.owner = owner;
        this.ownerName = name == null ? "" : name;
        // 归属者恒全权限，不占权限表条目
        if (owner != null && perms.remove(owner) != null) {
            names.remove(owner);
        }
        changed();
    }

    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    // ===== 登记 / 移除 =====

    /**
     * 用身份卡登记（安全页「登记」按钮）：把卡上的身份与权限位写入权限表。
     * 卡未绑定身份 ⇒ 写入默认权限条目。归属者本人不接受登记（恒全权限）。
     *
     * @return true = 已写入/更新
     */
    public boolean register(ItemStack card) {
        if (card == null || !(card.getItem() instanceof AkaishiWirelessIdentityCardItem)) {
            return false;
        }
        int mask = AkaishiWirelessIdentityCardItem.permsOf(card) & AkaishiSecurityPermission.ALL;
        UUID player = AkaishiWirelessIdentityCardItem.playerOf(card);
        if (player == null) {
            hasDefault = true;
            defaultPerms = mask;
            changed();
            return true;
        }
        if (player.equals(owner)) {
            return false;
        }
        if (!perms.containsKey(player) && perms.size() >= MAX_ENTRIES) {
            return false;
        }
        names.put(player, nameOr(AkaishiWirelessIdentityCardItem.playerNameOf(card), player));
        perms.put(player, mask);
        changed();
        return true;
    }

    /** 移除一条玩家条目 */
    public boolean removePlayer(UUID player) {
        if (player == null || perms.remove(player) == null) {
            return false;
        }
        names.remove(player);
        changed();
        return true;
    }

    /** 移除默认权限条目 */
    public boolean clearDefault() {
        if (!hasDefault) {
            return false;
        }
        hasDefault = false;
        defaultPerms = AkaishiSecurityPermission.NONE;
        changed();
        return true;
    }

    /** 直接设置默认权限条目（勾选默认行时用） */
    public void setDefault(int mask) {
        hasDefault = true;
        defaultPerms = mask & AkaishiSecurityPermission.ALL;
        changed();
    }

    /**
     * 勾选/取消一条权限位（安全页勾选框）。
     *
     * @param player null = 默认权限条目（勾选即视为登记默认条目）
     */
    public void toggle(UUID player, AkaishiSecurityPermission perm, boolean granted) {
        if (perm == null) {
            return;
        }
        if (player == null) {
            hasDefault = true;
            defaultPerms = perm.apply(defaultPerms, granted);
        } else {
            if (!perms.containsKey(player) && perms.size() >= MAX_ENTRIES) {
                return;
            }
            perms.put(player, perm.apply(perms.getOrDefault(player, AkaishiSecurityPermission.NONE), granted));
            names.putIfAbsent(player, shortUuid(player));
        }
        changed();
    }

    // ===== 查询（GUI / 镜像推送） =====

    /** 权限表（身份 → 权限位掩码）：只读视图，外部写入会绕过 changed() 导致镜像/快照静默过期 */
    public Map<UUID, Integer> entries() {
        return java.util.Collections.unmodifiableMap(perms);
    }

    /** 权限表玩家名（只读视图） */
    public Map<UUID, String> entryNames() {
        return java.util.Collections.unmodifiableMap(names);
    }

    public boolean hasDefaultEntry() {
        return hasDefault;
    }

    public int defaultPerms() {
        return defaultPerms;
    }

    /** 权限表条数（不含默认条目） */
    public int entryCount() {
        return perms.size();
    }

    /** 变更计数（GUI 同步包比对用） */
    public int revision() {
        return revision;
    }

    /** 权限表第 index 位玩家（GUI 行号 → 身份，与 {@link #entries()} 顺序一致）；越界返回 null */
    public UUID playerAt(int index) {
        if (index < 0 || index >= perms.size()) {
            return null;
        }
        int i = 0;
        for (UUID player : perms.keySet()) {
            if (i++ == index) {
                return player;
            }
        }
        return null;
    }

    /** 某条目当前权限位（player 为 null ⇒ 默认条目；未登记返回 0） */
    public int bitsOf(UUID player) {
        if (player == null) {
            return hasDefault ? defaultPerms : AkaishiSecurityPermission.NONE;
        }
        return perms.getOrDefault(player, AkaishiSecurityPermission.NONE);
    }

    /** 条目显示名（无名字时退化为 UUID 前 8 位） */
    public String displayName(UUID player) {
        if (player == null) {
            return "";
        }
        String name = names.get(player);
        return name == null || name.isEmpty() ? shortUuid(player) : name;
    }

    /** 把当前状态推送到网络注册表镜像（端口/便携终端/客户端 UI 查询用） */
    public void pushTo(UUID terminalId) {
        if (terminalId == null) {
            return;
        }
        WirelessNetworkManager.updateSecurity(terminalId, owner, perms, names, hasDefault, defaultPerms);
    }

    // ===== NBT =====

    /** 写入父 tag（空表不写，保持 NBT 干净） */
    public void save(CompoundTag parent) {
        CompoundTag tag = new CompoundTag();
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        tag.putBoolean("HasDefault", hasDefault);
        tag.putInt("DefaultPerms", defaultPerms);
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Integer> entry : perms.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("Player", entry.getKey());
            c.putString("Name", names.getOrDefault(entry.getKey(), ""));
            c.putInt("Perms", entry.getValue());
            list.add(c);
        }
        tag.put("Entries", list);
        parent.put(TAG_ROOT, tag);
    }

    /** 从父 tag 读入（缺失即视为空表；条数按 {@link #MAX_ENTRIES} 裁剪，防伪造 NBT） */
    public void load(CompoundTag parent) {
        perms.clear();
        names.clear();
        hasDefault = false;
        defaultPerms = AkaishiSecurityPermission.NONE;
        // 归属者必须一并复位：只在含 Owner 时覆盖会让"同一实例二次 load"保留旧归属者，
        // 而归属者恒全权限 —— 那就是一条静默的越权路径
        owner = null;
        ownerName = "";
        CompoundTag tag = parent.getCompound(TAG_ROOT);
        if (tag.contains("Owner")) {
            owner = tag.getUUID("Owner");
            ownerName = tag.getString("OwnerName");
        }
        hasDefault = tag.getBoolean("HasDefault");
        defaultPerms = tag.getInt("DefaultPerms") & AkaishiSecurityPermission.ALL;
        ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && perms.size() < MAX_ENTRIES; i++) {
            CompoundTag c = list.getCompound(i);
            if (!c.hasUUID("Player")) {
                continue;
            }
            UUID player = c.getUUID("Player");
            if (player.equals(owner)) {
                continue; // 归属者恒全权限，不占条目
            }
            names.put(player, c.getString("Name"));
            perms.put(player, c.getInt("Perms") & AkaishiSecurityPermission.ALL);
        }
    }

    // ===== 内部 =====

    private void changed() {
        revision++;
        onChanged.run();
    }

    private static String nameOr(String name, UUID fallback) {
        return name == null || name.isEmpty() ? shortUuid(fallback) : name;
    }

    private static String shortUuid(UUID id) {
        return id == null ? "" : id.toString().substring(0, 8).toUpperCase();
    }
}
