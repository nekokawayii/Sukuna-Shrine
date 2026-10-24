package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** 实际扣血以后才发打击包；刀面方向由命中的那一段带入。 */
public final class SlashImpact {
    public static final int FX=261;
    private record Cut(Vec3 normal,long tick){}
    private static final Map<LivingEntity,Cut> planes=new WeakHashMap<>();
    public static void plane(LivingEntity target,Vec3 edge,Vec3 travel){
        Vec3 n=edge.cross(travel).normalize();
        if(n.lengthSqr()>.1)planes.put(target,new Cut(n,target.level().getGameTime()));
    }
    public static void after(ServerPlayer caster,LivingEntity target,String type,float before,boolean accepted){
        Cut cut=planes.remove(target);
        int index=AttackTypes.index(type);
        boolean slash=index>=0&&AttackTypes.SLASH.equals(AttackTypes.family(index));
        if(!accepted||before<=target.getHealth()+target.getAbsorptionAmount()||(!slash&&cut==null))return;
        Vec3 normal=cut!=null&&cut.tick==target.level().getGameTime()?cut.normal:new Vec3(0,1,0);
        boolean killed=!target.isAlive();
        Network.send(caster.serverLevel(),new Network.Effect(FX,target.getId(),target.position(),normal,killed?1:0,killed?100:12,caster.getId()));
    }
    private SlashImpact(){}
}
