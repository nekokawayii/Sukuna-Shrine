package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 「咒力附拳」的表现层（客户端，**所有玩家都能看见**）：双手上飘荡的红色半透明咒力火焰。
 *
 *  <p>服务端在附拳开启期间周期性广播一条 {@code kind=236} 特效（带玩家 entityId 与寿命），
 *  客户端据此把该玩家登记进 {@link #BURNING}，然后在**玩家渲染器上加一层**，
 *  用 {@code ModelPart#translateAndRotate} 把面片贴到左右手的位置上——所以别人看你的手也着着火。</p>
 *
 *  <p>火焰＝若干张相机朝向的红色面片，逐片随时间上下飘（`sin`）、左右摆、明暗闪烁；全半透明、加法感靠多层叠加。
 *  第一人称下自己的手走的是手持渲染通路，看不到这一层，所以本机保留原有 {@link FistAura} 的屏幕表现。</p> */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class FistFlameClient {
    public static final int FX=236;
    /** 正在附拳的玩家 → 到期刻。 */
    private static final Map<Integer,Long> BURNING=new HashMap<>();
    private static final int FLAMES=7;
    private static final int DEEP=0xE00A0A0A,HOT=0xFFE02418,RED=0xFFB01810;

    public static void receive(Network.Effect e){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        BURNING.put(e.owner(),mc.level.getGameTime()+Math.max(10,e.life()));
    }
    public static void tick(){
        var mc=Minecraft.getInstance();if(mc.level==null||BURNING.isEmpty())return;
        long now=mc.level.getGameTime();BURNING.entrySet().removeIf(en->now>en.getValue());
    }
    public static void clear(){BURNING.clear();}
    private static boolean burning(int id){Long until=BURNING.get(id);return until!=null&&Minecraft.getInstance().level!=null&&Minecraft.getInstance().level.getGameTime()<=until;}

    @Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){
            for(String skin:e.getSkins()){PlayerRenderer renderer=e.getSkin(skin);if(renderer!=null)renderer.addLayer(new Flames(renderer));}
        }
    }
    /** 在玩家模型的左右手上各画一簇红色咒力火焰。 */
    private static final class Flames extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
        Flames(PlayerRenderer parent){super(parent);}
        @Override public void render(PoseStack ps,MultiBufferSource source,int light,AbstractClientPlayer player,float limb,float amount,float partial,float age,float yaw,float pitch){
            if(!burning(player.getId()))return;
            var model=getParentModel();
            var b=source.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation()));
            float t=age+partial;
            draw(ps,b,light,model.rightArm,t);
            draw(ps,b,light,model.leftArm,t+1.7f);
        }
        /** 以某条手臂的骨骼为基准，往手掌方向偏移后画一簇火焰。 */
        private static void draw(PoseStack ps,VertexConsumer b,int light,net.minecraft.client.model.geom.ModelPart arm,float t){
            ps.pushPose();arm.translateAndRotate(ps);
            // 模型空间里 **+Y 是向下**：从肩到手要往 +Y 挪 9/16 格（原来看起来落在耳朵上就是这里写成了 -6/16）
            ps.translate(0,9/16.0,0);
            ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));   // 翻转后火苗朝上
            var pose=ps.last();
            for(int i=0;i<FLAMES;i++){
                float ph=i*1.3f;
                double drift=Math.sin(t*0.18+ph)*0.055;                    // 飘荡
                double sway=Math.cos(t*0.13+ph*1.4)*0.045;
                double h=0.10+0.05*Math.abs(Math.sin(t*0.22+ph));          // 火苗高矮起伏
                float x=(float)(sway+(i%3-1)*0.055),z=(float)(drift+(i%2)*0.02);
                float half=0.045f+0.012f*(i%2);
                int alpha=(int)((0.30+0.35*Math.abs(Math.sin(t*0.25+ph)))*255);
                flame(pose,b,light,x,half,z,h,alpha);
            }
            ps.popPose();
        }
    }
    private static void flame(PoseStack.Pose pose,VertexConsumer b,int light,float x,float half,float z,double h,int alpha){
        float top=(float)(h*1.6),bot=0f,w=half,tw=half*0.35f;
        int c1=(alpha<<24)|(HOT&0xFFFFFF),c2=((int)(alpha*0.55)<<24)|(RED&0xFFFFFF),c3=((int)(alpha*0.85)<<24)|(DEEP&0xFFFFFF);
        vertex(pose,b,light,x-w,bot,z,c3);vertex(pose,b,light,x+w,bot,z,c3);vertex(pose,b,light,x+tw,top,z,c1);vertex(pose,b,light,x-tw,top,z,c1);
        vertex(pose,b,light,x+w*0.6f,bot,z+0.01f,c2);vertex(pose,b,light,x-w*0.6f,bot,z+0.01f,c2);vertex(pose,b,light,x-tw*0.6f,top,z+0.01f,c1);vertex(pose,b,light,x+tw*0.6f,top,z+0.01f,c1);
    }
    private static void vertex(PoseStack.Pose pose,VertexConsumer b,int light,float x,float y,float z,int color){
        b.vertex(pose.pose(),x,y,z).color(color).uv(0,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0,0,1).endVertex();
    }
    /** 第一人称：自己的手走的是手持渲染通路（不经过玩家模型），所以这里补一份，让本机也能看到手上的火焰。
     *  位置在 {@code translate(0.06,-0.30,-0.10)} —— 偏了就改这三个数。 */
    @SubscribeEvent public static void hand(RenderHandEvent e){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||mc.level==null||!Client.progress.fist()||e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND)return;
        float t=mc.level.getGameTime()+e.getPartialTick();
        var ps=e.getPoseStack();ps.pushPose();
        ps.translate(0.06,-0.30,-0.10);
        ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
        var b=e.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(p.getSkinTextureLocation()));
        var pose=ps.last();
        for(int i=0;i<FLAMES;i++){
            float ph=i*1.3f;
            double drift=Math.sin(t*0.18+ph)*0.055,sway=Math.cos(t*0.13+ph*1.4)*0.045,h=0.10+0.05*Math.abs(Math.sin(t*0.22+ph));
            float x=(float)(sway+(i%3-1)*0.055),z=(float)(drift+(i%2)*0.02),half=0.045f+0.012f*(i%2);
            int alpha=(int)((0.30+0.35*Math.abs(Math.sin(t*0.25+ph)))*255);
            flame(pose,b,e.getPackedLight(),x,half,z,h,alpha);
        }
        ps.popPose();
    }
    private FistFlameClient(){}
}
