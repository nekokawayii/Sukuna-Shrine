package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraft.tags.DamageTypeTags;

/** 净土战斗与和解共用同一个玩家外观实体；不创建玩家连接或强制加载区块。 */
public final class GuardianEntity extends Monster {
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(GuardianEntity.class,EntityDataSerializers.INT),AGE=SynchedEntityData.defineId(GuardianEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FRIENDLY=SynchedEntityData.defineId(GuardianEntity.class,EntityDataSerializers.BOOLEAN);
    private final ServerBossEvent bar=new ServerBossEvent(Component.literal("虎杖悠仁·净土守护者"),BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.NOTCHED_20);
    private CompoundTag settings=new CompoundTag();
    private UUID owner,speaker;
    private boolean configured,healthReady,internalHealth,completingDeath,rewardIssued,victorySoundPlayed;
    private int damageDepth;
    private float damageFloor;
    private DamageSource activeDamage,defeatSource;
    private int boost,domain,sequence,rest,guard,stun,dialogueTick,dialogueLine;
    private long nextGuard,nextBeam,nextDomain;
    private Vec3 center=Vec3.ZERO,dash=Vec3.ZERO,peaceHome=Vec3.ZERO;
    private final Set<UUID> participants=new HashSet<>();
    private final List<Impact> impacts=new ArrayList<>();
    private final List<Blood> blood=new ArrayList<>();
    private final GuardianGridSlash grid=new GuardianGridSlash();
    private final ArrayDeque<Vec3> aimHistory=new ArrayDeque<>();
    private UUID aimTarget;
    private float castBonus,gridBonus,domainBonus;
    private record Impact(UUID target,long due,float bonus){}
    private static final class Blood {Vec3 pos,velocity;UUID target;int life;float bonus;Blood(Vec3 p,Vec3 v,UUID id,float bonus){pos=p;velocity=v;target=id;this.bonus=bonus;}}
    private final Set<UUID> courtyardVisitors=new HashSet<>();
    private final Map<UUID,Long> nextGreeting=new HashMap<>();
    private ListTag gifts=new ListTag();
    private static final String[] DIALOGUE={
        "虎杖：是你赢了啊，我能看出来，你和“他”不一样，但却有着一样的术式",
        "虎杖：......168年了，你是第一个",
        "虎杖：我的挚友们也都去世了......",
        "虎杖：东堂、胀相、五条老师......还有大家",
        "虎杖：钉崎那家伙，到最后也没等到我请她吃那顿饭",
        "虎杖：伏黑他......算是过了还不错的一生",
        "虎杖：有时候我会想，如果他们还活着，会怎么看我",
        "虎杖：......不打不相识，我相信我们可以成为朋友，我能看出来，你和他不一样",
        "玩家：168年了，你为什么会在灵魂通道？",
        "虎杖：我没死，不过是想家了，地球上的事.....有后辈们了",
        "虎杖：那些孩子，比我们那时候强多了",
        "虎杖：我相信他们........也相信",
        "虎杖：你。",
        "虎杖：以后有空，来岩手坐坐",
        "虎杖：我就在这儿，不走",
        "虎杖：这里挺好的，有田，有风，有小时候的味道",
        "虎杖：......对了",
        "虎杖：谢谢你，陪我打完这一场",
        "虎杖：很久没这么痛快了"
    };
    public GuardianEntity(EntityType<? extends Monster> type,Level level){super(type,level);GuardianSettings.defaults(settings);setPersistenceRequired();xpReward=3000;healthReady=true;}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ACTION,0);entityData.define(AGE,0);entityData.define(FRIENDLY,false);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(5,new RandomStrollGoal(this,0.65,100){@Override public boolean canUse(){return friendly()&&dialogueFinished()&&super.canUse();}@Override public boolean canContinueToUse(){return friendly()&&dialogueFinished()&&super.canContinueToUse();}});
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8){@Override public boolean canUse(){return friendly()&&dialogueFinished()&&super.canUse();}});
        goalSelector.addGoal(7,new RandomLookAroundGoal(this){@Override public boolean canUse(){return friendly()&&dialogueFinished()&&super.canUse();}});
    }
    public int action(){return entityData.get(ACTION);}public int actionAge(){return entityData.get(AGE);}public boolean friendly(){return entityData.get(FRIENDLY);}
    private boolean dialogueFinished(){return GuardianTimeline.finished(dialogueLine,dialogueTick);}
    public boolean peacefulResident(){return friendly()&&dialogueFinished();}
    private double v(String k){return Math.max(0,BalanceRules.guardianAttribute(k,GuardianSettings.value(settings,k)));}
    private int ticks(String k){return (int)Math.min(1200000,v(k)*20);}
    private int count(String k,int safety){return (int)Math.min(safety,v(k));}
    private float amount(String k){return (float)Math.min(Float.MAX_VALUE,v(k));}
    public void configure(ServerPlayer p){owner=p.getUUID();settings=BossControl.guardian(p.server);applyAttributes(false);configured=true;}
    public void refreshSettings(CompoundTag tag,String changed){if(friendly())return;settings=tag.copy();applyAttributes(!"Health".equals(changed));if("GuardCooldown".equals(changed))nextGuard=level().getGameTime()+ticks("GuardCooldown");configured=true;}
    private void applyAttributes(boolean preserve){float ratio=getHealth()/getMaxHealth();getAttribute(Attributes.MAX_HEALTH).removeModifiers();getAttribute(Attributes.ARMOR).removeModifiers();getAttribute(Attributes.MOVEMENT_SPEED).removeModifiers();getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(1,v("Health")));getAttribute(Attributes.ARMOR).setBaseValue(v("Armor"));getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BalanceRules.strength(8));getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(v("MoveSpeed"));rawHealth(getMaxHealth()*(preserve?ratio:1));}
    public void enforceSettings(){if(friendly())return;float current=getHealth(),oldMax=getMaxHealth();boolean changed=false;for(var pair:java.util.Map.of(Attributes.MAX_HEALTH,Math.max(1,v("Health")),Attributes.ARMOR,v("Armor"),Attributes.ATTACK_DAMAGE,BalanceRules.strength(8),Attributes.MOVEMENT_SPEED,v("MoveSpeed")).entrySet()){var a=getAttribute(pair.getKey());if(a!=null&&(a.getBaseValue()!=pair.getValue()||!a.getModifiers().isEmpty())){a.removeModifiers();a.setBaseValue(pair.getValue());changed=true;}}if(changed)rawHealth((float)BalanceRules.healthAfterResize(current,oldMax,getMaxHealth()));}
    private void rawHealth(float value){boolean previous=internalHealth;internalHealth=true;try{super.setHealth(value);}finally{internalHealth=previous;}}
    @Override public void setHealth(float value){
        if(!healthReady||internalHealth||level().isClientSide||completingDeath){super.setHealth(value);return;}
        if(friendly()){super.setHealth(GuardianRules.DEFEAT_HEALTH);return;}
        float before=getHealth();
        float safe=GuardianRules.healthAfter(before,value,cap(),damageDepth>0?damageFloor:GuardianRules.DEFEAT_HEALTH);
        super.setHealth(safe);
        // 最终写血口兜底，其他模组在伤害事件中加伤或直接 setHealth 也不能跳过。
        if(value<before&&safe<=GuardianRules.DEFEAT_HEALTH)beginDefeat(activeDamage);
    }
    public void refresh(ServerPlayer p){refreshSettings(BossControl.guardian(p.server),"");}
    private ServerLevel server(){return (ServerLevel)level();}
    private boolean valid(LivingEntity p){return !friendly()&&BossTargets.valid(this,p);}
    private List<ServerPlayer> players(double radius){return server().getEntitiesOfClass(ServerPlayer.class,getBoundingBox().inflate(radius),p->valid(p)&&distanceToSqr(p)<=radius*radius);}
    public boolean contains(LivingEntity p){return !DomainClash.active(this)&&domain>0&&valid(p)&&p.position().distanceToSqr(center)<=v("DomainRadius")*v("DomainRadius");}
    public static boolean locked(LivingEntity p){return p.level() instanceof ServerLevel l&&!l.getEntitiesOfClass(GuardianEntity.class,p.getBoundingBox().inflate(512),g->g.isAlive()&&g.contains(p)).isEmpty();}
    private void fx(int kind,Vec3 p,Vec3 q,float size,int life,long seed){Network.send(server(),new Network.Effect(kind,getId(),p,q,size,life,seed));}
    private boolean reach(LivingEntity p){return contains(p)||distanceToSqr(p)<=v("Reach")*v("Reach")&&hasLineOfSight(p);}
    private boolean hit(LivingEntity p,double base,boolean punch){return hit(p,base,punch,castBonus);}
    private boolean hit(LivingEntity p,double base,boolean punch,float bonus){
        if(!valid(p)||base<=0)return false;
        double damage=base+(boost>0?v("DamageMultiplier"):0);
        boolean flash=punch&&action()!=9&&random.nextDouble()*100<v("FlashChance");
        if(flash)damage=Math.pow(damage,v("FlashExponent"));
        damage+=bonus;
        p.invulnerableTime=0;var type=server().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,new net.minecraft.resources.ResourceLocation("sukuna","guardian_soul")));boolean hit=p.hurt(new DamageSource(type,this),BalanceRules.damage(Math.min(Float.MAX_VALUE,damage)));
        if(hit){if(p instanceof ServerPlayer player)participants.add(player.getUUID());fx(81,p.position(),Vec3.ZERO,1,10,p.getId());if(flash){fx(85,p.getEyePosition(),Vec3.ZERO,1,12,p.getId());SkillSounds.play(this,"black_flash");}ServerPlayer caster=MaidBridge.progression(p);if(punch&&caster!=null)SoulCombat.peel(caster,false);}return hit;
    }
    private int interval(String k){return Math.max(1,ticks(k)-(boost>0?(int)v("AttackSpeedMultiplier"):0));}
    private void moveToward(LivingEntity p){Vec3 d=p.position().subtract(position()).multiply(1,0,1).normalize();double speed=v("MoveSpeed")+(boost>0?v("SpeedMultiplier"):0);setDeltaMovement(d.x*speed,getDeltaMovement().y,d.z*speed);}
    private void begin(int skill,LivingEntity p){
        entityData.set(ACTION,skill);entityData.set(AGE,0);getNavigation().stop();castBonus=BossRevelation.roll(this,BossRevelation.guardian(skill));SkillSounds.play(this,"guardian_"+skill);
        if(skill==1&&reach(p))hit(p,v("Punch"),true);
        if(skill==4){List<LivingEntity> targets=BossTargets.nearby(this,128);if(targets.isEmpty())targets=List.of(p);for(int i=0;i<RevelationRules.quantity(count("BloodCount",512),castBonus>0,512);i++){double a=i*Math.PI*2/Math.max(1,RevelationRules.quantity(count("BloodCount",512),castBonus>0,512));blood.add(new Blood(getEyePosition(),new Vec3(Math.cos(a),((i%5)-2)*0.12,Math.sin(a)).scale(v("BloodSpeed")),targets.get(i%targets.size()).getUUID(),castBonus));}}
        if(skill==5){boost=ticks("BoostSeconds")+(castBonus>0?400:0);fx(136,position(),Vec3.ZERO,1,24,0);}
        if(skill==6){gridBonus=castBonus;float radius=(float)Math.min(512,v("GridRadius")*(castBonus>0?1.5:1));grid.start(position(),radius,castBonus>0?GuardianGridSlash.LIFETIME*2:GuardianGridSlash.LIFETIME);fx(126,position(),Vec3.ZERO,radius,castBonus>0?GuardianGridSlash.LIFETIME*2:GuardianGridSlash.LIFETIME,0);tickGrid();}
        if(skill==8){domainBonus=castBonus;center=position();domain=ticks("DomainSeconds")*(castBonus>0?2:1);nextDomain=level().getGameTime()+GuardianRules.DOMAIN_COOLDOWN_TICKS;domainVisual();DomainClash.opened(this,center,Math.min(512,v("DomainRadius")));}
        if(skill==9){Vec3 side=p.getLookAngle().cross(new Vec3(0,1,0)).normalize();Vec3 q=p.position().add(side.scale(v("TeleportDistance")));if(level().noCollision(this,getBoundingBox().move(q.subtract(position()))))setPos(q.x,q.y,q.z);dash=p.position().subtract(position()).multiply(1,0,1).normalize();fx(84,position().add(0,1,0),p.position(),1,Math.max(1,ticks("DashWindup")),0);}
        if(skill==11)nextGuard=level().getGameTime()+ticks("GuardCooldown");
        if(skill==12){nextBeam=level().getGameTime()+ticks("BeamCooldown");aimHistory.clear();aimTarget=p.getUUID();}
    }
    private void finish(){entityData.set(ACTION,0);entityData.set(AGE,0);rest=ticks("SkillGap");setDeltaMovement(0,getDeltaMovement().y,0);}
    private void face(Vec3 point){Vec3 d=point.subtract(getEyePosition());getLookControl().setLookAt(point.x,point.y,point.z,180,180);setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));setXRot((float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));yBodyRot=getYRot();yHeadRot=getYRot();}
    @Override public void tick(){
        super.tick();if(level().isClientSide||!isAlive()||SixfoldFinale.controlled(this))return;
        if(friendly()){tickFriend();return;}
        if(owner==null&&getTarget() instanceof ServerPlayer player)owner=player.getUUID();
        if(!configured){ServerPlayer p=players(128).stream().findFirst().orElse(null);if(p!=null)configure(p);}
        if(domain>0)DomainClash.opened(this,center,Math.min(512,v("DomainRadius")));
        if(DomainClash.active(this)||DomainClash.stunned(this)){DomainClash.hold(this);bar.setProgress(Math.max(0,Math.min(1,getHealth()/getMaxHealth())));bar.setName(Component.literal("虎杖悠仁·净土守护者 · "+(long)getHealth()+" / "+(long)getMaxHealth()+(DomainClash.stunned(this)?" · 对拼败北僵直":" · 领域对拼")));return;}
        if(boost>0){boost--;if(tickCount%8==0)fx(136,position(),Vec3.ZERO,1,12,0);}
        if(guard>0){guard--;if(tickCount%6==0)fx(127,position().add(0,1,0),Vec3.ZERO,1,8,0);}
        tickBlood();tickImpacts();tickDomain();tickGrid();
        bar.setProgress(Math.max(0,Math.min(1,getHealth()/getMaxHealth())));bar.setName(Component.literal("虎杖悠仁·净土守护者 · "+(long)getHealth()+" / "+(long)getMaxHealth()+(stun>0?" · 僵直":"")));
        if(stun>0){stun--;getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);return;}
        LivingEntity p=BossTargets.select(this,256);
        if(p==null){if(action()!=0)finish();return;}
        if(action()!=12)face(p.getEyePosition());
        if(action()==0){if(rest>0){rest--;return;}int[] cycle={1,3,11,4,9,5,6,12,1,8,3,9,4,10};int next=cycle[sequence++%cycle.length];if(next==8&&!GuardianRules.domainReady(domain,level().getGameTime(),nextDomain))next=6;if(next==5&&boost>0)next=1;if(next==11&&(guard>0||level().getGameTime()<nextGuard))next=1;if(next==12&&level().getGameTime()<nextBeam)next=4;begin(next,p);}
        if(DomainClash.active(this)){DomainClash.hold(this);return;}
        int t=actionAge()+1;entityData.set(AGE,t);
        switch(action()){
            case 1 -> {moveToward(p);if(t>1&&t%interval("PunchInterval")==0&&reach(p))hit(p,v("Punch"),true);if(t>=interval("PunchInterval")*3)finish();}
            case 3 -> {moveToward(p);int delay=Math.max(1,ticks("SoulDelay"));if(t==1||t==delay+1){if(reach(p))hit(p,v("Punch"),true);if(contains(p)||distanceToSqr(p)<24*24)impacts.add(new Impact(p.getUUID(),level().getGameTime()+delay,castBonus));fx(140,getEyePosition(),p.getEyePosition(),1,8,0);}if(t>delay+1)finish();}
            case 4,5,6 -> {if(t>=6)finish();}
            case 8 -> {setDeltaMovement(0,getDeltaMovement().y,0);if(t>=20)finish();}
            case 9 -> {int wind=ticks("DashWindup"),gap=interval("ComboInterval"),n=RevelationRules.quantity(count("ComboCount",1000),castBonus>0,1000);if(t==wind+1){Vec3 delta=dash.scale(v("DashDistance"));for(int i=0;i<10;i++){Vec3 step=delta.scale(0.1);if(!level().noCollision(this,getBoundingBox().move(step)))break;setPos(getX()+step.x,getY(),getZ()+step.z);}}if(t>wind&&(t-wind-1)%gap==0&&(t-wind-1)/gap<n){fx(142,getEyePosition(),p.getEyePosition(),1,5,(t-wind-1)/gap);if(reach(p)&&hit(p,v("Punch"),true))p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,ticks("ComboSlowSeconds"),Math.max(0,count("ComboSlow",255)-1)));}if(t>=wind+Math.max(1,n*gap))finish();}
            case 10 -> {setDeltaMovement(0,getDeltaMovement().y,0);int wind=ticks("HealWindup"),duration=ticks("HealSeconds")*(castBonus>0?2:1);if(t<=wind+duration&&t%5==0)fx(137,position(),Vec3.ZERO,1,8,0);if(t>wind&&t<=wind+duration)heal(amount("HealPerSecond")/20);if(t>=wind+duration+ticks("HealRecovery"))finish();}
            case 11 -> {setDeltaMovement(0,getDeltaMovement().y,0);if(t>=ticks("GuardWindup")){guard=ticks("GuardSeconds")+(castBonus>0?600:0);fx(127,position().add(0,1,0),Vec3.ZERO,1,10,0);finish();}}
            case 12 -> tickBeam(p,t);
            default -> finish();
        }
    }
    private void tickBeam(LivingEntity target,int age){
        getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);
        if(!target.getUUID().equals(aimTarget)){aimTarget=target.getUUID();aimHistory.clear();}
        aimHistory.addLast(target.getBoundingBox().getCenter());while(aimHistory.size()>7)aimHistory.removeFirst();
        Vec3 aim=aimHistory.getFirst();face(aim);
        Vec3 origin=getEyePosition().add(0,-0.22,0).add(getLookAngle().scale(0.75));
        int wind=ticks("BeamWindup"),duration=ticks("BeamSeconds")*(castBonus>0?2:1);
        if(age<=wind){fx(138,origin,Vec3.ZERO,(float)(1-age/(double)Math.max(1,wind)),3,0);return;}
        Vec3 dir=aim.subtract(origin).normalize(),end=origin.add(dir.scale(Math.min(128,v("BeamRange"))));
        var block=level().clip(new ClipContext(origin,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));if(block.getType()!=HitResult.Type.MISS)end=block.getLocation();
        fx(139,origin,end,0.18f,2,0);
        if((age-wind)%Math.max(1,ticks("BeamInterval"))==0)for(LivingEntity victim:server().getEntitiesOfClass(LivingEntity.class,new AABB(origin,end).inflate(0.5),e->BossTargets.hostile(this,e))){if(victim.getBoundingBox().inflate(0.2).contains(origin)||victim.getBoundingBox().inflate(0.2).clip(origin,end).isPresent())hit(victim,v("BeamDamage"),false);}
        if(age>=wind+duration){aimHistory.clear();finish();}
    }
    private void tickGrid(){double damage=v("GridDamage");grid.tick(this,p->damage>0&&hit(p,damage,false,gridBonus));}
    private void tickImpacts(){for(Iterator<Impact> it=impacts.iterator();it.hasNext();){Impact i=it.next();if(level().getGameTime()<i.due)continue;it.remove();if(server().getEntity(i.target) instanceof LivingEntity p&&valid(p)&&distanceToSqr(p)<256*256&&hit(p,v("Punch"),false,i.bonus)){p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,ticks("SoulSlowSeconds"),2));fx(141,p.getBoundingBox().getCenter(),Vec3.ZERO,1,12,p.getId());}}}
    private void tickBlood(){for(Iterator<Blood> it=blood.iterator();it.hasNext();){Blood b=it.next();if(++b.life>120||!(server().getEntity(b.target) instanceof LivingEntity p)||!valid(p)){it.remove();continue;}Vec3 direction=p.getBoundingBox().getCenter().subtract(b.pos).normalize();b.velocity=b.velocity.scale(0.65).add(direction.scale(v("BloodSpeed")*0.35)).normalize().scale(v("BloodSpeed"));Vec3 next=b.pos.add(b.velocity);fx(125,b.pos,next,1,3,b.life);if(contains(p)||p.getBoundingBox().inflate(0.4).clip(b.pos,next).isPresent()){if(hit(p,v("BloodDamage"),false,b.bonus)){GuardianCombat.poison(p,this,ticks("PoisonSeconds"),amount("PoisonDamage"),v("PoisonCooldown"));p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,ticks("PoisonSeconds"),Math.max(0,count("PoisonSlow",255)-1)));}it.remove();}else b.pos=next;}}
    private void domainVisual(){fx(90,center,Vec3.ZERO,(float)Math.min(512,v("DomainRadius")),domain,1);}
    public boolean clashDomainActive(){return domain>0&&!friendly();}
    public void extendDomain(int ticks){if(domain>0)domain+=ticks;}
    public void clashCloseDomain(){domain=0;fx(91,center,Vec3.ZERO,1,1,0);}
    public void clashInterrupt(){blood.clear();impacts.clear();grid.clear();aimHistory.clear();guard=0;finish();}
    public void clashVisual(boolean show){if(show&&domain>0)domainVisual();else fx(91,center,Vec3.ZERO,1,1,0);}
    private void tickDomain(){if(domain<=0||DomainClash.active(this))return;if(--domain==0){fx(91,center,Vec3.ZERO,1,1,0);return;}if(domain%20==0)domainVisual();double r=Math.min(512,v("DomainRadius"));for(LivingEntity p:BossTargets.around(this,center,r,true)){ServerPlayer caster=MaidBridge.progression(p);if(caster!=null){if(MaidBridge.isCaster(caster)){Skills.closeDomain(caster);GuardianPlayerSkills.closeOwnedDomain(caster,false);}Curse.status(caster,"PureLandNoHeal",3);}if(domain%Math.max(1,ticks("DomainInterval"))==0)hit(p,v("DomainDamage"),false,domainBonus);}}
    @Override public boolean hurt(DamageSource source,float damage){
        if(friendly()){if(!level().isClientSide&&dialogueFinished()&&source.getEntity() instanceof Player p)p.sendSystemMessage(Component.literal("虎天帝：别闹"));return false;}
        if(level().isClientSide||completingDeath||Float.isNaN(damage)||damage<=0)return false;
        boolean ranged=source.is(DamageTypeTags.IS_PROJECTILE)||source.getEntity()!=null&&source.getEntity()!=this&&distanceToSqr(source.getEntity())>v("Reach")*v("Reach");
        if(!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)){
            if(guard>0&&v("MeleeImmune")>0&&(source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)||source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK))){if(source.getEntity() instanceof LivingEntity e){e.knockback(1,getX()-e.getX(),getZ()-e.getZ());if(BossTargets.valid(this,e))setTarget(e);}return false;}
            if(guard>0&&ranged)damage*=Math.max(0,1-v("RangedReduction")/100);
        }
        // 包含穿透无敌的伤害；外部事件后还有 setHealth 的最终兜底。
        damage=GuardianRules.cap(damage,cap());if(damage<=0)return false;
        ServerPlayer caster=CombatSettings.caster(source);if(caster!=null)participants.add(caster.getUUID());
        float before=getHealth();DamageSource previous=activeDamage;activeDamage=source;
        if(damageDepth++==0)damageFloor=Math.max(GuardianRules.DEFEAT_HEALTH,before-cap());
        boolean hit;
        try{hit=super.hurt(source,damage);}finally{activeDamage=previous;if(--damageDepth==0)damageFloor=0;}
        if(hit&&!friendly()&&before>getHealth()&&ranged&&action()==10){finish();stun=ticks("HealStun");fx(85,getEyePosition(),Vec3.ZERO,1,10,0);}
        return hit;
    }
    public float cap(){return amount("HitCap");}
    @Override public boolean doHurtTarget(Entity e){return false;}
    @Override public boolean isPushable(){return !friendly();}
    @Override public boolean removeWhenFarAway(double d){return false;}
    @Override protected boolean shouldDespawnInPeaceful(){return !friendly();}
    @Override public boolean causeFallDamage(float d,float m,DamageSource s){return false;}
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);if(!friendly())bar.addPlayer(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);}
    @Override public void kill(){if(level().isClientSide)super.kill();else hurt(damageSources().genericKill(),Float.MAX_VALUE);}
    @Override public void die(DamageSource source){
        if(level().isClientSide){super.die(source);return;}
        if(completingDeath){super.die(source);confirmVictory();return;}
        if(friendly())return;
        DamageSource previous=activeDamage;activeDamage=source;
        ServerPlayer caster=CombatSettings.caster(source);if(caster!=null)participants.add(caster.getUUID());
        try{if(getHealth()<=GuardianRules.DEFEAT_HEALTH)beginDefeat(source);else setHealth(0);}finally{activeDamage=previous;}
    }
    private void beginDefeat(DamageSource source){
        if(friendly()||completingDeath)return;
        ServerPlayer killer=source==null?null:CombatSettings.caster(source);
        if(killer==null&&getKillCredit() instanceof ServerPlayer player)killer=player;
        if(killer!=null)participants.add(killer.getUUID());
        if(owner!=null)participants.add(owner);
        speaker=killer!=null?killer.getUUID():owner;defeatSource=source;
        entityData.set(FRIENDLY,true);rawHealth(GuardianRules.DEFEAT_HEALTH);dead=false;deathTime=0;
        end();setTarget(null);setLastHurtByMob(null);getNavigation().stop();setDeltaMovement(Vec3.ZERO);entityData.set(ACTION,0);entityData.set(AGE,0);
        peaceHome=position();boost=guard=stun=0;dialogueTick=dialogueLine=0;
        setCustomName(Component.literal("虎杖悠仁 · 对话中"));setCustomNameVisible(true);clearFire();removeAllEffects();
        // 新对话逐句发送；击败瞬间只触发一次专属音乐槽位。
        if(!victorySoundPlayed){victorySoundPlayed=true;SkillSounds.play(this,"guardian_victory");}
        // 对话结束后才发放传承；击败本身不再直接跳过成长阶段。
    }
    private void finishDialogue(){
        if(!dialogueFinished())return;
        completingDeath=false;dead=false;deathTime=0;xpReward=0;rawHealth(GuardianRules.DEFEAT_HEALTH);
        setCustomName(Component.literal("虎天帝"));setCustomNameVisible(true);setPersistenceRequired();
        confirmVictory();
    }
    public void confirmVictory(){
        if(level().isClientSide||rewardIssued||!friendly()||!dialogueFinished())return;
        Set<UUID> qualified=new HashSet<>();
        for(UUID id:participants){ServerPlayer member=MaidBridge.find(server(),id);if(member!=null&&VoidJourney.evolved(member))qualified.add(id);}
        GuardianProgress.complete(server().getServer(),getUUID(),qualified);
        for(UUID id:qualified){ServerPlayer member=MaidBridge.find(server(),id);if(member instanceof MaidBridge.Actor actor)GuardianProgress.deliverMaid(actor);}
        rewardIssued=true;
    }
    private boolean inCourtyard(ServerPlayer p){return p.level()==level()&&Math.abs(p.getY()-peaceHome.y)<32&&(peaceHome.x>=IwateVillage.HOME_X&&peaceHome.x<IwateVillage.HOME_X+128&&peaceHome.z>=IwateVillage.HOME_Z&&peaceHome.z<IwateVillage.HOME_Z+128?p.getX()>=IwateVillage.HOME_X&&p.getX()<IwateVillage.HOME_X+128&&p.getZ()>=IwateVillage.HOME_Z&&p.getZ()<IwateVillage.HOME_Z+128:p.position().distanceToSqr(peaceHome)<64*64);}
    private void tickResident(){
        if(tickCount%20!=0)return;
        for(ServerPlayer p:server().getServer().getPlayerList().getPlayers()){
            boolean inside=inCourtyard(p);if(inside)courtyardVisitors.add(p.getUUID());else if(courtyardVisitors.remove(p.getUUID()))p.sendSystemMessage(Component.literal("虎天帝：有空再来"));
            long now=level().getGameTime();if(p.level()==level()&&distanceToSqr(p)<8*8&&now>=nextGreeting.getOrDefault(p.getUUID(),0L)&&random.nextInt(5)==0){p.sendSystemMessage(Component.literal(random.nextBoolean()?"虎天帝：来了？":"虎天帝：这边风景不错"));nextGreeting.put(p.getUUID(),now+1200);}
        }
    }
    @Override protected net.minecraft.world.InteractionResult mobInteract(Player player,net.minecraft.world.InteractionHand hand){
        if(!friendly()||!dialogueFinished())return super.mobInteract(player,hand);
        var stack=player.getItemInHand(hand);if(stack.isEmpty())return net.minecraft.world.InteractionResult.PASS;
        if(!level().isClientSide){if(gifts.size()>=1024){player.sendSystemMessage(Component.literal("虎天帝：东西够多了，先留着吧"));return net.minecraft.world.InteractionResult.SUCCESS;}var gift=stack.copy();gift.setCount(1);gifts.add(gift.save(new CompoundTag()));stack.shrink(1);player.sendSystemMessage(Component.literal("虎天帝：谢了"));}
        return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
    }
    private void tickFriend(){
        rawHealth(GuardianRules.DEFEAT_HEALTH);bar.setVisible(false);setTarget(null);setLastHurtByMob(null);
        getNavigation().stop();setDeltaMovement(Vec3.ZERO);setPos(peaceHome.x,peaceHome.y,peaceHome.z);clearFire();
        if(dialogueFinished()){finishDialogue();tickResident();return;}
        dialogueTick++;
        if(dialogueLine<DIALOGUE.length&&GuardianTimeline.due(dialogueLine,dialogueTick)){
            String line=DIALOGUE[dialogueLine++];
            for(ServerPlayer p:server().getServer().getPlayerList().getPlayers())if(participants.contains(p.getUUID())||p.getUUID().equals(speaker)||p.level()==level()&&p.distanceToSqr(this)<=96*96)p.sendSystemMessage(Component.literal(line));
        }
        if(dialogueFinished())finishDialogue();
    }
    private void end(){bar.removeAllPlayers();if(!level().isClientSide&&domain>0)fx(91,center,Vec3.ZERO,1,1,0);domain=0;blood.clear();impacts.clear();aimHistory.clear();grid.clear();}
    @Override public void remove(RemovalReason r){grid.clear();bar.removeAllPlayers();if(!level().isClientSide&&domain>0)fx(91,center,Vec3.ZERO,1,1,0);super.remove(r);}
    @Override public void addAdditionalSaveData(CompoundTag t){
        super.addAdditionalSaveData(t);t.put("ResidentGifts",gifts.copy());t.putFloat("DomainBonus",domainBonus);t.put("GuardianSettings",settings.copy());t.putBoolean("Configured",configured);
        if(owner!=null)t.putUUID("ControllerOwner",owner);if(speaker!=null)t.putUUID("Speaker",speaker);
        t.putInt("Domain",domain);t.putInt("Boost",boost);t.putDouble("CenterX",center.x);t.putDouble("CenterY",center.y);t.putDouble("CenterZ",center.z);
        t.putInt("Sequence",sequence);t.putInt("Guard",guard);t.putLong("NextGuard",nextGuard);t.putLong("NextBeam",nextBeam);t.putLong("NextDomain",nextDomain);
        t.putBoolean("Friendly",friendly());t.putInt("DialogueTick",dialogueTick);t.putInt("DialogueLine",dialogueLine);t.putBoolean("CompletingDeath",completingDeath);t.putBoolean("GuardianRewardIssued",rewardIssued);t.putBoolean("GuardianVictorySoundPlayed",victorySoundPlayed);
        t.putDouble("PeaceX",peaceHome.x);t.putDouble("PeaceY",peaceHome.y);t.putDouble("PeaceZ",peaceHome.z);
        ListTag ids=new ListTag();for(UUID id:participants)ids.add(StringTag.valueOf(id.toString()));t.put("GuardianParticipants",ids);
    }
    @Override public void readAdditionalSaveData(CompoundTag t){
        healthReady=false;try{super.readAdditionalSaveData(t);}finally{healthReady=true;}
        gifts=t.getList("ResidentGifts",10).copy();domainBonus=t.getFloat("DomainBonus");settings=t.getCompound("GuardianSettings");GuardianSettings.defaults(settings);configured=t.getBoolean("Configured");
        if(t.hasUUID("ControllerOwner"))owner=t.getUUID("ControllerOwner");if(t.hasUUID("Speaker"))speaker=t.getUUID("Speaker");
        domain=t.getInt("Domain");boost=t.getInt("Boost");sequence=t.getInt("Sequence");center=new Vec3(t.getDouble("CenterX"),t.getDouble("CenterY"),t.getDouble("CenterZ"));
        guard=t.getInt("Guard");nextGuard=t.getLong("NextGuard");nextBeam=t.getLong("NextBeam");nextDomain=t.contains("NextDomain")?t.getLong("NextDomain"):domain>0?level().getGameTime()+GuardianRules.DOMAIN_COOLDOWN_TICKS:0;
        entityData.set(FRIENDLY,t.getBoolean("Friendly"));dialogueTick=Math.max(0,t.getInt("DialogueTick"));dialogueLine=Math.max(0,Math.min(DIALOGUE.length,t.getInt("DialogueLine")));
        peaceHome=new Vec3(t.getDouble("PeaceX"),t.getDouble("PeaceY"),t.getDouble("PeaceZ"));participants.clear();
        for(Tag id:t.getList("GuardianParticipants",8))try{participants.add(UUID.fromString(id.getAsString()));}catch(IllegalArgumentException ignored){}
        rewardIssued=t.getBoolean("GuardianRewardIssued");victorySoundPlayed=t.getBoolean("GuardianVictorySoundPlayed");completingDeath=t.getBoolean("CompletingDeath");
        if(friendly()){
            domain=boost=guard=0;bar.setVisible(false);if(owner!=null)participants.add(owner);if(speaker!=null)participants.add(speaker);
            // 旧版和解实体与中途保存的对话都接回同一条完成流程，不重置台词。
            completingDeath=false;dead=false;deathTime=0;rawHealth(GuardianRules.DEFEAT_HEALTH);
            if(dialogueFinished())setCustomName(Component.literal("虎天帝"));
        }
    }
}
