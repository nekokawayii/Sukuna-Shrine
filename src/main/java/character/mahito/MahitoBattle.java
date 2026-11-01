package character.mahito;

import cn.blockforge.generated.nnnnr1201forge2.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;

/** 战斗用无为转变：玩家/NPC 共用，伤害走事件，不删除实体、不修改玩家存档。 */
public final class MahitoBattle {
    public static boolean strike(ServerPlayer caster) {
        Vec3 start=caster.getEyePosition(),end=start.add(caster.getLookAngle().scale(7));
        var wall=caster.level().clip(new net.minecraft.world.level.ClipContext(start,end,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,caster));
        if(wall.getType()!=HitResult.Type.MISS)end=wall.getLocation();
        final Vec3 tip=end;
        LivingEntity victim=caster.level().getEntitiesOfClass(LivingEntity.class,new AABB(start,end).inflate(1),
            t->CombatSettings.canHit(caster,t)&&(caster instanceof NpcBridge.NpcActor||t instanceof net.minecraft.world.entity.player.Player||t instanceof net.minecraft.world.entity.monster.Enemy||NpcBridge.supports(t)||t instanceof net.minecraft.world.entity.Mob mob&&mob.getTarget()!=null)&&t.getBoundingBox().inflate(.6).clip(start,tip).isPresent())
            .stream().min(java.util.Comparator.comparingDouble(caster::distanceToSqr)).orElse(null);
        caster.serverLevel().sendParticles(ParticleTypes.SOUL,tip.x,tip.y,tip.z,12,.35,.35,.35,.03);
        if(victim==null)return false;
        return soulHit(caster,victim,false);
    }
    private static boolean soulHit(ServerPlayer caster,LivingEntity victim,boolean domain) {
        if(DomainClash.active(caster)||DomainClash.active(victim)||!CombatSettings.canHit(caster,victim))return false;
        if(domain&&character.mainline.Mainline.simpleProtected(victim))return false;
        // 上限约束：普通攻击 8~26，领域脉冲 6~16，不对玩家或首领按最大血量秒杀。
        float damage=domain?Math.min(16,6+victim.getMaxHealth()*.015f):Math.min(26,8+victim.getMaxHealth()*.025f);
        boolean hit=CombatSettings.hurt(caster,victim,caster.damageSources().indirectMagic(caster,caster),damage);
        if(hit){
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,domain?30:50,0));
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,40,0));
            caster.serverLevel().sendParticles(ParticleTypes.SOUL,victim.getX(),victim.getY()+1,victim.getZ(),8,.3,.5,.3,.01);
        }
        return hit;
    }
    public static void domainTick(ServerPlayer caster,Vec3 center,double radius) {
        if(caster.level().getGameTime()%20!=0||DomainClash.active(caster))return;
        if(!Curse.spend(caster,20)){character.domainking.SelfEmbryoDomain.close(caster,"咒力不足");return;}
        for(var victim:caster.level().getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius),
            t->t.position().distanceToSqr(center)<=radius*radius&&CombatSettings.canHit(caster,t)))soulHit(caster,victim,true);
    }
    private MahitoBattle() { }
}
