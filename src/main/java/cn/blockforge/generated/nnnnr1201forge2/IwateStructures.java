package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.util.RandomSource;

/** 将保留家具的建筑蓝图编为原版StructureTemplate，再按当前区块边界放置。 */
public final class IwateStructures {
    private static final class Capture {final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();final Map<BlockPos,CompoundTag> nbt=new HashMap<>();}
    private record Template(StructureTemplate structure,BlockPos minimum){}
    private static final ThreadLocal<Capture> CAPTURE=new ThreadLocal<>();
    private static final Map<String,Template> CACHE=new ConcurrentHashMap<>();
    private static final String[] VILLAGE={"farmhouse","warehouse","shrine","bus","school","clinic","station"};
    static boolean capturing(){return CAPTURE.get()!=null;}
    static boolean capture(int x,int y,int z,BlockState state){Capture c=CAPTURE.get();if(c==null)return false;c.blocks.put(new BlockPos(x,y,z),state);return true;}
    static boolean captureNbt(CompoundTag tag){Capture c=CAPTURE.get();if(c==null)return false;c.nbt.put(new BlockPos(tag.getInt("x"),tag.getInt("y"),tag.getInt("z")),tag.copy());return true;}
    private static ListTag ints(int... values){ListTag t=new ListTag();for(int n:values)t.add(IntTag.valueOf(n));return t;}
    private static Template template(ServerLevelAccessor level,String name){return CACHE.computeIfAbsent(name,n->{Capture c=new Capture();CAPTURE.set(c);try{IwateVillage.blueprint(n);}finally{CAPTURE.remove();}
        int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,maxY=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;
        for(BlockPos p:c.blocks.keySet()){minX=Math.min(minX,p.getX());minY=Math.min(minY,p.getY());minZ=Math.min(minZ,p.getZ());maxX=Math.max(maxX,p.getX());maxY=Math.max(maxY,p.getY());maxZ=Math.max(maxZ,p.getZ());}
        BlockPos min=new BlockPos(minX,minY,minZ);CompoundTag root=new CompoundTag();root.putInt("DataVersion",3465);root.put("size",ints(maxX-minX+1,maxY-minY+1,maxZ-minZ+1));root.put("entities",new ListTag());ListTag palette=new ListTag(),blocks=new ListTag();Map<BlockState,Integer> indices=new LinkedHashMap<>();
        for(var entry:c.blocks.entrySet()){BlockPos pos=entry.getKey().subtract(min);BlockState state=entry.getValue();int index=indices.computeIfAbsent(state,s->{palette.add(NbtUtils.writeBlockState(s));return indices.size();});CompoundTag block=new CompoundTag();block.put("pos",ints(pos.getX(),pos.getY(),pos.getZ()));block.putInt("state",index);CompoundTag data=c.nbt.get(entry.getKey());if(data!=null){data=data.copy();data.remove("x");data.remove("y");data.remove("z");data.remove("LootTableSeed");block.put("nbt",data);}blocks.add(block);}
        root.put("palette",palette);root.put("blocks",blocks);StructureTemplate structure=new StructureTemplate();structure.load(level.registryAccess().registryOrThrow(Registries.BLOCK).asLookup(),root);return new Template(structure,min);
    });}
    private static boolean intersects(ChunkAccess c,int x,int z,int width,int depth){return x<=c.getPos().getMaxBlockX()&&x+width>c.getPos().getMinBlockX()&&z<=c.getPos().getMaxBlockZ()&&z+depth>c.getPos().getMinBlockZ();}
    public static void place(ServerLevelAccessor level,ChunkAccess chunk){
        put(level,chunk,"home",IwateVillage.HOME_X,IwateVillage.HOME_Z,1300,true,0);
        int cellX=Math.floorDiv(chunk.getPos().getMinBlockX(),96),cellZ=Math.floorDiv(chunk.getPos().getMinBlockZ(),96);
        for(int cx=cellX-1;cx<=cellX;cx++)for(int cz=cellZ-1;cz<=cellZ;cz++){
            long hash=IwateTerrain.hash(cx,cz);if(Math.floorMod(hash,100)>=86)continue;
            int x=cx*96+12+(int)Math.floorMod(hash>>>9,19),z=cz*96+12+(int)Math.floorMod(hash>>>18,19);
            if(x<192&&x+55>-32&&z<88&&z+55>-88)continue;
            boolean forest=IwateTerrain.forest(x+20,z+20);String name=forest?"forest_cabin":VILLAGE[(int)Math.floorMod(hash>>>28,VILLAGE.length)];
            // 森林边界也不允许伸入学校、诊所等村区建筑。
            if(!forest)for(int dx=0;dx<=48;dx+=12)for(int dz=0;dz<=48;dz+=12)if(IwateTerrain.forest(x+dx,z+dz))name="forest_cabin";
            boolean wet=false;for(int dx=-4;dx<=54;dx++)for(int dz=-4;dz<=54;dz++)if(IwateTerrain.wetland(x+dx,z+dz))wet=true;
            if(wet)continue; // 河道与梯田优先，建筑不能把水面抹平。
            int ground=IwateVillage.height(x+20,z+20);put(level,chunk,name,x,z,ground,false,hash);
        }
    }
    private static void put(ServerLevelAccessor level,ChunkAccess c,String name,int x,int z,int ground,boolean home,long seed){
        int broad=home?128:55;if(!intersects(c,x-5,z-5,broad+10,broad+10))return;Template t=template(level,name);var size=t.structure.getSize();
        int minX=home?x:x+t.minimum.getX(),minZ=home?z:z+t.minimum.getZ();int base=ground+(t.minimum.getY()-1300);
        if(!intersects(c,minX,minZ,size.getX(),size.getZ()))return;
        // 模板内显式空气负责室内空间，未记录的位置保留原地形。
        // 禁止清空整个包围盒：院落、附属物会把包围盒拉得远大于房屋。
        BlockPos origin=new BlockPos(minX,base,minZ);StructurePlaceSettings settings=new StructurePlaceSettings().setIgnoreEntities(true).setKeepLiquids(false).setKnownShape(true).setBoundingBox(new BoundingBox(c.getPos().getMinBlockX(),c.getMinBuildHeight(),c.getPos().getMinBlockZ(),c.getPos().getMaxBlockX(),c.getMaxBuildHeight()-1,c.getPos().getMaxBlockZ()));
        t.structure.placeInWorld(level,origin,origin,settings,RandomSource.create(seed),2);
    }
    private IwateStructures(){}
}
