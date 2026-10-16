package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 「咒力附拳」的表现层：**双手上飘荡的红色半透明咒力火焰**，直接绑在手部骨骼上（所有玩家都看得见）。
 *
 *  <p>两套叠起来用，保证一定看得见：</p>
 *  <ol>
 *    <li>**手部面片**（主力）：渲染层里 {@code arm.translateAndRotate(ps)} → {@code translate(0,9/16,0)} 挪到手，
 *        再画若干张红色半透明火苗面片（底宽顶尖、随时间上下飘/左右摆/明暗闪）。
 *        模型空间 **+Y 是向下**，所以"从肩到手"是 **+9/16**（写成负数会跑到头/耳朵上）。</li>
 *    <li>**PhotonFx 发光**（附加）：同一位置每 2 刻点一簇 {@code blobs} 火团 + {@code spark} 加法发光，
 *        给火焰加 bloom 光晕；没装 Photon 时静默跳过。</li>
 *  </ol>
 *  <p>第一人称的手不经过玩家模型，所以在 {@code RenderHandEvent} 里按手持姿势再画一份同样的火苗。</p> */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class FistFlameClient {
    /** 服务端广播的特效号：附拳期间每 15 刻一条，带 entityId 与寿命。 */
    public static final int FX=236;
    private static final Map<Integer,Long> BURNING=new HashMap<>();
    private static final Map<Integer,Vec3[]> HANDS=new HashMap<>();
    private static final int FLAMES=7,SPAWN_INTERVAL=2;
    private static final int DEEP=0xB00A0A0A,HOT=0xE6E02418,RED=0xCFB01810;
    private static final int BLOOM_RED=0xFF8A1208;
    private static final String GLOW="textures/effect/glow.png";

    public static void receive(Network.Effect e){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        BURNING.put(e.owner(),mc.level.getGameTime()+Math.max(10,e.life()));
        note("客户端收到 kind=236 owner="+e.owner()+" life="+e.life());
    }
    /** 验证埋点：同一条消息 3 秒内只写一次，避免刷屏。 */
    private static String lastNote="";
    private static long lastNoteAt;
    private static void note(String what){
        var mc=Minecraft.getInstance();long now=mc.level==null?0:mc.level.getGameTime();
        if(what.equals(lastNote)&&now-lastNoteAt<60)return;
        lastNote=what;lastNoteAt=now;
        com.mojang.logging.LogUtils.getLogger().info("[附拳] {}",what);
    }
    private static void fail(String what,Throwable t){
        String key="ERR:"+what;
        if(!key.equals(lastNote)){lastNote=key;com.mojang.logging.LogUtils.getLogger().warn("[附拳] {} 失败：{}",what,t.toString());}
    }
    /** 每刻：清过期 + 每 2 刻在手部位置点一簇 PhotonFx（面片由渲染层每帧画）。 */
    public static void tick(){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        long now=mc.level.getGameTime();BURNING.entrySet().removeIf(en->now>en.getValue());
        if(now%SPAWN_INTERVAL!=0)return;
        for(var entry:HANDS.entrySet()){
            boolean local=mc.player!=null&&entry.getKey()==mc.player.getId();
            if(local?!Client.progress.fist():!burning(entry.getKey()))continue;
            for(Vec3 h:entry.getValue())if(h!=null)photon(h);
        }
    }
    public static void clear(){BURNING.clear();HANDS.clear();}
    private static boolean burning(int id){Long until=BURNING.get(id);return until!=null&&Minecraft.getInstance().level!=null&&Minecraft.getInstance().level.getGameTime()<=until;}

    /** 附加的 PhotonFx 发光：暗红火团（轻微上升）+ 加法发光核（bloom）。 */
    private static void photon(Vec3 hand){
        try{
            PhotonFx.blobs(hand,0.13,0.10,0.07,0xFF5512,BLOOM_RED,0.06,-0.012,5,1,9);
            PhotonFx.spark(hand,0.07,0.11,0xFF2A10,BLOOM_RED,GLOW,true,1,7,4,0.25);
        }catch(Throwable ignored){}
    }

    @Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){
            for(String skin:e.getSkins()){PlayerRenderer renderer=e.getSkin(skin);if(renderer!=null)renderer.addLayer(new Flames(renderer));}
        }
    }

    /** 第三人称/别人看：把火苗绑在左右手骨骼上。 */
    private static final class Flames extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
        Flames(PlayerRenderer parent){super(parent);}
        @Override public void render(PoseStack ps,MultiBufferSource source,int light,AbstractClientPlayer player,float limb,float amount,float partial,float age,float yaw,float pitch){
            if(!burning(player.getId()))return;
            note("渲染层开始绘制 owner="+player.getId());
            try{
                var model=getParentModel();
                var b=source.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation()));
                float t=age+partial;
                Vec3[] store=HANDS.computeIfAbsent(player.getId(),k->new Vec3[2]);
                hand(ps,b,light,model.rightArm,t,store,0);
                hand(ps,b,light,model.leftArm,t+1.7f,store,1);
            }catch(Throwable t){fail("手部面片绘制",t);}
        }
        /** 一条手臂：挪到手 → 画一簇火苗，同时把该点世界坐标记下来给 PhotonFx。 */
        private static void hand(PoseStack ps,VertexConsumer b,int light,ModelPart arm,float t,Vec3[] store,int slot){
            ps.pushPose();arm.translateAndRotate(ps);
            ps.translate(0,9/16.0,0);                                     // 模型空间 +Y 向下：+9/16 才是手
            if(store!=null)store[slot]=worldFromPose(ps);
            ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));      // 翻转后火苗朝上
            var pose=ps.last();
            for(int i=0;i<FLAMES;i++){
                float ph=i*1.3f;
                double drift=Math.sin(t*0.18+ph)*0.065,sway=Math.cos(t*0.13+ph*1.4)*0.055,h=0.15+0.07*Math.abs(Math.sin(t*0.22+ph));
                float x=(float)(sway+(i%3-1)*0.065),z=(float)(drift+(i%2)*0.028),half=0.070f+0.020f*(i%2);
                int alpha=(int)((0.45+0.45*Math.abs(Math.sin(t*0.25+ph)))*255);
                flame(pose,b,light,x,half,z,h,alpha);
            }
            ps.popPose();
        }
    }

    /** 第一人称：自己的手走手持通路，不经过玩家模型，这里按手持姿势再画一份，并估算世界坐标给 PhotonFx。 */
    @SubscribeEvent public static void hand(RenderHandEvent e){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||mc.level==null||!Client.progress.fist()||e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND)return;
        note("第一人称手部绘制（本机附拳="+Client.progress.fist()+"）");
        try{
        float t=mc.level.getGameTime()+e.getPartialTick();
        var ps=e.getPoseStack();ps.pushPose();
        // 不再猜偏移：直接用手持事件矩阵的原点（那就是这只手的位置），只沿局部 +Y 稍微抬起一点点
        ps.translate(0,-0.02,0);
        var cam=mc.gameRenderer.getMainCamera();
        Vec3 eye=cam.getPosition(),look=new Vec3(cam.getLookVector()),right=new Vec3(cam.getLeftVector()).scale(-1),up=new Vec3(cam.getUpVector());
        HANDS.put(p.getId(),new Vec3[]{eye.add(look.scale(0.42)).add(right.scale(0.34)).subtract(up.scale(0.24)),null});
        ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
        var b=e.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(p.getSkinTextureLocation()));
        var pose=ps.last();
        for(int i=0;i<FLAMES;i++){
            float ph=i*1.3f;
            double drift=Math.sin(t*0.18+ph)*0.05,sway=Math.cos(t*0.13+ph*1.4)*0.045,h=0.13+0.06*Math.abs(Math.sin(t*0.22+ph));
            float x=(float)(sway+(i%3-1)*0.06),z=(float)(drift+(i%2)*0.02),half=0.065f+0.018f*(i%2);
            int alpha=(int)((0.45+0.45*Math.abs(Math.sin(t*0.25+ph)))*255);
            flame(pose,b,e.getPackedLight(),x,half,z,h,alpha);
        }
        ps.popPose();
        }catch(Throwable t){fail("第一人称手部绘制",t);}
    }

    /** 一张竖直火苗面片：底宽、顶尖、内外两层颜色错开。 */
    private static void flame(PoseStack.Pose pose,VertexConsumer b,int light,float x,float half,float z,double h,int alpha){
        float top=(float)(h*1.6),bot=0f,w=half,tw=half*0.35f;
        int c1=(alpha<<24)|(HOT&0xFFFFFF),c2=((int)(alpha*0.55)<<24)|(RED&0xFFFFFF),c3=((int)(alpha*0.85)<<24)|(DEEP&0xFFFFFF);
        vertex(pose,b,light,x-w,bot,z,c3);vertex(pose,b,light,x+w,bot,z,c3);vertex(pose,b,light,x+tw,top,z,c1);vertex(pose,b,light,x-tw,top,z,c1);
        vertex(pose,b,light,x+w*0.6f,bot,z+0.01f,c2);vertex(pose,b,light,x-w*0.6f,bot,z+0.01f,c2);vertex(pose,b,light,x-tw*0.6f,top,z+0.01f,c1);vertex(pose,b,light,x+tw*0.6f,top,z+0.01f,c1);
    }
    private static void vertex(PoseStack.Pose pose,VertexConsumer b,int light,float x,float y,float z,int color){
        b.vertex(pose.pose(),x,y,z).color(color).uv(0,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0,0,1).endVertex();
    }
    /** 渲染层的 PoseStack 以相机为原点：世界坐标 = 相机位置 + 矩阵平移列。 */
    private static Vec3 worldFromPose(PoseStack ps){
        var m=ps.last().pose();var cam=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return cam.add(m.m30(),m.m31(),m.m32());
    }
    private FistFlameClient(){}
}
