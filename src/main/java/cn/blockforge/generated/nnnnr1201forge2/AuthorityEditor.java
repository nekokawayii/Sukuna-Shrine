package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 独立施术上下文把十倍伤害保留到弹体和持续伤害命中，不会污染玩家下一招。 */
@Mod.EventBusSubscriber(modid="sukuna")
public final class AuthorityEditor {
    private record Echo(ServerPlayer owner,ArmorStand body,MaidBridge.Actor actor,long born,boolean offhand){}
    private static final List<Echo> echoes=new ArrayList<>();
    private static boolean spawning;
    public static void strike(ServerPlayer p,LivingEntity target){
        if(!Authority.unlocked(p)||!Authority.editorHeld(p)||!p.isAlive()||p.isSpectator()||ObserverRitual.active(p)||!Curse.canUseTechnique(p)||Curse.active(p,"AuthorityEditorCooldown")||!CombatSettings.canHit(p,target)||target.distanceToSqr(p)>96*96||!p.hasLineOfSight(target))return;
        List<Integer> pool=AuthoritySkills.owned(p).stream().filter(AuthoritySkills::damaging).toList();if(pool.isEmpty())return;
        Echo echo=null;for(Echo old:echoes)if(old.owner==p&&!old.offhand&&old.body.level()==p.level()&&!AuthoritySkills.busy(old.actor)&&!Skills.sustaining(old.actor)&&!GojoCombat.sustaining(old.actor)){echo=old;break;}
        if(echo==null){if(echoes.stream().filter(e->e.owner==p).count()>=8)return;echo=create(p,false);if(echo==null)return;}
        var actor=echo.actor;echo.body.moveTo(p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot());actor.aim=target;actor.syncPosition();AuthoritySkills.inherit(p,actor);
        List<Integer> ready=pool.stream().filter(i->AuthoritySkills.ready(actor,i)).toList();if(ready.isEmpty())return;
        int chosen=ready.get(p.getRandom().nextInt(ready.size()));Curse.status(p,"AuthorityEditorCooldown",AuthorityRules.EDITOR_INTERVAL);
        target.invulnerableTime=0;CombatSettings.hurt(p,target,p.damageSources().indirectMagic(p,p),CombatSettings.spellDamage(p,1));
        AuthoritySkills.cast(actor,chosen);
    }
    /** 副手独立时间轴，避免两个蓄力术式互相覆盖；仍使用玩家100%伤害和同一咒力池。 */
    public static void offhand(ServerPlayer p,int skill){
        Echo echo=echoes.stream().filter(e->e.owner==p&&e.offhand&&e.body.level()==p.level()).findFirst().orElse(null);
        if(echo==null)echo=create(p,true);if(echo==null)return;
        var actor=echo.actor;echo.body.moveTo(p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot());actor.aim=Skills.aimTarget(p);actor.syncPosition();AuthoritySkills.inherit(p,actor);
        if(!AuthoritySkills.busy(actor)||(skill==5&&Skills.domainActive(actor))||(skill==107&&GojoCombat.domainOpen(actor)))AuthoritySkills.cast(actor,skill);
    }
    private static Echo create(ServerPlayer p,boolean offhand){
        ArmorStand body=EntityType.ARMOR_STAND.create(p.serverLevel());if(body==null)return null;
        var initial=new net.minecraft.nbt.CompoundTag();body.saveWithoutId(initial);initial.putBoolean("Marker",true);body.load(initial);
        body.setInvisible(true);body.setInvulnerable(true);body.setNoGravity(true);body.setSilent(true);body.setPos(p.position());body.getPersistentData().putUUID("AuthorityEchoOwner",p.getUUID());body.getPersistentData().putDouble("AuthorityEchoScale",offhand?1:AuthorityRules.EDITOR_DAMAGE);
        spawning=true;try{if(!p.serverLevel().addFreshEntity(body))return null;}finally{spawning=false;}
        var actor=MaidBridge.actor(body);AuthoritySkills.inherit(p,actor);Echo echo=new Echo(p,body,actor,p.level().getGameTime(),offhand);echoes.add(echo);return echo;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END)return;for(var it=echoes.iterator();it.hasNext();){Echo echo=it.next();ServerPlayer p=echo.owner;if(!p.isAlive()||p.isRemoved()||echo.body.isRemoved()||p.hasDisconnected()||p.level()!=echo.body.level()||!echo.offhand&&p.level().getGameTime()-echo.born>2400||ObserverRitual.active(p)){AuthoritySkills.cancel(echo.actor);echo.body.discard();it.remove();continue;}echo.body.setPos(p.position());echo.body.setDeltaMovement(Vec3.ZERO);echo.body.setYRot(p.getYRot());echo.body.setXRot(p.getXRot());echo.actor.syncPosition();}}
    @SubscribeEvent public static void join(EntityJoinLevelEvent e){if(!e.getLevel().isClientSide&&Authority.echo(e.getEntity() instanceof LivingEntity l?l:null)&&!spawning)e.setCanceled(true);}
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent e){echoes.clear();}
    private AuthorityEditor(){}
}
