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
    public record Domain(int ownerId,Vec3 center,double radius,long end) {}
    private static final List<Domain> DOMAINS=new ArrayList<>();
    public static List<Domain> domains(){return DOMAINS;}
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
                if(e.direction().lengthSqr()<1e-4){
                    // 脚下火柱：从地面竖直窜起 10 格（粗芯 + 外焰 + 分段爆点）
                    Vec3 top=pos.add(0,10,0);
                    PhotonFx.beam(pos,top,1.15,10);
                    PhotonFx.beam(pos,top,2.0,6);
                    for(int i=0;i<5;i++){
                        double h=i*2.0;
                        PhotonFx.spark(pos.add(0,h,0),1.5+i*0.15,1.1,LAVA,FIRE_BLOOM,GLOW,true,3,9,28,0.35);
                        PhotonFx.ring(pos.add(0,h,0),1.3+i*0.12,0.35,FIRE,FIRE_BLOOM,12,4,4);
                    }
                    PhotonFx.spark(pos.add(0,10,0),2.2,1.4,FIRE,FIRE_BLOOM,GLOW,true,4,12,26,0.5);
                }else{
                    Vec3 dir=e.direction().normalize();
                    Vec3 end=pos.add(dir.scale(1.6));
                    PhotonFx.beam(pos,end,0.55,5);
                    PhotonFx.spark(pos,1.2,0.8,LAVA,FIRE_BLOOM,GLOW,true,2,7,24,0.4);
                }
            }
            case JogoFx.DOMAIN -> {
                double radius=Math.max(6,e.size());
                int life=Math.max(40,e.life());
                DOMAINS.add(new Domain(e.owner(),pos,radius,Minecraft.getInstance().level.getGameTime()+life));
            }
            case JogoFx.DOMAIN_END -> DOMAINS.removeIf(d->d.ownerId()==e.owner());
            default -> {}
        }
    }

    /** 每刻：岩浆泡破裂 + 上升火山灰，制造"地面在沸腾、天空被灰遮住"的观感。 */
    public static void tick() {
        var mc=Minecraft.getInstance();
        if(mc.level==null||DOMAINS.isEmpty())return;
        long now=mc.level.getGameTime();
        DOMAINS.removeIf(d->now>=d.end());
        // 地面表现全部交给真实方块（见 JogoTerrain），这里不再生成任何地面粒子
    }
    public static void clear(){DOMAINS.clear();}
    private JogoFxClient() {}
}
