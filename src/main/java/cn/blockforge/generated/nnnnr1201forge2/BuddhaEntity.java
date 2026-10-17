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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.sounds.*;
import net.minecraft.core.particles.ParticleTypes;

/** 十种术式用服务器计时器判定；客户端只接收演出。 */
public class BuddhaEntity extends Monster {
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(BuddhaEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(BuddhaEntity.class,EntityDataSerializers.INT);
    private final ServerBossEvent bar=new ServerBossEvent(Component.literal("即身佛 · 入定"),BossEvent.BossBarColor.YELLOW,BossEvent.BossBarOverlay.NOTCHED_20);
    private final ServerBossEvent countdown=new ServerBossEvent(Component.literal("即身佛 · 入灭"),BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.PROGRESS);
    private int actionTicks,rest=160,sequence,weakness,stun,darkness,melee;
    private boolean configured;
    private boolean transitionDone,finaleUsed,revealed;
    private int revelations;
    private float castBonus;
    private float revealPower(){return revealed?2.5f:1;}
    private double revealRange(double radius){return radius+(revealed?12:0);}
    private Vec3 anchor=Vec3.ZERO,echoOrigin=Vec3.ZERO,echoDirection=Vec3.ZERO;
    private boolean echoVertical;
    private final Set<UUID> echoHits=new HashSet<>();
    private UUID mother;
    private int motherTimer;
    private final List<Mark> marks=new ArrayList<>();
    private record Mark(Vec3 pos,int due,float radius,float damage,int type,float bonus){}
    public BuddhaEntity(EntityType<? extends Monster> type,Level level){super(type,level);xpReward=100;setPersistenceRequired();countdown.setVisible(false);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(PHASE,1);entityData.define(ACTION,0);}
    public int phase(){return entityData.get(PHASE);}
    public int action(){return entityData.get(ACTION);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));}
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);bar.addPlayer(p);countdown.addPlayer(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);countdown.removePlayer(p);}
    @Override public boolean removeWhenFarAway(double d){return false;}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    private ServerLevel server(){return (ServerLevel)level();}
    private List<ServerPlayer> players(double radius){return server().getEntitiesOfClass(ServerPlayer.class,getBoundingBox().inflate(radius),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&p.distanceToSqr(this)<=radius*radius);}
    private void effect(int kind,Vec3 pos,Vec3 direction,float size,int life,long seed){Network.send(server(),new Network.Effect(kind,getId(),pos,direction,size,life,seed));}
    private void sound(SoundEvent sound,float pitch){level().playSound(null,blockPosition(),sound,SoundSource.HOSTILE,3,pitch);}
    private void tell(String text){for(ServerPlayer p:players(64))p.displayClientMessage(Component.literal(text),true);}
    public boolean interruptSiphon(){if(level().isClientSide||action()!=1)return false;finish(40);effect(37,position(),Vec3.ZERO,12,30,0);tell("虹吸已打断 · 即身佛硬直2秒");return true;}
    private void finish(int recovery){entityData.set(ACTION,0);actionTicks=0;double frequency=BossControl.value(this,"BuddhaFrequency",1);rest=Math.max(1,(int)Math.ceil(Math.max(1,recovery)/frequency));stun=recovery;setNoGravity(false);getNavigation().stop();}
    private void begin(int action){
        entityData.set(ACTION,action);actionTicks=0;anchor=position();getNavigation().stop();
        String[] names={"","咒力虹吸 · 离开12格或使用断咒符","寄生佛·绽放 · 优先清理寄生佛","神化威压 · 跳跃躲首波，格挡或位移躲后续","即身佛·千手 · 离开金色预警圈","咒物反噬 · 停手5秒","即身佛·开眼 · 黑暗与减速，远离首领等待黑暗消退","肉体再生 · 回血期间减伤，保留输出等结束后1.5秒虚弱","宿傩残响 · 直线斩击，竖斩向侧面闪避，横斩跳跃躲避","寄生佛·母体 · 30秒内击杀母体","即身佛·入灭 · 15秒内斩杀！","即身佛 · 开眼转生：推开近处目标并施加黑暗，远离40格等待转生结束"};
        castBonus=BossRevelation.roll(this,names[action]);revealed=castBonus>0;
        bar.setName(Component.literal("即身佛 · "+(phase()==2?"开眼":"入定")+(revealed?" · 术式公开":"")));
        if(revealed)effect(66,position(),Vec3.ZERO,12,60,action);
        SkillSounds.play(this,"buddha_"+action);
        effect(30,position(),Vec3.ZERO,action==1?12:action==10?30:8,action==10?300:action==11?80:100,action);
        sound(action==8?SoundEvents.PLAYER_ATTACK_SWEEP:SoundEvents.BELL_BLOCK,action==11?0.5f:0.65f);
        if(action==8){echoHits.clear();echoOrigin=position();LivingEntity t=getTarget();echoDirection=t==null?getLookAngle():t.position().subtract(position()).multiply(1,0,1).normalize();if(echoDirection.lengthSqr()<0.1)echoDirection=new Vec3(0,0,1);echoVertical=random.nextBoolean();effect(34,echoOrigin,echoDirection,28,16,echoVertical?1:0);}
        if(action==11){setNoGravity(true);for(LivingEntity p:BossTargets.nearby(this,revealRange(40))){pushAway(p,revealed?5.8:2.8,0.6);p.addEffect(new MobEffectInstance(MobEffects.DARKNESS,revealed?300:100,0));}bar.setColor(BossEvent.BossBarColor.RED);effect(36,position(),Vec3.ZERO,40,80,0);}
        if(action==10){countdown.setVisible(true);countdown.setProgress(1);}
    }
    @Override public void tick(){
        super.tick();if(level().isClientSide||!isAlive()||SixfoldFinale.controlled(this))return;
        bar.setProgress(Math.max(0,Math.min(1,getHealth()/getMaxHealth())));
        if(weakness>0)weakness--;if(darkness>0)darkness--;if(melee>0)melee--;
        if(darkness>0&&tickCount%20==0)for(LivingEntity p:BossTargets.nearby(this,48))p.addEffect(new MobEffectInstance(MobEffects.DARKNESS,40,0));
        if(action()!=0&&tickCount%24==0){sound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(),0.55f);if(tickCount%48==0)sound(SoundEvents.EVOKER_PREPARE_SUMMON,0.5f);}
        BossTargets.select(this,64);
        if(getTarget()!=null){getLookControl().setLookAt(getTarget(),30,30);Vec3 facing=getTarget().position().subtract(position());setYRot((float)Math.toDegrees(Math.atan2(-facing.x,facing.z)));yBodyRot=getYRot();}
        processMarks();processMother();
        if(!transitionDone&&getHealth()<=getMaxHealth()*0.5f){transitionDone=true;entityData.set(PHASE,2);setHealth(getMaxHealth());begin(11);return;}
        if(transitionDone&&action()!=11&&!finaleUsed&&getHealth()<getMaxHealth()*0.1f){finaleUsed=true;begin(10);}
        if(action()==0){
            if(stun>0){stun--;getNavigation().stop();return;}
            LivingEntity target=getTarget();if(target==null)return;
            rest-=Math.max(1,(int)Math.ceil(BossControl.value(this,"BuddhaFrequency",1)));
             if(rest<=0){if(phase()==1)begin(1+(sequence++%5));else {int[] order={6,1,8,2,7,3,9,4,5};begin(order[sequence++%order.length]);}return;}
            getNavigation().moveTo(target,darkness>0?1.2:1.0);
            if(distanceToSqr(target)<6&&melee<=0){doHurtTarget(target);melee=darkness>0?15:20;}
            return;
        }
        actionTicks++;int t=actionTicks;
        if(action()!=11&&action()!=1)setDeltaMovement(0,getDeltaMovement().y,0);
        switch(action()){
            case 1 -> {
                setNoGravity(true);setDeltaMovement(0,(anchor.y+1-getY())*0.12,0);
                if(t%10==0){for(LivingEntity p:BossTargets.nearby(this,revealRange(12))){double drain=p instanceof ServerPlayer player?BalanceRules.strength(Curse.drain(player,(phase()==2?0.08:0.05)*(revealed?2:1)),BalanceRules.Category.BOSS_ATTACK):BalanceRules.strength(Math.min(20,p.getMaxHealth()*0.05),BalanceRules.Category.BOSS_ATTACK);if(!(p instanceof Player))strike(p,(float)BalanceRules.raw(drain,BalanceRules.Category.BOSS_ATTACK));heal((float)drain);effect(31,p.getBoundingBox().getCenter(),getBoundingBox().getCenter(),1,12,p.getId());}if(t%40==0){sound(SoundEvents.WARDEN_HEARTBEAT,0.65f);sound(SoundEvents.EVOKER_PREPARE_SUMMON,0.5f);}}
                if(t>=100)finish(35);
            }
            case 2 -> {if(t==20){int count=((phase()==2?10:6)+random.nextInt(3))*(revealed?2:1);for(int i=0;i<count;i++)spawnParasite(false,i*Math.PI*2/count);sound(SoundEvents.GHAST_AMBIENT,1.8f);}if(t>=45)finish(35);}
            case 3 -> {if(t==20||t==26||t==32){int wave=(t-20)/6;effect(32,position(),Vec3.ZERO,(float)revealRange(16),20,wave);for(LivingEntity p:BossTargets.nearby(this,revealRange(16))){if(wave==0&&p.getY()>getY()+0.45)continue;if(blocked(p)){effect(37,p.position(),Vec3.ZERO,2,12,0);continue;}strike(p,6);pushAway(p,0.9,0.3);if(p instanceof ServerPlayer player){Curse.status(player,"Disorder",160);Curse.sync(player);}}sound(SoundEvents.WARDEN_SONIC_BOOM,0.7f);}if(t>=55)finish(35);}
            case 4 -> {if(t>=10&&t<=115&&t%7==3){LivingEntity target=getTarget();if(target!=null){boolean last=t==115;Vec3 point=target.position();float radius=(float)revealRange(last?5:2.5);marks.add(new Mark(point,tickCount+20,radius,last?18:9,1,castBonus));effect(33,point,position().add(0,3,0),radius,20,last?1:0);}}if(t>=145)finish(30);}
            case 5 -> {if(t%20==0)effect(35,position(),Vec3.ZERO,3,25,0);if(t>=100)finish(20);}
            case 6 -> {if(t==1){darkness=revealed?800:400;for(LivingEntity p:BossTargets.nearby(this,revealRange(48))){p.addEffect(new MobEffectInstance(MobEffects.DARKNESS,darkness,0));if(revealed)p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,160,1));}addEffect(new MobEffectInstance(MobEffects.GLOWING,darkness,0));effect(38,position(),Vec3.ZERO,(float)revealRange(48),darkness,0);}if(t>=25)finish(20);}
            case 7 -> {if(t==1)effect(39,position(),Vec3.ZERO,4,60,0);if(t>=60){heal(BalanceRules.damage(revealed?2000:750));weakness=30;finish(30);tell("再生结束 · 1.5秒内承受伤害增加50%");}}
            case 8 -> {if(t==16){effect(40,echoOrigin,echoDirection,28,18,echoVertical?1:0);sound(SoundEvents.WARDEN_SONIC_BOOM,1.3f);}if(t>=16&&t<=30){double front=(t-16)*2;for(LivingEntity p:BossTargets.nearby(this,40)){Vec3 q=p.position().subtract(echoOrigin);double f=q.dot(echoDirection),side=Math.abs(q.x*echoDirection.z-q.z*echoDirection.x);if(Math.abs(f-front)>1.5||echoHits.contains(p.getUUID()))continue;boolean hit=echoVertical?side<2.2:side<15&&p.getY()<echoOrigin.y+0.8&&!p.isCrouching();if(hit){echoHits.add(p.getUUID());strike(p,echoVertical?18:24);}}if(echoVertical&&t>16&&(t-16)%2==0){Vec3 q=echoOrigin.add(echoDirection.scale(front));marks.add(new Mark(q,tickCount+20,2.5f,12,2,castBonus));effect(33,q,Vec3.ZERO,2.5f,20,2);}}if(t>=45)finish(30);}
            case 9 -> {if(t==20&&mother==null){ParasiteEntity mob=spawnParasite(true,0);if(mob!=null){mother=mob.getUUID();motherTimer=0;if(revealed){BossControl.revealedMother(mob);}}weakness=600;}if(t>=40&&mother==null)finish(30);}
            case 10 -> {countdown.setProgress(Math.max(0,1-t/300f));countdown.setName(Component.literal("入灭 · 剩余 "+Math.max(0,(300-t+19)/20)+" 秒"));if(t%40==0){float radius=8+t/10f;effect(32,position(),Vec3.ZERO,radius,24,t>=200?3:0);for(LivingEntity p:BossTargets.nearby(this,radius))if(!blocked(p))strike(p,5+t/40f);}if(t==200)effect(41,position(),Vec3.ZERO,40,100,0);if(t>=300){setHealth(getMaxHealth()*0.3f);countdown.setVisible(false);finish(40);tell("入灭未被阻止 · 即身佛恢复至30%生命");}}
            case 11 -> {setDeltaMovement(0,(anchor.y+2-getY())*0.1,0);if(t==40)sound(SoundEvents.GHAST_AMBIENT,1.8f);if(t>=80){sound(SoundEvents.PLAYER_ATTACK_SWEEP,0.5f);finish(20);sequence=0;}}
            default -> finish(30);
        }
        if(darkness>0&&tickCount%45==0){LivingEntity target=getTarget();if(target!=null){Vec3 q=target.position().add(random.nextInt(9)-4,0,random.nextInt(9)-4);marks.add(new Mark(q,tickCount+30,1.8f,0,3,0));effect(33,q,Vec3.ZERO,1.8f,30,3);}}
        if(tickCount%12==0)server().sendParticles(phase()==1?ParticleTypes.ENCHANT:ParticleTypes.SMOKE,getX(),getY()+1.2,getZ(),5,0.8,1,0.8,0.02);
    }
    private boolean blocked(LivingEntity p){return p.isBlocking()&&p.getLookAngle().dot(position().subtract(p.position()).normalize())>0;}
    private void strike(LivingEntity p,float amount){strike(p,amount,castBonus);}
    private void strike(LivingEntity p,float amount,float bonus){if(!BossTargets.hostile(this,p))return;p.invulnerableTime=0;p.hurt(damageSources().mobAttack(this),BalanceRules.damage((amount+bonus)*BossControl.value(this,"BuddhaDamage",1),BalanceRules.Category.BOSS_ATTACK));}
    private void pushAway(LivingEntity p,double force,double up){Vec3 d=p.position().subtract(position()).multiply(1,0,1).normalize();p.push(d.x*force,up,d.z*force);p.hurtMarked=true;}
    private void processMarks(){for(Iterator<Mark> it=marks.iterator();it.hasNext();){Mark m=it.next();if(tickCount<m.due)continue;effect(m.type==2?40:32,m.pos,new Vec3(0,1,0),m.radius,18,m.type);for(LivingEntity p:BossTargets.nearby(this,64)){if(p.position().distanceToSqr(m.pos)>m.radius*m.radius)continue;if(m.type==3){p.addEffect(new MobEffectInstance(MobEffects.GLOWING,60,0));continue;}strike(p,m.damage,m.bonus);p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,10,6));if(m.radius>=5)pushAway(p,0.6,0.8);}sound(SoundEvents.ANVIL_LAND,0.6f);it.remove();}}
    private ParasiteEntity spawnParasite(boolean large,double angle){ParasiteEntity mob=(large?Content.MOTHER:Content.PARASITE).get().create(level());if(mob==null)return null;mob.moveTo(getX()+Math.cos(angle)*3,getY(),getZ()+Math.sin(angle)*3,0,0);mob.owner=getUUID();mob.setTarget(getTarget());level().addFreshEntity(mob);effect(37,mob.position(),Vec3.ZERO,large?5:1,25,1);return mob;}
    private void processMother(){if(mother==null)return;Entity entity=server().getEntity(mother);motherTimer++;if(entity instanceof ParasiteEntity m&&m.isAlive()){if(motherTimer>=600){heal(getMaxHealth()*0.1f);m.owner=null;m.discard();mother=null;weakness=0;finish(30);tell("母体存活30秒 · 即身佛恢复10%生命");}}else if(entity!=null||motherTimer>40){mother=null;weakness=0;if(action()==9)finish(30);}}
    public void motherKilled(){mother=null;weakness=0;invulnerableTime=0;float amount=getMaxHealth()*0.15f;if(!finaleUsed)amount=Math.min(amount,Math.max(0,getHealth()-getMaxHealth()*0.09f));super.hurt(damageSources().genericKill(),amount);if(isAlive())finish(30);}
    @Override public boolean hurt(DamageSource source,float amount){
        if(level().isClientSide)return false;
        // 只反弹生物的近战/投射物，反伤本身和持续伤害不能递归反弹。
        if(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return super.hurt(source,amount);
        boolean reflect=action()==5&&source.getEntity() instanceof LivingEntity attacker&&BossTargets.valid(this,attacker)&&(source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)||source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)||source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile);
        float adjusted=amount*(action()==7?0.5f:0.9f)*(weakness>0?1.5f:1);
        if(!transitionDone)adjusted=Math.min(adjusted,Math.max(0,getHealth()-getMaxHealth()*0.5f));
        if(action()==11)adjusted=Math.min(adjusted,Math.max(0,getHealth()-1));
        if(transitionDone&&!finaleUsed&&action()!=11)adjusted=Math.min(adjusted,Math.max(0,getHealth()-getMaxHealth()*0.09f));
        boolean hit=super.hurt(source,adjusted);
        if(hit&&reflect&&source.getEntity() instanceof LivingEntity p){p.hurt(damageSources().sonicBoom(this),BalanceRules.damage(amount*(revealed?0.85f:phase()==2?0.45f:0.3f),BalanceRules.Category.BOSS_ATTACK));effect(42,p.getEyePosition(),Vec3.ZERO,amount*(revealed?0.85f:phase()==2?0.45f:0.3f),12,p.getId());sound(SoundEvents.AMETHYST_BLOCK_HIT,1.8f);}
        return hit;
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){super.dropCustomDeathLoot(source,looting,recentlyHit);spawnAtLocation(Content.BODY.get());spawnAtLocation(Content.RELIC.get());spawnAtLocation(ProgressionContent.FETUS.get());effect(43,position(),Vec3.ZERO,8,80,0);sound(SoundEvents.PLAYER_LEVELUP,0.6f);}
    @Override public void die(DamageSource source){super.die(source);bar.removeAllPlayers();countdown.removeAllPlayers();if(!level().isClientSide){for(ParasiteEntity e:server().getEntitiesOfClass(ParasiteEntity.class,getBoundingBox().inflate(96)))if(getUUID().equals(e.owner))e.discard();}}
    @Override public void remove(RemovalReason reason){super.remove(reason);bar.removeAllPlayers();countdown.removeAllPlayers();}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.SKELETON_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.WITHER_DEATH;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("Transition",transitionDone);tag.putBoolean("FinaleUsed",finaleUsed);tag.putInt("Sequence",sequence);tag.putInt("Action",action());tag.putInt("ActionTicks",actionTicks);tag.putInt("Darkness",darkness);if(mother!=null)tag.putUUID("Mother",mother);tag.putInt("MotherTimer",motherTimer);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);transitionDone=tag.getBoolean("Transition");finaleUsed=tag.getBoolean("FinaleUsed");entityData.set(PHASE,transitionDone?2:1);bar.setColor(transitionDone?BossEvent.BossBarColor.RED:BossEvent.BossBarColor.YELLOW);sequence=tag.getInt("Sequence");darkness=tag.getInt("Darkness");if(tag.hasUUID("Mother"))mother=tag.getUUID("Mother");motherTimer=tag.getInt("MotherTimer");int saved=tag.getInt("Action");if(saved==10){entityData.set(ACTION,10);actionTicks=tag.getInt("ActionTicks");countdown.setVisible(true);}else{entityData.set(ACTION,0);setNoGravity(false);rest=40;}}
}
