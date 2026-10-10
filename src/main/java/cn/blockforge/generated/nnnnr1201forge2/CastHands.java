package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.InteractionHand;

/** 复用玩家皮肤与原版手臂，施法时保留盔甲、名字和其他渲染层。 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class CastHands {
    private record Pose(int kind,int skill,long start,int life){}
    private static final Map<Integer,Pose> poses=new HashMap<>();
    public static void clear(){poses.clear();}
    public static void receive(Network.Effect e){var l=Minecraft.getInstance().level;if(l!=null)poses.put(e.owner(),new Pose(e.kind(),(int)e.size(),l.getGameTime(),e.life()));}
    private static Pose active(int id){if(FinaleClient.active(id)||MotionRenderCompat.externalPlayerModels()||DismantleMotionClient.active(id))return null;var mc=Minecraft.getInstance();Pose pose=poses.get(id);if(mc.level==null||pose==null)return null;if(mc.level.getGameTime()-pose.start>=pose.life){poses.remove(id);return null;}return pose;}
    @Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){for(String skin:e.getSkins()){PlayerRenderer renderer=e.getSkin(skin);if(renderer!=null)renderer.addLayer(new Arms(renderer));}}
    }
    @SubscribeEvent public static void body(RenderPlayerEvent.Pre e){if(active(e.getEntity().getId())==null)return;var m=e.getRenderer().getModel();m.leftArm.skipDraw=m.rightArm.skipDraw=m.leftSleeve.skipDraw=m.rightSleeve.skipDraw=true;}
    @SubscribeEvent public static void after(RenderPlayerEvent.Post e){var m=e.getRenderer().getModel();m.leftArm.skipDraw=m.rightArm.skipDraw=m.leftSleeve.skipDraw=m.rightSleeve.skipDraw=false;}
    private static final class Arms extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
        Arms(PlayerRenderer parent){super(parent);}
        @Override public void render(PoseStack ps,MultiBufferSource source,int light,AbstractClientPlayer player,float limb,float amount,float partial,float age,float yaw,float pitch){
            Pose pose=active(player.getId());if(pose==null)return;var m=getParentModel();float lx=m.leftArm.xRot,ly=m.leftArm.yRot,rx=m.rightArm.xRot,ry=m.rightArm.yRot;
            if(pose.kind==2){m.leftArm.xRot=-1.55f;m.leftArm.yRot=0.25f;m.rightArm.xRot=-1.4f;m.rightArm.yRot=-0.8f;}
            else if(pose.kind==70&&pose.skill==3){m.leftArm.xRot=-0.3f;m.leftArm.yRot=0.15f;m.rightArm.xRot=-2.4f;m.rightArm.yRot=-0.25f;}
            else {m.leftArm.xRot=m.rightArm.xRot=-1.2f;m.leftArm.yRot=0.6f;m.rightArm.yRot=-0.6f;}
            m.leftSleeve.copyFrom(m.leftArm);m.rightSleeve.copyFrom(m.rightArm);
            var b=source.getBuffer(RenderType.entityCutoutNoCull(player.getSkinTextureLocation()));
            for(var arm:List.of(m.leftArm,m.rightArm,m.leftSleeve,m.rightSleeve)){arm.skipDraw=false;arm.render(ps,b,light,OverlayTexture.NO_OVERLAY);}
            m.leftArm.xRot=lx;m.leftArm.yRot=ly;m.rightArm.xRot=rx;m.rightArm.yRot=ry;m.leftSleeve.copyFrom(m.leftArm);m.rightSleeve.copyFrom(m.rightArm);
        }
    }
    @SubscribeEvent public static void hand(RenderHandEvent e){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        Pose pose=active(mc.player.getId());if(pose==null)return;
        float age=mc.level.getGameTime()-pose.start+e.getPartialTick();
        e.setCanceled(true);if(e.getHand()!=InteractionHand.MAIN_HAND)return;
        var ps=e.getPoseStack();var renderer=(PlayerRenderer)mc.getEntityRenderDispatcher().getRenderer(mc.player);
        float draw=Math.min(1,age/Math.max(1,pose.life));
        for(int side:new int[]{-1,1}){
            ps.pushPose();
            if(pose.kind==2){ps.translate(side<0?-0.42:0.32,-0.38,side<0?-0.95:-0.62+draw*0.28);ps.mulPose(Axis.YP.rotationDegrees(side<0?22:-30));ps.mulPose(Axis.ZP.rotationDegrees(side<0?-12:25));}
            else if(pose.kind==70&&pose.skill==3){ps.translate(side>0?0.32:-0.42,side>0?-0.08:-0.55,side>0?-0.7:-0.52);ps.mulPose(Axis.YP.rotationDegrees(side*-12));ps.mulPose(Axis.ZP.rotationDegrees(side>0?-20:8));}
            else {double sweep=pose.skill==5?0:Math.sin(draw*Math.PI)*0.18;ps.translate(side*(pose.skill==5?0.13:0.3)+sweep,-0.4,-0.72);ps.mulPose(Axis.YP.rotationDegrees(side*-24));ps.mulPose(Axis.ZP.rotationDegrees(side*(pose.skill==5?38:18)));}
            ps.mulPose(Axis.XP.rotationDegrees(-75));
            if(side>0)renderer.renderRightHand(ps,e.getMultiBufferSource(),e.getPackedLight(),mc.player);else renderer.renderLeftHand(ps,e.getMultiBufferSource(),e.getPackedLight(),mc.player);
            ps.popPose();
        }
    }
}
