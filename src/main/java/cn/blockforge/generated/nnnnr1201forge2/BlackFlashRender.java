package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** 黑闪表现层（客户端，纯顶点通路自绘 + 少量 Photon 加法发光）。
 *
 *  <p>按用户给的原作截图定形态：**又粗又长的黑芯红边闪电**向四面八方劈开 + **大量黑色火花（红边）**散布，
 *  两样都要多；圆心仍是准星所指的命中点，拳风照旧从圆心推出。</p>
 *  <ul>
 *    <li><b>黑色闪电</b>：{@link #BOLTS} 道，每道 10~14 段折线、长 9~14 格、粗 0.34~0.55（越往外越细），
 *        三层绘制＝红色辉光（2.3 倍宽）+ 红层 + 近黑芯；每段独立闪烁（波谷整段跳过）。</li>
 *    <li><b>黑色火花</b>：{@link #SPARKS} 颗，从圆心向外飞散，每颗＝红色辉光 + 近黑方片，逐颗独立闪烁；
 *        另有 {@link #CORE_SPARKS} 颗贴着圆心。</li>
 *    <li><b>拳风</b>：四层风环沿出拳方向推出。</li>
 *    <li><b>光污染</b>：命中点 Photon 加法发光点 + 拳风光环；顶点通路的红辉光层负责其余光晕。</li>
 *  </ul> */
public final class BlackFlashRender {
    private record Bolt(double[] yaw,double[] pitch,double len,double width){}
    private record Spark(double yaw,double pitch,double speed,double size,double phase){}
    private static final class Flash {
        final Vec3 pos,dir;final long born;final List<Bolt> bolts=new ArrayList<>();final List<Spark> sparks=new ArrayList<>();
        Flash(Vec3 pos,Vec3 dir,long born,Random rnd){
            this.pos=pos;this.dir=dir;this.born=born;
            for(int i=0;i<BOLTS;i++){
                int seg=SEG_MIN+rnd.nextInt(SEG_MAX-SEG_MIN+1);
                double[] y=new double[seg+1],p=new double[seg+1];
                double yaw=rnd.nextDouble()*Math.PI*2,pitch=(rnd.nextDouble()-0.5)*1.7;
                for(int s=0;s<=seg;s++){
                    y[s]=yaw;p[s]=pitch;
                    yaw+=(rnd.nextDouble()-0.5)*1.15;     // 折线抖得狠一点，像裂开的黑电
                    pitch+=(rnd.nextDouble()-0.5)*1.15;
                }
                bolts.add(new Bolt(y,p,9.0+rnd.nextDouble()*5.0,0.34+rnd.nextDouble()*0.21));
            }
            for(int i=0;i<SPARKS;i++)
                sparks.add(new Spark(rnd.nextDouble()*Math.PI*2,(rnd.nextDouble()-0.5)*1.5,1.2+rnd.nextDouble()*3.2,0.16+rnd.nextDouble()*0.30,rnd.nextDouble()*6.28));
        }
    }
    private static final List<Flash> FLASHES=new ArrayList<>();
    private static final Random RND=new Random();
    /** —— 用户要求"要很多"：闪电 26 道、火花 72 颗 —— */
    private static final int BOLTS=26,SPARKS=72,CORE_SPARKS=14,SEG_MIN=10,SEG_MAX=14,LIFE=13,WIND_LIFE=10;
    private static final double WIND_RADIUS=4.2;
    private static final int RED=0xFFE02418,BLACK=0xFF060606,WIND=0xFFE6ECF2;
    private static final int BLOOM_RED=0xFF8A1208;
    private static final String GLOW="textures/effect/glow.png";
    private static final Vec3 FALLBACK=new Vec3(0,0,1);

    public static void play(Vec3 pos,Vec3 dir){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        Vec3 d=dir==null||dir.lengthSqr()<1e-6?Vec3.ZERO:dir.normalize();
        FLASHES.add(new Flash(pos,d,mc.level.getGameTime(),RND));
        if(FLASHES.size()>12)FLASHES.remove(0);
        try{
            PhotonFx.spark(pos,0.6,0.45,0xFF2A10,BLOOM_RED,GLOW,true,1,9,26,0.55);
            if(d.lengthSqr()>1e-6)PhotonFx.ring(pos.add(d.scale(1.6)),2.4,0.8,0xFFD8E6F2,BLOOM_RED,26,2,9);
        }catch(Throwable ignored){}
        Client.shake(13);
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
            // ---- 黑色闪电：从圆心向四面八方劈开 ----
            for(Bolt bolt:f.bolts){
                Vec3 from=f.pos;int seg=bolt.yaw().length-1;
                for(int s=0;s<seg;s++){
                    double yaw=bolt.yaw()[s]+Math.sin((age*1.6+s)*1.7)*0.16;
                    double pitch=bolt.pitch()[s]+Math.cos((age*1.4+s)*1.3)*0.16;
                    double step=bolt.len()/seg;
                    Vec3 to=from.add(Math.cos(yaw)*Math.cos(pitch)*step,Math.sin(pitch)*step,Math.sin(yaw)*Math.cos(pitch)*step);
                    double taper=1-s/(double)seg;
                    double wave=Math.sin((age*1.9+s)*2.1+yaw);
                    if(wave<-0.35&&age>1.5){from=to;continue;}                          // 炸断
                    int a=(int)((0.35+0.65*Math.abs(wave))*life*255);
                    double w=bolt.width()*taper;
                    ribbon(b,ps,from,to,w*2.3+0.06,(int)(a*0.42)<<24|(RED&0xFFFFFF));   // 红辉光（光污染）
                    ribbon(b,ps,from,to,w*1.35+0.03,(int)(a*0.72)<<24|(RED&0xFFFFFF));  // 红层
                    ribbon(b,ps,from,to,w,(a<<24)|(BLACK&0xFFFFFF));                     // 近黑芯
                    from=to;
                }
            }
            // ---- 黑色火花：向外飞散 ----
            for(Spark s:f.sparks)sparkAt(b,ps,f.pos.add(dirOf(s).scale(s.speed()*age)),s.size()*(1+age*0.03),life,age,s.phase());
            // ---- 圆心：贴着命中的一小撮火花 ----
            for(int i=0;i<CORE_SPARKS;i++){
                double ang=i*Math.PI*2/CORE_SPARKS+age*0.2;
                Vec3 p=f.pos.add(Math.cos(ang)*(0.5+age*0.12),0.25*Math.sin(ang*2+age),Math.sin(ang)*(0.5+age*0.12));
                sparkAt(b,ps,p,0.22,life,age,i*1.1);
            }
            // ---- 拳风：四层风环 ----
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
    private static Vec3 dirOf(Spark s){return new Vec3(Math.cos(s.yaw())*Math.cos(s.pitch()),Math.sin(s.pitch()),Math.sin(s.yaw())*Math.cos(s.pitch()));}
    /** 一颗火花：红色辉光 + 近黑方片，逐颗闪烁。 */
    private static void sparkAt(BufferBuilder b,PoseStack ps,Vec3 p,double size,double life,double age,double phase){
        double wave=Math.sin(age*2.1+phase);
        if(wave<-0.30)return;
        int a=(int)((0.30+0.70*Math.abs(wave))*life*255);
        quad(b,ps,p,size*2.2,(int)(a*0.55)<<24|(RED&0xFFFFFF));
        quad(b,ps,p,size,(a<<24)|(BLACK&0xFFFFFF));
    }
    private static void ribbon(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 z,double half,int color){
        var cam=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 mid=a.lerp(z,.5),across=z.subtract(a).cross(cam.subtract(mid));
        if(across.lengthSqr()<1e-8)across=FALLBACK;
        Vec3 side=across.normalize().scale(half);
        Client.quad(b,ps,a.add(side),z.add(side),z.subtract(side),a.subtract(side),color);
    }
    private static void quad(BufferBuilder b,PoseStack ps,Vec3 c,double r,int color){
        var cam=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 n=cam.subtract(c);n=n.lengthSqr()<1e-6?FALLBACK:n.normalize();
        Vec3 up=new Vec3(0,1,0).subtract(n.scale(new Vec3(0,1,0).dot(n)));
        if(up.lengthSqr()<1e-4)up=new Vec3(1,0,0).subtract(n.scale(n.x));
        up=up.normalize();Vec3 right=n.cross(up).normalize();
        Vec3 u=up.scale(r).add(right.scale(r)),w=up.scale(r).subtract(right.scale(r));
        Client.quad(b,ps,c.add(u),c.add(w),c.subtract(u),c.subtract(w),color);
    }
    /** 垂直于 {@code dir} 的圆环（16 段面片拼成）。 */
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
