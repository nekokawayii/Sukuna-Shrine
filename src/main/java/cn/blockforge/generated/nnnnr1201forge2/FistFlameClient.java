package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 「咒力附拳」的表现层：**只用用户给的 `sukuna:fx/meleepunch.fx`**（拳击特效），
 *  并且**绑定手臂朝向**——贴在手上、随手臂转动。自绘的咒力光团与旧的 `photon:11` 都已经删掉。
 *
 *  <p>机制：渲染层（第三人称/别人看）与第一人称世界层各自算出**手上那一点的世界坐标与朝向**，
 *  起一份 Photon 的 `BlockEffect`：`setOffset(手相对方块中心)` + `setRotation(手臂/相机的四元数)`；
 *  持续期间每 {@link #REFRESH} 刻重起一份（旧的先 `setForcedDeath(true)`，永远只留一份），
 *  附拳关闭或切世界时统一销毁。Photon 是运行时可选依赖，全部走**类名字符串反射**，未装则静默跳过。</p> */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class FistFlameClient {
    /** 服务端广播的特效号：附拳期间每 15 刻一条（带 entityId 与寿命）。 */
    public static final int FX=236;
    /** 用户给的拳击特效：`ldlib/assets/photon/fx/11.fx`（LDLib 文件系统资源目录，用 `photon:11` 取）。 */
    private static final ResourceLocation PUNCH_FX=new ResourceLocation("photon","11");
    private static final String PHOTON_ROOT="com.lowdragmc.photon.client.";
    /** 手动生命周期：**每 {@link #REBUILD} 刻销毁旧实例并重起一份**（新的一份立刻起，所以看不出接缝），
     *  因为这个 fx 的粒子一旦生成就留在世界里，对跑着的实例 `setOffset` 不会让它跟手（r237 实测"特效留在原地"）。
     *  附拳关闭 / 换维度 / 切世界时才彻底销毁。 */
    private static final int REBUILD=3;
    private static final Map<Integer,Long> BORN=new HashMap<>();
    private static final Map<Integer,Long> BURNING=new HashMap<>();
    private static final Map<Integer,Object> EFFECTS=new HashMap<>();
    private static final Map<Integer,ResourceLocation> LEVELS=new HashMap<>();
    private static Object fx;private static boolean broken;
    private static String lastNote="";private static long lastNoteAt;

    public static void receive(Network.Effect e){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        BURNING.put(e.owner(),mc.level.getGameTime()+Math.max(10,e.life()));
        note("客户端收到 kind=236 owner="+e.owner());
    }
    /** 每刻：过期的玩家把特效销毁（生命周期由我们自己写）。 */
    public static void tick(){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        long now=mc.level.getGameTime();
        BURNING.entrySet().removeIf(en->now>en.getValue());
        for(Iterator<Map.Entry<Integer,Object>> it=EFFECTS.entrySet().iterator();it.hasNext();){
            var e=it.next();
            boolean local=mc.player!=null&&e.getKey()==mc.player.getId();
            if(local?(Client.progress.fist()&&Client.progress.unlocked()):BURNING.containsKey(e.getKey()))continue;
            kill(e.getKey());
            it.remove();
        }
    }
    public static void clear(){
        for(Integer id:List.copyOf(EFFECTS.keySet()))kill(id);
    }
    private static void kill(int id){
        Object o=EFFECTS.get(id);if(o==null)return;
        try{rcall(o,"setForcedDeath",true);note("销毁特效（玩家 "+id+"）");}catch(Throwable ignored){}
    }
    private static void note(String what){
        var mc=Minecraft.getInstance();long now=mc.level==null?0:mc.level.getGameTime();
        if(what.equals(lastNote)&&now-lastNoteAt<60)return;
        lastNote=what;lastNoteAt=now;com.mojang.logging.LogUtils.getLogger().info("[附拳] {}",what);
    }
    private static void fail(String what,Throwable t){
        String key="ERR:"+what;
        if(!key.equals(lastNote)){lastNote=key;com.mojang.logging.LogUtils.getLogger().warn("[附拳] {} 失败：{}",what,t.toString());}
    }

    // ------------------------------------------------------------ Photon（类名字符串反射）
    private static final Map<String,java.lang.reflect.Method> RM=new HashMap<>();
    private static Object rcall(Object o,String name,Object...args)throws Exception{
        Class<?> c=o instanceof Class<?> cl?cl:o.getClass();
        String key=c.getName()+"/"+name+"/"+args.length;
        var m=RM.get(key);
        if(m==null){
            outer:for(var v:c.getMethods()){if(!v.getName().equals(name)||v.getParameterCount()!=args.length)continue;
                Class<?>[] p=v.getParameterTypes();
                for(int i=0;i<p.length;i++){Class<?> t=p[i];Object a=args[i];
                    if(t==int.class?!(a instanceof Integer):t==boolean.class?!(a instanceof Boolean):t==double.class?!(a instanceof Double):t==float.class?!(a instanceof Float):!t.isInstance(a))continue outer;}
                m=v;break;}
            if(m==null)throw new NoSuchMethodException(key);
            RM.put(key,m);
        }
        return m.invoke(o instanceof Class<?>?null:o,args);
    }
    /** 手动生命周期：到期（{@link #REBUILD} 刻）或还没起过 / 换维度 → 销毁旧的重起一份；否则什么都不做。 */
    private static void ensure(int playerId,Vec3 at,org.joml.Quaternionf rot,ResourceLocation level){
        var mc=Minecraft.getInstance();if(mc.level==null||broken)return;
        try{
            long now=mc.level.getGameTime();
            Object effect=EFFECTS.get(playerId);
            boolean expired=effect!=null&&now-BORN.getOrDefault(playerId,now)>=REBUILD;
            if(effect!=null&&(!level.equals(LEVELS.get(playerId))||expired)){kill(playerId);effect=null;}
            if(effect!=null)return;                                   // 还没到期：不动它（跑着的实例改了也没用）
            if(fx==null)fx=rcall(Class.forName(PHOTON_ROOT+"fx.FXHelper"),"getFX",PUNCH_FX);
            if(fx==null)return;
            BlockPos bp=BlockPos.containing(at);
            effect=Class.forName(PHOTON_ROOT+"fx.BlockEffect")
                .getConstructor(Class.forName(PHOTON_ROOT+"fx.FX"),net.minecraft.world.level.Level.class,BlockPos.class)
                .newInstance(fx,mc.level,bp);
            rcall(effect,"setOffset",at.x-bp.getX()-0.5,at.y-bp.getY()-0.5,at.z-bp.getZ()-0.5);
            rcall(effect,"setRotation",rot);
            rcall(effect,"setAllowMulti",false);
            rcall(effect,"start");
            EFFECTS.put(playerId,effect);LEVELS.put(playerId,level);BORN.put(playerId,now);
            note("起效 11.fx（玩家 "+playerId+"）@ "+String.format(Locale.ROOT,"%.2f/%.2f/%.2f",at.x,at.y,at.z));
        }catch(ClassNotFoundException|NoClassDefFoundError e){broken=true;note("未安装 Photon，附拳特效跳过");}
        catch(Throwable t){fail("起效 11.fx",t);}
    }

    @Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){
            for(String skin:e.getSkins()){PlayerRenderer renderer=e.getSkin(skin);if(renderer!=null)renderer.addLayer(new Punch(renderer));}
        }
    }

    /** 第三人称/别人看：读出手部世界坐标 + 手臂姿态，交给 meleepunch。 */
    private static final class Punch extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
        Punch(PlayerRenderer parent){super(parent);}
        @Override public void render(PoseStack ps,net.minecraft.client.renderer.MultiBufferSource source,int light,AbstractClientPlayer player,float limb,float amount,float partial,float age,float yaw,float pitch){
            if(!BURNING.containsKey(player.getId()))return;
            try{
                var model=getParentModel();
                handAt(ps,model.rightArm,player.getId(),player);
            }catch(Throwable t){fail("第三人称取手部姿态",t);}
        }
        /** 手臂骨骼 → 手（+9/16 格；这套 PoseStack 里 **1 单位 = 1 格**、1 像素 = 1/16 格）
         *  → 世界坐标 + 姿态四元数；再沿视线前移一点，避免特效糊在身后。 */
        private static void handAt(PoseStack ps,ModelPart arm,int playerId,AbstractClientPlayer player){
            ps.pushPose();arm.translateAndRotate(ps);ps.translate(0,9/16.0,0);
            var m=ps.last().pose();
            var mc=Minecraft.getInstance();
            var cam=mc.gameRenderer.getMainCamera().getPosition();
            Vec3 at=cam.add(m.m30(),m.m31(),m.m32()).add(player.getLookAngle().scale(0.25));
            var rot=new org.joml.Matrix3f(m).getNormalizedRotation(new org.joml.Quaternionf());
            if(mc.level!=null)ensure(playerId,at,rot,mc.level.dimension().location());
            ps.popPose();
        }
    }
    /** 第一人称：自己的手不经玩家模型，按相机基向量估算手的世界坐标，姿态取相机朝向。 */
    @SubscribeEvent public static void scene(RenderLevelStageEvent e){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||mc.level==null||!Client.progress.fist())return;
        if(!mc.options.getCameraType().isFirstPerson())return;
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        try{
            var cam=e.getCamera();
            Vec3 look=new Vec3(cam.getLookVector()),right=new Vec3(cam.getLeftVector()).scale(-1),up=new Vec3(cam.getUpVector());
            Vec3 at=cam.getPosition().add(look.scale(0.60)).add(right.scale(0.30)).subtract(up.scale(0.34));
            ensure(p.getId(),at,new org.joml.Quaternionf(cam.rotation()),mc.level.dimension().location());
        }catch(Throwable t){fail("第一人称起效",t);}
    }
    private FistFlameClient(){}
}
