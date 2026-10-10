package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** 无限指几何判定不截长度：只处理已加载实体，绝不生成无限坐标或强制加载区块。 */
public final class SlashReach {
    public static final float INFINITE=-1;
    public static Vec3 axis(Vec3 direction,long angle){
        Vec3 side=new Vec3(-direction.z,0,direction.x).normalize();if(side.lengthSqr()<0.01)side=new Vec3(1,0,0);
        double a=Math.toRadians(angle);return side.scale(Math.cos(a)).add(direction.cross(side).normalize().scale(Math.sin(a))).normalize();
    }
    public static List<LivingEntity> targets(ServerPlayer p,LivingEntity primary,Vec3 point,Vec3 direction,long angle){
        Vec3 axis=axis(direction,angle);List<LivingEntity> result=new ArrayList<>();
        for(var entity:p.serverLevel().getAllEntities())if(entity instanceof LivingEntity e&&CombatSettings.canHit(p,e)){
            Vec3 center=e.getBoundingBox().getCenter();double reach=center.distanceTo(point)+e.getBbHeight()+e.getBbWidth()+2;
            if(e==primary||e.getBoundingBox().inflate(0.35).clip(point.subtract(axis.scale(reach)),point.add(axis.scale(reach))).isPresent())result.add(e);
        }
        return result;
    }
    private SlashReach(){}
}
