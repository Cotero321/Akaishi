package com.example.akaishi.boss.agaitolos.skill;

import com.example.akaishi.boss.agaitolos.AgaitolosEntity;
import com.example.akaishi.boss.agaitolos.entity.AgaitolosWitherSkull;
import com.example.akaishi.entity.ModEntities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 一阶段远程攻击：召唤凋零头颅（设计文档 §0「远程攻击」/§1 第 20 条）。
 * <p>
 * 本类只负责「怎么发射」；命中结算（5 爆炸 + 凋零 III 2s + 2 真实伤害）在弹体
 * {@link AgaitolosWitherSkull} 内完成。伤害常量因此放在弹体类、发射参数放在本类，
 * 两个类单向引用（本类 → 弹体），不互相引用。
 */
public final class AgaitolosSkullSkill {

    /** 弹体初速（格/tick）0.8 ≈ 16 格/s。待调手感值 / P8 转配置项 */
    public static final float SKULL_VELOCITY = 0.8F;

    /** 弹体散布（原版 shoot 的 inaccuracy 参数，弧度）。待调手感值 / P8 转配置项 */
    public static final float SKULL_INACCURACY = 1.0F;

    private AgaitolosSkullSkill() {
    }

    /**
     * 朝目标发射一发凋零头颅（仅服务端生成，由 {@code AgaitolosEntity#performRangedAttack} 调用）。
     */
    public static void fire(AgaitolosEntity boss, LivingEntity target) {
        if (boss.level().isClientSide()) {
            return;
        }
        AgaitolosWitherSkull skull = new AgaitolosWitherSkull(ModEntities.AGAITOLOS_WITHER_SKULL.get(), boss.level());
        // owner = BOSS：既是击杀归属，也是「是否被反弹」的判据来源（被反弹后 owner 会变成玩家）
        skull.setOwner(boss);
        // 不可破坏地形：dangerous=false 降低飞行惯性、取消对火方块的额外破坏；
        // 真正的场地保护由牢狱方块自身的 Float.MAX_VALUE 爆炸抗性兜底（设计 §4.3）
        skull.setDangerous(false);
        Vec3 origin = new Vec3(boss.getX(), boss.getEyeY(), boss.getZ());
        // 瞄准目标身体中部而非脚底，避免目标贴地时弹体一路撞地板
        Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(origin);
        skull.setPos(origin.x, origin.y, origin.z);
        skull.shoot(aim.x, aim.y, aim.z, SKULL_VELOCITY, SKULL_INACCURACY);
        boss.level().addFreshEntity(skull);
    }
}
