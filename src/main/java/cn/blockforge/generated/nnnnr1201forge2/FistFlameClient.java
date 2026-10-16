package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 「咒力附拳」＝挂在**双手**上的用户特效 `ldlib/assets/photon/fx/11.fx`。
 *
 *  <p>用户要求（r239）：**一个实例一直存在、每刻更新它的位置/朝向**（这个 fx 没有生命周期，可以一直存在），
 *  不要"反复创建新的"。所以：开启时每只手起**一份**，之后每刻调 `setOffset(Vector3f)` + `setRotation(Quaternionf)`；
 *  只有附拳关闭 / 换维度 / 切世界才 `setForcedDeath(true)` 销毁。</p>
 *
 *  <p>位置来源：第三人称/别人看＝玩家渲染层里手臂骨骼矩阵（`arm.translateAndRotate` → `translate(0,+9/16,0)` 到手，
 *  再 `相机位置 + m30/m31/m32` 得世界坐标，旋转取 `Matrix3f.getNormalizedRotation`）；第一人称＝相机基向量估算。
 *  注意这套 PoseStack 里 **1 单位 = 1 格、1 像素 = 1/16 格**，且**模型 +Y 向下**。</p> */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class FistFlameClient {
    /** 服务端广播的特效号：附拳期间每 15 刻一条（带 entityId 与寿命）。 */
    public static final int FX=236;
    /** 用户指定的拳击特效（LDLib 文件系统资源目录里的 `photon:fx/11.fx`）。 */
    private static final ResourceLocation PUNCH_FX=new ResourceLocation("photon","11");
    private static final String PHOTON_ROOT="com.lowdragmc.photon.client.";
    private static final Map<Integer,Long> BURNING=new HashMap<>();
    /** key = 玩家 id * 2 + 手（0=右手 1=左手），value = Photon 特效实例（**一直存在，只更新位置**）。 */
    private static final Map<Integer,Object> EFFECTS=new HashMap<>();
    private static final Map<Integer,ResourceLocation> LEVELS=new HashMap<>();
    private static Object fx;private static boolean broken;
    private static String lastNote="";private static long lastNoteAt;

    public static void receive(Network.Effect e){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        BURNING.put(e.owner(),mc.level.getGameTime()+Math.max(10,e.life()));
        note("客户端收到 kind=236 owner="+e.owner());
    }
    /** 每刻：附拳关闭/广播过期 → 销毁特效（生命周期由我们显式掌管）。 */
    public static void tick(){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        long now=mc.level.getGameTime();
        BURNING.entrySet().removeIf(en->now>en.getValue());
        for(Iterator<Map.Entry<Integer,Object>> it=EFFECTS.entrySet().iterator();it.hasNext();){
            var e=it.next();int pid=e.getKey()/2;
            boolean local=mc.player!=null&&pid==mc.player.getId();
            if(local?(Client.progress.fist()&&Client.progress.unlocked()):BURNING.containsKey(pid))continue;
            kill(e.getKey());
            it.remove();
        }
    }
    public static void clear(){
        for(Integer k:List.copyOf(EFFECTS.keySet()))kill(k);
        EFFECTS.clear();LEVELS.clear();BURNING.clear();
    }
    private static void kill(int key){
        Object o=EFFECTS.get(key);if(o==null)return;
        try{rcall(o,"setForcedDeath",true);note("销毁特效（"+describe(key)+"）");}catch(Throwable ignored){}
        EFFECTS.remove(key);LEVELS.remove(key);
    }
    private static String describe(int key){return "玩家 "+(key/2)+(key%2==1?" 左手":" 右手");}
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
    /** **一个实例一直存在**：没起过就起一份，起过就只更新位置与朝向（不重复创建）。 */
    private static void upsert(int key,Vec3 at,org.joml.Quaternionf rot){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        try{
            ResourceLocation level=mc.level.dimension().location();
            Object effect=EFFECTS.get(key);
            if(effect!=null&&!level.equals(LEVELS.get(key))){kill(key);effect=null;}
            BlockPos bp=BlockPos.containing(at);
            var off=new org.joml.Vector3f((float)(at.x-bp.getX()-0.5),(float)(at.y-bp.getY()-0.5),(float)(at.z-bp.getZ()-0.5));
            if(effect==null){
                if(fx==null)fx=rcall(Class.forName(PHOTON_ROOT+"fx.FXHelper"),"getFX",PUNCH_FX);
                if(fx==null)return;
                effect=Class.forName(PHOTON_ROOT+"fx.BlockEffect")
                    .getConstructor(Class.forName(PHOTON_ROOT+"fx.FX"),net.minecraft.world.level.Level.class,BlockPos.class)
                    .newInstance(fx,mc.level,bp);
                rcall(effect,"setOffset",off);rcall(effect,"setRotation",rot);
                rcall(effect,"setAllowMulti",false);
                rcall(effect,"start");
                EFFECTS.put(key,effect);LEVELS.put(key,level);
                note("起效 11.fx（"+describe(key)+"）@ "+String.format(Locale.ROOT,"%.2f/%.2f/%.2f",at.x,at.y,at.z));
                return;
            }
            rcall(effect,"setOffset",off);      // 一直在跑：只更新位置
            rcall(effect,"setRotation",rot);    // 与朝向
        }catch(ClassNotFoundException|NoClassDefFoundError e){broken=true;note("未安装 Photon，附拳特效跳过");}
        catch(Throwable t){fail("更新 11.fx",t);}
    }

    @Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){
            for(String skin:e.getSkins()){PlayerRenderer renderer=e.getSkin(skin);if(renderer!=null)renderer.addLayer(new Punch(renderer));}
        }
    }

    /** 第三人称/别人看：左右手各读一次（位置 + 姿态）。 */
    private static final class Punch extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
        Punch(PlayerRenderer parent){super(parent);}
        @Override public void render(PoseStack ps,net.minecraft.client.renderer.MultiBufferSource source,int light,AbstractClientPlayer player,float limb,float amount,float partial,float age,float yaw,float pitch){
            if(!BURNING.containsKey(player.getId()))return;
            try{
                var model=getParentModel();
                Vec3 forward=player.getLookAngle().scale(0.25);        // 沿视线前移，避免糊在身后
                handAt(ps,model.rightArm,player.getId(),0,forward);
                handAt(ps,model.leftArm,player.getId(),1,forward);
            }catch(Throwable t){fail("第三人称取手部姿态",t);}
        }
        private static void handAt(PoseStack ps,ModelPart arm,int playerId,int hand,Vec3 forward){
            ps.pushPose();arm.translateAndRotate(ps);ps.translate(0,9/16.0,0);
            var m=ps.last().pose();
            var cam=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Vec3 at=cam.add(m.m30(),m.m31(),m.m32()).add(forward);
            var rot=new org.joml.Matrix3f(m).getNormalizedRotation(new org.joml.Quaternionf());
            upsert(playerId*2+hand,at,rot);
            ps.popPose();
        }
    }

    /** 第一人称：手不经玩家模型，按相机基向量估算双手，朝向取相机姿态。 */
    @SubscribeEvent public static void scene(RenderLevelStageEvent e){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||mc.level==null||!Client.progress.fist())return;
        if(!mc.options.getCameraType().isFirstPerson())return;
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        try{
            var cam=e.getCamera();
            Vec3 look=new Vec3(cam.getLookVector()),right=new Vec3(cam.getLeftVector()).scale(-1),up=new Vec3(cam.getUpVector());
            var rot=new org.joml.Quaternionf(cam.rotation());
            Vec3 base=cam.getPosition().add(look.scale(0.60)).subtract(up.scale(0.34));
            upsert(p.getId()*2,base.add(right.scale(0.30)),rot);
            upsert(p.getId()*2+1,base.subtract(right.scale(0.30)),rot);
        }catch(Throwable t){fail("第一人称起效",t);}
    }
    private FistFlameClient(){}
}
