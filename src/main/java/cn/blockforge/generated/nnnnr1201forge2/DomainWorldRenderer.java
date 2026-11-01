package cn.blockforge.generated.nnnnr1201forge2;

import character.domain.DomainPresentation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** 真实世界中的球面天空和对拼交界。深度测试保留，绝不使用遮住 NPC 的全屏贴片。 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class DomainWorldRenderer {
    private static DomainPresentation.Snapshot snapshot;
    private static long received;
    private static net.minecraft.client.multiplayer.ClientLevel receivedLevel;
    @Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event){
            event.enqueueWork(()->DomainPresentation.receiver=DomainWorldRenderer::receive);
        }
    }
    private static void receive(DomainPresentation.Snapshot value){
        var level=Minecraft.getInstance().level;
        if(level==null||!level.dimension().location().equals(value.dimension()))return;
        snapshot=value;received=level.getGameTime();receivedLevel=level;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        var level=Minecraft.getInstance().level;
        if(level==null||level!=receivedLevel||level.getGameTime()-received>40){snapshot=null;receivedLevel=null;}
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||snapshot==null)return;
        var mc=Minecraft.getInstance();if(mc.level==null||!mc.level.dimension().location().equals(snapshot.dimension()))return;
        Vec3 camera=event.getCamera().getPosition();double time=mc.level.getGameTime()+event.getPartialTick();
        // 先挑对拼双方；不因为距离排序截断其中一方，造成只剩一个领域。
        var fields=snapshot.fields().stream().sorted(Comparator.<DomainPresentation.Frame>comparingInt(f->f.phase()>0?0:1)
            .thenComparingDouble(f->camera.distanceToSqr(f.plane()))).limit(16).toList();
        var ps=event.getPoseStack();ps.pushPose();ps.translate(-camera.x,-camera.y,-camera.z);
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.enableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();
        try{
            for(var f:fields){
                boolean inside=camera.distanceToSqr(f.center())<f.radius()*f.radius();
                if(!f.style().texture().isEmpty()&&(inside||f.phase()>0))sphere(ps,f,camera,time,true,inside?255:85);
                if(f.phase()>0){sphere(ps,f,camera,time,false,inside?34:60);rings(ps,f,camera,time);}
            }
        }finally{RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();RenderSystem.setShaderColor(1,1,1,1);ps.popPose();}
    }
    private static Vec3 point(Vec3 center,double radius,double u,double v){
        double az=(u-.5)*Math.PI*2,lat=(.5-v)*Math.PI,c=Math.cos(lat);
        return center.add(radius*c*Math.cos(az),radius*Math.sin(lat),radius*c*Math.sin(az));
    }
    private static double surfaceRadius(DomainPresentation.Frame f,Vec3 camera){
        return camera.distanceToSqr(f.center())<f.radius()*f.radius()?Math.max(2,f.radius()-2.4):f.radius()+.3;
    }
    private static void sphere(PoseStack ps,DomainPresentation.Frame f,Vec3 camera,double time,boolean textured,int alpha){
        var b=Tesselator.getInstance().getBuilder();
        if(textured){RenderSystem.setShader(GameRenderer::getPositionTexColorShader);RenderSystem.setShaderTexture(0,new ResourceLocation(f.style().texture()));}
        else RenderSystem.setShader(GameRenderer::getPositionColorShader);
        b.begin(VertexFormat.Mode.TRIANGLES,textured?DefaultVertexFormat.POSITION_TEX_COLOR:DefaultVertexFormat.POSITION_COLOR);
        // 内视图在实际方块壳内侧，外视图在外侧；同一切面驱动两面与边缘光环。
        boolean inside=camera.distanceToSqr(f.center())<f.radius()*f.radius();
        int lon=64,lat=32;double radius=surfaceRadius(f,camera);
        for(int y=0;y<lat;y++)for(int x=0;x<lon;x++){
            double u=x/(double)lon,v=y/(double)lat,u1=(x+1)/(double)lon,v1=(y+1)/(double)lat;
            Vec3[] p={point(f.center(),radius,u,v),point(f.center(),radius,u1,v),point(f.center(),radius,u1,v1),point(f.center(),radius,u,v1)};
            List<Vec3> polygon=clip(f,Arrays.asList(p));if(polygon.size()<3)continue;
            int count=polygon.size();float[] us=new float[count],vs=new float[count];
            for(int i=0;i<count;i++){
                Vec3 direction=polygon.get(i).subtract(f.style().cameraSky()?camera:f.center());
                us[i]=(float)character.domain.DomainVisualRules.u(direction.x,direction.z); // Math.atan2
                vs[i]=(float)character.domain.DomainVisualRules.v(direction.x,direction.y,direction.z); // Math.asin
                if(f.style().texture().equals("sukuna:textures/effect/domain_sunset.png"))
                    vs[i]=(float)character.domain.DomainVisualRules.sunsetV(vs[i]);
            }
            for(int i=1;i<count;i++)us[i]=(float)character.domain.DomainVisualRules.unwrap(us[0],us[i]);
            int color=textured?0xFFFFFF:f.style().color();
            for(int i=1;i<count-1;i++)for(int index:new int[]{0,i,i+1}){
                Vec3 pos=polygon.get(index);b.vertex(ps.last().pose(),(float)pos.x,(float)pos.y,(float)pos.z);
                if(textured)b.uv(us[index]+f.style().yaw()/360f+(float)(time*f.style().spin()/7200),vs[index]);
                b.color((color>>16)&255,(color>>8)&255,color&255,alpha).endVertex();
            }
        }
        BufferUploader.drawWithShader(b.end());
    }
    private static List<Vec3> clip(DomainPresentation.Frame f,List<Vec3> input){
        if(f.phase()==0)return input;
        List<Vec3> out=new ArrayList<>();Vec3 previous=input.get(input.size()-1);
        double a=previous.subtract(f.plane()).dot(f.normal());
        for(Vec3 point:input){
            double z=point.subtract(f.plane()).dot(f.normal());
            if((a<=0)!=(z<=0))out.add(previous.lerp(point,character.domain.DomainVisualRules.intersection(a,z)));
            if(z<=0)out.add(point);previous=point;a=z;
        }
        return out;
    }
    /** 旧的整屏领域效果在对拼期间由分区画布接管，避免一方盖住另一方。 */
    public static boolean clashing(int entityId){
        var level=Minecraft.getInstance().level;if(level==null||snapshot==null)return false;
        var entity=level.getEntity(entityId);return entity!=null&&snapshot.fields().stream().anyMatch(f->f.phase()>0&&f.owner().equals(entity.getUUID()));
    }
    private static void rings(PoseStack ps,DomainPresentation.Frame f,Vec3 camera,double time){
        RenderSystem.setShader(GameRenderer::getPositionColorShader);var b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        int color=0xDD000000|(f.style().color()&0xFFFFFF);
        // 纹理、球壳和经纬线使用相同半径与切面，波纹单独在交界内部流动。
        double radius=surfaceRadius(f,camera);
        for(int ring=0;ring<3;ring++)for(int i=0;i<80;i++){
            Vec3 a=ring(f,ring,i*2*Math.PI/80,radius),z=ring(f,ring,(i+1)*2*Math.PI/80,radius);
            double da=a.subtract(f.plane()).dot(f.normal()),dz=z.subtract(f.plane()).dot(f.normal());
            if(da>0&&dz>0)continue;
            if((da<=0)!=(dz<=0)){Vec3 crossing=a.lerp(z,character.domain.DomainVisualRules.intersection(da,dz));if(da>0)a=crossing;else z=crossing;}
            Client.ribbon(b,ps,a,z,.12,color,camera);
        }
        Vec3 normal=f.normal(),right=normal.cross(new Vec3(0,1,0));if(right.lengthSqr()<.01)right=new Vec3(1,0,0);
        right=right.normalize();Vec3 up=normal.cross(right).normalize();
        double distance=f.plane().subtract(f.center()).dot(normal);
        double crossRadius=Math.sqrt(Math.max(0,radius*radius-distance*distance));
        Vec3 center=f.center().add(normal.scale(distance));
        // 交界内部也有向外扩散的小环；人在实体墙内仍能看见争夺，而不是只看见墙外的大球。
        double wave=2+(time%32)/32.0*Math.min(9,crossRadius);
        for(int i=0;i<48;i++){
            double a=i*2*Math.PI/48,z=(i+1)*2*Math.PI/48;
            Vec3 from=center.add(right.scale(Math.cos(a)*wave)).add(up.scale(Math.sin(a)*wave));
            Vec3 to=center.add(right.scale(Math.cos(z)*wave)).add(up.scale(Math.sin(z)*wave));
            Client.ribbon(b,ps,from,to,.1,color,camera);
        }
        for(int i=0;i<96;i++){
            double a=i*2*Math.PI/96,z=(i+1)*2*Math.PI/96;
            double r=crossRadius;
            Vec3 from=center.add(right.scale(Math.cos(a)*r)).add(up.scale(Math.sin(a)*r));
            Vec3 to=center.add(right.scale(Math.cos(z)*r)).add(up.scale(Math.sin(z)*r));
            Client.ribbon(b,ps,from,to,.2,0xEEFFFFFF,camera);
        }
        BufferUploader.drawWithShader(b.end());
    }
    private static Vec3 ring(DomainPresentation.Frame f,int ring,double angle,double radius){
        double a=Math.cos(angle)*radius,b=Math.sin(angle)*radius;
        return f.center().add(ring==0?a:ring==1?a:0,ring==0?0:b,ring==0?b:ring==1?0:a);
    }
    private DomainWorldRenderer(){ }
}
