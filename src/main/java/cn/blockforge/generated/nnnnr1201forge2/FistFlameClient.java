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

/** 「咒力附拳」的表现层：**小臂到拳头上裹着一大团粉红半透明咒力**（照原作截图：光滑、成团、不是尖火苗）。
 *
 *  <p>做法＝把 {@code textures/effect/glow.png}（柔和的径向光斑）**整张贴**画成面向相机的大方片，
 *  叠两层（外层暗红大一点、内层亮粉小一点）就得到"一大团咒力"的观感；只在**小臂中段到拳头**铺，
 *  不会包住整个人。</p>
 *
 *  <p>坐标系注意事项（踩过的坑）：骨骼坐标系单位是**模型像素**（1 像素 = 1/16 格），
 *  所以骨骼里画的尺寸要给像素级；贴图**不能**用玩家皮肤（UV 会采到黑色像素 → 变黑烟），
 *  这里统一用 glow 贴图，并让 UV 覆盖 (0,0)~(1,1) 以保留它的柔边。</p> */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class FistFlameClient {
    public static final int FX=236;
    private static final ResourceLocation GLOW=new ResourceLocation("sukuna","textures/effect/glow.png");
    private static final Map<Integer,Long> BURNING=new HashMap<>();
    private static final Map<Integer,Vec3[]> HANDS=new HashMap<>();
    /** 用哪几种颜色：外层暗红偏黑、中层正红、内层亮粉（照原作那团咒力的层次）。 */
    private static final int OUTER=0x9A5A0A0A,MID=0xC8C81A18,INNER=0xD8FF7A82;
    private static final int BLOOM_RED=0xFF8A1208;
    private static final int SPAWN_INTERVAL=2;
    private static String lastNote="";
    private static long lastNoteAt;

    public static void receive(Network.Effect e){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        BURNING.put(e.owner(),mc.level.getGameTime()+Math.max(10,e.life()));
        note("客户端收到 kind=236 owner="+e.owner());
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
    /** 附加的 PhotonFx 发光（有 Photon 时给这团咒力加 bloom，没有也不影响主绘制）。 */
    private static void photon(Vec3 hand){
        try{
            PhotonFx.blobs(hand,0.20,0.14,0.10,0xFF5512,BLOOM_RED,0.05,-0.010,5,1,10);
            PhotonFx.spark(hand,0.12,0.16,0xFF2A10,BLOOM_RED,"textures/effect/glow.png",true,1,7,4,0.2);
        }catch(Throwable ignored){}
    }

    @Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){
            for(String skin:e.getSkins()){PlayerRenderer renderer=e.getSkin(skin);if(renderer!=null)renderer.addLayer(new Flames(renderer));}
        }
    }

    /** 第三人称/别人看：小臂中段到拳头裹两团光斑。单位＝模型像素。 */
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
            }catch(Throwable t){fail("第三人称绘制",t);}
        }
        /** 沿小臂铺：像素 y=4(肘) 与 y=8.5(拳) 两处，每处两层大光斑，随呼吸缩放。 */
        private static void arm(PoseStack ps,VertexConsumer b,int light,ModelPart part,float t,Vec3[] store,int slot){
            ps.pushPose();part.translateAndRotate(ps);
            for(int k=0;k<2;k++){
                float y=k==0?4.5f:9.0f;
                double pulse=1.0+0.10*Math.sin(t*0.16+k*2.1f+slot*1.7f);   // 呼吸
                double len=11.0*pulse,wide=8.5*pulse;
                if(store!=null&&slot==0&&k==1)store[0]=worldFromPose(ps,y,0);
                blob(ps,b,light,0,y,0,wide,len,OUTER);                      // 外层：暗红、更大
                blob(ps,b,light,0,y,0.06f,wide*0.78,len*0.80,MID);          // 中层：正红
                blob(ps,b,light,0,y,0.12f,wide*0.50,len*0.55,INNER);        // 内层：亮粉
            }
            ps.popPose();
        }
    }

    /** 第一人称：自己在世界渲染里沿"肘→拳"这段画同样两团光斑（尺寸以格为单位）。 */
    @SubscribeEvent public static void scene(RenderLevelStageEvent e){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||mc.level==null||!Client.progress.fist())return;
        if(!mc.options.getCameraType().isFirstPerson())return;
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        try{
            var cam=e.getCamera().getPosition();
            Vec3 look=new Vec3(e.getCamera().getLookVector()),right=new Vec3(e.getCamera().getLeftVector()).scale(-1),up=new Vec3(e.getCamera().getUpVector());
            Vec3 hand=cam.add(look.scale(0.50)).add(right.scale(0.30)).subtract(up.scale(0.34));
            Vec3 elbow=cam.add(look.scale(0.34)).add(right.scale(0.30)).subtract(up.scale(0.62));
            float t=mc.level.getGameTime()+e.getPartialTick();
            var ps=e.getPoseStack();ps.pushPose();ps.translate(-cam.x,-cam.y,-cam.z);
            RenderSystem.enableDepthTest();RenderSystem.disableCull();RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            for(int k=0;k<2;k++){
                Vec3 at=k==0?elbow:hand;
                double pulse=1+0.10*Math.sin(t*0.16+k*2.1);
                double len=(k==0?0.34:0.30)*pulse,wide=(k==0?0.26:0.24)*pulse;
                worldBlob(b,ps,at,look,right,up,wide,len,OUTER);
                worldBlob(b,ps,at,look,right,up,wide*0.78,len*0.80,MID);
                worldBlob(b,ps,at,look,right,up,wide*0.50,len*0.55,INNER);
            }
            BufferUploader.drawWithShader(b.end());
            RenderSystem.enableCull();RenderSystem.disableBlend();
            ps.popPose();
            HANDS.put(p.getId(),new Vec3[]{hand,null});
            note("第一人称世界层绘制");
        }catch(Throwable t){fail("第一人称世界层绘制",t);}
    }

    /** 骨骼坐标系里的一团光斑：竖直方片，UV 覆盖整张 glow（保留柔边）。 */
    private static void blob(PoseStack ps,VertexConsumer b,int light,float x,float y,float z,double wide,double len,int color){        ps.pushPose();ps.translate(x,y,z);ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
        var pose=ps.last();
        float w=(float)wide,h=(float)len;
        quad(pose,b,light,-w,0,-h,w,0,h,color,false);
        ps.popPose();
    }
    /** 世界空间里的一团光斑：以相机基向量铺一张面向相机的方片。 */
    private static void worldBlob(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 look,Vec3 right,Vec3 up,double wide,double len,int color){
        Vec3 u=up.scale(len),w=right.scale(wide);
        Client.quad(b,ps,c.subtract(w).subtract(u),c.add(w).subtract(u),c.add(w).add(u),c.subtract(w).add(u),color);
    }
    private static void quad(PoseStack.Pose pose,VertexConsumer b,int light,float x0,float y0,float z0,float x1,float y1,float z1,int color,boolean unused){
        // UV 逐顶点铺满整张贴图：glow 的径向柔边才会显出来（全钉 (0,0) 只会采到透明角 → 看不见）
        vertex(pose,b,light,x0,y1,z0,color,0f,0f);
        vertex(pose,b,light,x1,y1,z0,color,1f,0f);
        vertex(pose,b,light,x1,y0,z1,color,1f,1f);
        vertex(pose,b,light,x0,y0,z1,color,0f,1f);
    }
    private static void vertex(PoseStack.Pose pose,VertexConsumer b,int light,float x,float y,float z,int color,float u,float v){
        b.vertex(pose.pose(),x,y,z).color(color).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0,0,1).endVertex();
    }
    private static Vec3 worldFromPose(PoseStack ps,double dy_pixels,double dz_pixels){
        var m=ps.last().pose();var cam=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return cam.add(m.m30(),m.m31()+dy_pixels/16.0,m.m32()+dz_pixels/16.0);
    }
    private FistFlameClient(){}
}
