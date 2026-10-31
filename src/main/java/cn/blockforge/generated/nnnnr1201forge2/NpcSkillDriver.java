package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * NPC 使用「玩家同款技能」的驱动（按《NPC使用玩家技能与自定义技能通用写法》路线）：
 * <p>用 {@link MaidBridge#actor(LivingEntity)} 取该生物专属的假玩家施术上下文 → 先设目标、同步位置
 * → 交回原技能的服务端实现（{@link BloodArts#automatic} / {@link Uraume#cast} / 漏壶技能）。
 * 咒力与冷却仍记在生物自己身上（{@link NpcCurse}），不借附近真人。
 * <p>原技能如有角色/章节门禁而拒绝执行时返回 false，由调用方回退到该生物自己的 boss 式实现。
 */
public final class NpcSkillDriver {

    /** 取（或创建）该生物的施术上下文，并把目标/位置同步好。 */
    public static MaidBridge.Actor actorFor(Mob mob, LivingEntity target) {
        if (mob == null || mob.level().isClientSide) return null;
        MaidBridge.Actor actor = MaidBridge.actor(mob);
        if (actor == null) return null;
        actor.aim = target;
        actor.syncPosition();
        return actor;
    }

    /** 赤血操术：交给 BloodArts 自动施法（会自行完成百敛蓄力等状态机）。 */
    public static boolean blood(Mob mob, LivingEntity target, int id, double cost) {
        MaidBridge.Actor actor = actorFor(mob, target);
        if (actor == null || target == null) return false;
        if (!NpcCurse.spend(mob, cost)) return false;
        if (!BloodArts.available(actor, id)) { NpcCurse.add(mob, cost); return false; }
        try {
            BloodArts.automatic(actor, id);
            return true;
        } catch (Throwable t) {
            com.mojang.logging.LogUtils.getLogger().warn("[NPC技能] 赤血 {} 交给玩家实现失败：{}", id, t.toString());
            NpcCurse.add(mob, cost);
            return false;
        }
    }

    /** 冰凝咒法：交给 Uraume 原实现（霜凪 / 直瀑）。 */
    public static boolean ice(Mob mob, LivingEntity target, int skill, double cost) {
        MaidBridge.Actor actor = actorFor(mob, target);
        if (actor == null || target == null) return false;
        if (!NpcCurse.spend(mob, cost)) return false;
        if (!Uraume.available(actor, skill)) { NpcCurse.add(mob, cost); return false; }
        try {
            actor.setYRot((float) (Math.toDegrees(Math.atan2(
                target.getZ() - mob.getZ(), target.getX() - mob.getX())) - 90));
            Uraume.cast(actor, skill);
            return true;
        } catch (Throwable t) {
            com.mojang.logging.LogUtils.getLogger().warn("[NPC技能] 冰凝 {} 交给玩家实现失败：{}", skill, t.toString());
            NpcCurse.add(mob, cost);
            return false;
        }
    }

    /** 漏壶灾厄火焰：按技能号直接调用 JogoCombat 的原实现（绕过需要真人角色的入口检查）。 */
    public static boolean fire(Mob mob, LivingEntity target, int skillId, int rank, double cost) {
        MaidBridge.Actor actor = actorFor(mob, target);
        if (actor == null || target == null) return false;
        var skill = character.jogo.JogoSkills.all().stream().filter(s -> s.id() == skillId).findFirst().orElse(null);
        if (skill == null) return false;
        if (!NpcCurse.spend(mob, cost)) return false;
        try {
            switch (skillId) {
                case character.jogo.JogoSkills.FIRE_BUG -> character.jogo.JogoCombat.castBugs(actor, skill, rank);
                case character.jogo.JogoSkills.METEOR -> character.jogo.JogoCombat.castMeteors(actor, skill, rank);
                case character.jogo.JogoSkills.LAVA_BURST -> character.jogo.JogoCombat.castPillars(actor, skill, rank);
                case character.jogo.JogoSkills.DOMAIN -> character.jogo.JogoCombat.castDomain(actor, skill, rank);
                case character.jogo.JogoSkills.VOLCANO_BEAM -> character.jogo.JogoCombat.castVolcanoBeam(actor, skill, rank);
                case character.jogo.JogoSkills.MAGMA_TRAP -> character.jogo.JogoCombat.castMagmaTrap(actor, skill, rank);
                case character.jogo.JogoSkills.ASH_VEIL -> character.jogo.JogoCombat.castAshVeil(actor, skill, rank);
                default -> { return false; }
            }
            return true;
        } catch (Throwable t) {
            com.mojang.logging.LogUtils.getLogger().warn("[NPC技能] 火焰 {} 交给玩家实现失败：{}", skillId, t.toString());
            NpcCurse.add(mob, cost);
            return false;
        }
    }

    private NpcSkillDriver() {}
}
