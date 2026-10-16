package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

/** 服务端决定动作、碰撞、扣费和逐道发射；玩家和女仆共用时间轴。 */
public final class DismantleMotion {
    private static final class Motion {
        final ServerPlayer player;final LivingEntity actor;final ServerLevel level;final int kind;final UUID token=UUID.randomUUID();
        final Vec3 forward;final float yaw;final boolean gravity;final long born;final double width,height;
        final Set<UUID> kicked=new HashSet<>();
        Vec3 position;boolean blocked,ringReleased;int age,emitted;
        Motion(ServerPlayer p,int kind){
            player=p;actor=MaidBridge.body(p);level=p.serverLevel();this.kind=kind;position=actor.position();yaw=p.getYRot();
            double a=Math.toRadians(yaw);forward=new Vec3(-Math.sin(a),0,Math.cos(a));gravity=actor.isNoGravity();born=level.getGameTime();
            width=actor.getBbWidth();height=MaidBridge.isCaster(p)?actor.getBbHeight():1.8;
        }
    }
    private static final Map<UUID,Motion> motions=new HashMap<>();
    public static boolean active(LivingEntity entity){return motions.containsKey(entity.getUUID())||motions.values().stream().anyMatch(m->m.actor==entity);}
    private static boolean allowed(ServerPlayer p){
        return p.isAlive()&&!p.isRemoved()&&(MaidBridge.isCaster(p)||!p.hasDisconnected())&&!p.isSpectator()
            &&MaidBridge.body(p).level()==p.level()&&Journey.unlocked(p)&&Curse.canUseTechnique(p)&&!SoulCombat.suppressed(p)
            &&!MaidBridge.recovering(p)&&!EternalLove.settling(MaidBridge.body(p))&&!DomainClash.active(p)&&!NetBranches.locked(p)
            &&!Uraume.locked(p)&&!GuardianPlayerSkills.locked(p);
    }
    private static boolean grounded(LivingEntity body){return !body.isPassenger()&&!body.isSleeping()&&!body.isFallFlying()&&!body.isInWater()&&body.onGround();}
    /** 空中动作（冲拳）不要求落地；其余动作仍要求站在地面上。 */
    private static boolean needsGround(int kind){return kind!=DismantleMotionRules.SPIN&&kind!=DismantleMotionRules.AIR_PUNCH;}
    public static boolean available(ServerPlayer p,int kind){return DismantleMotionRules.valid(kind)&&!active(p)&&allowed(p)&&(!needsGround(kind)||grounded(MaidBridge.body(p)))&&Skills.available(p,0);}
    public static void start(ServerPlayer p,int kind){
        if(!DismantleMotionRules.valid(kind)||active(p))return;
        // 动作期间不能重入，但收招后可以立刻继续使用解。
        if(!allowed(p)){CastFeedback.unavailable(p,Curse.COST[0]);why(p,"状态不允许（禁用/领域冲突/锁技/非本维度等）");return;}
        LivingEntity actor=MaidBridge.body(p);
        if(needsGround(kind)&&!grounded(actor)){CastFeedback.tell(p,"请站在地面上释放解动作");why(p,"不在地面");return;}
        // r204：**删掉"必须先蹲下"的限制**——蹲下就疾跑不了，导致"疾跑滑铲/空中冲拳"这类动作根本用不出来。
        // 现在由 choose() 的按键组合决定动作：蹲下前进/后退仍是飞踢/后翻，疾跑前进就是滑铲（起跳=冲拳）。
        Motion m=new Motion(p,kind);
        if(!free(m,m.position)){CastFeedback.tell(p,"上方空间太低，无法完成动作");why(p,"上方空间不足");return;}
        if(!Skills.beginMotion(p)){CastFeedback.unavailable(p,Curse.COST[0]);why(p,"咒力不足（解动作花费 "+Curse.COST[0]+"）");return;}
        why(p,"开始动作 kind="+kind+name(kind));
        CombatExpansion.fallingBranch(p);begin(m);
    }
    private static String name(int kind){
        if(kind==DismantleMotionRules.BACKFLIP)return"(后翻)";
        if(kind==DismantleMotionRules.FLYING_KICK)return"(飞踢)";
        if(kind==DismantleMotionRules.SPIN)return"(旋转环斩)";
        if(kind==DismantleMotionRules.KNEEL)return"(跪地)";
        if(kind==DismantleMotionRules.AIR_PUNCH)return"(冲拳)";
        if(kind==DismantleMotionRules.SLIDE)return"(滑铲)";
        if(kind==DismantleMotionRules.GROUND_SMASH)return"(砸地)";
        return "";
    }
    /** 埋点：把"动作这次为什么没起来"写进 latest.log（同一玩家同一原因 4 秒内只写一条，不刷屏）。
     *  没有它的话，"没触发"只能靠猜——服务端有六个静默 return 点。 */
    private static final Map<UUID,long[]> lastWhy=new HashMap<>();
    public static void why(ServerPlayer p,String reason){
        long now=p.serverLevel().getGameTime();
        long[] stamp=lastWhy.get(p.getUUID());
        if(stamp!=null&&now-stamp[0]<80&&reason.equals(reasonCache.get(p.getUUID())))return;
        lastWhy.put(p.getUUID(),new long[]{now});
        reasonCache.put(p.getUUID(),reason);
        com.mojang.logging.LogUtils.getLogger().info("[解动作] {}：{}",p.getGameProfile().getName(),reason);
    }
    private static final Map<UUID,String> reasonCache=new HashMap<>();
    /** 姿态分支已在原技能入口扣费，不重复扣费或吞掉原分支。 */
    public static boolean startPaid(ServerPlayer p,int kind){
        if(!DismantleMotionRules.valid(kind)||active(p)||!allowed(p))return false;
        LivingEntity actor=MaidBridge.body(p);if(actor.isPassenger()||actor.isSleeping()||actor.isFallFlying()||actor.isInWater())return false;
        Motion m=new Motion(p,kind);if(!free(m,m.position))return false;begin(m);return true;
    }
    private static void begin(Motion m){ServerPlayer p=m.player;LivingEntity actor=m.actor;
        Skills.cancelCharge(p);FreehandDrawing.cancel(p);actor.stopUsingItem();
        motions.put(p.getUUID(),m);actor.setNoGravity(true);actor.setDeltaMovement(Vec3.ZERO);actor.fallDistance=0;
        send(m,false);SkillSounds.cast(p,0);
    }
    private static AABB body(Motion m,Vec3 at){double half=m.width/2;return new AABB(at.x-half,at.y,at.z-half,at.x+half,at.y+m.height,at.z+half);}
    private static boolean free(Motion m,Vec3 at){
        AABB box=body(m,at);
        if(box.minY<m.level.getMinBuildHeight()||box.maxY>m.level.getMaxBuildHeight()||!m.level.getWorldBorder().isWithinBounds(box))return false;
        for(double x:new double[]{box.minX,box.maxX})for(double z:new double[]{box.minZ,box.maxZ})if(!m.level.hasChunkAt(BlockPos.containing(x,at.y,z)))return false;
        return m.level.noCollision(m.actor,box);
    }
    private static Vec3 safeMove(Motion m,Vec3 delta){
        int steps=Math.max(1,(int)Math.ceil(delta.length()/0.15));Vec3 step=delta.scale(1.0/steps),at=m.position;
        for(int i=0;i<steps;i++){
            Vec3 next=at.add(step);if(free(m,next)){at=next;continue;}
            if(Math.abs(step.x)+Math.abs(step.z)>1e-6)m.blocked=true;
            Vec3 vertical=at.add(0,step.y,0);if(free(m,vertical))at=vertical;else break;
        }
        return at;
    }
    private static Network.MotionState packet(Motion m,boolean end){return new Network.MotionState(m.actor.getId(),m.token,m.kind,m.age,m.yaw,m.position,end);}
    private static void send(Motion m,boolean end){
        // 必须追踪真实实体，不能向女仆的 FakePlayer 连接发包。
        Network.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->m.actor),packet(m,end));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        for(Motion m:List.copyOf(motions.values())){
            ServerPlayer p=m.player;LivingEntity actor=m.actor;
            if(!allowed(p)||actor.level()!=m.level||actor.isPassenger()||actor.isSleeping()||actor.position().distanceToSqr(m.position)>16){cancel(p);continue;}
            int next=(int)(m.level.getGameTime()-m.born);if(next<=m.age)continue;
            if(next>DismantleMotionRules.duration(m.kind)){cancel(p);continue;}
            double dx=DismantleMotionRules.horizontal(m.kind,next)-DismantleMotionRules.horizontal(m.kind,m.age);
            if(m.kind==DismantleMotionRules.FLYING_KICK&&m.age<18&&next>=18)m.blocked=false;
            double dy=DismantleMotionRules.height(m.kind,next)-DismantleMotionRules.height(m.kind,m.age);
            Vec3 before=m.position;
            m.position=safeMove(m,m.forward.scale(m.blocked?0:dx).add(0,dy,0));m.age=next;
            int landing=m.kind==DismantleMotionRules.BACKFLIP?22:m.kind==DismantleMotionRules.FLYING_KICK?30:m.kind==DismantleMotionRules.AIR_PUNCH?1000:0;
            if(next>=landing)m.position=safeMove(m,new Vec3(0,-0.35,0));
            actor.setPos(m.position);actor.setDeltaMovement(Vec3.ZERO);actor.setNoGravity(true);actor.setOnGround(!free(m,m.position.add(0,-0.08,0)));actor.fallDistance=0;
            if(MaidBridge.isCaster(p)){
                actor.setYRot(m.yaw);actor.yBodyRot=actor.yHeadRot=m.yaw;
                p.setPos(m.position);p.setYRot(m.yaw);p.setDeltaMovement(Vec3.ZERO);
            }else p.connection.resetPosition();
            if(DismantleMotionRules.kickActive(m.kind,next))kick(m,before);
            if(motions.get(p.getUUID())!=m)continue;
            if(m.kind!=DismantleMotionRules.SPIN&&!m.ringReleased&&next>=DismantleMotionRules.release(m.kind)){m.ringReleased=true;CombatExpansion.motionRelease(p,m.kind);}
            if(motions.get(p.getUUID())!=m)continue;
            if(m.kind==DismantleMotionRules.SPIN){
                if(!m.ringReleased&&next>=DismantleMotionRules.release(m.kind)){m.ringReleased=true;SlashForms.motionRing(p,m.position.add(0,Math.min(0.9,m.height*0.5),0));SkillSounds.cast(p,0);}
            }else if(DismantleMotionRules.bladeReady(m.kind,next,m.emitted)){
                // 一次服务端 tick 最多发一道；尚未出手的剑气随动作中断一并取消。
                SlashForms.motionBlade(p,m.forward);m.emitted++;SkillSounds.cast(p,0);
            }
            if(motions.get(p.getUUID())==m)send(m,false);
        }
    }
    private static void kick(Motion m,Vec3 before){
        // 扫过真实身体前方的踢击路径；腾空时不再只检查脚底附近。
        Vec3 offset=m.forward.scale(m.width/2+0.35).add(0,Math.min(0.9,m.height*0.5),0);
        Vec3 from=before.add(offset),to=m.position.add(offset);double radius=m.width/2+0.3;
        AABB sweep=new AABB(from,to).inflate(radius,0.65,radius);
        for(LivingEntity target:m.level.getEntitiesOfClass(LivingEntity.class,sweep,t->CombatSettings.canHit(m.player,t))){
            if(m.kicked.contains(target.getUUID())||DomainClash.active(target))continue;
            AABB box=target.getBoundingBox(),expanded=box.inflate(radius,0.65,radius);
            Vec3 contact=expanded.contains(from)?from:expanded.clip(from,to).orElse(null);if(contact==null)continue;
            Vec3 touch=new Vec3(Math.max(box.minX,Math.min(box.maxX,contact.x)),Math.max(box.minY,Math.min(box.maxY,contact.y)),Math.max(box.minZ,Math.min(box.maxZ,contact.z)));
            // 接触体积可以略大于脚，但不能隔着实心方块踢到另一侧。
            Vec3 origin=before.add(0,Math.min(0.9,m.height*0.5),0);
            if(m.level.clip(new net.minecraft.world.level.ClipContext(origin,touch,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,m.actor)).getType()!=HitResult.Type.MISS)continue;
            m.kicked.add(target.getUUID());int previousInvulnerability=target.invulnerableTime;target.invulnerableTime=0;
            float damage=CombatSettings.spellDamage(m.player,DismantleMotionRules.KICK_DAMAGE);
            if(!CombatSettings.hurt(m.player,target,m.player.damageSources().indirectMagic(m.actor,m.actor),damage)){target.invulnerableTime=previousInvulnerability;continue;}
            if(!target.isAlive())continue;
            Vec3 oldVelocity=target.getDeltaMovement();
            target.knockback(DismantleMotionRules.KICK_KNOCKBACK,-m.forward.x,-m.forward.z);
            Vec3 velocity=target.getDeltaMovement();
            // 保留原版击退抗性和其他模组取消击退的结果；空中命中也有抬升。
            if(!velocity.equals(oldVelocity)){
                double lift=DismantleMotionRules.kickLift(target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE));
                if(lift>0)target.setDeltaMovement(velocity.x,Math.max(velocity.y,lift),velocity.z);
                target.hurtMarked=true;
            }
        }
    }
    public static void cancel(ServerPlayer p){
        Motion m=motions.remove(p.getUUID());if(m==null)return;
        m.actor.setNoGravity(m.gravity);m.actor.setDeltaMovement(Vec3.ZERO);m.actor.fallDistance=0;send(m,true);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e){if(e.getEntity() instanceof ServerPlayer listener){Motion m=motions.get(e.getTarget().getUUID());if(m!=null)Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->listener),packet(m,false));}}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){for(Motion m:List.copyOf(motions.values()))cancel(m.player);motions.clear();}
    private DismantleMotion(){}
}
