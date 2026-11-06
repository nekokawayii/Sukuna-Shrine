package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/** 视线经纬投影：相邻格共享同一片天空，只画球壳可见表面，不画覆盖生物的屏幕贴片。 */
public final class DomainMaskRenderer implements BlockEntityRenderer<DomainMaskBlockEntity> {
    private static final ResourceLocation STAR=new ResourceLocation("sukuna","textures/block/yuki_star_shell.png");
    private static final ResourceLocation SUNSET=new ResourceLocation("sukuna","textures/effect/domain_sunset.png");
    private static final ResourceLocation[] IMAGES=java.util.stream.IntStream.range(0,9)
        .mapToObj(i->i<=1?SUNSET:new ResourceLocation("sukuna","textures/block/domain_screen_"+i+".png")).toArray(ResourceLocation[]::new);
    private static final float[][][] FACES={
        {{0,0,0},{1,0,0},{1,0,1},{0,0,1}},
        {{0,1,1},{1,1,1},{1,1,0},{0,1,0}},
        {{1,0,0},{0,0,0},{0,1,0},{1,1,0}},
        {{0,0,1},{1,0,1},{1,1,1},{0,1,1}},
        {{0,0,0},{0,0,1},{0,1,1},{0,1,0}},
        {{1,0,1},{1,0,0},{1,1,0},{1,1,1}}
    };
    public DomainMaskRenderer(BlockEntityRendererProvider.Context context) { }
    @Override public int getViewDistance(){return 160;}
    @Override public void render(DomainMaskBlockEntity be,float partial,PoseStack ps,MultiBufferSource buffers,int light,int overlay){
        if(be.getLevel()==null)return;
        var state=be.getBlockState();var pos=be.getBlockPos();
        boolean sunset=!state.is(Content.YUKI_STAR_SHELL.get())&&state.getValue(DomainScreenBlock.IMAGE)<=1;
        ResourceLocation texture=state.is(Content.YUKI_STAR_SHELL.get())?STAR:sunset?SUNSET:IMAGES[state.getValue(DomainScreenBlock.IMAGE)];
        VertexConsumer out=buffers.getBuffer(RenderType.entityTranslucentEmissive(texture,false));
        Vec3 camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        for(Direction face:Direction.values()){
            var next=pos.relative(face);var neighbor=be.getLevel().getBlockState(next);
            if(neighbor.getBlock() instanceof DomainMaskBlock)continue;
            if(!Block.shouldRenderFace(state,be.getLevel(),pos,face,next))continue;
            var normal=face.getNormal();Vec3 middle=Vec3.atCenterOf(pos).add(normal.getX()*.5,normal.getY()*.5,normal.getZ()*.5);
            if(camera.subtract(middle).dot(new Vec3(normal.getX(),normal.getY(),normal.getZ()))<=0)continue;
            float[][] corners=FACES[face.ordinal()];float[] us=new float[4],vs=new float[4];
            for(int i=0;i<4;i++){
                var c=corners[i];double x=pos.getX()+c[0]-camera.x,y=pos.getY()+c[1]-camera.y,z=pos.getZ()+c[2]-camera.z;
                double length=Math.max(.00001,Math.sqrt(x*x+y*y+z*z));
                us[i]=(float)(.5+Math.atan2(z,x)/(2*Math.PI));vs[i]=(float)(.5-Math.asin(Math.max(-1,Math.min(1,y/length)))/Math.PI);
                if(sunset)vs[i]=(float)character.domain.DomainVisualRules.sunsetV(vs[i]);
            }
            // 经纬接缝只跨短边，防止一张图被压缩到单格里形成亮线。
            for(int i=1;i<4;i++){while(us[i]-us[0]>.5f)us[i]-=1;while(us[i]-us[0]<-.5f)us[i]+=1;}
            for(int i=0;i<4;i++){
                var c=corners[i];out.vertex(ps.last().pose(),c[0],c[1],c[2]).color(255,255,255,255).uv(us[i],vs[i])
                    .overlayCoords(overlay).uv2(LightTexture.FULL_BRIGHT).normal(ps.last().normal(),normal.getX(),normal.getY(),normal.getZ()).endVertex();
            }
        }
    }
}
