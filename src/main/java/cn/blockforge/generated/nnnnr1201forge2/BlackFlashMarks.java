package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 咒力附拳期间的黑闪标记（用户要求）：
 *  <ul>
 *    <li>附拳开启时，**每 0.5 秒**都有 **1%** 概率，在周围 **5 格**内所有敌人的**随机位置**闪一个
 *        **半径 0.3 格、持续 0.7 秒的红圈**，且**始终画在生物之上**（关深度测试，最高优先级）。</li>
 *    <li>如果玩家在有效时间内**准星对准红圈 + 按左键**：**瞬间跳到敌人身前**打出黑闪，
 *        伤害 = **本次普攻伤害的 3 次方**（封顶 {@link #MAX_DAMAGE}）。</li>
 *  </ul>
 *  服务端负责掷骰、发标记与结算瞬移黑闪；客户端只管画红圈、瞄准判定与发按键包。 */
@Mod.EventBusSubscriber(modid="sukuna")
public final class BlackFlashMarks {
    /** 特效号：客户端据此在指定点画红圈（position=点，owner=敌人 id，life=持续时间刻）。 */
    public static final int FX_MARK=237;
    public static final double RANGE=5.0,MARK_RADIUS=0.3;
    public static final int MARK_TICKS=14;      // 0.7 秒
    public static final int ROLL_TICKS=10;      // 0.5 秒
    public static final double CHANCE=0.05;     // 5%（用户指定）
    public static final double MAX_DAMAGE=1.0e7;
    /** 瞬移到敌人身前后的无敌帧（刻）。 */
    public static final int INVULN_TICKS=20;

    /** 服务端：玩家 -> 有效标记（敌人 id -> 到期刻）。 */
    private static final Map<UUID,Map<Integer,Long>> MARKS=new HashMap<>();

    // ------------------------------------------------------------------ 服务端
    /** 独立的服务端每刻钩子：**每刻都跑**，内部再按 10 刻（0.5 秒）掷骰。
     *  （之前挂在附拳每 15 刻的广播循环里，和 10 刻取最小公倍数 → 实际 30 刻才掷一次，看起来像只出现一次。） */
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if(server==null)return;
        for(ServerPlayer p:server.getPlayerList().getPlayers())tick(p);
    }
    /** 每 0.5 秒掷一次骰子：附拳开启 + 半径 5 格内有敌人时，1% 概率给**每个**敌人一个随机点标记。 */
    public static void tick(ServerPlayer p){
        if(p.level().getGameTime()%ROLL_TICKS!=0)return;
        if(!Curse.data(p).getBoolean("Fist"))return;
        MARKS.values().forEach(m->m.entrySet().removeIf(e->p.level().getGameTime()>e.getValue()));
        if(p.getRandom().nextDouble()>=CHANCE)return;
        AABB box=p.getBoundingBox().inflate(RANGE);
        List<LivingEntity> enemies=((ServerLevel)p.level()).getEntitiesOfClass(LivingEntity.class,box,
            e->e!=p&&e.isAlive()&&e.distanceToSqr(p)<=RANGE*RANGE&&CombatSettings.canHit(p,e));
        if(enemies.isEmpty())return;
        Map<Integer,Long> mine=MARKS.computeIfAbsent(p.getUUID(),k->new HashMap<>());
        long until=p.level().getGameTime()+MARK_TICKS;
        for(LivingEntity e:enemies){
            AABB b=e.getBoundingBox();
            Vec3 at=new Vec3(b.minX+p.getRandom().nextDouble()*b.getXsize(),
                             b.minY+p.getRandom().nextDouble()*b.getYsize(),
                             b.minZ+p.getRandom().nextDouble()*b.getZsize());
            mine.put(e.getId(),until);
            Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),
                new Network.Effect(FX_MARK,e.getId(),at,Vec3.ZERO,1,MARK_TICKS,0));
            SkillSounds.play(p,"domainshatter",3.5f);   // 红圈出现音效（声音要大）
        }
    }

    /** 服务端：玩家在有效时间内瞄准并左键 → 瞬移到敌人身前，打出"普攻伤害³"的黑闪。 */
    public static void strike(ServerPlayer p,int targetId){
        Map<Integer,Long> mine=MARKS.get(p.getUUID());
        if(mine==null||!mine.containsKey(targetId))return;
        if(p.level().getGameTime()>mine.getOrDefault(targetId,0L)){mine.remove(targetId);return;}
        if(!(p.level().getEntity(targetId) instanceof LivingEntity target)||!target.isAlive()){mine.remove(targetId);return;}
        if(target.distanceToSqr(p)>RANGE*RANGE*4)return;                       // 距离兜底
        mine.remove(targetId);
        // 跳到敌人身前：沿"玩家→敌人"方向退 1.2 格，朝敌人面朝
        Vec3 dir=target.position().subtract(p.position());
        if(dir.lengthSqr()<1e-4)dir=p.getLookAngle();
        dir=dir.normalize();
        Vec3 at=target.position().subtract(dir.scale(1.2));
        p.connection.teleport(at.x,target.getY(),at.z,p.getYRot(),p.getXRot());
        float yaw=(float)(Math.toDegrees(Math.atan2(-dir.x,dir.z)));
        p.connection.teleport(at.x,target.getY(),at.z,yaw,0f);
        p.invulnerableTime=INVULN_TICKS;                          // 无敌帧（用户要求）
        // 伤害 = 本次普攻伤害的 3 次方（封顶）
        double base=Math.max(1,p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
        double damage=Math.min(MAX_DAMAGE,Math.pow(base,3));
        AttackTypes.mark(AttackTypes.LIGHTNING);
        CombatSettings.hurt(p,target,target.damageSources().playerAttack(p),(float)damage);
        fx(p,target.getEyePosition(),dir);
    }
    private static void fx(ServerPlayer p,Vec3 at,Vec3 dir){
        Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p),
            new Network.Effect(85,p.getId(),at,dir,1,16,p.getId()));
        SkillSounds.play(p,"black_flash");
    }

    // ------------------------------------------------------------------ 客户端
    private record Mark(int entityId,Vec3 point,long until){}
    private static final List<Mark> CLIENT=new ArrayList<>();
    private static final List<Mark> DESYNC=new ArrayList<>();

    public static void receive(Network.Effect e){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        CLIENT.add(new Mark(e.owner(),e.position(),mc.level.getGameTime()+Math.max(4,e.life())));
    }

    /** 客户端每刻：过期清理 + 准星是否压在某个红圈上。 */
    public static void clientTick(){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null){CLIENT.clear();return;}
        long now=mc.level.getGameTime();
        CLIENT.removeIf(m->now>m.until());
        if(!Client.progress.fist()||mc.screen!=null)return;
        Mark aimed=aimed();
        if(aimed!=null&&mc.options.keyAttack.consumeClick())
            Network.CHANNEL.sendToServer(new Network.BlackFlashStrike(aimed.entityId()));
    }
    /** 准星射线与红圈（半径 0.3 + 0.15 容差）求交；取最近的那个。 */
    private static Mark aimed(){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return null;
        Vec3 eye=mc.player.getEyePosition(1f),look=mc.player.getViewVector(1f);
        double best=Double.MAX_VALUE;Mark hit=null;
        for(Mark m:CLIENT){
            if(mc.level.getEntity(m.entityId())==null)continue;
            Vec3 d=m.point().subtract(eye);
            double t=d.dot(look);
            if(t<=0||t>6.0)continue;
            double dist=d.subtract(look.scale(t)).length();
            if(dist<=MARK_RADIUS+0.45&&t<best){best=t;hit=m;}
        }
        return hit;
    }

    /** 客户端渲染：关深度测试，**始终画在生物之上**（用户要求的最高优先级）。 */
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(CLIENT.isEmpty())return;
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        long now=mc.level.getGameTime();
        var cam=e.getCamera().getPosition();
        float t=mc.level.getGameTime()+e.getPartialTick();
        RenderSystem.disableDepthTest();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        var ps=e.getPoseStack();ps.pushPose();ps.translate(-cam.x,-cam.y,-cam.z);
        for(Mark m:CLIENT){
            float left=(m.until()-t)/MARK_TICKS;                        // 0~1 剩余
            if(left<0)continue;
            int a=(int)((0.55+0.45*Math.abs(Math.sin(t*0.6)))*255*Math.min(1,left*3));
            ring(b,ps,m.point(),MARK_RADIUS,a,cam);
        }
        BufferUploader.drawWithShader(b.end());
        ps.popPose();RenderSystem.enableCull();RenderSystem.enableDepthTest();RenderSystem.disableBlend();
    }
    /** 一个水平红圈（朝向相机的细环，用两圈三角带近似）。 */
    private static void ring(BufferBuilder b,PoseStack ps,Vec3 c,double r,int alpha,Vec3 cam){
        // 竖着画（面向相机的竖直圆环）——横圈贴着地面根本瞄不到
        Vec3 n=cam.subtract(c);if(n.lengthSqr()<1e-6)n=new Vec3(0,0,1);n=n.normalize();
        Vec3 up0=new Vec3(0,1,0).subtract(n.scale(n.y));
        if(up0.lengthSqr()<1e-4)up0=new Vec3(1,0,0);
        Vec3 right=n.cross(up0).normalize(),upv=up0.normalize();
        int color=(alpha<<24)|0xFF2A18;
        int seg=24;
        for(int i=0;i<seg;i++){
            double a0=i*Math.PI*2/seg,a1=(i+1)*Math.PI*2/seg;
            Vec3 p0=c.add(right.scale(Math.cos(a0)*r)).add(upv.scale(Math.sin(a0)*r));
            Vec3 p1=c.add(right.scale(Math.cos(a1)*r)).add(upv.scale(Math.sin(a1)*r));
            Vec3 p0b=c.add(right.scale(Math.cos(a0)*r*0.72)).add(upv.scale(Math.sin(a0)*r*0.72));
            Vec3 p1b=c.add(right.scale(Math.cos(a1)*r*0.72)).add(upv.scale(Math.sin(a1)*r*0.72));
            quad(b,ps,p0,p1,p1b,p0b,color);
        }
    }
    private static void quad(BufferBuilder b,PoseStack ps,Vec3 p0,Vec3 p1,Vec3 p2,Vec3 p3,int color){
        b.vertex(ps.last().pose(),(float)p0.x,(float)p0.y,(float)p0.z).color(color).endVertex();
        b.vertex(ps.last().pose(),(float)p1.x,(float)p1.y,(float)p1.z).color(color).endVertex();
        b.vertex(ps.last().pose(),(float)p2.x,(float)p2.y,(float)p2.z).color(color).endVertex();
        b.vertex(ps.last().pose(),(float)p3.x,(float)p3.y,(float)p3.z).color(color).endVertex();
    }
    private BlackFlashMarks(){}
}
