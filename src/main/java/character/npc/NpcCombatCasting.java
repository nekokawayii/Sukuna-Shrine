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
            || id.contains("ryu_npc") || id.contains("hakari_npc") || id.contains("naoya_npc") || id.contains("higuruma_npc") || id.contains("sukuna_npc") || id.contains("kashimo_npc");
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
        } else if (id.contains("kashimo_npc")) {
            // 鹿紫云一：幻兽琥珀全套（无冷却轮转），领域=静电立场/雷霆主场
            var kdata = mob.getPersistentData();
            int kstep = (kdata.getInt("KashimoNpcStep") + 1) % 11;
            kdata.putInt("KashimoNpcStep", kstep);
            switch (kstep) {
                case 0 -> { mugi(mob, player, 16); fire(mob, player, 0xFF7FD8FF); }   // 雷走
                case 1 -> { mugi(mob, player, 12); fire(mob, player, 0xFF9FE8FF); }   // 放电
                case 2 -> { mugi(mob, player, 10); fire(mob, player, 0xFFB8F0FF); }   // 电磁吸附
                case 3 -> { mugi(mob, player, 14); fire(mob, player, 0xFF7FFFD4); }   // 雷神·附着
                case 4 -> { mugi(mob, player, 22); fire(mob, player, 0xFFFFF27F); }   // 引爆
                case 5 -> { mugi(mob, player, 8);  fire(mob, player, 0xFFA8D8FF); }   // 静电立场
                case 6 -> { mugi(mob, player, 30); fire(mob, player, 0xFFFFFFFF); }   // 落雷
                case 7 -> { mugi(mob, player, 40); fire(mob, player, 0xFFBFEFFF); }   // 球形闪电
                case 8 -> { mugi(mob, player, 26); fire(mob, player, 0xFF8FD6FF); }   // 雷霆万钧
                case 9 -> { mugi(mob, player, 18); fire(mob, player, 0xFFD0F0FF); }   // 雷影化
                default -> { mugi(mob, player, 34); fire(mob, player, 0xFFFFE066); }  // 终奥·幻兽琥珀
            }
            if (ready(mob, K_DOM, 200) && distance <= 30)
                NpcDomain.open(mob, 26, NpcDomain.Style.GENERIC);   // 静电/雷霆主场
        } else if (id.contains("sukuna_npc")) {
            // 宿傩的全部术式（无冷却，每 10 刻按招表轮转），领域固定「伏魔御厨子」
            var data = mob.getPersistentData();
            int step = (data.getInt("SukunaNpcStep") + 1) % 12;
            data.putInt("SukunaNpcStep", step);
            switch (step) {
                case 0 -> { mugi(mob, player, 20); fire(mob, player, 0xFFE34D65); }            // 解·远程斩击
                case 1 -> { mugi(mob, player, 8);  fire(mob, player, 0xFFFF9FB0); }            // 解·八方罗网（多段）
                case 2 -> { mugi(mob, player, 34); fire(mob, player, 0xFFD62E4B); }            // 捌·必断斩击
                case 3 -> { mugi(mob, player, 16); fire(mob, player, 0xFFFF7A3D); }            // 开·炎弓
                case 4 -> { mugi(mob, player, 28); fire(mob, player, 0xFFFFB347); }            // 灶·粉尘爆炸
                case 5 -> { mugi(mob, player, 12); fire(mob, player, 0xFFB14CFF); }            // 灵魂剥离
                case 6 -> { mugi(mob, player, 22); fire(mob, player, 0xFFFFC0C8); }            // 解·滞留（八方罗网分支）
                case 7 -> { mugi(mob, player, 30); fire(mob, player, 0xFFB3123C); }            // 捌·滞留
                case 8 -> { mugi(mob, player, 46); fire(mob, player, 0xFFFFD700); }            // 灶·最大功率·开
                case 9 -> { mugi(mob, player, 18); fire(mob, player, 0xFFFFFFFF); }            // 斩击环（旋转环斩）
                case 10 -> { mugi(mob, player, 26); fire(mob, player, 0xFFFF4F65); }           // 咒力附拳
                default -> { mugi(mob, player, 36); fire(mob, player, 0xFF8B0000); }           // 神龛（御厨子内部斩击）
            }
            if (ready(mob, K_DOM, 200) && distance <= 32)
                NpcDomain.open(mob, 40, NpcDomain.Style.GENERIC);   // 伏魔御厨子（半径 40，必中斩击）
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

    private static int ownerCounter(Mob mob) {
        var data = mob.getPersistentData();
        int c = data.getInt("SukunaNpcDomainIdx") + 1;
        data.putInt("SukunaNpcDomainIdx", c);
        return c;
    }

    /** 宿傩 NPC 的斩击/爆炸：对目标点周围结算一次伤害。 */
    private static void mugi(Mob mob, ServerPlayer target, int damage) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(target.position(), target.position()).inflate(3), t -> t.isAlive() && t != mob))
            NpcCombat.hurt(mob, living, damage);
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
