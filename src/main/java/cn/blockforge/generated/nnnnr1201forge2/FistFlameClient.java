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
    /** 用户提供的拳击特效（放在 assets/sukuna/fx/meleepunch.fx，gzip NBT）。 */
    private static final ResourceLocation PUNCH_FX=new ResourceLocation("sukuna","meleepunch");
    private static final String PHOTON_ROOT="com.lowdragmc.photon.client.";
    private static final int REFRESH=10;
    private static final Map<Integer,Long> BURNING=new HashMap<>();
    private static final Map<Integer,Object> EFFECTS=new HashMap<>();
    private static final Map<Integer,Long> SPAWNED=new HashMap<>();
    private static Object fx;private static boolean broken;
    private static String lastNote="";private static long lastNoteAt;

    public static void receive(Network.Effect e){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        BURNING.put(e.owner(),mc.level.getGameTime()+Math.max(10,e.life()));
        note("客户端收到 kind=236 owner="+e.owner());
    }
    /** 每刻：过期的玩家把特效收掉（`setForcedDeath`），并清理登记表。 */
    public static void tick(){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        long now=mc.level.getGameTime();
        BURNING.entrySet().removeIf(en->now>en.getValue());
        for(Iterator<Map.Entry<Integer,Object>> it=EFFECTS.entrySet().iterator();it.hasNext();){
            var e=it.next();
            boolean local=mc.player!=null&&e.getKey()==mc.player.getId();
            if(local?Client.progress.fist():BURNING.containsKey(e.getKey()))continue;
            try{rcall(e.getValue(),"setForcedDeath",true);}catch(Throwable ignored){}
            SPAWNED.remove(e.getKey());it.remove();
        }
    }
    public static void clear(){
        for(Object o:EFFECTS.values())try{rcall(o,"setForcedDeath",true);}catch(Throwable ignored){}
        EFFECTS.clear();SPAWNED.clear();BURNING.clear();
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
    /** 在给定世界位置与朝向上刷新一份 meleepunch 特效（每 {@link #REFRESH} 刻一次，只留一份）。 */
    private static void spawn(int playerId,Vec3 at,org.joml.Quaternionf rot){
        var mc=Minecraft.getInstance();if(mc.level==null||broken)return;
        long now=mc.level.getGameTime();
        Long last=SPAWNED.get(playerId);
        if(last!=null&&now-last<REFRESH)return;
        SPAWNED.put(playerId,now);
        try{
            if(fx==null)fx=rcall(Class.forName(PHOTON_ROOT+"fx.FXHelper"),"getFX",PUNCH_FX);
            if(fx==null)return;
            Object old=EFFECTS.remove(playerId);
            if(old!=null)try{rcall(old,"setForcedDeath",true);}catch(Throwable ignored){}
            BlockPos bp=BlockPos.containing(at);
            Object effect=Class.forName(PHOTON_ROOT+"fx.BlockEffect")
                .getConstructor(Class.forName(PHOTON_ROOT+"fx.FX"),net.minecraft.world.level.Level.class,BlockPos.class)
                .newInstance(fx,mc.level,bp);
            rcall(effect,"setOffset",at.x-bp.getX()-0.5,at.y-bp.getY()-0.5,at.z-bp.getZ()-0.5);
            rcall(effect,"setRotation",rot);
            rcall(effect,"setAllowMulti",false);
            rcall(effect,"start");
            EFFECTS.put(playerId,effect);
            note("已起效 meleepunch（玩家 "+playerId+"）");
        }catch(ClassNotFoundException|NoClassDefFoundError e){broken=true;note("未安装 Photon，附拳特效跳过");}
        catch(Throwable t){fail("起效 meleepunch",t);}
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
                handAt(ps,model.rightArm,player.getId());
            }catch(Throwable t){fail("第三人称取手部姿态",t);}
        }
        /** 手臂骨骼 → 手（像素 +9）→ 世界坐标 + 姿态四元数 → 起效。 */
        private static void handAt(PoseStack ps,ModelPart arm,int playerId){
            ps.pushPose();arm.translateAndRotate(ps);ps.translate(0,9/16.0,0);
            var m=ps.last().pose();
            var cam=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Vec3 at=cam.add(m.m30(),m.m31(),m.m32());
            var rot=new org.joml.Matrix3f(m).getNormalizedRotation(new org.joml.Quaternionf());
            spawn(playerId,at,rot);
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
            Vec3 at=cam.getPosition().add(look.scale(0.50)).add(right.scale(0.30)).subtract(up.scale(0.34));
            spawn(p.getId(),at,new org.joml.Quaternionf(cam.rotation()));
        }catch(Throwable t){fail("第一人称起效",t);}
    }
    private FistFlameClient(){}
}
