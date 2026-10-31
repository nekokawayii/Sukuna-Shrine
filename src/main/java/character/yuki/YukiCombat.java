package character.yuki;

import character.CharacterSkill;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 九十九由基的战斗实现。
 * <p>核心是「加重」层数：最多 10 层，每层让质量拳/迦楼罗的伤害 ×1.5；
 * 第 11 次点击会把质量压成黑洞——自伤 90% 生命上限、把周围敌对生物吸过来并造成巨额伤害。
 */
public final class YukiCombat {
    /** 存档键：当前加重层数。 */
    private static final String RAGE_KEY = "YukiRage";
    /** 叠加上限；超过即触发黑洞。 */
    public static final int RAGE_MAX = 10;
    /** 每层加伤倍率（层数 n → 1 + 0.5n，满层 ×6）。 */
    private static final double RAGE_STEP = 0.5;

    public static int rage(Player p) {
        return p == null ? 0 : cn.blockforge.generated.nnnnr1201forge2.Curse.data(p).getInt(RAGE_KEY);
    }

    private static void setRage(Player p, int value) {
        cn.blockforge.generated.nnnnr1201forge2.Curse.data(p).putInt(RAGE_KEY, value);
    }

    /** 当前加重倍率。 */
    public static double rageMultiplier(Player p) {
        return 1 + RAGE_STEP * rage(p);
    }

    private static List<LivingEntity> around(ServerPlayer p, Vec3 center, double radius) {
        return p.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
            e -> e != p && e.isAlive() && !(e instanceof Player q && q.isCreative()));
    }

    /** 质量拳：前方锥形，重击 + 强击退，吃加重层数。 */
    public static void massFist(ServerPlayer p, CharacterSkill skill, int rank) {
        punchOnce(p, skill, rank, 1.0);
    }

    /** 星崩拳：质量拳的五倍伤害，无视防御（魔法伤害），保底目标 10% 生命上限。 */
    public static void starCrash(ServerPlayer p, CharacterSkill skill, int rank) {
        Vec3 eye = p.getEyePosition(), dir = p.getLookAngle();
        double range = skill.range();
        float base = (float) (skill.damageAt(rank) * rageMultiplier(p));
        int hit = 0;
        for (LivingEntity e : around(p, eye.add(dir.scale(range * 0.5)), range * 0.6 + skill.radius())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > range) continue;
            if (to.normalize().dot(dir) < Math.cos(Math.toRadians(skill.halfAngle()))) continue;
            // 先按正常伤害结算（magic：无视护甲）
            float floor = e.getMaxHealth() * 0.20f;                    // 保底 20% 目标生命上限
            float before = e.getHealth();
            e.hurt(p.damageSources().magic(), Math.max(base, floor));
            // 保底优先级最高：不足 20% 就用「真伤」补足，绕过一切限伤/防御/减伤。
            float lost = before - e.getHealth();
            if (e.isAlive() && lost < floor) {
                trueDamage(p, e, floor - lost);
            }
            Vec3 kb = to.normalize().scale(2.6);
            e.push(kb.x, 0.9, kb.z);
            e.hurtMarked = true;
            hit++;
        }
        if (p.level() instanceof ServerLevel level) {
            for (int i = 1; i <= 16; i++) {
                Vec3 at = eye.add(dir.scale(range * i / 16.0));
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
            level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.6f, 0.45f);
        }
        p.displayClientMessage(Component.literal("星崩拳 · 命中 " + hit + " 个目标"), true);
    }

    /** 单次质量拳判定；multiplier 用于连拳把每拳调弱一点。 */
    private static void punchOnce(ServerPlayer p, CharacterSkill skill, int rank, double multiplier) {
        Vec3 eye = p.getEyePosition(), dir = p.getLookAngle();
        double range = skill.range();
        float damage = (float) (skill.damageAt(rank) * rageMultiplier(p) * multiplier);
        for (LivingEntity e : around(p, eye.add(dir.scale(range * 0.5)), range * 0.6 + skill.radius())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > range) continue;
            if (to.normalize().dot(dir) < Math.cos(Math.toRadians(skill.halfAngle()))) continue;
            e.hurt(p.damageSources().playerAttack(p), damage);
            Vec3 kb = to.normalize().scale(2.0);
            e.push(kb.x, 0.6, kb.z);
            e.hurtMarked = true;
        }
        if (p.level() instanceof ServerLevel level) {
            for (int i = 1; i <= 8; i++) {
                Vec3 at = eye.add(dir.scale(range * i / 8.0));
                level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
            level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.7f);
        }
    }

    /** 迦楼罗：沿准星贯穿，命中全部沿途目标，吃加重层数。 */
    public static void garuda(ServerPlayer p, CharacterSkill skill, int rank) {
        Vec3 eye = p.getEyePosition(), dir = p.getLookAngle();
        double range = skill.range();
        float damage = (float) (skill.damageAt(rank) * rageMultiplier(p));
        java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
        for (double t = 1; t <= range; t += 1.0) {
            hit.addAll(around(p, eye.add(dir.scale(t)), skill.radius()));
        }
        for (LivingEntity e : hit) {
            e.hurt(p.damageSources().playerAttack(p), damage);
            e.hurtMarked = true;
        }
        if (p.level() instanceof ServerLevel level) {
            for (double t = 0; t <= range; t += 0.7) {
                Vec3 at = eye.add(dir.scale(t));
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, 2, 0.15, 0.15, 0.15, 0.01);
            }
            level.playSound(null, p.blockPosition(), SoundEvents.PHANTOM_SWOOP, SoundSource.PLAYERS, 1.4f, 0.8f);
        }
    }

    /** 领域展开·星轨坠亡。 */
    public static void domain(ServerPlayer p, CharacterSkill skill, int rank) {
        YukiDomain.open(p);
    }

    /** 加重：叠一层；第 11 次（已满 10 层）压成黑洞。 */
    public static void starRage(ServerPlayer p, CharacterSkill skill, int rank) {
        int stacks = rage(p);
        if (stacks >= RAGE_MAX) {
            blackHole(p, rank, stacks);
            setRage(p, 0);
            return;
        }
        int next = stacks + 1;
        setRage(p, next);
        // 视觉反馈：层数越高越沉
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, Math.min(3, next / 3), false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, Math.min(2, next / 4), false, false, true));
        p.displayClientMessage(Component.literal(
            "加重 " + next + "/" + RAGE_MAX + " · 伤害 ×" + String.format(java.util.Locale.ROOT, "%.1f", 1 + RAGE_STEP * next)
            + (next >= RAGE_MAX ? " · 下次将压成黑洞" : "")), true);
        if (p.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ASH, p.getX(), p.getY() + 1, p.getZ(), 30 + next * 6, 0.6, 0.8, 0.6, 0.02);
            level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8f, 0.5f + next * 0.05f);
        }
    }

    /**
     * 黑洞（第 11 次加重触发）：
     * <ul>
     *   <li>自身承受 90% 生命上限的伤害（真实伤害类型，不吃护甲）</li>
     *   <li>把 24 格内的敌对生物吸到身边</li>
     *   <li>对它们结算一次巨额伤害（按满层倍率）</li>
     * </ul>
     */
    private static void blackHole(ServerPlayer p, int rank, int stacks) {
        double multiplier = 1 + RAGE_STEP * stacks;
        float selfDamage = (float) (p.getMaxHealth() * 0.9);
        p.hurt(p.damageSources().magic(), selfDamage);
        p.displayClientMessage(Component.literal("质量坍缩——黑洞炸开（自身承受 90% 生命）"), false);

        float damage = (float) ((1200 + 300 * Math.max(0, rank)) * multiplier);
        Vec3 center = p.position();
        for (LivingEntity e : around(p, center, 24)) {
            if (!(e instanceof Enemy) && !(e instanceof net.minecraft.world.entity.Mob m && m.getTarget() == p)) continue;
            Vec3 pull = center.subtract(e.position());
            double dist = Math.max(0.4, pull.length());
            Vec3 v = pull.normalize().scale(1.8 / dist);
            e.push(v.x * 8, 0.6, v.z * 8);
            e.hurtMarked = true;
            e.hurt(p.damageSources().magic(), damage);
        }
        if (p.level() instanceof ServerLevel level) {
            for (int i = 0; i < 160; i++) {
                double a = i * Math.PI / 40, r = 10 * (1 - i / 180.0);
                level.sendParticles(ParticleTypes.PORTAL,
                    center.x + Math.cos(a) * r * 2, center.y + 0.3 + (i % 15) * 0.4, center.z + Math.sin(a) * r * 2,
                    2, 0.1, 0.1, 0.1, 0.06);
            }
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 1, center.z, 1, 0, 0, 0, 0);
            level.playSound(null, p.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2f, 0.4f);
        }
    }

    /**
     * 真伤：无视限伤、护甲、减伤、伤害事件。
     * <p>净土守护者这类 BOSS 在 setHealth 里用 private 的 internalHealth 开关做兜底，
     * 普通写血会被夹到 HitCap；这里用反射临时把开关打开再写，写完整回原值。
     */
    public static void trueDamage(ServerPlayer p, LivingEntity e, double amount) {
        if (e == null || amount <= 0) return;
        float want = (float) Math.max(0.0, e.getHealth() - amount);
        try {
            var field = findField(e.getClass(), "internalHealth");
            if (field != null) {
                field.setAccessible(true);
                boolean previous = field.getBoolean(e);
                field.setBoolean(e, true);
                try {
                    e.setHealth(want);
                } finally {
                    field.setBoolean(e, previous);
                }
            } else {
                e.setHealth(want);
            }
        } catch (ReflectiveOperationException ex) {
            e.setHealth(want);
        }
        e.hurtMarked = true;
        if (e.getHealth() <= 0f) e.die(p.damageSources().magic());
    }

    /** 在类继承链上找字段（守护者的开关就在它自己类里）。 */
    private static java.lang.reflect.Field findField(Class<?> type, String name) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // 继续往父类找
            }
        }
        return null;
    }

    /** 生命封顶之后，成长改走「抗性」：27 章以后每多一章 +1% 减伤，上限 60%。 */
    public static double resist(ServerPlayer p) {
        if (!"yuki".equals(character.CharacterService.selected(p) == null ? ""
            : character.CharacterService.selected(p).id())) return 0;
        int rank = character.CharacterService.rank(p);
        return Math.min(0.60, Math.max(0, rank - 27) * 0.01);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onHurt(net.minecraftforge.event.entity.living.LivingHurtEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        double cut = resist(p);
        if (cut <= 0) return;
        e.setAmount((float) (e.getAmount() * (1 - cut)));
    }

    private YukiCombat() {}
}
