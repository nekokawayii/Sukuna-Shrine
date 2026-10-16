package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** 「灶 · 最大功率 · 开」的客户端表现层：与苍 / 赫 / 茈 同一条 Photon 管线（{@link PhotonFx}），
 *  **不使用任何贴图粒子**（flame/dust/void_rift 一律不用）。
 *
 *  <ul>
 *    <li>**蓄力**：火**一团一团**在玩家**左侧**凝成火弓——弓臂/弓弦/箭由小火团点出，
 *        另有一层火团从外侧沿径向飞进弓心；每团火都带抖动、上飘与火星，做出"燃烧感"。
 *        **不在玩家身上放光雾**（上一版挡视野又光污染），镀橙的雾色与屏幕橙染也调轻了；</li>
 *    <li>**镀橙**：周身 60 格铺橙色光雾球 + 雾色橙染（55%），3 秒由原色渐变到纯橙；
 *        中断 70 刻缓慢恢复；打出去时立刻清干净并缩成红球，不与爆炸重叠；</li>
 *    <li>**弓矢**：亮柱 + 金色细线 + 一串火团尾迹；</li>
 *    <li>**爆炸（要光污染）**：镜头震动 + 闪光、三层冲击波环、八道火环、三层放射丝线（带物理）、
 *        中心火海、抛洒大块火团；</li>
 *    <li>**火柱**：**创建即锁定 15 秒**（不再被任何收尾包提前掐掉），柱体由火团堆成 + 一根内核圆柱，
 *        越烧越矮越细，**烧完缩成红球**消失。</li>
 *  </ul> */
public final class FurnaceMaxClient {
    /** 火焰配色（argb）。 */
    private static final int FIRE=0xC8FF6A14,FIRE_HOT=0xD8FFD9A0,GOLD=0x99FFC24A,DEEP=0xB8E03A08,
        RED=0xD8FF2418,SMOOTH=0x30A08878;
    private static final int BLOOM_FIRE=0xFF7A1206,BLOOM_GOLD=0xFF803010,BLOOM_RED=0xFF8A0A06;
    private static final String GLOW="textures/effect/glow.png";
    /** 弓高：玩家身高 1.8 的两倍 = 3.6，再按用户要求放大三倍 → 10.8 格。 */
    private static final double BOW_HEIGHT=10.8;
    /** 镀橙渐变 / 收拢（中断 70 刻、打出去 8 刻）/ 红球时长。 */
    private static final int COAT_RAMP=60,COLLAPSE_TICKS=70,FAST_COLLAPSE=8,BALL_TICKS=22;

    /** 不抢占 key 的句柄：新旧发射器**重叠存活**，各自到点自然消失。
     *  长驻形状（弓脊、火柱内核、镀色光雾）必须这么刷，否则"边建边停"会出现间隙（看起来就是时有时无）。 */
    private record Spare(PhotonFx.Handle handle,long end,String group){}
    private static final class Charge{int owner;Vec3 pos=Vec3.ZERO;Vec3 dir=new Vec3(0,0,1);float progress;long end;}
    private static final class Field{
        int owner;Vec3 center=Vec3.ZERO;long start;long holdUntil;long collapseFrom;int collapseTicks=COLLAPSE_TICKS;
        double intensity;boolean wiped;
    }
    private static final class Pillar{int owner;Vec3 center=Vec3.ZERO;long born;long end;}
    private static final class Ball{Vec3 center;long from;}
    private static final class Burn{int entity;long end;Vec3 at=Vec3.ZERO;double height=1.8;}
    private static final Map<Integer,Charge> charges=new LinkedHashMap<>();
    private static final Map<Integer,Field> fields=new LinkedHashMap<>();
    private static final List<Pillar> pillars=new ArrayList<>();
    private static final List<Ball> balls=new ArrayList<>();
    private static final List<Burn> burns=new ArrayList<>();
    private static final List<Spare> spares=new ArrayList<>();
    private static final Map<String,Long> throttled=new HashMap<>();
    private static final Random random=new Random();
    private static long time;
    /** 蓄力时的 FOV 倍数（用户要求：**不减** FOV，反而放大 2 倍）。 */
    private static double fovBoost=1,SEEK_FOV=2.0;

    // ------------------------------------------------------------------ 句柄生命周期
    /** 重叠刷新：只登记，不去停上一个（到点自然过期）。 */
    private static void spawn(String group,PhotonFx.Handle handle,int life){
        if(handle!=null)spares.add(new Spare(handle,time+life,group));
    }
    /** 立刻清掉某一组重叠句柄（爆炸瞬间清镀色、弓收招、火柱收尾等）。 */
    private static void wipeGroup(String prefix){
        spares.removeIf(s->{if(s.group().startsWith(prefix)){PhotonFx.stop(s.handle());return true;}return false;});
    }
    private static boolean throttle(String key,int gap){
        Long last=throttled.get(key);
        if(last!=null&&time-last<gap)return false;
        throttled.put(key,time);
        return true;
    }
    public static void clear(){
        for(Spare s:spares)PhotonFx.stop(s.handle());
        spares.clear();throttled.clear();
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
                wipeGroup("bow/"+e.owner()+"/");
                collapse(e.owner(),COLLAPSE_TICKS);
            }
            return;
        }
        if(e.kind()==FurnaceMax.FX_SHOT){   // 弓矢：亮柱 + 金线 + 火团尾迹
            if(!PhotonFx.ready())return;
            Vec3 at=e.position(),dir=e.direction().lengthSqr()<1e-6?new Vec3(0,0,1):e.direction().normalize();
            spawn("shot/",PhotonFx.beam(at.subtract(dir.scale(5.0)),at.add(dir.scale(2.6)),1.1,6),8);
            PhotonFx.ray(at,4.6,0.045,1.7,GOLD,BLOOM_GOLD,true,3,6,44,2.2);
            flameCluster(at,0.95,3.0);
            flameCluster(at.subtract(dir.scale(2.6)),0.8,2.4);
            if(time%2==0)PhotonFx.spark(at,3.2,2.6,FIRE,BLOOM_FIRE,GLOW,true,2,6,34,1.1);
            return;
        }
        if(e.kind()==FurnaceMax.FX_BLAST){
            charges.remove(e.owner());
            wipeGroup("bow/"+e.owner()+"/");
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
            if(e.life()>0){
                Pillar found=findPillar(e.owner(),e.position());
                if(found==null){
                    found=new Pillar();found.owner=e.owner();found.center=e.position();
                    found.born=time;found.end=time+FurnaceMax.PILLAR_TICKS;   // 创建即锁定 15 秒
                    pillars.add(found);
                }else{
                    found.end=Math.max(found.end,time+Math.min(e.life(),FurnaceMax.PILLAR_TICKS));
                }
            }else{
                // 收尾包只在"确实烧满了"时才认，任何异常都不能把火柱提前掐掉
                Pillar q=findPillar(e.owner(),e.position());
                if(q!=null&&time-q.born>=FurnaceMax.PILLAR_TICKS-30)finishPillar(q);
            }
            return;
        }
    }
    private static Pillar findPillar(int owner,Vec3 at){
        for(Pillar q:pillars)if(q.owner==owner&&q.center.distanceToSqr(at)<9)return q;
        return null;
    }
    /** 火柱烧完：收掉所有柱体句柄并在柱底生成红球。 */
    private static void finishPillar(Pillar q){
        wipeGroup("pillar/"+q.owner+"/");
        pillars.remove(q);
        balls.add(newBall(q.center.add(0,1,0)));
    }
    private static void collapse(int owner,int ticks){
        Field f=fields.get(owner);
        if(f==null)return;
        f.collapseFrom=time;f.collapseTicks=ticks;
    }
    private static void wipeField(int owner){
        Field f=fields.get(owner);
        if(f!=null)f.wiped=true;
        wipeGroup("coat/"+owner+"/");
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
        spares.removeIf(s->{if(time>=s.end()){PhotonFx.stop(s.handle());return true;}return false;});
        // FOV：蓄力期间放大到 2 倍，每刻 8% 平滑（约 1 秒到位）
        var local=Minecraft.getInstance().player;
        double fovTarget=local!=null&&charges.containsKey(local.getId())?SEEK_FOV:1.0;
        fovBoost+=(fovTarget-fovBoost)*0.08;
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
                if(time>f.holdUntil+12)f.collapseFrom=time;
            }else{
                f.intensity=Math.max(0,1-(time-f.collapseFrom)/(double)f.collapseTicks);
                if(f.intensity<=0){wipeGroup("coat/"+f.owner+"/");it.remove();continue;}
            }
            if(!f.wiped)coat(f);
        }
        for(Iterator<Ball> it=balls.iterator();it.hasNext();){
            Ball b=it.next();
            double k=(time-b.from)/(double)BALL_TICKS;
            if(k>=1){wipeGroup("ball/");it.remove();continue;}
            redBall(b,k);
        }
        for(Iterator<Burn> it=burns.iterator();it.hasNext();){
            Burn b=it.next();
            if(time>b.end){it.remove();continue;}
            Entity entity=player==null?null:player.level().getEntity(b.entity);
            if(entity instanceof LivingEntity living&&living.isAlive())b.at=living.position();
            if(time%2!=0||!PhotonFx.ready())continue;
            flameCluster(b.at.add(0,b.height*0.5,0),Math.max(0.3,b.height*0.35),1.0);
        }
        int pn=pillars.size();
        for(int i=0;i<pn;i++){
            Pillar q=pillars.get(i);
            if(time>q.born+FurnaceMax.PILLAR_TICKS){finishPillar(q);break;}   // 到点自己收尾
            drawPillar(q);
        }
    }

    // ------------------------------------------------------------------ 镀橙（雾色/屏幕）
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
    /** 蓄力期间 FOV ×{@link #SEEK_FOV}（2 倍）：视野拉宽，不是缩窄。 */
    public static void fov(net.minecraftforge.client.event.ViewportEvent.ComputeFov e){
        if(Math.abs(fovBoost-1)<0.01)return;
        e.setFOV(e.getFOV()*(float)fovBoost);
    }
    /** 雾色向橙金靠拢（比上一版轻，避免"糊屏"）。 */
    public static void fog(net.minecraftforge.client.event.ViewportEvent.ComputeFogColor e){
        double k=intensity();
        if(k<=0.01)return;
        double m=0.55*k;
        e.setRed((float)(e.getRed()*(1-m)+1.00*m));
        e.setGreen((float)(e.getGreen()*(1-m)+0.45*m));
        e.setBlue((float)(e.getBlue()*(1-m)+0.10*m));
    }
    /** 屏幕橙染已按要求删除（蓄力的光污染）。 */
    public static void overlay(GuiGraphics g){}

    // ------------------------------------------------------------------ 蓄力：左侧凝成火弓
    /** 火**一团一团**凝成弓：点位都在玩家的**左手边**，颜色/大小/上飘都带抖动＝燃烧感。
     *  **不在玩家身上放任何光雾**，只留一点收束金线，不挡视野。 */
    private static void drawBow(int owner,Vec3 pos,Vec3 look,float progress){
        if(time%2!=0||!PhotonFx.ready())return;
        Vec3 fwd=look.lengthSqr()<1e-6?new Vec3(0,0,1):look.normalize();
        Vec3 right=fwd.cross(new Vec3(0,1,0));
        if(right.lengthSqr()<1e-4)right=new Vec3(1,0,0);
        right=right.normalize();
        Vec3 left=right.scale(-1);                     // 玩家左手边
        Vec3 vertical=right.cross(fwd).normalize();     // 弓臂方向（竖直）
        double h=BOW_HEIGHT*Math.max(0.18,progress);
        double bulge=h*0.17;                            // 弓弧深度：随弓一起放大
        // 弓面**垂直于视线**（弓背朝左 / 弦朝玩家）：从背后看才是清清楚楚一张弓，
        // 而不是顺着视线的一条竖线——上一版"看不出来是弓"的主因就在这里。
        Vec3 center=pos.add(0,1.15,0).add(left.scale(1.2)).add(fwd.scale(0.5));
        Vec3 tipTop=center.add(vertical.scale(h*0.5)),tipBottom=center.subtract(vertical.scale(h*0.5));
        Vec3 belly=center.add(left.scale(bulge));       // 弓背（弧顶）
        double cs=0.85+0.55*progress;                   // 单团火的大小（随蓄力更粗）
        // 飞进来的火团：从弓心外沿径向飞入（"凝成"的过程），范围与密度都加大
        spawn("bow/"+owner+"/in",PhotonFx.blobs(center,h*0.50+2*progress,0.34,0.55,FIRE,BLOOM_FIRE,-2.0,0,26,4,7),11);
        // 弓臂：两条二次贝塞尔弧（上稍→弓背、弓背→下稍），各 9 团
        for(int half=0;half<2;half++){
            Vec3 a=half==0?tipTop:belly,b=half==0?belly:tipBottom;
            Vec3 ctrl=half==0?center.add(vertical.scale(h*0.30)).add(left.scale(bulge*0.62))
                            :center.subtract(vertical.scale(h*0.30)).add(left.scale(bulge*0.62));
            for(int i=0;i<=8;i++){
                double t=i/8.0,mt=1-t;
                Vec3 at=a.scale(mt*mt).add(ctrl.scale(2*mt*t)).add(b.scale(t*t));
                flameCluster(at,cs,3.4);
            }
        }
        // 弓弦：从上稍到下稍的直线（在弓背右侧＝靠玩家这一侧），中点被拉后 draw 格（拉弓）
        double draw=bulge*0.95*progress;
        Vec3 nock=center.add(right.scale(draw*0.9));
        for(int half=0;half<2;half++){
            Vec3 a=half==0?tipTop:tipBottom;
            for(int i=1;i<=7;i++)flameCluster(a.lerp(nock,i/7.0),cs*0.7,1.7);   // 弦：更细
        }
        flameCluster(nock,cs*0.9,2.0);
        // 搭在弦上的箭：从弦中段沿瞄准方向伸出
        flameCluster(nock.add(fwd.scale(h*0.14)),cs*0.9,2.2);
        flameCluster(nock.add(fwd.scale(h*0.30+1.2*progress)),cs*0.8,2.4);
        // 弓身外圈的火环 + 上飘火星：让整张弓"烧起来"
        if(time%4==0)PhotonFx.spark(center,h*0.46,2.2,FIRE,BLOOM_FIRE,GLOW,true,4,10,55,1.1);
        if(time%3==0)PhotonFx.lines(center.subtract(vertical.scale(h*0.45)),h*0.55,0.05,1.8,GOLD,BLOOM_GOLD,3.6,0.02,26,4,12,false);
        // 金色收束线/收束射线**已按要求删除**：蓄力期间只有"火团凝成弓"这一件事
    }
    /** 一小团火：大小/颜色随机 + 轻微外扩 + 上飘火星＝燃烧感。 */
    private static void flameCluster(Vec3 at,double size,double rate){
        int color=random.nextInt(4)==0?FIRE_HOT:(random.nextInt(3)==0?DEEP:FIRE);
        PhotonFx.blobs(at,0.30+size*0.8,size*0.65,size*1.05,color,BLOOM_FIRE,0.22+random.nextDouble()*0.4,
            0.012,Math.max(6,(int)(12*rate)),3,6+random.nextInt(4));
    }

    // ------------------------------------------------------------------ 镀橙场（只留"方块变橙"，不放光雾）
    /** 蓄力期间的**光污染已按要求删掉**：这里不再生成任何橙色光雾粒子，
     *  只用雾色把周身 60 格的方块由原色渐变到纯橙（见 {@link #fog}）。 */
    private static void coat(Field f){
        if(f.wiped)return;   // 光雾已移除：保留接口，收拢逻辑改由 intensity 驱动雾色
    }

    // ------------------------------------------------------------------ 火柱＝"刚爆炸的样子"保持 15 秒
    /** 不再收成一根细柱：**爆炸那一刻的火海原样保持 15 秒**——持续翻腾的大火球 +
     *  不断上飘的火团 + 每 20 刻一圈向外扩散的冲击环 + 持续向外飞散的丝线，最后 2 秒收缩进红球。 */
    private static void drawPillar(Pillar q){
        if(!PhotonFx.ready())return;
        double remaining=Math.max(0,q.born+FurnaceMax.PILLAR_TICKS-time);
        double k=Math.min(1,remaining/FurnaceMax.PILLAR_TICKS);
        double shrink=k<0.16?k/0.16:1;               // 只有最后 ~2.4 秒才收缩
        // 火海本体：两层大火球持续补，保持"刚爆炸"的体积（再加大）
        if(time%6==0){
            spawn("pillar/"+q.owner+"/sea",PhotonFx.sphere(q.center.add(0,5*shrink,0),42*shrink,13*shrink,FIRE,BLOOM_FIRE,GLOW,true,12,12,220*shrink,0.7),20);
            spawn("pillar/"+q.owner+"/sea2",PhotonFx.sphere(q.center.add(0,2*shrink,0),20*shrink,8*shrink,FIRE_HOT,BLOOM_FIRE,GLOW,true,10,10,140*shrink,1.3),18);
            spawn("pillar/"+q.owner+"/sea3",PhotonFx.sphere(q.center.add(0,14*shrink,0),26*shrink,10*shrink,DEEP,BLOOM_FIRE,GLOW,true,10,12,120*shrink,0.9),20);
        }
        // 火团持续上飘 / 向四周翻滚
        if(time%2==0){
            for(int i=0;i<11;i++){
                double a=random.nextDouble()*Math.PI*2,r=Math.sqrt(random.nextDouble())*40*shrink;
                flameCluster(q.center.add(Math.cos(a)*r,random.nextDouble()*34*shrink,Math.sin(a)*r),0.6+0.4*k,2.6);
            }
        }
        // 每 12 刻一圈向外扩散的环 + 闪一下：让"爆炸还在继续"
        if(time%12==0){
            spawn("pillar/"+q.owner+"/pulse",PhotonFx.shockwave(q.center.add(0,1,0),0xE0FF7A20,(float)(16+50*shrink),30),38);
            PhotonFx.ray(q.center.add(0,2,0),55*shrink,0.02,1.8,0xAAFFD9A0,BLOOM_GOLD,true,4,6,40,7.0);
            spawn("pillar/"+q.owner+"/tornado",PhotonFx.sphere(q.center.add(0,18*shrink,0),30*shrink,8*shrink,FIRE,BLOOM_FIRE,GLOW,true,10,12,140*shrink,2.4),22);
            Client.flash(5);Client.shake(6);
        }
        // 持续向外飞散的丝线（带物理）
        if(time%6==0){
            spawn("pillar/"+q.owner+"/ray",PhotonFx.lines(q.center.add(0,3*shrink,0),22*shrink,0.07,2.8,GOLD,BLOOM_GOLD,6.5,0.10,80,10,18,false),30);
            spawn("pillar/"+q.owner+"/ray2",PhotonFx.lines(q.center.add(0,2*shrink,0),11*shrink,0.05,1.7,0xFFF0C0,BLOOM_GOLD,9.5,0.14,60,8,14,false),24);
            spawn("pillar/"+q.owner+"/ray3",PhotonFx.lines(q.center.add(0,4*shrink,0),34*shrink,0.09,3.6,DEEP,BLOOM_FIRE,4.2,0.06,45,12,22,false),32);
        }
    }

    // ------------------------------------------------------------------ 收尾红球
    private static void redBall(Ball b,double k){
        if(!PhotonFx.ready()||time%2!=0)return;
        double radius=1.8*(1-k)+0.25,size=2.2*(1-k)+0.3;
        PhotonFx.spark(b.center.add(0,1,0),radius,size,RED,BLOOM_RED,GLOW,true,2,5,42,0.4);
        if(time%6==0)PhotonFx.shockwave(b.center.add(0,1,0),0x88FF2418,Math.max(0.5,radius*2.4),10);
        if(k<0.4)spawn("ball/"+b.center+"/",PhotonFx.sphere(b.center.add(0,1,0),radius*2.4,3.0,SMOOTH,0,GLOW,true,3,8,26,0.8),12);
    }

    // ------------------------------------------------------------------ 爆炸（往"光污染"方向堆）
    private static void blast(Vec3 center){
        if(!PhotonFx.ready())return;
        Client.shake(52);Client.flash(22);
        GojoBlastClient.spawn(center,70,0xFFC070);
        GojoBlastClient.spawn(center.add(0,8,0),34,0xFFE8B0);
        GojoBlastClient.spawn(center,18,0xFFFFFFF0);
        spawn("blast/",PhotonFx.shockwave(center,0xF0FF7A20,(float)FurnaceMax.BLAST_RADIUS,40),46);
        spawn("blast/",PhotonFx.shockwave(center,0xC0FFD9A0,(float)(FurnaceMax.BLAST_RADIUS*0.62),34),40);
        spawn("blast/",PhotonFx.shockwave(center,0xA0FF3A10,(float)(FurnaceMax.BLAST_RADIUS*1.06),54),60);
        spawn("blast/",PhotonFx.shockwave(center,0xFFE0A8,(float)(FurnaceMax.BLAST_RADIUS*0.34),28),34);
        spawn("blast/",PhotonFx.shockwave(center,0x90FF3A10,(float)(FurnaceMax.BLAST_RADIUS*1.45),70),80);
        spawn("blast/",PhotonFx.shockwave(center,0x70FFFFFF,(float)(FurnaceMax.BLAST_RADIUS*0.18),22),28);
        for(int ring=1;ring<=8;ring++){                       // 八道火环
            double r=FurnaceMax.BLAST_RADIUS*ring/8.0;
            for(int i=0;i<8;i++){
                double a=i*Math.PI/4+random.nextDouble()*0.3;
                Vec3 at=center.add(Math.cos(a)*r,(random.nextDouble()-0.35)*8,Math.sin(a)*r);
                PhotonFx.spark(at,5.5+ring*1.8,4.0+ring*1.2,ring<=4?FIRE:FIRE_HOT,BLOOM_FIRE,GLOW,true,5,11,44,1.7);
            }
        }
        // 六层带物理的放射丝线（飞出去会被重力压弯）
        spawn("blast/",PhotonFx.lines(center,14,0.06,2.6,GOLD,BLOOM_GOLD,7.5,0.10,90,10,18,false),28);
        spawn("blast/",PhotonFx.lines(center,7,0.04,1.6,0xFFF0C0,BLOOM_GOLD,11.0,0.14,70,8,14,false),22);
        spawn("blast/",PhotonFx.lines(center,20,0.09,3.4,DEEP,BLOOM_FIRE,5.0,0.06,60,12,22,false),32);
        spawn("blast/",PhotonFx.lines(center,30,0.11,4.2,0xB0FF3A10,BLOOM_FIRE,3.2,0.05,50,14,24,false),36);
        spawn("blast/",PhotonFx.lines(center,44,0.13,5.4,0xA0FF6A14,BLOOM_FIRE,2.4,0.04,40,16,26,false),38);
        spawn("blast/",PhotonFx.lines(center,4,0.03,1.1,0xFFFFFFFF,BLOOM_GOLD,14.0,0.18,50,6,12,false),20);
        PhotonFx.ray(center,140,0.02,2.2,0xAAFFD9A0,BLOOM_GOLD,true,6,8,46,8.0);
        spawn("blast/",PhotonFx.sphere(center,42,13,FIRE,BLOOM_FIRE,GLOW,true,30,16,200,0.8),48);
        spawn("blast/",PhotonFx.sphere(center,18,8,FIRE_HOT,BLOOM_FIRE,GLOW,true,24,12,130,1.4),40);
        spawn("blast/",PhotonFx.sphere(center.add(0,18,0),30,10,DEEP,BLOOM_FIRE,GLOW,true,26,14,110,1.0),44);
        for(int i=0;i<30;i++){                                // 抛洒大块火团
            double a=random.nextDouble()*Math.PI*2,r=random.nextDouble()*110;
            Vec3 at=center.add(Math.cos(a)*r,(random.nextDouble()-0.3)*16,Math.sin(a)*r);
            PhotonFx.blobs(at,8,0.55,1.0,FIRE,BLOOM_FIRE,4.2,0.10,22,7,18);
        }
    }

    private FurnaceMaxClient(){}
}
