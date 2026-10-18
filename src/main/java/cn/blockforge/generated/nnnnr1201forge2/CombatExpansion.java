package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;

/** 所有新增战斗均在服务端结算；特效只是结果，不由客户端声明命中。 */
public final class CombatExpansion {
    private static final int HUNT=0,LINGER=1,GIANT=2,DRAGON=3,METEOR=4;
    private static final class Projectile {
        final ServerPlayer p;final ServerLevel level;final Set<UUID> hit=new HashSet<>();final Map<UUID,BitSet> dragonHits=new HashMap<>();
        Vec3 pos,dir;UUID target;int kind,age,stationary=-1,rise;double width;
        Projectile(ServerPlayer p,Vec3 pos,Vec3 dir,int kind,double width){this.p=p;level=p.serverLevel();this.pos=pos;this.dir=dir.normalize();this.kind=kind;this.width=width;}
    }
    private record Scheduled(ServerPlayer p,ServerLevel level,long due,Vec3 from,Vec3 dir,UUID target,double width){}
    private record Fall(ServerPlayer p,ServerLevel level,LivingEntity body,long until){}
    private static final class Rain {
        final ServerPlayer p;final ServerLevel level;final LivingEntity target;final Vec3 anchor;final long start,id;
        Rain(ServerPlayer p,LivingEntity t){this.p=p;level=p.serverLevel();target=t;anchor=t.position();start=level.getGameTime();id=--holdId;}
    }
    private static final class Chant {
        final ServerPlayer p;final ServerLevel level;final boolean dragon;final long start;final TechniqueInput.Input input;int word;long heartbeat;
        Chant(ServerPlayer p,boolean dragon,TechniqueInput.Input input){this.p=p;level=p.serverLevel();this.dragon=dragon;this.input=input;start=heartbeat=level.getGameTime();}
    }
    private static final class Star {
        final ServerPlayer p;final ServerLevel level;final Vec3 center,side,up;int strokes;long last;
        Star(ServerPlayer p){this.p=p;level=p.serverLevel();Vec3 dir=p.getLookAngle();center=p.getEyePosition().add(dir.scale(5));side=NetBranches.side(dir);up=side.cross(dir).normalize();}
    }
    private record Burst(ServerPlayer p,ServerLevel level,Vec3 center,long due){}
    private record CutSeries(ServerPlayer p,ServerLevel level,UUID target,int left,long due,double fraction){}
    private static final List<CutSeries> cutSeries=new ArrayList<>();
    private record AutoCast(ServerPlayer p,ServerLevel level,int skill,long start){}
    private static final Map<UUID,AutoCast> automatic=new HashMap<>();
    public static boolean automatic(ServerPlayer p){return automatic.containsKey(p.getUUID())||chanting(p);}
    public static boolean available(ServerPlayer p,int skill){return CombatExpansionRules.branch(skill)&&allowed(p)&&!automatic(p)&&!DismantleMotion.active(p)&&(skill!=CombatExpansionRules.DRAGON_STAR||Curse.awakened(p))&&(skill!=CombatExpansionRules.PURPLE_FURNACE||VoidJourney.evolved(p)&&!Curse.active(p,"PurpleFurnaceCooldown"))&&(skill!=CombatExpansionRules.CLEAVE_LINGER||Curse.fingers(p)>=5)&&Curse.canSpend(p,skill==CombatExpansionRules.DRAGON_STAR?2000+6*Curse.COST[0]:skill==CombatExpansionRules.PURPLE_FURNACE?Curse.COST[3]:Curse.COST[0]);}
    public static void castAutomatic(ServerPlayer p,int skill){if(!available(p,skill))return;if(skill==CombatExpansionRules.PURPLE_FURNACE){purple(p);return;}automatic.put(p.getUUID(),new AutoCast(p,p.serverLevel(),skill,p.level().getGameTime()));TechniqueInput.receive(p,new TechniqueInput.Input(skill,0,false,false,false));}
    private static final List<Projectile> projectiles=new ArrayList<>();
    private static final List<Scheduled> scheduled=new ArrayList<>();
    private static final List<Burst> stars=new ArrayList<>();
    private static final Map<UUID,Fall> falls=new HashMap<>();
    private static final Map<UUID,Rain> rains=new HashMap<>();
    private static final Map<UUID,Chant> chants=new HashMap<>();
    private static final Map<UUID,Star> drawing=new HashMap<>();
    /** 断流：一次性斩掉半径多少格内的全部流体（不碰方块、不看破坏方块开关）。 */
    public static final int STREAM_RADIUS=16;
    private static long holdId;
    static boolean valid(ServerPlayer p,ServerLevel l){return p.isAlive()&&!p.isRemoved()&&!p.isSpectator()&&p.level()==l&&MaidBridge.body(p).isAlive()&&!EternalLove.settling(MaidBridge.body(p))&&(MaidBridge.isCaster(p)||!p.hasDisconnected());}
    public static boolean allowed(ServerPlayer p){return valid(p,p.serverLevel())&&Journey.unlocked(p)&&Curse.canUseTechnique(p)&&!SoulCombat.suppressed(p)&&!MaidBridge.recovering(p)&&!GuardianPlayerSkills.locked(p);}
    public static void chantHeartbeat(ServerPlayer p){Chant c=chants.get(p.getUUID());if(c!=null)c.heartbeat=p.level().getGameTime();}
    public static boolean chanting(ServerPlayer p){return chants.containsKey(p.getUUID());}
    public static boolean controlled(LivingEntity e){if(e instanceof MaidBridge.Actor a)e=a.maid;if(SixfoldFinale.controlled(e))return true;Rain r=rains.get(e.getUUID());return r!=null&&r.target==e;}
    private static List<LivingEntity> near(ServerPlayer p,Vec3 at,double radius){return Skills.targets(p.serverLevel(),p,at,radius).stream().filter(t->!DomainClash.active(t)&&t.getBoundingBox().getCenter().distanceToSqr(at)<=radius*radius).sorted(Comparator.comparingDouble(t->t.getBoundingBox().getCenter().distanceToSqr(at))).toList();}
    private static void fx(ServerPlayer p,int kind,Vec3 at,Vec3 dir,float size,int life,long seed){Network.send(p.serverLevel(),new Network.Effect(kind,MaidBridge.body(p).getId(),at,dir,size,life,seed));}
    private static void line(ServerPlayer p,Vec3 a,Vec3 b,int life,long color){fx(p,CombatExpansionRules.FX_SLASH,a,b.subtract(a),1,life,color);}
    /** 调用处已验证解锁并扣费。基础解的姿态替换原输出，分支保留自身输出并追加姿态。 */
    public static boolean afterCast(ServerPlayer p,int skill){
        if(!CombatExpansionRules.dismantle(skill)&&!CombatExpansionRules.cleave(skill))return false;
        LivingEntity body=MaidBridge.body(p);TechniqueInput.Input input=TechniqueInput.intent(p);
        boolean crouch=input.crouch()||body.isShiftKeyDown();
        if(CombatExpansionRules.cleave(skill)&&crouch){
            if(!DismantleMotion.startPaid(p,DismantleMotionRules.GROUND_SMASH))rain(p);return true;
        }
        if(!CombatExpansionRules.dismantle(skill))return false;
        if(DismantleMotion.active(p))return false;
        int kind=DismantleMotionRules.choose(crouch,input.forward(),input.backward(),input.jump(),body.isSprinting(),body.onGround(),body.getDeltaMovement().y);
        boolean falling=!body.onGround()&&(body.getDeltaMovement().y<-.06||body.fallDistance>.1);
        // 明确的组合按键优先启动动作，下落护身斩仍追加，不能提前 return 吞掉后空翻。
        if(falling)fall(p);
        if(kind!=0)return DismantleMotion.startPaid(p,kind);
        if(falling)return true;
        return false;
    }
    public static void fallingBranch(ServerPlayer p){LivingEntity body=MaidBridge.body(p);if(!body.onGround()&&(body.getDeltaMovement().y<-.06||body.fallDistance>.1))fall(p);}
    private static void fall(ServerPlayer p){
        LivingEntity body=MaidBridge.body(p);if(falls.containsKey(body.getUUID()))return;
        falls.put(body.getUUID(),new Fall(p,p.serverLevel(),body,p.level().getGameTime()+1200));body.fallDistance=0;
        for(int i=0;i<CombatExpansionRules.FALL_CUTS;i++){double a=i*Math.PI*2/5;Vec3 dir=new Vec3(Math.cos(a)*.24,-1,Math.sin(a)*.24).normalize();schedule(p,p.position().add(0,1,0),dir,null,12,i*2);}
    }
    private static void landed(Fall f){
        if(falls.remove(f.body.getUUID())==null||!valid(f.p,f.level))return;f.body.fallDistance=0;
        int i=0;for(LivingEntity t:near(f.p,f.body.position(),64).stream().limit(CombatExpansionRules.LANDING_TARGETS).toList())schedule(f.p,f.body.getEyePosition(),t.getBoundingBox().getCenter().subtract(f.body.getEyePosition()),t.getUUID(),12,i++*2);
    }
    @SubscribeEvent public static void fallDamage(LivingFallEvent e){Fall f=falls.get(e.getEntity().getUUID());if(f!=null&&valid(f.p,f.level)){e.setDamageMultiplier(0);e.setDistance(0);landed(f);}}
    public static void motionRelease(ServerPlayer p,int kind){
        Vec3 forward=new Vec3(-Math.sin(Math.toRadians(p.getYRot())),0,Math.cos(Math.toRadians(p.getYRot())));
        if(kind==DismantleMotionRules.GROUND_SMASH){rain(p);fx(p,CombatExpansionRules.FX_DUST,p.position(),Vec3.ZERO,3,12,0);}
        if(kind==DismantleMotionRules.KNEEL){SlashForms.directed(p,p.getEyePosition(),forward,80);}
        if(kind==DismantleMotionRules.AIR_PUNCH){
            List<LivingEntity> targets=Skills.crosshairOrder(p,near(p,p.position(),64));int count=CombatExpansionRules.huntingCuts(p.getRandom().nextDouble());
            for(int i=0;i<count;i++){LivingEntity t=targets.isEmpty()?null:targets.get(i%targets.size());schedule(p,p.getEyePosition(),t==null?forward:t.getBoundingBox().getCenter().subtract(p.getEyePosition()),t==null?null:t.getUUID(),12,i*2);}
        }
        if(kind==DismantleMotionRules.SLIDE)for(int i=0;i<3;i++){schedule(p,p.position().add(0,.8,0),forward,null,12,i*4);schedule(p,p.position().add(0,.8,0),forward.scale(-1),null,12,i*4+2);}
        if(kind==DismantleMotionRules.BACKFLIP){
            fx(p,CombatExpansionRules.FX_DUST,p.position(),Vec3.ZERO,8,18,0);
            for(LivingEntity t:near(p,p.position(),8))SlashForms.hit(p,t,3);
            SkillDestruction.impact(p,p.position(),3);SkillSounds.play(p,"space_slash");
        }
    }
    private static void rain(ServerPlayer p){
        LivingEntity target=Skills.aimTarget(p);if(target==null||SixfoldFinale.active(target)||target.distanceToSqr(p)>96*96||DomainClash.active(target)||NetBranches.locked(target)||Uraume.locked(target)||controlled(target))return;
        Rain r=new Rain(p,target);rains.put(target.getUUID(),r);target.stopRiding();target.stopUsingItem();
        ServerPlayer victim=target instanceof ServerPlayer sp?sp:MaidBridge.caster(target);if(victim!=null){Skills.cancelCharge(victim);TechniqueInput.cancel(victim);DismantleMotion.cancel(victim);}
        holdEffect(r,45);
    }
    private static void holdEffect(Rain r,int life){Network.send(r.level,new Network.Effect(NetBranchRules.FX_HOLD,r.target.getId(),r.anchor,Vec3.ZERO,1,life,r.id));}
    private static void releaseRain(Rain r){rains.remove(r.target.getUUID(),r);holdEffect(r,0);r.target.setDeltaMovement(Vec3.ZERO);r.target.fallDistance=0;}
    private static void holdRain(Rain r){LivingEntity t=r.target;t.stopRiding();t.stopUsingItem();t.setDeltaMovement(Vec3.ZERO);t.fallDistance=0;t.hurtMarked=true;if(t instanceof Mob m)m.getNavigation().stop();if(t instanceof ServerPlayer sp)sp.connection.teleport(r.anchor.x,r.anchor.y,r.anchor.z,t.getYRot(),t.getXRot());else t.teleportTo(r.anchor.x,r.anchor.y,r.anchor.z);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void controlledTick(LivingEvent.LivingTickEvent e){Rain r=rains.get(e.getEntity().getUUID());if(r!=null&&!e.getEntity().level().isClientSide){if(!valid(r.p,r.level)||r.target.level()!=r.level||!r.target.isAlive()||!CombatSettings.canHit(r.p,r.target)||DomainClash.active(r.p)||DomainClash.active(r.target)){releaseRain(r);return;}holdRain(r);if(!(e.getEntity() instanceof net.minecraft.world.entity.player.Player))e.setCanceled(true);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(LivingAttackEvent e){
        if(e.getSource().getEntity() instanceof LivingEntity attacker&&controlled(attacker)){e.setCanceled(true);return;}
        if(DismantleMotion.active(e.getEntity())&&!e.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)&&!e.getSource().is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL))e.setCanceled(true);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent e){if(e.isCancelable()&&controlled(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent public static void breakSpeed(PlayerEvent.BreakSpeed e){if(controlled(e.getEntity()))e.setCanceled(true);}
    private static void schedule(ServerPlayer p,Vec3 from,Vec3 dir,UUID target,double width,int delay){if(scheduled.size()<2048)scheduled.add(new Scheduled(p,p.serverLevel(),p.level().getGameTime()+delay,from,dir.normalize(),target,width));}
    private static void add(Projectile b){if(projectiles.size()<2048)projectiles.add(b);}
    public static boolean startLinger(ServerPlayer p,int skill){
        if(!allowed(p)||(skill==CombatExpansionRules.CLEAVE_LINGER&&Curse.fingers(p)<5)||projectiles.size()>=2048||!Curse.spend(p,Curse.COST[0]))return false;
        Projectile b=new Projectile(p,p.getEyePosition().add(p.getLookAngle()),p.getLookAngle(),LINGER,12);add(b);afterCast(p,skill);return true;
    }
    public static void releaseLinger(ServerPlayer p){for(Projectile b:projectiles)if(b.p==p&&b.kind==LINGER&&b.stationary<0)b.stationary=0;}
    public static void cancelHeld(ServerPlayer p){projectiles.removeIf(b->b.p==p&&b.kind==LINGER&&b.stationary<0);stopDragonChant(p);Chant c=chants.get(p.getUUID());if(c!=null&&!c.dragon)chants.remove(p.getUUID());}
    /** 已停住的滞留与任意普通解的飞行线段相交时，由服务端合并并消费来袭斩击。 */
    private static SlashCollision.Point point(Vec3 p){return new SlashCollision.Point(p.x,p.y,p.z);}
    public static boolean collideSlash(ServerPlayer source,Vec3 from,Vec3 to){return collideSlash(source,from,to,source.getLookAngle());}
    public static boolean collideSlash(ServerPlayer source,Vec3 from,Vec3 to,Vec3 direction){
        for(Projectile b:List.copyOf(projectiles))if(b.kind==LINGER&&b.stationary>=0&&b.level==source.level()&&valid(b.p,b.level)){
            Vec3 axis=NetBranches.side(b.dir).scale(b.width/2);
            if(SlashCollision.distanceSquared(point(b.pos.subtract(axis)),point(b.pos.add(axis)),point(from),point(to))>.64)continue;
            b.kind=GIANT;b.stationary=-1;b.age=0;b.width=24;b.dir=direction.normalize();return true;
        }return false;
    }
    private static void multiHit(ServerPlayer p,LivingEntity t,int count,double fraction){
        if(fraction==.5)SlashForms.exactHit(p,t,fraction);else SlashForms.hit(p,t,fraction);
        if(count>1&&valid(p,p.serverLevel())&&t.isAlive()&&cutSeries.size()<4096)cutSeries.add(new CutSeries(p,p.serverLevel(),t.getUUID(),count-1,p.level().getGameTime()+1,fraction));
    }
    private static void tickProjectile(Projectile b){
        if(!valid(b.p,b.level)||DomainClash.active(b.p)){projectiles.remove(b);return;}
        if(b.kind==LINGER&&b.stationary<0&&!TechniqueInput.holding(b.p,CombatExpansionRules.LINGER)&&!TechniqueInput.holding(b.p,CombatExpansionRules.CLEAVE_LINGER)){projectiles.remove(b);return;}
        if(b.kind==LINGER&&b.stationary>=0&&++b.stationary>CombatExpansionRules.LINGER_LIFE||b.kind!=LINGER&&b.age++>200||b.kind==LINGER&&b.stationary<0&&b.age++>1200){projectiles.remove(b);return;}
        if(b.kind==DRAGON&&b.rise>0){
            b.pos=b.pos.add(0,.3,0);fx(b.p,CombatExpansionRules.FX_DRAGON,b.pos,new Vec3(0,1,0),1,3,b.age);
            if(++b.rise>10){split(b);projectiles.remove(b);}return;
        }
        LivingEntity target=b.target!=null&&b.level.getEntity(b.target) instanceof LivingEntity t&&CombatSettings.canHit(b.p,t)&&!b.hit.contains(t.getUUID())?t:null;
        if((b.kind==HUNT&&b.target!=null||b.kind==METEOR||b.kind==DRAGON)&&target==null&&b.age%4==0){target=Skills.crosshairOrder(b.p,near(b.p,b.pos,b.kind==DRAGON?128:64)).stream().filter(t->!b.hit.contains(t.getUUID())).findFirst().orElse(null);b.target=target==null?null:target.getUUID();}
        if(target!=null)b.dir=b.dir.lerp(target.getBoundingBox().getCenter().subtract(b.pos).normalize(),b.kind==DRAGON?.18:.3).normalize();
        if(b.kind==LINGER&&b.stationary<0)b.dir=b.p.getLookAngle().normalize();
        double speed=b.kind==LINGER?(b.stationary<0?.1:0):b.kind==DRAGON?1.6:b.kind==METEOR?1.8:3.25;
        Vec3 next=b.pos.add(b.dir.scale(speed));
        if(b.kind!=LINGER&&b.kind!=DRAGON&&collideSlash(b.p,b.pos,next,b.dir)){projectiles.remove(b);return;}
        if(b.kind==DRAGON){
            fx(b.p,CombatExpansionRules.FX_DRAGON,next,b.dir,1,3,b.age);
            dragonDamage(b.p,b.level,b.pos,next,b.dir,b.age,b.dragonHits,.5);b.hit.addAll(b.dragonHits.keySet());
            if(b.age>=160||b.hit.size()>=10)b.rise=1;b.pos=next;return;
        }
        else {Vec3 axis=NetBranches.side(b.dir);line(b.p,next.subtract(axis.scale(b.width/2)),next.add(axis.scale(b.width/2)),3,b.kind==METEOR?2:b.kind==GIANT?1:0);}
        double radius=b.kind==DRAGON?2:b.kind==METEOR?.65:b.width/2;
        for(LivingEntity t:b.level.getEntitiesOfClass(LivingEntity.class,new AABB(b.pos,next).inflate(radius),t->CombatSettings.canHit(b.p,t)&&!DomainClash.active(t))){
            if(b.hit.contains(t.getUUID()))continue;
            Vec3 axis=NetBranches.side(b.dir);boolean hit=false;
            for(int i=0;i<=8;i++){Vec3 c=b.pos.lerp(next,i/8.0);AABB box=t.getBoundingBox().inflate(b.kind==DRAGON?1.2:.35);if(box.contains(c)||box.clip(c.subtract(axis.scale(radius)),c.add(axis.scale(radius))).isPresent()){hit=true;break;}}
            if(!hit)continue;b.hit.add(t.getUUID());multiHit(b.p,t,b.kind==LINGER?10:b.kind==GIANT?20:b.kind==DRAGON?3:1,b.kind==DRAGON||b.kind==METEOR?.5:1);
            if(!projectiles.contains(b))return;
            if(b.kind==LINGER||b.kind==METEOR||b.kind==HUNT&&b.target!=null){projectiles.remove(b);return;}
            if(b.kind==DRAGON&&b.hit.size()>=10){b.rise=1;break;}
        }
        if(b.kind==DRAGON&&b.age>=160)b.rise=1;
        if(b.kind==GIANT)SkillDestruction.line(b.p,b.pos,next,2);
        b.pos=next;
    }
    static void dragonDamage(ServerPlayer p,ServerLevel level,Vec3 previous,Vec3 next,Vec3 direction,int age,Map<UUID,BitSet> memory,double fraction){dragonDamage(p,level,previous,next,direction,age,memory,fraction,t->true);}
    static void dragonDamage(ServerPlayer p,ServerLevel level,Vec3 previous,Vec3 next,Vec3 direction,int age,Map<UUID,BitSet> memory,double fraction,java.util.function.Predicate<LivingEntity> eligible){
        List<LivingEntity> targets=level.getEntitiesOfClass(LivingEntity.class,new AABB(previous,next).inflate(15),t->CombatSettings.canHit(p,t)&&!DomainClash.active(t)&&eligible.test(t));
        int steps=Math.max(1,(int)Math.ceil(previous.distanceTo(next)/.4));
        for(int sample=0;sample<=steps;sample++){
            List<DragonGeometry.Cut> cuts=DragonGeometry.cuts(previous.lerp(next,sample/(double)steps),direction,age);
            for(LivingEntity t:targets){BitSet seen=memory.computeIfAbsent(t.getUUID(),id->new BitSet(cuts.size()));
                for(int i=0;i<cuts.size()&&t.isAlive();i++){if(seen.get(i))continue;DragonGeometry.Cut cut=cuts.get(i);AABB box=t.getBoundingBox().inflate(.3);
                    if(box.contains(cut.a())||box.clip(cut.a(),cut.b()).isPresent()){seen.set(i);SlashForms.exactHit(p,t,fraction);}
                }
                if(seen.isEmpty())memory.remove(t.getUUID());
            }
        }
    }
    static void starDamage(ServerPlayer p,Vec3 at,double fraction){for(LivingEntity t:Skills.targets(p.serverLevel(),p,at,3))for(DragonGeometry.Cut cut:DragonGeometry.star(at)){AABB box=t.getBoundingBox().inflate(.3);if(box.contains(cut.a())||box.clip(cut.a(),cut.b()).isPresent())SlashForms.exactHit(p,t,fraction);}}
    private static void split(Projectile b){
        for(int i=0;i<CombatExpansionRules.STARS;i++){double a=i*Math.PI/3;Vec3 at=b.pos.add(Math.cos(a)*4,Math.sin(a)*3,0);fx(b.p,CombatExpansionRules.FX_STAR,at,Vec3.ZERO,1,20,i);starDamage(b.p,at,.5);if(stars.size()<256)stars.add(new Burst(b.p,b.level,at,b.level.getGameTime()+16));}
    }
    public static void worldChant(ServerPlayer p,TechniqueInput.Input input){
        if(!allowed(p)||!Curse.awakened(p)){CastFeedback.tell(p,"世界斩需要融合即身佛肉体，且术式可用");return;}
        if(chants.containsKey(p.getUUID())||CastFeedback.cooldown(p,"FormCooldown"+SlashFormRules.WORLD,"世界斩"))return;
        if(!Curse.canSpend(p,2000)){CastFeedback.unavailable(p,2000);return;}
        chants.put(p.getUUID(),new Chant(p,false,input));
    }
    public static void dragonPress(ServerPlayer p){
        if(!allowed(p)||!Curse.awakened(p)){CastFeedback.tell(p,"龙之昴星需要先融合即身佛肉体");return;}
        if(chants.containsKey(p.getUUID()))return;
        long now=p.level().getGameTime();Star s=drawing.get(p.getUUID());
        if(s==null||s.level!=p.level()||CombatExpansionRules.strokeExpired(now,s.last)){s=new Star(p);drawing.put(p.getUUID(),s);}
        if(s.strokes>=6)return;
        if(!Curse.spend(p,Curse.COST[0]))return;s.last=now;
        Vec3[] edge=starEdge(s,s.strokes++);line(p,edge[0],edge[1],10,2);
        for(LivingEntity t:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(edge[0],edge[1]).inflate(1),t->CombatSettings.canHit(p,t)))if(t.getBoundingBox().inflate(.4).clip(edge[0],edge[1]).isPresent())SlashForms.exactHit(p,t,.5);
        CastFeedback.tell(p,"龙之昴星 · "+s.strokes+"/6"+(s.strokes==6?" · 继续按住咏唱":" · 半秒内接下一笔"));
    }
    private static Vec3[] starEdge(Star s,int stroke){int[][] edges={{0,2},{2,4},{4,0},{1,3},{3,5},{5,1}};Vec3[] result=new Vec3[2];for(int i=0;i<2;i++){double a=-Math.PI/2+edges[stroke][i]*Math.PI/3;result[i]=s.center.add(s.side.scale(Math.cos(a)*4)).add(s.up.scale(Math.sin(a)*4));}return result;}
    public static void dragonHold(ServerPlayer p){Star s=drawing.get(p.getUUID());if(s!=null&&s.strokes==6&&!chants.containsKey(p.getUUID())&&allowed(p))chants.put(p.getUUID(),new Chant(p,true,TechniqueInput.intent(p)));}
    public static void stopDragonChant(ServerPlayer p){Chant c=chants.get(p.getUUID());if(c!=null&&c.dragon){chants.remove(p.getUUID());drawing.remove(p.getUUID());}}
    private static void announce(ServerPlayer p,String word){
        Component message=Component.literal(word);for(ServerPlayer listener:p.server.getPlayerList().getPlayers()){
            if(TechniqueConfig.chat())listener.sendSystemMessage(Component.literal("["+MaidBridge.body(p).getName().getString()+"] "+word));
            else {listener.connection.send(new ClientboundSetTitlesAnimationPacket(2,TechniqueConfig.gap()-4,2));listener.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(MaidBridge.body(p).getName().getString()+" · 咏唱")));listener.connection.send(new ClientboundSetTitleTextPacket(message));}
        }
    }
    public static void purple(ServerPlayer p){
        if(!allowed(p)||!VoidJourney.evolved(p)){CastFeedback.tell(p,"灶·开需要解锁虚空御厨子阶段");return;}
        if(CastFeedback.cooldown(p,"PurpleFurnaceCooldown","灶·开")||!Curse.spend(p,Curse.COST[3]))return;
        Curse.status(p,"PurpleFurnaceCooldown",60);Skills.purpleBow(p);
    }
    public static void toggleStream(ServerPlayer p){
        if(!allowed(p))return;
        int erased=SkillDestruction.flushFluids(p,STREAM_RADIUS);
        Curse.sync(p);
        CastFeedback.tell(p,"断流 · 一刀斩灭周围 "+STREAM_RADIUS+" 格内流体 "+erased+" 格（不破坏方块，无需开启破坏方块）");
    }
    public static boolean streaming(ServerPlayer p){return false;}
    public static int lingerCount(ServerPlayer p){return (int)projectiles.stream().filter(b->b.p==p&&b.level==p.level()&&b.kind==LINGER&&b.stationary>=0&&b.stationary<CombatExpansionRules.LINGER_LIFE).count();}
    public static void consumeLingers(ServerPlayer p){projectiles.removeIf(b->b.p==p&&b.kind==LINGER&&b.stationary>=0);}
    private static void stream(ServerPlayer p){
        // 断流已改为"按下即一次性斩完"，这里保留空实现只为兼容旧调用点。
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        for(AutoCast a:List.copyOf(automatic.values())){
            if(!allowed(a.p)||a.p.level()!=a.level){automatic.remove(a.p.getUUID());TechniqueInput.cancel(a.p);continue;}
            int age=(int)(a.level.getGameTime()-a.start);TechniqueInput.receive(a.p,new TechniqueInput.Input(a.skill,1,false,false,false));
            if(a.skill==CombatExpansionRules.DRAGON_STAR&&age>0&&age<=40&&age%8==0){TechniqueInput.receive(a.p,new TechniqueInput.Input(a.skill,2,false,false,false));TechniqueInput.receive(a.p,new TechniqueInput.Input(a.skill,0,false,false,false));}
            if(a.skill!=CombatExpansionRules.DRAGON_STAR&&age>=30||age>50+TechniqueConfig.gap()*4){TechniqueInput.receive(a.p,new TechniqueInput.Input(a.skill,2,false,false,false));automatic.remove(a.p.getUUID());}
        }
        for(Fall f:List.copyOf(falls.values())){
            if(!valid(f.p,f.level)||f.body.level()!=f.level||f.level.getGameTime()>f.until){falls.remove(f.body.getUUID());continue;}
            f.body.fallDistance=0;if(f.body.onGround())landed(f);
            else if(f.body.isInWater()||f.body.isInLava()||f.body.isPassenger()||f.body.isFallFlying())falls.remove(f.body.getUUID());
        }
        for(Scheduled s:List.copyOf(scheduled)){
            if(!valid(s.p,s.level)||DomainClash.active(s.p)){scheduled.remove(s);continue;}if(s.level.getGameTime()<s.due)continue;scheduled.remove(s);
            if(s.target==null)SlashForms.directed(s.p,s.from,s.dir,s.width);
            else {Projectile b=new Projectile(s.p,s.from,s.dir,HUNT,s.width);b.target=s.target;add(b);}
        }
        for(Rain r:List.copyOf(rains.values())){
            if(!valid(r.p,r.level)||!r.target.isAlive()||r.target.level()!=r.level||!CombatSettings.canHit(r.p,r.target)||DomainClash.active(r.p)||DomainClash.active(r.target)){releaseRain(r);continue;}
            int age=(int)(r.level.getGameTime()-r.start);holdRain(r);
            if(age>=1&&age<=CombatExpansionRules.RAIN_CUTS*4-3&&age%4==1){Vec3 center=r.target.getBoundingBox().getCenter();fx(r.p,CombatExpansionRules.FX_SLASH,center.add((age%8==1?1:-1)*.5,8,0),new Vec3(0,-8,0),1,4,3);}
            if(age>=4&&age<=CombatExpansionRules.RAIN_CUTS*4&&age%4==0)SlashForms.hit(r.p,r.target,1);
            if(age>=CombatExpansionRules.RAIN_CUTS*4)releaseRain(r);
        }
        for(Chant c:List.copyOf(chants.values())){
            if(!allowed(c.p)||c.p.level()!=c.level||!c.dragon&&!MaidBridge.isCaster(c.p)&&c.level.getGameTime()-c.heartbeat>25||c.dragon&&!TechniqueInput.holding(c.p,CombatExpansionRules.DRAGON_STAR)){chants.remove(c.p.getUUID());drawing.remove(c.p.getUUID());continue;}
            int age=(int)(c.level.getGameTime()-c.start),gap=TechniqueConfig.gap();String[] words=c.dragon?new String[]{"龍（リュウ）","昴（スバル）","三千（サンゼン）","斬（ザン）"}:new String[]{"龙鳞","反发","成双之流星"};
            if(c.word<words.length&&age>=c.word*gap)announce(c.p,words[c.word++]);
            if(age<words.length*gap)continue;chants.remove(c.p.getUUID());drawing.remove(c.p.getUUID());
            if(c.dragon){if(!Curse.spend(c.p,2000))continue;Projectile b=new Projectile(c.p,c.p.getEyePosition(),c.p.getLookAngle(),DRAGON,4);add(b);Skills.closeDomain(c.p);Curse.enterBurnout(c.p,CombatExpansionRules.DRAGON_BURNOUT);}
            else if(Curse.spend(c.p,2000)){Curse.status(c.p,"FormCooldown"+SlashFormRules.WORLD,CombatExpansionRules.WORLD_COOLDOWN);SlashForms.world(c.p);afterWorld(c);}
        }
        for(Star s:List.copyOf(drawing.values())){
            if(!valid(s.p,s.level)||!allowed(s.p)||!chants.containsKey(s.p.getUUID())&&CombatExpansionRules.strokeExpired(s.level.getGameTime(),s.last)){drawing.remove(s.p.getUUID());continue;}
            if(s.level.getGameTime()%3==0)for(int i=0;i<s.strokes;i++){Vec3[] edge=starEdge(s,i);line(s.p,edge[0],edge[1],3,2);}
        }
        for(CutSeries cuts:List.copyOf(cutSeries)){
            if(!valid(cuts.p,cuts.level)||DomainClash.active(cuts.p)){cutSeries.remove(cuts);continue;}
            if(cuts.level.getGameTime()<cuts.due)continue;cutSeries.remove(cuts);
            if(cuts.level.getEntity(cuts.target) instanceof LivingEntity t&&CombatSettings.canHit(cuts.p,t)&&!DomainClash.active(t)){
                Vec3 axis=SlashReach.axis(cuts.p.getLookAngle(),165+cuts.p.getRandom().nextInt(31)).scale(2),center=t.getBoundingBox().getCenter();line(cuts.p,center.subtract(axis),center.add(axis),4,0);SlashForms.hit(cuts.p,t,cuts.fraction);
                if(cuts.left>1&&valid(cuts.p,cuts.level)&&t.isAlive())cutSeries.add(new CutSeries(cuts.p,cuts.level,cuts.target,cuts.left-1,cuts.level.getGameTime()+1,cuts.fraction));
            }
        }
        for(Projectile b:List.copyOf(projectiles))if(projectiles.contains(b))tickProjectile(b);
        for(Burst star:List.copyOf(stars)){
            if(!valid(star.p,star.level)){stars.remove(star);continue;}if(star.level.getGameTime()<star.due)continue;stars.remove(star);
            List<LivingEntity> targets=Skills.crosshairOrder(star.p,near(star.p,star.center,96));
            for(int i=0;i<CombatExpansionRules.METEORS_PER_STAR;i++){double a=i*2.399963,y=1-2*(i+.5)/20.0,r=Math.sqrt(1-y*y);Projectile b=new Projectile(star.p,star.center,new Vec3(Math.cos(a)*r,y,Math.sin(a)*r),METEOR,1.3);if(!targets.isEmpty())b.target=targets.get(i%targets.size()).getUUID();add(b);}
        }
    }
    private static void afterWorld(Chant c){TechniqueInput.withIntent(c.p,c.input,()->afterCast(c.p,SlashFormRules.WORLD));}
    public static void protect(ServerPlayer owner,UUID target){Rain r=rains.get(target);if(r!=null&&r.p==owner)releaseRain(r);}
    public static void cancel(ServerPlayer p){
        automatic.remove(p.getUUID());TechniqueInput.cancel(p);cutSeries.removeIf(c->c.p==p);projectiles.removeIf(b->b.p==p);scheduled.removeIf(s->s.p==p);stars.removeIf(s->s.p==p);falls.values().removeIf(f->f.p==p);chants.remove(p.getUUID());drawing.remove(p.getUUID());
        for(Rain r:List.copyOf(rains.values()))if(r.p==p||r.target==MaidBridge.body(p))releaseRain(r);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){for(Rain r:List.copyOf(rains.values()))releaseRain(r);projectiles.clear();cutSeries.clear();scheduled.clear();stars.clear();falls.clear();chants.clear();drawing.clear();automatic.clear();TechniqueInput.clear();}
    private CombatExpansion(){}
}
