package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** 黑闪表现层（客户端，**自绘**，不依赖任何外部 .fx）：
 *
 *  <p>构成（按命中后经过的刻数）：<br>
 *  ① **纯黑核心**：0~10 刻，三片相机朝向的黑面片由 0.9 格膨胀到 2.0 格再淡出——Photon 的加法混合画不出纯黑，
 *     所以这一块走模组自己的顶点通路（同无量空处黑盘的做法）；<br>
 *  ② **红黑放射丝线**：0 刻一次性铺 24 道带物理的细丝，径向初速 1.5~3.0 格/刻、带随机抖动，
 *     PhotonFx.lines + streak 贴图，看起来就是"空间被撕开的红黑裂纹"；<br>
 *  ③ **冲击环**：0 刻一圈红环向外炸开；<br>
 *  ④ **黑红碎屑**：0 刻 30 个缓慢外扩的暗色火团，给核心一点体积感。</p>
 *
 *  <p>屏幕演出（尖刺 + 屏幕震动）在 {@link SoulClient} 里，音效在服务端（{@code sukuna:black_flash}）。</p> */
public final class BlackFlashRender {
    private static final class Flash {
        final Vec3 pos;final long born;
        Flash(Vec3 pos,long born){this.pos=pos;this.born=born;}
    }
    private static final List<Flash> FLASHES=new ArrayList<>();
    private static long tick;
    private static final String GLOW="textures/effect/glow.png",STREAK="textures/effect/streak.png";
    /** bloom 参数（发光色，同 FurnaceMaxClient 的写法）：红黑主题。 */
    private static final int BLOOM_RED=0xFF7A0A06,BLOOM_DARK=0xFF2A0A0A;
    /** 黑核寿命（刻）与半径范围；红黑丝线/环/碎屑的寿命也在这一组常数里。 */
    private static final int CORE_LIFE=10,STRAND_LIFE=9,RING_LIFE=7,DEBRIS_LIFE=10;
    private static final double CORE_MIN=0.9,CORE_MAX=2.0;

    /** 命中点触发一次黑闪（由 {@link SoulClient#receive} 在特效包 kind=85 时调用）。 */
    public static void play(Vec3 pos){
        if(Minecraft.getInstance().level==null)return;
        long now=Minecraft.getInstance().level.getGameTime();
        FLASHES.add(new Flash(pos,now));
        if(FLASHES.size()>24)FLASHES.remove(0);   // 极端连打时只保留最近的，避免无限堆积
        try{
            // ② 红黑放射丝线：径向初速 + 抖动，像裂开的空间
            PhotonFx.lines(pos,0.35,0.06,0.24,0xFF2A08,BLOOM_RED,2.2,0.015,24,1,STRAND_LIFE,false);
            // ③ 冲击环：红环外炸
            PhotonFx.ring(pos,1.1,0.34,0xFFFF3A18,BLOOM_RED,42,2,RING_LIFE);
            // ④ 黑红碎屑：缓慢外扩的暗色火团
            PhotonFx.blobs(pos,0.55,0.16,0.30,0x50100808,BLOOM_DARK,-1.2,0.02,26,2,DEBRIS_LIFE);
        }catch(Throwable ignored){/* 没装 Photon：只剩黑核与屏幕演出，不影响判定与音效 */}
        Client.shake(8);
    }
    /** 每刻推进（由 SoulClient.tick 调用）。 */
    public static void tick(){tick++;}
    /** 世界渲染：黑核（在 {@link SoulClient#draw} 的同一批里画，坐标已是相机相对）。 */
    public static void draw(BufferBuilder b,PoseStack ps,Vec3 cam,float partial){
        if(FLASHES.isEmpty())return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        for(Flash f:FLASHES){
            double age=f.born>=0?mc.level.getGameTime()-f.born+partial:0;
            if(age<0||age>CORE_LIFE)continue;
            double p=Math.max(0,Math.min(1,age/CORE_LIFE));
            double r=CORE_MIN+(CORE_MAX-CORE_MIN)*DismantleMotionRules.phase(age,0,CORE_LIFE*0.6);
            int alpha=(int)(205*(1-p*p))<<24;                       // 先实后淡
            Vec3 c=f.pos,view=cam.subtract(c);
            Vec3 n=view.lengthSqr()<1e-6?new Vec3(0,0,1):view.normalize();
            Vec3 up=new Vec3(0,1,0).subtract(n.scale(new Vec3(0,1,0).dot(n)));
            if(up.lengthSqr()<1e-4)up=new Vec3(1,0,0).subtract(n.scale(n.x));
            up=up.normalize();Vec3 right=n.cross(up).normalize();
            // 三片同色黑面片错开角度，从任何方向看都是一团实心黑（纯黑靠顶点颜色，加法混合不会把它冲淡）
            for(int layer=0;layer<3;layer++){
                double a=layer*Math.PI/3,rr=r*(1-layer*0.16);
                Vec3 u=up.scale(Math.cos(a)).add(right.scale(Math.sin(a))).scale(rr);
                Vec3 w=up.scale(-Math.sin(a)).add(right.scale(Math.cos(a))).scale(rr);
                Client.quad(b,ps,c.add(u).add(w),c.subtract(u).add(w),c.subtract(u).subtract(w),c.add(u).subtract(w),alpha|0x000000);
            }
        }
    }
    /** 清理（死亡/换维度/断线）。 */
    public static void clear(){FLASHES.clear();}
    private BlackFlashRender(){}
}
