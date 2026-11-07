package character.nanami;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import cn.blockforge.generated.nnnnr1201forge2.CombatSafety;
import cn.blockforge.generated.nnnnr1201forge2.CombatSettings;
import cn.blockforge.generated.nnnnr1201forge2.Curse;
import cn.blockforge.generated.nnnnr1201forge2.Network;

/** 七海建人新三招（r342）：飞刃 / 震地 / 处刑。 */
public final class NanamiExtras {
    public static final int BLADES = 2604, QUAKE = 2605, EXECUTE = 2606;
    public static final int FX_BLADES = 386, FX_QUAKE = 387, FX_EXECUTE = 388;

    private record Delayed(ServerPlayer owner, ServerLevel level, LivingEntity target, long at, int kind) { }
    private static final List<Delayed> DELAYED = new ArrayList<>();

    public static void cast(ServerPlayer p, int skill) {
        if (!(p.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        String key = "NanamiCD_" + skill;
        if (Curse.data(p).getLong(key) > now) {
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("冷却中 · " + ((Curse.data(p).getLong(key) - now + 19) / 20) + " 秒"), true);
            return;
        }
        int cost = switch (skill) { case BLADES -> 120; case QUAKE -> 180; case EXECUTE -> 260; default -> 0; };
        if (!Curse.spend(p, cost)) { p.displayClientMessage(net.minecraft.network.chat.Component.literal("咒力不足"), true); return; }
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        boolean ok = true;
        switch (skill) {
            case BLADES -> { // 扇形刀光，命中即强制 7:3 弱点
                int hits = 0;
                for (LivingEntity t : targets(p, eye.add(look.scale(8)), 9)) {
                    Vec3 to = t.position().add(0, t.getBbHeight() * .5, 0).subtract(eye);
                    if (to.length() > 16 || to.normalize().dot(look) < Math.cos(Math.toRadians(35))) continue;
                    t.invulnerableTime = 0;
                    CombatSettings.hurt(p, t, p.damageSources().playerAttack(p), 14f);
                    t.invulnerableTime = 0;
                    NanamiRatio.mark(p, t);
                    hits++;
                    if (hits >= 5) break;
                }
                Network.send(level, new Network.Effect(FX_BLADES, p.getId(), eye, look, 1.2f, 16, 0));
                if (hits == 0) { Curse.addRaw(p, cost); ok = false; p.displayClientMessage(net.minecraft.network.chat.Component.literal("飞刃：前方扇形内没有目标"), true); }
            }
            case QUAKE -> { // 防御反击后砸地：范围击飞 + 眩晕 1.5 秒
                for (LivingEntity t : targets(p, p.position(), 4.5)) {
                    t.invulnerableTime = 0;
                    CombatSettings.hurt(p, t, p.damageSources().playerAttack(p), 30f);
                    t.invulnerableTime = 0;
                    Vec3 push = t.position().subtract(p.position()).normalize().scale(1.2);
                    t.setDeltaMovement(push.x, 0.85, push.z);
                    t.hurtMarked = true;
                    if (t instanceof Mob m) character.npc.NpcControl.stun(m, "nanami_quake", 30);
                }
                Network.send(level, new Network.Effect(FX_QUAKE, p.getId(), p.position(), look, 4.5f, 24, 0));
            }
            case EXECUTE -> { // 对 30% 血以下且带弱点的目标处刑
                LivingEntity target = null; double best = 12 * 12;
                for (LivingEntity t : targets(p, p.position(), 12)) {
                    if (!NanamiRatio.marked(p, t)) continue;
                    if (t.getHealth() > t.getMaxHealth() * 0.30f) continue;
                    double d = t.position().distanceToSqr(p.position());
                    if (d < best) { best = d; target = t; }
                }
                if (target == null) { Curse.addRaw(p, cost); ok = false; p.displayClientMessage(net.minecraft.network.chat.Component.literal("处刑：12 格内没有残血且带弱点的目标"), true); }
                else {
                    Vec3 at = target.position();
                    Vec3 dir = at.subtract(p.position()).normalize().scale(1.2);
                    p.teleportTo(at.x - dir.x, at.y, at.z - dir.z);
                    float base = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    float dmg = Math.max(30f, base * 3f);
                    target.invulnerableTime = 0;
                    var holder = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL);
                    target.hurt(new net.minecraft.world.damagesource.DamageSource(holder, p, p), dmg);
                    target.invulnerableTime = 0;
                    Network.send(level, new Network.Effect(FX_EXECUTE, p.getId(), at.add(0, 1, 0), p.getLookAngle(), 1.5f, 20, 0));
                    if (!target.isAlive()) {
                        Curse.data(p).putLong(key, 0); // 击杀刷新冷却
                        p.displayClientMessage(net.minecraft.network.chat.Component.literal("处刑成功 · 冷却刷新"), true);
                    }
                }
            }
            default -> ok = false;
        }
        if (ok && Curse.data(p).getLong(key) <= now) {
            int cd = switch (skill) { case BLADES -> 100; case QUAKE -> 160; case EXECUTE -> 240; default -> 100; };
            Curse.data(p).putLong(key, now + cd);
        }
    }

    private static List<LivingEntity> targets(ServerPlayer p, Vec3 at, double r) {
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r),
            t -> t != p && t.isAlive() && CombatSafety.canTarget(p, t));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        long now = e.getServer().overworld().getGameTime();
        for (var it = DELAYED.iterator(); it.hasNext(); ) {
            Delayed d = it.next();
            if (now < d.at) continue;
            it.remove();
        }
    }

    public static void testReset(ServerPlayer p) {
        var d = Curse.data(p);
        for (String k : new ArrayList<>(d.getAllKeys())) if (k.startsWith("NanamiCD_")) d.remove(k);
    }

    private NanamiExtras() { }
}
