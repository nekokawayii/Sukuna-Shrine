package cn.blockforge.generated.nnnnr1201forge2;

import java.util.Optional;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** 显式的记录编解码器保持扁平JSON；直接xmap验证型Codec会被结构分派当成value字段读取。 */
public final class SukunaTempleStructure extends Structure {
    public static final Codec<SukunaTempleStructure> CODEC=RecordCodecBuilder.create(i->i.group(
        settingsCodec(i),
        StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s->s.pool),
        ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s->s.jigsawName),
        Codec.intRange(0,7).fieldOf("size").forGetter(s->s.size),
        HeightProvider.CODEC.fieldOf("start_height").forGetter(s->s.height),
        Codec.BOOL.fieldOf("use_expansion_hack").forGetter(s->s.expansion),
        Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s->s.heightmap),
        Codec.intRange(1,80).fieldOf("max_distance_from_center").forGetter(s->s.distance)
    ).apply(i,SukunaTempleStructure::new));
    public static final DeferredRegister<StructureType<?>> TYPES=DeferredRegister.create(Registries.STRUCTURE_TYPE,"sukuna");
    public static final RegistryObject<StructureType<SukunaTempleStructure>> TYPE=TYPES.register("overworld_temple",()->()->CODEC);
    private static volatile ChunkGenerator overworldGenerator;
    private final Holder<StructureTemplatePool> pool;
    private final Optional<ResourceLocation> jigsawName;
    private final int size,distance;
    private final HeightProvider height;
    private final boolean expansion;
    private final Optional<Heightmap.Types> heightmap;
    private final JigsawStructure template;

    private SukunaTempleStructure(StructureSettings settings,Holder<StructureTemplatePool> pool,Optional<ResourceLocation> jigsawName,int size,HeightProvider height,boolean expansion,Optional<Heightmap.Types> heightmap,int distance){
        super(settings);this.pool=pool;this.jigsawName=jigsawName;this.size=size;this.height=height;this.expansion=expansion;this.heightmap=heightmap;this.distance=distance;
        template=new JigsawStructure(settings,pool,jigsawName,size,height,expansion,heightmap,distance);
    }
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context){
        if(context.chunkGenerator()!=overworldGenerator)return Optional.empty();
        return template.findGenerationPoint(context);
    }
    @Override public StructureType<?> type(){return TYPE.get();}
    @SubscribeEvent public static void load(LevelEvent.Load event){
        if(event.getLevel() instanceof ServerLevel level&&level.dimension()==Level.OVERWORLD)overworldGenerator=level.getChunkSource().getGenerator();
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event){overworldGenerator=null;}
}
