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
            || id.contains("ryu_npc") || id.contains("hakari_npc") || id.contains("naoya_npc") || id.contains("higuruma_npc");
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
            if (ready(mob, K_DOM, 1200) && distance <= 24) NpcDomain.open(mob, 27, NpcDomain.Style.LAVA);   // 盖棺铁围山（与玩家同半径 27）
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
            if (ready(mob, K_DOM, 1400) && distance <= 24) NpcDomain.open(mob, 22, NpcDomain.Style.ICE);
        } else if (id.contains("choso")) {
            if (distance <= 5 && ready(mob, K_ATK, 40)) {
                hurtAround(mob, player, 4, 14);
                fire(mob, player, 0xFFD84362);
            } else if (distance <= 16 && ready(mob, K_ATK, 90)) {
                hurtAround(mob, player, 5, 10);
                fire(mob, player, 0xFFFF7AA0);
            }
            if (ready(mob, K_DOM, 1200) && distance <= 24)
                NpcDomain.open(mob, 26, NpcDomain.Style.BLOOD);     // 胎海·赤血领域
        } else if (id.contains("ryu_npc")) {
            if (distance <= 14 && ready(mob, K_ATK, 60)) {
                hurtAround(mob, player, 5, 16);
                fire(mob, player, 0xFF9FDFFF);
            }
            if (ready(mob, K_DOM, 1200) && distance <= 24) NpcDomain.open(mob, 35, NpcDomain.Style.FIREWORK);   // 打上花火（半径与玩家一致 35）
        } else if (id.contains("higuruma_npc")) {
            if (distance <= 6 && ready(mob, K_ATK, 50)) {
                if (NpcDomain.deathMark(mob, player)) { player.setHealth(0); }
                else hurtAround(mob, player, 4, 14);
                fire(mob, player, 0xFFC9A227);
            }
            if (ready(mob, K_DOM, 1200) && distance <= 24)
                NpcDomain.open(mob, 30, NpcDomain.Style.HIGURUMA);   // 诛伏赐死
        } else if (id.contains("naoya_npc")) {
            if (distance <= 6 && ready(mob, K_ATK, 45)) {
                hurtAround(mob, player, 4, 16);                        // 直线投射
                fire(mob, player, 0xFF7FD8FF);
            } else if (distance <= 16 && ready(mob, K_ATK, 70)) {
                hurtAround(mob, player, 5, 10);                        // Z 字折返
                fire(mob, player, 0xFFB8EEFF);
            }
            if (ready(mob, K_DOM, 1200) && distance <= 24) NpcDomain.open(mob, 30, NpcDomain.Style.NAOYA);
        } else if (id.contains("hakari_npc")) {
            if (distance <= 6 && ready(mob, K_ATK, 40)) {
                hurtAround(mob, player, 3.5, (int) (18 * (jackpot ? 2 : 1)));
                rollSlot(mob);
                fire(mob, player, 0xFFFFC44D);
            } else if (distance <= 14 && ready(mob, K_ATK, 60)) {
                hurtAround(mob, player, 5, (int) (9 * (jackpot ? 2 : 1)));
                rollSlot(mob);
                fire(mob, player, 0xFFE0E0E0);
            }
            if (ready(mob, K_DOM, 1200) && distance <= 24) NpcDomain.open(mob, 30, NpcDomain.Style.PACHINKO);   // 坐杀博徒（与玩家同半径 30）
        }
    }

    /** 秤金次 NPC 的抽奖：每次攻击命中 +1 格，满 30 格中奖 4 分 11 秒。 */
    private static void rollSlot(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        var data = mob.getPersistentData();
        int luck = data.getInt("HakariNpcLuck") + 1;
        if (luck >= 30) {
            luck = 0;
            data.putLong("HakariNpcJackpot", level.getGameTime() + 5020);
            mob.heal(mob.getMaxHealth());
            Network.send(level, new Network.Effect(character.hakari.HakariFx.JACKPOT, mob.getId(),
                mob.position(), Vec3.ZERO, 1f, 5020, 0));
        }
        data.putInt("HakariNpcLuck", luck);
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
