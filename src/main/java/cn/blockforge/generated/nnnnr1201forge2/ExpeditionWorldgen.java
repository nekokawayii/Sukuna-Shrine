package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import java.util.stream.Stream;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.event.server.ServerAboutToStartEvent;

public final class ExpeditionWorldgen {
    public static final List<String> BIOMES=List.of("cursed_wasteland","cursed_sakura_forest","scorched_volcanic_fields","ancient_battlefield");
    private static final DeferredRegister<Codec<? extends BiomeSource>> SOURCES=DeferredRegister.create(Registries.BIOME_SOURCE,"sukuna");
    private static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS=DeferredRegister.create(Registries.CHUNK_GENERATOR,"sukuna");
    private static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(ForgeRegistries.FEATURES,"sukuna");
    static {
        SOURCES.register("cursed_overworld",()->CursedSource.CODEC);
        GENERATORS.register("culling_city",()->CullingCityGenerator.CODEC);
        GENERATORS.register("hyakki_cathedral",()->HyakkiCathedralGenerator.CODEC);
        FEATURES.register("cursed_landscape",CursedLandscapeFeature::new);
    }
    public static void init(IEventBus bus){SOURCES.register(bus);GENERATORS.register(bus);FEATURES.register(bus);}
    public static String biome(LevelReader l,BlockPos pos){return l.getBiome(pos).unwrapKey().map(k->k.location().getPath()).orElse("");}
    /** 在世界创建前装饰原主世界群系源，不替换地形生成器、不重写旧区块、不碰其他维度。 */
    @SubscribeEvent public static void beforeWorld(ServerAboutToStartEvent e){
        var registry=e.getServer().registryAccess();
        var stems=registry.registry(Registries.LEVEL_STEM);if(stems.isEmpty())return;
        var stem=stems.get().get(LevelStem.OVERWORLD);if(stem==null)return;
        var generator=stem.generator();if(!(generator instanceof NoiseBasedChunkGenerator)||generator.getBiomeSource() instanceof CursedSource)return;
        var base=generator.getBiomeSource();
        // 已经由其他模组全面接管的群系源保持原样，避免破坏整合包；专用探索维度仍可使用。
        if(!(base instanceof MultiNoiseBiomeSource))return;
        List<Holder<Biome>> added=BIOMES.stream().map(id->(Holder<Biome>)registry.registryOrThrow(Registries.BIOME).getHolderOrThrow(ResourceKey.create(Registries.BIOME,new ResourceLocation("sukuna",id)))).toList();
        try{
            var field=Arrays.stream(ChunkGenerator.class.getDeclaredFields()).filter(f->f.getType()==BiomeSource.class).findFirst().orElseThrow();
            var source=new CursedSource(base,added);field.setAccessible(true);field.set(generator,source);
            // 原构造器的装饰排序闭包捕获了旧群系源，必须同时重建，否则新群系的矿和树会漏掉。
            var featureField=Arrays.stream(ChunkGenerator.class.getDeclaredFields()).filter(f->f.getType()==java.util.function.Supplier.class).findFirst().orElseThrow();
            featureField.setAccessible(true);
            featureField.set(generator,com.google.common.base.Suppliers.memoize(()->FeatureSorter.buildFeaturesPerStep(List.copyOf(source.possibleBiomes()),holder->generator.getBiomeGenerationSettings(holder).features(),true)));
        }catch(ReflectiveOperationException ex){throw new IllegalStateException("无法接入诅咒主世界群系",ex);}
    }
    public static final class CursedSource extends BiomeSource {
        public static final Codec<CursedSource> CODEC=RecordCodecBuilder.create(i->i.group(BiomeSource.CODEC.fieldOf("base").forGetter(s->s.base),Biome.CODEC.listOf().fieldOf("cursed_biomes").forGetter(s->s.added)).apply(i,CursedSource::new));
        private final BiomeSource base;private final List<Holder<Biome>> added;
        public CursedSource(BiomeSource base,List<Holder<Biome>> added){if(added.size()!=4)throw new IllegalArgumentException("诅咒群系必须为四种");this.base=base;this.added=List.copyOf(added);}
        private static long mix(long n){n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);}
        @Override protected Codec<? extends BiomeSource> codec(){return CODEC;}
        @Override protected Stream<Holder<Biome>> collectPossibleBiomes(){return Stream.concat(base.possibleBiomes().stream(),added.stream());}
        @Override public Holder<Biome> getNoiseBiome(int x,int y,int z,Climate.Sampler sampler){
            Holder<Biome> original=base.getNoiseBiome(x,y,z,sampler);String id=original.unwrapKey().map(k->k.location().getPath()).orElse("");
            if(id.contains("ocean")||id.contains("river")||id.contains("cave")||id.equals("deep_dark")||id.contains("beach"))return original;
            // 一个候选格为384格，每个格 18% 机会出现一小片诅咒群系（r202 起半径 84–140 格，面积约 3 倍）。
            // Climate噪声作为种子扰动：同一世界稳定，不强行改写正常河流、海洋或洞穴。
            int cellX=Math.floorDiv(x,96),cellZ=Math.floorDiv(z,96);
            var climate=sampler.sample(cellX*96+48,16,cellZ*96+48);
            long hash=mix(cellX*341873128712L+cellZ*132897987541L+climate.temperature()*31+climate.weirdness()*17+climate.erosion());
            if(Math.floorMod(hash,100)>=18)return original;
            int centerX=cellX*96+24+(int)Math.floorMod(hash>>>8,48),centerZ=cellZ*96+24+(int)Math.floorMod(hash>>>16,48);
            int radius=21+(int)Math.floorMod(hash>>>24,14);double dx=x-centerX,dz=z-centerZ;
            if(dx*dx+dz*dz<=radius*radius)return added.get((int)Math.floorMod(hash>>>32,4));
            return original;
        }
    }
    private ExpeditionWorldgen(){}
}
