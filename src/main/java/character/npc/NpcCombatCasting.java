package character.npc;

import cn.blockforge.generated.nnnnr1201forge2.NpcCombat;
import cn.blockforge.generated.nnnnr1201forge2.Network;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * NPC 的"自己的咒术"（r384/r385，用户要求：照秤金次/石流龙 NPC 的写法，**各自真会放术式**）。
 *
 * <p>不依赖 `NpcSkillDriver` 的假玩家路线：每 20 刻扫一遍已加载的这五类 NPC，
 * 按距离直接打出对应招式（`NpcCombat.hurt` 结算），并按冷却开 {@link NpcDomain}。
 */
public final class NpcCombatCasting {
    private static final String K_ATK = "NpcNextAttack", K_DOM = "NpcNextDomain";
    private static long counter;

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (++counter % 10 != 0) return;                 // 每 0.5 秒判定一次
        for (ServerLevel level : e.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (!player.isAlive() || player.isSpectator()) continue;
                List<Mob> mobs = level.getEntitiesOfClass(Mob.class,
                    new AABB(player.position(), player.position()).inflate(32), NpcCombatCasting::isNpc);
                for (Mob mob : mobs) {
                    if (!mob.isAlive() || mob.getTarget() != null && mob.getTarget() != player) continue;
                    cast(mob, player);
                }
            }
        }
    }

    private static boolean isNpc(Mob mob) {
        String id = mob.getType().toString();
        return id.contains("jogo_npc") || id.contains("uraume_npc") || id.contains("choso")
            || id.contains("ryu_npc") || id.contains("hakari_npc");
    }

    private static long now(Mob mob) { return mob.level().getGameTime(); }

    private static boolean ready(Mob mob, String key, long interval) {
        var data = mob.getPersistentData();
        long at = data.getLong(key);
        if (at > now(mob)) return false;
        data.putLong(key, now(mob) + interval);
        return true;
    }

    private static void cast(Mob mob, ServerPlayer player) {
        String id = mob.getType().toString();
        double distance = mob.distanceTo(player);
        boolean jackpot = mob.getPersistentData().getLong("HakariNpcJackpot") > now(mob);
        if (id.contains("jogo_npc")) {
            if (distance <= 6 && ready(mob, K_ATK, 60)) {
                hurtAround(mob, player, 4.5, 12);
                fire(mob, player, 0xFFFF6A1E);
            } else if (distance <= 18 && ready(mob, K_ATK, 80)) {
                hurtAround(mob, player, 6, 8);
                burn(player);
                fire(mob, player, 0xFFFFA33C);
            }
            if (ready(mob, K_DOM, 1200) && distance <= 24) NpcDomain.open(mob, 26);   // 盖棺铁围山
        } else if (id.contains("uraume_npc")) {
            if (distance <= 5 && ready(mob, K_ATK, 50)) {
                hurtAround(mob, player, 4, 10);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                fire(mob, player, 0xFFA8EEFF);
            } else if (distance <= 14 && ready(mob, K_ATK, 70)) {
                hurtAround(mob, player, 3, 12);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                fire(mob, player, 0xFFD6F6FF);
            }
        } else if (id.contains("choso")) {
            if (distance <= 5 && ready(mob, K_ATK, 40)) {
                hurtAround(mob, player, 4, 14);
                fire(mob, player, 0xFFD84362);
            } else if (distance <= 16 && ready(mob, K_ATK, 90)) {
                hurtAround(mob, player, 5, 10);
                fire(mob, player, 0xFFFF7AA0);
            }
        } else if (id.contains("ryu_npc")) {
            if (distance <= 14 && ready(mob, K_ATK, 60)) {
                hurtAround(mob, player, 5, 16);
                fire(mob, player, 0xFF9FDFFF);
            }
            if (ready(mob, K_DOM, 1200) && distance <= 24) NpcDomain.open(mob, 26);   // 打上花火
        } else if (id.contains("hakari_npc")) {
            if (distance <= 6 && ready(mob, K_ATK, 40)) {
                hurtAround(mob, player, 3.5, (int) (18 * (jackpot ? 2 : 1)));
                fire(mob, player, 0xFFFFC44D);
            } else if (distance <= 14 && ready(mob, K_ATK, 60)) {
                hurtAround(mob, player, 5, (int) (9 * (jackpot ? 2 : 1)));
                fire(mob, player, 0xFFE0E0E0);
            }
            if (ready(mob, K_DOM, 1200) && distance <= 24) NpcDomain.open(mob, 26);   // 坐杀博徒
        }
    }

    private static void hurtAround(Mob mob, ServerPlayer player, double radius, int damage) {
        var level = (ServerLevel) mob.level();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(mob.position(), mob.position()).inflate(radius + 1),
                t -> t != mob && t.isAlive() && !(t instanceof Mob m && isNpc(m)))) {
            if (living.distanceToSqr(mob.position()) <= radius * radius)
                NpcCombat.hurt(mob, living, damage);
        }
    }

    private static void burn(ServerPlayer player) {
        player.setSecondsOnFire(3);
    }

    private static void fire(Mob mob, ServerPlayer player, int color) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        Network.send(level, new Network.Effect(character.ryu.RyuFx.BURST, mob.getId(), mob.position(),
            player.position().subtract(mob.position()), 1.4f, 10, 0));
    }

    private NpcCombatCasting() {}
}
