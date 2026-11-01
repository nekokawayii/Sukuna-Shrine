package character.npc;

import cn.blockforge.generated.nnnnr1201forge2.*;
import character.yuta.*;

/** 乙骨的复制不打开玩家界面；从当前对手的旧术式池里选招并实际释放。 */
public final class YutaNpcCombat {
    public static void copy(NpcBridge.NpcActor actor){
        if(!(actor.mob instanceof net.minecraft.world.entity.Mob mob)||!RikaRules.active(actor))return;
        int[] pool=YutaCopies.poolFor(mob.getTarget());
        // 对手没有可复制术式时，使用已经掌握的解；不生成任意角色或领域编号。
        java.util.List<Integer> choices=new java.util.ArrayList<>();
        for(int id:pool)choices.add(id);
        if(choices.isEmpty())choices.add(0);
        CopyBorrow.begin(actor);
        try{
            for(int offset=0;offset<choices.size();offset++){
                int id=choices.get(Math.floorMod(actor.mob.tickCount/20+offset,choices.size()));
                if(Curse.active(actor,"YutaCopyCooldown_"+id))continue;
                if(YutaCopies.isCustom(id)){
                    var skill=character.CharacterRegistry.get("jogo").config().skill(id);
                    if(skill==null||!Curse.spend(actor,skill.cost()))continue;
                    character.CharacterCasting.castAs(actor,"jogo",id,character.CharacterService.rank(actor));
                    Curse.status(actor,"YutaCopyCooldown_"+id,YutaCopies.copiedCooldown(id));break;
                }
                int internal=AuthoritySkills.fromInput(id);
                if(!AuthoritySkills.ready(actor,internal))continue;
                AuthoritySkills.cast(actor,internal);YutaCopies.add(actor,id);
                Curse.status(actor,"YutaCopyCooldown_"+id,YutaCopies.copiedCooldown(id));
                break;
            }
        }finally{CopyBorrow.end(actor);}
    }
    private YutaNpcCombat() { }
}
