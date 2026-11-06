package cn.blockforge.generated.nnnnr1201forge2;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 里梅「冰凝咒法」扩展包（r287）：霜寒叠层、千手冰牢、急冻风暴、冰龙、大冰河时代。
 *
 * <p>全部以服务端 tick 结算，视觉走 {@link BranchRules#FX_FROST}/{@link BranchRules#FX_ICE}。
 */
public final class UraumeIce {
    /** 霜寒：每层 -10% 移速（用缓慢药水近似），满 5 层强制冻结 1.5 秒并在解冻时吃真实伤害。 */
    public static final int FROST_MAX = 5, FROST_TICKS = 200, FREEZE_ON_MAX = 30;
    public static final float SHATTER_DAMAGE = 8f;
    /** 大冰河时代：半径 50、冻住 10 秒、自身冻伤 30 秒。 */
    public static final double ICE_AGE_RADIUS = 50, ICE_AGE_DAMAGE = 40;
    public static final int ICE_AGE_FREEZE = 200, ICE_AGE_FROSTBITE = 600;

    private record Storm(Mob owner, ServerLevel level, Vec3 center, long end) { }
    private static final List<Storm> STORMS = new ArrayList<>();

    public static void addFrost(LivingEntity target, int amount) {
        if (target == null || !target.isAlive()) return;
        CompoundTag data = target.getPersistentData();
        int stacks = Math.min(FROST_MAX, data.getInt("FrostStacks") + amount);
        data.putInt("FrostStacks", stacks);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FROST_TICKS,
            Math.max(0, stacks - 1), false, false));
        if (stacks >= FROST_MAX) {
            data.putInt("FrostStacks", 0);
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), FREEZE_ON_MAX + 60));
            data.putLong("FrostShatterAt", target.level().getGameTime() + FREEZE_ON_MAX);
        }
    }

    /** 千手冰牢：目标脚下巨型手印合拢，范围内敌人被冻住并定身 2 秒。 */
    public static void handPrison(Mob caster, LivingEntity target) {
        if (!(caster.level() instanceof ServerLevel level) || target == null) return;
        Vec3 at = target.position();
        Network.send(level, new Network.Effect(UraumeFx.PRISON, caster.getId(),
            at.add(0, 2, 0), new Vec3(0, -1, 0), 5f, 40, 1));
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(4.5),
            t -> t != caster && t.isAlive() && t instanceof Mob)) {
            t.setTicksFrozen(240);
            if (t instanceof Mob m) character.npc.NpcControl.stun(m, "uraume_prison", 40);
            NpcCombat.hurt(caster, t, 14f);
            addFrost(t, 1);
        }
        level.playSound(null, net.minecraft.core.BlockPos.containing(at),
            net.minecraft.sounds.SoundEvents.GLASS_PLACE, net.minecraft.sounds.SoundSource.HOSTILE, 1.4f, .7f);
    }

    /** 急冻风暴：以自身为中心 5 秒的冰雪龙卷，每 4 刻把敌人吸向中心并造成伤害。 */
    public static void storm(Mob caster) {
        if (!(caster.level() instanceof ServerLevel level)) return;
        STORMS.add(new Storm(caster, level, caster.position(), level.getGameTime() + 100));
        level.playSound(null, caster.blockPosition(), net.minecraft.sounds.SoundEvents.ELDER_GUARDIAN_CURSE,
            net.minecraft.sounds.SoundSource.HOSTILE, 1.2f, 1.4f);
    }

    /** 冰龙：向前方贯穿 20 格，沿途生物受高伤害并被强击退。 */
    public static void dragon(Mob caster) {
        if (!(caster.level() instanceof ServerLevel level)) return;
        LivingEntity target = caster.getTarget();
        Vec3 dir = target != null ? target.position().subtract(caster.position()).normalize() : caster.getLookAngle();
        Vec3 origin = caster.position().add(0, 1.0, 0);
        Network.send(level, new Network.Effect(UraumeFx.DRAGON, caster.getId(), origin, dir, 20f, 30, 2));
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class,
            new AABB(origin, origin.add(dir.scale(20))).inflate(3.5),
            t -> t != caster && t.isAlive() && t instanceof Mob)) {
            Vec3 delta = t.position().subtract(origin);
            if (delta.lengthSqr() > 1 && delta.normalize().dot(dir) < 0.6) continue;
            NpcCombat.hurt(caster, t, 25f);
            addFrost(t, 2);
            t.setDeltaMovement(dir.normalize().scale(2.2).add(0, .55, 0));
            t.hurtMarked = true;
        }
        level.playSound(null, caster.blockPosition(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
            net.minecraft.sounds.SoundSource.HOSTILE, 1.4f, 1.3f);
    }

    /** 极之番·大冰河时代：50 格全冻；自身 30 秒严重冻伤（移速减半、无法放技能）。 */
    public static void iceAge(Mob caster) {
        if (!(caster.level() instanceof ServerLevel level)) return;
        Vec3 center = caster.position();
        Network.send(level, new Network.Effect(UraumeFx.ICEAGE, caster.getId(), center, Vec3.ZERO, 50f, 60, 9));
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(ICE_AGE_RADIUS),
            t -> t != caster && t.isAlive() && t instanceof Mob)) {
            t.setTicksFrozen(ICE_AGE_FREEZE + 200);
            if (t instanceof Mob m) character.npc.NpcControl.stun(m, "uraume_iceage", ICE_AGE_FREEZE);
            NpcCombat.hurt(caster, t, (float) ICE_AGE_DAMAGE);
            addFrost(t, 3);
            t.getPersistentData().putBoolean("IceStatue", true);
        }
        caster.getPersistentData().putLong("FrostbiteUntil", level.getGameTime() + ICE_AGE_FROSTBITE);
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ICE_AGE_FROSTBITE, 2, false, false));
        level.playSound(null, caster.blockPosition(), net.minecraft.sounds.SoundEvents.GLASS_BREAK,
            net.minecraft.sounds.SoundSource.HOSTILE, 1.6f, .5f);
    }

    /** 被冻成冰雕的目标，如果这段时间内被重击（≥20 伤害）就直接碎掉（普通生物即死，Boss 额外高伤）。 */
    @SubscribeEvent
    public static void statueShatter(net.minecraftforge.event.entity.living.LivingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || !victim.getPersistentData().getBoolean("IceStatue")) return;
        if (e.getAmount() < 20f) return;
        victim.getPersistentData().remove("IceStatue");
        if (victim instanceof Mob mob && mob.getMaxHealth() > 2000) {
            NpcCombat.hurt(mob, mob, 200f); // Boss：高额冻伤而不是直接处决
        } else {
            victim.hurt(victim.level().damageSources().freeze(), 100000f);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        long now = e.getServer().overworld().getGameTime();
        for (var it = STORMS.iterator(); it.hasNext(); ) {
            Storm storm = it.next();
            if (now >= storm.end || !storm.owner.isAlive() || storm.owner.level() != storm.level) { it.remove(); continue; }
            if (now % 4 != 0) continue;
            Vec3 center = storm.center;
            for (LivingEntity t : storm.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(9), t -> t != storm.owner && t.isAlive() && t instanceof Mob)) {
                Vec3 pull = center.subtract(t.position()).normalize().scale(1.1);
                t.setDeltaMovement(t.getDeltaMovement().add(pull.x, .06, pull.z));
                t.hurtMarked = true;
                if (t.distanceToSqr(center) < 16) {
                    NpcCombat.hurt(storm.owner, t, 4f);
                    addFrost(t, 1);
                }
            }
        }
    }

    private UraumeIce() { }
}
