package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** 十影共用的实体，形态、动作与主人由同步数据决定；所有战斗在服务端执行。 */
public final class ShadowEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> KIND=SynchedEntityData.defineId(ShadowEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(ShadowEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> ACTION_AT=SynchedEntityData.defineId(ShadowEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(ShadowEntity.class,EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> RITUAL=SynchedEntityData.defineId(ShadowEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<org.joml.Vector3f> EMERGE=SynchedEntityData.defineId(ShadowEntity.class,EntityDataSerializers.VECTOR3);
    private static final ResourceKey<DamageType> PIERCE=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("sukuna","shadow_totality"));
    public static final int IDLE=0,ATTACK=1,SPECIAL=2,EMERGING=3,DISSOLVE=4;
    private long expires,actionEnd;
    private int attackCooldown,specialCooldown,stage,oxAim,oxRun,oxRecovery;
    private UUID oxTarget;
    private boolean pack,bossOwned,registeredDeath;
    private double chargeDistance;
    private Vec3 chargeDirection=new Vec3(0,0,1),lastChargePosition;
    private final Set<UUID> charged=new HashSet<>();
    private LivingEntity bound;

    public ShadowEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setPersistenceRequired();setMaxUpStep(1.2f);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(KIND,0);entityData.define(ACTION,0);entityData.define(ACTION_AT,0L);entityData.define(OWNER,Optional.empty());entityData.define(RITUAL,false);entityData.define(EMERGE,new org.joml.Vector3f());}
    public int kind(){return entityData.get(KIND);}
    public int action(){return entityData.get(ACTION);}
    public long actionAge(){return Math.max(0,level().getGameTime()-entityData.get(ACTION_AT));}
    public Vec3 emergeOffset(){var v=entityData.get(EMERGE);return new Vec3(v.x,v.y,v.z);}
    public boolean ritual(){return entityData.get(RITUAL);}
    public UUID ownerId(){return entityData.get(OWNER).orElse(null);}
    public boolean ownedBy(Player p){return ownerId()!=null&&ownerId().equals(p.getUUID());}
    public long expires(){return expires;}
    public LivingEntity owner(){if(ownerId()==null)return null;if(level() instanceof ServerLevel l){Entity e=l.getEntity(ownerId());return e instanceof LivingEntity living?living:null;}return level().getPlayerByUUID(ownerId());}
    private void action(int action,int ticks){entityData.set(ACTION,action);entityData.set(ACTION_AT,level().getGameTime());actionEnd=level().getGameTime()+ticks;}
    public void configure(LivingEntity owner,int kind,boolean ritual){
        entityData.set(KIND,kind);entityData.set(OWNER,Optional.of(owner.getUUID()));entityData.set(RITUAL,ritual);
        bossOwned=owner instanceof MegumiEntity;
        stage=owner instanceof ServerPlayer p?SukunaBranches.stage(p):5;pack=BalanceRules.modpackMode();
        expires=level().getGameTime()+TenShadowRules.duration(kind);
        chargeDirection=owner.getLookAngle().multiply(1,0,1).normalize();if(chargeDirection.lengthSqr()<.1)chargeDirection=new Vec3(0,0,1);
        applyStats(false);specialCooldown=40;refreshDimensions();setCustomName(Component.literal((ritual?"调服 · ":"")+TenShadowRules.NAMES[kind]));
    }
    public void inheritTotality(){entityData.set(KIND,TenShadowRules.TOTALITY);expires=level().getGameTime()+TenShadowRules.duration(kind());applyStats(true);setCustomName(Component.literal("玉犬浑"));refreshDimensions();if(level() instanceof ServerLevel l)ShadowSystem.fx(l,0,position(),position(),1.8f,35);}
    private void applyStats(boolean keepRatio){
        float ratio=keepRatio&&getMaxHealth()>0?getHealth()/getMaxHealth():1;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(TenShadowRules.health(kind(),stage,pack));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(TenShadowRules.damage(kind(),stage,pack));
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(TenShadowRules.speed(kind(),stage));
        getAttribute(Attributes.ARMOR).setBaseValue(TenShadowRules.armor(kind()));
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(kind()==TenShadowRules.DEER?1:kind()==TenShadowRules.SERPENT?.9:.3);
        setHealth(getMaxHealth()*ratio);
    }
    public void emergeFrom(Vec3 source){
        Vec3 delta=source.subtract(position());entityData.set(EMERGE,new org.joml.Vector3f((float)delta.x,(float)delta.y,(float)delta.z));action(EMERGING,20);
        if(level() instanceof ServerLevel l){ShadowSystem.fx(l,0,source,source,Math.max(1,getBbWidth()),35);
            for(LivingEntity e:l.getEntitiesOfClass(LivingEntity.class,new AABB(source,source).inflate(1),this::validTarget))if(e.position().distanceToSqr(source)<1.01){Vec3 d=e.position().subtract(source);e.knockback(.15,-d.x,-d.z);}
        }
    }
    public void dissolve(){if(isRemoved()||action()==DISSOLVE)return;action(DISSOLVE,20);navigation.stop();setTarget(null);ShadowBindings.release(this);if(owner() instanceof MegumiEntity m)m.forgetGuard(this);if(owner() instanceof ServerPlayer p&&!ritual())ShadowSystem.forget(p,this);if(level() instanceof ServerLevel l)ShadowSystem.fx(l,0,position(),position(),Math.max(.65f,getBbWidth()),35);}
    @Override public EntityDimensions getDimensions(Pose pose){
        int k=entityData==null||!entityData.hasItem(KIND)?0:kind();return switch(k){case 2->EntityDimensions.scalable(1.1f,1.65f);case 3->EntityDimensions.scalable(1.5f,.9f);case 4->EntityDimensions.scalable(1.1f,.85f);case 5->EntityDimensions.scalable(2.2f,2.3f);case 6->EntityDimensions.scalable(2.5f,2.8f);case 7->EntityDimensions.scalable(.38f,.45f);case 8->EntityDimensions.scalable(1.2f,2.6f);case 9->EntityDimensions.scalable(1.7f,2.1f);case 10->EntityDimensions.scalable(1.4f,1.8f);default->EntityDimensions.scalable(.8f,1.25f);};
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(KIND.equals(key))refreshDimensions();}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    @Override public boolean isPushable(){return action()!=DISSOLVE&&kind()!=TenShadowRules.DEER;}
    @Override protected boolean shouldDropLoot(){return false;}
    public boolean validTarget(LivingEntity target){
        if(target==null||target==this||!target.isAlive()||target.level()!=level()||target.isSpectator())return false;
        LivingEntity owner=owner();if(ritual())return target==owner;
        if(target==owner||target instanceof ShadowEntity s&&Objects.equals(s.ownerId(),ownerId()))return false;
        if(owner instanceof ServerPlayer p)return CombatSafety.summonTarget(p,target)||(target==getLastHurtByMob()&&CombatSafety.canTarget(p,target));
        if(owner instanceof MegumiEntity m)return m.enemy(target);
        return false;
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(action()==DISSOLVE)return false;
        if(!ritual()&&(source.getEntity()==owner()||source.getEntity() instanceof ShadowEntity s&&!s.ritual()&&Objects.equals(s.ownerId(),ownerId())))return false;
        return super.hurt(source,amount);
    }
    @Override public void die(DamageSource source){super.die(source);if(!level().isClientSide&&dead&&!registeredDeath){registeredDeath=true;ShadowBindings.release(this);if(owner() instanceof MegumiEntity m)m.forgetGuard(this);ShadowSystem.defeated(this);if(level() instanceof ServerLevel l)ShadowSystem.fx(l,0,position(),position(),Math.max(.6f,getBbWidth()),35);}}
    private boolean strike(LivingEntity target,double multiplier){
        if(!validTarget(target)||!hasLineOfSight(target))return false;
        float damage=(float)(getAttributeValue(Attributes.ATTACK_DAMAGE)*multiplier);
        DamageSource source=damageSources().mobAttack(this);
        if(kind()==TenShadowRules.TOTALITY){damage=CombatRules.getDamageAfterAbsorb(damage,target.getArmorValue()*.35f,(float)target.getAttributeValue(Attributes.ARMOR_TOUGHNESS)*.35f);source=new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(PIERCE),this);}
        boolean accepted;try{AttackTypes.mark(AttackTypes.TEN_SHADOWS);accepted=owner() instanceof ServerPlayer p&&!ritual()?CombatSafety.hurt(p,target,source,damage):target.hurt(source,damage);}finally{AttackTypes.clear();}
        if(accepted){setLastHurtMob(target);if(owner() instanceof ServerPlayer p&&!ritual())target.setLastHurtByPlayer(p);}
        return accepted;
    }
    @Override public void tick(){
        super.tick();if(level().isClientSide||!isAlive())return;
        long now=level().getGameTime();
        if(action()==DISSOLVE){setDeltaMovement(Vec3.ZERO);if(now>=actionEnd)discard();return;}
        LivingEntity owner=owner();
        if(owner==null||!owner.isAlive()||owner instanceof MegumiEntity m&&m.downed()||expires>0&&now>=expires){dissolve();return;}
        if(bossOwned&&owner instanceof MegumiEntity m&&!m.guardAllowed(this)){dissolve();return;}
        if(!ritual()&&!bossOwned&&owner instanceof ServerPlayer p){if(!ShadowSystem.authorized(p,this)){dissolve();return;}ShadowSystem.restoreInheritance(p,this);}
        if(action()==EMERGING){navigation.stop();setDeltaMovement(Vec3.ZERO);if(now>=actionEnd)action(IDLE,0);return;}
        if(action()!=IDLE&&now>=actionEnd)action(IDLE,0);
        if(attackCooldown>0)attackCooldown--;if(specialCooldown>0)specialCooldown--;
        if(tickCount%40==0&&owner instanceof ServerPlayer p){int grown=SukunaBranches.stage(p);if(grown!=stage||pack!=BalanceRules.modpackMode()){stage=grown;pack=BalanceRules.modpackMode();applyStats(true);}}
        if(kind()==TenShadowRules.TIGER&&!ritual()&&tickCount%20==0&&owner instanceof ServerPlayer p&&!Curse.spend(p,300)){dissolve();return;}
        if(getTarget()!=null&&!validTarget(getTarget()))setTarget(null);
        if(kind()!=TenShadowRules.DEER&&kind()!=TenShadowRules.RABBIT&&tickCount%10==0)setTarget(ShadowSystem.target(this,owner));
        LivingEntity target=getTarget();
        if(kind()==TenShadowRules.WHITE||kind()==TenShadowRules.TOTALITY)scout();
        if(kind()==TenShadowRules.SERPENT||kind()==TenShadowRules.RABBIT)taunt();
        if(kind()==TenShadowRules.DEER){deer(owner);return;}
        if(kind()==TenShadowRules.OX){charge(owner,target);return;}
        if(kind()==TenShadowRules.NUE){fly(owner,target);return;}
        setNoGravity(false);
        if(kind()==TenShadowRules.TOAD&&tongue(target))return;
        if(kind()==TenShadowRules.RABBIT){follow(owner);if(onGround()&&tickCount%18==0)setDeltaMovement(getDeltaMovement().add(0,.28,0));return;}
        if(target==null){follow(owner);return;}
        face(target.position());
        if(kind()==TenShadowRules.ELEPHANT&&specialCooldown<=0&&distanceToSqr(target)<28*28&&hasLineOfSight(target)){water(target);return;}
        if((kind()==TenShadowRules.WHITE||kind()==TenShadowRules.TOTALITY)&&attackCooldown>16&&distanceToSqr(target)<25){Vec3 away=position().subtract(target.position()).normalize();navigation.moveTo(getX()+away.x*4,getY(),getZ()+away.z*4,1.1);return;}
        double reach=(getBbWidth()+target.getBbWidth())*.5+1;
        if(distanceToSqr(target)<reach*reach&&hasLineOfSight(target)){
            navigation.stop();if(attackCooldown<=0){action(ATTACK,18);strike(target,1);attackCooldown=kind()==TenShadowRules.TIGER?24:kind()==TenShadowRules.WHITE?48:kind()==TenShadowRules.TOTALITY?36:32;}
        }else if(tickCount%8==0)navigation.moveTo(target,1.1);
    }
    private void face(Vec3 at){Vec3 d=at.subtract(position());setYRot((float)(Math.toDegrees(Math.atan2(d.z,d.x))-90));yBodyRot=getYRot();yHeadRot=getYRot();}
    private void follow(LivingEntity owner){
        double distance=distanceToSqr(owner);if(distance>32*32&&tickCount%20==0){Vec3 at=ShadowSystem.findSpace((ServerLevel)level(),this,owner.position(),getId());if(at!=null){moveTo(at.x,at.y,at.z,getYRot(),0);navigation.stop();}return;}
        if(distance>9&&tickCount%10==0)navigation.moveTo(owner,1.15);else if(distance<4)navigation.stop();
    }
    private void scout(){if(tickCount%40!=0)return;for(LivingEntity e:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(55),x->x!=this&&x.isAlive()&&distanceToSqr(x)<=55*55))e.addEffect(new MobEffectInstance(MobEffects.GLOWING,60,0,false,false));}
    private void taunt(){if(tickCount%20!=0)return;for(Mob m:level().getEntitiesOfClass(Mob.class,getBoundingBox().inflate(16),x->validTarget(x)&&(x instanceof net.minecraft.world.entity.monster.Enemy||x.getTarget()==owner())))m.setTarget(this);}
    private boolean tongue(LivingEntity target){
        if(bound!=null){
            if(!validTarget(bound)||!ShadowBindings.boundBy(bound,this)){bound=null;action(IDLE,0);return false;}
            navigation.stop();setDeltaMovement(Vec3.ZERO);face(bound.position());
            if(tickCount%5==0)ShadowSystem.fx((ServerLevel)level(),3,position().add(0,.55,0),bound.getBoundingBox().getCenter(),.1f,7);
            return true;
        }
        if(target!=null&&specialCooldown<=0&&distanceToSqr(target)<12*12&&hasLineOfSight(target)){
            bound=target;ShadowBindings.bind(this,target,40);action(SPECIAL,40);specialCooldown=140;navigation.stop();strike(target,1);return true;
        }return false;
    }
    private void water(LivingEntity target){
        navigation.stop();action(SPECIAL,28);specialCooldown=100;
        boolean pressure=(tickCount/100)%2==0;Vec3 from=position().add(getLookAngle().scale(getBbWidth()*.5)).add(0,getBbHeight()*.65,0),to=target.getBoundingBox().getCenter();
        ShadowSystem.fx((ServerLevel)level(),2,from,to,pressure?.25f:1.1f,22);
        Vec3 direction=to.subtract(from).normalize();
        for(LivingEntity e:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(28),this::validTarget)){
            Vec3 delta=e.getBoundingBox().getCenter().subtract(from);double along=delta.dot(direction),side=delta.subtract(direction.scale(along)).length();
            if(along>=0&&along<=28&&side<(pressure?.6:2.8)+e.getBbWidth()/2&&hasLineOfSight(e)){strike(e,pressure?1.6:.3);if(!pressure){e.setDeltaMovement(e.getDeltaMovement().add(direction.scale(1.5)).add(0,.35,0));e.hurtMarked=true;}}
        }
    }
    private void fly(LivingEntity owner,LivingEntity target){
        navigation.stop();setNoGravity(true);fallDistance=0;
        if(target!=null&&specialCooldown<=0&&getY()>target.getY()+4){
            action(SPECIAL,24);specialCooldown=180;
            for(LivingEntity e:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(24),this::validTarget))if(distanceToSqr(e)<24*24&&hasLineOfSight(e)){ShadowSystem.fx((ServerLevel)level(),1,position(),e.getBoundingBox().getCenter(),.15f,14);strike(e,1.1);}
        }
        boolean dive=target!=null&&(tickCount%100)>=65;
        Vec3 base=target==null?owner.position():target.position();
        double angle=tickCount*.065;
        Vec3 desired=dive?target.getBoundingBox().getCenter():base.add(Math.cos(angle)*6,8+Math.sin(angle*.5)*2,Math.sin(angle)*6);
        Vec3 d=desired.subtract(position());setDeltaMovement(d.normalize().scale(Math.min(d.length()*.12,dive?.9:.45)));face(desired);
        if(horizontalCollision)setDeltaMovement(getDeltaMovement().add(0,.45,0));
        if(dive&&target!=null&&distanceToSqr(target)<9&&attackCooldown<=0){action(ATTACK,20);attackCooldown=45;strike(target,1.2);ShadowSystem.fx((ServerLevel)level(),1,position(),target.getBoundingBox().getCenter(),.12f,12);}
    }
    private void deer(LivingEntity owner){
        navigation.stop();setDeltaMovement(Vec3.ZERO);ServerLevel l=(ServerLevel)level();
        if(tickCount%20==0){
            heal(TenShadowRules.deerHeal());
            ShadowSystem.fx(l,4,position().add(0,1,0),position().add(0,2,0),1,20);
            if(!ritual()&&distanceToSqr(owner)<=2500){owner.heal(TenShadowRules.deerHeal());ShadowSystem.fx(l,4,position().add(0,1,0),owner.position().add(0,1,0),1,20);}
        }
        if(!ritual()||tickCount%10!=0)return;
        // 调服伤害圈覆盖靠近的玩家及挑战者式神；普通村民和自然生物不卷入仪式。
        for(LivingEntity e:l.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(8),x->x!=this&&x.isAlive()&&!x.isSpectator()&&(x instanceof Player||x instanceof ShadowEntity s&&Objects.equals(s.ownerId(),ownerId())))){
            if(TenShadowRules.deerAuraRange(distanceToSqr(e)))try{AttackTypes.mark(AttackTypes.TEN_SHADOWS);e.hurt(damageSources().mobAttack(this),TenShadowRules.deerAuraDamage(stage,pack));}finally{AttackTypes.clear();}
        }
        for(int i=0;i<24;i++){double angle=i*Math.PI/12;l.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,getX()+Math.cos(angle)*8,getY()+.25,getZ()+Math.sin(angle)*8,1,0,.12,0,0);}
    }
    private void finishCharge(){oxRun=0;oxAim=0;oxTarget=null;oxRecovery=TenShadowRules.OX_RECOVERY_TICKS;chargeDistance=0;lastChargePosition=null;setDeltaMovement(new Vec3(0,getDeltaMovement().y,0));action(IDLE,0);}
    private void charge(LivingEntity owner,LivingEntity target){
        setNoGravity(false);
        if(oxRun>0){
            navigation.stop();Vec3 previous=lastChargePosition;double travelled=previous==null?0:position().subtract(previous).horizontalDistance();
            // 跟随传送或外部推送不能给贯牛叠出无限伤害。
            if(travelled>3){finishCharge();return;}chargeDistance+=travelled;lastChargePosition=position();
            if(TenShadowRules.oxFinished(oxRun,chargeDistance,horizontalCollision)){finishCharge();return;}
            Vec3 swept=previous==null?Vec3.ZERO:previous.subtract(position());
            for(LivingEntity e:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().expandTowards(swept).inflate(.3),this::validTarget))if(!charged.contains(e.getUUID())&&strike(e,TenShadowRules.oxMultiplier(chargeDistance))){charged.add(e.getUUID());e.knockback(1,-chargeDirection.x,-chargeDirection.z);}
            setDeltaMovement(chargeDirection.scale(Math.min(1.15,.5+chargeDistance*.025)).add(0,getDeltaMovement().y,0));face(position().add(chargeDirection));
            if(--oxRun<=0)finishCharge();return;
        }
        if(oxRecovery>0){oxRecovery--;navigation.stop();setDeltaMovement(new Vec3(0,getDeltaMovement().y,0));return;}
        if(target==null||!validTarget(target)||!hasLineOfSight(target)||distanceToSqr(target)>32*32){oxAim=0;oxTarget=null;if(action()==SPECIAL)action(IDLE,0);if(target!=null)navigation.moveTo(target,1);else follow(owner);return;}
        navigation.stop();setDeltaMovement(new Vec3(0,getDeltaMovement().y,0));face(target.position());
        if(!target.getUUID().equals(oxTarget)){oxTarget=target.getUUID();oxAim=TenShadowRules.OX_AIM_TICKS;action(SPECIAL,TenShadowRules.OX_AIM_TICKS);}
        if(--oxAim>0)return;
        Vec3 direction=target.position().subtract(position()).multiply(1,0,1);if(direction.lengthSqr()<.01){oxAim=5;return;}
        chargeDirection=direction.normalize();chargeDistance=0;lastChargePosition=position();charged.clear();oxRun=TenShadowRules.OX_CHARGE_TICKS;action(ATTACK,oxRun);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);tag.putInt("ShadowKind",kind());if(ownerId()!=null)tag.putUUID("ShadowOwner",ownerId());tag.putBoolean("ShadowRitual",ritual());tag.putBoolean("ShadowBoss",bossOwned);tag.putLong("ShadowExpires",expires);tag.putInt("ShadowStage",stage);tag.putBoolean("ShadowPack",pack);tag.putInt("ShadowBalance",TenShadowRules.BALANCE_VERSION);tag.putBoolean("ShadowDissolving",action()==DISSOLVE);
        tag.putDouble("ChargeX",chargeDirection.x);tag.putDouble("ChargeZ",chargeDirection.z);tag.putDouble("ChargeDistance",chargeDistance);tag.putInt("OxRun",oxRun);tag.putInt("OxRecovery",oxRecovery);
        net.minecraft.nbt.ListTag hits=new net.minecraft.nbt.ListTag();for(UUID id:charged)hits.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));tag.put("ChargeHits",hits);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);entityData.set(KIND,Math.max(0,Math.min(10,tag.getInt("ShadowKind"))));entityData.set(OWNER,tag.hasUUID("ShadowOwner")?Optional.of(tag.getUUID("ShadowOwner")):Optional.empty());entityData.set(RITUAL,tag.getBoolean("ShadowRitual"));bossOwned=tag.getBoolean("ShadowBoss");expires=tag.getLong("ShadowExpires");stage=tag.getInt("ShadowStage");pack=tag.getBoolean("ShadowPack");
        if(tag.getInt("ShadowBalance")!=TenShadowRules.BALANCE_VERSION)applyStats(true);
        chargeDirection=new Vec3(tag.getDouble("ChargeX"),0,tag.getDouble("ChargeZ")).normalize();chargeDistance=Math.min(40,Math.max(0,tag.getDouble("ChargeDistance")));oxRun=Math.max(0,Math.min(TenShadowRules.OX_CHARGE_TICKS,tag.getInt("OxRun")));oxRecovery=Math.max(0,Math.min(TenShadowRules.OX_RECOVERY_TICKS,tag.getInt("OxRecovery")));
        oxAim=0;oxTarget=null;lastChargePosition=null;charged.clear();for(var hit:tag.getList("ChargeHits",8))try{charged.add(UUID.fromString(hit.getAsString()));}catch(IllegalArgumentException ignored){}
        if(oxRun>0&&chargeDirection.lengthSqr()>.1)action(ATTACK,oxRun);else oxRun=0;
        if(tag.getBoolean("ShadowDissolving"))action(DISSOLVE,1);refreshDimensions();
    }
}
