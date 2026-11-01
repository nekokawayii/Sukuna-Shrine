package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** 旧入口兼容：不再复制伤害或仅遍历 Player，统一走真实技能和资源门槛。 */
public final class NpcSkillCast {
    public record Caster(Mob body,String character,int rank){
        public static Caster of(LivingEntity npc,String role){return new Caster(npc instanceof Mob m?m:null,role,1);}
    }
    public static boolean cast(character.SkillCaster caster,int skill,LivingEntity target){
        return caster.body() instanceof Mob mob&&character.TechniqueApi.cast(mob,target,skill);
    }
    public static boolean cast(Caster caster,int skill,LivingEntity target){
        if(caster==null||caster.body()==null)return false;
        String role=caster.character().equals("choso")?"blood":caster.character();
        if(!role.equals(NpcBridge.character(caster.body())))return false;
        return character.TechniqueApi.cast(caster.body(),target,skill);
    }
    private NpcSkillCast() { }
}
