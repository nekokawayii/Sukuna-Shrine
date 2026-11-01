package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 天逆蛑的「术式切断」公共实现：把目标正在维持的术式、领域、无下限全部终止，
 * 并给目标挂 3 秒的术式封印。
 *
 * <p>这个类也作为后续咒具的接口：只要调用 {@link #sever(LivingEntity, boolean, LivingEntity)}，
 * 玩家、NPC（{@code NpcBridge} 假玩家）、女仆、五条悟/虎杖/净土守护者等首领都会走各自已有的关闭路径。</p>
 */
public final class TechniqueSeverance {
    /** 术式封印时长：3 秒 = 60 刻。 */
    public static final int SEAL_TICKS = 60;
    /** 右键「术式切断」的作用半径（格）。 */
    public static final double RADIUS = 16.0;
    /** 目标身上记录封印到期的存档键；NPC 的旧技能门也会读它。 */
    public static final String SEAL_KEY = "TenkaSeveredUntil";
    /** 切断进行中：领域关闭本身会追加的 2 分钟熔断不生效，天逆蛑只封 3 秒。 */
    private static boolean severing;

    /** 领域/首领的关闭路径会调用它：切断期间不要追加长熔断，3 秒封印由天逆蛑自己加。 */
    public static boolean severing() { return severing; }

    /** 右键：终止自己的术式，并切断周围一切生物的术式（自己不 sealed）。返回被波及的目标数量。 */
    public static int severAround(ServerPlayer user) {
        ServerLevel level = user.serverLevel();
        int count = DomainClash.severDomainsAt(level, user.position());
        sever(user, false, user);
        AABB area = user.getBoundingBox().inflate(RADIUS, 8.0, RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
            e -> e != user && e.isAlive() && !e.isSpectator())) {
            if (sever(target, true, user)) count++;
        }
        ring(level, user.position());
        return count;
    }

    /**
     * 终止一个生物的一切术式。
     *
     * @param target   目标
     * @param seal     是否额外封印 3 秒（右键范围效果为 true；被攻击者也为 true）
     * @param source   施术者（目前只用于事件归属，可为 null）
     * @return 是否真的处理了这个目标
     */
    public static boolean sever(LivingEntity target, boolean seal, LivingEntity source) {
        return sever(target, seal, source, SEAL_TICKS);
    }

    /** r215：可指定封印时长（甚尔命中要 5 秒，即 100 刻）。 */
    public static boolean sever(LivingEntity target, boolean seal, LivingEntity source, int sealTicks) {
        if (target == null || target.level().isClientSide) return false;
        boolean outer = severing;
        severing = true;
        try {
            return sever0(target, seal, source, Math.max(1, sealTicks));
        } finally {
            severing = outer;
        }
    }

    private static boolean sever0(LivingEntity target, boolean seal, LivingEntity source, int ticks) {
        boolean touched = DomainClash.sever(target);
        if (target instanceof ServerPlayer p) {
            cancelCasting(p);
            touched = true;
            if (seal) sealPlayer(p, ticks);
        } else if (target instanceof Mob mob) {
            // NPC（含假玩家）与女仆：先走它们自己的施术上下文。
            ServerPlayer actor = NpcBridge.supports(mob) ? NpcBridge.actorFor(mob) : MaidBridge.progression(mob);
            if (actor != null) {
                touched |= DomainClash.sever(actor);
                cancelCasting(actor);
                if (seal) sealPlayer(actor, ticks);
            }
            if (mob instanceof GojoEntity gojo) {
                gojo.severTechnique();
                touched = true;
            } else if (mob instanceof GuardianEntity guardian) {
                guardian.clashInterrupt();
                touched = true;
            } else if (mob instanceof YujiEntity yuji) {
                yuji.clashInterrupt();
                touched = true;
            }
            if (seal) sealMob(mob, ticks);
        }
        if (seal && !(target instanceof ServerPlayer) && !(target instanceof Mob)) sealMob(target, ticks);
        return touched || seal;
    }

    /** 关闭玩家/假玩家/女仆当前所有持续术式与领域；对已关闭的对象重复调用是幂等的。 */
    private static void cancelCasting(ServerPlayer p) {
        try { AuthoritySkills.cancel(p); } catch (Throwable ignored) { }
        try { Skills.closeDomain(p); } catch (Throwable ignored) { }
        try { BloodDomain.close(p); } catch (Throwable ignored) { }
        try { GuardianPlayerSkills.closeOwnedDomain(p, false); } catch (Throwable ignored) { }
        try { GojoCombat.closeDomain(p); } catch (Throwable ignored) { }
        try { character.jogo.JogoCombat.closeDomain(p); } catch (Throwable ignored) { }
        try { character.ryu.RyuDomain.forceClose(p); } catch (Throwable ignored) { }
        try { character.naoya.NaoyaDomain.forceClose(p); } catch (Throwable ignored) { }
        try { if (character.higuruma.HigurumaDomain.active(p)) character.higuruma.HigurumaDomain.close(p, "天逆蛑·术式切断"); } catch (Throwable ignored) { }
        try { if (character.domainking.SelfEmbryoDomain.active(p)) character.domainking.SelfEmbryoDomain.close(p, "天逆蛑·术式切断"); } catch (Throwable ignored) { }
        try { if (character.yuta.YutaDomain.active(p)) character.yuta.YutaDomain.close(p, "天逆蛑·术式切断"); } catch (Throwable ignored) { }
        try { if (character.yuki.YukiDomain.active(p)) character.yuki.YukiDomain.close(p, "天逆蛑·术式切断"); } catch (Throwable ignored) { }
        try { if (character.hakari.HakariDomain.active(p)) character.hakari.HakariDomain.close(p, "天逆蛑·术式切断"); } catch (Throwable ignored) { }
        try { if (p instanceof NpcBridge.NpcActor a && a.mob instanceof Mob npc) character.npc.NpcDomain.close(npc, "天逆蛑·术式切断"); } catch (Throwable ignored) { }
    }

    private static void sealPlayer(ServerPlayer p, int ticks) {
        Curse.severTechnique(p, ticks);
    }

    /** 没有玩家上下文的首领/生物：写同一个到期键，旧技能门会读它。 */
    private static void sealMob(LivingEntity target, int ticks) {
        long now = target.level().getGameTime();
        var data = target.getPersistentData();
        data.putLong(SEAL_KEY, now + ticks);
        data.putLong("NpcLegacyBurnout", now + ticks);
        data.putLong("NpcLegacyRepairAt", now + ticks);
        target.addEffect(new MobEffectInstance(TechniqueEffects.TECHNIQUE_SEAL.get(), ticks, 0, false, true));
    }

    /** 持续接触天逆蛑：只续上封锁，不重复关闭领域、遍历术式或每刻同步整份角色资料。 */
    public static void maintainSeal(LivingEntity target) {
        if (target == null || target.level().isClientSide) return;
        long until = target.level().getGameTime() + SEAL_TICKS;
        target.getPersistentData().putLong(SEAL_KEY, Math.max(until, target.getPersistentData().getLong(SEAL_KEY)));
        ServerPlayer actor = target instanceof ServerPlayer p ? p : NpcBridge.existing(target);
        if (actor == null) actor = MaidBridge.caster(target);
        if (actor != null) Curse.data(actor).putLong(SEAL_KEY, Math.max(until, Curse.data(actor).getLong(SEAL_KEY)));
        var effect = target.getEffect(TechniqueEffects.TECHNIQUE_SEAL.get());
        if (effect == null || effect.getDuration() < 40)
            target.addEffect(new MobEffectInstance(TechniqueEffects.TECHNIQUE_SEAL.get(), SEAL_TICKS, 0, false, true));
    }

    /** 一圈向外扩散的切断波纹，纯表现。 */
    private static void ring(ServerLevel level, Vec3 center) {
        for (int band = 0; band < 3; band++) {
            double radius = 4.0 + band * 5.0;
            for (int i = 0; i < 72; i++) {
                double a = Math.PI * 2.0 * i / 72.0;
                double x = center.x + Math.cos(a) * radius;
                double z = center.z + Math.sin(a) * radius;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, center.y + 0.9, z, 1, 0, 0, 0, 0);
                if (i % 4 == 0) {
                    level.sendParticles(ParticleTypes.END_ROD, x, center.y + 0.9, z, 1, 0, 0, 0, 0.01);
                }
            }
        }
    }

    private TechniqueSeverance() { }
}
