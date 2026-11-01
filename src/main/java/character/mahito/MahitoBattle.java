package character.mahito;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** 无为转变战斗入口；结界只维持咒力，不再自动脉冲扣血。 */
public final class MahitoBattle {
    public static boolean strike(ServerPlayer caster){return MahitoCombat.strike(caster);}
    public static void domainTick(ServerPlayer caster,Vec3 center,double radius){
        if(caster.level().getGameTime()%20==0&&!cn.blockforge.generated.nnnnr1201forge2.Curse.spend(caster,20))character.domainking.SelfEmbryoDomain.close(caster,"咒力不足");
    }
    private MahitoBattle(){}
}
