package character.domain;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 收回前先疏散，落点不依赖即将消失的地板；按完整碰撞箱检查而非仅检查脚和头。 */
@Mod.EventBusSubscriber(modid="sukuna")
public final class DomainSafety {
    private record Zone(ServerLevel level,AABB box) {}
    private static final Map<UUID,Zone> ZONES=new HashMap<>();
    public static void watch(ServerLevel level,UUID owner,Vec3 center,double radius){
        ZONES.put(owner,new Zone(level,new AABB(center,center).inflate(radius+1)));
    }
    public static void release(ServerLevel level,UUID owner,AABB fallback){
        Zone zone=ZONES.remove(owner);
        AABB area=zone!=null&&zone.level==level?(fallback==null?zone.box:zone.box.minmax(fallback)):fallback;
        if(area!=null)evacuate(level,area);
    }
    public static void evacuate(ServerLevel level,AABB area){
        var occupants=new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,area.inflate(1),e->e.isAlive()&&!e.isSpectator()));
        // 大生物先占位，小生物随后寻找其它落点，避免所有人叠在一起。
        occupants.sort(Comparator.comparingDouble((LivingEntity e)->e.getBbWidth()).reversed());
        for(var entity:occupants){
            Vec3 safe=findExit(level,entity,area);
            if(safe==null)safe=emergencyPlatform(level,entity,area);
            if(safe==null)safe=carveRefuge(level,entity,area);
            if(safe!=null)move(entity,safe);
            // 保存短期复查标记；断线、卸载、重启后仍检查一次，避免恢复队列晚于传送。
            entity.getPersistentData().putLong("DomainSafetyUntil",level.getGameTime()+200);
        }
    }
    private static boolean dangerous(BlockState s){
        return s.is(Blocks.LAVA)||s.is(Blocks.FIRE)||s.is(Blocks.SOUL_FIRE)||s.is(Blocks.MAGMA_BLOCK)
            ||s.is(Blocks.CACTUS)||s.is(Blocks.CAMPFIRE)||s.is(Blocks.SOUL_CAMPFIRE)
            ||s.is(Blocks.POWDER_SNOW)||s.is(Blocks.SWEET_BERRY_BUSH)||s.is(Blocks.WITHER_ROSE);
    }
    private static boolean aquatic(LivingEntity e){return e instanceof net.minecraft.world.entity.animal.WaterAnimal;}
    private static boolean safeWater(ServerLevel level,LivingEntity e,Vec3 at){
        AABB box=e.getBoundingBox().move(at.subtract(e.position())).deflate(.001);
        if(!level.getWorldBorder().isWithinBounds(box)||box.minY<level.getMinBuildHeight()+1||box.maxY>=level.getMaxBuildHeight())return false;
        for(BlockPos p:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))
            if(!level.hasChunkAt(p)||DomainTerrain.changing(level,p)||!level.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER))return false;
        return level.noCollision(e,box);
    }
    public static boolean safe(ServerLevel level,LivingEntity e,Vec3 feet){return aquatic(e)?safeWater(level,e,feet):safe(level,e,feet,true);}
    public static boolean safeForMovement(ServerLevel level,LivingEntity e,Vec3 feet){return safe(level,e,feet,false);}
    private static boolean safe(ServerLevel level,LivingEntity e,Vec3 feet,boolean stable){
        AABB box=e.getBoundingBox().move(feet.subtract(e.position())).deflate(.001);
        if(box.minY<=level.getMinBuildHeight()||box.maxY>=level.getMaxBuildHeight()
            ||!level.getWorldBorder().isWithinBounds(box))return false;
        BlockPos support=BlockPos.containing(feet.x,feet.y-.05,feet.z);
        if(!level.hasChunkAt(support)||!level.getBlockState(support).isFaceSturdy(level,support,Direction.UP))return false;
        for(BlockPos pos:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY-.05,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))){
            if(!level.hasChunkAt(pos)||stable&&DomainTerrain.changing(level,pos))return false;
            BlockState s=level.getBlockState(pos);
            if(dangerous(s)||!s.getFluidState().isEmpty())return false;
        }
        if(!level.noCollision(e,box))return false;
        // 覆盖整个脚底，杜绝宽体咒灵只靠一个方块悬在悬崖边。
        int y=(int)Math.floor(feet.y-.05);
        for(int x=(int)Math.floor(box.minX);x<=Math.floor(box.maxX);x++)for(int z=(int)Math.floor(box.minZ);z<=Math.floor(box.maxZ);z++){
            BlockPos p=new BlockPos(x,y,z);BlockState s=level.getBlockState(p);
            if(stable&&DomainTerrain.changing(level,p)||dangerous(s)||!s.isFaceSturdy(level,p,Direction.UP))return false;
        }
        return true;
    }
    private static Vec3 findExit(ServerLevel level,LivingEntity entity,AABB area){
        Vec3 center=area.getCenter();double base=Math.atan2(entity.getZ()-center.z,entity.getX()-center.x);
        double rx=area.getXsize()/2+entity.getBbWidth()+3,rz=area.getZsize()/2+entity.getBbWidth()+3;
        for(int ring=0;ring<5;ring++)for(int step=0;step<48;step++){
            int side=step%2==0?step/2:-(step+1)/2;double angle=base+side*Math.PI/24;
            double cos=Math.cos(angle),sin=Math.sin(angle),edge=1/Math.max(Math.abs(cos)/rx,Math.abs(sin)/rz);
            int x=(int)Math.floor(center.x+cos*(edge+ring*4)),z=(int)Math.floor(center.z+sin*(edge+ring*4));
            BlockPos column=new BlockPos(x,(int)entity.getY(),z);if(!level.hasChunkAt(column))continue;
            for(int dy=0;dy<=16;dy++)for(int sign:dy==0?new int[]{1}:new int[]{1,-1}){
                Vec3 at=new Vec3(x+.5,Math.floor(entity.getY())+dy*sign,z+.5);
                if(safe(level,entity,at))return at;
            }
            int surface=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            Vec3 at=new Vec3(x+.5,aquatic(entity)?surface-Math.ceil(entity.getBbHeight())-1:surface,z+.5);if(safe(level,entity,at))return at;
        }
        return null;
    }
    private static Vec3 emergencyPlatform(ServerLevel level,LivingEntity entity,AABB area){
        // 极端虚空/洞窟兜底：只在空处铺脚台，不挖原建筑，不覆盖容器或基岩。
        int half=Math.max(2,(int)Math.ceil(entity.getBbWidth()/2)+1);
        int top=Math.min(level.getMaxBuildHeight()-(int)Math.ceil(entity.getBbHeight())-2,(int)Math.ceil(area.maxY)+4);
        for(int radius=0;radius<24;radius+=half*2+2)for(int i=0;i<16;i++){
            double a=i*Math.PI/8;int x=(int)area.getCenter().x+(int)(Math.cos(a)*radius),z=(int)area.getCenter().z+(int)(Math.sin(a)*radius);
            for(int y=top;y>level.getMinBuildHeight()+2;y-=4){
                Vec3 at=new Vec3(x+.5,y,z+.5);AABB box=entity.getBoundingBox().move(at.subtract(entity.position())).inflate(.2);
                if(area.intersects(box)||!level.getWorldBorder().isWithinBounds(box))continue;
                boolean clear=true;
                for(BlockPos p:BlockPos.betweenClosed(new BlockPos(x-half,y-1,z-half),new BlockPos(x+half,y+(int)Math.ceil(entity.getBbHeight()),z+half))){
                    if(!level.hasChunkAt(p)||DomainTerrain.changing(level,p)||!level.getBlockState(p).isAir()){clear=false;break;}
                }
                if(!clear)continue;
                for(int dx=-half;dx<=half;dx++)for(int dz=-half;dz<=half;dz++)level.setBlock(new BlockPos(x+dx,y-1,z+dz),Blocks.STONE.defaultBlockState(),3);
                if(aquatic(entity))fillPool(level,x,y,z,half,(int)Math.ceil(entity.getBbHeight())+1);
                if(safe(level,entity,at))return at;
            }
        }
        return null;
    }
    private static Vec3 carveRefuge(ServerLevel level,LivingEntity entity,AABB area){
        // 四周全是实心岩层时的最后兜底：领域外开一个最小安全洞，绝不碰容器、基岩和其它领域。
        int half=Math.max(1,(int)Math.ceil(entity.getBbWidth()/2)+(aquatic(entity)?1:0)),height=(int)Math.ceil(entity.getBbHeight())+1;
        Vec3 center=area.getCenter();double radius=Math.max(area.getXsize(),area.getZsize())/2+half+4;
        for(int ring=0;ring<4;ring++)for(int i=0;i<32;i++){
            double a=i*Math.PI/16;int x=(int)Math.floor(center.x+Math.cos(a)*(radius+ring*4)),z=(int)Math.floor(center.z+Math.sin(a)*(radius+ring*4));
            int y=Math.max(level.getMinBuildHeight()+2,Math.min(level.getMaxBuildHeight()-height-2,(int)entity.getY()));
            Vec3 at=new Vec3(x+.5,y,z+.5);AABB box=new AABB(x-half,y-1,z-half,x+half+1,y+height+1,z+half+1);
            if(area.intersects(box)||!level.getWorldBorder().isWithinBounds(box)||!level.getEntities(entity,box).isEmpty())continue;
            boolean allowed=true;
            for(BlockPos p:BlockPos.betweenClosed(new BlockPos(x-half,y-1,z-half),new BlockPos(x+half,y+height,z+half))){
                if(!level.hasChunkAt(p)||DomainTerrain.changing(level,p)){allowed=false;break;}
                var state=level.getBlockState(p);if(state.hasBlockEntity()||state.getDestroySpeed(level,p)<0){allowed=false;break;}
            }
            if(!allowed)continue;
            for(BlockPos p:BlockPos.betweenClosed(new BlockPos(x-half,y-1,z-half),new BlockPos(x+half,y+height,z+half)))level.setBlock(p,p.getY()==y-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
            if(aquatic(entity))fillPool(level,x,y,z,half,height-1);
            if(safe(level,entity,at))return at;
        }
        return null;
    }
    private static void fillPool(ServerLevel level,int x,int y,int z,int half,int height){
        for(int dy=0;dy<height;dy++)for(int dx=-half;dx<=half;dx++)for(int dz=-half;dz<=half;dz++){
            boolean wall=Math.abs(dx)==half||Math.abs(dz)==half;
            level.setBlock(new BlockPos(x+dx,y+dy,z+dz),(wall?Blocks.STONE:Blocks.WATER).defaultBlockState(),3);
        }
    }
    private static void move(LivingEntity entity,Vec3 at){
        entity.stopRiding();entity.ejectPassengers();
        if(entity instanceof Mob mob)mob.getNavigation().stop();
        entity.teleportTo(at.x,at.y,at.z);entity.setDeltaMovement(Vec3.ZERO);entity.fallDistance=0;entity.hurtMarked=true;
    }
    @SubscribeEvent public static void recover(LivingEvent.LivingTickEvent event){
        var e=event.getEntity();if(!(e.level() instanceof ServerLevel level)||e.tickCount%10!=0)return;
        var data=e.getPersistentData();
        if(data.contains("DomainSafetyInside")){
            var tag=data.getCompound("DomainSafetyInside");
            if(tag.hasUUID("Owner")&&!ZONES.containsKey(tag.getUUID("Owner"))){
                AABB old=new AABB(tag.getDouble("MinX"),tag.getDouble("MinY"),tag.getDouble("MinZ"),tag.getDouble("MaxX"),tag.getDouble("MaxY"),tag.getDouble("MaxZ"));
                if(old.intersects(e.getBoundingBox())){
                    Vec3 at=findExit(level,e,old);if(at==null)at=emergencyPlatform(level,e,old);if(at==null)at=carveRefuge(level,e,old);if(at!=null)move(e,at);
                    data.putLong("DomainSafetyUntil",level.getGameTime()+200);
                }
                data.remove("DomainSafetyInside");
            }
        }
        boolean inside=false;
        for(var entry:ZONES.entrySet())if(entry.getValue().level==level&&entry.getValue().box.intersects(e.getBoundingBox())){
            inside=true;var tag=data.getCompound("DomainSafetyInside");
            if(!tag.hasUUID("Owner")||!tag.getUUID("Owner").equals(entry.getKey())){
                var box=entry.getValue().box;tag.putUUID("Owner",entry.getKey());tag.putDouble("MinX",box.minX);tag.putDouble("MinY",box.minY);tag.putDouble("MinZ",box.minZ);tag.putDouble("MaxX",box.maxX);tag.putDouble("MaxY",box.maxY);tag.putDouble("MaxZ",box.maxZ);data.put("DomainSafetyInside",tag);
            }
            break;
        }
        if(!inside)data.remove("DomainSafetyInside");
        if(!data.contains("DomainSafetyUntil"))return;
        if(!level.noCollision(e)||e.getY()<level.getMinBuildHeight()+1){
            AABB area=e.getBoundingBox().inflate(2);Vec3 at=findExit(level,e,area);if(at==null)at=emergencyPlatform(level,e,area);if(at==null)at=carveRefuge(level,e,area);if(at!=null)move(e,at);
        }
        if(level.getGameTime()>=data.getLong("DomainSafetyUntil"))data.remove("DomainSafetyUntil");
    }
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent event){ZONES.clear();}
    private DomainSafety(){}
}
