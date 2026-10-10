package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 遗迹的局部天空和非实体残影，不生成怪物、不改变世界时间。 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class RuinClient {
    private static Vec3 origin;private static int rotation;private static long until,caption;
    private static Vec3 local(double x,double y,double z){return switch(rotation){case 1->origin.add(64-z,y,x);case 2->origin.add(48-x,y,64-z);case 3->origin.add(z,y,48-x);default->origin.add(x,y,z);};}
    private static HumanoidModel<LivingEntity> ghost;
    public static void clear(){origin=null;until=caption=0;ghost=null;}
    public static void receive(Network.Effect e){var l=Minecraft.getInstance().level;if(l==null)return;origin=e.position();rotation=(int)e.direction().x;until=l.getGameTime()+e.life();if(e.seed()==1)caption=l.getGameTime()+70;}
    private static boolean active(){var mc=Minecraft.getInstance();return origin!=null&&mc.level!=null&&mc.player!=null&&mc.level.dimension()==net.minecraft.world.level.Level.OVERWORLD&&mc.level.getGameTime()<until&&mc.player.getX()>=origin.x&&mc.player.getX()<origin.x+(rotation%2==0?49:65)&&mc.player.getZ()>=origin.z&&mc.player.getZ()<origin.z+(rotation%2==0?65:49);}
    @SubscribeEvent public static void fog(ViewportEvent.ComputeFogColor e){if(active()){e.setRed(0.43f);e.setGreen(0.39f);e.setBlue(0.51f);}}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){var mc=Minecraft.getInstance();if(!active()||mc.options.hideGui||mc.level.getGameTime()>=caption)return;int h=mc.getWindow().getGuiScaledHeight();e.getGuiGraphics().drawString(mc.font,"……护人之魂，镇咒于此……",12,h/2,0xA5D7CBE3);}
    private static void vertex(BufferBuilder b,PoseStack ps,Vec3 p,int color){b.vertex(ps.last().pose(),(float)p.x,(float)p.y,(float)p.z).color(color>>16&255,color>>8&255,color&255,color>>>24).endVertex();}
    private static void quad(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 c,Vec3 d,Vec3 e,int color){vertex(b,ps,a,color);vertex(b,ps,c,color);vertex(b,ps,d,color);vertex(b,ps,e,color);}
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(!active())return;boolean sky=e.getStage()==RenderLevelStageEvent.Stage.AFTER_SKY;
        if(!sky&&e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();PoseStack ps=e.getPoseStack();Vec3 cam=e.getCamera().getPosition();float t=mc.level.getGameTime()+e.getPartialTick();
        ps.pushPose();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.depthMask(false);RenderSystem.setShader(GameRenderer::getPositionColorShader);
        if(sky)RenderSystem.disableDepthTest();else {RenderSystem.enableDepthTest();ps.translate(-cam.x,-cam.y,-cam.z);}
        BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        if(sky){double r=240;quad(b,ps,new Vec3(-r,120,-r),new Vec3(r,120,-r),new Vec3(r,120,r),new Vec3(-r,120,r),0xFF80768E);for(int i=0;i<32;i++){double a=i*Math.PI/16,z=(i+1)*Math.PI/16;quad(b,ps,new Vec3(Math.cos(a)*r,-150,Math.sin(a)*r),new Vec3(Math.cos(z)*r,-150,Math.sin(z)*r),new Vec3(Math.cos(z)*r,121,Math.sin(z)*r),new Vec3(Math.cos(a)*r,121,Math.sin(a)*r),0xFF70657F);}}
        else {
            for(int i=0;i<90;i++){double x=2+Math.floorMod(i*173,450)/10.0,z=2+Math.floorMod(i*97,600)/10.0,y=11+(t*.008+i*.719)%8;Vec3 p=local(x,y,z);Client.ribbon(b,ps,p,p.add(.04,.07,.02),.035,0x906F449E,cam);}
            for(int i=0;i<40;i++){double age=(t*.013+i*.37)%2;Vec3 p=local(3+Math.floorMod(i*61,430)/10.0,11+age,3+Math.floorMod(i*37,580)/10.0);Client.ribbon(b,ps,p,p.add(0,.10,0),.025,((int)(120*(1-age/2))<<24)|0xE8B85F,cam);}
            // 丝线靠近实际墙体，地下密度更高。
            for(int i=0;i<48;i++){boolean below=i>=24;Vec3 p=below?local(31+(i%2)*14,2+(i%5),40+(i%13)):local(i%2==0?5.04:42.96,12+i%4,34+i%24);for(int j=0;j<5;j++){Vec3 q=p.add(Math.sin(t*.025+i+j)*.12,.21,.18);Client.ribbon(b,ps,p,q,.012,0x65E3E0F1,cam);p=q;}}
        }
        BufferUploader.drawWithShader(b.end());RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.enableCull();RenderSystem.disableBlend();
        if(!sky)ghosts(ps,t);
        ps.popPose();
    }
    private static void ghosts(PoseStack ps,float t){
        var mc=Minecraft.getInstance();if(ghost==null)ghost=new HumanoidModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER));
        var source=mc.renderBuffers().bufferSource();var buffer=source.getBuffer(RenderType.entityTranslucent(new ResourceLocation("sukuna","textures/entity/yuji.png")));
        for(int i=0;i<5;i++){float phase=(t+i*57)%240;if(phase>75)continue;float alpha=(float)Math.sin(phase/75*Math.PI)*0.22f;boolean runner=i==0,teacher=i==4;Vec3 p=runner?local(12+phase*.25,11,27):local(i<3?10:34,teacher?11:11.15,teacher?48:39+(i%2)*12);
            ghost.setupAnim(mc.player,runner?t*.3f:0,runner?.7f:0,t,0,0);ghost.hat.visible=false;
            if(!runner&&!teacher){ghost.rightLeg.xRot=ghost.leftLeg.xRot=-1.4f;ghost.rightArm.xRot=ghost.leftArm.xRot=-0.65f;}
            if(teacher)ghost.rightArm.xRot=-1.5f+(float)Math.sin(t*.04)*.2f;
            ps.pushPose();ps.translate(p.x,p.y+1.5,p.z);ps.mulPose(Axis.YP.rotationDegrees((runner?90:180)-rotation*90));ps.scale(-1,-1,1);ghost.renderToBuffer(ps,buffer,0xF000F0,OverlayTexture.NO_OVERLAY,.7f,.72f,.9f,alpha);ps.popPose();
        }
        source.endBatch();
    }
}
