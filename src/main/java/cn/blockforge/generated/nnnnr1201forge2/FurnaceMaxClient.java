package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** 「灶 · 最大功率 · 开」的客户端表现层：**全部由 Photon 粒子发射器拼出来**
 *  （统一走 {@link PhotonBridge}，和既有的阳华/剑气特效同一套通路）。
 *
 *  <ul>
 *    <li>**蓄力**：周身火环 + 一把随蓄力一起长大的**火焰弓**（两段弓臂 + 弓弦 + 箭），
 *        满蓄力时弓高 = 玩家身高的两倍（3.6 格）；</li>
 *    <li>**弓矢**：飞行时拖一条火焰尾迹；</li>
 *    <li>**爆炸**：中心爆燃 + 25/50/75/100 格四道扩散环 + 冲击扬尘；</li>
 *    <li>**火柱**：冲天柱体高度 80 格，40 秒内越烧越矮、越细、越稀（缓慢熄灭）。</li>
 *  </ul>
 *
 *  <p>所有位置都由服务端给定（本地玩家用实时位置补正），不依赖实体锚点。</p> */
public final class FurnaceMaxClient {
    private static final int FLAME=0xFFFF5B0B,FLAME_HOT=0xFFFFD9A0,EMBER=0xFFFF9A3C,DUST=0x80C9B6A6;
    /** 火柱生成高度（格）与同时存在的 Photon 发射器上限。 */
    private static final double PILLAR_HEIGHT=80,BOW_HEIGHT=3.6;
    private static final int MAX_PUFFS=260;
    private static final class Charge{int owner;Vec3 pos=Vec3.ZERO;Vec3 dir=new Vec3(0,0,1);float progress;long end;}
    private static final class Pillar{int owner;Vec3 center=Vec3.ZERO;long end;}
    private static final class Puff{final PhotonBridge.Handle handle;final long end;Puff(PhotonBridge.Handle h,long e){handle=h;end=e;}}
    private static final Map<Integer,Charge> charges=new LinkedHashMap<>();
    private static final List<Pillar> pillars=new ArrayList<>();
    private static final List<Puff> puffs=new ArrayList<>();
    private static final Random random=new Random();
    private static long tick;

    public static void receive(Network.Effect e){
        if(Minecraft.getInstance().level==null)return;
        if(e.kind()==FurnaceMax.FX_CHARGE){
            if(e.life()<=0){charges.remove(e.owner());return;}   // life=0 = 服务端要求收掉蓄力画面
            Charge c=charges.computeIfAbsent(e.owner(),k->new Charge());
            c.owner=e.owner();c.pos=e.position();
            if(e.direction().lengthSqr()>1e-6)c.dir=e.direction();
            c.progress=Math.max(0f,Math.min(1f,e.size()));c.end=tick+e.life()+2;
            return;
        }
        if(e.kind()==FurnaceMax.FX_SHOT){    // 弓矢尾迹
            puff(e.position(),FLAME_HOT,1.2,5,6);
            puff(e.position().add(random.nextGaussian()*0.5,random.nextGaussian()*0.5,random.nextGaussian()*0.5),FLAME,1.0,4,9);
            return;
        }
        if(e.kind()==FurnaceMax.FX_BLAST){blast(e.position());return;}
        if(e.kind()==FurnaceMax.FX_PILLAR){
            if(e.life()<=0){pillars.removeIf(q->q.owner==e.owner()&&q.center.distanceToSqr(e.position())<9);return;}
            Pillar found=null;
            for(Pillar q:pillars)if(q.owner==e.owner()&&q.center.distanceToSqr(e.position())<9){found=q;break;}
            if(found==null){found=new Pillar();found.owner=e.owner();found.center=e.position();pillars.add(found);}
            found.end=tick+e.life();
        }
    }

    public static void tick(){
        if(Minecraft.getInstance().level==null){clear();return;}
        tick++;
        var player=Minecraft.getInstance().player;
        for(Iterator<Charge> it=charges.values().iterator();it.hasNext();){
            Charge c=it.next();
            if(tick>c.end){it.remove();continue;}   // 服务端不再同步 = 蓄力已经结束
            // 本地玩家：直接用实时位置与朝向，20 秒的长蓄力不会被 4 刻一次的同步拖出迟滞
            Vec3 pos=c.pos,dir=c.dir;
            if(player!=null&&player.getId()==c.owner){pos=player.position();dir=player.getLookAngle();}
            drawCharge(pos,dir,c.progress);
        }
        for(Pillar q:pillars)drawPillar(q);
        puffs.removeIf(x->{if(tick>=x.end){PhotonBridge.stop(x.handle);return true;}return false;});
    }

    /** 蓄力：周身火焰 + 汇聚成的火焰弓（弓高随蓄力从 15% 长到 100%）。 */
    private static void drawCharge(Vec3 pos,Vec3 look,float progress){
        if(tick%2!=0)return;
        Vec3 fwd=look.lengthSqr()<1e-6?new Vec3(0,0,1):look.normalize();
        Vec3 side=fwd.cross(new Vec3(0,1,0));
        if(side.lengthSqr()<1e-4)side=new Vec3(1,0,0);
        side=side.normalize();
        Vec3 vertical=side.cross(fwd).normalize();
        double h=BOW_HEIGHT*Math.max(0.15,progress);
        Vec3 center=pos.add(0,1.1,0).add(fwd.scale(0.9));
        // 周身火环：绕着身体一圈、往外飘
        for(int i=0;i<3;i++){
            double a=random.nextDouble()*Math.PI*2,r=0.7+random.nextDouble()*0.9,y=0.2+random.nextDouble()*1.5;
            puff(pos.add(Math.cos(a)*r,y,Math.sin(a)*r),random.nextInt(3)==0?FLAME_HOT:EMBER,0.75,3,7);
        }
        // 两段弓臂：中间向外鼓 0.55 格
        for(int i=0;i<5;i++){
            double t=0.2+0.8*i/4.0;
            for(int s=-1;s<=1;s+=2){
                double ty=s*t;
                Vec3 at=center.add(vertical.scale(h*0.5*ty)).add(fwd.scale(0.55*(1-ty*ty)));
                puff(at,FLAME,0.5+0.7*progress,2,6);
            }
        }
        // 弓弦（在弓臂内侧）与搭在弦上的箭
        for(int i=0;i<3;i++){
            double t=-0.7+0.7*i;
            puff(center.add(vertical.scale(h*0.5*t)).subtract(fwd.scale(0.15)),FLAME_HOT,0.4+0.4*progress,2,5);
        }
        for(int i=0;i<3;i++)puff(center.add(fwd.scale(0.6+i*0.7)),FLAME_HOT,0.35+0.5*progress,2,5);
    }

    /** 火柱：40 秒里越烧越矮、越细、越稀（缓慢熄灭）。 */
    private static void drawPillar(Pillar q){
        if(tick%2!=0)return;
        double remaining=Math.max(0,q.end-tick);
        double k=Math.min(1,remaining/FurnaceMax.PILLAR_TICKS);   // 1 → 0
        double height=PILLAR_HEIGHT*(0.35+0.65*k),radius=1.5+3.5*k,size=1.8+2.6*k;
        int n=k>0.5?3:2;
        for(int i=0;i<n;i++){
            double y=Math.pow(random.nextDouble(),0.7)*height;
            double a=random.nextDouble()*Math.PI*2,r=Math.sqrt(random.nextDouble())*radius*Math.max(0.2,1-0.5*y/height);
            puff(q.center.add(Math.cos(a)*r,y,Math.sin(a)*r),random.nextInt(4)==0?FLAME_HOT:FLAME,
                size*Math.max(0.4,1-0.4*y/height),4+(int)(4*k),10+(int)(14*k));
        }
        if(tick%10==0)puff(q.center.add(0,1,0),FLAME_HOT,2+5*k,10,18);   // 柱底的核心
    }

    /** 半径 100 格的火焰爆炸：中心爆燃 + 四道扩散环 + 冲击扬尘。 */
    private static void blast(Vec3 center){
        puff(center,FLAME_HOT,10,80,26);
        puff(center,FLAME,16,60,34);
        for(int ring=1;ring<=4;ring++){
            double r=FurnaceMax.BLAST_RADIUS*ring/4.0;
            for(int i=0;i<8;i++){
                double a=i*Math.PI/4+random.nextDouble()*0.25;
                Vec3 at=center.add(Math.cos(a)*r,(random.nextDouble()-0.35)*4,Math.sin(a)*r);
                puff(at,ring<=2?FLAME:EMBER,3.0+ring*1.6,6,20+ring*4);
            }
        }
        for(int i=0;i<10;i++){
            double a=random.nextDouble()*Math.PI*2,r=2+random.nextDouble()*FurnaceMax.BLAST_RADIUS;
            puff(center.add(Math.cos(a)*r,random.nextDouble()*2,Math.sin(a)*r),DUST,2.5,5,16);
        }
    }

    private static void puff(Vec3 at,int color,double size,int count,int life){
        if(puffs.size()>=MAX_PUFFS)return;
        PhotonBridge.Handle handle=PhotonBridge.spawn(at,color,size,count,life);
        if(handle!=null)puffs.add(new Puff(handle,tick+life+2));
    }

    public static void clear(){
        for(Puff p:puffs)PhotonBridge.stop(p.handle);
        puffs.clear();charges.clear();pillars.clear();tick=0;
    }
    private FurnaceMaxClient(){}
}
