package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 主世界地表低概率生成：每位生存玩家每30秒一次20%尝试，最多生成一只。 */
public final class CurseSpawning {
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||player.tickCount%600!=0||player.isCreative()||player.isSpectator())return;
        ServerLevel level=player.serverLevel();
        if(!level.dimension().equals(Level.OVERWORLD)||level.getDifficulty()==Difficulty.PEACEFUL||!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)||level.random.nextInt(5)!=0)return;
        if(level.getEntitiesOfClass(LowCurseEntity.class,player.getBoundingBox().inflate(96),e->e.isAlive()&&e.summoner()==null).size()>=4)return;
        int index=CurseCatalog.choose(level.random,false);
        for(int tries=0;tries<8;tries++){
            double angle=level.random.nextDouble()*Math.PI*2;int radius=32+level.random.nextInt(33);
            BlockPos at=player.blockPosition().offset((int)(Math.cos(angle)*radius),0,(int)(Math.sin(angle)*radius));
            if(!level.hasChunkAt(at))continue;
            at=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,at);
            if(!level.getWorldBorder().isWithinBounds(at)||!level.canSeeSky(at)||!level.getFluidState(at).isEmpty()||!level.getBlockState(at.below()).isSolidRender(level,at.below()))continue;
            boolean nearPlayer=false;for(var other:level.players())if(!other.isSpectator()&&other.distanceToSqr(at.getX()+.5,at.getY(),at.getZ()+.5)<24*24){nearPlayer=true;break;}if(nearPlayer)continue;
            LowCurseEntity curse=CurseRoster.create(level,index);if(curse==null)return;
            curse.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,level.random.nextFloat()*360,0);
            if(!level.noCollision(curse)||!level.getEntities(curse,curse.getBoundingBox()).isEmpty())continue;
            curse.getPersistentData().putBoolean("ExpeditionWild",true);
            curse.getPersistentData().putLong("ExpeditionDespawnAt",level.getGameTime()+6000);
            level.addFreshEntity(curse);return;
        }
    }
    private CurseSpawning(){}
}
