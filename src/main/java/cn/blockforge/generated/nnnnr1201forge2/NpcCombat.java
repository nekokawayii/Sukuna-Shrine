package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** 单次、可取消的伤害入口；不再用无来源伤害绕过友军/其他模组保护。 */
public final class NpcCombat {
    public static boolean hurt(Mob caster,LivingEntity target,float amount){
        if(caster==null||!Float.isFinite(amount)||amount<=0||!NpcBridge.canHit(caster,target)
            ||DomainClash.active(caster)||DomainClash.active(target)||DomainClash.stunned(caster))return false;
        var actor=NpcBridge.actorFor(caster);
        AttackTypes.mark(AttackTypes.MAGIC);
        try{
            return actor!=null?CombatSettings.hurt(actor,target,caster.damageSources().mobAttack(caster),amount)
                :target.hurt(caster.damageSources().mobAttack(caster),amount);
        }finally{AttackTypes.clear();}
    }
    private NpcCombat() { }
}
