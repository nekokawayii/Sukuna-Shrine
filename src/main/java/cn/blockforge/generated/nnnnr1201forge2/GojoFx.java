package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.world.phys.Vec3;

/** 五条悟招式编号与客户端 Photon 演出：服务端只发编号，客户端按编号即时构建 Photon 特效。 */
public final class GojoFx {
    // 编号必须避开终式 180-186，否则会被 Client.receive 的终式分支抢先处理；
    // 204-218 是魔虚罗、219-220 是信夫（ShinbukeVisuals），所以「宇宙大爆炸」只能取 221。
    public static final int BLOCK=187,HIT=188,CHARGE=189,BEAM=190,BLUE=191,BLUE_END=192,RED=193,RED_END=194,
        PURPLE=195,PURPLE_GATHER=196,PURPLE_FORM=197,PURPLE_BURST=198,UNLIMITED=199,TELEPORT=200,
        DOMAIN=201,DOMAIN_OPEN=202,DOMAIN_CLOSE=203,BIG_BANG=221,
        PURPLE_CHARGE=222,PURPLE_TAP=223,PURPLE_TAP_BURST=224;
    /** 无量空处的绝对半径：画布、领域判定、对拼范围共用同一个数。 */
    public static final float DOMAIN_RADIUS=100f;
    private record Entry(PhotonFx.Handle handle,long end,boolean sticky){}
    private static final Map<String,Entry> active=new HashMap<>();
    private static final Map<String,Long> throttled=new HashMap<>();
    /** 记录发射器真正的创建时刻：Photon 的 lifetime 到期后不会自己续命。 */
    private static long time;
    public static void clear(){for(Entry e:active.values())PhotonFx.stop(e.handle());active.clear();throttled.clear();time=0;GojoVoidClient.close();GojoBlastClient.clear();GojoBeamClient.clear();GojoTrailClient.clear();GojoGravityClient.clear();}
    public static void tick(){
        time++;GojoVoidClient.tick();GojoBlastClient.tick();GojoBeamClient.tick();GojoTrailClient.tick();GojoGravityClient.tick();
        active.values().removeIf(e->{
            if(e.sticky()&&time<e.end())return false;
            if(time>=e.end()){PhotonFx.stop(e.handle());return true;}
            return false;
        });
    }
    private static void keep(String key,PhotonFx.Handle handle,int life,boolean sticky){
        Entry old=active.remove(key);
        if(old!=null&&old.handle()!=handle)PhotonFx.stop(old.handle());
        if(handle!=null)active.put(key,new Entry(handle,time+life,sticky));
    }
    /** 只续期不重建：领域等长时间特效不再每帧重建，避免闪烁。 */
    private static void refresh(String key,int life){
        Entry e=active.get(key);
        if(e!=null)active.put(key,new Entry(e.handle(),time+life,e.sticky()));
    }
    /** 同一来源的高频命中只保留一个句柄并限频，避免体术特效无限叠加。 */
    private static boolean throttle(String key,int gap){
        Long last=throttled.get(key);
        if(last!=null&&time-last<gap)return false;
        throttled.put(key,time);
        return true;
    }
    private static void drop(String key){
        Entry e=active.remove(key);
        if(e!=null)PhotonFx.stop(e.handle());
    }
    private static final String GLOW="textures/effect/glow.png",GALAXY="textures/effect/galaxy.png",STREAK="textures/effect/streak.png";
    private static Vec3 sideOf(Vec3 at){
        var mc=net.minecraft.client.Minecraft.getInstance();
        Vec3 eye=mc.player==null?at.add(1,0,0):mc.player.getEyePosition();
        Vec3 dir=at.subtract(eye).multiply(1,0,1);
        if(dir.lengthSqr()<1e-4)return new Vec3(1,0,0);
        dir=dir.normalize();
        return new Vec3(-dir.z,0,dir.x);
    }
    public static void receive(Network.Effect e){
        Vec3 at=e.position();String owner=String.valueOf(e.owner());
        // 宇宙大爆炸走模组自己的顶点通路（GojoBlastClient），**不依赖 Photon**：
        // 必须在 ready() 门槛之前处理，否则没装 Photon 的客户端就只剩一声响。
        if(e.kind()==BIG_BANG){
            GojoBlastClient.bigBang(at,Math.max(1,e.size()));
            GojoTrailClient.drop(orbKey(e));
            GojoGravityClient.drop(orbKey(e));
            if(!PhotonFx.ready())return;
            // 星尘 + 一圈冲击波：给自绘的大爆炸再叠一层粒子（银河贴图自带星野感）
            spark(at,Math.max(3,e.size())*0.85,1.1,0xC0C08CFF,0xFF3E1C63,GALAXY,true,3,8,34,1.4);
            PhotonFx.shockwave(at,0xE0C8A0FF,Math.max(4,e.size())*1.8,30);
            return;
        }
        // 拖尾同样是自绘的：先把这一包的坐标记进缎带，再决定要不要叠粒子。
        long orb=orbKey(e);
        if(e.kind()==BLUE||e.kind()==PURPLE)GojoTrailClient.feed(e.kind()==BLUE?GojoTrailClient.BLUE:GojoTrailClient.PURPLE,orb,at);
        if(e.kind()==BLUE)GojoGravityClient.track(orb,at,Math.max(0.8,e.size()));
        if(!PhotonFx.ready())return;
        switch(e.kind()){
            case BLOCK->{if(throttle("block/"+owner,5))keep("block/"+owner,PhotonFx.shockwave(at,0x66BFE4FF,1.6,10),10,false);}
            case HIT->{if(throttle("hit/"+owner,3))keep("hit/"+owner,PhotonFx.shockwave(at,0x88FFD9C8,1.3,9),9,false);}
            // 蓄力：手心一颗红球 + 细小光线从四周飞入（stretch 是"窄而长"的细线，不是白方块）
            case CHARGE->{
                spark(at,0.30,0.42,0xC0FF3A20,0xFF7A1206,GLOW,true,2,3,26,3.0);
                PhotonFx.ray(at,1.7,0.035,0.85,0x99FFFFFF,0x30A0C0FF,true,2,3,10,-1.6);
                PhotonFx.ray(at,2.8,0.022,0.70,0x66FFD9A0,0x20FF8040,true,2,3,7,-2.4);
            }
            // 打出去：Photon 画三维红圆柱，白环与光轴由 GojoBeamClient 补（每 2 格一圈）
            case BEAM->{
                PhotonFx.beam(at,e.direction(),Math.max(0.14,e.size()),Math.max(3,e.life()));
                GojoBeamClient.spawn(at,e.direction(),Math.max(0.14,e.size()),Math.max(3,e.life()));
            }
            case BLUE,BLUE_END->{
                if(e.kind()==BLUE_END){GojoTrailClient.drop(orb);GojoGravityClient.drop(orb);GojoBlastClient.spawn(at,Math.max(0,e.size()),0x5AA8FF);return;}
                spark(at,Math.max(0.8,e.size()),Math.max(0.8,e.size())*0.62,0xB43A8CFF,0xFF12304F,GLOW,true,2,5,22,4.5);
                if(time%2==0)spark(at,Math.max(1.4,e.size())*1.8,1.1,0x66FFFFFF,0,GLOW,true,2,6,8,2.2);
            }
            case RED,RED_END->{
                if(e.kind()==RED_END){GojoBlastClient.spawn(at,Math.max(0,e.size()),0xFF5B3A);return;}
                spark(at,Math.max(0.6,e.size()),0.7,0xC0FF2A16,0xFF7A1206,GLOW,true,2,5,26,2.0);
            }
            case PURPLE->{
                spark(at,Math.max(1.2,e.size()),Math.max(1.2,e.size())*0.95,0xC0C08CFF,0xFF3E1C63,GALAXY,true,2,6,26,1.2);
                if(time%2==0)spark(at,Math.max(1.2,e.size())*1.6,0.32,0x77CFE4FF,0,GLOW,true,2,6,8,2.0);
            }
            // 对撞过程：苍球与赫球**绕着彼此旋转**收拢（用户要求"苍和赫为旋转对撞"），位置每刻由服务端给
            case PURPLE_GATHER->{
                double t=Math.max(0,Math.min(1,e.seed()/100.0));
                Vec3 side=sideOf(at);
                double ang=t*Math.PI*3.0;                                   // 收拢全程转 1.5 圈
                double rad=Math.max(0.18,e.size())*(1-t)+0.12;
                Vec3 base=side.scale(Math.cos(ang)*rad).add(0,Math.sin(ang)*rad,0);
                spark(at.add(base),1.5,0.95,0xB43A8CFF,0xFF12304F,GLOW,true,2,5,16,5.0);
                spark(at.subtract(base),0.9,0.6,0xC0FF2A16,0xFF7A1206,GLOW,true,2,5,16,2.0);
                if(t>=0.98)spark(at,2.2,2.1,0xC0C08CFF,0xFF3E1C63,GALAXY,true,3,8,30,1.2);
            }
            case PURPLE_FORM->spark(at,Math.max(1.5,e.size()),Math.max(1.5,e.size())*0.95,0xC0C08CFF,0xFF3E1C63,GALAXY,true,4,10,40,1.2);
            // ===== 茈·点按版的**独立特效组**（222/223/224）：小、亮、紧，跟对撞版那个大球完全分开 =====
            // 222 蓄力：手心一颗小紫球，size 由服务端从 0.20 逐步长到 1.00（一格大）；
            //     外圈再叠一层薄雾 + 一圈往内收的亮边，读起来"在聚能"而不是"突然出现"。
            case PURPLE_CHARGE->{
                double sz=Math.max(0.12,e.size());
                spark(at,sz*0.50,sz*0.62,0xD6C88CFF,0xFF3E1C63,GALAXY,true,2,3,36,1.1);
                spark(at,sz*0.85,sz*0.30,0x8FD8FFE8,0,GLOW,true,2,3,18,1.8);
                if(time%2==0)spark(at,sz*1.7,sz*0.16,0x66CFA8FF,0,GLOW,true,2,4,8,-1.2); // 向内收（orbit 取负）
            }
            // 223 飞行弹体：一圈小紫球（半径/颗粒都压到一格内），飞行中只滚一点点，不像大球那样糊一脸
            case PURPLE_TAP->{
                spark(at,0.34,0.46,0xC8C08CFF,0xFF3E1C63,GALAXY,true,2,4,26,1.0);
                spark(at,0.28,0.22,0xB8D8FFFF,0,GLOW,true,2,3,12,1.5);
            }
            // 224 出膛/命中/消散：小闪光（不用 PURPLE_BURST 那种 12 格的大爆）
            case PURPLE_TAP_BURST->{
                spark(at,Math.max(0.3,e.size())*0.75,Math.max(0.3,e.size())*0.55,0xB0C08CFF,0xFF3E1C63,GALAXY,true,3,7,34,1.2);
                GojoBlastClient.spawn(at,Math.max(0.8,e.size())*1.1,0xCCB15C);
            }
            case PURPLE_BURST->GojoBlastClient.spawn(at,Math.max(0,e.size()),0xCCB15C);
            case UNLIMITED->{
                keep("unlimited",PhotonFx.purple(at,Math.max(4,e.size()),60),60,false);
                PhotonFx.shockwave(at,0xE0C08CFF,e.size(),40);
                GojoBlastClient.spawn(at,Math.max(2,e.size()),0xE0C08C);
            }
            case TELEPORT->{PhotonFx.shockwave(at,0x99D8C8FF,1.8,14);PhotonFx.shockwave(e.direction(),0x99D8C8FF,1.8,14);}
            case DOMAIN_OPEN->{
                drop("domain");
                GojoVoidClient.open(e.owner(),at,e.direction(),e.size(),Math.max(20,e.life()));
            }
            case DOMAIN->{
                // 收起后的 3 秒内丢掉心跳：迟到的包不许把已经收起的领域再放回来
                if(GojoVoidClient.recentlyClosed())break;
                if(GojoVoidClient.active())GojoVoidClient.refresh(e.owner(),at,e.size(),40);
                else GojoVoidClient.open(e.owner(),at,e.direction(),e.size(),40); // 中途进服也能看到画布
            }
            case DOMAIN_CLOSE->{drop("domain");GojoVoidClient.close();}
            default->{}
        }
    }
    private static void spark(Vec3 at,double radius,double size,int color,int bloom,String texture,boolean additive,int duration,int life,double rate,double orbit){
        PhotonFx.spark(at,radius,size,color,bloom,texture,additive,duration,life,rate,orbit);
    }
    /** 拖尾 / 引力视界的键：**加害者 id + 弹体 id**。
     *  五条悟实体与玩家的弹体 id 各自从 1 起，只按弹体 id 会把两人的弹体并成同一条拖尾。 */
    private static long orbKey(Network.Effect e){return ((long)e.owner()<<32)^(e.seed()&0xFFFFFFFFL);}
}
