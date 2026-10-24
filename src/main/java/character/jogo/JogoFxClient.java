package character.jogo;

import cn.blockforge.generated.nnnnr1201forge2.Network;
import cn.blockforge.generated.nnnnr1201forge2.PhotonFx;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 漏壶的 PhotonFx 表现层：火焰只用模组自己的加法光效，不用原版粒子。 */
public final class JogoFxClient {
    private static final String GLOW="textures/effect/glow.png", STREAK="textures/effect/streak.png", RING="textures/effect/ring.png";
    private static final int FIRE=0xFFFF7A18, FIRE_BLOOM=0xFFFF3A00, LAVA=0xFFFFB024, EMBER=0xFFFFD08A, ASH=0xFF2A1A18;
    private record Domain(Vec3 center,double radius,long end) {}
    private static final List<Domain> DOMAINS=new ArrayList<>();
    private static final Random RANDOM=new Random();

    public static void receive(Network.Effect e) {
        if(!PhotonFx.ready())return;
        Vec3 pos=e.position();
        switch(e.kind()) {
            case JogoFx.BUG -> PhotonFx.spark(pos,0.55,0.42,FIRE,FIRE_BLOOM,GLOW,true,1,6,26,0.3);
            case JogoFx.BUG_BURST -> {
                PhotonFx.spark(pos,2.6,1.6,LAVA,FIRE_BLOOM,GLOW,true,2,8,44,0.8);
                PhotonFx.ring(pos,3.0,0.9,FIRE,FIRE_BLOOM,26,2,8);
                PhotonFx.shockwave(pos,FIRE_BLOOM,3.0,10);
            }
            case JogoFx.METEOR_TRAIL -> {
                PhotonFx.spark(pos,1.5,1.0,FIRE,FIRE_BLOOM,GLOW,true,2,7,30,0.5);
                PhotonFx.spark(pos.add(0,1.2,0),0.8,0.5,ASH,0,GLOW,false,3,12,14,0.2);
            }
            case JogoFx.METEOR_IMPACT -> {
                PhotonFx.spark(pos,5.0,3.0,LAVA,FIRE_BLOOM,GLOW,true,3,10,60,1.0);
                PhotonFx.ring(pos,5.2,1.4,FIRE,FIRE_BLOOM,34,3,10);
                PhotonFx.shockwave(pos,FIRE_BLOOM,5.0,14);
                for(int i=0;i<10;i++){
                    double a=RANDOM.nextDouble()*Math.PI*2;
                    PhotonFx.spark(pos.add(Math.cos(a)*RANDOM.nextDouble()*3.5,0.6,Math.sin(a)*RANDOM.nextDouble()*3.5),1.1,0.7,EMBER,0,GLOW,true,2,12,14,0.3);
                }
            }
            case JogoFx.PILLAR -> {
                Vec3 dir=e.direction().lengthSqr()<1e-4?new Vec3(0,0,1):e.direction().normalize();
                Vec3 end=pos.add(dir.scale(1.6));
                PhotonFx.beam(pos,end,0.55,5);
                PhotonFx.spark(pos,1.2,0.8,LAVA,FIRE_BLOOM,GLOW,true,2,7,24,0.4);
            }
            case JogoFx.DOMAIN -> {
                double radius=Math.max(6,e.size());
                int life=Math.max(40,e.life());
                // 半径 100 时粒子会被"稀疏"，改用：稀疏球壳 + 分层环墙 + 地面岩浆环，保证边界一眼可见
                PhotonFx.domain(pos,Math.min(radius,60),life);                                     // 主体暗壳（按 60 格上限，避免单颗粒子过大）
                for(int i=1;i<=6;i++){                                                              // 六层环墙：0.2R~1.0R 高度
                    double h=radius*0.18*i;
                    PhotonFx.ring(pos.add(0,h,0),radius,3.0,FIRE,FIRE_BLOOM,radius*1.4,life,16);
                }
                PhotonFx.ring(pos.add(0,0.15,0),radius,4.0,FIRE,FIRE_BLOOM,radius*2.0,life,16);     // 地面暗红岩壁
                PhotonFx.ring(pos.add(0,0.05,0),radius*0.82,2.0,LAVA,FIRE_BLOOM,radius*1.4,life,12);// 地面岩浆环
                DOMAINS.add(new Domain(pos,radius,Minecraft.getInstance().level.getGameTime()+life));
            }
            default -> {}
        }
    }

    /** 每刻：岩浆泡破裂 + 上升火山灰，制造"地面在沸腾、天空被灰遮住"的观感。 */
    public static void tick() {
        var mc=Minecraft.getInstance();
        if(mc.level==null||DOMAINS.isEmpty())return;
        long now=mc.level.getGameTime();
        DOMAINS.removeIf(d->now>=d.end());
        for(Domain d:DOMAINS){
            if(now%4==0){
                for(int i=0;i<3;i++){
                    double a=RANDOM.nextDouble()*Math.PI*2,r=Math.sqrt(RANDOM.nextDouble())*d.radius()*0.92;
                    Vec3 at=d.center().add(Math.cos(a)*r,0.2,Math.sin(a)*r);
                    PhotonFx.spark(at,0.9,0.6,RANDOM.nextBoolean()?LAVA:EMBER,FIRE_BLOOM,GLOW,true,2,10,16,0.25);
                }
            }
            if(now%10==0)PhotonFx.spark(d.center().add((RANDOM.nextDouble()-.5)*d.radius()*1.6,d.radius()*0.9,(RANDOM.nextDouble()-.5)*d.radius()*1.6),
                1.6,1.0,ASH,0,STREAK,false,8,40,10,0.1);
            if(now%6==0)PhotonFx.ring(d.center().add(0,0.1,0),d.radius()*0.9,0.7,FIRE,FIRE_BLOOM,18,2,6);
        }
    }
    public static void clear(){DOMAINS.clear();}
    private JogoFxClient() {}
}
