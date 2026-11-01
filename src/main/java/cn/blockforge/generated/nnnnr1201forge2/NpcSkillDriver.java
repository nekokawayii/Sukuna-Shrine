package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** 旧调用兼容：cost 参数仅保留源码兼容，费用以玩家技能定义为准，不能重复扣费。 */
public final class NpcSkillDriver {
    public static NpcBridge.NpcActor actorFor(Mob mob,LivingEntity target){
        if(mob==null||!NpcBridge.canHit(mob,target))return null;
        mob.setTarget(target);var actor=NpcBridge.actorFor(mob);if(actor!=null)actor.syncPosition();return actor;
    }
    public static boolean blood(Mob mob,LivingEntity target,int id,double cost){return character.TechniqueApi.cast(mob,target,id);}
    public static boolean ice(Mob mob,LivingEntity target,int id,double cost){return character.TechniqueApi.cast(mob,target,id);}
    public static boolean fire(Mob mob,LivingEntity target,int id,int rank,double cost){return character.TechniqueApi.cast(mob,target,id);}
    private NpcSkillDriver() { }
}
