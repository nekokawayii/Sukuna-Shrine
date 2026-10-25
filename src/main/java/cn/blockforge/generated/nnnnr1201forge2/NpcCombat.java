package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** NPC 生物的统一伤害入口：伤害来源归属该生物，并被 CombatSafety 认可为"敌方生物攻击"。
 *  依次尝试 mobAttack → magic → generic，任意一种成交即返回，避免被某条保护链整段拦掉。 */
public final class NpcCombat {
    public static boolean hurt(Mob caster, LivingEntity target, float amount) {
        if (target == null || !target.isAlive() || amount <= 0) return false;
        AttackTypes.mark(AttackTypes.MAGIC);
        try {
            if (target.hurt(caster.damageSources().mobAttack(caster), amount)) return true;
            if (target.hurt(caster.damageSources().magic(), amount)) return true;
            return target.hurt(caster.damageSources().generic(), amount);
        } finally {
            AttackTypes.clear();
        }
    }
    private NpcCombat() {}
}
