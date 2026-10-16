package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 「咒力附拳」的表现层：**整条手臂上覆着红色半透明咒力火焰**（自己第一人称与别人都看得见）。
 *
 *  <p>三条经验（r228–r233 踩出来的）：</p>
 *  <ol>
 *    <li>**不要用玩家皮肤贴图当火焰贴图**：`UV(0,0)` 会采样到皮肤里的黑色像素，火焰就变成"黑烟"。
 *        改用 `sukuna:textures/effect/glow.png` 并**把 UV 固定在贴图中心**（(0.5,0.5)，那里的像素是白色不透明），
 *        这样整块面片的颜色完全由顶点色决定。</li>
 *    <li>**骨骼坐标系是模型像素**（1 单位 = 1/16 格）：手臂箱体只有 4×12 像素，
 *        所以火焰尺寸要给 **像素级**（半宽 1.8~2.6、高 5~8），给 0.07 等于 0.07 像素 → 肉眼看不见。</li>
 *    <li>**第一人称自己的手**走手持通路、且那个 PoseStack 的量级不可控（照 0.06 画就会糊满半个屏幕），
 *        所以第一人称改到 **世界渲染**（`RenderLevelStageEvent`）里，按估算的"肩→手"线画一串火焰，
 *        尺寸用**格**、完全可控。</li>
 *  </ol> */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class FistFlameClient {
    /** 服务端广播的特效号：附拳期间每 15 刻一条（带 entityId 与寿命）。 */
    public static final int FX=236;
    private static final ResourceLocation GLOW=new ResourceLocation("sukuna","textures/effect/glow.png");
    private static final Map<Integer,Long> BURNING=new HashMap<>();
    private static final Map<Integer,Vec3[]> HANDS=new HashMap<>();
    private static final int FLAMES=6,ARM_STEPS=4,SPAWN_INTERVAL=2;
    private static final int DEEP=0xB00A0A0A,HOT=0xE6FF3A20,RED=0xD0D01A10;
    private static final int BLOOM_RED=0xFF8A1208;
    private static String lastNote="";
    private static long lastNoteAt;

    public static void receive(Network.Effect e){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        BURNING.put(e.owner(),mc.level.getGameTime()+Math.max(10,e.life()));
        note("客户端收到 kind=236 owner="+e.owner()+" life="+e.life());
    }
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
    private static void note(String what){
        var mc=Minecraft.getInstance();long now=mc.level==null?0:mc.level.getGameTime();
        if(what.equals(lastNote)&&now-lastNoteAt<60)return;
        lastNote=what;lastNoteAt=now;com.mojang.logging.LogUtils.getLogger().info("[附拳] {}",what);
    }
    private static void fail(String what,Throwable t){
        String key="ERR:"+what;
        if(!key.equals(lastNote)){lastNote=key;com.mojang.logging.LogUtils.getLogger().warn("[附拳] {} 失败：{}",what,t.toString());}
    }
    private static void photon(Vec3 hand){
        try{
            PhotonFx.blobs(hand,0.16,0.12,0.08,0xFF5512,BLOOM_RED,0.06,-0.012,6,1,10);
            PhotonFx.spark(hand,0.09,0.13,0xFF2A10,BLOOM_RED,"textures/effect/glow.png",true,1,7,4,0.25);
        }catch(Throwable ignored){}
    }

    @Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){
            for(String skin:e.getSkins()){PlayerRenderer renderer=e.getSkin(skin);if(renderer!=null)renderer.addLayer(new Flames(renderer));}
        }
    }

    /** 第三人称/别人看：沿**整条手臂**（肩到手，0~10 像素）铺火焰。 */
    private static final class Flames extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
        Flames(PlayerRenderer parent){super(parent);}
        @Override public void render(PoseStack ps,MultiBufferSource source,int light,AbstractClientPlayer player,float limb,float amount,float partial,float age,float yaw,float pitch){
            if(!burning(player.getId()))return;
            try{
                var model=getParentModel();
                var b=source.getBuffer(RenderType.entityTranslucent(GLOW));
                float t=age+partial;
                Vec3[] store=HANDS.computeIfAbsent(player.getId(),k->new Vec3[2]);
                arm(ps,b,light,model.rightArm,t,store,0);
                arm(ps,b,light,model.leftArm,t+1.7f,store,1);
            }catch(Throwable t){fail("第三人称手部绘制",t);}
        }
        /** 一条手臂：从肩(0)到手(10 像素)分 ARM_STEPS 段，每段一簇火焰。单位为**像素**。 */
        private static void arm(PoseStack ps,VertexConsumer b,int light,ModelPart part,float t,Vec3[] store,int slot){
            ps.pushPose();part.translateAndRotate(ps);
            for(int s=0;s<ARM_STEPS;s++){
                float y=1.5f+s*3.0f;                                       // 像素：沿手臂往下
                ps.pushPose();ps.translate(0,y,0);
                if(store!=null&&slot==0&&s==ARM_STEPS-1)store[0]=null;
                ps.translate(0,0,0);
                ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));   // 让火苗朝上
                var pose=ps.last();
                for(int i=0;i<FLAMES;i++){
                    float ph=i*1.3f+s*2.1f;
                    double px=Math.cos(t*0.13+ph*1.4)*1.2+(i%3-1)*1.5, pz=Math.sin(t*0.18+ph)*1.2+(i%2)*0.8;
                    float half=1.8f+0.6f*(i%2), h=(float)(5.0+2.0*Math.abs(Math.sin(t*0.22+ph)));
                    int alpha=(int)((0.42+0.42*Math.abs(Math.sin(t*0.25+ph)))*255);
                    flame(pose,b,light,(float)px,half,(float)pz,h,alpha);
                }
                ps.popPose();
            }
            ps.popPose();
        }
    }

    /** 第一人称：自己在世界渲染里沿估算的"肩→手"线铺火焰（尺寸以格为单位，完全可控）。 */
    @SubscribeEvent public static void scene(RenderLevelStageEvent e){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||mc.level==null||!Client.progress.fist())return;
        if(!mc.options.getCameraType().isFirstPerson())return;              // 第三人称已由渲染层覆盖
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        try{
            var cam=e.getCamera().getPosition();
            Vec3 look=new Vec3(e.getCamera().getLookVector()),right=new Vec3(e.getCamera().getLeftVector()).scale(-1),up=new Vec3(e.getCamera().getUpVector());
            Vec3 hand=cam.add(look.scale(0.45)).add(right.scale(0.33)).subtract(up.scale(0.30));
            Vec3 shoulder=cam.add(look.scale(0.10)).add(right.scale(0.30)).subtract(up.scale(0.75));
            float t=mc.level.getGameTime()+e.getPartialTick();
            var ps=e.getPoseStack();ps.pushPose();ps.translate(-cam.x,-cam.y,-cam.z);
            RenderSystem.enableDepthTest();RenderSystem.disableCull();RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            for(int s=0;s<=ARM_STEPS;s++){
                double f=s/(double)ARM_STEPS;
                Vec3 at=shoulder.lerp(hand,f);
                for(int i=0;i<FLAMES;i++){
                    float ph=i*1.3f+s*1.7f;
                    Vec3 q=at.add(look.scale(Math.sin(t*0.18+ph)*0.03)).add(right.scale(Math.cos(t*0.13+ph*1.4)*0.035)).add(0,0.02*Math.sin(t*0.22+ph),0);
                    double half=0.045+0.015*(i%2),h=0.10+0.05*Math.abs(Math.sin(t*0.22+ph));
                    int alpha=(int)((0.45+0.40*Math.abs(Math.sin(t*0.25+ph)))*255);
                    cameraQuad(b,ps,q,look,right,up,half,h,alpha);
                }
            }
            BufferUploader.drawWithShader(b.end());
            RenderSystem.enableCull();RenderSystem.disableBlend();
            ps.popPose();
            HANDS.put(p.getId(),new Vec3[]{hand,null});
            note("第一人称世界层绘制（附拳=true）");
        }catch(Throwable t){fail("第一人称世界层绘制",t);}
    }
    /** 世界空间里面向相机的一张竖直火苗（用 look/right/up 当基向量，尺寸单位是格）。 */
    private static void cameraQuad(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 look,Vec3 right,Vec3 up,double half,double h,int alpha){
        Vec3 u=up.scale(h),w=right.scale(half);
        int c1=(alpha<<24)|(HOT&0xFFFFFF),c3=((int)(alpha*0.85)<<24)|(DEEP&0xFFFFFF);
        face(b,ps,c.subtract(w),c.add(w),c.add(w.scale(0.4)).add(u),c.subtract(w.scale(0.4)).add(u),c3,c1);
    }
    private static void face(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 z,Vec3 c,Vec3 d,int ca,int cz){
        Client.quad(b,ps,a,z,c,d,ca);
        Client.quad(b,ps,a,z,c,d,cz);
    }
    /** 一张竖直火苗面片（骨骼坐标系，单位＝像素）：底宽、顶尖。 */
    private static void flame(PoseStack.Pose pose,VertexConsumer b,int light,float x,float half,float z,double h,int alpha){
        float top=(float)h,bot=0f,w=half,tw=half*0.35f;
        int c1=(alpha<<24)|(HOT&0xFFFFFF),c2=((int)(alpha*0.55)<<24)|(RED&0xFFFFFF),c3=((int)(alpha*0.85)<<24)|(DEEP&0xFFFFFF);
        vert(pose,b,light,x-w,bot,z,c3);vert(pose,b,light,x+w,bot,z,c3);vert(pose,b,light,x+tw,top,z,c1);vert(pose,b,light,x-tw,top,z,c1);
        vert(pose,b,light,x+w*0.6f,bot,z+0.01f,c2);vert(pose,b,light,x-w*0.6f,bot,z+0.01f,c2);vert(pose,b,light,x-tw*0.6f,top,z+0.01f,c1);vert(pose,b,light,x+tw*0.6f,top,z+0.01f,c1);
    }
    /** UV 固定取贴图中心（那里的像素是白色不透明），颜色完全由顶点色决定。 */
    private static void vert(PoseStack.Pose pose,VertexConsumer b,int light,float x,float y,float z,int color){
        b.vertex(pose.pose(),x,y,z).color(color).uv(0.5f,0.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0,0,1).endVertex();
    }
    private FistFlameClient(){}
}
