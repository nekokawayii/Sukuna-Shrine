package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.systems.RenderSystem;
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
    /** 每只手臂上布几个咒力发射点（肩→拳均匀取样，读的是手臂模型方块的实际范围）。 */
    private static final int ARM_POINTS=3;
    /** 拖尾滞后：按实体位移往回拖多少刻（以及最大拖出多少格），挥拳时咒力会拖在后面。 */
    private static final double DRAG_TICKS=1.6,DRAG_MAX=0.35;
    // ---------- 「包覆手臂的咒力」自绘层（复刻 jujutsu-vfx 那套观感：逐帧自绘、裹住整条手臂、随出拳放大） ----------
    private static final ResourceLocation GLOW=new ResourceLocation("sukuna","textures/effect/glow.png");
    private static final ResourceLocation AURA_FX_TEXTURE=GLOW;
    private static final int AURA_SEG=5;                       // 每条手臂几段
    private static final double AURA_WIDTH=0.42,AURA_LEN=0.24; // 单段尺寸（格）；手臂本体只有 0.25 格宽，所以这是"裹一圈"
    private static final int AURA_OUT=0x73C81414,AURA_IN=0x8CFF6A72,AURA_CORE=0x66FFD0D6;
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
    private static String describe(int key){int hand=(key/8)%2;return "玩家 "+(key/8/2)+(hand==1?" 左手":" 右手")+" 第"+(key%8+1)+"点";}
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
                // 拖尾滞后：按实体位移往回拖（上限 DRAG_MAX 格），手一挥咒力就落在后面
                Vec3 d=player.getDeltaMovement();
                Vec3 drag=new Vec3(-d.x*DRAG_TICKS,-d.y*DRAG_TICKS*0.4,-d.z*DRAG_TICKS);
                if(drag.length()>DRAG_MAX)drag=drag.normalize().scale(DRAG_MAX);
                Vec3 forward=player.getLookAngle().scale(0.25).add(drag);
                armPoints(ps,model.rightArm,player.getId(),0,forward);
                armPoints(ps,model.leftArm,player.getId(),1,forward);
                // 自绘的包覆层：两条手臂各画一遍（用 render 事件给的 buffer，不碰 Photon 状态）
                var vb=source.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(AURA_FX_TEXTURE));
                float power=power(player,partial);
                aura(ps,vb,model.rightArm,light,power);
                aura(ps,vb,model.leftArm,light,power);
            }catch(Throwable t){fail("第三人称取手部姿态",t);}
        }
        /** 出拳强度曲线：静止时轻轻起伏，挥拳（`getAttackAnim`）时明显放大。 */
        private static float power(AbstractClientPlayer player,float partial){
            float t=player.tickCount+partial;
            float swing=player.getAttackAnim(partial);
            return (float)(0.70+0.18*Math.sin(t*0.16)+0.55*swing*swing);
        }
        /** 包覆层：沿手臂每段画两张交叉的柔光面片（XY 面 + ZY 面），从任何角度看都"裹"在手臂上。 */
        private static void aura(PoseStack ps,com.mojang.blaze3d.vertex.VertexConsumer vb,ModelPart arm,int light,float power){
            ps.pushPose();arm.translateAndRotate(ps);
            var pose=ps.last();
            for(int s=0;s<AURA_SEG;s++){
                float y=1.5f+ (s/(float)Math.max(1,AURA_SEG-1))*7.0f;      // 像素：前臂→拳
                float w=(float)(AURA_WIDTH*power),h=(float)(AURA_LEN*power);
                float fy=y/16f;
                quad(pose,vb,light,-w,fy-h,0,w,fy+h,0,AURA_OUT,AURA_IN,AURA_CORE);
                quad(pose,vb,light,0,fy-h,-w,0,fy+h,w,AURA_OUT,AURA_IN,AURA_CORE);
            }
            ps.popPose();
        }
        /** 一张柔光面片：三层（外暗红 → 中亮粉 → 内芯），UV 铺满整张 glow 保留柔边。 */
        private static void quad(PoseStack.Pose pose,com.mojang.blaze3d.vertex.VertexConsumer vb,int light,float x0,float y0,float z0,float x1,float y1,float z1,int outer,int mid,int core){
            layer(pose,vb,light,x0,y0,z0,x1,y1,z1,outer,1.35f);
            layer(pose,vb,light,x0,y0,z0,x1,y1,z1,mid,1.0f);
            layer(pose,vb,light,x0,y0,z0,x1,y1,z1,core,0.6f);
        }
        private static void layer(PoseStack.Pose pose,com.mojang.blaze3d.vertex.VertexConsumer vb,int light,float x0,float y0,float z0,float x1,float y1,float z1,int color,float scale){
            float cx=(x0+x1)/2,cy=(y0+y1)/2,cz=(z0+z1)/2;
            float hx=(x1-x0)/2*scale,hy=(y1-y0)/2*scale,hz=(z1-z0)/2*scale;
            vertex(pose,vb,light,cx-hx,cy+hy,cz-hz,color,0,0);
            vertex(pose,vb,light,cx+hx,cy+hy,cz+hz,color,1,0);
            vertex(pose,vb,light,cx+hx,cy-hy,cz+hz,color,1,1);
            vertex(pose,vb,light,cx-hx,cy-hy,cz-hz,color,0,1);
        }
        private static void vertex(PoseStack.Pose pose,com.mojang.blaze3d.vertex.VertexConsumer vb,int light,float x,float y,float z,int color,float u,float v){
            vb.vertex(pose.pose(),x,y,z).color(color).uv(u,v).overlayCoords(4194304).uv2(light).normal(pose.normal(),0,0,1).endVertex();
        }
        /** 沿**整条手臂**均匀取 ARM_POINTS 个点（玩家手臂箱体固定为肩下 -2~10 像素，取 1~9.5 像素＝前臂到拳头）。
         *  注：`ModelPart.cubes` 是 private，那个参考模组是带 Mixin/AT 才读到方块几何的；玩家模型尺寸固定，直接用像素范围更稳。 */
        private static void armPoints(PoseStack ps,ModelPart arm,int playerId,int hand,Vec3 offset){
            float from=1f,to=9.5f;
            for(int i=0;i<ARM_POINTS;i++){
                float t=ARM_POINTS==1?0.85f:(i+0.5f)/ARM_POINTS;      // 0.17/0.5/0.83 → 前臂到拳
                float y=from+(to-from)*t;
                ps.pushPose();arm.translateAndRotate(ps);ps.translate(0,y/16.0f,0);
                var m=ps.last().pose();
                var cam=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                Vec3 at=cam.add(m.m30(),m.m31(),m.m32()).add(offset);
                var rot=new org.joml.Matrix3f(m).getNormalizedRotation(new org.joml.Quaternionf());
                want((playerId*2+hand)*8+i,at,rot);
                ps.popPose();
            }
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
            Vec3 hand=cam.getPosition().add(look.scale(0.60)).subtract(up.scale(0.34));
            Vec3 elbow=cam.getPosition().add(look.scale(0.34)).subtract(up.scale(0.62));
            Vec3 d=p.getDeltaMovement();
            Vec3 drag=new Vec3(-d.x*DRAG_TICKS,-d.y*DRAG_TICKS*0.4,-d.z*DRAG_TICKS);
            if(drag.length()>DRAG_MAX)drag=drag.normalize().scale(DRAG_MAX);
            for(int hand1=0;hand1<2;hand1++){
                Vec3 side=hand1==0?right.scale(0.30):right.scale(-0.30);
                for(int i=0;i<ARM_POINTS;i++){
                    double t=(i+0.5)/ARM_POINTS;
                    Vec3 at=elbow.lerp(hand,t).add(side).add(drag);
                    want((p.getId()*2+hand1)*8+i,at,rot);
                }
            }
            // 第一人称的自绘包覆层：沿"肘→拳"画一串柔光面片（贴图通道，尺寸以格为单位）
            float power=(float)(0.70+0.18*Math.sin((p.tickCount+e.getPartialTick())*0.16)+0.55*p.getAttackAnim(e.getPartialTick())*p.getAttackAnim(e.getPartialTick()));
            RenderSystem.enableDepthTest();RenderSystem.disableCull();RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0,AURA_FX_TEXTURE);
            BufferBuilder b=Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            for(int hand1=0;hand1<2;hand1++){
                Vec3 side=hand1==0?right.scale(0.30):right.scale(-0.30);
                for(int s=0;s<AURA_SEG;s++){
                    double t=(s+0.5)/AURA_SEG;
                    Vec3 at=elbow.lerp(hand,t).add(side).add(drag);
                    double w=AURA_WIDTH*power*0.9,h=AURA_LEN*power;
                    worldQuad(b,at,right,up,w,h,AURA_OUT,1.35);   // 外层
                    worldQuad(b,at,right,up,w,h,AURA_IN,1.0);     // 中层
                    worldQuad(b,at,right,up,w,h,AURA_CORE,0.6);   // 内芯
                }
            }
            BufferUploader.drawWithShader(b.end());
            RenderSystem.enableCull();RenderSystem.disableBlend();
        }catch(Throwable t){fail("第一人称取手部位置",t);}
    }
    /** 第一人称世界层里的一张柔光面片（面向相机，贴图通道，尺寸单位＝格）。 */
    private static void worldQuad(BufferBuilder b,Vec3 c,Vec3 right,Vec3 up,double w,double h,int color,double scale){
        Vec3 u=up.scale(h*scale),r=right.scale(w*scale);
        b.vertex(c.x-r.x-u.x,c.y-r.y-u.y,c.z-r.z-u.z).uv(0,0).color(color).endVertex();
        b.vertex(c.x+r.x-u.x,c.y+r.y-u.y,c.z+r.z-u.z).uv(1,0).color(color).endVertex();
        b.vertex(c.x+r.x+u.x,c.y+r.y+u.y,c.z+r.z+u.z).uv(1,1).color(color).endVertex();
        b.vertex(c.x-r.x+u.x,c.y-r.y+u.y,c.z-r.z+u.z).uv(0,1).color(color).endVertex();
    }
    private FistFlameClient(){}
}
