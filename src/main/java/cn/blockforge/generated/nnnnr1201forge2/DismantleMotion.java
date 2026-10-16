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
        Vec3 position;boolean blocked,ringReleased,backwardMove;int age,emitted;
        Motion(ServerPlayer p,int kind,int rotate){
            player=p;actor=MaidBridge.body(p);level=p.serverLevel();this.kind=kind;position=actor.position();yaw=p.getYRot();
            double a=Math.toRadians(yaw);
            Vec3 facing=new Vec3(-Math.sin(a),0,Math.cos(a));
            // 按客户端实际按的移动键取"真前进方向"；后退类动作沿它的反方向位移，但人仍然面向敌人
            Vec3 look=new Vec3(p.getLookAngle().x,0,p.getLookAngle().z);
            if(look.lengthSqr()<1e-4)look=facing;
            look=look.normalize();
            double wantX=0,wantZ=0;
            if((rotate&1)!=0){wantX+=look.x;wantZ+=look.z;}
            if((rotate&2)!=0){wantX-=look.x;wantZ-=look.z;}
            Vec3 want=new Vec3(wantX,0,wantZ);
            // 默认（没有位移输入）：飞踢等"向前"的动作沿用视线方向
            forward=(want.lengthSqr()>1e-4?want.normalize():look);
            backwardMove=(rotate&2)!=0&&(rotate&1)==0;
            gravity=actor.isNoGravity();born=level.getGameTime();
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
    public static boolean available(ServerPlayer p,int kind){return DismantleMotionRules.valid(kind)&&!active(p)&&allowed(p)&&grounded(MaidBridge.body(p))&&Skills.available(p,0);}
    public static void start(ServerPlayer p,int kind){start(p,kind,0);}
    /** {@code rotate} 是客户端的"转向意图"位：1 前进、2 后退、4 蹲下、8 跳跃、16 疾跑。
     *  后退类动作（后翻）**保持面向敌人**，位移方向按实际移动方向反转，符合《使用说明》的
     *  "向后移动 10 格/向前飞踢"以及"剑气朝起手时面对的水平方向飞行"。 */
    public static void start(ServerPlayer p,int kind,int rotate){
        if(!DismantleMotionRules.valid(kind)||active(p))return;
        // 动作期间不能重入，但收招后可以立刻继续使用解。
        if(!allowed(p)){CastFeedback.unavailable(p,Curse.COST[0]);return;}
        LivingEntity actor=MaidBridge.body(p);
        if(!grounded(actor)&&kind!=DismantleMotionRules.SPIN){CastFeedback.tell(p,"请站在地面上释放解动作");return;}
        if(!MaidBridge.isCaster(p)&&kind!=DismantleMotionRules.SPIN&&!p.isShiftKeyDown()){CastFeedback.tell(p,"按住蹲下和前进／后退，再释放解");return;}
        Motion m=new Motion(p,kind,rotate);
        if(!free(m,m.position)){CastFeedback.tell(p,"上方空间太低，无法完成动作");return;}
        if(!Skills.beginMotion(p))return;
        CombatExpansion.fallingBranch(p);begin(m);
    }
    /** 姿态分支已在原技能入口扣费，不重复扣费或吞掉原分支。 */
    public static boolean startPaid(ServerPlayer p,int kind){
        if(!DismantleMotionRules.valid(kind)||active(p)||!allowed(p))return false;
        LivingEntity actor=MaidBridge.body(p);if(actor.isPassenger()||actor.isSleeping()||actor.isFallFlying()||actor.isInWater())return false;
        Motion m=new Motion(p,kind,0);if(!free(m,m.position))return false;begin(m);return true;
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
            m.position=safeMove(m,m.forward.scale(m.blocked?0:(m.backwardMove?-dx:dx)).add(0,dy,0));m.age=next;
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
            // 只"轻推开"，不结算独立踢击伤害；也不碰无敌帧（避免把别人的伤害判定吃掉）。
            m.kicked.add(target.getUUID());
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
