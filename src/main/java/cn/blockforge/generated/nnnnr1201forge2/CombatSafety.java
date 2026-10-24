package cn.blockforge.generated.nnnnr1201forge2;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 敌友安全入口。只在服务端授权；延迟命中每次重查，不缓存旧敌友结果。 */
public final class CombatSafety {
    public static UUID ownerId(Entity entity) {
        if(entity instanceof ServerPlayer p)return p.getUUID();
        if(entity instanceof ShadowEntity s)return s.ritual()?null:s.ownerId();
        if(entity instanceof MahoragaEntity m)return m.ally()?m.allyOwner():null;
        if(entity instanceof TamableAnimal tame)return tame.getOwnerUUID();
        if(entity instanceof JacobsLadderEntity ladder)return ladder.ownerId();
        return Legion.ownerId(entity);
    }
    public static ServerPlayer owner(Entity entity) {
        for(int depth=0;entity!=null&&depth<8;depth++) {
            if(entity instanceof ServerPlayer p)return p;
            if(entity instanceof Projectile projectile){entity=projectile.getOwner();continue;}
            ServerPlayer actor=MaidBridge.caster(entity);
            if(actor!=null)return actor;
            UUID id=ownerId(entity);
            return id!=null&&entity.level() instanceof ServerLevel level?level.getServer().getPlayerList().getPlayer(id):null;
        }
        return null;
    }
    public static boolean canAffect(ServerPlayer caster,LivingEntity target) {
        return caster!=null&&target!=null&&caster.level()==target.level()&&!target.isRemoved()&&CombatSettings.canHit(caster,target);
    }
    /** 主动索敌和最终伤害用同一授权；玩家只有在 O 菜单标敌且服务器允许 PvP 时才入选。 */
    public static boolean canTarget(ServerPlayer caster,LivingEntity target){return canAffect(caster,target);}
    /** 召唤物只主动追击明确敌人、怪物或正在交战者，不主动屠杀中立生物。 */
    public static boolean summonTarget(ServerPlayer caster,LivingEntity target){
        if(!canTarget(caster,target))return false;
        UUID id=ownerId(target);
        return CombatRelationRules.summon(true,CombatSettings.relation(caster,id==null?target.getUUID():id)==CombatSettings.ENEMY,
            target instanceof net.minecraft.world.entity.monster.Enemy,
            target==caster.getLastHurtMob()&&caster.tickCount-caster.getLastHurtMobTimestamp()<200,
            target==caster.getLastHurtByMob()&&caster.tickCount-caster.getLastHurtByMobTimestamp()<200,
            target instanceof net.minecraft.world.entity.Mob mob&&mob.getTarget()==caster);
    }
    /** 十影、魔虚罗及新召唤物直接调用：主人攻击 > 主人受击 > 标敌玩家/其召唤物 > 其他威胁。 */
    public static LivingEntity summonTarget(ServerPlayer caster,net.minecraft.world.entity.Mob summon,double range){
        if(caster==null||caster.level()!=summon.level())return null;
        java.util.function.Predicate<LivingEntity> valid=t->t!=summon&&summonTarget(caster,t)&&summon.distanceToSqr(t)<=range*range;
        LivingEntity t=caster.getLastHurtMob();if(caster.tickCount-caster.getLastHurtMobTimestamp()<200&&valid.test(t))return t;
        t=caster.getLastHurtByMob();if(caster.tickCount-caster.getLastHurtByMobTimestamp()<200&&valid.test(t))return t;
        return summon.level().getEntitiesOfClass(LivingEntity.class,summon.getBoundingBox().inflate(range),valid).stream()
            .min(java.util.Comparator.<LivingEntity>comparingInt(e->{UUID id=ownerId(e);return CombatSettings.relation(caster,id==null?e.getUUID():id)==CombatSettings.ENEMY?0:e instanceof net.minecraft.world.entity.Mob m&&m.getTarget()==caster?1:2;})
                .thenComparingDouble(summon::distanceToSqr)).orElse(null);
    }
    public static boolean hurt(ServerPlayer caster,LivingEntity target,DamageSource source,float amount) {
        if(!Float.isFinite(amount)||amount<=0||!canAffect(caster,target)){AttackTypes.consume();return false;}
        return CombatSettings.hurt(caster,target,source,amount);
    }
    public static boolean effect(ServerPlayer caster,LivingEntity target,MobEffectInstance effect) {
        return canAffect(caster,target)&&target.addEffect(effect,MaidBridge.body(caster));
    }
    public static boolean push(ServerPlayer caster,LivingEntity target,Vec3 velocity) {
        if(!canAffect(caster,target)||BloodArts.armored(target))return false;
        target.push(velocity.x,velocity.y,velocity.z);target.hurtMarked=true;return true;
    }
    private static boolean denied(LivingEntity victim,DamageSource source) {
        if(victim.level().isClientSide)return false;
        ServerPlayer caster=CombatSettings.caster(source);
        // 自身反噬保留；不把禁止友伤变成无限制茈的免反噬漏洞。
        if(caster!=null)return victim!=MaidBridge.body(caster)&&victim!=caster&&!canAffect(caster,victim);
        Entity cause=source.getEntity()!=null?source.getEntity():source.getDirectEntity();
        // 已知归属、主人不在线时不将术式变为无主伤害。
        return cause!=null&&ownerId(cause)!=null;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void attack(LivingAttackEvent event){if(denied(event.getEntity(),event.getSource()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void hurt(LivingHurtEvent event){if(denied(event.getEntity(),event.getSource()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void damage(LivingDamageEvent event){if(denied(event.getEntity(),event.getSource()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void melee(AttackEntityEvent event){if(event.getEntity() instanceof ServerPlayer p&&event.getTarget() instanceof LivingEntity target&&(DeathStar.charging(p)||!canAffect(p,target)))event.setCanceled(true);}
    private CombatSafety(){}
}
