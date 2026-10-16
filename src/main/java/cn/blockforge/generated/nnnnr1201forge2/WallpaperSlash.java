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
    private static int batchCuts,skippedNoImage,skippedNoOwner;
    @Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void shaders(RegisterShadersEvent e)throws java.io.IOException{
            e.registerShader(new ShaderInstance(e.getResourceProvider(),new ResourceLocation("sukuna","wallpaper_slash"),DefaultVertexFormat.POSITION_COLOR),s->shader=s);
        }
    }
    public static void begin(){pending.clear();batchCuts=0;collecting=true;SlashAppearance.reset();}
    public static void end(PoseStack ps){try{flush(ps);}finally{pending.clear();batchCuts=0;collecting=false;SlashAppearance.reset();}}
    public static void core(BufferBuilder b,PoseStack ps,Vec3 p,Vec3 q,Vec3 r,Vec3 s,float alpha,UUID owner){
        if(!collecting||alpha<=0)return;
        if(shader!=null&&owner!=null&&WallpaperClient.hasImage(owner))pending.add(new Cut(owner,p,q,r,s,Math.min(1,alpha)));
        else if(owner==null)skippedNoOwner++;else skippedNoImage++;
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
        Vec3 mid=a.lerp(z,.5),side=across.normalize().scale(WorldSlashRules.HALF_WIDTH);
        var mc=Minecraft.getInstance();int opacity=(int)(255*Math.min(1,alpha))<<24;double time=mc.level==null?0:mc.level.getGameTime()+mc.getFrameTime();
        Client.quad(b,ps,a,mid.add(side),z,mid.subtract(side),opacity|SlashStyle.edge(time));
        Vec3 toward=camera.subtract(mid).normalize().scale(.008);
        Vec3 p=a.lerp(z,SlashStyleRules.TIP_INSET).add(toward),q=mid.add(side.scale(SlashStyleRules.CORE)).add(toward),r=z.lerp(a,SlashStyleRules.TIP_INSET).add(toward),s=mid.subtract(side.scale(SlashStyleRules.CORE)).add(toward);
        Client.quad(b,ps,p,q,r,s,opacity|0x000000);
        core(b,ps,p,q,r,s,alpha,owner);
    }
    public static void drawBillboard(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 z,float alpha,Vec3 camera,UUID owner){
        Vec3 across=z.subtract(a).cross(camera.subtract(a.lerp(z,.5)));
        draw(b,ps,a,z,across,alpha,camera,owner);
    }
    public static void flush(PoseStack ps){
        if(pending.isEmpty()||shader==null){
            // 埋点：为什么没贴上去（没图 / 没主 / 没着色器）——每 200 刻最多写一次日志
            if(shader==null)note("着色器 wallpaper_slash 未就绪");
            else if(skippedNoImage>0||skippedNoOwner>0)note("跳过：无图 "+skippedNoImage+" 道 / 无主 "+skippedNoOwner+" 道");
            skippedNoImage=skippedNoOwner=0;
            pending.clear();return;
        }
        var mc=Minecraft.getInstance();var window=WallpaperClient.window();var desktop=WallpaperClient.desktop();
        shader.safeGetUniform("FrameSize").set((float)mc.getMainRenderTarget().width,(float)mc.getMainRenderTarget().height);
        float rx=(window.x()-desktop.x())/desktop.width(),ry=(window.y()-desktop.y())/desktop.height(),
              rw=window.width()/desktop.width(),rh=window.height()/desktop.height();
        // 退化/越界的区域会让 shader 把 uv 判到 [0,1] 之外 → 整片黑；这时退回"整张桌面"
        if(!(rw>0)||!(rh>0)||rw>4||rh>4||rx<-0.05||ry<-0.05||rx+rw>1.05||ry+rh>1.05){
            note("DesktopRegion 越界("+rx+","+ry+","+rw+","+rh+") → 本次退回整张桌面");
            rx=0;ry=0;rw=1;rh=1;
        }
        if(!logged){
            logged=true;
            com.mojang.logging.LogUtils.getLogger().info("[斩击壁纸] 贴图已接入斩击：window=({},{},{}x{}) desktop=({},{},{}x{}) region=({},{},{},{}) 本批 {} 道",
                window.x(),window.y(),window.width(),window.height(),desktop.x(),desktop.y(),desktop.width(),desktop.height(),rx,ry,rw,rh,pending.size());
        }
        shader.safeGetUniform("DesktopRegion").set(rx,ry,rw,rh);
        Map<UUID,List<Cut>> byOwner=new LinkedHashMap<>();for(Cut c:pending)byOwner.computeIfAbsent(c.owner,k->new ArrayList<>()).add(c);
        for(var entry:byOwner.entrySet()){
            ResourceLocation image=WallpaperClient.texture(entry.getKey());if(image==null)continue;
            RenderSystem.setShader(()->shader);RenderSystem.setShaderTexture(0,image);
            BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            for(Cut c:entry.getValue())Client.quad(b,ps,c.a,c.b,c.c,c.d,((int)(255*c.alpha)<<24)|0xFFFFFF);
            BufferUploader.drawWithShader(b.end());
        }
        pending.clear();RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }
    /** 诊断：每 200 刻最多一条，避免刷屏又能定位"为什么斩击是黑的"。 */
    private static long lastNote;
    private static boolean logged;
    private static void note(String what){
        long now=Minecraft.getInstance().level==null?0:Minecraft.getInstance().level.getGameTime();
        if(now-lastNote<200)return;
        lastNote=now;
        com.mojang.logging.LogUtils.getLogger().info("[斩击壁纸] {}",what);
    }
    private WallpaperSlash(){}
}
