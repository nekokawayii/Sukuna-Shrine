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
        @SubscribeEvent public static void shaders(RegisterShadersEvent e)throws java.io.IOException{
            e.registerShader(new ShaderInstance(e.getResourceProvider(),new ResourceLocation("sukuna","wallpaper_slash"),DefaultVertexFormat.POSITION_COLOR),s->shader=s);
        }
    }
    public static void begin(){pending.clear();batchCuts=0;collecting=true;SlashAppearance.reset();}
    public static void end(PoseStack ps){try{flush(ps);}finally{pending.clear();batchCuts=0;collecting=false;SlashAppearance.reset();}}
    public static void core(BufferBuilder b,PoseStack ps,Vec3 p,Vec3 q,Vec3 r,Vec3 s,float alpha,UUID owner){
        if(!collecting||alpha<=0)return;
        if(shader!=null&&owner!=null&&WallpaperClient.hasImage(owner))pending.add(new Cut(owner,p,q,r,s,Math.min(1,alpha)));
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
        if(pending.isEmpty()||shader==null){pending.clear();return;}var mc=Minecraft.getInstance();var window=WallpaperClient.window();var desktop=WallpaperClient.desktop();
        shader.safeGetUniform("FrameSize").set((float)mc.getMainRenderTarget().width,(float)mc.getMainRenderTarget().height);
        shader.safeGetUniform("DesktopRegion").set((window.x()-desktop.x())/desktop.width(),(window.y()-desktop.y())/desktop.height(),window.width()/desktop.width(),window.height()/desktop.height());
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
    private WallpaperSlash(){}
}
