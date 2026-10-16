package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** 「灶 · 最大功率 · 开」的客户端表现层：**与苍 / 赫 / 茈 走同一条管线**
 *  （{@link PhotonFx}：球体 / 球壳 / 圆环 / 三维圆柱 / 射线，全部在客户端即时程序化构建），
 *  不再使用 flame / dust / void_rift 这类"贴图粒子"，因此：
 *  <ul>
 *    <li>不会再有原版粒子味的火苗贴图；</li>
 *    <li>不会再有 {@code void_rift.png} 那种紫色裂痕（上一版被当成"虚空捌特效"的就是它）。</li>
 *  </ul>
 *
 *  <p>素材只有三张通用光效图，与苍赫茈完全相同：glow（光斑/雾）、ring（冲击波环）、streak（细光线）。</p>
 *  <ul>
 *    <li>**蓄力**：金色细线（{@link PhotonFx#ray}）从四周收束进身体 + 身体周围的火光球壳 +
 *        **三维圆柱（{@link PhotonFx#beam}）拼出的火焰弓**：两段弓臂各 4 节、两节弓弦（随蓄力往后拉开）、一支箭；</li>
 *    <li>**镀橙**：周身 60 格由橙色光雾球（球体内发射的 glow）铺满 + 雾色橙染，3 秒由原色渐变到纯橙；
 *        中断时 70 刻缓慢收拢恢复；打出去时立刻清干净并缩成红球，避免和爆炸糊在一起；</li>
 *    <li>**爆炸**：{@link GojoBlastClient} 的闪光 + 超大冲击波环 + 25/50/75/100 四道扩散环 +
 *        向外飞散的细光线；</li>
 *    <li>**火柱**：一根 80 格高的三维圆柱 + 柱底火球，40 秒里越烧越矮越细；</li>
 *    <li>**燃烧**：蓄力时被点燃的敌人身上挂橙色光斑（取代原版着火粒子）。</li>
 *  </ul> */
public final class FurnaceMaxClient {
    /** 火焰配色（argb）：主体橙、亮部近白、边缘金、深部暗红。 */
    private static final int FIRE=0xB4FF6A14,FIRE_HOT=0xC8FFD9A0,GOLD=0x99FFC24A,DEEP=0xA8E03A08,
        COAT=0x44FF8A2A,COAT_HOT=0x3CFFB060,RED=0xCCFF2418,SMOOTH=0x30A08878;
    private static final int BLOOM_FIRE=0xFF7A1206,BLOOM_GOLD=0xFF803010,BLOOM_COAT=0xFF8A4010,BLOOM_RED=0xFF8A0A06;
    /** 与苍赫茈相同的三张通用光效图。 */
    private static final String GLOW="textures/effect/glow.png",RING="textures/effect/ring.png",STREAK="textures/effect/streak.png";
    /** 火柱生成高度；弓高（满蓄力）= 玩家身高 1.8 的两倍。 */
    private static final double PILLAR_HEIGHT=80,BOW_HEIGHT=3.6;
    /** 镀橙渐变 / 收拢（中断 70 刻、打出去 8 刻）/ 红球时长。 */
    private static final int COAT_RAMP=60,COLLAPSE_TICKS=70,FAST_COLLAPSE=8,BALL_TICKS=22;

    private record Entry(PhotonFx.Handle handle,long end){}
    private static final class Charge{int owner;Vec3 pos=Vec3.ZERO;Vec3 dir=new Vec3(0,0,1);float progress;long end;}
    private static final class Field{
        int owner;Vec3 center=Vec3.ZERO;long start;long holdUntil;long collapseFrom;long nextCoat;int collapseTicks=COLLAPSE_TICKS;
        double intensity;boolean wiped;
    }
    private static final class Pillar{int owner;Vec3 center=Vec3.ZERO;long end;}
    private static final class Ball{Vec3 center;long from;}
    private static final class Burn{int entity;long end;Vec3 at=Vec3.ZERO;double height=1.8;}
    private static final Map<Integer,Charge> charges=new LinkedHashMap<>();
    private static final Map<Integer,Field> fields=new LinkedHashMap<>();
    private static final List<Pillar> pillars=new ArrayList<>();
    private static final List<Ball> balls=new ArrayList<>();
    private static final List<Burn> burns=new ArrayList<>();
    private static final Map<String,Entry> active=new HashMap<>();
    private static final Map<String,Long> throttled=new HashMap<>();
    private static long time;

    // ------------------------------------------------------------------ 句柄生命周期（照搬 GojoFx 的做法）
    private static void keep(String key,PhotonFx.Handle handle,int life){
        Entry old=active.remove(key);
        if(old!=null&&old.handle()!=handle)PhotonFx.stop(old.handle());
        if(handle!=null)active.put(key,new Entry(handle,time+life));
    }
    private static void drop(String key){
        Entry e=active.remove(key);
        if(e!=null)PhotonFx.stop(e.handle());
    }
    private static void dropPrefix(String prefix){
        List<String> keys=new ArrayList<>();
        for(String k:active.keySet())if(k.startsWith(prefix))keys.add(k);
        for(String k:keys)drop(k);
    }
    private static boolean throttle(String key,int gap){
        Long last=throttled.get(key);
        if(last!=null&&time-last<gap)return false;
        throttled.put(key,time);
        return true;
    }
    public static void clear(){
        for(Entry e:active.values())PhotonFx.stop(e.handle());
        active.clear();throttled.clear();
        charges.clear();fields.clear();pillars.clear();balls.clear();burns.clear();time=0;
    }

    // ------------------------------------------------------------------ 收包
    public static void receive(Network.Effect e){
        if(Minecraft.getInstance().level==null)return;
        if(e.kind()==FurnaceMax.FX_CHARGE){
            if(e.life()>0){
                Charge c=charges.computeIfAbsent(e.owner(),k->new Charge());
                c.owner=e.owner();c.pos=e.position();
                if(e.direction().lengthSqr()>1e-6)c.dir=e.direction();
                c.progress=Math.max(0f,Math.min(1f,e.size()));c.end=time+e.life()+2;
                Field f=fields.computeIfAbsent(e.owner(),k->new Field());
                if(f.start==0)f.start=time;
                f.owner=e.owner();f.center=e.position();f.holdUntil=time+e.life();f.collapseFrom=0;
                f.collapseTicks=COLLAPSE_TICKS;f.wiped=false;
            }else{
                charges.remove(e.owner());
                dropPrefix("bow/"+e.owner()+"/");
                collapse(e.owner(),COLLAPSE_TICKS);
            }
            return;
        }
        if(e.kind()==FurnaceMax.FX_SHOT){   // 弓矢：一根亮柱 + 金色细线尾迹
            if(!PhotonFx.ready())return;
            Vec3 at=e.position(),dir=e.direction().lengthSqr()<1e-6?new Vec3(0,0,1):e.direction().normalize();
            keep("shot/"+e.owner(),PhotonFx.beam(at.subtract(dir.scale(2.4)),at.add(dir.scale(1.2)),Math.max(0.18,e.size()*0.22),6),8);
            PhotonFx.ray(at,2.6,0.03,0.9,GOLD,BLOOM_GOLD,true,2,4,14,1.2);
            return;
        }
        if(e.kind()==FurnaceMax.FX_BLAST){
            // 爆炸这一刻：橙色镀色**立刻清干净**（不再和爆炸糊在一起），同时缩成红球
            charges.remove(e.owner());
            dropPrefix("bow/"+e.owner()+"/");
            collapse(e.owner(),FAST_COLLAPSE);
            wipeField(e.owner());
            balls.add(newBall(playerCenter(e.owner(),e.position())));
            blast(e.position());
            return;
        }
        if(e.kind()==FurnaceMax.FX_BURN){
            Burn b=null;
            for(Burn q:burns)if(q.entity==e.owner()){b=q;break;}
            if(b==null){b=new Burn();b.entity=e.owner();burns.add(b);}
            b.end=time+e.life();b.at=e.position();b.height=Math.max(1,e.size());
            return;
        }
        if(e.kind()==FurnaceMax.FX_PILLAR){
            if(e.life()<=0){
                // 火柱烧完：不是直接消失，而是缩成一颗红球再散掉
                pillars.removeIf(q->q.owner==e.owner()&&q.center.distanceToSqr(e.position())<9);
                drop("pillar/"+e.owner());drop("pillarBase/"+e.owner());
                drop("pillarFlow/"+e.owner());drop("pillarFlow2/"+e.owner());
                balls.add(newBall(e.position().add(0,1,0)));
                return;
            }
            Pillar found=null;
            for(Pillar q:pillars)if(q.owner==e.owner()&&q.center.distanceToSqr(e.position())<9){found=q;break;}
            if(found==null){found=new Pillar();found.owner=e.owner();found.center=e.position();pillars.add(found);}
            found.end=time+e.life();
        }
    }
    private static void collapse(int owner,int ticks){
        Field f=fields.get(owner);
        if(f==null)return;
        f.collapseFrom=time;f.collapseTicks=ticks;   // 后到的（更快的）收拢直接覆盖先前那条
    }
    /** 立刻清掉某个场的橙色光雾，并禁止再补：保证爆炸不与镀色重叠。 */
    private static void wipeField(int owner){
        Field f=fields.get(owner);
        if(f!=null)f.wiped=true;
        dropPrefix("coat/"+owner+"/");
    }
    private static Vec3 playerCenter(int owner,Vec3 fallback){
        var player=Minecraft.getInstance().player;
        return player!=null&&player.getId()==owner?player.position():fallback;
    }
    private static Ball newBall(Vec3 center){Ball b=new Ball();b.center=center;b.from=time;return b;}

    // ------------------------------------------------------------------ 每刻
    public static void tick(){
        if(Minecraft.getInstance().level==null){clear();return;}
        time++;
        active.values().removeIf(e->{if(time>=e.end()){PhotonFx.stop(e.handle());return true;}return false;});
        var player=Minecraft.getInstance().player;
        for(Iterator<Charge> it=charges.values().iterator();it.hasNext();){
            Charge c=it.next();
            if(time>c.end){it.remove();continue;}
            Vec3 pos=c.pos,dir=c.dir;
            if(player!=null&&player.getId()==c.owner){pos=player.position();dir=player.getLookAngle();}
            drawBow(c.owner,pos,dir,c.progress);
        }
        for(Iterator<Field> it=fields.values().iterator();it.hasNext();){
            Field f=it.next();
            if(player!=null&&player.getId()==f.owner)f.center=player.position();
            if(f.collapseFrom==0){
                f.intensity=Math.min(1,(time-f.start)/(double)COAT_RAMP);
                if(time>f.holdUntil+12)f.collapseFrom=time;   // 兜底：丢包也会自己收
            }else{
                f.intensity=Math.max(0,1-(time-f.collapseFrom)/(double)f.collapseTicks);
                if(f.intensity<=0){dropPrefix("coat/"+f.owner+"/");it.remove();continue;}
            }
            if(!f.wiped)coat(f);
        }
        for(Iterator<Ball> it=balls.iterator();it.hasNext();){
            Ball b=it.next();
            double k=(time-b.from)/(double)BALL_TICKS;
            if(k>=1){dropPrefix("ball/");it.remove();continue;}
            redBall(b,k);
        }
        for(Iterator<Burn> it=burns.iterator();it.hasNext();){
            Burn b=it.next();
            if(time>b.end){it.remove();continue;}
            Entity entity=player==null?null:player.level().getEntity(b.entity);
            if(entity instanceof LivingEntity living&&living.isAlive())b.at=living.position();
            if(time%2!=0||!PhotonFx.ready())continue;
            double h=b.height;
            PhotonFx.spark(b.at.add(0,h*0.5,0),Math.max(0.5,h*0.7),Math.max(0.6,h*0.45),FIRE,BLOOM_FIRE,GLOW,true,2,5,18,2.0);
            // 身上的火苗往上飘（带物理的火团）
            if(time%4==0)PhotonFx.blobs(b.at.add(0,h*0.35,0),Math.max(0.6,h*0.6),0.30,0.50,FIRE,BLOOM_FIRE,1.1,0.01,14,3,6);
        }
        for(Pillar q:pillars)drawPillar(q);
    }

    /** 当前"镀橙"强度（0~1）：给雾色与屏幕橙染用。只有站在场里才染。 */
    public static double intensity(){
        var player=Minecraft.getInstance().player;
        if(player==null)return 0;
        double k=0;
        for(Field f:fields.values()){
            double reach=FurnaceMax.FIELD_RADIUS*Math.max(0.35,f.intensity);
            if(player.position().distanceToSqr(f.center)<=reach*reach)k=Math.max(k,f.intensity);
        }
        for(Ball b:balls)if(player.position().distanceToSqr(b.center)<=256)k=Math.max(k,0.3);
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
    /** 屏幕橙染（很轻，主要交给雾色与光雾，避免刺眼）。 */
    public static void overlay(GuiGraphics g){
        double k=intensity();
        if(k<=0.02)return;
        var mc=Minecraft.getInstance();
        int alpha=(int)Math.min(60,54*k);
        g.fill(0,0,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),(alpha<<24)|0xFF6A14);
    }

    // ------------------------------------------------------------------ 蓄力：光线收束 + 圆柱拼出的火焰弓
    private static void drawBow(int owner,Vec3 pos,Vec3 look,float progress){
        if(time%2!=0||!PhotonFx.ready())return;
        Vec3 fwd=look.lengthSqr()<1e-6?new Vec3(0,0,1):look.normalize();
        Vec3 side=fwd.cross(new Vec3(0,1,0));
        if(side.lengthSqr()<1e-4)side=new Vec3(1,0,0);
        side=side.normalize();
        Vec3 vertical=side.cross(fwd).normalize();
        double h=BOW_HEIGHT*Math.max(0.15,progress),draw=0.95*progress;
        Vec3 center=pos.add(0,1.1,0).add(fwd.scale(0.9));
        Vec3 nock=center.subtract(fwd.scale(0.15+draw));
        Vec3 top=center.add(vertical.scale(h*0.5)),bottom=center.subtract(vertical.scale(h*0.5));
        String k="bow/"+owner+"/";
        // 两段弓臂：各 4 节三维圆柱，沿弧线首尾相接
        for(int limb=0;limb<2;limb++){
            double sign=limb==0?1:-1;
            Vec3 prev=null;
            for(int i=0;i<=4;i++){
                double t=0.16+0.84*i/4.0,ty=sign*t;
                Vec3 at=center.add(vertical.scale(h*0.5*ty)).add(fwd.scale(0.55*(1-ty*ty)));
                if(prev!=null)keep(k+"limb"+limb+"_"+i,PhotonFx.beam(prev,at,0.115+0.05*progress,5),6);
                prev=at;
            }
            keep(k+"tip"+limb,PhotonFx.shockwave(sign>0?top:bottom,0x88FFC24A,(float)(0.55+0.5*progress),5),6);
        }
        // 弓弦：中段被拉后 draw 格（拉弓）
        keep(k+"stringU",PhotonFx.beam(top,nock,0.055,5),6);
        keep(k+"stringD",PhotonFx.beam(bottom,nock,0.055,5),6);
        // 搭在弦上的箭
        keep(k+"arrow",PhotonFx.beam(nock,nock.add(fwd.scale(2.2+0.6*progress)),0.13+0.05*progress,5),6);
        // 小团火焰飞向"身前"汇聚成弓：球壳上生成的小火团沿径向飞入弓心（带物理，会拖着短尾）
        keep(k+"flow",PhotonFx.blobs(center,6.0+1.8*progress,0.30,0.46,FIRE,BLOOM_FIRE,-1.9,0,30,2,6),8);
        keep(k+"flow2",PhotonFx.blobs(center,3.0+1.0*progress,0.22,0.34,GOLD,BLOOM_GOLD,-1.4,0,18,2,5),7);
        // 金色细线从四周收进身体 + 身体火光 + 每 6 刻一圈脉冲
        PhotonFx.ray(pos,4.6+1.6*progress,0.028,0.85,GOLD,BLOOM_GOLD,true,2,4,10+ (int)(8*progress),-2.6);
        PhotonFx.spark(pos.add(0,1.0,0),1.5+0.7*progress,1.0+0.4*progress,FIRE,BLOOM_FIRE,GLOW,true,2,6,16,3.0);
        if(time%6==0)PhotonFx.shockwave(pos,0x66FF8A2A,(float)(2.2+2.4*progress),12);
    }

    // ------------------------------------------------------------------ 镀橙场（橙色光雾球）
    private static void coat(Field f){
        if(time<f.nextCoat)return;
        f.nextCoat=time+8;
        if(!PhotonFx.ready())return;
        double k=f.intensity;
        // 两个不同半径的球体发射器：外圈把 60 格铺满，内圈加厚近处（发射率对齐模组既有特效的量级，不硬堆）
        keep("coat/"+f.owner+"/outer",PhotonFx.sphere(f.center,FurnaceMax.FIELD_RADIUS*(0.55+0.45*k),7+4*k,COAT,BLOOM_COAT,GLOW,true,10,12,55+85*k,0.5),16);
        keep("coat/"+f.owner+"/inner",PhotonFx.sphere(f.center,FurnaceMax.FIELD_RADIUS*0.45*k,5+3*k,COAT_HOT,BLOOM_COAT,GLOW,true,10,12,28+45*k,1.2),16);
    }

    // ------------------------------------------------------------------ 火柱（一根三维圆柱）
    private static void drawPillar(Pillar q){
        if(!PhotonFx.ready())return;
        double remaining=Math.max(0,q.end-time);
        double k=Math.min(1,remaining/FurnaceMax.PILLAR_TICKS);
        // 越接近烧完缩得越快（k² 曲线），最后正好缩进柱底那颗红球里
        double height=PILLAR_HEIGHT*(0.12+0.88*k*k),width=1.6+3.6*k;
        if(throttle("pillarFill",6)){
            keep("pillar/"+q.owner,PhotonFx.beam(q.center,q.center.add(0,height,0),width,10),12);
            keep("pillarBase/"+q.owner,PhotonFx.sphere(q.center.add(0,1.5,0),2.0+2.6*k,2.2+2.4*k,FIRE,BLOOM_FIRE,GLOW,true,8,10,50+50*k,1.6),12);
        }
        if(time%4==0){
            PhotonFx.ray(q.center.add(0,1,0),Math.max(2,width*1.6),0.035,1.6,GOLD,BLOOM_GOLD,true,3,5,14,3.0);
            PhotonFx.spark(q.center.add(0,height*0.6,0),Math.max(1.5,width),1.6+1.4*k,FIRE_HOT,BLOOM_FIRE,GLOW,true,3,6,26,1.0);
            // 火星往上飘：径向初速 + 一点点重力（丝线物理）
            keep("pillarFlow/"+q.owner,PhotonFx.lines(q.center.add(0,1,0),Math.max(1.6,width*1.1),0.05,1.5,GOLD,BLOOM_GOLD,2.6,0.05,20,4,10,false),12);
            keep("pillarFlow2/"+q.owner,PhotonFx.blobs(q.center.add(0,2,0),Math.max(1.4,width*0.9),0.40,0.60,FIRE,BLOOM_FIRE,1.6,0.03,16,4,9),12);
        }
    }

    // ------------------------------------------------------------------ 收尾红球
    private static void redBall(Ball b,double k){
        if(!PhotonFx.ready()||time%2!=0)return;
        double radius=1.8*(1-k)+0.25,size=2.2*(1-k)+0.3;
        PhotonFx.spark(b.center.add(0,1,0),radius,size,RED,BLOOM_RED,GLOW,true,2,5,42,0.4);
        if(time%6==0)PhotonFx.shockwave(b.center.add(0,1,0),0x88FF2418,Math.max(0.5,radius*2.4),10);
        if(k<0.4)keep("ball/"+b.center+"/smoke",PhotonFx.sphere(b.center.add(0,1,0),radius*2.4,3.0,SMOOTH,0,GLOW,true,3,8,26,0.8),12);
    }

    // ------------------------------------------------------------------ 爆炸
    private static void blast(Vec3 center){
        if(!PhotonFx.ready())return;
        GojoBlastClient.spawn(center,26,0xFFB060);
        keep("blastRing",PhotonFx.shockwave(center,0xE0FF7A20,(float)FurnaceMax.BLAST_RADIUS,34),40);
        // 25/50/75/100 四道扩散环
        for(int ring=1;ring<=4;ring++){
            double r=FurnaceMax.BLAST_RADIUS*ring/4.0;
            for(int i=0;i<6;i++){
                double a=i*Math.PI/3+random.nextDouble()*0.3;
                Vec3 at=center.add(Math.cos(a)*r,(random.nextDouble()-0.35)*4,Math.sin(a)*r);
                PhotonFx.spark(at,4.0+ring*2.0,3.0+ring*1.4,ring<=2?FIRE:FIRE_HOT,BLOOM_FIRE,GLOW,true,4,10,60,1.5);
            }
        }
        // 向外飞散的丝线：**带物理**——径向初速 + 重力，飞出去会往下坠
        keep("burst",PhotonFx.lines(center,12,0.055,2.4,GOLD,BLOOM_GOLD,6.5,0.09,64,8,16,false),24);
        keep("burst2",PhotonFx.lines(center,6,0.035,1.4,0xFFF0C0,BLOOM_GOLD,9.0,0.12,40,6,12,false),18);
        PhotonFx.ray(center,70,0.02,1.6,0xAAFFD9A0,BLOOM_GOLD,true,3,5,26,5.5);
        PhotonFx.spark(center,26,7,DEEP,BLOOM_FIRE,GLOW,true,4,12,90,0.6);
    }

    private static final Random random=new Random();
    private FurnaceMaxClient(){}
}
