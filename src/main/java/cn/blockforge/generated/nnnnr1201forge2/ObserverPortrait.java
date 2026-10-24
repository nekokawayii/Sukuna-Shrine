package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.awt.image.BufferedImage;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.joml.Matrix4f;

/** 只把已有虎杖模型画进独立透明缓冲，不截取游戏窗口或桌面。 */
public final class ObserverPortrait {
    private static TextureTarget target;
    private static final int[] lastAge={-1,-1};
    private static boolean broken;
    public static void draw(GuiGraphics g,net.minecraft.world.entity.LivingEntity entity,int age,boolean boss){
        int slot=boss?1:0;
        if(broken||!ObserverWindow.desktopAllowed()||!ObserverDesktop.available()||age==lastAge[slot]||!boss&&age%2!=0)return;lastAge[slot]=age;
        var mc=Minecraft.getInstance();g.flush();
        Matrix4f projection=new Matrix4f(RenderSystem.getProjectionMatrix());VertexSorting sorting=RenderSystem.getVertexSorting();
        var model=RenderSystem.getModelViewStack();model.pushPose();
        try{
            if(target==null)target=new TextureTarget(160,200,true,Minecraft.ON_OSX);
            target.setClearColor(0,0,0,0);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,160,200,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            model.setIdentity();model.translate(0,0,-11000);RenderSystem.applyModelViewMatrix();
            GuiGraphics portrait=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
            InventoryScreen.renderEntityInInventoryFollowsMouse(portrait,80,184,76,12,0,entity);portrait.flush();
            // Screenshot.takeScreenshot 使用 RGB，会丢失透明通道，不能用于桌面角色。
            RenderSystem.bindTexture(target.getColorTextureId());
            try(var image=new NativeImage(160,200,false)){
                image.downloadTexture(0,false);image.flipY();
                BufferedImage out=new BufferedImage(160,200,BufferedImage.TYPE_INT_ARGB);
                for(int y=0;y<200;y++)for(int x=0;x<160;x++){int c=image.getPixelRGBA(x,y);out.setRGB(x,y,(c&0xFF00FF00)|(c&255)<<16|(c>>16&255));}
                ObserverDesktop.portrait(out,boss);
            }
        }catch(RuntimeException|LinkageError ex){broken=true;com.mojang.logging.LogUtils.getLogger().warn("高位桌面角色绘制失败：{}",ex.toString());}
        finally{model.popPose();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,sorting);mc.getMainRenderTarget().bindWrite(true);}
    }
    public static void clear(){if(target!=null){target.destroyBuffers();target=null;}java.util.Arrays.fill(lastAge,-1);ObserverDesktop.portrait(null,false);ObserverDesktop.portrait(null,true);}
    private ObserverPortrait(){}
}
