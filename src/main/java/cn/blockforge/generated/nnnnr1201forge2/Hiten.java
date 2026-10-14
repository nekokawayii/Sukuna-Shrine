package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** 武器「飞天」：**伤害 20、攻击范围 15 格**。
 *
 *  <p>手持时**所有斩击速度变慢 70%**（连击速率 ×0.3，见 {@link #SLASH_SLOW}）；右键把
 *  **周围半径 10 格** 以及 **面前扇形 40 格** 内的敌人**直接吹到斩击上**——先被风拽到准星前方，
 *  再立刻吃一发范围 15 格的斩击。</p> */
public final class Hiten {
    /** 近战伤害（原值）。 */
    public static final double DAMAGE=20;
    /** 攻击范围（格）。 */
    public static final double RANGE=15;
    /** 手持时斩击速度倍率：变慢 70% → ×0.3。 */
    public static final double SLASH_SLOW=0.3;
    /** 右键：周围半径（格）与面前扇形射程（格）、半角 45°（合计 90°）。 */
    public static final double AURA_RADIUS=10,CONE_RANGE=40,CONE_HALF_ANGLE=Math.PI/4;
    /** 右键冷却（刻）。 */
    public static final int COOLDOWN=80;
    /** 敌人被拽到准星前方多远（格）。 */
    public static final double PULL_DISTANCE=6;
    /** 是否手持飞天（主手或副手）。 */
    public static boolean holding(net.minecraft.world.entity.LivingEntity p){
        return p.getMainHandItem().is(VoidContent.HITEN.get())||p.getOffhandItem().is(VoidContent.HITEN.get());
    }
    /** 右键：把周围 10 格 + 面前扇形 40 格的敌人吹到斩击上。 */
    public static boolean pull(ServerPlayer p){
        if(Curse.active(p,"HitenCooldown")){
            CastFeedback.tell(p,"飞天 · 风压冷却中（"+((Curse.data(p).getLong("HitenCooldown")-p.level().getGameTime()+19)/20)+" 秒）");
            return false;
        }
        ServerLevel level=p.serverLevel();
        Vec3 eye=p.getEyePosition(),look=p.getLookAngle().multiply(1,0,1);
        if(look.lengthSqr()<1e-4)look=new Vec3(0,0,1);
        look=look.normalize();
        List<LivingEntity> caught=new ArrayList<>();
        for(LivingEntity e:Skills.targets(level,p,eye,CONE_RANGE+2)){
            Vec3 rel=e.position().subtract(eye);
            double dist=rel.length();
            if(dist>CONE_RANGE)continue;
            boolean near=dist<=AURA_RADIUS;                       // 周围 10 格：全向
            boolean front=false;
            if(!near){                                            // 面前扇形 40 格
                Vec3 flat=new Vec3(rel.x,0,rel.z);
                if(flat.length()>0.6)front=flat.normalize().dot(look)>=Math.cos(CONE_HALF_ANGLE);
                else front=true;
            }
            if(!near&&!front)continue;
            if(distanceSq(p,e)>RANGE*RANGE&&!p.hasLineOfSight(e))continue;
            caught.add(e);
        }
        // 风的落点：准星前方 PULL_DISTANCE 格（就是斩击要打的地方）
        Vec3 landing=eye.add(look.scale(PULL_DISTANCE));
        for(LivingEntity e:caught){
            Vec3 to=landing.subtract(e.position());
            double d=to.length();
            if(d<0.5)continue;
            Vec3 dir=to.normalize();
            // "被风吹到斩击上"：直接给一个朝落点的强位移，并按距离补偿速度，做到几乎瞬移到位
            Vec3 move=dir.scale(Math.min(d,Math.max(2.2,d*0.55)));
            BossTargets.move(e,e.position().add(move));
            e.setDeltaMovement(dir.x*0.9,0.32,dir.z*0.9);
            e.hurtMarked=true;
            e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,30,3));
            level.sendParticles(ParticleTypes.CLOUD,e.getX(),e.getY()+e.getBbHeight()*0.5,e.getZ(),14,0.35,0.35,0.35,0.06);
        }
        // 风压视觉：沿扇形方向刮出去的一排云气 + 落点一圈
        for(int i=2;i<=CONE_RANGE;i+=2){
            Vec3 at=eye.add(look.scale(i));
            level.sendParticles(ParticleTypes.CLOUD,at.x,at.y,at.z,2,0.5,0.35,0.5,0.02);
        }
        for(int i=0;i<24;i++){
            double a=i*Math.PI*2/24;
            level.sendParticles(ParticleTypes.CLOUD,landing.x+Math.cos(a)*2.2,landing.y+0.15,landing.z+Math.sin(a)*2.2,1,0,0,0,0.01);
        }
        level.playSound(null,p.blockPosition(),SoundEvents.PHANTOM_FLAP,SoundSource.PLAYERS,1.6f,0.8f);
        level.playSound(null,p.blockPosition(),SoundEvents.ENDER_DRAGON_FLAP,SoundSource.PLAYERS,0.9f,1.4f);
        Curse.status(p,"HitenCooldown",COOLDOWN);
        // 风压**本身不造成任何伤害**：只把敌人搬到"斩击上"，伤害仍然由玩家随后的斩击（左键连击/术式）去打。
        p.displayClientMessage(Component.literal("飞天 · 风卷 "+caught.size()+" 个目标到斩击前（风压无伤害）")
            .setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0xBFE9FF)),true);
        return true;
    }
    private static double distanceSq(ServerPlayer p,LivingEntity e){return e.position().distanceToSqr(p.getEyePosition());}
    private Hiten(){}
}
