package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;

/** 只主动寻找玩家；保留受伤或外部模组赋予的生物仇恨。 */
public final class BossTargets {
    public static boolean managed(LivingEntity e){return e instanceof GuardianEntity||e instanceof YujiEntity||e instanceof BrokenMahitoEntity||e instanceof BuddhaEntity||e instanceof DaitianEntity||e instanceof ParasiteEntity||e instanceof GojoEntity||e instanceof MahoragaEntity;}
    public static boolean valid(Mob boss,LivingEntity target){if(Legion.member(boss)){ServerPlayer owner=Legion.owner(boss);return owner!=null&&target!=null&&target!=boss&&CombatSettings.canHit(owner,target)&&target.level()==boss.level();}return target!=null&&target!=boss&&target.isAlive()&&!target.isRemoved()&&target.level()==boss.level()&&!target.isSpectator()&&!(target instanceof Player p&&p.isCreative())&&!(target instanceof GuardianEntity g&&g.friendly())&&!(target instanceof YujiEntity y&&y.familyMember());}
    /** 友方魔虚罗的主人（给其他系统判断"别打自己人"用）。 */
    public static java.util.UUID allyOwner(LivingEntity e){return e instanceof MahoragaEntity m&&m.ally()?m.allyOwner():null;}
    /** 魔虚罗不分敌我：谁都打，召唤者也打。 */
    public static boolean hostile(Mob boss,LivingEntity target){
        if(!valid(boss,target))return false;
        if(Legion.member(boss))return target==Legion.commanded(boss);
        // 友方魔虚罗只认"正在打主人的人/怪"与"主人正在打的怪"
        if(boss instanceof MahoragaEntity ally&&ally.ally()){
            java.util.UUID owner=ally.allyOwner();
            if(owner!=null&&owner.equals(target.getUUID()))return false;
            if(target==boss.getTarget()||target==boss.getLastHurtByMob())return true;
            return false;
        }
        return boss instanceof MahoragaEntity||BossControl.war(boss)||target instanceof Player||MaidBridge.isMaid(target)||target==boss.getTarget()||target==boss.getLastHurtByMob();
    }
    public static LivingEntity select(Mob boss,double radius){
        if(Legion.member(boss)){LivingEntity target=Legion.commanded(boss);boss.setTarget(target);return target;}
        if(boss instanceof GuardianEntity g&&g.friendly()||boss instanceof YujiEntity y&&y.familyMember()){boss.setTarget(null);return null;}
        LivingEntity current=boss.getTarget();
        if(valid(boss,current)&&boss.distanceToSqr(current)<=radius*radius)return current;
        LivingEntity attacker=boss.getLastHurtByMob();
        if(valid(boss,attacker)&&boss.distanceToSqr(attacker)<=radius*radius){boss.setTarget(attacker);return attacker;}
        LivingEntity next=boss.level().getEntitiesOfClass(LivingEntity.class,boss.getBoundingBox().inflate(radius),p->(p instanceof Player||MaidBridge.isMaid(p)||BossControl.war(boss))&&valid(boss,p)&&boss.distanceToSqr(p)<=radius*radius).stream().min(Comparator.comparingDouble(boss::distanceToSqr)).orElse(null);
        boss.setTarget(next);return next;
    }
    public static List<LivingEntity> nearby(Mob boss,double radius){return around(boss,boss.position(),radius,false);}
    public static List<LivingEntity> around(Mob boss,Vec3 center,double radius,boolean domain){return boss.level().getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius),e->(domain?valid(boss,e):hostile(boss,e))&&e.position().distanceToSqr(center)<=radius*radius);}
    public static void move(LivingEntity target,Vec3 point){if(target instanceof net.minecraft.server.level.ServerPlayer p)p.teleportTo((ServerLevel)p.level(),point.x,point.y,point.z,p.getYRot(),p.getXRot());else target.teleportTo(point.x,point.y,point.z);}
    // 只在最终伤害未被取消时建立仇恨，避免免伤/挚爱保护也激怒首领。
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void retaliate(LivingDamageEvent event){
        if(event.getAmount()<=0||event.getEntity().level().isClientSide||!(event.getEntity() instanceof Mob boss)||!managed(boss)||boss instanceof GuardianEntity g&&g.friendly()||boss instanceof YujiEntity y&&y.familyMember())return;
        if(event.getSource().getEntity() instanceof LivingEntity source){LivingEntity attacker=source instanceof ServerPlayer p?MaidBridge.body(p):source;if(valid(boss,attacker)){boss.setLastHurtByMob(attacker);boss.setTarget(attacker);}}
    }
    @SubscribeEvent public static void warTick(LivingEvent.LivingTickEvent event){
        if(!(event.getEntity() instanceof Mob mob)||mob.level().isClientSide||mob.tickCount%20!=0)return;
        var tag=mob.getPersistentData();
        if(!BossControl.war(mob)){if(tag.getBoolean("SukunaWarTarget")){mob.setTarget(null);mob.setLastHurtByMob(null);tag.remove("SukunaWarTarget");}return;}
        if(managed(mob)){select(mob,64);tag.putBoolean("SukunaWarTarget",true);return;}
        LivingEntity target=mob.level().getEntitiesOfClass(Mob.class,mob.getBoundingBox().inflate(48),e->managed(e)&&valid(mob,e)).stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        if(target!=null){mob.setTarget(target);mob.setLastHurtByMob(target);tag.putBoolean("SukunaWarTarget",true);mob.getNavigation().moveTo(target,1);if(mob.distanceToSqr(target)<4&&mob.hasLineOfSight(target)){if(mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)!=null)mob.doHurtTarget(target);else target.hurt(mob.damageSources().mobAttack(mob),2);}}
    }
    private BossTargets(){}
}
