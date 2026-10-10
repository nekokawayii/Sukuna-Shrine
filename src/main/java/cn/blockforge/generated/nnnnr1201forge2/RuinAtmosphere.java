package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 自然生成与任务生成共用模板，范围记录保存在各自世界中。 */
public final class RuinAtmosphere {
    private static final ResourceKey<Structure> KEY=ResourceKey.create(Registries.STRUCTURE,new ResourceLocation("sukuna","jujutsu_ruins"));
    public static final class Sites extends SavedData {
        final List<BlockPos> origins=new ArrayList<>();
        static Sites load(CompoundTag tag){Sites s=new Sites();for(long n:tag.getLongArray("Origins"))s.origins.add(BlockPos.of(n));return s;}
        @Override public CompoundTag save(CompoundTag tag){tag.putLongArray("Origins",origins.stream().mapToLong(BlockPos::asLong).toArray());return tag;}
    }
    private static Sites sites(ServerLevel l){return l.getDataStorage().computeIfAbsent(Sites::load,Sites::new,"sukuna_ruins_r20");}
    public static boolean build(ServerPlayer p){
        if(p.level().dimension()!=Level.OVERWORLD){p.sendSystemMessage(Component.literal("请回到主世界生成高专遗迹。"));return false;}
        var level=p.serverLevel();var template=level.getStructureManager().get(new ResourceLocation("sukuna","jujutsu_ruins"));
        if(template.isEmpty()){p.sendSystemMessage(Component.literal("高专建筑资源未载入，未生成建筑。"));return false;}
        BlockPos base=null;
        // 只选择玩家未踏入的区块，避免清除住宅；地形起伏限制防止地基悬空。
        search:for(int r=128;r<=640;r+=96)for(int k=0;k<8;k++){
            int x=(p.blockPosition().getX()+(int)(Math.cos(k*Math.PI/4)*r))&~15,z=(p.blockPosition().getZ()+(int)(Math.sin(k*Math.PI/4)*r))&~15;
            // 先用生成器采样地势，不为不合适的候选区域加载整片区块。
            var generator=level.getChunkSource().getGenerator();int low=10000,high=-10000;
            for(int dx=0;dx<=48;dx+=24)for(int dz=0;dz<=64;dz+=32){int h=generator.getBaseHeight(x+dx,z+dz,net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,level,level.getChunkSource().randomState());low=Math.min(low,h);high=Math.max(high,h);}
            if(high-low>5||low<level.getSeaLevel())continue;
            int min=10000,max=-10000;boolean safe=true;
            for(int cx=x>>4;cx<=(x+48)>>4;cx++)for(int cz=z>>4;cz<=(z+64)>>4;cz++){var chunk=level.getChunk(cx,cz);if(chunk.getInhabitedTime()>0||!chunk.getBlockEntities().isEmpty())safe=false;}
            if(!safe)continue;
            for(int dx=0;dx<=48;dx+=8)for(int dz=0;dz<=64;dz+=8){int h=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR,x+dx,z+dz);min=Math.min(min,h);max=Math.max(max,h);if(!level.getFluidState(new BlockPos(x+dx,h,z+dz)).isEmpty())safe=false;}
            if(!safe||max-min>5||min<level.getMinBuildHeight()+12||max+18>=level.getMaxBuildHeight())continue;
            base=new BlockPos(x,max-11,z);break search;
        }
        if(base==null){p.sendSystemMessage(Component.literal("附近没有合适的未使用平地。走到平原后再用 /sukuna soul ruins 生成。"));return false;}
        if(!template.get().placeInWorld(level,base,base,new StructurePlaceSettings().setKeepLiquids(false),level.random,2))return false;
        Sites data=sites(level);data.origins.add(base);data.setDirty();
        var tag=Curse.data(p);tag.putBoolean("SoulRuinBuilt",true);tag.putInt("SoulRuinVersion",20);tag.putInt("SoulRuinX",base.getX()+24);tag.putInt("SoulRuinY",base.getY()+11);tag.putInt("SoulRuinZ",base.getZ()+4);
        p.sendSystemMessage(Component.literal("新版高专遗迹校门："+(base.getX()+24)+", "+(base.getY()+11)+", "+(base.getZ()+4)+"。穿过操场进入主楼，沿走廊尽头石阶下到忌库。"));return true;
    }
    public static BlockPos origin(ServerPlayer p){
        if(p.level().dimension()!=Level.OVERWORLD)return null;
        for(BlockPos b:sites(p.serverLevel()).origins)if(p.getX()>=b.getX()&&p.getX()<b.getX()+49&&p.getZ()>=b.getZ()&&p.getZ()<b.getZ()+65&&p.getY()>=b.getY()&&p.getY()<b.getY()+36)return b;
        StructureStart start=p.serverLevel().structureManager().getStructureWithPieceAt(p.blockPosition(),KEY);
        if(start.isValid()){BoundingBox b=start.getBoundingBox();return new BlockPos(b.minX(),b.minY(),b.minZ());}return null;
    }
    private static int rotation(ServerPlayer p){
        StructureStart start=p.serverLevel().structureManager().getStructureWithPieceAt(p.blockPosition(),KEY);
        if(start.isValid()&&!start.getPieces().isEmpty()&&start.getPieces().get(0) instanceof PoolElementStructurePiece piece)return switch(piece.getRotation()){case CLOCKWISE_90->1;case CLOCKWISE_180->2;case COUNTERCLOCKWISE_90->3;default->0;};return 0;
    }
    public static void echo(ServerPlayer p){if(origin(p)!=null&&p.level().getGameTime()>p.getPersistentData().getLong("RuinEchoAfter")){p.getPersistentData().putLong("RuinEchoAfter",p.level().getGameTime()+30);p.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE,SoundSource.AMBIENT,0.65f,0.55f);p.displayClientMessage(Component.literal("墙壁里的灵魂，记住了这一斩。"),true);}}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||p.tickCount%20!=0)return;
        BlockPos base=origin(p);if(base==null){p.getPersistentData().remove("InsideSoulRuin");return;}
        boolean enter=!p.getPersistentData().getBoolean("InsideSoulRuin");p.getPersistentData().putBoolean("InsideSoulRuin",true);
        Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),new Network.Effect(97,p.getId(),Vec3.atLowerCornerOf(base),new Vec3(rotation(p),0,0),1,45,enter?1:0));
        if(enter)p.playNotifySound(SoundEvents.BELL_RESONATE,SoundSource.AMBIENT,0.7f,0.5f);
        if(p.tickCount%160==0){p.playNotifySound(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(),SoundSource.AMBIENT,0.35f,0.65f);p.playNotifySound(SoundEvents.EVOKER_PREPARE_SUMMON,SoundSource.AMBIENT,0.14f,0.5f);}
        if(p.tickCount%240==0)p.playNotifySound(SoundEvents.SKELETON_AMBIENT,SoundSource.AMBIENT,0.2f,0.55f);
        if(p.tickCount%100==0)p.playNotifySound(SoundEvents.CHAIN_STEP,SoundSource.AMBIENT,0.12f,0.6f);
        if(p.getY()<base.getY()+8&&!p.getPersistentData().getBoolean("RuinApology")){p.getPersistentData().putBoolean("RuinApology",true);p.sendSystemMessage(Component.literal("虎杖残留的声音：对不起……"));p.playNotifySound(SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM,SoundSource.AMBIENT,0.25f,0.5f);}
    }
}
