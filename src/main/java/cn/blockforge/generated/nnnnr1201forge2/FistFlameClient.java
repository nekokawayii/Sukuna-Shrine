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
 *  <p>三条来自实测的要求（r239/r240）：</p>
 *  <ol>
 *    <li>**一个实例一直存在、每刻只更新位置/朝向**（这个 fx 没有生命周期），不要反复创建新的；</li>
 *    <li>**左右手各一份**；</li>
 *    <li>**渲染路径只记录坐标、不碰 Photon**（r239 在渲染里销毁特效直接 `ConcurrentModificationException` 崩了一帧）；
 *        起效/更新/销毁全部放到客户端 `tick()` 里做，回收用"**{@link #GRACE} 刻没刷新才销毁**"的宽限判定，
 *        既不会因为状态抖动反复重建，也能在附拳关闭后自动收掉。</li>
 *  </ol> */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class FistFlameClient {
    /** 服务端广播的特效号：附拳期间每 15 刻一条（带 entityId 与寿命）。 */
    public static final int FX=236;
    private static final ResourceLocation PUNCH_FX=new ResourceLocation("photon","11");
    private static final String PHOTON_ROOT="com.lowdragmc.photon.client.";
    /** 宽限：这么久没被刷新才回收（约 2 秒）。 */
    private static final int GRACE=40;
    private record Where(Vec3 at,org.joml.Quaternionf rot,ResourceLocation level,long seen){}
    private static final Map<Integer,Long> BURNING=new HashMap<>();
    /** 渲染路径写、tick 路径读：key = 玩家id*2+手（0右 1左）。 */
    private static final Map<Integer,Where> WANT=new HashMap<>();
    /** 已经在跑的特效实例（真正一直存在的那一份）。 */
    private static final Map<Integer,Object> EFFECTS=new HashMap<>();
    private static final Map<Integer,Long> SEEN=new HashMap<>();
    private static Object fx;private static boolean broken;
    private static String lastNote="";private static long lastNoteAt;

    public static void receive(Network.Effect e){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        BURNING.put(e.owner(),mc.level.getGameTime()+Math.max(10,e.life()));
    }
    /** 唯一动 Photon 的地方：先把本轮要求起效的补上/更新，再统一回收过期的（先收集再删，避免并发修改）。 */
    public static void tick(){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        long now=mc.level.getGameTime();
        BURNING.entrySet().removeIf(en->now>en.getValue());
        for(var e:List.copyOf(WANT.entrySet()))apply(e.getKey(),e.getValue(),now);
        List<Integer> dead=new ArrayList<>();
        for(Integer k:EFFECTS.keySet())if(now-SEEN.getOrDefault(k,0L)>GRACE)dead.add(k);
        for(Integer k:dead)kill(k);
        for(Integer k:List.copyOf(WANT.keySet()))if(now-WANT.get(k).seen()>GRACE)WANT.remove(k);
    }
    public static void clear(){
        for(Integer k:List.copyOf(EFFECTS.keySet()))kill(k);
        EFFECTS.clear();SEEN.clear();WANT.clear();BURNING.clear();
    }
    /** 渲染路径调用：只登记"这只手现在在哪、什么朝向"。 */
    private static void want(int key,Vec3 at,org.joml.Quaternionf rot){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        WANT.put(key,new Where(at,rot,mc.level.dimension().location(),mc.level.getGameTime()));
    }
    private static void apply(int key,Where w,long now){
        if(broken)return;
        try{
            Object effect=EFFECTS.get(key);
            if(effect!=null&&!w.level().equals(dimensionOf(key))){kill(key);effect=null;}
            BlockPos bp=BlockPos.containing(w.at());
            var off=new org.joml.Vector3f((float)(w.at().x-bp.getX()-0.5),(float)(w.at().y-bp.getY()-0.5),(float)(w.at().z-bp.getZ()-0.5));
            if(effect==null){
                if(fx==null)fx=rcall(Class.forName(PHOTON_ROOT+"fx.FXHelper"),"getFX",PUNCH_FX);
                if(fx==null)return;
                var mc=Minecraft.getInstance();if(mc.level==null)return;
                effect=Class.forName(PHOTON_ROOT+"fx.BlockEffect")
                    .getConstructor(Class.forName(PHOTON_ROOT+"fx.FX"),net.minecraft.world.level.Level.class,BlockPos.class)
                    .newInstance(fx,mc.level,bp);
                rcall(effect,"setOffset",off);rcall(effect,"setRotation",w.rot());
                rcall(effect,"setAllowMulti",false);
                rcall(effect,"start");
                EFFECTS.put(key,effect);LEVEL.put(key,w.level());
                note("起效 11.fx（"+describe(key)+"）");
            }else{
                rcall(effect,"setOffset",off);       // 一直在跑：只更新位置
                rcall(effect,"setRotation",w.rot()); // 与朝向
            }
            SEEN.put(key,now);
        }catch(ClassNotFoundException|NoClassDefFoundError e){broken=true;note("未安装 Photon，附拳特效跳过");}
        catch(Throwable t){fail("更新 11.fx",t);}
    }
    private static final Map<Integer,ResourceLocation> LEVEL=new HashMap<>();
    private static ResourceLocation dimensionOf(int key){return LEVEL.get(key);}
    private static void kill(int key){
        Object o=EFFECTS.remove(key);SEEN.remove(key);LEVEL.remove(key);
        if(o==null)return;
        try{rcall(o,"setForcedDeath",true);note("销毁特效（"+describe(key)+"）");}catch(Throwable ignored){}
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

    @Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){
            for(String skin:e.getSkins()){PlayerRenderer renderer=e.getSkin(skin);if(renderer!=null)renderer.addLayer(new Punch(renderer));}
        }
    }

    /** 第三人称/别人看：只读出左右手的位置与姿态，登记给 tick（不碰 Photon）。 */
    private static final class Punch extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
        Punch(PlayerRenderer parent){super(parent);}
        @Override public void render(PoseStack ps,net.minecraft.client.renderer.MultiBufferSource source,int light,AbstractClientPlayer player,float limb,float amount,float partial,float age,float yaw,float pitch){
            if(!BURNING.containsKey(player.getId()))return;
            try{
                var model=getParentModel();
                Vec3 forward=player.getLookAngle().scale(0.25);
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
            want(playerId*2+hand,at,rot);
            ps.popPose();
        }
    }

    /** 第一人称：手不经玩家模型，按相机基向量估算双手，朝向取相机姿态；同样只登记。 */
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
            want(p.getId()*2,base.add(right.scale(0.30)),rot);
            want(p.getId()*2+1,base.subtract(right.scale(0.30)),rot);
        }catch(Throwable t){fail("第一人称取手部位置",t);}
    }
    private FistFlameClient(){}
}
