package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.sounds.*;

/** 破碎真人：近战单次最多100伤害，发射的肢体可被斩击切断。 */
public final class BrokenMahitoEntity extends Monster {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(BrokenMahitoEntity.class,EntityDataSerializers.INT);
    private final ServerBossEvent bar=new ServerBossEvent(Component.literal("破碎真人"),BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.NOTCHED_20);
    private final Set<UUID> participants=new HashSet<>();
    private int attackCooldown=30,limbCooldown=65,quietTicks,stunTicks,windup;
    private float castBonus;
    public float castBonus(){return castBonus;}
    private Vec3 strikeOrigin=Vec3.ZERO,strikeDirection=Vec3.ZERO;
    private UUID target;
    public BrokenMahitoEntity(EntityType<? extends Monster> type,Level level){super(type,level);setPersistenceRequired();xpReward=1200;}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ACTION,0);}
    @Override protected void registerGoals(){}
    public int action(){return entityData.get(ACTION);}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    @Override public boolean doHurtTarget(Entity entity){return false;}
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);bar.addPlayer(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);}
    private ServerLevel server(){return (ServerLevel)level();}
    private List<ServerPlayer> players(){return server().getEntitiesOfClass(ServerPlayer.class,getBoundingBox().inflate(64),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator());}
    private void fx(int kind,Vec3 point,Vec3 direction,float size,int life,long seed){Network.send(server(),new Network.Effect(kind,getId(),point,direction,size,life,seed));}
    private ServerPlayer nearest(){return players().stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);}
    @Override public void tick(){
        super.tick();if(level().isClientSide||!isAlive()||SixfoldFinale.controlled(this))return;
        LivingEntity p=BossTargets.select(this,64);
        quietTicks++;bar.setProgress(Math.max(0,Math.min(1,getHealth()/getMaxHealth())));bar.setName(Component.literal("破碎真人 · "+(int)getHealth()+" / "+(int)getMaxHealth()+(stunTicks>0?" · 肢体断裂硬直":"")));
        if(stunTicks>0){stunTicks--;windup=0;getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);entityData.set(ACTION,0);return;}
        if(p==null){getNavigation().stop();target=null;return;}target=p.getUUID();participants.add(p.getUUID());
        if(attackCooldown>0)attackCooldown--;if(limbCooldown>0)limbCooldown--;
        if(windup>0){
            getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);
            if(--windup==0){Vec3 end=strikeOrigin.add(strikeDirection.scale(castBonus>0?6.4:3.2));
                for(LivingEntity victim:BossTargets.nearby(this,64))if(hasLineOfSight(victim)&&victim.getBoundingBox().inflate(0.3).clip(strikeOrigin,end).isPresent())victim.hurt(damageSources().mobAttack(this),BalanceRules.damage(40+castBonus));
                fx(130,end,strikeOrigin,1.5f,10,0);entityData.set(ACTION,0);attackCooldown=26;
            }return;
        }
        getLookControl().setLookAt(p,30,30);double distance=distanceTo(p);
        if(limbCooldown<=0&&hasLineOfSight(p)){entityData.set(ACTION,2);castBonus=BossRevelation.roll(this,"断肢飞射：沿红线发射肢体，横移躲开或用斩击切断");launchLimb(p);limbCooldown=80;attackCooldown=Math.max(attackCooldown,22);getNavigation().stop();}
        else if(distance<=3.2&&attackCooldown<=0&&hasLineOfSight(p)){
            castBonus=BossRevelation.roll(this,"肢体突刺：短距离直线重击，抬手时侧移离开攻击线");strikeOrigin=getEyePosition().add(0,-0.45,0);strikeDirection=p.getBoundingBox().getCenter().subtract(strikeOrigin).normalize();windup=12;entityData.set(ACTION,1);getNavigation().stop();
            fx(134,strikeOrigin,strikeOrigin.add(strikeDirection.scale(castBonus>0?6.4:3.2)),0.08f,12,0);
        }else if(distance>2.6){if(tickCount%8==0)getNavigation().moveTo(p,1.0);}else getNavigation().stop();
        if(quietTicks>=100&&tickCount%20==0){setHealth(Math.min(getMaxHealth(),getHealth()+BalanceRules.damage(500)));fx(131,position().add(0,1,0),Vec3.ZERO,2,15,0);}
        if(windup==0&&limbCooldown<62)entityData.set(ACTION,0);
    }
    private void launchLimb(LivingEntity p){MahitoLimbEntity limb=Content.MAHITO_LIMB.get().create(level());if(limb==null)return;limb.moveTo(getX(),getEyeY()-0.25,getZ(),getYRot(),0);limb.setup(this,p);level().addFreshEntity(limb);}
    public void limbSevered(){if(stunTicks>0)return;stunTicks=50;attackCooldown=Math.max(attackCooldown,70);entityData.set(ACTION,0);fx(135,getBoundingBox().getCenter(),Vec3.ZERO,2.5f,50,0);level().playSound(null,blockPosition(),SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.HOSTILE,2,0.5f);}
    @Override public boolean hurt(DamageSource source,float amount){
        if(level().isClientSide)return false;
        if(source.getEntity() instanceof ServerPlayer p)participants.add(p.getUUID());
        if(source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK))amount=Math.min(BalanceRules.damage(100),amount);
        quietTicks=0;boolean hit=super.hurt(source,amount);return hit;
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){super.dropCustomDeathLoot(source,looting,recentlyHit);spawnAtLocation(Content.IDLE_TRANSFIGURATION_CURSE.get());spawnAtLocation(Content.MAHITO_FINGER.get());}
    @Override public void die(DamageSource source){boolean wasDead=dead;super.die(source);if(!level().isClientSide&&dead&&!wasDead)SoulJourney.brokenMahitoDefeated(this);if(dead)bar.removeAllPlayers();}
    @Override public void remove(RemovalReason reason){bar.removeAllPlayers();super.remove(reason);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("QuietTicks",quietTicks);tag.putInt("StunTicks",stunTicks);if(target!=null)tag.putUUID("Target",target);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);quietTicks=tag.getInt("QuietTicks");stunTicks=tag.getInt("StunTicks");if(tag.hasUUID("Target"))target=tag.getUUID("Target");}
}
