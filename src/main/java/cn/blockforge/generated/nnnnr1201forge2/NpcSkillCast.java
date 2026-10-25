package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/**
 * NPC「借用玩家技能」的接口层（第一版）。
 * <p>思路：NPC 不复制玩家技能代码，而是按“角色 + 技能号”查同一份数值表（JogoSkills / BloodRules / BranchRules），
 * 用 {@link NpcCurse} 扣咒力、用 {@link NpcCombat} 把伤害归属到 NPC 自己，并复用玩家侧的特效号。
 * 这样以后把 Skills/BloodArts 里对 ServerPlayer 的依赖抽出来后，只需替换本类内部实现，NPC 调用方不用改。</p>
 */
public final class NpcSkillCast {

    /** 玩家/NPC 通用施法者上下文（后续把 CharacterCasting 的 ServerPlayer 参数替换成它）。 */
    public record Caster(Mob body, String character, int rank) {
        public static Caster of(LivingEntity npc, String character) {
            if (!(npc instanceof Mob mob)) return new Caster(null, character, 1);
            return new Caster(mob, character, Math.max(1, (int) (mob.getMaxHealth() / 300.0)));
        }
    }

    /** 接口版入口：技能实现只依赖 character.SkillCaster，生物/玩家都能传进来。 */
    public static boolean cast(character.SkillCaster caster, int skill, LivingEntity target) {
        if (!(caster.body() instanceof Mob mob)) return false;
        return cast(Caster.of(mob, caster.character().isEmpty() ? "jogo" : caster.character()), skill, target);
    }

    /** 施放一次技能：资格/冷却由调用方（NPC AI）负责，这里只管咒力、伤害归属与表现。 */
    public static boolean cast(Caster caster, int skill, LivingEntity target) {
        Mob npc = caster.body();
        if (npc == null || !npc.isAlive() || target == null || !target.isAlive()) return false;
        double cost = cost(caster.character(), skill);
        if (!NpcCurse.spend(npc, cost)) return false;
        if (!(npc.level() instanceof ServerLevel level)) return false;
        Vec3 eye = npc.getEyePosition();
        Vec3 to = target.getBoundingBox().getCenter();
        Vec3 dir = to.subtract(eye).normalize();
        switch (caster.character()) {
            case "jogo" -> jogo(level, npc, skill, eye, to, dir);
            case "choso" -> choso(level, npc, skill, eye, to, dir);
            case "uraume" -> uraume(level, npc, skill, eye, to, dir);
            default -> { return false; }
        }
        return true;
    }

    private static double cost(String character, int skill) {
        return switch (character) {
            case "jogo" -> skill == 1003 ? 200 : 30;
            case "choso" -> 25;
            case "uraume" -> 40;
            default -> 20;
        };
    }

    // ---------------------------------------------------------------- 漏壶 · 灾厄火焰
    private static void jogo(ServerLevel level, Mob npc, int skill, Vec3 eye, Vec3 to, Vec3 dir) {
        switch (skill) {
            case 1000 -> { // 火砾虫
                Network.send(level, new Network.Effect(character.jogo.JogoFx.BUG, npc.getId(), eye, dir, 1f, 20, 0));
                for (var t : players(level, npc, 24, t -> t.position().subtract(npc.position()).normalize().dot(dir) > 0.8))
                    NpcCombat.hurt(npc, t, 6f);
            }
            case 1001 -> { // 陨石
                Network.send(level, new Network.Effect(character.jogo.JogoFx.METEOR_IMPACT, npc.getId(), to, Vec3.ZERO, 4f, 16, 0));
                for (var t : players(level, npc, 0, t -> t.position().distanceToSqr(to) <= 20))
                    NpcCombat.hurt(npc, t, 12f);
            }
            case 1002 -> { // 岩浆（火焰喷发）
                Network.send(level, new Network.Effect(character.jogo.JogoFx.PILLAR, npc.getId(), eye, dir, 2f, 14, 0));
                for (var t : players(level, npc, 18, t -> t.position().subtract(npc.position()).normalize().dot(dir) > 0.45))
                    NpcCombat.hurt(npc, t, 7f);
            }
            case 1003 -> { // 领域
                Network.send(level, new Network.Effect(character.jogo.JogoFx.DOMAIN, npc.getId(), npc.position(), Vec3.ZERO, 27f, 600, 0));
                level.playSound(null, npc.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.6f, 0.45f);
            }
            case 1004 -> { // 火山喷发
                Network.send(level, new Network.Effect(character.jogo.JogoFx.PILLAR, npc.getId(), eye, dir, 2f, 16, 0));
                for (var t : players(level, npc, 40, t -> Math.abs(t.position().subtract(eye).normalize().dot(dir) - 1) < 0.06))
                    NpcCombat.hurt(npc, t, 10f);
            }
            case 1005 -> { // 岩浆陷阱
                for (int i = 0; i < 5; i++) {
                    Vec3 at = to.add((npc.getRandom().nextDouble() - .5) * 6, 0, (npc.getRandom().nextDouble() - .5) * 6);
                    Network.send(level, new Network.Effect(character.jogo.JogoFx.PILLAR, npc.getId(), at, Vec3.ZERO, 1.5f, 12, 0));
                    for (var t : players(level, npc, 0, t -> t.position().distanceToSqr(at) <= 9)) {
                        NpcCombat.hurt(npc, t, 7f);
                        t.setDeltaMovement(t.getDeltaMovement().add(0, .8, 0));
                        t.hurtMarked = true;
                    }
                }
            }
            default -> { // 1006 火山灰隐身
                npc.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.INVISIBILITY, 100, 0, false, false));
                npc.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 100, 2, false, false));
                Network.send(level, new Network.Effect(character.jogo.JogoFx.BUG_BURST, npc.getId(), npc.position(), Vec3.ZERO, 2f, 10, 0));
            }
        }
    }

    // ---------------------------------------------------------------- 胀相 · 赤血操术（简化映射到 20 招编号 0..19）
    private static void choso(ServerLevel level, Mob npc, int skill, Vec3 eye, Vec3 to, Vec3 dir) {
        int idx = Math.max(0, Math.min(19, skill));
        if (idx == 14 || idx == 15) {   // 胎海 / 死胎星
            Network.send(level, new Network.Effect(412, npc.getId(), npc.position(), Vec3.ZERO, idx == 14 ? 30 : 20, 60, 0));
            for (var t : players(level, npc, idx == 14 ? 30 : 20, t -> true))
                NpcCombat.hurt(npc, t, idx == 14 ? 10f : 40f);
            return;
        }
        Network.send(level, new Network.Effect(400 + idx, npc.getId(), idx == 3 ? eye : to, dir, 3f, 12, 0));
        for (var t : players(level, npc, 18, t -> true)) NpcCombat.hurt(npc, t, 8f + idx);
    }

    // ---------------------------------------------------------------- 里梅 · 冰凝咒法
    private static void uraume(ServerLevel level, Mob npc, int skill, Vec3 eye, Vec3 to, Vec3 dir) {
        if (skill == BranchRules.FROST) {
            Network.send(level, new Network.Effect(BranchRules.FX_FROST, npc.getId(), eye, dir, 14, 10, 0));
            for (var t : players(level, npc, 14, t -> t.position().subtract(npc.position()).normalize().dot(dir) > 0.55)) {
                NpcCombat.hurt(npc, t, 12f);
                t.setTicksFrozen(Math.min(600, t.getTicksFrozen() + 200));
            }
        } else {
            Network.send(level, new Network.Effect(BranchRules.FX_ICE, npc.getId(), to.add(0, 10, 0), new Vec3(0, -1, 0), 3, 20, 0));
            for (var t : players(level, npc, 0, t -> t.position().distanceToSqr(to) <= 12)) {
                NpcCombat.hurt(npc, t, 16f);
                t.setTicksFrozen(Math.min(600, t.getTicksFrozen() + 120));
            }
        }
    }

    private static List<Player> players(ServerLevel level, Mob npc, double range, java.util.function.Predicate<Player> filter) {
        double r = range <= 0 ? 4.0 : range;
        return level.getEntitiesOfClass(Player.class, new AABB(npc.position(), npc.position()).inflate(r),
            p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && filter.test(p));
    }

    private NpcSkillCast() {}
}
