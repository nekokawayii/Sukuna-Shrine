package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;

/** 通关后第二页的守护者同款招式：服务端校验资格、消耗、冷却、目标及敌友。 */
public final class GuardianPlayerSkills {
    public static final int FIRST=7,COUNT=10;
    public static final String[] NAMES={"咒力拳","径庭拳","超新星","赤鳞跃动","网格斩","领域展开 · 净土","连续拳","术式反转外放","斩击无下限","百敛·穿血"};
    public static final String[] SHORT={"咒力拳","径庭","超新星","赤鳞","网斩","净土","连拳","反转","无下限","百敛·穿血"};
    public static final String[] DESC={"贴身咒力连击 · 黑闪判定","拳击后延迟灵魂冲击","追踪准星目标 · 毒血减速","短时间提高移动、拳速和伤害","身周方形网格 · 每个目标命中一次","范围必中 · 敌人禁疗 · 压制御厨子","侧向瞬移 · 冲刺连续拳","准星附近生物回复10生命 · 护甲修复25耐久","弹开近战 · 远程减伤75%","蓄力后持续直线穿血 · 墙体可挡"};
    public static final int[] COST={30,60,150,100,120,300,150,100,200,180};
    private static final int[] COOLDOWN={20,60,160,500,120,3600,180,240,1800,400};
    private static final int[] ACTION={1,3,4,5,6,8,9,10,11,12};
    private static final UUID SPEED=UUID.fromString("c820ccf7-2726-4abd-bd53-a207ca1efb01"),ATTACK=UUID.fromString("c820ccf7-2726-4abd-bd53-a207ca1efb02"),ATTACK_SPEED=UUID.fromString("c820ccf7-2726-4abd-bd53-a207ca1efb03");
    private static final Map<UUID,State> states=new HashMap<>();
    private static final List<Delayed> delayed=new ArrayList<>();
    private static final List<Blood> blood=new ArrayList<>();
    private static final List<Poison> poisons=new ArrayList<>();
    private static final List<Grid> grids=new ArrayList<>();
    private static final class State {
        final ServerPlayer p;final ServerLevel level;final long[] cooldowns=new long[COUNT];
        CompoundTag settings;int casting=-1,age;UUID target;long boostUntil,guardUntil,domainUntil,stunUntil;Vec3 center=Vec3.ZERO;
        State(ServerPlayer p){this.p=p;level=p.serverLevel();settings=BossControl.guardian(p.server);for(int i=0;i<COUNT;i++)cooldowns[i]=Curse.data(p).getLong("GuardianSkillCooldown"+i);}
        double v(String key){return Math.max(0,GuardianSettings.value(settings,key));}
        int ticks(String key){return Math.max(1,(int)Math.min(12000,v(key)*20));}
        int count(String key,int max){return Math.max(0,(int)Math.min(max,v(key)));}
    }
    private record Delayed(ServerPlayer owner,ServerLevel level,UUID target,long due,float amount){}
    private static final class Blood {final State s;final UUID target;Vec3 pos,velocity;int age;Blood(State s,LivingEntity t,Vec3 velocity){this.s=s;target=t==null?null:t.getUUID();pos=s.p.getEyePosition();this.velocity=t==null?s.p.getLookAngle().scale(Math.max(0.2,velocity.length())).add(velocity.scale(0.18)):velocity;}}
    private record Poison(ServerPlayer owner,ServerLevel level,UUID target,long until,float damage){}
    private static final class Grid {final State s;final AABB bounds;final Set<UUID> hit=new HashSet<>();int remaining=GuardianGridSlash.LIFETIME;Grid(State s){this.s=s;Vec3 p=s.p.position();double r=Math.min(512,s.v("GridRadius"));bounds=new AABB(p.x-r,p.y,p.z-r,p.x+r,p.y+8,p.z+r);}}
    private static boolean valid(State s){return valid(s.p,s.level)&&GuardianCombat.awakened(s.p);}
    private static boolean valid(ServerPlayer p,ServerLevel level){return p.isAlive()&&!p.isRemoved()&&!p.isSpectator()&&p.level()==level;}
    private static void fx(State s,int kind,Vec3 point,Vec3 direction,float size,int life,long seed){Network.send(s.level,new Network.Effect(kind,s.p.getId(),point,direction,size,life,seed));}
    private static LivingEntity target(State s){var e=s.target==null?null:s.level.getEntity(s.target);return e instanceof LivingEntity t&&CombatSettings.canHit(s.p,t)?t:null;}
    private static boolean reach(State s,LivingEntity t){return contains(s,t)||s.p.distanceToSqr(t)<=s.v("Reach")*s.v("Reach")&&s.p.hasLineOfSight(t);}
    private static boolean contains(State s,LivingEntity t){double r=Math.min(512,s.v("DomainRadius"));return !DomainClash.active(s.p)&&t!=null&&t.level()==s.level&&s.domainUntil>s.level.getGameTime()&&t.position().distanceToSqr(s.center)<=r*r&&CombatSettings.canHit(s.p,t);}
    public static boolean locked(ServerPlayer p){for(State s:states.values())if(valid(s)&&contains(s,MaidBridge.body(p)))return true;return false;}
    private static boolean hit(State s,LivingEntity target,double base,boolean punch){
        if(DomainClash.active(s.p)||DomainClash.active(target)||!CombatSettings.canHit(s.p,target))return false;
        double amount=base+(s.boostUntil>s.level.getGameTime()?s.v("DamageMultiplier"):0);
        // 黑闪判定：`casting==6` 是净土领域、`casting==1` 是**径庭拳**——两者都不再触发黑闪（用户要求），只有咒力拳等其它拳招才吃这个概率。
        boolean flash=punch&&s.casting!=6&&s.casting!=1&&s.p.getRandom().nextDouble()*100<s.v("FlashChance");
        if(flash)amount=Math.pow(amount,Math.min(5,s.v("FlashExponent")));
        target.invulnerableTime=0;
        AttackTypes.mark(AttackTypes.LIGHTNING);boolean hit=CombatSettings.hurt(s.p,target,s.level.damageSources().sonicBoom(s.p),CombatSettings.spellDamage(s.p,Math.min(1000000,amount)));
        if(hit){fx(s,flash?85:81,target.getBoundingBox().getCenter(),Vec3.ZERO,1,12,target.getId());if(flash)SkillSounds.play(s.p,"black_flash");if(punch&&target instanceof ServerPlayer p)SoulCombat.peel(p,false);}
        return hit;
    }
    public static boolean sustaining(ServerPlayer p){State s=states.get(p.getUUID());return s!=null&&s.domainUntil>0&&(DomainClash.active(p)||s.domainUntil>p.level().getGameTime());}
    public static void cancel(ServerPlayer p){State s=states.remove(p.getUUID());if(s!=null)cleanup(s);}
    static void testReset(ServerPlayer p){cancel(p);delayed.removeIf(d->d.owner==p||d.target.equals(p.getUUID()));blood.removeIf(b->b.s.p==p);poisons.removeIf(v->v.owner==p||v.target.equals(p.getUUID()));grids.removeIf(g->g.s.p==p);}
    public static void closeOwnedDomain(ServerPlayer p,boolean voluntary){State s=states.get(p.getUUID());if(s!=null)closeDomain(s,voluntary);}
    public static void extendDomain(ServerPlayer p,int ticks){State s=states.get(p.getUUID());if(s!=null&&s.domainUntil>0)s.domainUntil+=ticks;}
    public static void clashInterrupt(ServerPlayer p){State s=states.get(p.getUUID());if(s!=null)s.casting=-1;}
    public static void clashVisual(ServerPlayer p,boolean show){State s=states.get(p.getUUID());if(s==null||s.domainUntil<=0)return;if(show)fx(s,90,s.center,Vec3.ZERO,(float)Math.min(512,s.v("DomainRadius")),(int)Math.max(1,s.domainUntil-s.level.getGameTime()),1);else fx(s,91,s.center,Vec3.ZERO,1,1,0);}
    public static boolean casting(ServerPlayer p){State s=states.get(p.getUUID());return s!=null&&s.p==p&&s.level==p.level()&&s.casting>=0;}
    private static long cooldownUntil(ServerPlayer p,State s,int skill){
        // 女仆的两种领域只读共用计时，旧版净土的三分钟记录不再叠加。
        if(skill==5&&MaidBridge.isCaster(p))return Curse.data(p).getLong("MaidDomainCooldown");
        return s==null?Curse.data(p).getLong("GuardianSkillCooldown"+skill):s.cooldowns[skill];
    }
    public static boolean available(ServerPlayer p,int skill){
        if(skill<0||skill>=COUNT||!p.isAlive()||p.isSpectator()||DomainClash.active(p)||!GuardianCombat.awakened(p))return false;
        if(skill==7)return p instanceof MaidBridge.Actor actor?ReverseOutput.maidReady(actor):Curse.canSpend(p,COST[skill])&&!Curse.active(p,"ReverseOutputCooldown");
        State s=states.get(p.getUUID());long now=p.level().getGameTime();
        if(!Curse.canSpend(p,COST[skill])||!Curse.canUseTechnique(p)||SoulCombat.suppressed(p)||now<cooldownUntil(p,s,skill)||s!=null&&(s.casting>=0||s.stunUntil>now))return false;
        if(skill==5)return MaidBridge.domainReady(p)&&!Skills.domainActive(p)&&!sustaining(p)&&(!MaidBridge.isCaster(p)||!GuardianEntity.locked(MaidBridge.body(p))&&!locked(p));
        return true;
    }
    public static void cast(ServerPlayer p,int skill){
        if(skill==7){ReverseOutput.cast(p);return;}
        if(skill<0||skill>=COUNT||!p.isAlive()||p.isSpectator()||DomainClash.active(p)||!GuardianCombat.awakened(p))return;
        if(Curse.burnout(p)&&skill!=7){p.displayClientMessage(Component.literal("术式熔断中：只能使用术式反转或修复熔断"),true);return;}
        State s=states.get(p.getUUID());if(s==null||s.p!=p||s.level!=p.level()){if(s!=null)cleanup(s);s=new State(p);states.put(p.getUUID(),s);}
        long now=p.level().getGameTime();
        if(skill==5&&Skills.domainActive(p)){Skills.closeDomain(p,true);return;}
        if(skill==5&&s.domainUntil>now){closeDomain(s,true);return;}
        if(SoulCombat.suppressed(p)||s.stunUntil>now){p.displayClientMessage(Component.literal("术式被压制，暂时无法释放"),true);return;}
        if(s.casting>=0){p.displayClientMessage(Component.literal("当前招式尚未结束"),true);return;}
        long cooldown=cooldownUntil(p,s,skill);
        if(now<cooldown){p.displayClientMessage(Component.literal(NAMES[skill]+"冷却中 · "+((cooldown-now+19)/20)+"秒"),true);return;}
        if(skill==5&&!MaidBridge.domainReady(p))return;
        if(skill==5&&MaidBridge.isCaster(p)&&(GuardianEntity.locked(MaidBridge.body(p))||locked(p))){p.displayClientMessage(Component.literal("正在被净土领域压制"),true);return;}
        LivingEntity target=Skills.aimTarget(p);
        s.settings=BossControl.guardian(p.server);
        if(!Curse.spend(p,COST[skill]))return;
        s.cooldowns[skill]=now+(skill==5&&MaidBridge.isCaster(p)?ShrineRules.MAID_DOMAIN_COOLDOWN_TICKS:(long)Math.ceil(COOLDOWN[skill]*SoulCombat.cooldown(p)));Curse.data(p).putLong("GuardianSkillCooldown"+skill,s.cooldowns[skill]);s.target=target==null?null:target.getUUID();s.casting=skill;s.age=0;
        SkillSounds.play(p,"guardian_"+ACTION[skill]);
        switch(skill){
            case 2 -> {int count=s.count("BloodCount",128);for(int i=0;i<count;i++){double a=i*Math.PI*2/Math.max(1,count);if(blood.size()<4096)blood.add(new Blood(s,target,new Vec3(Math.cos(a),((i%5)-2)*0.12,Math.sin(a)).scale(Math.min(8,s.v("BloodSpeed")))));}s.casting=-1;}
            case 3 -> {s.boostUntil=now+s.ticks("BoostSeconds");boost(s,true);fx(s,136,p.position(),Vec3.ZERO,1,24,0);s.casting=-1;}
            case 4 -> {if(grids.size()<128)grids.add(new Grid(s));fx(s,126,p.position(),Vec3.ZERO,(float)Math.min(512,s.v("GridRadius")),GuardianGridSlash.LIFETIME,0);s.casting=-1;}
            case 5 -> {Skills.closeDomain(p);MaidBridge.domainOpened(p);s.center=p.position();s.domainUntil=now+ShrineRules.domainTicks(MaidBridge.isCaster(p));fx(s,90,s.center,Vec3.ZERO,(float)Math.min(512,s.v("DomainRadius")),(int)(s.domainUntil-now),1);s.casting=-1;DomainClash.opened(p,s.center,Math.min(512,s.v("DomainRadius")));}
            case 6 -> {if(target==null){fx(s,84,p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(8)),1,s.ticks("DashWindup"),0);break;}Vec3 side=target.getLookAngle().cross(new Vec3(0,1,0)).normalize();Vec3 point=target.position().add(side.scale(Math.min(6,s.v("TeleportDistance"))));if(p.distanceToSqr(target)<=32*32&&p.hasLineOfSight(target)&&s.level.noCollision(p,p.getBoundingBox().move(point.subtract(p.position()))))p.teleportTo(point.x,point.y,point.z);fx(s,84,p.getEyePosition(),target.getEyePosition(),1,s.ticks("DashWindup"),0);}
        }
    }
    private static void boost(State s,boolean enabled){
        attribute(s.p,Attributes.MOVEMENT_SPEED,SPEED,enabled?s.v("SpeedMultiplier"):0);
        attribute(s.p,Attributes.ATTACK_DAMAGE,ATTACK,enabled?BalanceRules.strength(s.v("DamageMultiplier")):0);
        attribute(s.p,Attributes.ATTACK_SPEED,ATTACK_SPEED,enabled?s.v("AttackSpeedMultiplier"):0);
    }
    private static void attribute(ServerPlayer p,Attribute key,UUID id,double value){var a=MaidBridge.body(p).getAttribute(key);if(a==null)return;a.removeModifier(id);if(value!=0)a.addTransientModifier(new AttributeModifier(id,"赤鳞跃动",value,AttributeModifier.Operation.ADDITION));}
    private static void closeDomain(State s){closeDomain(s,false);}
    private static void closeDomain(State s,boolean voluntary){if(s.domainUntil!=0){boolean early=voluntary&&s.level.getGameTime()<s.domainUntil;fx(s,91,s.center,Vec3.ZERO,1,1,0);s.domainUntil=0;Curse.endDomain(s.p,early);}}
    private static void cleanup(State s){boost(s,false);closeDomain(s);s.casting=-1;s.guardUntil=s.boostUntil=0;}
    private static void channel(State s){
        int age=++s.age;LivingEntity t=target(s);ServerPlayer p=s.p;
        if(t==null&&(s.casting==0||s.casting==1||s.casting==6)){t=Skills.aimTarget(p);if(t!=null)s.target=t.getUUID();}
        if((s.casting==0||s.casting==1||s.casting==6)&&age%4==1)fx(s,140,p.getEyePosition(),t==null?p.getEyePosition().add(p.getLookAngle().scale(Math.min(8,s.v("Reach")))):t.getEyePosition(),1,5,0);
        switch(s.casting){
            case 0 -> {int gap=Math.max(1,s.ticks("PunchInterval")-(s.boostUntil>s.level.getGameTime()?(int)s.v("AttackSpeedMultiplier"):0));if((age-1)%gap==0&&t!=null&&reach(s,t))hit(s,t,s.v("Punch"),true);if(age>=gap*3)s.casting=-1;}
            case 1 -> {int delay=s.ticks("SoulDelay");if((age==1||age==delay+1)&&t!=null&&reach(s,t)){hit(s,t,s.v("Punch"),true);if(delayed.size()<4096)delayed.add(new Delayed(p,s.level,t.getUUID(),s.level.getGameTime()+delay,(float)s.v("Punch")));fx(s,140,p.getEyePosition(),t.getEyePosition(),1,8,0);}if(age>delay+1)s.casting=-1;}
            case 6 -> {int wind=s.ticks("DashWindup"),gap=Math.max(1,s.ticks("ComboInterval")-(s.boostUntil>s.level.getGameTime()?(int)s.v("AttackSpeedMultiplier"):0)),count=s.count("ComboCount",100);
                if(age==wind+1){Vec3 step=(t==null?p.getLookAngle():t.position().subtract(p.position())).multiply(1,0,1).normalize().scale(Math.min(12,s.v("DashDistance"))/10);for(int i=0;i<10;i++){if(!s.level.noCollision(p,p.getBoundingBox().move(step)))break;p.teleportTo(p.getX()+step.x,p.getY(),p.getZ()+step.z);}}
                if(age>wind&&(age-wind-1)%gap==0&&(age-wind-1)/gap<count&&t!=null){fx(s,142,p.getEyePosition(),t.getEyePosition(),1,5,(age-wind-1)/gap);if(reach(s,t)&&hit(s,t,s.v("Punch"),true))t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,s.ticks("ComboSlowSeconds"),Math.max(0,s.count("ComboSlow",10)-1)));}
                if(age>=wind+Math.max(1,count*gap))s.casting=-1;
            }
            case 7 -> {int wind=s.ticks("HealWindup"),duration=s.ticks("HealSeconds");if(age<=wind+duration&&age%5==0)fx(s,137,p.position(),Vec3.ZERO,1,8,0);if(age>wind&&age<=wind+duration)p.heal(BalanceRules.damage(10.0/20.0));if(age>=wind+duration+s.ticks("HealRecovery"))s.casting=-1;}
            case 8 -> {if(age>=s.ticks("GuardWindup")){s.guardUntil=s.level.getGameTime()+s.ticks("GuardSeconds");fx(s,127,p.position().add(0,1,0),Vec3.ZERO,1,10,0);s.casting=-1;}}
            case 9 -> {int wind=s.ticks("BeamWindup"),duration=s.ticks("BeamSeconds");Vec3 origin=p.getEyePosition().add(p.getLookAngle().scale(0.3));
                LivingEntity aim=Skills.aimTarget(p);Vec3 dir=aim==null?p.getLookAngle():aim.getBoundingBox().getCenter().subtract(origin).normalize();
                if(age<=wind){fx(s,138,origin,Vec3.ZERO,(float)(1-age/(double)wind),3,0);break;}
                Vec3 end=origin.add(dir.scale(Math.min(128,s.v("BeamRange"))));var block=s.level.clip(new ClipContext(origin,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));if(block.getType()!=HitResult.Type.MISS)end=block.getLocation();fx(s,139,origin,end,0.18f,2,0);
                if((age-wind)%s.ticks("BeamInterval")==0)for(LivingEntity victim:s.level.getEntitiesOfClass(LivingEntity.class,new AABB(origin,end).inflate(0.5),e->CombatSettings.canHit(p,e)))if(victim.getBoundingBox().inflate(0.2).contains(origin)||victim.getBoundingBox().inflate(0.2).clip(origin,end).isPresent())hit(s,victim,s.v("BeamDamage"),false);
                if(age>=wind+duration)s.casting=-1;
            }
            default -> s.casting=-1;
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(Iterator<State> it=states.values().iterator();it.hasNext();){State s=it.next();if(!valid(s)){cleanup(s);it.remove();continue;}long now=s.level.getGameTime();
            if(DomainClash.active(s.p))continue;
            if(SoulCombat.suppressed(s.p)||Curse.burnout(s.p)){s.casting=-1;s.guardUntil=0;if(s.boostUntil>0){s.boostUntil=0;boost(s,false);}closeDomain(s);continue;}
            if(s.boostUntil>0&&now>=s.boostUntil){s.boostUntil=0;boost(s,false);}
            if(s.boostUntil>now&&now%8==0)fx(s,136,s.p.position(),Vec3.ZERO,1,12,0);
            if(s.guardUntil>now&&now%6==0)fx(s,127,s.p.position().add(0,1,0),Vec3.ZERO,1,8,0);
            if(s.domainUntil>0){if(now>=s.domainUntil||MaidBridge.isCaster(s.p)&&(GuardianEntity.locked(MaidBridge.body(s.p))||locked(s.p))||now%20==0&&!Curse.spend(s.p,70))closeDomain(s);else{
                double r=Math.min(512,s.v("DomainRadius"));if(now%20==0)fx(s,90,s.center,Vec3.ZERO,(float)r,(int)(s.domainUntil-now),1);
                for(LivingEntity t:Skills.targets(s.level,s.p,s.center,r)){if(!contains(s,t))continue;t.getPersistentData().putLong("GuardianSkillNoHeal",now+2);ServerPlayer caster=MaidBridge.progression(t);if(caster!=null&&MaidBridge.isCaster(caster))Skills.closeDomain(caster);if(now%s.ticks("DomainInterval")==0)hit(s,t,s.v("DomainDamage"),false);}
            }}
            if(s.casting>=0)channel(s);
        }
        for(Iterator<Delayed> it=delayed.iterator();it.hasNext();){Delayed d=it.next();if(d.level.getGameTime()<d.due)continue;it.remove();State s=states.get(d.owner.getUUID());if(s!=null&&valid(s)&&valid(d.owner,d.level)&&d.level.getEntity(d.target) instanceof LivingEntity t&&hit(s,t,d.amount,false)){t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,s.ticks("SoulSlowSeconds"),2));fx(s,141,t.getBoundingBox().getCenter(),Vec3.ZERO,1,12,t.getId());}}
        for(Iterator<Blood> it=blood.iterator();it.hasNext();){Blood b=it.next();State s=b.s;if(!valid(s)||++b.age>120){it.remove();continue;}
            LivingEntity t=b.target!=null&&s.level.getEntity(b.target) instanceof LivingEntity target&&CombatSettings.canHit(s.p,target)?target:null;
            double speed=Math.min(8,s.v("BloodSpeed"));if(t!=null){Vec3 dir=t.getBoundingBox().getCenter().subtract(b.pos).normalize();b.velocity=b.velocity.scale(0.65).add(dir.scale(speed*0.35)).normalize().scale(speed);}Vec3 next=b.pos.add(b.velocity);
            if(t==null)for(LivingEntity candidate:s.level.getEntitiesOfClass(LivingEntity.class,new AABB(b.pos,next).inflate(0.5),v->CombatSettings.canHit(s.p,v)))if(candidate.getBoundingBox().inflate(0.4).clip(b.pos,next).isPresent()){t=candidate;break;}
            var block=s.level.clip(new ClipContext(b.pos,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,s.p));if(block.getType()!=HitResult.Type.MISS)next=block.getLocation();fx(s,125,b.pos,next,1,3,b.age);
            if(t!=null&&(contains(s,t)||t.getBoundingBox().inflate(0.4).clip(b.pos,next).isPresent())){
                if(hit(s,t,s.v("BloodDamage"),false)){long until=s.level.getGameTime()+s.ticks("PoisonSeconds");UUID victimId=t.getUUID();poisons.removeIf(v->v.owner==s.p&&v.target.equals(victimId));poisons.add(new Poison(s.p,s.level,t.getUUID(),until,(float)s.v("PoisonDamage")));t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,s.ticks("PoisonSeconds"),Math.max(0,s.count("PoisonSlow",10)-1)));ServerPlayer victim=MaidBridge.progression(t);if(victim!=null){Curse.data(victim).putLong("PlayerGuardianPoisonUntil",until);Curse.data(victim).putDouble("PlayerGuardianPoisonCooldown",s.v("PoisonCooldown"));}}
                it.remove();
            }else if(block.getType()!=HitResult.Type.MISS)it.remove();else b.pos=next;
        }
        for(Iterator<Poison> it=poisons.iterator();it.hasNext();){Poison poison=it.next();AttackTypes.mark(AttackTypes.MAGIC);var entity=poison.level.getEntity(poison.target);if(!valid(poison.owner,poison.level)||poison.level.getGameTime()>=poison.until||!(entity instanceof LivingEntity t)||!CombatSettings.canHit(poison.owner,t)){if(entity instanceof Player player&&Curse.data(player).getLong("PlayerGuardianPoisonUntil")==poison.until)Curse.data(player).remove("PlayerGuardianPoisonUntil");it.remove();continue;}if(poison.level.getGameTime()%20==0){t.invulnerableTime=0;CombatSettings.hurt(poison.owner,t,poison.level.damageSources().indirectMagic(poison.owner,poison.owner),CombatSettings.casterDamage(poison.owner,poison.damage));}}
        for(Iterator<Grid> it=grids.iterator();it.hasNext();){Grid grid=it.next();if(!valid(grid.s)){it.remove();continue;}for(LivingEntity t:grid.s.level.getEntitiesOfClass(LivingEntity.class,grid.bounds,e->CombatSettings.canHit(grid.s.p,e)&&!grid.hit.contains(e.getUUID())))if(hit(grid.s,t,grid.s.v("GridDamage"),false))grid.hit.add(t.getUUID());if(--grid.remaining<=0)it.remove();}
    }
    @SubscribeEvent public static void healing(LivingHealEvent event){if(event.getEntity().getPersistentData().getLong("GuardianSkillNoHeal")>event.getEntity().level().getGameTime())event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGH) public static void guard(LivingHurtEvent event){
        if(event.getEntity() instanceof ServerPlayer p)guardCaster(p,event);
    }
    public static void guardCaster(ServerPlayer p,LivingHurtEvent event){State s=states.get(p.getUUID());if(s==null||!valid(s)||s.guardUntil<=p.level().getGameTime()||SoulCombat.suppressed(p)||event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        var source=event.getSource();boolean melee=source.is(DamageTypes.PLAYER_ATTACK)||source.is(DamageTypes.MOB_ATTACK);
        if(melee&&s.v("MeleeImmune")>0){event.setCanceled(true);if(source.getEntity() instanceof LivingEntity attacker)attacker.knockback(1,p.getX()-attacker.getX(),p.getZ()-attacker.getZ());}
        else if(source.is(DamageTypeTags.IS_PROJECTILE)||source.getEntity()!=null&&p.distanceToSqr(source.getEntity())>s.v("Reach")*s.v("Reach"))event.setAmount(event.getAmount()*(float)Math.max(0,1-s.v("RangedReduction")/100));
    }
    @SubscribeEvent public static void interrupt(LivingDamageEvent event){if(event.getAmount()<=0||!(event.getEntity() instanceof ServerPlayer p))return;State s=states.get(p.getUUID());if(s!=null&&s.casting==7&&(event.getSource().is(DamageTypeTags.IS_PROJECTILE)||event.getSource().getEntity()!=null&&p.distanceToSqr(event.getSource().getEntity())>s.v("Reach")*s.v("Reach"))){s.casting=-1;s.stunUntil=p.level().getGameTime()+s.ticks("HealStun");p.displayClientMessage(Component.literal("反转术式被远程攻击打断"),true);}}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){for(State s:states.values())cleanup(s);states.clear();delayed.clear();blood.clear();poisons.clear();grids.clear();}
    private GuardianPlayerSkills(){}
}
