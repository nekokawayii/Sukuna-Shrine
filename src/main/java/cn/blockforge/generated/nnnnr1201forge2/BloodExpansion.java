package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

/** 有界弹幕队列与逻辑血海：不放置方块、不强载区块，每格最多一次自动索敌。 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="sukuna")
public final class BloodExpansion {
    public static final int FX_OCEAN=470,FX_CELL=471,FX_TRAP=472,FX_NEEDLE=473,FX_METEOR=474,FX_OCEAN_CLOSE=475;
    private static final List<Shot> shots=new ArrayList<>();
    private static final List<Trap> traps=new ArrayList<>();
    private static final List<Ocean> oceans=new ArrayList<>();
    private static final List<Burst> bursts=new ArrayList<>();
    private static final Map<UUID,Slow> slows=new HashMap<>();
    private static final UUID SLOW=UUID.fromString("ae133000-0001-4000-8000-000000000010");
    private static long serial;
    private record Burst(ServerPlayer p,ServerLevel level,Vec3 at,long due,boolean empowered){}
    private record Slow(LivingEntity target,ServerPlayer p,long until){}
    private static final class Volley {final Map<UUID,Integer> hits=new HashMap<>();}
    private static final class Shot {
        final ServerPlayer p;final ServerLevel level;final boolean meteor;final Volley volley;final Vec3 direction;
        Vec3 at;int age,delay;
        Shot(ServerPlayer p,Vec3 at,Vec3 dir,boolean meteor,int delay,Volley volley){this.p=p;level=p.serverLevel();this.at=at;direction=dir;this.meteor=meteor;this.delay=delay;this.volley=volley;}
    }
    private static final class Trap {
        final ServerPlayer p;final ServerLevel level;final Vec3 at;final long end;final Map<UUID,Integer> dwell=new HashMap<>();
        Trap(ServerPlayer p,Vec3 at){this.p=p;level=p.serverLevel();this.at=at;end=level.getGameTime()+1200;}
    }
    private static final class Ocean {
        final ServerPlayer p;final ServerLevel level;final Vec3 center;final long start,id=++serial;int cursor;
        final Map<UUID,Integer> hits=new HashMap<>();
        Ocean(ServerPlayer p,Vec3 center){this.p=p;level=p.serverLevel();this.center=center;start=level.getGameTime();}
    }
    private static boolean valid(ServerPlayer p,ServerLevel level){return p.isAlive()&&!p.isRemoved()&&(p instanceof MaidBridge.Actor||!p.hasDisconnected())&&p.level()==level&&MaidBridge.body(p).level()==level&&!DeathStar.anemic(p)&&Curse.canUseTechnique(p)&&!PrisonSystem.locked(MaidBridge.body(p));}
    private static List<LivingEntity> enemies(ServerPlayer p,Vec3 at,double r){return p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(r),t->CombatSafety.canTarget(p,t)&&t.getBoundingBox().getCenter().distanceToSqr(at)<=r*r);}
    private static Vec3 ground(ServerPlayer p,Vec3 at){if(!p.serverLevel().hasChunkAt(BlockPos.containing(at)))return at;var hit=p.level().clip(new ClipContext(at.add(0,3,0),at.add(0,-12,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,MaidBridge.body(p)));return hit.getType()==HitResult.Type.MISS?at:hit.getLocation().add(0,.025,0);}
    private static boolean visible(ServerPlayer p,Vec3 at,LivingEntity t){return p.level().clip(new ClipContext(at,t.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,MaidBridge.body(p))).getType()==HitResult.Type.MISS;}
    public static void meteors(ServerPlayer p,Vec3 at,Vec3 dir){int count=BloodRules.meteors(BloodArts.rank(p));Volley volley=new Volley();Vec3 side=NetBranches.side(dir);for(int i=0;i<count&&shots.size()<2048;i++)shots.add(new Shot(p,at.add(side.scale(((i%3)-1)*.48)),dir,true,i*2,volley));}
    public static void senbon(ServerPlayer p,Vec3 at,Vec3 dir){int count=BloodRules.needles(BloodArts.rank(p));Volley volley=new Volley();Vec3 side=NetBranches.side(dir),up=dir.cross(side).normalize();for(int i=0;i<count&&shots.size()<2048;i++){double angle=((i+.5)/count-.5)*Math.PI*2/3;Vec3 ray=dir.scale(Math.cos(angle)).add(side.scale(Math.sin(angle))).add(up.scale(Math.sin(i*2.399)*.12)).normalize();shots.add(new Shot(p,at,ray,false,0,volley));}}
    public static void novaEchoes(ServerPlayer p,Vec3 at,boolean empowered){for(int i=0;i<BloodArts.rank(p)&&bursts.size()<256;i++)bursts.add(new Burst(p,p.serverLevel(),at,p.level().getGameTime()+6*(i+1),empowered));}
    public static void trap(ServerPlayer p,Vec3 origin,Vec3 dir){Vec3 end=origin.add(dir.scale(28));var hit=p.level().clip(new ClipContext(origin,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,MaidBridge.body(p)));Vec3 at=ground(p,hit.getType()==HitResult.Type.MISS?end:hit.getLocation().subtract(dir.scale(.08)));long own=traps.stream().filter(t->t.p==p).count();int limit=6+BloodArts.rank(p)*2;if(own>=limit){for(Trap t:List.copyOf(traps))if(t.p==p){traps.remove(t);break;}}if(traps.size()<256)traps.add(new Trap(p,at));BloodArts.fx(p,FX_TRAP,at,Vec3.ZERO,1,35,0);}
    public static boolean canOcean(ServerPlayer p){return oceans.size()<8&&oceans.stream().noneMatch(o->o.p==p);}
    public static void ocean(ServerPlayer p,Vec3 at){if(!canOcean(p))return;Ocean o=new Ocean(p,at);oceans.add(o);sync(o);}
    private static void sync(Ocean o){BloodArts.fx(o.p,FX_OCEAN,o.center,new Vec3(o.start,o.cursor,0),100,500,o.id);}
    private static void slow(ServerPlayer p,LivingEntity t,long until){if(!CombatSafety.canAffect(p,t))return;var a=t.getAttribute(Attributes.MOVEMENT_SPEED);if(a!=null&&a.getModifier(SLOW)==null)a.addTransientModifier(new AttributeModifier(SLOW,"毒血·血洋减速",-.35,AttributeModifier.Operation.MULTIPLY_TOTAL));slows.put(t.getUUID(),new Slow(t,p,until));}
    public static boolean slowed(LivingEntity target){Slow s=slows.get(target.getUUID());return s!=null&&s.until>target.level().getGameTime()&&CombatSafety.canAffect(s.p,target);}
    private static void clearSlow(Slow s){var a=s.target.getAttribute(Attributes.MOVEMENT_SPEED);if(a!=null)a.removeModifier(SLOW);}
    public static void protect(ServerPlayer p,UUID id){Slow s=slows.get(id);if(s!=null&&s.p==p){clearSlow(s);slows.remove(id);}for(Trap t:traps)if(t.p==p)t.dwell.remove(id);}
    public static void exit(ServerPlayer p){shots.removeIf(s->s.p==p);traps.removeIf(t->t.p==p);bursts.removeIf(b->b.p==p);for(Ocean o:List.copyOf(oceans))if(o.p==p){BloodArts.fx(p,FX_OCEAN_CLOSE,o.center,Vec3.ZERO,0,1,o.id);oceans.remove(o);}for(Slow s:List.copyOf(slows.values()))if(s.p==p){clearSlow(s);slows.remove(s.target.getUUID());}}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){if(event.phase!=TickEvent.Phase.END)return;tickShots();tickTraps();tickOceans();for(Burst b:List.copyOf(bursts)){if(!valid(b.p,b.level)){bursts.remove(b);continue;}if(b.level.getGameTime()>=b.due){bursts.remove(b);BloodArts.nova(b.p,b.at,b.empowered);}}for(Slow s:List.copyOf(slows.values()))if(!s.target.isAlive()||!valid(s.p,s.p.serverLevel())||s.target.level().getGameTime()>=s.until||!CombatSafety.canAffect(s.p,s.target)){clearSlow(s);slows.remove(s.target.getUUID());}}
    private static void tickShots(){for(Shot s:List.copyOf(shots)){
        if(!valid(s.p,s.level)||s.age>(s.meteor?24:6+BloodArts.rank(s.p))){shots.remove(s);continue;}if(s.delay-->0)continue;s.age++;
        Vec3 next=s.at.add(s.direction.scale(s.meteor?1.3:2.6));if(!s.level.hasChunkAt(BlockPos.containing(next))){shots.remove(s);continue;}var wall=s.level.clip(new ClipContext(s.at,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,MaidBridge.body(s.p)));boolean blocked=wall.getType()!=HitResult.Type.MISS;if(blocked)next=wall.getLocation();Vec3 end=next;
        var targets=s.level.getEntitiesOfClass(LivingEntity.class,new AABB(s.at,next).inflate(s.meteor?.5:.12),t->CombatSafety.canAffect(s.p,t)&&t.getBoundingBox().inflate(s.meteor?.4:.10).clip(s.at,end).isPresent());targets.sort(Comparator.comparingDouble(t->t.distanceToSqr(s.at)));
        BloodArts.fx(s.p,s.meteor?FX_METEOR:FX_NEEDLE,s.at,next,1,s.meteor?5:4,s.age);
        if(!targets.isEmpty()){LivingEntity t=targets.get(0);int hits=s.volley.hits.getOrDefault(t.getUUID(),0);s.volley.hits.put(t.getUUID(),hits+1);if(BloodArts.hit(s.p,t,(s.meteor?16:2.4f)*(s.meteor?1:BloodRules.needleFalloff(hits)),s.meteor?0:1,s.meteor)){if(s.meteor)BloodArts.fx(s.p,BloodArts.FX+6,t.getBoundingBox().getCenter(),s.direction,1,14,2);else BloodArts.dot(s.p,t,40,true);}shots.remove(s);}else if(blocked)shots.remove(s);else s.at=next;
    }}
    private static void tickTraps(){for(Trap t:List.copyOf(traps)){
        long now=t.level.getGameTime();if(!valid(t.p,t.level)||now>=t.end){traps.remove(t);continue;}
        if(now%25==0)BloodArts.fx(t.p,FX_TRAP,t.at,Vec3.ZERO,1,27,0);
        var touching=enemies(t.p,t.at.add(0,.65,0),1.5);Set<UUID> present=new HashSet<>();boolean trigger=false;
        for(LivingEntity e:touching){if(Math.abs(e.getY()-t.at.y)>1.5)continue;present.add(e.getUUID());int age=t.dwell.merge(e.getUUID(),1,Integer::sum);if(age==1)BloodArts.fx(t.p,FX_TRAP,t.at,Vec3.ZERO,1,40,1);if(age>=BloodRules.TRAP_DWELL)trigger=true;}
        t.dwell.keySet().retainAll(present);if(!trigger)continue;traps.remove(t);int rank=BloodArts.rank(t.p);BloodArts.fx(t.p,BloodArts.FX+8,t.at.add(0,.2,0),Vec3.ZERO,1,28,96+rank*16);
        for(int i=0;i<8+rank*4;i++){double angle=i*Math.PI*2/(8+rank*4);BloodArts.fx(t.p,BloodArts.FX+12,t.at.add(Math.cos(angle)*2,0,Math.sin(angle)*2),Vec3.ZERO,1,22,0);}
        for(LivingEntity e:enemies(t.p,t.at,4+rank))if(visible(t.p,t.at.add(0,.3,0),e)&&BloodArts.hit(t.p,e,38,.5f,false))CombatSafety.push(t.p,e,new Vec3(0,.65,0));
    }}
    private static void tickOceans(){int budget=1024;int share=Math.max(1,budget/Math.max(1,oceans.size()));
        for(Ocean o:List.copyOf(oceans)){
            int age=(int)(o.level.getGameTime()-o.start);if(!valid(o.p,o.level)){exit(o.p);continue;}
            if(age%40==0)sync(o);
            double radius=BloodRules.oceanRadius(age);
            if(age<400){if(age%10==0)for(LivingEntity t:enemies(o.p,o.center.add(0,2,0),radius+4)){double dx=t.getX()-o.center.x,dz=t.getZ()-o.center.z;if(dx*dx+dz*dz<=radius*radius){Vec3 floor=ground(o.p,new Vec3(t.getX(),t.getY(),t.getZ()));if(Math.abs(t.getY()-floor.y)<2.5)slow(o.p,t,o.level.getGameTime()+25);}}continue;}
            // 每格有独立位置与一次索敌机会；无敌人不创建空弹，绝不压成整片一次范围伤害。
            var targets=enemies(o.p,o.center.add(0,2,0),116);Map<Long,List<LivingEntity>> bins=new HashMap<>();for(LivingEntity t:targets){int bx=(int)Math.floor(t.getX()/16),bz=(int)Math.floor(t.getZ()/16);bins.computeIfAbsent(bin(bx,bz),k->new ArrayList<>()).add(t);}
            int local=Math.min(Math.min(256,share),budget);int visual=0;
            while(local-->0&&o.cursor<201*201){budget--;int cell=o.cursor++,dx=cell%201-100,dz=cell/201-100;if(dx*dx+dz*dz>10000)continue;Vec3 planar=o.center.add(dx,0,dz);if(!o.level.hasChunkAt(BlockPos.containing(planar)))continue;
                int bx=(int)Math.floor(planar.x/16),bz=(int)Math.floor(planar.z/16);LivingEntity best=null;double distance=144;
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(LivingEntity t:bins.getOrDefault(bin(bx+x,bz+z),List.of())){if(!CombatSafety.canTarget(o.p,t))continue;double d=t.position().distanceToSqr(planar);if(d<distance){best=t;distance=d;}}
                if(best==null){if(visual++<3)BloodArts.fx(o.p,FX_CELL,ground(o.p,planar),Vec3.ZERO,1,16,cell);continue;}
                Vec3 from=ground(o.p,planar).add(0,.15,0);if(!visible(o.p,from,best))continue;
                int hits=o.hits.merge(best.getUUID(),1,Integer::sum);float amount=8*BloodRules.needleFalloff(hits-1);BloodArts.hit(o.p,best,amount,1,false);
                if(visual++<12)BloodArts.fx(o.p,FX_CELL,from,best.getBoundingBox().getCenter(),1,16,cell);
                // 高阶段每格额外分裂，优先另一个敌人。
                if(BloodArts.rank(o.p)>=2&&cell%4==0)for(LivingEntity extra:bins.getOrDefault(bin(bx,bz),List.of()))if(extra!=best&&CombatSafety.canTarget(o.p,extra)&&extra.distanceToSqr(from)<144&&visible(o.p,from,extra)){int n=o.hits.merge(extra.getUUID(),1,Integer::sum);BloodArts.hit(o.p,extra,4*BloodRules.needleFalloff(n-1),1,false);break;}
            }
            if(o.cursor>=201*201){BloodArts.fx(o.p,FX_OCEAN_CLOSE,o.center,Vec3.ZERO,0,1,o.id);oceans.remove(o);}
            if(budget<=0)break;
        }
    }
    private static long bin(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){for(Slow s:slows.values())clearSlow(s);slows.clear();shots.clear();traps.clear();oceans.clear();bursts.clear();}
    private BloodExpansion(){}
}
