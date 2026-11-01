package character.npc;

import cn.blockforge.generated.nnnnr1201forge2.*;
import character.CharacterCasting;
import character.CharacterRegistry;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** NPC 独立调度玩家同款术式；没有真人在场也会运行。 */
public final class NpcCombatCasting {
    public static List<Integer> skills(Mob mob) {
        String role = NpcBridge.character(mob);
        if (mob instanceof NpcTechniqueUser user) {
            var loadout=user.npcTechniques();
            if(loadout!=null&&loadout.role().equals(role))return loadout.skills();
        }
        return role.isEmpty()?List.of():NpcLoadout.all(role).skills();
    }
    /** 对外入口同样执行冷却与开场门槛，不能通过外部 AI 绕过。 */
    public static boolean cast(Mob mob, LivingEntity target, int id) {
        if (mob == null || mob.isNoAi() || !mob.isAlive() || !NpcBridge.canHit(mob,target)) return false;
        var actor = NpcBridge.actorFor(mob);
        if (actor == null || !skills(mob).contains(id)) return false;
        if(actor.characterId.equals("mahito")&&!character.mahito.MahitoCombat.ready(actor,target,id))return false;
        mob.setTarget(target); actor.syncPosition();
        if (DomainClash.active(mob) || DomainClash.stunned(mob) || Curse.active(actor,"NpcCastInterval")) return false;
        boolean domain = character.mainline.Mainline.domainSkill(actor,id);
        if (domain && (!domainReady(mob) || DomainClash.hasDomain(actor))) return false;
        if(domain&&!DomainClash.counterCasting(actor))return DomainClash.counterCast(actor,()->cast(mob,target,id));
        if (actor.characterId.equals("toji")) {
            if (!character.toji.TojiNpcCombat.cast(actor,id)) return false;
            return committed(actor,false);
        }
        if (!Curse.canUseTechnique(actor) || AuthoritySkills.busy(actor)) return false;
        if (actor.characterId.equals("hakari") && domain && character.hakari.HakariCombat.jackpot(actor)) return false;
        if (actor.characterId.equals("blood") && id == BloodRules.DEATH_STAR
            && !NpcBattleRules.deathStarReady(mob.getHealth(),mob.getMaxHealth(),BloodDomain.active(actor))) return false;
        if (actor.characterId.equals("gojo") && id>=34 && id<34+GojoPlayer.SLOTS) {
            int internal=100+GojoPlayer.WHEEL[id-34];
            if (!AuthoritySkills.ready(actor,internal)) return false;
            AuthoritySkills.cast(actor,internal);return committed(actor,domain);
        }
        if(actor.characterId.equals("yuta")){
            if(id==character.yuta.YutaSkills.RIKA&&RikaRules.active(actor))return false;
            if(id==character.yuta.YutaSkills.COPY&&!RikaRules.active(actor))return false;
            if(id==character.yuta.YutaSkills.SURGERY&&mob.getHealth()>=mob.getMaxHealth()*.8f)return false;
        }
        if (character.CharacterSkill.custom(id)) {
            var def=CharacterRegistry.get(actor.characterId);var skill=def.config().skill(id);
            if (skill==null || Curse.active(actor,"CharacterSkillCooldown_"+id)
                || Curse.active(actor,"CharacterGlobalCooldown") || !Curse.canSpend(actor,skill.cost())) return false;
            if (actor.characterId.equals("naoya") && id==character.naoya.NaoyaSkills.RELEASE && Curse.active(actor,"NpcProjectionUntil")) return false;
            if (actor.characterId.equals("ryu")&&id==character.ryu.RyuSkills.BLAST) {
                if(!Curse.spend(actor,skill.cost()))return false;
                character.ryu.RyuCombat.startCharge(actor);Curse.status(actor,"NpcChannelUntil",80);
                Curse.status(actor,"CharacterSkillCooldown_"+id,skill.cooldown());
            } else CharacterCasting.cast(actor,id);
            if(!Curse.active(actor,"CharacterSkillCooldown_"+id))return false;
            if(actor.characterId.equals("naoya")&&id==character.naoya.NaoyaSkills.PROJECT)Curse.status(actor,"NpcProjectionUntil",60);
            return committed(actor,domain);
        }
        if(!AuthoritySkills.ready(actor,id))return false;
        AuthoritySkills.cast(actor,id);
        return committed(actor,domain);
    }
    private static boolean committed(NpcBridge.NpcActor actor,boolean domain) {
        Curse.status(actor,"NpcCastInterval",NpcBattleRules.CAST_INTERVAL);
        if(domain)actor.mob.getPersistentData().putLong("NpcDomainReadyAt",actor.level().getGameTime()+NpcBattleRules.DOMAIN_COOLDOWN);
        actor.mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return true;
    }
    public static boolean domainReady(Mob mob) {
        var actor=NpcBridge.actorFor(mob);if(actor==null)return false;
        var d=mob.getPersistentData();
        return NpcBattleRules.domainReady(mob.level().getGameTime(),d.getLong("NpcDomainReadyAt"),
            d.getInt("NpcCombatTicks"),mob.getHealth()/mob.getMaxHealth(),DomainClash.inEnemyDomain(actor));
    }
    private static void repair(NpcBridge.NpcActor actor) {
        if(!Curse.burnout(actor)){Curse.data(actor).remove("NpcRepairAt");return;}
        if(DomainClash.penalized(actor)||Curse.active(actor,"FinaleBurnout"))return;
        var d=Curse.data(actor);long now=actor.level().getGameTime();
        if(!d.contains("NpcRepairAt")){d.putLong("NpcRepairAt",now+NpcBattleRules.REPAIR_DELAY);return;}
        if(now>=d.getLong("NpcRepairAt")){Curse.repairBurnout(actor);d.putLong("NpcRepairAt",now+40);}
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent e) {
        if (!(e.getEntity() instanceof Mob mob) || mob.level().isClientSide || !NpcBridge.supports(mob)
            || !mob.isAlive() || mob.isNoAi()) return;
        var actor=NpcBridge.actorFor(mob);
        if(DomainClash.active(mob)||DomainClash.stunned(mob)){DomainClash.hold(mob);return;}
        var data=mob.getPersistentData();LivingEntity target=mob.getTarget();
        if(!NpcBridge.canHit(mob,target)||mob.distanceToSqr(target)>64*64){data.putInt("NpcCombatTicks",0);return;}
        data.putInt("NpcCombatTicks",Math.min(12000,data.getInt("NpcCombatTicks")+1));
        repair(actor);
        if(actor.characterId.equals("mahito")){
            if(mob.tickCount%5==0&&mob.hurtTime>0&&cast(mob,target,character.mahito.MahitoSkills.DODGE))return;
            if(mob.tickCount%10==0&&mob.getHealth()<mob.getMaxHealth()*.55f&&cast(mob,target,character.mahito.MahitoSkills.REFORGE))return;
            if(character.domainking.SelfEmbryoDomain.active(actor)&&mob.tickCount%30==0&&character.mahito.MahitoCombat.touch(actor,target))return;
        }
        if(actor.characterId.equals("ryu")&&Curse.data(actor).contains("NpcChannelUntil")){
            if(!Curse.active(actor,"NpcChannelUntil")){character.ryu.RyuCombat.testReset(actor);Curse.data(actor).remove("NpcChannelUntil");}
            return;
        }
        if(mob.tickCount%10!=0||AuthoritySkills.busy(actor)||Curse.active(actor,"NpcCastInterval"))return;
        var list=skills(mob);if(list.isEmpty())return;
        if(actor.characterId.equals("toji")){
            character.toji.TojiNpcCombat.tick(actor,target,list);return;
        }
        if(domainReady(mob)&&!DomainClash.hasDomain(actor))
            for(int id:list)if(character.mainline.Mainline.domainSkill(actor,id)&&cast(mob,target,id))return;
        if(actor.characterId.equals("blood")){int follow=BloodArts.followup(actor);if(follow>=0&&cast(mob,target,follow))return;}
        int start=Math.floorMod(data.getInt("NpcSkillCursor"),list.size());
        for(int i=0;i<list.size();i++){
            int index=(start+i)%list.size(),id=list.get(index);
            if(character.mainline.Mainline.domainSkill(actor,id))continue;
            if(cast(mob,target,id)){data.putInt("NpcSkillCursor",(index+1)%list.size());return;}
        }
    }
    private NpcCombatCasting() { }
}
