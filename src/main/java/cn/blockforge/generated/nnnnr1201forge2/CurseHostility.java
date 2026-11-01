package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** NPC与首领共同识别敌对咒灵；不攻击自己的召唤物、队友和玩家友军。 */
public final class CurseHostility {
    public static boolean curse(LivingEntity e){return e instanceof LowCurseEntity||e instanceof BuddhaEntity||e instanceof DaitianEntity||e instanceof ParasiteEntity||e instanceof BrokenMahitoEntity||e instanceof RikaEntity||e instanceof JogoNpcEntity||e instanceof SorcererNpcEntity s&&s.characterId().equals("mahito");}
    public static boolean hunter(LivingEntity e){return e instanceof Mob&&!(e instanceof GetoEntity)&&!(e instanceof LowCurseEntity)&&!(e instanceof ParasiteEntity)&&(NpcBridge.supports(e)||BossTargets.managed(e)||e instanceof MegumiEntity);}
    public static boolean truce(LivingEntity a,LivingEntity b){return a!=null&&b!=null&&(a instanceof GetoEntity&&curse(b)||b instanceof GetoEntity&&curse(a));}
    @SubscribeEvent public static void target(net.minecraftforge.event.entity.living.LivingChangeTargetEvent event){
        if(truce(event.getEntity(),event.getNewTarget()))event.setNewTarget(null);
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void preventDamage(net.minecraftforge.event.entity.living.LivingAttackEvent event){
        var source=event.getSource().getEntity();
        if(source instanceof NpcBridge.NpcActor actor)source=actor.mob;
        if(source instanceof net.minecraft.world.entity.projectile.Projectile projectile)source=projectile.getOwner();
        if(source instanceof LivingEntity attacker&&truce(attacker,event.getEntity()))event.setCanceled(true);
    }
    @SubscribeEvent public static void clearOldTarget(LivingEvent.LivingTickEvent event){
        if(event.getEntity() instanceof Mob mob&&!mob.level().isClientSide&&truce(mob,mob.getTarget())){mob.setTarget(null);mob.setLastHurtByMob(null);mob.getNavigation().stop();}
    }
    public static boolean enemy(Mob hunter,LivingEntity target){
        if(truce(hunter,target)||!curse(target)||!NpcBridge.canHit(hunter,target))return false;
        if(target instanceof LowCurseEntity c&&hunter.getUUID().equals(c.summoner()))return false;
        if(target instanceof ParasiteEntity parasite&&hunter.getUUID().equals(parasite.owner))return false;
        if(hunter instanceof ParasiteEntity parasite&&target.getUUID().equals(parasite.owner))return false;
        java.util.UUID owner=CombatSafety.ownerId(hunter),theirs=CombatSafety.ownerId(target);
        if(theirs!=null&&(theirs.equals(hunter.getUUID())||theirs.equals(owner)))return false;
        if(owner!=null&&hunter.level() instanceof net.minecraft.server.level.ServerLevel level){var player=NpcBridge.find(level.getServer(),owner);if(player!=null&&!CombatSettings.canHit(player,target))return false;}
        return true;
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event){
        if(event.getLevel().isClientSide()||!(event.getEntity() instanceof Mob mob)||!hunter(mob))return;
        // 每次实体实例加入只安装一次；不要把这个瞬时标志写入NBT。
        if(installed.put(mob,Boolean.TRUE)!=null)return;
        mob.targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(mob,LivingEntity.class,10,true,false,target->enemy(mob,target)));
    }
    private static final java.util.Map<Mob,Boolean> installed=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    /** 已进入和平剧情的NPC只用普通近战清理敌对咒灵，不重新触发Boss剧情。 */
    public static boolean defend(Mob mob){
        if(mob.isNoAi()||!mob.isAlive())return false;
        LivingEntity target=mob.getTarget();
        if(target==null||!enemy(mob,target)||mob.distanceToSqr(target)>32*32){
            if(mob.tickCount%20!=0)return false;
            target=mob.level().getEntitiesOfClass(LivingEntity.class,mob.getBoundingBox().inflate(32),e->enemy(mob,e)&&mob.hasLineOfSight(e)).stream().min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
            mob.setTarget(target);
        }
        if(target==null)return false;
        mob.getLookControl().setLookAt(target,30,30);if((mob.tickCount+mob.getId())%10==0)mob.getNavigation().moveTo(target,1.1);
        if(mob.tickCount%20==0&&mob.getBoundingBox().inflate(1.5).intersects(target.getBoundingBox())&&mob.hasLineOfSight(target)){mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);mob.doHurtTarget(target);}
        return true;
    }
    private CurseHostility(){}
}
