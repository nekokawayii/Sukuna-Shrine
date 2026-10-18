package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.*;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * 灵魂通道北端那道常驻裂隙的表现层：撕裂的黑色口子，里面是**玩家的电脑桌面**
 * （复用斩击壁纸那条按屏幕坐标采样的管线，所以真的是"从裂口里看见桌面"而不是贴一张图）。
 * 没有开启壁纸共享时退化成一块没有信号的暗屏 + 红色乱码条，不影响剧情判定。
 */
public final class BeyondRiftClient {
    private static final int SLICES=56;
    private static final double MARGIN=0.6;
    private static boolean logged;
    public static void draw(BufferBuilder b,PoseStack ps,Vec3 cam,float partial){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null)return;
        if(mc.level.dimension()!=SoulJourney.CORRIDOR){logged=false;return;}
        if(cam.distanceToSqr(BeyondRift.CENTER)>256*256)return;
        double time=mc.level.getGameTime()+partial;
        UUID self=mc.player.getUUID();
        boolean desktop=WallpaperClient.hasImage(self);
        if(!logged){logged=true;com.mojang.logging.LogUtils.getLogger().info("[裂隙] 开始绘制：桌面壁纸={} 玩家={}",desktop?"可用":"不可用（未共享或读取失败，走暗屏回退）",self);}
        double z=BeyondRift.CENTER.z;
        int n=SLICES;
        double[] xs=new double[n+1],top=new double[n+1],bot=new double[n+1];
        for(int i=0;i<=n;i++){
            double t=i/(double)n;
            double env=Math.pow(Math.max(0,Math.sin(Math.PI*t)),0.62);
            double breathe=0.05*Math.sin(time*0.055+i*1.31);
            xs[i]=BeyondRift.CENTER.x-BeyondRift.HALF_WIDTH+2*BeyondRift.HALF_WIDTH*t;
            top[i]=BeyondRift.CENTER.y+BeyondRift.HALF_HEIGHT*env*(0.82+0.30*noise(i)+breathe);
            bot[i]=BeyondRift.CENTER.y-BeyondRift.HALF_HEIGHT*env*(0.82+0.30*noise(i+7919)-breathe);
        }
        // ① 撕开的黑色边缘：形状比内层大一圈，看起来是"从墙里被撕出来的口子"
        for(int i=0;i<n;i++){
            double x0=xs[i]-(i==0?MARGIN:0),x1=xs[i+1]+(i==n-1?MARGIN:0);
            Client.quad(b,ps,new Vec3(x0,top[i]+MARGIN,z),new Vec3(x1,top[i+1]+MARGIN,z),new Vec3(x1,bot[i+1]-MARGIN,z),new Vec3(x0,bot[i]-MARGIN,z),0xFF040108);
        }
        // ② 裂口内部：玩家的电脑桌面（壁纸共享管线的屏幕坐标采样）
        for(int i=0;i<n;i++){
            if(top[i]<=bot[i])continue;
            Vec3 p0=new Vec3(xs[i],top[i],z+0.02),p1=new Vec3(xs[i+1],top[i+1],z+0.02),p2=new Vec3(xs[i+1],bot[i+1],z+0.02),p3=new Vec3(xs[i],bot[i],z+0.02);
            if(desktop)WallpaperSlash.core(b,ps,p0,p1,p2,p3,1f,self);
            else Client.quad(b,ps,p0,p1,p2,p3,0x6A1E3E66);
        }
        if(!desktop)for(int i=0;i<16;i++){   // 没共享壁纸：暗屏 + 扫描线，别留一块纯黑
            double y=BeyondRift.CENTER.y-BeyondRift.HALF_HEIGHT*0.5+i*(BeyondRift.HALF_HEIGHT*0.066);
            double w=BeyondRift.HALF_WIDTH*0.55*(0.55+0.45*noise(i*31));
            Client.quad(b,ps,new Vec3(BeyondRift.CENTER.x-w,y,z+0.04),new Vec3(BeyondRift.CENTER.x+w,y,z+0.04),new Vec3(BeyondRift.CENTER.x+w,y+0.05,z+0.04),new Vec3(BeyondRift.CENTER.x-w,y+0.05,z+0.04),i%3==0?0x2AFF3A4C:0x1AFFFFFF);
        }
        // ③ 红色乱码条：位置、长度、粗细都随时间跳，模拟"越界数据"（限制在裂口内部）
        for(int i=0;i<10;i++){
            double seed=i*13.7,phase=time*0.9+seed;
            double pick=fract(Math.sin(phase*1.7+seed)*43758.5453);
            if(pick<0.42)continue;
            double x0=BeyondRift.CENTER.x-BeyondRift.HALF_WIDTH*0.82+fract(Math.sin(seed*5.3)*3517.2)*BeyondRift.HALF_WIDTH*1.2;
            double width=(0.2+0.6*fract(Math.sin(phase*0.7)*7412.9))*BeyondRift.HALF_WIDTH*0.9;
            double env=env(x0+width*0.5);
            if(env<0.06)continue;
            double y=BeyondRift.CENTER.y+(fract(Math.sin(seed*3.1)*9123.4)-0.5)*BeyondRift.HALF_HEIGHT*1.4*env;
            double h=0.05+0.18*fract(Math.sin(phase*2.3)*1731.7);
            int color=pick>0.82?0xA6FFFFFF:0x8CFF2A3C;
            Client.quad(b,ps,new Vec3(x0,y-h,z+0.06),new Vec3(x0+width,y-h,z+0.06),new Vec3(x0+width,y+h,z+0.06),new Vec3(x0,y+h,z+0.06),color);
        }
        // ④ 上下缘的暗红描边，让裂口在漆黑里也有轮廓
        for(int i=0;i<n;i++){
            edge(b,ps,xs[i],top[i],xs[i+1],top[i+1],0.085,0xFF5E0B18,z+0.05);
            edge(b,ps,xs[i],bot[i],xs[i+1],bot[i+1],0.085,0xFF4A0712,z+0.05);
        }
        // ⑤ 偶尔一道竖直撕裂
        double tear=fract(Math.sin(time*0.11)*9871.3);
        if(tear>0.87){
            double x=BeyondRift.CENTER.x+(fract(Math.sin(time*0.07)*3571.7)-0.5)*BeyondRift.HALF_WIDTH*1.1;
            double env=env(x);
            if(env>0.12){
                double half=BeyondRift.HALF_HEIGHT*env*(0.4+0.5*fract(Math.sin(time*0.23)*1317.3));
                Client.quad(b,ps,new Vec3(x-0.05,BeyondRift.CENTER.y+half,z+0.07),new Vec3(x+0.05,BeyondRift.CENTER.y+half,z+0.07),new Vec3(x+0.05,BeyondRift.CENTER.y-half,z+0.07),new Vec3(x-0.05,BeyondRift.CENTER.y-half,z+0.07),0xA8FF5060);
            }
        }
    }
    /** 平滑的裂口轮廓：与切片同一套 sin^0.62 包络，用来把乱码条限制在口子内部。 */
    private static double env(double x){
        double t=(x-BeyondRift.CENTER.x+BeyondRift.HALF_WIDTH)/(2*BeyondRift.HALF_WIDTH);
        if(t<=0||t>=1)return 0;
        return Math.pow(Math.sin(Math.PI*t),0.62);
    }
    private static void edge(BufferBuilder b,PoseStack ps,double x0,double y0,double x1,double y1,double width,int color,double z){
        Client.quad(b,ps,new Vec3(x0,y0+width,z),new Vec3(x1,y1+width,z),new Vec3(x1,y1-width,z),new Vec3(x0,y0-width,z),color);
    }
    private static double noise(int i){return fract(Math.sin(i*12.9898+78.233)*43758.5453);}
    private static double fract(double v){return v-Math.floor(v);}
    private BeyondRiftClient(){}
}
