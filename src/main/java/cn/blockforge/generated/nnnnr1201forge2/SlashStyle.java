package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** 全部斩击共用尖梭轮廓；高阶段按施术者共享选择显示壁纸，包边缓慢由白变黑。 */
public final class SlashStyle {
    public static int edge(double time){return SlashStyleRules.edge(time);}
    public static void draw(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 z,double width,float alpha,Vec3 camera){
        if(alpha<=0||a.distanceToSqr(z)<1e-10)return;
        alpha=Math.min(1,alpha);Vec3 axis=z.subtract(a).normalize(),mid=a.lerp(z,.5),side=axis.cross(camera.subtract(mid));
        if(side.lengthSqr()<1e-8)side=axis.cross(new Vec3(0,1,0));
        if(side.lengthSqr()<1e-8)side=axis.cross(new Vec3(1,0,0));
        // 【睁眼】全部斩击露壁纸的阶段里，斩击先闭合成一条黑线，再按进度张开；其余阶段宽度不变。
        boolean eye=SlashAppearance.desktop();
        double half=Math.max(.012,width);if(eye)half=SlashEye.width(half,SlashAppearance.open());
        side=side.normalize().scale(half);
        var mc=Minecraft.getInstance();double time=mc.level==null?0:mc.level.getGameTime()+mc.getFrameTime();int opacity=(int)(255*alpha)<<24;
        int rim=eye?SlashEye.RIM:edge(time);
        Client.quad(b,ps,a,mid.add(side),z,mid.subtract(side),opacity|rim);
        Vec3 toward=camera.subtract(mid).normalize().scale(.006);
        Vec3 p=a.lerp(z,SlashStyleRules.TIP_INSET).add(toward),q=mid.add(side.scale(SlashStyleRules.CORE)).add(toward),r=z.lerp(a,SlashStyleRules.TIP_INSET).add(toward),s=mid.subtract(side.scale(SlashStyleRules.CORE)).add(toward);
        Client.quad(b,ps,p,q,r,s,opacity|(eye?SlashEye.RIM:0x020202));
        WallpaperSlash.core(b,ps,p,q,r,s,alpha,SlashAppearance.desktop()?SlashAppearance.source():null);
    }
    /** 斩痕分叉与尾迹：低阶段保留原效果，高阶段也显示壁纸。 */
    public static void detail(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 z,double width,int color,Vec3 camera){
        if(SlashAppearance.desktop())draw(b,ps,a,z,width,((color>>>24)&255)/255f,camera);
        else Client.ribbon(b,ps,a,z,width,color,camera);
    }
    private SlashStyle(){}
}
