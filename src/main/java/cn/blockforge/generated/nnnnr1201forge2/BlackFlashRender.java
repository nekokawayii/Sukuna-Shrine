package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** 黑闪表现层（客户端，**纯顶点通路自绘** + 少量 Photon 加法发光）。
 *
 *  <p>按用户设定：**以准星（命中点）为圆心，黑色火花围成圆环向外扩散；圆环中间是拳风 + 黑色火花**。</p>
 *  <ul>
 *    <li><b>黑色火花圆环</b>：28 颗黑芯火花排在一个**垂直于视线的圆**上（所以从玩家视角看就是一个圆），
 *        半径从 0.6 格扩散到 5.2 格；每颗火花＝外面一层红色辉光 + 里面一颗近黑火花，逐颗独立闪烁（波谷整颗跳过）。</li>
 *    <li><b>圆心（准星处）</b>：拳风的四层风环沿出拳方向推出去 + 一圈向内簇拥的黑色火花（10 颗）。</li>
 *    <li><b>光污染</b>：Photon 加法混合两处——命中点发光点（bloom）+ 拳风光环；顶点通路的红色辉光层负责其余光晕。</li>
 *  </ul>
 *
 *  <p>没有贴图、没有长寿命发射器：不会残留，也不吃显存/粒子内存。</p> */
public final class BlackFlashRender {
    /** 一颗火花：环上角度、半径比例（0~1）、离面抖动、大小倍率。 */
    private record Spark(double ang,double rad,double tilt,double size){}
    private static final class Flash {
        final Vec3 pos,dir;final long born;final List<Spark> ring=new ArrayList<>(),core=new ArrayList<>();
        Flash(Vec3 pos,Vec3 dir,long born,Random rnd){
            this.pos=pos;this.dir=dir;this.born=born;
            for(int i=0;i<SPARKS;i++){
                double ang=(i+ rnd.nextDouble()*0.5)*Math.PI*2/SPARKS;
                ring.add(new Spark(ang,0.9+rnd.nextDouble()*0.2,(rnd.nextDouble()-0.5)*0.5,0.8+rnd.nextDouble()*0.5));
            }
            for(int i=0;i<CORE_SPARKS;i++)
                core.add(new Spark(rnd.nextDouble()*Math.PI*2,rnd.nextDouble()*0.45,(rnd.nextDouble()-0.5)*0.8,0.7+rnd.nextDouble()*0.6));
        }
    }
    private static final List<Flash> FLASHES=new ArrayList<>();
    private static final Random RND=new Random();
    /** 可调参数。 */
    private static final int SPARKS=28,CORE_SPARKS=10,LIFE=12,WIND_LIFE=10;
    private static final double RING_MIN=0.6,RING_MAX=5.2,SPARK_SIZE=0.26,WIND_RADIUS=4.2;
    private static final int RED=0xFFE02418,BLACK=0xFF060606,WIND=0xFFE6ECF2;
    private static final int BLOOM_RED=0xFF8A1208;
    private static final String GLOW="textures/effect/glow.png";

    /** 命中点（＝准星指向处）触发。{@code dir} = 出拳方向（可为 {@link Vec3#ZERO}）。 */
    public static void play(Vec3 pos,Vec3 dir){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        Vec3 d=dir==null||dir.lengthSqr()<1e-6?Vec3.ZERO:dir.normalize();
        FLASHES.add(new Flash(pos,d,mc.level.getGameTime(),RND));
        if(FLASHES.size()>16)FLASHES.remove(0);
        // 光污染：命中点 bloom 发光 + 拳风处加法光环（都短命）
        try{
            PhotonFx.spark(pos,0.5,0.40,0xFF2A10,BLOOM_RED,GLOW,true,1,8,20,0.5);
            if(d.lengthSqr()>1e-6)PhotonFx.ring(pos.add(d.scale(1.6)),2.4,0.8,0xFFD8E6F2,BLOOM_RED,26,2,9);
        }catch(Throwable ignored){}
        Client.shake(12);
    }
    public static void tick(){}
    public static void clear(){FLASHES.clear();}

    public static void draw(BufferBuilder b,PoseStack ps,Vec3 cam,float partial){
        if(FLASHES.isEmpty())return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        for(Flash f:FLASHES){
            double age=mc.level.getGameTime()-f.born+partial;
            if(age<0||age>LIFE)continue;
            double life=Math.max(0,1-age/LIFE);
            // 圆环所在平面：垂直于视线（玩家看过去就是一圈）
            Vec3 n=cam.subtract(f.pos);n=n.lengthSqr()<1e-6?new Vec3(0,0,1):n.normalize();
            Vec3 ref=Math.abs(n.y)>0.9?new Vec3(1,0,0):new Vec3(0,1,0);
            Vec3 e1=n.cross(ref).normalize(),e2=n.cross(e1).normalize();
            // ---- 黑色火花圆环：半径扩散 ----
            double rad=RING_MIN+(RING_MAX-RING_MIN)*DismantleMotionRules.phase(age,0,LIFE*0.8);
            for(Spark s:f.ring)spark(b,ps,f.pos,e1,e2,s,rad,life,age);
            // ---- 圆心：向内簇拥的黑色火花 ----
            for(Spark s:f.core)spark(b,ps,f.pos,e1,e2,s,RING_MIN*0.9+s.rad()*1.4,life,age);
            // ---- 圆心：拳风四层风环（沿出拳方向推出去） ----
            if(f.dir.lengthSqr()>1e-6&&age<=WIND_LIFE){
                double wl=Math.max(0,1-age/WIND_LIFE),r=WIND_RADIUS*(0.35+0.85*(age/WIND_LIFE));
                int a=(int)(185*wl*wl)<<24;
                windRing(b,ps,f.pos.add(f.dir.scale(0.5+r*0.45)),f.dir,r*1.05,0.52,a|(WIND&0xFFFFFF),cam);
                windRing(b,ps,f.pos.add(f.dir.scale(0.4+r*0.9)),f.dir,r*0.85,0.34,(int)(a*0.85)<<24|(WIND&0xFFFFFF),cam);
                windRing(b,ps,f.pos.add(f.dir.scale(0.3+r*1.4)),f.dir,r*0.65,0.22,(int)(a*0.65)<<24|(WIND&0xFFFFFF),cam);
                windRing(b,ps,f.pos.add(f.dir.scale(0.8+r*1.9)),f.dir,r*1.35,0.14,(int)(a*0.45)<<24|(WIND&0xFFFFFF),cam);
            }
        }
    }

    /** 画一颗火花：红色辉光 + 近黑芯，逐颗独立闪烁。 */
    private static void spark(BufferBuilder b,PoseStack ps,Vec3 center,Vec3 e1,Vec3 e2,Spark s,double rad,double life,double age){
        double wave=Math.sin(age*1.9+s.ang()*3.1);
        if(wave<-0.30)return;                                     // 整颗跳过 = 闪烁的"炸断"感
        double a=(0.25+0.75*Math.abs(wave))*life;
        Vec3 p=center.add(e1.scale(Math.cos(s.ang())*rad)).add(e2.scale(Math.sin(s.ang())*rad)).add(0,s.tilt(),0);
        double size=SPARK_SIZE*s.size();
        quad(b,ps,p,size*2.4,(int)(a*70)<<24|(RED&0xFFFFFF));     // 外：红色辉光（光污染）
        quad(b,ps,p,size,(int)(a*235)<<24|(BLACK&0xFFFFFF));      // 内：近黑火花
    }
    /** 相机朝向的方形面片。 */
    private static void quad(BufferBuilder b,PoseStack ps,Vec3 c,double r,int color){
        var cam=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 n=cam.subtract(c);n=n.lengthSqr()<1e-6?new Vec3(0,0,1):n.normalize();
        Vec3 up=new Vec3(0,1,0).subtract(n.scale(new Vec3(0,1,0).dot(n)));
        if(up.lengthSqr()<1e-4)up=new Vec3(1,0,0).subtract(n.scale(n.x));
        up=up.normalize();Vec3 right=n.cross(up).normalize();
        Vec3 u=up.scale(r).add(right.scale(r)),w=up.scale(r).subtract(right.scale(r));
        Client.quad(b,ps,c.add(u),c.add(w),c.subtract(u),c.subtract(w),color);
    }
    /** 垂直于 {@code dir} 的圆环（16 段面片拼成，不依赖 Photon）。 */
    private static void windRing(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 dir,double r,double thick,int color,Vec3 cam){
        Vec3 n=dir.normalize();
        Vec3 ref=Math.abs(n.y)>0.9?new Vec3(1,0,0):new Vec3(0,1,0);
        Vec3 e1=n.cross(ref).normalize(),e2=n.cross(e1).normalize();
        int seg=16;
        for(int i=0;i<seg;i++){
            double a0=i*Math.PI*2/seg,a1=(i+1)*Math.PI*2/seg;
            Vec3 p0=c.add(e1.scale(Math.cos(a0)*r)).add(e2.scale(Math.sin(a0)*r));
            Vec3 p1=c.add(e1.scale(Math.cos(a1)*r)).add(e2.scale(Math.sin(a1)*r));
            Vec3 mid=p0.lerp(p1,.5),across=p1.subtract(p0).cross(cam.subtract(mid));
            if(across.lengthSqr()<1e-8)continue;
            Vec3 side=across.normalize().scale(thick);
            Client.quad(b,ps,p0.add(side),p1.add(side),p1.subtract(side),p0.subtract(side),color);
        }
    }
    private BlackFlashRender(){}
}
