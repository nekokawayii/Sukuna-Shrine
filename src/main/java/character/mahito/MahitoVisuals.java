package character.mahito;

import cn.blockforge.generated.nnnnr1201forge2.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 纯客户端演出，不生成一堆装饰生物；每个领域固定24只倒悬手臂。 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class MahitoVisuals {
    private record Visual(Network.Effect effect,long born){}
    private static final List<Visual> EFFECTS=new ArrayList<>();
    private static final Map<Integer,Visual> DOMAINS=new HashMap<>();
    private static BbModel tail=new BbModel("mahito_tail","mahito_soul"),arm=new BbModel("mahito_arm","mahito_soul");
    private static final ResourceLocation SKIN=new ResourceLocation("sukuna","textures/entity/mahito_soul_0.png");
    private static final ResourceLocation FACES=new ResourceLocation("sukuna","textures/block/mahito_faces.png");
    public static void receive(Network.Effect e){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        if(e.kind()==MahitoCombat.DOMAIN_END){DOMAINS.remove(e.owner());return;}
        var visual=new Visual(e,mc.level.getGameTime());
        if(e.kind()==MahitoCombat.DOMAIN){DOMAINS.put(e.owner(),visual);return;}
        if(EFFECTS.size()>=160)EFFECTS.remove(0);EFFECTS.add(visual);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){EFFECTS.clear();DOMAINS.clear();}
    public static void reload(){tail=new BbModel("mahito_tail","mahito_soul");arm=new BbModel("mahito_arm","mahito_soul");}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;long now=mc.level.getGameTime();float partial=event.getPartialTick();
        EFFECTS.removeIf(v->now-v.born>v.effect.life());DOMAINS.values().removeIf(v->now-v.born>v.effect.life()+40);
        var ps=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();Vec3 camera=event.getCamera().getPosition();
        ps.pushPose();ps.translate(-camera.x,-camera.y,-camera.z);
        for(Visual visual:EFFECTS){
            var fx=visual.effect;if(fx.position().distanceToSqr(camera)>128*128)continue;
            float age=now-visual.born+partial;float seconds=age/20f;
            ps.pushPose();ps.translate(fx.position().x,fx.position().y,fx.position().z);
            float yaw=(float)Math.toDegrees(Math.atan2(-fx.direction().x,-fx.direction().z));
            switch(fx.kind()){
                case MahitoCombat.TAIL -> {ps.mulPose(Axis.YP.rotationDegrees(yaw));ps.translate(0,0,1);tail.render(ps,buffers,15728880,"sweep",seconds,1,.85f,.9f,.85f);}
                case MahitoCombat.ARMS -> {ps.mulPose(Axis.YP.rotationDegrees(yaw));arm.render(ps,buffers,15728880,"rise",Math.max(0,seconds),1,1,1,1);}
                case MahitoCombat.TOUCH -> {
                    Vec3 delta=fx.direction().subtract(fx.position());ps.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees(Math.atan2(delta.x,delta.z))));ps.mulPose(Axis.XP.rotationDegrees((float)Math.toDegrees(Math.atan2(delta.horizontalDistance(),delta.y))));
                    ps.scale(.16f,(float)delta.length()/3.625f,.16f);arm.render(ps,buffers,15728880,"",0,1,1,1,.9f);
                }
                case MahitoCombat.THREADS -> {
                    var caster=mc.level.getEntity(fx.owner());var target=mc.level.getEntity((int)fx.size());Vec3 start=caster==null?fx.position():caster.getEyePosition(partial),end=target==null?fx.direction():target.getBoundingBox().getCenter();
                    for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 offset=new Vec3(Math.cos(a)*.45,(i%3-1)*.35,Math.sin(a)*.45);ribbon(ps,buffers,start.subtract(fx.position()),end.add(offset).subtract(fx.position()),.018f,.8f,.7f,.85f);}
                }
                case MahitoCombat.DODGE -> {box(ps,buffers,-.9f,.02f,-.8f,.9f,.14f,.8f,.65f,.35f,.42f,.8f);ps.translate(fx.direction().x-fx.position().x,fx.direction().y-fx.position().y,fx.direction().z-fx.position().z);float rise=Math.min(1,age/8);box(ps,buffers,-.45f,0,-.4f,.45f,3*rise,.4f,.9f,.8f,.85f,.35f);}
                case MahitoCombat.PUPPET -> {float size=.4f+age*.06f;for(int i=0;i<10;i++){ps.pushPose();ps.mulPose(Axis.YP.rotationDegrees(i*36));ps.translate(size,.2+Math.sin(age*.2+i)*.3,0);box(ps,buffers,-.12f,-.12f,-.12f,.12f,.12f,.12f,.8f,.2f,.3f,.8f);ps.popPose();}}
                case MahitoCombat.REFORGE -> {for(int i=0;i<8;i++){double a=age*.15+i*Math.PI/4;Vec3 at=new Vec3(Math.cos(a),.3+age*.07,Math.sin(a));ribbon(ps,buffers,at,at.add(0,.5,0),.04f,.5f,.95f,.75f);}}
                default -> {}
            }
            ps.popPose();
        }
        for(Visual visual:DOMAINS.values()){
            var fx=visual.effect;if(fx.position().distanceToSqr(camera)>160*160)continue;
            if(!DomainWorldRenderer.clashing(fx.owner()))shell(ps,buffers,fx);
            for(int ring=0;ring<3;ring++)for(int i=0;i<8;i++){
                double a=i*Math.PI/4+ring*.4,r=7+ring*7;double y=Math.sqrt(Math.max(0,fx.size()*fx.size()-r*r))-1.5;
                ps.pushPose();ps.translate(fx.position().x+Math.cos(a)*r,fx.position().y+y,fx.position().z+Math.sin(a)*r);
                ps.mulPose(Axis.ZP.rotationDegrees(180));ps.mulPose(Axis.YP.rotationDegrees(i*45));float s=1.2f+.12f*(float)Math.sin((now+partial)*.045+i);ps.scale(s,s,s);
                arm.render(ps,buffers,15728880,"",0,.95f,.85f,.9f,1);ps.popPose();
            }
        }
        ps.popPose();
    }
    private static void shell(PoseStack ps,MultiBufferSource buffers,Network.Effect effect){
        // 即刻显示人面穹顶；实体方块仍按预算铺设，不让大批改方块卡住开场。
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(FACES));double radius=effect.size()-.35;
        for(int y=0;y<12;y++)for(int x=0;x<32;x++){
            double low=y*Math.PI/24,high=(y+1)*Math.PI/24,a=x*Math.PI/16,b=(x+1)*Math.PI/16;
            double[][] angles={{low,a},{low,b},{high,b},{high,a}};
            for(int i=0;i<4;i++){
                double lat=angles[i][0],az=angles[i][1],h=Math.cos(lat)*radius;
                float px=(float)(effect.position().x+Math.cos(az)*h),py=(float)(effect.position().y+Math.sin(lat)*radius),pz=(float)(effect.position().z+Math.sin(az)*h);
                out.vertex(ps.last().pose(),px,py,pz).color(1f,1f,1f,1f).uv(i==0||i==3?0:1,i<2?1:0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0,1,0).endVertex();
            }
        }
    }
    private static void ribbon(PoseStack ps,MultiBufferSource buffers,Vec3 a,Vec3 b,float width,float r,float g,float blue){
        var out=buffers.getBuffer(RenderType.entityTranslucent(SKIN));var matrix=ps.last().pose();
        for(Vec3 p:List.of(a.add(width,0,0),b.add(width,0,0),b.add(-width,0,0),a.add(-width,0,0)))out.vertex(matrix,(float)p.x,(float)p.y,(float)p.z).color(r,g,blue,.9f).uv(.1f,.1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0,1,0).endVertex();
    }
    private static void box(PoseStack ps,MultiBufferSource buffers,float x,float y,float z,float X,float Y,float Z,float r,float g,float b,float alpha){
        float[][][] faces={{{x,y,z},{X,y,z},{X,Y,z},{x,Y,z}},{{X,y,Z},{x,y,Z},{x,Y,Z},{X,Y,Z}},{{x,y,Z},{x,y,z},{x,Y,z},{x,Y,Z}},{{X,y,z},{X,y,Z},{X,Y,Z},{X,Y,z}},{{x,Y,z},{X,Y,z},{X,Y,Z},{x,Y,Z}}};
        var out=buffers.getBuffer(RenderType.entityTranslucent(SKIN));for(var face:faces)for(int i=0;i<4;i++)out.vertex(ps.last().pose(),face[i][0],face[i][1],face[i][2]).color(r,g,b,alpha).uv(i%2*.4f,(i/2)*.4f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0,1,0).endVertex();
    }
    public static final class PuppetRenderer extends EntityRenderer<MahitoPuppetEntity>{
        private final BbModel model=new BbModel("mahito_puppet","mahito_soul");
        public PuppetRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.5f;}
        @Override public ResourceLocation getTextureLocation(MahitoPuppetEntity entity){return SKIN;}
        @Override public void render(MahitoPuppetEntity entity,float yaw,float partial,PoseStack ps,MultiBufferSource buffers,int light){
            ps.pushPose();ps.mulPose(Axis.YP.rotationDegrees(180-yaw));ps.scale(.82f,.82f,.82f);model.render(ps,buffers,light,entity.getDeltaMovement().horizontalDistanceSqr()>.001?"walk":"idle",(entity.tickCount+partial)/20,1,1,1,1);ps.popPose();super.render(entity,yaw,partial,ps,buffers,light);
        }
    }
    private MahitoVisuals(){}
}
