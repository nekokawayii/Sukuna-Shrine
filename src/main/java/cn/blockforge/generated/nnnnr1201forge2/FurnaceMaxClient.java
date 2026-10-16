package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/** 「灶 · 最大功率 · 开」的客户端表现层：**全部由 Photon 粒子发射器拼出来**
 *  （统一走 {@link PhotonBridge}，与既有的阳华/剑气/虚空特效同一套通路）。
 *
 *  <ul>
 *    <li>**蓄力**：周身火焰不断**向身前汇聚成一把弓**（弓高最终 = 玩家身高的两倍），
 *        弓弦随蓄力**往后拉开**（拉弓），箭搭在弦上；</li>
 *    <li>**镀橙**：蓄力时周身 **60 格**内的方块被镀上橙色——颜色由原色**逐渐变成纯橙**
 *        （3 秒渐变 + 雾气/屏幕橙染），**术式结束后缓慢恢复**，最后**收拢成一个红球消失**；</li>
 *    <li>**弓矢**：出膛后拖一条金色火焰尾迹；</li>
 *    <li>**爆炸**：中心爆燃 + 25/50/75/100 格四道扩散环 + 向外放射的金色光弧（参考图的拉丝感）；</li>
 *    <li>**火柱**：冲天柱体 80 格，40 秒里越烧越矮、越细、越稀。</li>
 *  </ul>
 *
 *  <p>所有位置由服务端给定（本地玩家用实时位置补正）；发射器总量有硬上限，过期立刻 {@link PhotonBridge#stop}。</p> */
public final class FurnaceMaxClient {
    private static final int FLAME=0xFFFF5B0B,FLAME_HOT=0xFFFFE0A8,EMBER=0xFFFF9A3C,GOLD=0xFFFFC24A,
        DEEP=0xFFE03A08,COAT=0xE0FF7A1E,COAT_HOT=0xFFFFB060,RED=0xFFFF2418,DUST=0x80C9B6A6;
    /** 火柱生成高度；弓高（满蓄力）= 玩家身高 1.8 的两倍。 */
    private static final double PILLAR_HEIGHT=80,BOW_HEIGHT=3.6;
    /** 同时存在的 Photon 发射器上限，以及"镀橙"渐变/收拢/红球的时间。 */
    private static final int MAX_PUFFS=420,COAT_RAMP=60,COLLAPSE_TICKS=70,BALL_TICKS=34;

    private static final class Charge{int owner;Vec3 pos=Vec3.ZERO;Vec3 dir=new Vec3(0,0,1);float progress;long end;}
    /** 方块镀橙用的场：蓄力期间逐渐铺满（COAT_RAMP），术式结束后收拢（COLLAPSE_TICKS）再变红球。 */
    private static final class Field{
        int owner;Vec3 center=Vec3.ZERO;long start;long holdUntil;long collapseFrom;long nextCoat;boolean collapsed;
        double intensity;
    }
    private static final class Pillar{int owner;Vec3 center=Vec3.ZERO;long end;}
    private static final class Ball{Vec3 center;long from;}
    private static final class Puff{final PhotonBridge.Handle handle;final long end;Puff(PhotonBridge.Handle h,long e){handle=h;end=e;}}
    private static final Map<Integer,Charge> charges=new LinkedHashMap<>();
    private static final Map<Integer,Field> fields=new LinkedHashMap<>();
    private static final List<Pillar> pillars=new ArrayList<>();
    private static final List<Ball> balls=new ArrayList<>();
    private static final List<Puff> puffs=new ArrayList<>();
    private static final Random random=new Random();
    private static long tick;

    // ------------------------------------------------------------------ 收包
    public static void receive(Network.Effect e){
        if(Minecraft.getInstance().level==null)return;
        if(e.kind()==FurnaceMax.FX_CHARGE){
            if(e.life()>0){
                Charge c=charges.computeIfAbsent(e.owner(),k->new Charge());
                c.owner=e.owner();c.pos=e.position();
                if(e.direction().lengthSqr()>1e-6)c.dir=e.direction();
                c.progress=Math.max(0f,Math.min(1f,e.size()));c.end=tick+e.life()+2;
                Field f=fields.computeIfAbsent(e.owner(),k->new Field());
                if(f.start==0){f.start=tick;f.nextCoat=tick;}
                f.owner=e.owner();f.center=e.position();f.holdUntil=tick+e.life();f.collapseFrom=0;f.collapsed=false;
            }else{
                charges.remove(e.owner());
                startCollapse(e.owner());
            }
            return;
        }
        if(e.kind()==FurnaceMax.FX_SHOT){    // 弓矢：金色尾迹 + 火星
            puff(e.position(),FLAME_HOT,1.4,6,6);
            puff(e.position(),GOLD,1.0,4,8);
            puff(e.position().add(random.nextGaussian()*0.5,random.nextGaussian()*0.5,random.nextGaussian()*0.5),FLAME,1.1,4,9);
            streak(e.position(),GOLD,4.5,4,random.nextInt(360));
            return;
        }
        if(e.kind()==FurnaceMax.FX_BLAST){blast(e.position());startCollapse(e.owner());return;}
        if(e.kind()==FurnaceMax.FX_PILLAR){
            if(e.life()<=0){pillars.removeIf(q->q.owner==e.owner()&&q.center.distanceToSqr(e.position())<9);return;}
            Pillar found=null;
            for(Pillar q:pillars)if(q.owner==e.owner()&&q.center.distanceToSqr(e.position())<9){found=q;break;}
            if(found==null){found=new Pillar();found.owner=e.owner();found.center=e.position();pillars.add(found);}
            found.end=tick+e.life();
        }
    }
    /** 术式结束：不是"啪一下消失"，而是开始收拢（缓慢恢复 + 缩成红球）。 */
    private static void startCollapse(int owner){
        Field f=fields.get(owner);
        if(f!=null&&f.collapseFrom==0)f.collapseFrom=tick;
    }

    // ------------------------------------------------------------------ 每刻
    public static void tick(){
        if(Minecraft.getInstance().level==null){clear();return;}
        tick++;
        var player=Minecraft.getInstance().player;
        for(Iterator<Charge> it=charges.values().iterator();it.hasNext();){
            Charge c=it.next();
            if(tick>c.end){it.remove();continue;}
            Vec3 pos=c.pos,dir=c.dir;
            if(player!=null&&player.getId()==c.owner){pos=player.position();dir=player.getLookAngle();}
            drawBow(pos,dir,c.progress);
        }
        for(Iterator<Field> it=fields.values().iterator();it.hasNext();){
            Field f=it.next();
            if(player!=null&&player.getId()==f.owner)f.center=player.position();
            if(f.collapseFrom==0){
                f.intensity=Math.min(1,(tick-f.start)/(double)COAT_RAMP);
                // 兜底：万一没收到收尾包（丢包/掉线），也自己开始收拢，不留一片永久橙色
                if(tick>f.holdUntil+12)f.collapseFrom=tick;
            }else{
                f.intensity=Math.max(0,1-(tick-f.collapseFrom)/(double)COLLAPSE_TICKS);
                if(f.intensity<=0){balls.add(newBall(f.center));it.remove();continue;}
            }
            coat(f);
            sparks(f);
        }
        for(Iterator<Ball> it=balls.iterator();it.hasNext();){
            Ball b=it.next();
            double k=(tick-b.from)/(double)BALL_TICKS;
            if(k>=1){it.remove();continue;}
            double size=2.8*(1-k)+0.25;
            puff(b.center.add(0,1,0),RED,size,8,5);
            puff(b.center.add((random.nextDouble()-0.5),0.8+random.nextDouble(),(random.nextDouble()-0.5)),DEEP,size*0.7,5,4);
        }
        for(Pillar q:pillars)drawPillar(q);
        puffs.removeIf(x->{if(tick>=x.end){PhotonBridge.stop(x.handle);return true;}return false;});
    }
    private static Ball newBall(Vec3 center){Ball b=new Ball();b.center=center;b.from=tick;return b;}

    /** 当前"镀橙"强度（0~1）：给雾色与屏幕橙染用。只有**站在场里**（收拢后范围同步变小）才染。 */
    public static double intensity(){
        var player=Minecraft.getInstance().player;
        if(player==null)return 0;
        double k=0;
        for(Field f:fields.values()){
            double reach=FurnaceMax.FIELD_RADIUS*Math.max(0.35,f.intensity);
            if(player.position().distanceToSqr(f.center)<=reach*reach)k=Math.max(k,f.intensity);
        }
        for(Ball b:balls)if(player.position().distanceToSqr(b.center)<=256)k=Math.max(k,0.35);
        return k;
    }
    /** 世界雾色向橙金靠拢：方块"由原来的颜色慢慢变成纯橙色"主要靠它 + {@link #coat}。 */
    public static void fog(net.minecraftforge.client.event.ViewportEvent.ComputeFogColor e){
        double k=intensity();
        if(k<=0.01)return;
        double m=0.85*k;
        e.setRed((float)(e.getRed()*(1-m)+1.00*m));
        e.setGreen((float)(e.getGreen()*(1-m)+0.45*m));
        e.setBlue((float)(e.getBlue()*(1-m)+0.10*m));
    }
    /** 屏幕橙染（很轻，主要交给雾色与粒子，避免"刺眼/花"）。 */
    public static void overlay(GuiGraphics g){
        double k=intensity();
        if(k<=0.02)return;
        var mc=Minecraft.getInstance();
        int alpha=(int)Math.min(64,58*k);
        g.fill(0,0,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),(alpha<<24)|0xFF6A14);
    }

    // ------------------------------------------------------------------ 蓄力：汇聚成弓 + 拉弓
    /** 火焰不断从四周汇到身前，凝成一把弓；弓弦随蓄力往后拉开、箭搭在弦上。 */
    private static void drawBow(Vec3 pos,Vec3 look,float progress){
        if(tick%2!=0)return;
        Vec3 fwd=look.lengthSqr()<1e-6?new Vec3(0,0,1):look.normalize();
        Vec3 side=fwd.cross(new Vec3(0,1,0));
        if(side.lengthSqr()<1e-4)side=new Vec3(1,0,0);
        side=side.normalize();
        Vec3 vertical=side.cross(fwd).normalize();
        double h=BOW_HEIGHT*Math.max(0.15,progress);
        Vec3 center=pos.add(0,1.1,0).add(fwd.scale(0.9));
        // 汇聚：火焰从四周往身前收（越蓄越快）
        for(int i=0;i<4;i++){
            double a=random.nextDouble()*Math.PI*2,r=2.0+random.nextDouble()*3.5,y=(random.nextDouble()-0.5)*2.6;
            Vec3 at=pos.add(Math.cos(a)*r,1+y,Math.sin(a)*r);
            Vec3 to=center.subtract(at).scale(0.12);
            puff(at.add(to),random.nextInt(3)==0?flame():FLAME,0.7+0.5*progress,4,7);
        }
        // 弓臂：两段弧，各 3 段（每段 6~10 颗，比上一版更密）
        for(int i=0;i<3;i++){
            double t=0.22+0.78*i/2.0;
            for(int s=-1;s<=1;s+=2){
                double ty=s*t;
                Vec3 at=center.add(vertical.scale(h*0.5*ty)).add(fwd.scale(0.55*(1-ty*ty)));
                puff(at,FLAME,0.85+0.7*progress,7,6);
                if(random.nextInt(3)==0)puff(at,flame(),0.6+0.5*progress,3,9);
            }
        }
        // 弓弦：中点被拉后 draw 格（拉弓）
        double draw=0.95*progress;
        Vec3 nock=center.subtract(fwd.scale(0.15+draw));
        Vec3 top=center.add(vertical.scale(h*0.5)),bottom=center.subtract(vertical.scale(h*0.5));
        for(int i=1;i<=2;i++){
            double t=i/3.0;
            puff(top.lerp(nock,t),FLAME_HOT,0.5+0.4*progress,4,5);
            puff(bottom.lerp(nock,t),FLAME_HOT,0.5+0.4*progress,4,5);
        }
        // 搭在弦上的箭
        puff(nock.add(fwd.scale(0.5)),FLAME_HOT,0.6+0.6*progress,7,6);
        puff(nock.add(fwd.scale(1.6)),GOLD,0.5+0.5*progress,4,6);
        // 金色拉丝：参考图里那种细亮弧线
        if(tick%3==0){
            for(int i=0;i<2;i++){
                double a=random.nextDouble()*Math.PI*2,r=3+random.nextDouble()*6;
                Vec3 at=pos.add(Math.cos(a)*r,0.5+random.nextDouble()*3,Math.sin(a)*r);
                streak(at,random.nextBoolean()?GOLD:FLAME_HOT,4+random.nextDouble()*3,7+random.nextInt(6),(float)Math.toDegrees(a)+90);
            }
        }
    }
    private static int flame(){return random.nextInt(3)==0?FLAME_HOT:GOLD;}

    // ------------------------------------------------------------------ 镀橙场
    /** 每 8 刻换一批"镀色云"：旧云自然淡出、新云更靠内 —— 于是收拢时看起来就是整片橙色往身体缩。 */
    private static void coat(Field f){
        if(tick<f.nextCoat)return;
        f.nextCoat=tick+8;
        double k=f.intensity;
        double spread=FurnaceMax.FIELD_RADIUS*2*Math.max(0.25,k);
        for(int i=0;i<3;i++){
            Vec3 at=f.center.add((random.nextDouble()-0.5)*spread*0.7,(random.nextDouble()-0.35)*spread*0.5,(random.nextDouble()-0.5)*spread*0.7);
            cloud(at,random.nextInt(4)==0?COAT_HOT:COAT,spread*(0.45+0.35*random.nextDouble()),5+7*k,60+(int)(150*k),18+(int)(14*k));
        }
    }
    /** 场里的火星与落灰，让"镀橙"有火气。 */
    private static void sparks(Field f){
        double k=f.intensity;
        if(tick%2!=0)return;
        double r=FurnaceMax.FIELD_RADIUS*Math.max(0.25,k);
        for(int i=0;i<2;i++){
            Vec3 at=f.center.add((random.nextDouble()-0.5)*2*r,(random.nextDouble()-0.1)*r*0.5,(random.nextDouble()-0.5)*2*r);
            puff(at,random.nextInt(3)==0?flame():EMBER,1.2+0.8*k,3,10+random.nextInt(8));
        }
    }

    // ------------------------------------------------------------------ 火柱
    /** 火柱：40 秒里越烧越矮、越细、越稀（缓慢熄灭）。 */
    private static void drawPillar(Pillar q){
        if(tick%2!=0)return;
        double remaining=Math.max(0,q.end-tick);
        double k=Math.min(1,remaining/FurnaceMax.PILLAR_TICKS);
        double height=PILLAR_HEIGHT*(0.35+0.65*k),radius=1.6+3.8*k,size=2.0+3.0*k;
        int n=k>0.5?4:3;
        for(int i=0;i<n;i++){
            double y=Math.pow(random.nextDouble(),0.7)*height;
            double a=random.nextDouble()*Math.PI*2,r=Math.sqrt(random.nextDouble())*radius*Math.max(0.2,1-0.5*y/height);
            puff(q.center.add(Math.cos(a)*r,y,Math.sin(a)*r),random.nextInt(4)==0?flame():FLAME,
                size*Math.max(0.4,1-0.4*y/height),6+(int)(6*k),10+(int)(14*k));
            if(random.nextInt(5)==0)streak(q.center.add(Math.cos(a)*r*0.6,y,Math.sin(a)*r*0.6),GOLD,4,6+random.nextInt(6),random.nextInt(360));
        }
        if(tick%10==0)puff(q.center.add(0,1,0),FLAME_HOT,2.2+5*k,14,18);
    }

    /** 半径 100 格的火焰爆炸：中心爆燃 + 四道扩散环 + 向外放射的金色光弧。 */
    private static void blast(Vec3 center){
        puff(center,FLAME_HOT,11,90,26);
        puff(center,FLAME,17,70,34);
        for(int ring=1;ring<=4;ring++){
            double r=FurnaceMax.BLAST_RADIUS*ring/4.0;
            for(int i=0;i<6;i++){
                double a=i*Math.PI/3+random.nextDouble()*0.3;
                Vec3 at=center.add(Math.cos(a)*r,(random.nextDouble()-0.35)*4,Math.sin(a)*r);
                puff(at,ring<=2?flame():EMBER,3.2+ring*1.8,9,20+ring*4);
            }
        }
        for(int i=0;i<24;i++){   // 放射拉丝：参考图里最抢眼的那层
            double a=random.nextDouble()*Math.PI*2,elev=(random.nextDouble()-0.3)*0.8;
            Vec3 at=center.add(Math.cos(a)*8,elev*8,Math.sin(a)*8);
            streak(at,random.nextInt(3)==0?GOLD:FLAME_HOT,5+random.nextDouble()*4,10+random.nextInt(10),(float)Math.toDegrees(a)+90);
        }
        for(int i=0;i<10;i++){
            double a=random.nextDouble()*Math.PI*2,r=2+random.nextDouble()*FurnaceMax.BLAST_RADIUS;
            puff(center.add(Math.cos(a)*r,random.nextDouble()*2,Math.sin(a)*r),DUST,2.8,6,16);
        }
    }

    // ------------------------------------------------------------------ 工具
    private static void puff(Vec3 at,int color,double size,int count,int life){
        if(puffs.size()>=MAX_PUFFS)return;
        PhotonBridge.Handle handle=PhotonBridge.spawn(at,color,size,count,life);
        if(handle!=null)puffs.add(new Puff(handle,tick+life+2));
    }
    private static void cloud(Vec3 at,int color,double spread,double size,int count,int life){
        if(puffs.size()>=MAX_PUFFS)return;
        PhotonBridge.Handle handle=PhotonBridge.cloud(at,color,spread,size,count,life);
        if(handle!=null)puffs.add(new Puff(handle,tick+life+2));
    }
    private static void streak(Vec3 at,int color,double size,int life,float angle){
        if(puffs.size()>=MAX_PUFFS)return;
        PhotonBridge.Handle handle=PhotonBridge.streak(at,color,size,life,angle);
        if(handle!=null)puffs.add(new Puff(handle,tick+life+2));
    }

    public static void clear(){
        for(Puff p:puffs)PhotonBridge.stop(p.handle);
        puffs.clear();charges.clear();fields.clear();pillars.clear();balls.clear();tick=0;
    }
    private FurnaceMaxClient(){}
}
