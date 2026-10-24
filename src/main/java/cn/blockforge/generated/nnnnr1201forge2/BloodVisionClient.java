package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;

/** 只放慢本机绘制的呼吸/挥手动画；实体位置、碰撞和服务器时间绝不改动。 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class BloodVisionClient {
    private record Saved(int tick,float attack,float oldAttack){}
    private static final Map<LivingEntity,Saved> saved=new IdentityHashMap<>();
    private static final Map<UUID,Double> phases=new HashMap<>();
    private static double lastTime=-1,visualTime;
    private static float focus;
    private static void before(LivingEntity e){
        if(!BloodClient.predicted(e.getId())||saved.containsKey(e))return;
        saved.put(e,new Saved(e.tickCount,e.attackAnim,e.oAttackAnim));
        double start=phases.computeIfAbsent(e.getUUID(),k->e.tickCount-visualTime);
        e.tickCount=(int)(start+visualTime);
        // 挥手保留真实方向，只拉长本帧内的插值；不延后命中盒。
        e.attackAnim=e.oAttackAnim+(e.attackAnim-e.oAttackAnim)*.35f;
    }
    private static void after(LivingEntity e){Saved s=saved.remove(e);if(s!=null){e.tickCount=s.tick;e.attackAnim=s.attack;e.oAttackAnim=s.oldAttack;}}
    private static void restore(){for(LivingEntity e:List.copyOf(saved.keySet()))after(e);}
    public static void clear(){restore();phases.clear();lastTime=-1;visualTime=0;focus=0;}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void pre(RenderLivingEvent.Pre<?,?> e){before(e.getEntity());}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void post(RenderLivingEvent.Post<?,?> e){after(e.getEntity());}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void playerPre(RenderPlayerEvent.Pre e){before(e.getEntity());}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void playerPost(RenderPlayerEvent.Post e){after(e.getEntity());}
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent e){
        if(e.phase==TickEvent.Phase.END){restore();return;}
        var mc=Minecraft.getInstance();if(mc.level==null){clear();return;}
        double time=mc.level.getGameTime()+e.renderTickTime,delta=lastTime<0?0:Math.max(0,Math.min(2,time-lastTime));lastTime=time;
        boolean on=BloodClient.vision();visualTime+=delta*(on?.35:1);focus+=(on?1-focus:-focus)*Math.min(1,(float)delta*.2f);
        if(!on)phases.clear();
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.START)restore();}
    @SubscribeEvent public static void fov(ViewportEvent.ComputeFov e){if(e.usedConfiguredFov()&&focus>.001)e.setFOV(e.getFOV()*(1-.08*focus));}
    private BloodVisionClient(){}
}
