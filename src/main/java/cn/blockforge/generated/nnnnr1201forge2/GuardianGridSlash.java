package cn.blockforge.generated.nnnnr1201forge2;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 网格显示期间持续检查碰撞；同一次施放只对每个目标成功结算一次。 */
final class GuardianGridSlash {
    static final int LIFETIME=16;
    static final int FIRST_LINE_HEIGHT=1,LAST_LINE_HEIGHT=7,LINE_HEIGHT_STEP=3;
    private final Set<UUID> hitTargets=new HashSet<>();
    private AABB bounds;
    private int remaining;

    void start(Vec3 center,double radius){start(center,radius,LIFETIME);}
    void start(Vec3 center,double radius,int duration){
        clear();
        if(!Double.isFinite(radius)||radius<=0)return;
        double r=Math.min(512,radius);
        // 与画面的方形网格一致；按实体碰撞箱而不是脚下坐标判定边缘命中。
        bounds=new AABB(center.x-r,center.y+FIRST_LINE_HEIGHT-1,center.z-r,
                        center.x+r,center.y+LAST_LINE_HEIGHT+1,center.z+r);
        remaining=Math.max(1,Math.min(200,duration));
    }

    void tick(GuardianEntity caster,Predicate<LivingEntity> damage){
        if(remaining<=0)return;
        if(caster.level().isClientSide||!caster.isAlive()||caster.isRemoved()||caster.friendly()){
            clear();return;
        }
        for(LivingEntity target:caster.level().getEntitiesOfClass(LivingEntity.class,bounds,
                e->BossTargets.hostile(caster,e)&&!hitTargets.contains(e.getUUID()))){
            // 尊重正常伤害事件；被其他模组取消时不强制扣血，也不提前记为命中。
            if(damage.test(target))hitTargets.add(target.getUUID());
        }
        if(--remaining==0)clear();
    }

    void clear(){bounds=null;remaining=0;hitTargets.clear();}
}
