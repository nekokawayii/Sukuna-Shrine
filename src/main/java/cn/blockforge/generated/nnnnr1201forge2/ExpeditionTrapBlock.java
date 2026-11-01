package cn.blockforge.generated.nnnnr1201forge2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.effect.*;
public final class ExpeditionTrapBlock extends Block {
    public ExpeditionTrapBlock(){super(BlockBehaviour.Properties.copy(Blocks.CHISELED_STONE_BRICKS).strength(3).lightLevel(s->4));}
    @Override public void stepOn(Level l,BlockPos pos,BlockState s,Entity e){super.stepOn(l,pos,s,e);if(e instanceof ServerPlayer p&&!p.isCreative()&&!p.isSpectator()&&l.getGameTime()>p.getPersistentData().getLong("ExpeditionTrapNext")){p.getPersistentData().putLong("ExpeditionTrapNext",l.getGameTime()+80);p.hurt(l.damageSources().magic(),Math.max(8,p.getMaxHealth()*.25f));p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,3));p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,60));p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,p.getX(),p.getY()+1,p.getZ(),70,.8,1,.8,.1);}}
}
