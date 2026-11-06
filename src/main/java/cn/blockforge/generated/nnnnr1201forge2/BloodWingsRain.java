package cn.blockforge.generated.nnnnr1201forge2;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 「翅王」的左键血雨（r261）：准星处先亮一个 5×5 红圈，1 秒后 100 条穿血从上往下铺满。
 *
 * <p>每条的伤害是原版穿血的 5%（穿血 base=32，倍率走 {@link BloodArts} 的同一套算法）。
 * 玩家与胀相 NPC 共用这套调度。
 */
public final class BloodWingsRain {
    /** 红圈半径（格）与网格。 */
    public static final double RADIUS = 2.5;
    public static final int GRID = 10, DELAY = 20;
    private static final List<Object[]> JOBS = new ArrayList<>();

    /** 播放红圈并排定 1 秒后的血雨。 */
    public static void request(LivingEntity caster, ServerLevel level, Vec3 center) {
        Network.send(level, new Network.Effect(BloodArts.FX + 22, caster.getId(), center, Vec3.ZERO, 5f, DELAY, 0));
        JOBS.add(new Object[]{caster, level, center, level.getGameTime() + DELAY});
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || JOBS.isEmpty()) return;
        for (var it = JOBS.iterator(); it.hasNext(); ) {
            Object[] job = it.next();
            LivingEntity caster = (LivingEntity) job[0];
            ServerLevel level = (ServerLevel) job[1];
            Vec3 c = (Vec3) job[2];
            if (level.getGameTime() < (long) job[3]) continue;
            it.remove();
            if (!caster.isAlive() || caster.level() != level) continue;
            fire(caster, level, c);
        }
    }

    private static void fire(LivingEntity caster, ServerLevel level, Vec3 center) {
        // r263：红圈内随机落点、随机倾角，从天上扎满整个圈；不再用穿血表现
        var rnd = level.random;
        for (int i = 0; i < 90; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(rnd.nextDouble()) * RADIUS;
            double x = center.x + Math.cos(a) * r;
            double z = center.z + Math.sin(a) * r;
            double tiltX = (rnd.nextDouble() - .5) * .7;
            double tiltZ = (rnd.nextDouble() - .5) * .7;
            Vec3 point = new Vec3(x, center.y, z);
            Network.send(level, new Network.Effect(BloodArts.FX_SPIKE, caster.getId(),
                point, new Vec3(tiltX, -1, tiltZ), (float)(1.0 + rnd.nextDouble() * .4), 16, 0));
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(point, point).inflate(0.75),
                t -> t.isAlive() && t != caster && t.getBoundingBox().inflate(0.15).contains(point))) {
                hurt(caster, t);
            }
        }
        level.playSound(null, net.minecraft.core.BlockPos.containing(center),
            net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.3f);
    }

    private static void hurt(LivingEntity caster, LivingEntity target) {
        if (caster instanceof ServerPlayer p) {
            if (!CombatSafety.canTarget(p, target)) return;
            // 原版穿血 base 32；这里取 5%（0.5 的比例再乘 0.05）
            BloodArts.wingsRainHit(p, target);
        } else {
            target.hurt(caster.damageSources().mobAttack(caster), 1.6f);
        }
    }

    private BloodWingsRain() { }
}
