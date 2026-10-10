package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.*;

/** 仅技能攻击；动作由服务端同步，距离命中与隔空命中分别判定。 */
public class YujiEntity extends Monster {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(YujiEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(YujiEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DECOY=SynchedEntityData.defineId(YujiEntity.class,EntityDataSerializers.BOOLEAN);
    private final ServerBossEvent bar=new ServerBossEvent(Component.literal("虎杖残影 · 30000"),BossEvent.BossBarColor.WHITE,BossEvent.BossBarOverlay.NOTCHED_20);
    private final Set<UUID> participants=new HashSet<>();
    private final List<Delayed> delayed=new ArrayList<>();
    private float castBonus,domainBonus;
    private record Delayed(UUID target,long due,float bonus){}
    public UUID challenger;
    private UUID original;
    private int rest=40,weakness,domainTicks,comboCount,cloneLife=120,lastSkill;
    private boolean configured,familyMember;
    private long nextDomain;
    private Vec3 dash=Vec3.ZERO,domainCenter=Vec3.ZERO,threadTarget=Vec3.ZERO,cutPoint=Vec3.ZERO,cutDir=Vec3.ZERO;
    private long cutAngle;
    public boolean domainOverlaps(Vec3 center,double radius){return domainTicks>0&&domainCenter.distanceToSqr(center)<Math.pow(radius+48,2);}
    private boolean domainContains(LivingEntity p){return !DomainClash.active(this)&&domainTicks>0&&BossTargets.valid(this,p)&&p.position().distanceToSqr(domainCenter)<48*48;}
    private boolean sureHit(LivingEntity p){ServerPlayer caster=MaidBridge.progression(p);return domainContains(p)&&!(caster!=null&&Skills.neutralizes(caster));}
    private boolean dashHit,rageUsed,comboFinisher;
    public YujiEntity(EntityType<? extends Monster> type,Level level){super(type,level);setPersistenceRequired();xpReward=500;}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ACTION,0);entityData.define(AGE,0);entityData.define(DECOY,false);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));}
    public int action(){return entityData.get(ACTION);}public int actionAge(){return entityData.get(AGE);}public boolean decoy(){return entityData.get(DECOY);}
    public void setFamilyMember(){familyMember=true;setPersistenceRequired();xpReward=0;setTarget(null);entityData.set(ACTION,0);}
    public boolean familyMember(){return familyMember;}
    public boolean participated(UUID id){return participants.contains(id);}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    @Override public boolean doHurtTarget(Entity entity){return false;}
    @Override public boolean isPushable(){return !decoy();}
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);if(!decoy())bar.addPlayer(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);}
    private ServerLevel server(){return (ServerLevel)level();}
    private List<ServerPlayer> players(double radius){return server().getEntitiesOfClass(ServerPlayer.class,getBoundingBox().inflate(radius),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&distanceToSqr(p)<=radius*radius);}
    private void fx(int kind,Vec3 point,Vec3 direction,float size,int life,long seed){Network.send(server(),new Network.Effect(kind,getId(),point,direction,size,life,seed));}
    private void sound(SoundEvent sound,float pitch){level().playSound(null,blockPosition(),sound,SoundSource.HOSTILE,2,pitch);}
    private boolean block(LivingEntity p){return p.isBlocking()&&p.getLookAngle().dot(position().subtract(p.position()).normalize())>0;}
    private boolean strike(LivingEntity p,float amount,boolean soul,boolean respectFrames){return strike(p,amount,soul,respectFrames,castBonus);}
    private boolean strike(LivingEntity p,float amount,boolean soul,boolean respectFrames,float bonus){
        if(!BossTargets.valid(this,p)||!sureHit(p)&&(block(p)||respectFrames&&p.invulnerableTime>10))return false;
        if(p instanceof ServerPlayer)participants.add(p.getUUID());if(!respectFrames||sureHit(p))p.invulnerableTime=0;
        boolean flash=action()!=5&&(BossControl.value(this,"BlackFlash",100))>random.nextInt(100);
        float dealt=BalanceRules.damage(((flash?amount*2.5f:amount)+bonus)*BossControl.value(this,"YujiDamage",1));
        boolean hit=p.hurt(soul||sureHit(p)?damageSources().sonicBoom(this):damageSources().mobAttack(this),dealt);
        if(hit){fx(81,p.position().add(0,1,0),position(),1,8,p.getId());if(flash)fx(58,p.position(),Vec3.ZERO,dealt,10,p.getId());sound(flash?SoundEvents.GENERIC_EXPLODE:soul?SoundEvents.NOTE_BLOCK_BASEDRUM.value():SoundEvents.PLAYER_ATTACK_STRONG,flash?0.35f:soul?0.5f:0.8f);}return hit;
    }
    private boolean inReach(LivingEntity p){return sureHit(p)||distanceToSqr(p)<3.4*3.4&&hasLineOfSight(p);}
    private void begin(int skill){entityData.set(ACTION,skill);entityData.set(AGE,0);getNavigation().stop();dashHit=false;comboFinisher=false;lastSkill=skill;castBonus=BossRevelation.roll(this,BossRevelation.yuji(skill));if(skill==6)domainBonus=castBonus;
        if(skill!=7)SkillSounds.play(this,"yuji_"+skill);fx(80,position(),getLookAngle(),skill,skill==6?40:20,0);
        if(skill==5){comboCount=RevelationRules.quantity(5+random.nextInt(3),castBonus>0,20);for(ServerPlayer p:players(12))SoulCombat.suppress(p,20+comboCount*3);}
        if(skill==3&&getTarget()!=null)threadTarget=getTarget().position();
        if(skill==4)spawnEchoes();
    }
    private void finish(int ticks){entityData.set(ACTION,0);entityData.set(AGE,0);double frequency=BossControl.value(this,"YujiFrequency",1);rest=Math.max(1,(int)Math.ceil(Math.max(5,ticks/2)/frequency));setDeltaMovement(0,getDeltaMovement().y,0);getNavigation().stop();}
    @Override public void tick(){
        super.tick();if(level().isClientSide||!isAlive()||SixfoldFinale.controlled(this))return;
        if(familyMember){bar.setVisible(false);setTarget(null);getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);entityData.set(ACTION,0);return;}
        if(decoy()){tickDecoy();return;}
        if(weakness>0)weakness--;
        bar.setProgress(Math.max(0,Math.min(1,getHealth()/getMaxHealth())));bar.setName(Component.literal("虎杖残影 · "+(int)getHealth()+" / "+(int)getMaxHealth()+(weakness>0?" · 虚弱":"")));
        if(domainTicks>0)DomainClash.opened(this,domainCenter,48);
        if(DomainClash.active(this)||DomainClash.stunned(this)){DomainClash.hold(this);bar.setName(Component.literal("虎杖残影 · "+(int)getHealth()+" / "+(int)getMaxHealth()+(DomainClash.stunned(this)?" · 对拼败北僵直":" · 领域对拼")));return;}
        processDelayed();tickDomain();
        LivingEntity target=BossTargets.select(this,128);
        if(target==null){if(action()!=0){finish(30);clearEchoes();}return;}
        getLookControl().setLookAt(target,35,35);Vec3 face=target.position().subtract(position());setYRot((float)Math.toDegrees(Math.atan2(-face.x,face.z)));yBodyRot=getYRot();
        if(action()==0){
            rest-=Math.max(1,(int)Math.ceil(BossControl.value(this,"YujiFrequency",1)));
            if(rest>0){getNavigation().stop();return;}
            if(comboFinisher){begin(2);return;}
            if(getHealth()<getMaxHealth()*0.5f&&level().getGameTime()>=nextDomain&&domainTicks==0){begin(6);return;}
            if(getHealth()<getMaxHealth()*0.3f&&!rageUsed){rageUsed=true;begin(4);return;}
            if(distanceToSqr(target)>18*18){getNavigation().moveTo(target,1.2);return;}
            int[] choices={1,2,3,5,7};int choice=choices[random.nextInt(choices.length)];if(choice==lastSkill)choice=choices[(java.util.stream.IntStream.range(0,choices.length).filter(i->choices[i]==lastSkill).findFirst().orElse(0)+1+random.nextInt(choices.length-1))%choices.length];begin(choice);return;
        }
        int t=actionAge()+1;entityData.set(AGE,t);getNavigation().stop();
        switch(action()){
            case 1 -> {if(t<=7)advance(target,0.65);if(t==8&&inReach(target)&&strike(target,5,false,false)){delayed.add(new Delayed(target.getUUID(),level().getGameTime()+10,castBonus));}if(t>=20)finish(16);}
            case 2 -> {
                if(t<=20){setDeltaMovement(0,getDeltaMovement().y,0);if(t%4==0)fx(84,position().add(0,1.2,0),target.position(),1,6,0);}
                if(t==20)dash=target.position().subtract(position()).multiply(1,0,1).normalize();
                if(t>20&&t<=28){advance(dash,1.8);if(!dashHit&&sweptHit(target,dash.scale(1.8))){dashHit=strike(target,32,true,false);if(dashHit){target.push(dash.x*2,0.65,dash.z*2);target.hurtMarked=true;fx(85,target.getEyePosition(),dash,1,16,target.getId());}}}
                if(t>=29)finish(dashHit?12:20);
            }
            case 3 -> {
                setDeltaMovement(0,getDeltaMovement().y,0);
                if(t<=24&&t%4==0)fx(83,position().add(0,0.08,0),threadTarget,9,12,0);
                if(t==24)for(LivingEntity p:BossTargets.nearby(this,64))if(sureHit(p)||p.position().distanceToSqr(threadTarget)<9*9){if(p instanceof ServerPlayer)participants.add(p.getUUID());SoulCombat.gap(p,60+(castBonus>0?120:0),getId());fx(87,p.position(),p.position().add(0,1.2,-2),1,60,p.getId());}
                if(t>=48){weakness=15;finish(16);}
            }
            case 4 -> {
                if(t%18==1){Vec3 back=target.position().subtract(target.getLookAngle().multiply(1,0,1).normalize().scale(4));if(level().noCollision(this,getBoundingBox().move(back.subtract(position()))))setPos(back.x,back.y,back.z);}
                advance(target,0.8);if(t%12==0&&inReach(target))strike(target,7,true,false);
                if(t>=110){clearEchoes();finish(25);}
            }
            case 5 -> {
                if(t<12)setDeltaMovement(0,getDeltaMovement().y,0);
                if(t>=12&&t<18)advance(target,0.9);
                if(t>=18&&(t-18)%3==0&&(t-18)/3<comboCount){int punch=(t-18)/3;fx(86,position().add(0,1.3,0),target.position().add(0,1,0),1,5,punch);if(inReach(target)&&strike(target,punch==comboCount-1?5:3,false,false)){ServerPlayer caster=MaidBridge.progression(target);if(caster!=null){SoulCombat.stiff(caster);SoulCombat.suppress(caster,12);if(SoulCombat.stacks(caster)>=5)comboFinisher=true;}else target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,12,2));}if(punch==comboCount-1)sound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(),0.45f);}
                if(t>=18+comboCount*3){Vec3 retreat=position().subtract(target.position()).multiply(1,0,1).normalize().scale(0.65);finish(30);setDeltaMovement(retreat.x,0.15,retreat.z);}
            }
            case 6 -> {
                setDeltaMovement(0,getDeltaMovement().y,0);
                if(t==40){domainCenter=position();domainTicks=castBonus>0?600:300;nextDomain=level().getGameTime()+3600;sound(SoundEvents.BELL_BLOCK,0.55f);fx(90,domainCenter,Vec3.ZERO,48,300,level().getGameTime());DomainClash.opened(this,domainCenter,48);if(!DomainClash.active(this))for(ServerPlayer p:players(48))SoulCombat.iwate(p,3);}
                if(t>=42)finish(18);
            }
            case 7 -> {
                if(t==10){cutPoint=target.getBoundingBox().getCenter();cutDir=target.position().subtract(position()).normalize();cutAngle=2160+random.nextInt(360);fx(96,cutPoint,cutDir,5,6,cutAngle);if(sureHit(target)||hasLineOfSight(target))strike(target,4.2f,true,false);}
                if(t==16){fx(72,cutPoint,cutDir,5,SlashMotion.LIFE,cutAngle);if(sureHit(target)||target.getBoundingBox().inflate(0.3).contains(cutPoint))strike(target,12,true,false);sound(SoundEvents.PLAYER_ATTACK_SWEEP,0.65f);}
                if(t>=20)finish(12);
            }
            default -> finish(20);
        }
    }
    private void advance(LivingEntity target,double speed){advance(target.position().subtract(position()).multiply(1,0,1).normalize(),speed);}
    private void advance(Vec3 dir,double speed){setDeltaMovement(dir.x*speed,getDeltaMovement().y,dir.z*speed);}
    private boolean sweptHit(LivingEntity p,Vec3 motion){return p.getBoundingBox().inflate(0.7).clip(position().add(0,1,0),position().add(0,1,0).add(motion)).isPresent()||inReach(p);}
    private void processDelayed(){for(Iterator<Delayed> it=delayed.iterator();it.hasNext();){Delayed d=it.next();if(level().getGameTime()<d.due)continue;it.remove();Entity entity=server().getEntity(d.target);if(entity instanceof LivingEntity p&&strike(p,12,true,true,d.bonus)){ServerPlayer caster=MaidBridge.progression(p);if(caster!=null)SoulCombat.peel(caster,false);SkillSounds.play(this,"yuji_soul_impact");}}}
    private void tickDomain(){if(domainTicks<=0||DomainClash.active(this))return;domainTicks--;
        if(domainTicks==0){endDomain();return;}
        // 持续补发剩余时间，支持玩家中途进场和客户端重新载入。
        if(domainTicks%20==0)fx(90,domainCenter,Vec3.ZERO,48,domainTicks,level().getGameTime());
        for(LivingEntity p:BossTargets.around(this,domainCenter,48,true)){
            if(p instanceof YujiEntity echo&&getUUID().equals(echo.original))continue;
            ServerPlayer caster=MaidBridge.progression(p);if(caster!=null){SoulCombat.iwate(caster,3);if(Skills.neutralizes(caster))continue;}
            if(domainTicks%10==0&&p.getDeltaMovement().horizontalDistanceSqr()>0.002)server().playSound(null,p.blockPosition(),SoundEvents.GRASS_STEP,SoundSource.PLAYERS,0.35f,0.85f);
            if(domainTicks%40==10)fx(81,p.position(),Vec3.ZERO,2,10,p.getId());
            if(domainTicks%40==0&&strike(p,8,true,false,domainBonus)&&caster!=null){SoulCombat.peel(caster,true);Curse.status(caster,"SoulHealHalf",100);}
        }
        if(domainTicks%60==0){SkillSounds.play(this,"iwate_wind");sound(SoundEvents.PARROT_AMBIENT,0.7f);}
    }
    private void endDomain(){domainTicks=0;fx(91,domainCenter,Vec3.ZERO,48,1,0);}
    public boolean clashDomainActive(){return domainTicks>0&&!decoy()&&!familyMember;}
    public void extendDomain(int ticks){if(domainTicks>0)domainTicks+=ticks;}
    public void clashCloseDomain(){endDomain();}
    public void clashInterrupt(){delayed.clear();clearEchoes();finish(20);}
    public void clashVisual(boolean show){if(show&&domainTicks>0)fx(90,domainCenter,Vec3.ZERO,48,domainTicks,level().getGameTime());else fx(91,domainCenter,Vec3.ZERO,48,1,0);}
    private void spawnEchoes(){clearEchoes();LivingEntity target=getTarget();if(target==null)return;
        for(int i=0;i<6;i++){double a=i*Math.PI/3;YujiEntity echo=SoulContent.YUJI.get().create(level());if(echo==null)continue;echo.entityData.set(DECOY,true);echo.original=getUUID();echo.noPhysics=true;echo.moveTo(target.getX()+Math.cos(a)*7,target.getY(),target.getZ()+Math.sin(a)*7,0,0);echo.setTarget(target);echo.setHealth(1);echo.xpReward=0;level().addFreshEntity(echo);}
    }
    private void tickDecoy(){noPhysics=true;setNoGravity(true);setDeltaMovement(getDeltaMovement().x,0,getDeltaMovement().z);if(--cloneLife<=0||original==null||!(server().getEntity(original) instanceof YujiEntity boss)||!boss.isAlive()||boss.action()!=4){discard();return;}LivingEntity target=getTarget();if(target==null){setTarget(boss.getTarget());return;}entityData.set(ACTION,4);entityData.set(AGE,120-cloneLife);getLookControl().setLookAt(target,30,30);advance(target,0.7);if(cloneLife%15==0&&inReach(target)){strike(target,2,false,false);sound(SoundEvents.PLAYER_ATTACK_WEAK,1.5f);}}
    private void clearEchoes(){if(level().isClientSide)return;for(YujiEntity e:server().getEntitiesOfClass(YujiEntity.class,getBoundingBox().inflate(192)))if(getUUID().equals(e.original))e.discard();}
    /** 百分比部分在术式入口减半，固定伤害不被误判为百分比。 */
    public static float percent(LivingEntity target,float amount){return target instanceof YujiEntity?amount*0.5f:amount;}
    @Override public boolean hurt(DamageSource source,float amount){if(level().isClientSide)return false;if(decoy()){fx(87,position(),position().add(0,1,0),1,10,0);discard();return true;}
        if(familyMember)return false;
        ServerPlayer caster=CombatSettings.caster(source);if(caster!=null)participants.add(caster.getUUID());
        boolean hit=super.hurt(source,source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)?amount:amount*0.8f*(weakness>0?1.5f:1));
        if(hit&&action()==4){clearEchoes();weakness=20;finish(20);}return hit;
    }
    @Override public void die(DamageSource source){super.die(source);if(!level().isClientSide){clearEchoes();endDomain();}bar.removeAllPlayers();}
    @Override public void remove(RemovalReason reason){bar.removeAllPlayers();if(!level().isClientSide&&domainTicks>0){fx(91,domainCenter,Vec3.ZERO,48,1,0);if(!reason.shouldSave())domainTicks=0;}super.remove(reason);}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.PLAYER_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.PLAYER_DEATH;}
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);tag.putLong("NextDomain",nextDomain);tag.putBoolean("Configured",configured);
        tag.putInt("DomainTicks",domainTicks);tag.putDouble("DomainX",domainCenter.x);tag.putDouble("DomainY",domainCenter.y);tag.putDouble("DomainZ",domainCenter.z);
        tag.putBoolean("RageUsed",rageUsed);tag.putBoolean("Decoy",decoy());tag.putBoolean("FamilyMember",familyMember);if(challenger!=null)tag.putUUID("Challenger",challenger);if(original!=null)tag.putUUID("Original",original);int i=0;for(UUID id:participants)tag.putUUID("Participant"+i++,id);tag.putInt("Participants",i);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);nextDomain=tag.getLong("NextDomain");
        // 旧存档受伤的首领保留血量，不能再次配置时被回满而错过半血领域。
        configured=tag.contains("Configured")?tag.getBoolean("Configured"):getHealth()<getMaxHealth();
        domainTicks=Math.max(0,Math.min(1200000,tag.getInt("DomainTicks")));
        domainCenter=new Vec3(tag.getDouble("DomainX"),tag.getDouble("DomainY"),tag.getDouble("DomainZ"));
        rageUsed=tag.getBoolean("RageUsed");familyMember=tag.getBoolean("FamilyMember");entityData.set(DECOY,tag.getBoolean("Decoy"));if(tag.hasUUID("Challenger"))challenger=tag.getUUID("Challenger");if(tag.hasUUID("Original"))original=tag.getUUID("Original");for(int i=0;i<Math.min(100,tag.getInt("Participants"));i++)if(tag.hasUUID("Participant"+i))participants.add(tag.getUUID("Participant"+i));finish(40);
    }
}
