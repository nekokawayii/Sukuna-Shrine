package cn.blockforge.generated.nnnnr1201forge2;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;

public class ParasiteEntity extends Monster {
    UUID owner;
    private UUID attached;
    private int attack,life;
    public ParasiteEntity(EntityType<? extends Monster> type,Level level){super(type,level);xpReward=2;}
    public boolean mother(){return getType()==Content.MOTHER.get();}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));}
    @Override public boolean isAlliedTo(Entity e){return e!=getTarget()&&e!=getLastHurtByMob()&&(e instanceof BuddhaEntity||e instanceof ParasiteEntity||super.isAlliedTo(e));}
    @Override public void tick(){super.tick();if(level().isClientSide||!isAlive()||SixfoldFinale.controlled(this))return;life++;if(life>2400&&!mother()){discard();return;}if(attack>0)attack--;
        ServerLevel server=(ServerLevel)level();LivingEntity selected=BossTargets.select(this,48);if(selected!=null&&tickCount%20==0)CursedGrowth.update(this,selected);
        if(attached!=null&&(selected==null||!attached.equals(selected.getUUID()))){attached=null;setNoGravity(false);}
        if(attached!=null){Entity entity=server.getEntity(attached);if(!(entity instanceof LivingEntity p)||!BossTargets.hostile(this,p)){attached=null;setNoGravity(false);return;}getNavigation().stop();int count=server.getEntitiesOfClass(ParasiteEntity.class,p.getBoundingBox().inflate(2),e->p.getUUID().equals(e.attached)&&e.isAlive()).size();double angle=(getId()%3)*Math.PI*2/3;setPos(p.getX()+Math.cos(angle)*0.48,p.getY()+0.5+(getId()%3)*0.35,p.getZ()+Math.sin(angle)*0.48);setNoGravity(true);if(tickCount%20==0){p.invulnerableTime=0;p.hurt(damageSources().mobAttack(this),BalanceRules.damage(2*CursedGrowth.scale(p)));if(p instanceof Player player)Curse.status(player,"Parasite",30);if(count>=3)p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,10,6));}return;}
        setNoGravity(false);LivingEntity p=selected;if(p==null){getNavigation().stop();return;}
        getNavigation().moveTo(p,1.1);
        if(!mother()&&distanceToSqr(p)<1.8){long count=server.getEntitiesOfClass(ParasiteEntity.class,p.getBoundingBox().inflate(2),e->p.getUUID().equals(e.attached)&&e.isAlive()).size();if(count<3)attached=p.getUUID();}
        if(mother()&&attack==0){if(distanceToSqr(p)<25){attack=55;Network.send(server,new Network.Effect(32,getId(),position(),Vec3.ZERO,5,24,0));for(LivingEntity other:BossTargets.nearby(this,5))other.hurt(damageSources().mobAttack(this),BalanceRules.damage(12));}else if(distanceToSqr(p)<400){attack=65;Vec3 dir=p.getEyePosition().subtract(getEyePosition()).normalize();Network.send(server,new Network.Effect(40,getId(),getEyePosition(),dir,20,25,2));for(LivingEntity other:BossTargets.nearby(this,20)){Vec3 q=other.getEyePosition().subtract(getEyePosition());double along=q.dot(dir);if(along>0&&along<20&&q.subtract(dir.scale(along)).length()<2)other.hurt(damageSources().indirectMagic(this,this),BalanceRules.damage(8));}}}
    }
    @Override public boolean hurt(DamageSource source,float amount){if(mother()&&source.getEntity() instanceof Player p){Vec3 a=p.getEyePosition(),b=a.add(p.getLookAngle().scale(6));var hit=getBoundingBox().clip(a,b);if(hit.isPresent()){double y=hit.get().y-getY();if(y>0.9&&y<1.9)amount*=1.5f;}}return super.hurt(source,amount);}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recent){super.dropCustomDeathLoot(source,looting,recent);spawnAtLocation(new net.minecraft.world.item.ItemStack(Content.ORB.get(),mother()?4:1));if(level() instanceof ServerLevel server){Network.send(server,new Network.Effect(37,getId(),position(),Vec3.ZERO,mother()?5:1,25,1));}}
    @Override public void die(DamageSource source){super.die(source);if(mother()&&level() instanceof ServerLevel server&&owner!=null&&server.getEntity(owner) instanceof BuddhaEntity boss&&boss.isAlive()){owner=null;boss.motherKilled();}}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(owner!=null)tag.putUUID("Owner",owner);tag.putInt("Life",life);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(tag.hasUUID("Owner"))owner=tag.getUUID("Owner");life=tag.getInt("Life");}
}
