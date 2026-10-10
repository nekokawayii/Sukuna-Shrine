package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;

/** 玩家和女仆共用：服务端锁定、结算、敌友检查；不永久修改AI或重力。 */
public final class NetBranches {
    private static final Map<UUID,Cross> crosses=new HashMap<>();
    private static final List<Qi> projectiles=new ArrayList<>();
    private record AirCross(ServerPlayer p,ServerLevel level,Vec3 center,long born){}
    private static final List<AirCross> airCrosses=new ArrayList<>();
    private static long nextId;
    private static final class Cross {
        final ServerPlayer owner;final ServerLevel level;final LivingEntity target;
        final Vec3 start,anchor;final long born,id;
        Cross(ServerPlayer p,LivingEntity t){
            owner=p;level=p.serverLevel();target=t;start=t.position();
            // 检查完整上浮路径，包含目标自身高度；不足5格仍可释放，在原地控制。
            boolean clearAbove=level.noCollision(t,t.getBoundingBox().expandTowards(0,NetBranchRules.RISE,0));
            anchor=start.add(0,NetBranchRules.crossRise(clearAbove),0);born=level.getGameTime();id=++nextId;
        }
    }
    private static final class Qi {
        final ServerPlayer owner;final ServerLevel level;final Vec3 direction,side;final long id;
        final Set<UUID> hit=new HashSet<>();Vec3 position;int age;
        Qi(ServerPlayer p){owner=p;level=p.serverLevel();position=p.getEyePosition();direction=p.getLookAngle().normalize();side=side(direction);id=++nextId;}
    }
    static Vec3 side(Vec3 direction){Vec3 side=new Vec3(-direction.z,0,direction.x).normalize();return side.lengthSqr()<0.01?new Vec3(1,0,0):side;}
    private static String cooldownKey(int skill){return skill==NetBranchRules.CROSS_SKILL?"NetCrossCooldown":"NetQiCooldown";}
    private static boolean valid(ServerPlayer p,ServerLevel level){return p.isAlive()&&!p.isRemoved()&&p.serverLevel()==level&&MaidBridge.body(p).level()==level&&!EternalLove.settling(MaidBridge.body(p));}
    private static boolean crossValid(Cross c){return valid(c.owner,c.level)&&c.target.isAlive()&&!c.target.isRemoved()&&c.target.level()==c.level&&CombatSettings.canHit(c.owner,c.target)&&!DomainClash.active(c.owner)&&!DomainClash.active(c.target)&&c.level.getGameTime()-c.born<=NetBranchRules.CROSS_TICKS;}
    public static boolean locked(LivingEntity target){if(target instanceof MaidBridge.Actor a)target=a.maid;Cross c=crosses.get(target.getUUID());return c!=null&&c.target==target&&crossValid(c);}
    public static boolean casting(ServerPlayer p){return crosses.values().stream().anyMatch(c->c.owner==p&&crossValid(c));}
    public static boolean available(ServerPlayer p,int skill){return NetBranchRules.branch(skill)&&Curse.awakened(p)&&Journey.unlocked(p)&&valid(p,p.serverLevel())&&!p.isSpectator()&&!MaidBridge.recovering(p)&&!locked(p)&&!casting(p)&&!SoulCombat.suppressed(p)&&Curse.canUseTechnique(p)&&!Curse.active(p,cooldownKey(skill))&&Curse.canSpend(p,NetBranchRules.cost(skill));}
    public static void cast(ServerPlayer p,int skill){
        if(!NetBranchRules.branch(skill))return;
        if(CastFeedback.cooldown(p,cooldownKey(skill),NetBranchRules.name(skill)))return;
        if(!Curse.awakened(p)){p.displayClientMessage(Component.literal("先融合即身佛肉体，才能使用"+NetBranchRules.name(skill)),true);return;}
        if(!available(p,skill)){CastFeedback.unavailable(p,NetBranchRules.cost(skill));return;}
        LivingEntity target=null;
        if(skill==NetBranchRules.CROSS_SKILL){
            target=Skills.aimTarget(p);
            if(target!=null&&(SixfoldFinale.active(target)||SixfoldFinale.controlled(target)||locked(target)||Uraume.locked(target)||DomainClash.active(target)))target=null;
        }
        if(!Curse.spend(p,NetBranchRules.cost(skill)))return;
        Curse.status(p,cooldownKey(skill),NetBranchRules.cooldown(skill));
        CombatExpansion.afterCast(p,skill);
        SkillSounds.cast(p,0);
        if(target!=null){Cross c=new Cross(p,target);crosses.put(target.getUUID(),c);target.stopRiding();target.stopUsingItem();ServerPlayer victim=target instanceof ServerPlayer sp?sp:MaidBridge.caster(target);if(victim!=null)Skills.cancelCharge(victim);hold(c);holdEffect(c,NetBranchRules.CROSS_TICKS);}
        else if(skill==NetBranchRules.CROSS_SKILL){if(airCrosses.size()<128)airCrosses.add(new AirCross(p,p.serverLevel(),Skills.aimPoint(p,24),p.level().getGameTime()));}
        else {Qi qi=new Qi(p);projectiles.add(qi);qiEffect(qi);}
    }
    private static void holdEffect(Cross c,int life){Network.send(c.level,new Network.Effect(NetBranchRules.FX_HOLD,c.target.getId(),c.anchor,Vec3.ZERO,1,life,c.id));}
    private static void hold(Cross c){
        double rise=Math.min(1,(c.level.getGameTime()-c.born)/(double)NetBranchRules.RISE_TICKS);Vec3 point=c.start.lerp(c.anchor,rise);LivingEntity t=c.target;
        t.stopRiding();t.stopUsingItem();t.setDeltaMovement(Vec3.ZERO);t.fallDistance=0;t.hurtMarked=true;
        if(t instanceof Mob mob)mob.getNavigation().stop();
        if(t instanceof ServerPlayer p)p.connection.teleport(point.x,point.y,point.z,p.getYRot(),p.getXRot());else t.teleportTo(point.x,point.y,point.z);
    }
    private static void release(Cross c){release(c,false);}
    private static void release(Cross c,boolean completed){
        c.target.setDeltaMovement(Vec3.ZERO);c.target.fallDistance=0;c.target.hurtMarked=true;holdEffect(c,0);
        // 正常解控后，已命中的十字继续沿对角飞完；中途取消仍立即清除全部特效。
        if(!completed)stop(c.owner,c.level,c.anchor,c.id);
    }
    private static void stop(ServerPlayer p,ServerLevel level,Vec3 at,long id){Network.send(level,new Network.Effect(NetBranchRules.FX_STOP,p.getId(),at,Vec3.ZERO,0,0,id));}
    public static void cancel(ServerPlayer p){airCrosses.removeIf(c->c.p==p);for(Cross c:List.copyOf(crosses.values()))if(c.owner==p||c.target.getUUID().equals(p.getUUID())){crosses.remove(c.target.getUUID(),c);release(c);}for(Qi qi:List.copyOf(projectiles))if(qi.owner==p){projectiles.remove(qi);stop(p,qi.level,qi.position,qi.id);}}
    public static void protect(ServerPlayer p,UUID target){Cross c=crosses.get(target);if(c!=null&&c.owner==p){crosses.remove(target,c);release(c);}}
    /** 先算完整一道解（含成长、倍率和女仆系数），再由命中处乘本轮统一40%，十字的每道为一道解，强化剑气为3倍。 */
    static float dismantleDamage(ServerPlayer p,LivingEntity target){
        double base=Curse.awakened(p)?12:7;
        if(VoidJourney.evolved(p))base=VoidTechniqueRules.DISMANTLE_DAMAGE*VoidController.voidDamage(p)*Curse.power(p);
        else if(SoulJourney.evolved(p))base+=Curse.soulSlashBonus(p);
        else base*=Math.pow(0.95,Math.max(0,(int)(target.getBoundingBox().getCenter().distanceTo(p.getEyePosition())/10)));
        return CombatSettings.spellDamage(p,base)*(float)SlashFormRules.DAMAGE;
    }
    private static void hit(ServerPlayer p,LivingEntity target,double fraction){
        if(!valid(p,p.serverLevel())||DomainClash.active(p)||DomainClash.active(target)||!CombatSettings.canHit(p,target))return;
        float amount=(float)(dismantleDamage(p,target)*fraction);target.invulnerableTime=0;
        CombatSettings.hurt(p,target,p.serverLevel().damageSources().sonicBoom(p),amount);
    }
    private static void qiEffect(Qi q){Network.send(q.level,new Network.Effect(NetBranchRules.FX_QI,q.owner.getId(),q.position,q.direction,(float)NetBranchRules.QI_RADIUS,Math.min(10,NetBranchRules.QI_LIFE-q.age),q.id));}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(AirCross c:List.copyOf(airCrosses)){
            int age=(int)(c.level.getGameTime()-c.born);if(!valid(c.p,c.level)||DomainClash.active(c.p)||age>NetBranchRules.CROSS_TICKS){airCrosses.remove(c);continue;}
            for(int wave=0;wave<NetBranchRules.WAVES;wave++){
                if(age==NetBranchRules.launch(wave))for(int corner=0;corner<8;corner++){int opposite=NetBranchRules.oppositeCorner(corner);Vec3 from=c.center.add(NetBranchRules.cornerX(corner),NetBranchRules.cornerY(corner),NetBranchRules.cornerZ(corner)),to=c.center.add(NetBranchRules.cornerX(opposite),NetBranchRules.cornerY(opposite),NetBranchRules.cornerZ(opposite));Network.send(c.level,new Network.Effect(NetBranchRules.FX_CROSS,c.p.getId(),from,to.subtract(from),3,NetBranchRules.CROSS_VISUAL_TICKS,++nextId));SkillDestruction.line(c.p,from,to,0.6);}
                if(age==NetBranchRules.arrival(wave))for(LivingEntity t:Skills.targets(c.level,c.p,c.center,2))for(int cut=0;cut<16&&t.isAlive();cut++)hit(c.p,t,NetBranchRules.CROSS_FRACTION);
            }
        }
        // 伤害回调可能撤销保护或结束女仆对战；使用快照，不在可重入集合的迭代器里结算。
        for(Cross c:List.copyOf(crosses.values())){
            if(crosses.get(c.target.getUUID())!=c)continue;
            if(!crossValid(c)){crosses.remove(c.target.getUUID(),c);release(c);continue;}
            int age=(int)(c.level.getGameTime()-c.born);hold(c);
            if(age%10==0)holdEffect(c,NetBranchRules.CROSS_TICKS-age);
            Vec3 center=c.anchor.add(0,c.target.getBbHeight()/2,0);
            for(int wave=0;wave<NetBranchRules.WAVES;wave++){
                if(age==NetBranchRules.launch(wave))for(int corner=0;corner<NetBranchRules.CORNERS;corner++){
                    int opposite=NetBranchRules.oppositeCorner(corner);
                    Vec3 from=center.add(NetBranchRules.cornerX(corner),NetBranchRules.cornerY(corner),NetBranchRules.cornerZ(corner));
                    Vec3 to=center.add(NetBranchRules.cornerX(opposite),NetBranchRules.cornerY(opposite),NetBranchRules.cornerZ(opposite));
                    SkillDestruction.line(c.owner,from,to,0.6);
                    Network.send(c.level,new Network.Effect(NetBranchRules.FX_CROSS,c.owner.getId(),from,to.subtract(from),3,NetBranchRules.CROSS_VISUAL_TICKS,c.id));
                }
                if(age==NetBranchRules.arrival(wave))for(int cut=0;cut<NetBranchRules.CORNERS*2&&crossValid(c)&&crosses.get(c.target.getUUID())==c;cut++)hit(c.owner,c.target,NetBranchRules.CROSS_FRACTION);
            }
            if(age>=NetBranchRules.CROSS_TICKS||!crossValid(c)){
                boolean completed=age==NetBranchRules.CROSS_TICKS&&crossValid(c)&&crosses.get(c.target.getUUID())==c;
                crosses.remove(c.target.getUUID(),c);release(c,completed);
            }
        }
        for(Qi q:List.copyOf(projectiles)){
            if(!projectiles.contains(q))continue;
            if(!valid(q.owner,q.level)||DomainClash.active(q.owner)||q.age>=NetBranchRules.QI_LIFE){projectiles.remove(q);stop(q.owner,q.level,q.position,q.id);continue;}
            Vec3 next=q.position.add(q.direction.scale(NetBranchRules.QI_SPEED));
            if(CombatExpansion.collideSlash(q.owner,q.position,next,q.direction)){projectiles.remove(q);stop(q.owner,q.level,q.position,q.id);continue;}
            for(int step=0;step<=5;step++){Vec3 center=q.position.lerp(next,step/5.0);Vec3 previous=null;for(int i=0;i<=16;i++){double angle=-Math.PI/2+Math.PI*i/16;Vec3 point=center.add(q.side.scale(NetBranchRules.qiSide(angle))).add(q.direction.cross(q.side).scale(NetBranchRules.qiBend(angle)));if(previous!=null)SkillDestruction.immediate(q.owner,previous,point,0.5);previous=point;}}
            for(LivingEntity target:q.level.getEntitiesOfClass(LivingEntity.class,new AABB(q.position,next).inflate(NetBranchRules.QI_RADIUS+0.5),e->!q.hit.contains(e.getUUID())&&CombatSettings.canHit(q.owner,e))){
                if(DomainClash.active(target))continue;
                boolean intersects=false;
                // 整个月牙扫过的体积逐段检测；完全不做方块碰撞，所以穿墙且不限命中数量。
                for(int i=0;i<=32&&!intersects;i++){
                    double angle=-Math.PI/2+Math.PI*i/32;Vec3 offset=q.side.scale(NetBranchRules.qiSide(angle)).add(q.direction.cross(q.side).scale(NetBranchRules.qiBend(angle)));
                    Vec3 from=q.position.add(offset),to=next.add(offset);AABB box=target.getBoundingBox().inflate(0.35);
                    intersects=box.contains(from)||box.contains(to)||box.clip(from,to).isPresent();
                }
                if(intersects){q.hit.add(target.getUUID());hit(q.owner,target,NetBranchRules.QI_FRACTION);if(!projectiles.contains(q))break;}
            }
            q.position=next;q.age++;
            if(q.age<NetBranchRules.QI_LIFE&&q.age%10==0)qiEffect(q);
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void freeze(LivingEvent.LivingTickEvent event){LivingEntity t=event.getEntity();if(t.level().isClientSide||!locked(t))return;hold(crosses.get(t.getUUID()));if(!(t instanceof net.minecraft.world.entity.player.Player))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(AttackEntityEvent event){if(locked(event.getEntity()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interact(PlayerInteractEvent event){if(event.isCancelable()&&locked(event.getEntity()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void outgoing(LivingAttackEvent event){if(event.getSource().getEntity() instanceof LivingEntity living&&locked(living))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void breakSpeed(PlayerEvent.BreakSpeed event){if(locked(event.getEntity()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void teleport(net.minecraftforge.event.entity.EntityTeleportEvent event){if(event.isCancelable()&&event.getEntity() instanceof LivingEntity living&&locked(living))event.setCanceled(true);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){crosses.clear();airCrosses.clear();projectiles.clear();nextId=0;}
    private NetBranches(){}
}
