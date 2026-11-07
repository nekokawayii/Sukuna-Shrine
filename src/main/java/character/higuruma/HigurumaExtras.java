package character.higuruma;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import cn.blockforge.generated.nnnnr1201forge2.CombatSafety;
import cn.blockforge.generated.nnnnr1201forge2.CombatSettings;
import cn.blockforge.generated.nnnnr1201forge2.Curse;
import cn.blockforge.generated.nnnnr1201forge2.Network;

/** 日车宽见新招（r343）：碎骨被动 / 一锤定音 / 证据冻结 / 灵柩 / 审判之光。 */
public final class HigurumaExtras {
    public static final int BONE = 2303, VERDICT = 2304, FREEZE = 2305, COFFIN = 2306, LIGHT = 2307;
    public static final int FX_BONE = 770, FX_VERDICT = 771, FX_FREEZE = 772, FX_COFFIN = 773, FX_LIGHT = 774;

    private record Slam(ServerPlayer owner, ServerLevel level, Vec3 at, Vec3 dir, long when, int kind) { }
    private static final List<Slam> PENDING = new ArrayList<>();
    private static final String MARK = "HigurumaMark";

    /** 碎骨：持法槌平A 对护甲/护盾目标额外 200%。 */
    @SubscribeEvent
    public static void attack(AttackEntityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.level().isClientSide) return;
        if (!(e.getTarget() instanceof LivingEntity t)) return;
        if (!"higuruma".equals(role(p))) return;
        boolean gavel = p.getMainHandItem().is(cn.blockforge.generated.nnnnr1201forge2.Content.GAVEL.get());
        if (!gavel) return;
        if (t.getArmorValue() > 0 || t.getAbsorptionAmount() > 0) {
            t.invulnerableTime = 0;
            CombatSettings.hurt(p, t, p.damageSources().playerAttack(p), 10f);
            t.invulnerableTime = 0;
            Network.send((ServerLevel) t.level(), new Network.Effect(FX_BONE, p.getId(), t.position(), p.getLookAngle(), 1.2f, 10, 0));
        }
    }

    public static void cast(ServerPlayer p, int skill) {
        if (!(p.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        String key = "HigurumaCD_" + skill;
        if (Curse.data(p).getLong(key) > now) { msg(p, "冷却中"); return; }
        int cost = switch (skill) { case VERDICT -> 200; case FREEZE -> 150; case COFFIN -> 220; case LIGHT -> 260; default -> 0; };
        if (cost > 0 && !Curse.spend(p, cost)) { msg(p, "咒力不足"); return; }
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        boolean ok = true;
        switch (skill) {
            case VERDICT -> { // 蓄力 1 秒后跃起猛砸：打断 + 眩晕 2 秒
                PENDING.add(new Slam(p, level, p.position(), look, now + 20, 0));
                Network.send(level, new Network.Effect(FX_VERDICT, p.getId(), eye, look, 2f, 20, 0));
            }
            case FREEZE -> { // 金链锁住目标 2 秒，禁止位移
                LivingEntity target = ray(p, 20);
                if (target == null) { Curse.addRaw(p, cost); ok = false; msg(p, "证据冻结：准星 20 格内没有目标"); }
                else {
                    target.getPersistentData().putLong("HigurumaBind", now + 40);
                    target.getPersistentData().putLong(MARK, now + 600);
                    target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
                    target.hurtMarked = true;
                    if (target instanceof Mob m) character.npc.NpcControl.stun(m, "higuruma_bind", 40);
                    Network.send(level, new Network.Effect(FX_FREEZE, p.getId(), target.position(), look, 1.5f, 40, 0));
                }
            }
            case COFFIN -> { // 前扇形：长椅卡住 1.5 秒
                int n = 0;
                for (LivingEntity t : targets(p, eye.add(look.scale(6)), 8)) {
                    Vec3 to = t.position().subtract(eye);
                    if (to.length() > 8 || to.normalize().dot(look) < Math.cos(Math.toRadians(60))) continue;
                    t.getPersistentData().putLong("HigurumaBind", now + 30);
                    t.getPersistentData().putLong(MARK, now + 600);
                    if (t instanceof Mob m) character.npc.NpcControl.stun(m, "higuruma_coffin", 30);
                    n++;
                }
                Network.send(level, new Network.Effect(FX_COFFIN, p.getId(), eye.add(look.scale(4)), look, 4f, 24, 0));
                if (n == 0) { Curse.addRaw(p, cost); ok = false; msg(p, "拘束·灵柩：前方没有目标"); }
            }
            case LIGHT -> { // 对被起诉目标降下金色光柱真伤
                LivingEntity target = null; double best = 30 * 30;
                for (LivingEntity t : targets(p, eye, 30)) {
                    Vec3 to = t.position().subtract(eye);
                    if (to.normalize().dot(look) < Math.cos(Math.toRadians(25))) continue;
                    if (t.getPersistentData().getLong(MARK) <= now) continue;
                    double d = to.lengthSqr();
                    if (d < best) { best = d; target = t; }
                }
                if (target == null) { Curse.addRaw(p, cost); ok = false; msg(p, "审判之光：30 格内没有已标记的目标（先用证据冻结）"); }
                else {
                    target.invulnerableTime = 0;
                    var holder = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL);
                    target.hurt(new net.minecraft.world.damagesource.DamageSource(holder, p, p), 60f);
                    target.invulnerableTime = 0;
                    Network.send(level, new Network.Effect(FX_LIGHT, p.getId(), target.position(), Vec3.ZERO, 2f, 30, 0));
                }
            }
            default -> ok = false;
        }
        if (ok) {
            int cd = switch (skill) { case VERDICT -> 160; case FREEZE -> 120; case COFFIN -> 180; case LIGHT -> 200; default -> 100; };
            Curse.data(p).putLong(key, now + cd);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        long now = e.getServer().overworld().getGameTime();
        for (var it = PENDING.iterator(); it.hasNext(); ) {
            Slam s = it.next();
            if (now < s.when) continue;
            it.remove();
            if (!s.owner.isAlive()) continue;
            s.owner.teleportTo(s.at.x + s.dir.x * 1.4, s.at.y, s.at.z + s.dir.z * 1.4);
            for (LivingEntity t : targets(s.owner, s.owner.position(), 4.5)) {
                t.invulnerableTime = 0;
                CombatSettings.hurt(s.owner, t, s.owner.damageSources().playerAttack(s.owner), 45f);
                t.invulnerableTime = 0;
                t.getPersistentData().putLong(MARK, now + 600);
                if (t instanceof Mob m) character.npc.NpcControl.stun(m, "higuruma_verdict", 40);
                Vec3 push = t.position().subtract(s.owner.position()).normalize().scale(0.9);
                t.setDeltaMovement(push.x, 0.5, push.z); t.hurtMarked = true;
            }
            Network.send(s.level, new Network.Effect(FX_VERDICT, s.owner.getId(), s.owner.position(), s.dir, 4f, 20, 0));
        }
        // 被锁定的目标禁止位移
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                    p.getBoundingBox().inflate(40), x -> x.getPersistentData().getLong("HigurumaBind") > now)) {
                t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
                t.hurtMarked = true;
            }
        }
    }

    private static LivingEntity ray(ServerPlayer p, double len) {
        Vec3 eye = p.getEyePosition();
        LivingEntity best = null; double bd = len * len;
        for (LivingEntity t : targets(p, eye, len)) {
            var clip = t.getBoundingBox().inflate(.35).clip(eye, eye.add(p.getLookAngle().scale(len)));
            if (clip.isEmpty()) continue;
            double d = clip.get().distanceToSqr(eye);
            if (d < bd) { bd = d; best = t; }
        }
        return best;
    }

    private static List<LivingEntity> targets(ServerPlayer p, Vec3 at, double r) {
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r),
            t -> t != p && t.isAlive() && CombatSafety.canTarget(p, t));
    }

    private static String role(ServerPlayer p) {
        var d = character.CharacterService.selected(p);
        return d == null ? "" : d.id();
    }
    private static void msg(ServerPlayer p, String s) { p.displayClientMessage(net.minecraft.network.chat.Component.literal(s), true); }

    public static void testReset(ServerPlayer p) {
        var d = Curse.data(p);
        for (String k : new ArrayList<>(d.getAllKeys())) if (k.startsWith("HigurumaCD_")) d.remove(k);
    }

    private HigurumaExtras() { }
}
