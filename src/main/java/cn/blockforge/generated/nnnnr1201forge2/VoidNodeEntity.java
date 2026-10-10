package cn.blockforge.generated.nnnnr1201forge2;

import java.util.UUID;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.*;
import net.minecraft.network.syncher.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/** 分身、灵魂锚、手掌节点和可斩断触手，均为可真正命中的实体。 */
public final class VoidNodeEntity extends Monster {
    private static final EntityDataAccessor<Integer> KIND=SynchedEntityData.defineId(VoidNodeEntity.class,EntityDataSerializers.INT);
    private UUID owner,victim;private int life=180,slot;private boolean resolved;private float castBonus;
    public VoidNodeEntity(EntityType<? extends Monster> t,Level l){super(t,l);setPersistenceRequired();xpReward=0;}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(KIND,0);}
    @Override protected void registerGoals(){}
    public int kind(){return entityData.get(KIND);}
    public void setup(VoidBossEntity boss,int kind,LivingEntity target,int life,int slot){owner=boss.getUUID();castBonus=boss.castBonus();victim=target==null?null:target.getUUID();entityData.set(KIND,kind);this.life=life;this.slot=slot;setHealth(getMaxHealth());}
    @Override public boolean removeWhenFarAway(double d){return false;}
    @Override public boolean causeFallDamage(float d,float m,DamageSource s){return false;}
    private VoidBossEntity boss(){return owner!=null&&level() instanceof ServerLevel l&&l.getEntity(owner) instanceof VoidBossEntity b?b:null;}
    private LivingEntity player(){return victim!=null&&level() instanceof ServerLevel l&&l.getEntity(victim) instanceof LivingEntity p?p:null;}
    @Override public void tick(){super.tick();if(level().isClientSide)return;if(kind()==1||kind()==2)noPhysics=true;VoidBossEntity b=boss();LivingEntity p=player();
        if(!isAlive())return;
        if(b==null||!b.isAlive()){release();discard();return;}
        if((kind()==1||kind()==2)&&(!BossTargets.valid(b,p))){release();discard();return;}
        if(--life<=0){if(!resolved&&p!=null){release();p.clearFire();if(kind()==1)p.hurt(damageSources().genericKill(),Float.MAX_VALUE);if(kind()==2)b.hit(p,100,castBonus);}discard();return;}
        if(kind()==0&&p!=null){Vec3 d=p.position().subtract(position()).multiply(1,0,1).normalize();setDeltaMovement(d.scale(0.22));if(distanceToSqr(p)<3){b.rift(position(),1.8,160);discard();}}
        else if(kind()==1||kind()==2){setDeltaMovement(Vec3.ZERO);setNoGravity(true);p.getPersistentData().putLong("VoidCaptured",level().getGameTime()+5);BossTargets.move(p,position());if(kind()==1)p.setInvisible(true);if(p instanceof ServerPlayer player){Skills.closeDomain(player);Skills.cancelCharge(player);}}
        else if(kind()==3){setNoGravity(true);double a=b.tickCount*0.07+slot*Math.PI/2;Vec3 end=b.position().add(Math.cos(a)*9,1.5,Math.sin(a)*9);setPos(end.x,end.y,end.z);if(tickCount%10==0){b.beam(b.position().add(0,2,0),position(),0.5f,12,108);for(LivingEntity target:b.targets())if(target.getBoundingBox().inflate(0.6).clip(b.position().add(0,1,0),position()).isPresent()){b.hit(target,18,castBonus);target.push(Math.cos(a)*1.5,0.4,Math.sin(a)*1.5);target.hurtMarked=true;}}}
    }
    private void release(){if(resolved)return;resolved=true;LivingEntity p=player();if(p!=null&&(kind()==1||kind()==2)){p.setInvisible(false);p.getPersistentData().remove("VoidCaptured");p.invulnerableTime=20;}}
    @Override public boolean hurt(DamageSource source,float amount){if((kind()==1||kind()==2)&&source.getEntity()!=null&&source.getEntity().getUUID().equals(victim))return false;if(kind()==3&&!source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)&&!source.is(net.minecraft.world.damagesource.DamageTypes.SONIC_BOOM)&&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return false;invulnerableTime=0;return super.hurt(source,amount);}
    @Override public void die(DamageSource s){VoidBossEntity b=boss();if(b!=null){if(kind()==0)b.removeRifts(3);if(kind()==3)b.rift(position().add(0,-1.5,0),1.3,100);if(s.getEntity() instanceof ServerPlayer p)b.record(p);}release();super.die(s);}
    @Override public void remove(RemovalReason reason){if(!level().isClientSide)release();super.remove(reason);}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);t.putInt("VoidNodeKind",kind());t.putInt("VoidNodeLife",life);if(owner!=null)t.putUUID("Owner",owner);if(victim!=null)t.putUUID("Victim",victim);t.putInt("Slot",slot);}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);entityData.set(KIND,t.getInt("VoidNodeKind"));life=Math.max(1,t.getInt("VoidNodeLife"));if(t.hasUUID("Owner"))owner=t.getUUID("Owner");if(t.hasUUID("Victim"))victim=t.getUUID("Victim");slot=t.getInt("Slot");}
    void releaseTestTarget(UUID id){if(id.equals(victim)&&(kind()==1||kind()==2)){release();discard();}}
    public boolean ownedBy(UUID id){return id.equals(owner);}
}
