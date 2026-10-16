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
                double yaw=rnd.nextDouble()*Math.PI*2,pitch=(rnd.nextDouble()-0.5)*1.2;
                for(int s=0;s<=SEG;s++){
                    y[s]=yaw;p[s]=pitch;
                    yaw+=(rnd.nextDouble()-0.5)*0.9;pitch+=(rnd.nextDouble()-0.5)*0.9;
                }
                bolts.add(new Bolt(y,p,SEG));
            }
        }
    }
    private static final List<Flash> FLASHES=new ArrayList<>();
    private static final Random RND=new Random();
    /** 参数都集中在这里，方便按手感调。 */
    private static final int BOLTS=12,SEG=6,LIFE=9,WIND_LIFE=7,DARK_LIFE=3;
    private static final double BOLT_LEN=4.2,BOLT_WIDTH=0.16,WIND_RADIUS=2.6;
    private static final int RED=0xFFB01810,BLACK=0xFF050505,WIND=0xFFD8DEE6,DARK=0xFF000000;

    /** 命中点触发：{@code pos} = 敌人身上（胸口附近），{@code dir} = 出拳方向（可为 {@link Vec3#ZERO}）。 */
    public static void play(Vec3 pos,Vec3 dir){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        Vec3 d=dir==null||dir.lengthSqr()<1e-6?Vec3.ZERO:dir.normalize();
        FLASHES.add(new Flash(pos,d,mc.level.getGameTime(),RND));
        if(FLASHES.size()>16)FLASHES.remove(0);   // 连打时只留最近的，绝不无限堆积
        Client.shake(6);
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
                    int flick=(int)(155+100*Math.abs(Math.sin((age+s)*2.3)));
                    int redA=(int)((150*flick/255.0)*life)<<24,blkA=(int)((235*flick/255.0)*life)<<24;
                    ribbon(b,ps,from,to,BOLT_WIDTH*2.6*taper+0.02,redA|(RED&0xFFFFFF),cam);   // 外：红
                    ribbon(b,ps,from,to,BOLT_WIDTH*1.3*taper+0.01,blkA|(BLACK&0xFFFFFF),cam); // 芯：黑
                    from=to;
                }
            }
            // ---- 拳风：沿出拳方向扩张的风环 ----
            if(f.dir.lengthSqr()>1e-6&&age<=WIND_LIFE){
                double wl=Math.max(0,1-age/WIND_LIFE),r=WIND_RADIUS*(0.35+0.75*(age/WIND_LIFE));
                int a=(int)(120*wl*wl)<<24;
                windRing(b,ps,f.pos.add(f.dir.scale(0.6+r*0.5)),f.dir,r,0.30,a|(WIND&0xFFFFFF),cam);
                windRing(b,ps,f.pos.add(f.dir.scale(0.4+r)),f.dir,r*0.72,0.16,a|(WIND&0xFFFFFF),cam);
            }
            // ---- 黑色余闪 ----
            if(age<=DARK_LIFE){
                double dl=1-age/DARK_LIFE;int a=(int)(170*dl)<<24;
                quad(b,ps,f.pos,0.55+0.5*(1-dl),cam,a|(DARK&0xFFFFFF));
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
