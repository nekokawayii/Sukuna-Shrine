package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** 黑闪表现层（客户端，**纯顶点通路自绘**，不用 Photon、不用任何 .fx）。
 *
 *  <p>按用户设定：**黑红闪电 + 拳风**，闪电**从敌人身上迸发**。</p>
 *  <ul>
 *    <li><b>黑红闪电</b>：12 道从敌人胸口迸发的折线闪电，每道 5~7 段、段间随机折角，外宽内尖；
 *        每段先画一层**红**（更宽、作外发光），再叠一层**黑**（更窄、作芯），所以整道是黑芯红边的黑红闪电；
 *        方向以"背离拳击方向"为主 + 随机散布，逐刻抖动折角、亮度闪烁，约 9 刻淡出。</li>
 *    <li><b>拳风</b>：沿出拳方向铺一圈向外扩张的**半透明风环** + 一层扇形风幕，
 *        白灰色、逐刻变大变淡，约 7 刻；没有方向（direction 为零）时不出拳风。</li>
 *    <li><b>黑色余闪</b>：命中点一下极短的暗闪（3 刻），把"黑"字点出来。</li>
 *  </ul>
 *
 *  <p>全部是实心面片（POSITION_COLOR + 普通混合），没有贴图、没有发射器：既不会残留，也不吃显存/粒子内存。</p> */
public final class BlackFlashRender {
    private record Bolt(double[] yaw,double[] pitch,int seg){}
    private static final class Flash {
        final Vec3 pos,dir;final long born;final List<Bolt> bolts=new ArrayList<>();
        Flash(Vec3 pos,Vec3 dir,long born,Random rnd){
            this.pos=pos;this.dir=dir;this.born=born;
            for(int i=0;i<BOLTS;i++){
                double[] y=new double[SEG+1],p=new double[SEG+1];
                // **竖过来**：一半朝上、一半朝下（俯仰角约 63°~89°），水平方向只做锯齿抖动 → 整体是竖着的雷电柱
                double up=i%2==0?1:-1;
                double yaw=rnd.nextDouble()*Math.PI*2,pitch=up*(1.10+rnd.nextDouble()*0.45);
                for(int s=0;s<=SEG;s++){
                    y[s]=yaw;p[s]=pitch;
                    yaw+=(rnd.nextDouble()-0.5)*0.85;      // 水平锯齿
                    pitch=up*(1.10+Math.abs(pitch)/22.0)+(rnd.nextDouble()-0.5)*0.22;   // 竖向偶尔回折
                }
                bolts.add(new Bolt(y,p,SEG));
            }
        }
    }
    private static final List<Flash> FLASHES=new ArrayList<>();
    private static final Random RND=new Random();
    /** 参数都集中在这里，方便按手感调。 */
    private static final int BOLTS=22,SEG=7,LIFE=13,WIND_LIFE=10,DARK_LIFE=4;
    private static final double BOLT_LEN=6.6,BOLT_WIDTH=0.24,WIND_RADIUS=4.2;
    private static final int RED=0xFFE02418,BLACK=0xFF050505,WIND=0xFFE6ECF2,DARK=0xFF000000;
    /** Photon 加法发光的 bloom 色（真 bloom 靠这条，用量小、寿命短，不会堆积）。 */
    private static final int BLOOM_RED=0xFF8A1208;
    private static final String GLOW="textures/effect/glow.png";

    /** 命中点触发：{@code pos} = 敌人身上（胸口附近），{@code dir} = 出拳方向（可为 {@link Vec3#ZERO}）。 */
    public static void play(Vec3 pos,Vec3 dir){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        Vec3 d=dir==null||dir.lengthSqr()<1e-6?Vec3.ZERO:dir.normalize();
        FLASHES.add(new Flash(pos,d,mc.level.getGameTime(),RND));
        if(FLASHES.size()>16)FLASHES.remove(0);   // 连打时只留最近的，绝不无限堆积
        // 一层 Photon 加法发光：真 bloom（顶点通路的普通混合做不出光晕）；寿命都很短，不会长期占用
        // 光污染：一处加法发光给整场戏"打光" + 一拳的风压光环（都短命，不堆积）
        try{
            PhotonFx.spark(pos,0.5,0.40,0xFF2A10,BLOOM_RED,GLOW,true,1,8,22,0.5);
            if(d.lengthSqr()>1e-6)PhotonFx.ring(pos.add(d.scale(1.6)),2.2,0.75,0xFFD8E6F2,BLOOM_RED,26,2,9);
        }catch(Throwable ignored){}
        Client.shake(12);
    }
    public static void tick(){}
    public static void clear(){FLASHES.clear();}

    /** 世界渲染（在 {@link SoulClient#draw} 的同一批里画；坐标已由上层减去相机）。 */
    public static void draw(BufferBuilder b,PoseStack ps,Vec3 cam,float partial){
        if(FLASHES.isEmpty())return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        for(Flash f:FLASHES){
            double age=mc.level.getGameTime()-f.born+partial;
            if(age<0||age>LIFE)continue;
            double life=Math.max(0,1-age/LIFE);
            // ---- 黑红闪电：从敌人身上迸发 ----
            for(Bolt bolt:f.bolts){
                Vec3 from=f.pos;
                for(int s=0;s<bolt.seg();s++){
                    double yaw=bolt.yaw()[s]+Math.sin((age+s)*1.7)*0.10;      // 逐刻抖动
                    double pitch=bolt.pitch()[s]+Math.cos((age+s)*1.3)*0.10;
                    Vec3 step=new Vec3(Math.cos(yaw)*Math.cos(pitch),Math.sin(pitch),Math.sin(yaw)*Math.cos(pitch)).scale(BOLT_LEN/bolt.seg());
                    Vec3 to=from.add(step);
                    double taper=(1-s/(double)bolt.seg());
                    // 张力：整段闪电会**突然熄灭再亮起**（每段独立判定），比单纯改透明度更有电击感
                    double wave=Math.sin((age*1.9+s)*2.3);
                    if(wave<-0.35&&age>1.5){from=to;continue;}
                    int flick=(int)(190+65*Math.abs(wave));
                    int redA=(int)((175*flick/255.0)*life)<<24,blkA=(int)((250*flick/255.0)*life)<<24;
                    ribbon(b,ps,from,to,BOLT_WIDTH*4.6*taper+0.05,(int)(redA*0.5)<<24|((RED>>1)&0x7F7F7F),cam);  // 最外：红色辉光（光污染层）
                    ribbon(b,ps,from,to,BOLT_WIDTH*2.6*taper+0.02,redA|(RED&0xFFFFFF),cam);                       // 外：红
                    ribbon(b,ps,from,to,BOLT_WIDTH*1.4*taper+0.01,blkA|(BLACK&0xFFFFFF),cam);                     // 芯：黑
                    from=to;
                }
            }
            // ---- 拳风：沿出拳方向扩张的风环（**更明显**：四层、更粗更亮更大） ----
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

    // ------------------------------------------------------------ 基础画法（相机朝向面片）
    private static void ribbon(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 z,double half,int color,Vec3 cam){
        Vec3 mid=a.lerp(z,.5),across=z.subtract(a).cross(cam.subtract(mid));
        if(across.lengthSqr()<1e-8)across=new Vec3(0,1,0);
        Vec3 side=across.normalize().scale(half);
        Client.quad(b,ps,a.add(side),z.add(side),z.subtract(side),a.subtract(side),color);
    }
    private static void quad(BufferBuilder b,PoseStack ps,Vec3 c,double r,Vec3 cam,int color){
        Vec3 n=cam.subtract(c);n=n.lengthSqr()<1e-6?new Vec3(0,0,1):n.normalize();
        Vec3 up=new Vec3(0,1,0).subtract(n.scale(new Vec3(0,1,0).dot(n)));
        if(up.lengthSqr()<1e-4)up=new Vec3(1,0,0).subtract(n.scale(n.x));
        up=up.normalize();Vec3 right=n.cross(up).normalize();
        Vec3 u=up.scale(r).add(right.scale(r)),w=up.scale(r).subtract(right.scale(r));
        Client.quad(b,ps,c.add(u),c.add(w),c.subtract(u),c.subtract(w),color);
    }
    /** 垂直于 {@code dir} 的圆环（用 16 段面片拼，避免依赖 Photon）。 */
    private static void windRing(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 dir,double r,double thick,int color,Vec3 cam){
        Vec3 n=dir.normalize();
        Vec3 ref=Math.abs(n.y)>0.9?new Vec3(1,0,0):new Vec3(0,1,0);
        Vec3 e1=n.cross(ref).normalize(),e2=n.cross(e1).normalize();
        int seg=16;
        for(int i=0;i<seg;i++){
            double a0=i*Math.PI*2/seg,a1=(i+1)*Math.PI*2/seg;
            Vec3 p0=c.add(e1.scale(Math.cos(a0)*r)).add(e2.scale(Math.sin(a0)*r));
            Vec3 p1=c.add(e1.scale(Math.cos(a1)*r)).add(e2.scale(Math.sin(a1)*r));
            Vec3 mid=p0.lerp(p1,.5);
            ribbon(b,ps,p0,p1,thick,color,cam.add(mid.scale(0)));   // 用 ribbon 拼成环
        }
    }
    private BlackFlashRender(){}
}
