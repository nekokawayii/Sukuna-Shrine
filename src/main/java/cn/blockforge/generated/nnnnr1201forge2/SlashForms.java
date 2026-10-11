package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 所有飞行线段共用扫掠命中，不追踪已发射的方向。 */
public final class SlashForms {
    public record Line(Vec3 a,Vec3 b){}
    private static final class Blade {
        final ServerPlayer owner;final ServerLevel level;final List<Line> lines;final Vec3 dir;final double fraction;
        final long due;final boolean accelerating,world;final Set<UUID> hit=new HashSet<>();int age;
        Blade(ServerPlayer p,List<Line> lines,Vec3 dir,double fraction,long due,boolean accelerating,boolean world){owner=p;level=p.serverLevel();this.lines=lines;this.dir=dir;this.fraction=fraction;this.due=due;this.accelerating=accelerating;this.world=world;}
    }
    private static final List<Blade> blades=new ArrayList<>();
    private record Pending(ServerPlayer owner,ServerLevel level,Line line,Vec3 dir,long due,boolean moving){}
    private static final List<Pending> pending=new ArrayList<>();
    public static boolean qi(ServerPlayer p){return Curse.data(p).getBoolean("DismantleQi");}
    public static void toggle(ServerPlayer p){if(!Journey.unlocked(p))return;Curse.data(p).putBoolean("DismantleQi",!qi(p));Curse.sync(p);CastFeedback.tell(p,"解已切换为"+(qi(p)?"剑气模式 · 65格/秒":"斩击模式"));}
    private static boolean valid(ServerPlayer p,ServerLevel l){return p.isAlive()&&!p.isRemoved()&&!p.isSpectator()&&p.level()==l&&!EternalLove.settling(MaidBridge.body(p));}
    public static boolean available(ServerPlayer p,int skill){
        return (skill==SlashFormRules.SHAPE||skill==SlashFormRules.CROSS_FLIGHT||skill==SlashFormRules.WORLD)
            &&Journey.unlocked(p)&&valid(p,p.serverLevel())&&Curse.canUseTechnique(p)&&!SoulCombat.suppressed(p)&&!MaidBridge.recovering(p)
            &&(skill!=SlashFormRules.WORLD||Curse.awakened(p))&&(skill!=SlashFormRules.CROSS_FLIGHT||Curse.fingers(p)>=5)
            &&!Curse.active(p,"FormCooldown"+skill)&&Curse.canSpend(p,skill==SlashFormRules.WORLD?2000:skill==SlashFormRules.SHAPE?120:180)
            &&blades.size()+pending.size()<=1800;
    }
    private static boolean ready(ServerPlayer p,int skill,int cost){
        if(CastFeedback.cooldown(p,"FormCooldown"+skill,SlashFormRules.name(skill)))return false;
        if(!Journey.unlocked(p)||(skill==SlashFormRules.WORLD&&!Curse.awakened(p))||(skill==SlashFormRules.CROSS_FLIGHT&&Curse.fingers(p)<5)){CastFeedback.tell(p,"该分支尚未解锁");return false;}
        if(!valid(p,p.serverLevel())||!Curse.canUseTechnique(p)||SoulCombat.suppressed(p)||MaidBridge.recovering(p)){CastFeedback.unavailable(p,cost);return false;}
        if(blades.size()+pending.size()>1800){CastFeedback.tell(p,"同时存在的斩击过多，请稍后释放");return false;}
        return Curse.spend(p,cost);
    }
    public static void cast(ServerPlayer p,int skill){
        if(skill==SlashFormRules.WORLD){CombatExpansion.worldChant(p,TechniqueInput.intent(p));return;}
        int cost=skill==SlashFormRules.WORLD?2000:skill==SlashFormRules.SHAPE?120:180;
        if(!ready(p,skill,cost))return;
        Curse.status(p,"FormCooldown"+skill,skill==SlashFormRules.WORLD?60:skill==SlashFormRules.SHAPE?100:160);
        CombatExpansion.afterCast(p,skill);
        Vec3 dir=p.getLookAngle().normalize();LivingEntity aimed=Skills.aimTarget(p);
        if(qi(p)&&aimed!=null)dir=aimed.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize();
        Vec3 side=NetBranches.side(dir),up=side.cross(dir).normalize();long now=p.level().getGameTime();
        if(skill==SlashFormRules.SHAPE){
            LivingEntity target=aimed;Vec3 center=qi(p)?p.getEyePosition().add(dir.scale(1.5)):target==null?Skills.aimPoint(p,32):target.getBoundingBox().getCenter();int i=0;
            // 整个图案随机倾斜，保留各边相接；每条边都从自己的起点依次出发。
            double tilt=DismantleMotionRules.bladeAngle(p.getRandom().nextDouble());
            for(var s:SlashFormRules.shape(p.getRandom().nextInt(7))){Line line=local(center,side,up,SlashFormRules.rotate(s,tilt-180),1);pending.add(new Pending(p,p.serverLevel(),line,dir,now+DismantleMotionRules.bladeDelay(i++),qi(p)));}
        }else if(skill==SlashFormRules.CROSS_FLIGHT){
            LivingEntity target=Skills.aimTarget(p);Vec3 aim=target==null?p.getEyePosition().add(dir.scale(48)):target.getBoundingBox().getCenter();int count=SlashFormRules.crosses(SukunaBranches.stage(p));
            for(int i=0;i<count;i++){double angle=i*2.39996323,r=1+Math.sqrt(i)*0.65;Vec3 center=p.getEyePosition().subtract(dir.scale(3+i%3*0.6)).add(side.scale(Math.cos(angle)*r)).add(up.scale(Math.sin(angle)*r));Vec3 flight=aim.subtract(center).normalize(),x=NetBranches.side(flight),y=x.cross(flight).normalize();
                // 一个十字由两道独立的解组成，各自40%，同一横/竖只命中一次。
                add(p,List.of(new Line(center.subtract(x.scale(1.8)),center.add(x.scale(1.8)))),flight,1,now,true,false);
                add(p,List.of(new Line(center.subtract(y.scale(1.8)),center.add(y.scale(1.8)))),flight,1,now,true,false);
            }
        }
        SkillSounds.cast(p,0);
    }
    private static Line local(Vec3 center,Vec3 side,Vec3 up,SlashFormRules.Segment s,double scale){return new Line(center.add(side.scale(s.x1()*scale)).subtract(up.scale(s.y1()*scale)),center.add(side.scale(s.x2()*scale)).subtract(up.scale(s.y2()*scale)));}
    public static void basic(ServerPlayer p){
        Vec3 dir=p.getLookAngle().normalize();LivingEntity target=Skills.aimTarget(p);if(target!=null)dir=target.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize();
        int count=TechniqueInput.holding(p,0)?1:Curse.tier(p)>=3?3:Curse.tier(p)>=1?2:1;
        for(int i=0;i<count;i++){Vec3 axis=SlashReach.axis(dir,p.getRandom().nextInt(360)),c=p.getEyePosition().add(dir.scale(1.2));add(p,List.of(new Line(c.subtract(axis.scale(6)),c.add(axis.scale(6)))),dir,1,p.level().getGameTime()+i*SlashFormRules.GAP,false,false);}
    }
    public static void directed(ServerPlayer p,Vec3 center,Vec3 direction,double length){
        Vec3 dir=direction.normalize(),axis=length>=80?NetBranches.side(dir):SlashReach.axis(dir,(long)DismantleMotionRules.bladeAngle(p.getRandom().nextDouble()));
        add(p,List.of(new Line(center.subtract(axis.scale(length/2)),center.add(axis.scale(length/2)))),dir,1,p.level().getGameTime(),false,false);
    }
    /** 每次只放一道；动作时间轴每隔两刻调用，角度独立随机。 */
    public static void motionBlade(ServerPlayer p,Vec3 forward){
        Vec3 side=NetBranches.side(forward),up=new Vec3(0,1,0),center=p.position().add(0,Math.min(1.1,MaidBridge.body(p).getBbHeight()*0.65),0).add(forward.scale(1.2));
        double angle=Math.toRadians(DismantleMotionRules.bladeAngle(p.getRandom().nextDouble()));Vec3 axis=side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
        add(p,List.of(new Line(center.subtract(axis.scale(6)),center.add(axis.scale(6)))),forward,1,p.level().getGameTime(),false,false);
    }
    private static final class Ring {
        final ServerPlayer owner;final ServerLevel level;final Vec3 center;final Set<UUID> hit=new HashSet<>();int age;
        Ring(ServerPlayer p,Vec3 center){owner=p;level=p.serverLevel();this.center=center;}
    }
    private static final List<Ring> rings=new ArrayList<>();
    public static void motionRing(ServerPlayer p,Vec3 center){
        if(rings.size()>=128)return;rings.add(new Ring(p,center));
        Network.send(p.serverLevel(),new Network.Effect(DismantleMotionRules.RING_FX,p.getId(),center,Vec3.ZERO,1,DismantleMotionRules.RING_LIFE,0));
    }
    private static Line ringLine(Vec3 center,double radius,int index){double a=index*Math.PI*2/DismantleMotionRules.RING_SEGMENTS,z=(index+1)*Math.PI*2/DismantleMotionRules.RING_SEGMENTS;return new Line(center.add(Math.cos(a)*radius,0,Math.sin(a)*radius),center.add(Math.cos(z)*radius,0,Math.sin(z)*radius));}
    private static void tickRings(){
        for(Ring r:List.copyOf(rings)){
            if(!valid(r.owner,r.level)||DomainClash.active(r.owner)||r.age>=DismantleMotionRules.RING_LIFE){rings.remove(r);continue;}
            double from=DismantleMotionRules.ringRadius(r.age),to=DismantleMotionRules.ringRadius(r.age+1);
            var targets=r.level.getEntitiesOfClass(LivingEntity.class,new AABB(r.center.x-to-1,r.center.y-1,r.center.z-to-1,r.center.x+to+1,r.center.y+1,r.center.z+to+1),t->CombatSettings.canHit(r.owner,t));
            for(int step=0;step<=9;step++)for(int i=0;i<DismantleMotionRules.RING_SEGMENTS;i++){
                Line line=ringLine(r.center,from+(to-from)*step/9.0,i);
                for(LivingEntity t:targets)if(!r.hit.contains(t.getUUID())&&intersects(line,Vec3.ZERO,Vec3.ZERO,t,false)){r.hit.add(t.getUUID());hit(r.owner,t,1);}
                SkillDestruction.immediate(r.owner,line.a,line.b,0.45);
            }
            r.age++;
        }
    }
    public static void world(ServerPlayer p){
        Vec3 dir=p.getLookAngle().normalize();LivingEntity target=Skills.aimTarget(p);if(target!=null)dir=target.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize();
        Vec3 axis=SlashReach.axis(dir,p.getRandom().nextInt(360)),center=p.getEyePosition().add(dir.scale(1.5));
        add(p,List.of(new Line(center.subtract(axis),center.add(axis))),dir,1,p.level().getGameTime(),false,true);SkillSounds.play(p,"space_slash");
    }
    public static void freehand(ServerPlayer p,List<SlashFormRules.Segment> segments,int height,double pixels){
        Vec3 dir=p.getLookAngle().normalize(),side=NetBranches.side(dir),up=side.cross(dir).normalize(),center=p.getEyePosition().add(dir.scale(1.5));
        // 按画布高度映射到前方12格高平面；伤害和咒力仍按实际屏幕像素。
        List<Line> lines=SlashFormRules.centered(segments).stream().map(s->local(center,side,up,s,12.0/height)).toList();
        if(!lines.isEmpty())add(p,lines,dir,SlashFormRules.freehandDamage(pixels),p.level().getGameTime(),false,false);
        CombatExpansion.afterCast(p,SlashFormRules.FREEHAND);SkillSounds.cast(p,0);
    }
    private static void add(ServerPlayer p,List<Line> lines,Vec3 dir,double fraction,long due,boolean accelerating,boolean world){if(blades.size()<2048)blades.add(new Blade(p,List.copyOf(lines),dir,fraction,due,accelerating,world));}
    private static void effect(Blade b){for(Line l:b.lines)Network.send(b.level,new Network.Effect(b.world?SlashFormRules.WORLD_FX:b.accelerating?SlashFormRules.ACCELERATING:SlashFormRules.FLIGHT,b.owner.getId(),l.a,l.b.subtract(l.a),b.world?SlashReach.INFINITE:1,b.world?SlashFormRules.WORLD_LIFE:100,pack(b.dir)));}
    static long pack(Vec3 d){int yaw=(int)Math.round(Math.toDegrees(Math.atan2(d.z,d.x))*100)+18000,pitch=(int)Math.round(Math.asin(d.y)*18000/Math.PI)+9000;return ((long)yaw<<16)|(pitch&65535);}
    public static void exactHit(ServerPlayer p,LivingEntity t,double fraction){
        if(!CombatSettings.canHit(p,t)||DomainClash.active(p)||DomainClash.active(t))return;
        t.invulnerableTime=0;CombatSettings.hurt(p,t,p.damageSources().sonicBoom(p),(float)(NetBranches.dismantleDamage(p,t)*fraction));
    }
    public static void hit(ServerPlayer p,LivingEntity t,double fraction){
        if(!CombatSettings.canHit(p,t)||DomainClash.active(p)||DomainClash.active(t))return;
        exactHit(p,t,fraction);
        if(t.isAlive()&&SoulJourney.evolved(p))SoulCombat.soulHit(p,t);
        if(t.isAlive()&&VoidJourney.evolved(p))VoidCombat.formHit(p,t);
    }
    private static boolean intersects(Line line,Vec3 motion,Vec3 step,LivingEntity t,boolean infinite){
        Vec3 a=line.a.add(motion),b=line.b.add(motion);AABB box=t.getBoundingBox().inflate(infinite?WorldSlashRules.HALF_WIDTH:0.35);
        if(infinite){Vec3 c=a.lerp(b,0.5),axis=b.subtract(a).normalize();double reach=t.getBoundingBox().getCenter().distanceTo(c)+t.getBbHeight()+t.getBbWidth()+4;a=c.subtract(axis.scale(reach));b=c.add(axis.scale(reach));}
        // 每0.4格扫描移动面，65格/秒也不会漏掉细小目标。
        int n=Math.max(1,(int)Math.ceil(step.length()/0.4));for(int i=0;i<=n;i++){Vec3 move=step.scale(i/(double)n),from=a.add(move),to=b.add(move);if(box.contains(from)||box.contains(to)||box.clip(from,to).isPresent())return true;}return false;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        tickRings();
        for(Pending s:List.copyOf(pending)){if(!valid(s.owner,s.level)){pending.remove(s);continue;}if(s.level.getGameTime()<s.due)continue;pending.remove(s);
            if(s.moving)add(s.owner,List.of(s.line),s.dir,1,s.due,false,false);
            else {if(CombatExpansion.collideSlash(s.owner,s.line.a,s.line.b,s.dir))continue;Network.send(s.level,new Network.Effect(SlashFormRules.STILL,s.owner.getId(),s.line.a,s.line.b.subtract(s.line.a),1,24,0));SkillDestruction.immediate(s.owner,s.line.a,s.line.b,0.5);for(LivingEntity t:s.level.getEntitiesOfClass(LivingEntity.class,new AABB(s.line.a,s.line.b).inflate(1),e->CombatSettings.canHit(s.owner,e)))if(intersects(s.line,Vec3.ZERO,Vec3.ZERO,t,false))hit(s.owner,t,1);}
        }
        for(Blade b:List.copyOf(blades)){
            if(!valid(b.owner,b.level)||DomainClash.active(b.owner)||b.age>=(b.world?SlashFormRules.WORLD_LIFE:100)){blades.remove(b);continue;}
            if(b.level.getGameTime()<b.due)continue;if(b.age==0)effect(b);
            Vec3 motion=b.dir.scale(SlashFormRules.travel(b.age,b.accelerating)),step=b.dir.scale(SlashFormRules.travel(b.age+1,b.accelerating)-SlashFormRules.travel(b.age,b.accelerating));
            boolean merged=false;for(Line l:b.lines){for(int sample=0;sample<=8;sample++){Vec3 offset=motion.add(step.scale(sample/8.0));if(CombatExpansion.collideSlash(b.owner,l.a.add(offset),l.b.add(offset),b.dir)){merged=true;break;}}if(merged)break;}
            if(merged){for(Line l:b.lines)Network.send(b.level,new Network.Effect(CombatExpansionRules.FX_BLADE_STOP,b.owner.getId(),l.a,l.b.subtract(l.a),1,1,pack(b.dir)));blades.remove(b);continue;}
            List<LivingEntity> targets=new ArrayList<>();if(b.world){for(var e:b.level.getAllEntities())if(e instanceof LivingEntity t&&CombatSettings.canHit(b.owner,t))targets.add(t);}else{
                AABB bounds=new AABB(b.lines.get(0).a,b.lines.get(0).b);for(Line l:b.lines)bounds=bounds.minmax(new AABB(l.a,l.b));targets=b.level.getEntitiesOfClass(LivingEntity.class,bounds.move(motion).expandTowards(step).inflate(1),t->CombatSettings.canHit(b.owner,t));
            }
            for(LivingEntity t:targets){if(DomainClash.active(t)||b.hit.contains(t.getUUID()))continue;for(Line l:b.lines)if(intersects(l,motion,step,t,b.world)){b.hit.add(t.getUUID());if(b.world){t.invulnerableTime=0;float amount=WorldSlashRules.damage(t.getMaxHealth(),NetBranches.dismantleDamage(b.owner,t));t.getPersistentData().putBoolean("SukunaWorldSlashHit",true);try{CombatSettings.hurt(b.owner,t,b.level.damageSources().sonicBoom(b.owner),amount);}finally{t.getPersistentData().remove("SukunaWorldSlashHit");}}else hit(b.owner,t,b.fraction);break;}}
            for(Line l:b.lines){Vec3 a=l.a.add(motion),z=l.b.add(motion);int n=(int)Math.ceil(step.length()/0.65);for(int i=0;i<=n;i++){Vec3 offset=step.scale(i/(double)Math.max(1,n));if(b.world)SkillDestruction.infinite(b.owner,a.add(offset).lerp(z.add(offset),0.5),z.subtract(a).normalize(),WorldSlashRules.HALF_WIDTH);else SkillDestruction.immediate(b.owner,a.add(offset),z.add(offset),0.45);}}
            b.age++;
        }
    }
    public static void cancel(ServerPlayer p){rings.removeIf(r->r.owner==p);blades.removeIf(b->b.owner==p);pending.removeIf(s->s.owner==p);FreehandDrawing.cancel(p);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){rings.clear();blades.clear();pending.clear();FreehandDrawing.clear();}
    private SlashForms(){}
}
