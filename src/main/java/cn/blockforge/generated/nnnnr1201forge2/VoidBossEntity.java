package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.*;

/** 所有危险区域由服务端逐刻判定，客户端只负责展示同一组坐标。 */
public final class VoidBossEntity extends Monster {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(VoidBossEntity.class,EntityDataSerializers.INT),AGE=SynchedEntityData.defineId(VoidBossEntity.class,EntityDataSerializers.INT),PHASE=SynchedEntityData.defineId(VoidBossEntity.class,EntityDataSerializers.INT);
    private static final UUID EYE_SLOW=UUID.fromString("731c3ef0-7dc0-422e-b4cd-0fae77215392");
    private final ServerBossEvent bar=new ServerBossEvent(Component.literal("构筑咒胎受肉：虚无相"),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.NOTCHED_20);
    private final Set<UUID> participants=new HashSet<>();
    private final List<Hazard> rifts=new ArrayList<>();
    private final List<Chain> chains=new ArrayList<>();
    private final List<Lock> locks=new ArrayList<>();
    private final List<Integer> deck=new ArrayList<>();
    private final Set<UUID> novaHits=new HashSet<>();
    private Vec3 home,mark=Vec3.ZERO,direction=Vec3.ZERO;
    private UUID selected;
    private int assault;private Vec3 assaultMark=Vec3.ZERO,assaultDirection=Vec3.ZERO;
    private final Set<UUID> pierced=new HashSet<>();
    private float castBonus;
    private int rest=60,weakness,eye,descent,nextRift,cage,phaseSeen=1,absence;
    private boolean reborn,rewarded,configured;
    private Vec3 cageCenter=Vec3.ZERO;
    private static final class Hazard {int id,life,age,locked;double radius;float bonus;Vec3 point;Hazard(int id,Vec3 p,double r,int life,float bonus){this.id=id;point=p;radius=r;this.life=life;this.bonus=bonus;}}
    private static final class Chain {Vec3 point;UUID victim;int age;float bonus;Chain(Vec3 p,UUID v,float bonus){point=p;victim=v;this.bonus=bonus;}}
    private static final class Lock {UUID victim;Vec3 point;int life;Lock(LivingEntity p,int life){victim=p.getUUID();point=p.position();this.life=life;}}
    public VoidBossEntity(EntityType<? extends Monster> t,Level l){super(t,l);setPersistenceRequired();xpReward=3000;}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ACTION,0);entityData.define(AGE,0);entityData.define(PHASE,1);}
    @Override protected void registerGoals(){}
    public int action(){return entityData.get(ACTION);}public int actionAge(){return entityData.get(AGE);}public int phase(){return entityData.get(PHASE);}
    public boolean participated(UUID id){return participants.contains(id);}
    public Set<UUID> participants(){return Set.copyOf(participants);}
    public void record(ServerPlayer p){participants.add(p.getUUID());}
    @Override public boolean removeWhenFarAway(double d){return false;}
    @Override public boolean causeFallDamage(float d,float m,DamageSource s){return false;}
    @Override public boolean doHurtTarget(Entity e){return false;}
    private ServerLevel server(){return (ServerLevel)level();}
    public List<ServerPlayer> players(){return server().getEntitiesOfClass(ServerPlayer.class,new AABB(anchor(),anchor()).inflate(96),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&p.position().distanceToSqr(anchor())<96*96);}
    public List<LivingEntity> targets(){return BossTargets.around(this,anchor(),96,false);}
    private Vec3 anchor(){return home==null?position():home;}
    public void beam(Vec3 a,Vec3 b,float width,int life,int kind){Network.send(server(),new Network.Effect(kind,getId(),a,b,width,life,0));}
    private void fx(int kind,Vec3 p,Vec3 dir,float size,int life,long seed){Network.send(server(),new Network.Effect(kind,getId(),p,dir,size,life,seed));}
    private void announce(String text){for(ServerPlayer p:players())p.displayClientMessage(Component.literal(text),false);}
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);bar.addPlayer(p);if(!level().isClientSide)for(Hazard h:rifts)sync(h);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);}
    public boolean hit(LivingEntity target,float damage){return hit(target,damage,castBonus);}
    public float castBonus(){return castBonus;}
    public boolean hit(LivingEntity target,float damage,float bonus){
        if(!BossTargets.hostile(this,target)||target instanceof VoidNodeEntity node&&node.ownedBy(getUUID())||target.getPersistentData().getLong("VoidCaptured")>level().getGameTime())return false;
        float scaled=BalanceRules.damage((damage+bonus)*BossControl.value(this,"BossDamage",1));ServerPlayer participant=MaidBridge.progression(target);if(participant!=null)record(participant);
        target.invulnerableTime=0;return target.hurt(damageSources().indirectMagic(this,this),scaled);
    }
    public void rift(Vec3 point,double radius,int life){if(rifts.size()>=64)return;Vec3 p=new Vec3(point.x,anchor().y+0.04,point.z);Hazard h=new Hazard(++nextRift,p,Math.min(6,radius)+(castBonus>0?3:0),life*(castBonus>0?2:1),castBonus);rifts.add(h);sync(h);}
    private void sync(Hazard h){fx(100,h.point,Vec3.ZERO,(float)h.radius,16,h.id);}
    private void remove(Hazard h){fx(101,h.point,Vec3.ZERO,0,1,h.id);}
    public void removeRifts(int count){for(Iterator<Hazard> it=rifts.iterator();it.hasNext()&&count>0;){Hazard h=it.next();if(h.locked>0)continue;remove(h);it.remove();count--;}}
    private void clearRifts(){for(Hazard h:rifts)remove(h);rifts.clear();}
    private Vec3 randomFloor(){double a=random.nextDouble()*Math.PI*2,r=8+random.nextDouble()*30;return anchor().add(Math.cos(a)*r,0,Math.sin(a)*r);}
    private Vec3 clamp(Vec3 p){Vec3 delta=p.subtract(anchor()).multiply(1,0,1);if(delta.length()>42)delta=delta.normalize().scale(42);return anchor().add(delta);}
    private LivingEntity selected(){return selected!=null&&server().getEntity(selected) instanceof LivingEntity p&&BossTargets.valid(this,p)?p:null;}
    private void begin(int skill,LivingEntity target){entityData.set(ACTION,skill);entityData.set(AGE,0);selected=target.getUUID();mark=target.position();direction=mark.subtract(position()).multiply(1,0,1).normalize();novaHits.clear();pierced.clear();getNavigation().stop();
        castBonus=BossRevelation.roll(this,VoidRules.NAMES[skill]+"："+VoidRules.TIPS[skill]);level().playSound(null,blockPosition(),SoundEvents.WARDEN_HEARTBEAT,SoundSource.HOSTILE,3,0.5f);fx(102,mark,Vec3.ZERO,skill==11?6:3,30,skill);
    }
    private int nextSkill(){if(deck.isEmpty())for(int s:VoidRules.skills(phase()))deck.add(s);return deck.remove(random.nextInt(deck.size()));}
    private void finish(int vulnerable){entityData.set(ACTION,0);entityData.set(AGE,0);weakness=Math.max(weakness,vulnerable);rest=vulnerable>0?vulnerable:descent>0?10:20;setNoGravity(false);setDeltaMovement(Vec3.ZERO);if(getY()>anchor().y+1)setPos(getX(),anchor().y,getZ());}
    @Override public void tick(){super.tick();if(level().isClientSide||!isAlive()||SixfoldFinale.controlled(this))return;if(home==null)home=position();
        if(weakness>0)weakness--;if(descent>0){descent--;if(descent==0){weakness=100;rest=Math.max(rest,100);}else if(descent%10==0)rift(randomFloor(),1.1,100);}
        int phase=VoidRules.phase(getHealth(),getMaxHealth());entityData.set(PHASE,phase);if(phase!=phaseSeen){phaseSeen=phase;deck.clear();announce("构筑咒胎受肉：虚无相进入第 "+phase+" 阶段");}
        bar.setProgress(Math.max(0,Math.min(1,getHealth()/getMaxHealth())));bar.setName(Component.literal("构筑咒胎受肉：虚无相 · "+(int)getHealth()+" / "+(int)getMaxHealth()+" · 第"+phase+"阶段"+(weakness>0?" · 核心虚弱":"")));
        LivingEntity primary=BossTargets.select(this,128);List<LivingEntity> nearby=targets();if(primary==null&&nearby.isEmpty()){if(++absence>=200){cleanup();finish(0);absence=200;}return;}absence=0;
         tickEye();tickLocks();tickCage();tickChains();tickRifts();
        if(VoidRules.rebirth(getHealth(),getMaxHealth(),reborn)){reborn=true;clearRifts();setHealth(Math.min(getMaxHealth(),getHealth()+getMaxHealth()*0.10f));descent=0;finish(200);announce("虚空轮回 · 恢复10%生命，全部裂隙消失！核心虚弱10秒，每场仅触发一次");fx(109,position().add(0,2,0),Vec3.ZERO,6,40,16);return;}
        LivingEntity target=primary!=null?primary:selected();if(target==null||distanceToSqr(target)>128*128)target=nearby.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);if(target==null)return;setTarget(target);getLookControl().setLookAt(target,30,30);Vec3 facing=target.position().subtract(position());setYRot((float)Math.toDegrees(Math.atan2(-facing.x,facing.z)));yBodyRot=getYRot();
        if(action()==0){rest-=Math.max(1,(int)Math.ceil(BossControl.value(this,"Frequency",3)));if(rest>0)return;begin(nextSkill(),target);return;}
        int t=actionAge()+1;entityData.set(AGE,t);setDeltaMovement(Vec3.ZERO);
        if(t==30)activate(action(),target);
        if(t<30)return;
        int age=t-30;switch(action()){
            case 3->{setNoGravity(true);setPos(getX(),anchor().y+3,getZ());if(age%10==0){Vec3 p=randomFloor();fx(103,p.add(0,14,0),p,1.4f,20,0);rift(p,0.8,160);}}
            case 4,15->{if(age%5==0){fx(109,position().add(0,2,0),Vec3.ZERO,action()==15?9:5,12,action());for(LivingEntity p:nearby)if(distanceToSqr(p)<16)hit(p,action()==15?40:24);}}
            case 6->{if(age%10==0)fx(110,position().add(0,2.8,0),getLookAngle(),2,15,0);}
            case 9->{if(age==40){LivingEntity p=selected();if(p!=null&&p.position().distanceToSqr(mark)<3*3){spawnNode(1,p,mark,100,0);p.sendSystemMessage(Component.literal("你被虚空吞噬！队友在5秒内击碎灵魂锚可救出你。"));}}}
            case 10->{if(age%10==0)fx(109,position().add(0,2,0),Vec3.ZERO,14,15,10);}
            case 11->{if(age%20==0)rift(cageCenter.add(random.nextDouble()*6-3,0,random.nextDouble()*6-3),0.8,80);}
            case 13->{if(age%10==0)level().playSound(null,blockPosition(),SoundEvents.WARDEN_HEARTBEAT,SoundSource.HOSTILE,2,0.4f);}
            case 14->{for(LivingEntity p:nearby){Vec3 d=position().subtract(p.position()).normalize();p.push(d.x*0.16,d.y*0.08,d.z*0.16);p.hurtMarked=true;if(age%10==0&&distanceToSqr(p)<9)hit(p,65);}if(age%10==0)fx(109,position().add(0,2,0),Vec3.ZERO,12,15,14);}
            case 17->{Vec3 end=position().add(direction.scale(42));beam(position().add(0,0.7,0),end.add(0,0.7,0),3,8,104);if(age%10==0){for(LivingEntity e:server().getEntitiesOfClass(LivingEntity.class,new AABB(position(),end).inflate(4)))if(e.getBoundingBox().inflate(2).clip(position().add(0,1,0),end.add(0,1,0)).isPresent())hit(e,70);for(int i=2;i<42;i+=4)consumeBlocks(position().add(direction.scale(i)),1);}}
            case 18->{if(age==20){LivingEntity p=selected();if(p!=null&&p.position().distanceToSqr(mark)<9){spawnNode(2,p,mark,60,0);p.sendSystemMessage(Component.literal("被虚空之握抓住！队友3秒内攻击手掌节点可打断。"));}}}
            case 19->{double radius=age*1.2;fx(106,position().add(0,0.15,0),Vec3.ZERO,(float)radius,3,0);for(LivingEntity p:nearby){double d=p.position().subtract(position()).horizontalDistance();if(Math.abs(d-radius)<1.5&&p.getY()<anchor().y+1.1&&novaHits.add(p.getUUID()))hit(p,35);}}
            case 20->{
                // 通常一次短招为30刻预警+40刻动作+20刻收招；降临每45刻一次。
                if(age%45==0){assault=(age/45)%3;assaultMark=target.position();assaultDirection=assaultMark.subtract(position()).multiply(1,0,1).normalize();fx(102,assaultMark,Vec3.ZERO,3,15,0);}
                if(age%45==15){if(assault==0){for(int i=3;i<36;i+=3)rift(position().add(assaultDirection.scale(i)),1,100);}else if(assault==1)launchChains(target,3);else{beam(assaultMark.add(0,18,0),assaultMark,1.4f,20,104);for(LivingEntity p:nearby)if(p.position().distanceToSqr(assaultMark)<9)hit(p,55);rift(assaultMark,2,100);}}
            }
            case 21->{if(age==12){for(LivingEntity e:server().getEntitiesOfClass(LivingEntity.class,new AABB(mark,mark).inflate(5)))if(e.position().distanceToSqr(mark)<25){if(hit(e,80)&&pierced.contains(e.getUUID()))forceNextPhase(e);}fx(109,mark.add(0,2,0),Vec3.ZERO,5,25,21);}}
        }
        if(age>=VoidRules.duration(action())){int old=action();finish(old==10?60:old==15?100:old==20?100:0);}
    }
    private void activate(int skill,LivingEntity target){switch(skill){
        case 1->{for(int i=3;i<=30;i+=3)rift(position().add(direction.scale(i)),0.8,100);beam(position(),position().add(direction.scale(30)),0.5f,25,104);}
        case 2->{Vec3 back=clamp(target.position().subtract(target.getLookAngle().multiply(1,0,1).normalize().scale(4)));fx(109,position().add(0,2,0),Vec3.ZERO,3,20,2);if(level().noCollision(this,getBoundingBox().move(back.subtract(position()))))setPos(back.x,back.y,back.z);rift(position(),2.5,60);for(LivingEntity p:targets())if(distanceToSqr(p)<25)hit(p,24);fx(109,position().add(0,2,0),Vec3.ZERO,3,20,2);}
        case 3->{}
        case 4,15->{if(rifts.isEmpty())for(int i=0;i<8;i++)rift(randomFloor(),2,160);}
        case 5->launchChains(target,4);
        case 6->{eye=200+(castBonus>0?400:0);}
        case 7->{for(Hazard h:rifts){h.radius=Math.min(10,h.radius*2);h.life=Math.max(h.life,200);h.locked=200;sync(h);}if(rifts.isEmpty())for(int i=0;i<6;i++){rift(randomFloor(),4,200);rifts.get(rifts.size()-1).locked=200;}}
        case 8->{for(int i=0;i<3;i++){double a=i*Math.PI*2/3;Vec3 p=position().add(Math.cos(a)*5,0,Math.sin(a)*5);spawnNode(0,target,p,240,i);rift(p,1.4,160);}}
        case 9->{mark=new Vec3(mark.x,anchor().y,mark.z);fx(107,mark,Vec3.ZERO,3,40,9);rift(mark,1.3,120);}
        case 10->{if(rifts.size()<8)for(int i=0;i<8;i++)rift(randomFloor(),2,180);}
        case 11->{cage=100+(castBonus>0?200:0);cageCenter=mark;fx(105,cageCenter,Vec3.ZERO,6,100,0);}
        case 12->{for(int i=0;i<4;i++)spawnNode(3,target,position().add(0,1.5,0),100,i);}
        case 13->{if(rifts.isEmpty())for(int i=0;i<6;i++)rift(randomFloor(),1.6,120);}
        case 14->{}
        case 17->beam(position(),position().add(direction.scale(42)),3,100,104);
        case 18->fx(111,mark,Vec3.ZERO,3,80,0);
        case 19->{}
        case 20->{descent=300;fx(109,position().add(0,2.5,0),Vec3.ZERO,5,300,20);}
        case 21->{beam(mark.add(0,18,0),mark.add(0,-1,0),1.4f,24,104);for(LivingEntity e:server().getEntitiesOfClass(LivingEntity.class,new AABB(mark,mark.add(0,20,0)).inflate(1.5))){if(hit(e,55))pierced.add(e.getUUID());}rift(mark,3,160);}
    }}
    private void tickRifts(){boolean collapse=(action()==4||action()==15)&&actionAge()>=30,storm=action()==10&&actionAge()>=30,resonance=action()==13&&actionAge()>=30;
        for(Iterator<Hazard> it=rifts.iterator();it.hasNext();){Hazard h=it.next();h.age++;if(collapse||resonance)h.life=Math.max(h.life,20);if(h.locked>0)h.locked--;if(--h.life<=0&&h.locked<=0){remove(h);it.remove();continue;}h.radius=Math.min(Math.max(h.radius,6),h.radius+0.006);
            if(collapse&&h.locked<=0){Vec3 d=position().subtract(h.point).multiply(1,0,1);h.point=h.point.add(d.normalize().scale(action()==15?0.65:0.42));if(d.lengthSqr()<2){remove(h);it.remove();continue;}if(tickCount%10==0)consumeBlocks(h.point,2);}
            if(storm){Vec3 d=h.point.subtract(position());double a=0.028;h.point=position().add(d.x*Math.cos(a)-d.z*Math.sin(a),anchor().y-position().y+0.04,d.x*Math.sin(a)+d.z*Math.cos(a));h.life=Math.max(h.life,20);}
            if(tickCount%2==0){if(resonance)fx(100,h.point,Vec3.ZERO,(float)(h.radius+2),16,h.id);else sync(h);}
            if(h.age<20||tickCount%10!=0)continue;
            double radius=h.radius+(resonance?2:0);for(LivingEntity e:server().getEntitiesOfClass(LivingEntity.class,new AABB(h.point,h.point).inflate(radius,2.5,radius))){if(e.position().subtract(h.point).horizontalDistanceSqr()>radius*radius||Math.abs(e.getY()-h.point.y)>2.4)continue;hit(e,collapse?24:storm?18:resonance?14:8,Math.max(h.bonus,collapse||storm||resonance?castBonus:0));if(BossTargets.hostile(this,e))e.getPersistentData().putLong("VoidNoHeal",level().getGameTime()+20);}
        }
    }
    private void tickEye(){if(eye<=0)return;eye--;for(LivingEntity p:targets()){p.getPersistentData().putLong("VoidEyeUntil",level().getGameTime()+3);var speed=p.getAttribute(Attributes.MOVEMENT_SPEED);if(speed!=null&&speed.getModifier(EYE_SLOW)==null)speed.addTransientModifier(new AttributeModifier(EYE_SLOW,"虚空之眼减速",-0.20,AttributeModifier.Operation.MULTIPLY_TOTAL));Vec3 toward=getEyePosition().subtract(p.getEyePosition()).normalize();if(p.getLookAngle().dot(toward)>0.94&&hasLineOfSight(p))p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,30,0,false,false));}}
    private void launchChains(LivingEntity target,int count){count=RevelationRules.quantity(count,castBonus>0,24);for(int i=0;i<count&&chains.size()<24;i++){Vec3 p=position().add((i-count/2.0)*0.7,2,0);chains.add(new Chain(p,target.getUUID(),castBonus));}}
    private void tickChains(){for(Iterator<Chain> it=chains.iterator();it.hasNext();){Chain c=it.next();if(++c.age>50||!(server().getEntity(c.victim) instanceof LivingEntity p)||!BossTargets.valid(this,p)){it.remove();continue;}Vec3 next=c.point.add(p.getBoundingBox().getCenter().subtract(c.point).normalize().scale(0.65));beam(c.point,next,0.12f,8,108);if(p.getBoundingBox().inflate(0.4).clip(c.point,next).isPresent()){hit(p,12,c.bonus);locks.add(new Lock(p,25));rift(p.position(),1.3,100);it.remove();}else c.point=next;}}
    private void tickLocks(){for(Iterator<Lock> it=locks.iterator();it.hasNext();){Lock k=it.next();if(--k.life<=0||!(server().getEntity(k.victim) instanceof LivingEntity p)||!BossTargets.valid(this,p)){it.remove();continue;}BossTargets.move(p,k.point);}}
    private void tickCage(){if(cage<=0)return;cage--;for(LivingEntity p:targets()){Vec3 d=p.position().subtract(cageCenter);double r=d.horizontalDistance();if(selected!=null&&p.getUUID().equals(selected)&&(r>5.4||p.getY()>cageCenter.y+4)){Vec3 q=cageCenter.add(d.multiply(1,0,1).normalize().scale(Math.min(r,5.2)));BossTargets.move(p,new Vec3(q.x,cageCenter.y,q.z));}}}
    private void spawnNode(int kind,LivingEntity p,Vec3 pos,int life,int slot){VoidNodeEntity n=VoidContent.NODE.get().create(level());if(n!=null){n.setup(this,kind,p,life,slot);n.moveTo(pos.x,pos.y,pos.z,0,0);n.setCustomName(Component.literal(kind==0?"虚空分身":kind==1?"灵魂锚 · 击碎救人":kind==2?"虚空之握 · 攻击打断":"裂隙触手 · 斩击可断"));n.setCustomNameVisible(true);level().addFreshEntity(n);}}
    private void consumeBlocks(Vec3 p,int radius){if(!SkillDestruction.enabled(server())||!level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))return;BlockPos c=BlockPos.containing(p);int budget=12;for(BlockPos q:BlockPos.betweenClosed(c.offset(-radius,0,-radius),c.offset(radius,2,radius))){if(budget<=0)break;if(SoulJourney.protectedBlock(level(),q))continue;if(level().dimension()==VoidJourney.ARENA&&q.getY()<=-60)continue;var state=level().getBlockState(q);if(!state.isAir()&&state.getDestroySpeed(level(),q)>=0&&!state.hasBlockEntity()){level().destroyBlock(q,false,this);budget--;}}}
    private void forceNextPhase(LivingEntity e){if(e==this||!e.isAlive()||e instanceof VoidNodeEntity)return;if(e instanceof VoidBossEntity b&&b.phase()<3)b.setHealth(Math.min(b.getHealth(),b.getMaxHealth()*(b.phase()==1?0.60f:0.30f)));else if(e instanceof BuddhaEntity b&&b.phase()==1)b.setHealth(Math.min(b.getHealth(),b.getMaxHealth()*0.5f));else if(e instanceof YujiEntity||e instanceof net.minecraft.world.entity.boss.wither.WitherBoss)e.setHealth(Math.max(1,Math.min(e.getHealth(),e.getMaxHealth()*0.49f)));}
    @Override public boolean hurt(DamageSource s,float n){if(!s.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)&&action()==10&&actionAge()>=30)return false;ServerPlayer caster=CombatSettings.caster(s);if(caster!=null)record(caster);return super.hurt(s,n*(weakness>0?1.6f:1));}
    private void cleanup(){clearRifts();chains.clear();locks.clear();cage=eye=0;for(VoidNodeEntity n:server().getEntitiesOfClass(VoidNodeEntity.class,new AABB(anchor(),anchor()).inflate(128)))if(n.ownedBy(getUUID()))n.discard();}
    @Override public void die(DamageSource s){
        super.die(s);
        // dead 仅在死亡事件没有被取消时设置；不能只看生命值是否为零。
        if(!level().isClientSide&&dead){
            ServerPlayer caster=CombatSettings.caster(s);if(caster!=null)record(caster);
            if(!rewarded){rewarded=true;VoidJourney.reward(this,s);}cleanup();
        }
        if(dead)bar.removeAllPlayers();
    }
    @Override public void remove(RemovalReason r){if(!level().isClientSide)cleanup();bar.removeAllPlayers();super.remove(r);}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);t.putBoolean("Reborn",reborn);t.putBoolean("Rewarded",rewarded);if(home!=null){t.putDouble("HomeX",home.x);t.putDouble("HomeY",home.y);t.putDouble("HomeZ",home.z);}ListTag ids=new ListTag();for(UUID id:participants)ids.add(StringTag.valueOf(id.toString()));t.put("Participants",ids);}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);reborn=t.getBoolean("Reborn");rewarded=t.getBoolean("Rewarded");if(t.contains("HomeY"))home=new Vec3(t.getDouble("HomeX"),t.getDouble("HomeY"),t.getDouble("HomeZ"));for(Tag id:t.getList("Participants",8))try{participants.add(UUID.fromString(id.getAsString()));}catch(IllegalArgumentException ignored){}finish(0);rest=60;}
}
