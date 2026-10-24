package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 黑芯负责未授权或读取失败时的回退；壁纸按屏幕坐标覆盖同一尖梭内部。 */
public final class WallpaperSlash {
    private record Cut(UUID owner,Vec3 a,Vec3 b,Vec3 c,Vec3 d,float alpha){}
    // 满批即提交并继续，不能只给前1024道斩击贴壁纸；大量墙、网不无限积压。
    private static final int BATCH_SIZE=512;
    private static final List<Cut> pending=new ArrayList<>(BATCH_SIZE);
    private static ShaderInstance shader;
    private static boolean collecting;
    private static int batchCuts;
    @Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void shaders(RegisterShadersEvent e){
            shader=null;try{e.registerShader(new ShaderInstance(e.getResourceProvider(),new ResourceLocation("sukuna","wallpaper_slash"),DefaultVertexFormat.POSITION_COLOR),s->shader=s);}catch(java.io.IOException|RuntimeException ex){com.mojang.logging.LogUtils.getLogger().warn("壁纸专用材质不可用，斩击保留原版贴图通道",ex);}
        }
    }
    public static void begin(){pending.clear();batchCuts=0;collecting=true;SlashAppearance.reset();}
    public static void end(PoseStack ps){try{flush(ps);}finally{pending.clear();batchCuts=0;collecting=false;SlashAppearance.reset();}}
    public static void core(BufferBuilder b,PoseStack ps,Vec3 p,Vec3 q,Vec3 r,Vec3 s,float alpha,UUID owner){
        if(!collecting||alpha<=0)return;
        if(owner!=null&&WallpaperClient.hasImage(owner))pending.add(new Cut(owner,p,q,r,s,Math.min(1,alpha)));
        // 黑芯和包边也占绘图内存：关闭共享、下载中或低阶段同样按斩击数分批。
        if(++batchCuts>=BATCH_SIZE){
            BufferUploader.drawWithShader(b.end());flush(ps);batchCuts=0;
            b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        }
    }
    public static void draw(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 z,Vec3 across,float alpha,Vec3 camera,UUID owner){
        if(alpha<=0||a.distanceToSqr(z)<1e-8)return;
        if(across.lengthSqr()<1e-8)across=z.subtract(a).cross(camera.subtract(a.lerp(z,.5)));
        if(across.lengthSqr()<1e-8)across=z.subtract(a).cross(new Vec3(0,1,0));
        if(across.lengthSqr()<1e-8)across=z.subtract(a).cross(new Vec3(1,0,0));
        // 【睁眼】虚空/传承阶段：世界斩同样先是一道闭合黑线，再张开露出壁纸。
        boolean eye=SlashAppearance.desktop();
        double half=WorldSlashRules.HALF_WIDTH;if(eye)half=SlashEye.width(half,SlashAppearance.open());
        Vec3 mid=a.lerp(z,.5),side=across.normalize().scale(half);
        var mc=Minecraft.getInstance();int opacity=(int)(255*Math.min(1,alpha))<<24;double time=mc.level==null?0:mc.level.getGameTime()+mc.getFrameTime();
        Client.quad(b,ps,a,mid.add(side),z,mid.subtract(side),opacity|(eye?SlashEye.RIM:SlashStyle.edge(time)));
        Vec3 toward=camera.subtract(mid).normalize().scale(.008);
        Vec3 p=a.lerp(z,SlashStyleRules.TIP_INSET).add(toward),q=mid.add(side.scale(SlashStyleRules.CORE)).add(toward),r=z.lerp(a,SlashStyleRules.TIP_INSET).add(toward),s=mid.subtract(side.scale(SlashStyleRules.CORE)).add(toward);
        Client.quad(b,ps,p,q,r,s,opacity|0x000000);
        core(b,ps,p,q,r,s,alpha,owner);
    }
    public static void drawBillboard(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 z,float alpha,Vec3 camera,UUID owner){
        Vec3 across=z.subtract(a).cross(camera.subtract(a.lerp(z,.5)));
        draw(b,ps,a,z,across,alpha,camera,owner);
    }
    /** 屏幕空间的"裂口/窗口"效果可以直接拿这套着色器：它按 gl_FragCoord 采样，与几何在哪无关。 */
    public static ShaderInstance shader(){return shader;}
    /** 绑定桌面采样需要的两个 uniform（FrameSize / DesktopRegion），调用前必须先 setShader(shader())。 */
    public static void desktopUniforms(){
        if(shader==null)return;var mc=Minecraft.getInstance();var window=WallpaperClient.window();var desktop=WallpaperClient.desktop();
        shader.safeGetUniform("FrameSize").set((float)mc.getMainRenderTarget().width,(float)mc.getMainRenderTarget().height);
        shader.safeGetUniform("DesktopRegion").set((window.x()-desktop.x())/desktop.width(),(window.y()-desktop.y())/desktop.height(),window.width()/desktop.width(),window.height()/desktop.height());
    }
    public static void flush(PoseStack ps){
        if(pending.isEmpty())return;
        // 普通斩击、剑气和分段墙统一先裁切，再绘制屏幕多边形，仍受地形遮挡。
        // 专用通道不受场景雾染黑，显式绑定图片；资源重载失败时保留原版贴图兜底。
        var window=WallpaperClient.window();var desktop=WallpaperClient.desktop();
        double ox=(window.x()-desktop.x())/desktop.width(),oy=(window.y()-desktop.y())/desktop.height(),sx=window.width()/desktop.width(),sy=window.height()/desktop.height();
        org.joml.Matrix4f projection=new org.joml.Matrix4f(RenderSystem.getProjectionMatrix());var sorting=RenderSystem.getVertexSorting();
        var modelView=RenderSystem.getModelViewStack();org.joml.Matrix4f transform=new org.joml.Matrix4f(projection).mul(RenderSystem.getModelViewMatrix()).mul(ps.last().pose());
        int texture=RenderSystem.getShaderTexture(0);float[] color=RenderSystem.getShaderColor().clone();
        Map<UUID,List<Cut>> byOwner=new LinkedHashMap<>();for(Cut c:pending)byOwner.computeIfAbsent(c.owner,k->new ArrayList<>()).add(c);
        modelView.pushPose();try{
            modelView.setIdentity();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(new org.joml.Matrix4f(),sorting);RenderSystem.setShaderColor(1,1,1,1);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();
            for(var entry:byOwner.entrySet()){
                ResourceLocation image=WallpaperClient.texture(entry.getKey());if(image==null)continue;
                boolean dedicated=shader!=null;
                RenderSystem.setShaderTexture(0,image);
                if(dedicated){RenderSystem.setShader(()->shader);shader.setSampler("Sampler0",Minecraft.getInstance().getTextureManager().getTexture(image));desktopUniforms();}
                else RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
                BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.TRIANGLES,dedicated?DefaultVertexFormat.POSITION_COLOR:DefaultVertexFormat.POSITION_TEX_COLOR);
                for(Cut c:entry.getValue()){
                    var polygon=WallpaperProjection.clip(List.of(project(transform,c.a),project(transform,c.b),project(transform,c.c),project(transform,c.d)));
                    for(int i=1;i+1<polygon.size();i++){screenVertex(b,polygon.get(0),c.alpha,ox,oy,sx,sy);screenVertex(b,polygon.get(i),c.alpha,ox,oy,sx,sy);screenVertex(b,polygon.get(i+1),c.alpha,ox,oy,sx,sy);}
                }
                BufferUploader.drawWithShader(b.end());
            }
        }finally{pending.clear();modelView.popPose();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,sorting);RenderSystem.setShaderTexture(0,texture);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);RenderSystem.setShader(GameRenderer::getPositionColorShader);}
    }
    private static WallpaperProjection.Point project(org.joml.Matrix4f matrix,Vec3 p){var v=new org.joml.Vector4f((float)p.x,(float)p.y,(float)p.z,1).mul(matrix);return new WallpaperProjection.Point(v.x,v.y,v.z,v.w);}
    private static void screenVertex(BufferBuilder b,WallpaperProjection.Point p,float alpha,double ox,double oy,double sx,double sy){double x=p.x()/p.w(),y=p.y()/p.w(),z=p.z()/p.w();double[] uv=WallpaperProjection.uv(x,y,ox,oy,sx,sy);b.vertex(x,y,z-.000001);if(shader==null)b.uv((float)uv[0],(float)uv[1]);b.color(1f,1f,1f,alpha).endVertex();}
    private WallpaperSlash(){}
}
