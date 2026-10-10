package cn.blockforge.generated.nnnnr1201forge2;

import java.util.UUID;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.*;

public final class DaitianEntity extends Monster {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(DaitianEntity.class,EntityDataSerializers.INT);
    private final ServerBossEvent bar=new ServerBossEvent(Component.literal("咒胎戴天 · 特级咒灵"),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
    private int windup,cooldown=100,melee;
    private UUID victim;
    private boolean revealed;
    private float castBonus;
    private int castCount;
    private double growth(){return CursedGrowth.scale(getTarget());}
    public DaitianEntity(EntityType<? extends Monster> type,Level level){super(type,level);xpReward=25;setPersistenceRequired();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier(UUID.fromString("75f17bd4-f6fe-4f5d-bf29-424c3c444832"),"咒胎攻击",0.2,AttributeModifier.Operation.MULTIPLY_TOTAL));setHealth(getMaxHealth());}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ACTION,0);}
    public int action(){return entityData.get(ACTION);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(4,new RandomStrollGoal(this,0.65));goalSelector.addGoal(5,new LookAtPlayerGoal(this,Player.class,16));}
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);bar.addPlayer(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);}
    @Override public boolean removeWhenFarAway(double d){return false;}
    private void fx(int kind,Vec3 p,int ticks,long seed){Network.send((ServerLevel)level(),new Network.Effect(kind,getId(),p,Vec3.ZERO,3,ticks,seed));}
    @Override public void tick(){super.tick();if(level().isClientSide||!isAlive()||SixfoldFinale.controlled(this))return;bar.setProgress(Math.min(1,getHealth()/getMaxHealth()));
        if(tickCount%10==0){boolean control=isFullyFrozen();for(var effect:getActiveEffects())if(effect.getAmplifier()>=2&&(effect.getEffect()==MobEffects.MOVEMENT_SLOWDOWN||effect.getEffect()==MobEffects.WEAKNESS||effect.getEffect()==MobEffects.DIG_SLOWDOWN))control=true;if(control){addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,80,2));fx(57,position(),12,0);}}
        LivingEntity target=BossTargets.select(this,64);
        if(target!=null&&tickCount%20==0)CursedGrowth.update(this,target);
        if(melee>0)melee--;
        if(action()!=0){getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);if(target!=null)getLookControl().setLookAt(target,30,30);if(--windup<=0){Entity entity=victim==null?null:((ServerLevel)level()).getEntity(victim);if(entity instanceof LivingEntity t&&BossTargets.hostile(this,t)){
            if(action()==1&&distanceToSqr(t)<=Math.pow(12*growth()*(revealed?1.75:1),2)&&hasLineOfSight(t)){for(MobEffectInstance effect:java.util.List.copyOf(t.getActiveEffects()))if(effect.getEffect().isBeneficial())t.removeEffect(effect.getEffect());t.hurt(damageSources().mobAttack(this),BalanceRules.damage(6+castBonus));if(revealed){t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,160,2));t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,1));}fx(56,t.getBoundingBox().getCenter(),30,t.getId());}
            if(action()==2&&distanceToSqr(t)<=Math.pow(4*growth()*(revealed?1.75:1),2)&&hasLineOfSight(t)){t.hurt(damageSources().mobAttack(this),(float)(getAttributeValue(Attributes.ATTACK_DAMAGE)+BalanceRules.strength(6+castBonus)));if(revealed){fx(32,position(),30,0);for(LivingEntity other:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(7*growth()),e->BossTargets.hostile(this,e)&&e!=t))other.hurt(damageSources().mobAttack(this),BalanceRules.damage(12*growth()));}Vec3 push=t.position().subtract(position()).normalize();t.push(push.x*1.1,0.4,push.z*1.1);t.hurtMarked=true;fx(58,t.position(),12,t.getId());}}
            if(action()==2){addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,1));addEffect(new MobEffectInstance(MobEffects.WEAKNESS,60,1));}entityData.set(ACTION,0);cooldown=120+random.nextInt(80);victim=null;}return;}
        if(target==null)return;getLookControl().setLookAt(target,30,30);getNavigation().moveTo(target,1.0);
        if(--cooldown<=0&&distanceToSqr(target)<100){int skill=distanceToSqr(target)>16?1:1+random.nextInt(2);entityData.set(ACTION,skill);castBonus=BossRevelation.roll(this,skill==1?"污浊诅咒：剥夺增益，前摇时离开24格或躲到墙后":"含恨而立：重击与冲击波，前摇时离开16格");revealed=castBonus>0;windup=(skill==1?60:50)+(revealed?20:0);victim=target.getUUID();SkillSounds.play(this,skill==1?"daitian_curse":"daitian_slam");if(revealed){fx(66,position(),windup,skill);SkillSounds.play(this,"boss_reveal");}fx(55,position(),windup,skill);level().playSound(null,blockPosition(),SoundEvents.EVOKER_PREPARE_ATTACK,SoundSource.HOSTILE,1.5f,0.6f);return;}
        if(distanceToSqr(target)<4&&melee==0){doHurtTarget(target);melee=25;}
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit){super.dropCustomDeathLoot(source,looting,hit);spawnAtLocation(Content.FINGER.get());}
    @Override public void die(DamageSource s){super.die(s);bar.removeAllPlayers();}
    @Override public void remove(RemovalReason reason){super.remove(reason);bar.removeAllPlayers();}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("SkillCooldown",cooldown);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);float ratio=getHealth()/getMaxHealth();getAttribute(Attributes.MAX_HEALTH).removeModifier(UUID.fromString("75f17bd4-f6fe-4f5d-bf29-424c3c444831"));getAttribute(Attributes.MAX_HEALTH).removeModifier(UUID.fromString("84113872-88e2-4b3d-82fc-010300000001"));getAttribute(Attributes.MAX_HEALTH).setBaseValue(BalanceRules.health(200));setHealth(getMaxHealth()*ratio);cooldown=Math.max(60,tag.getInt("SkillCooldown"));entityData.set(ACTION,0);}
}
