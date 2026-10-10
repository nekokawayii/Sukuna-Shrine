package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import java.util.stream.IntStream;

/** 地貌按世界坐标采样，跨区块的河水和梯田始终连续。 */
public final class IwateTerrain {
    private static final PerlinNoise HEIGHT=PerlinNoise.create(RandomSource.create(0x495741544531L),IntStream.rangeClosed(-3,0));
    private static final PerlinNoise BIOME=PerlinNoise.create(RandomSource.create(0x495741544532L),IntStream.rangeClosed(-2,0));
    public static final int RIVER_LEVEL=1299;
    public static boolean home(int x,int z){return x>=-32&&x<=192&&z>=-80&&z<=80;}
    private static int local(int n){return Math.floorMod(n+128,384)-128;}
    public static boolean forest(int x,int z){return !home(x,z)&&!terrace(x,z)&&BIOME.getValue(x/95.0,0,z/95.0)>0.14;}
    public static double riverDistance(int x,int z){return Math.abs(local(x)+18-Math.sin(z*0.018)*7-Math.sin(z*0.043)*3);}
    public static boolean river(int x,int z){return !home(x,z)&&riverDistance(x,z)<=3;}
    public static boolean terrace(int x,int z){int a=local(x),b=local(z);return !home(x,z)&&a>=-104&&a<=-40&&b>=48&&b<=120;}
    public static boolean terraceBank(int x,int z){int a=local(x),b=local(z);return Math.floorMod(a+104,16)==0||a==-40||Math.floorMod(b-48,18)==0||b==120;}
    public static boolean wetland(int x,int z){return river(x,z)||terrace(x,z);}
    private static int naturalHeight(int x,int z){double d=Math.max(Math.max(-32-x,x-192),Math.max(-80-z,z-80));double blend=Math.max(0,Math.min(1,d/48));double n=HEIGHT.getValue(x/44.0,0,z/44.0);return Math.max(1290,1300+(int)Math.round((n*28+5)*blend));}
    public static int height(int x,int z){
        if(home(x,z))return 1300;
        if(terrace(x,z))return 1300+Math.floorDiv(local(x)+104,16)*2;
        double distance=riverDistance(x,z);
        if(distance<=3)return RIVER_LEVEL-2;
        int natural=naturalHeight(x,z);
        if(distance<12)return (int)Math.round((RIVER_LEVEL+1)+(natural-RIVER_LEVEL-1)*Math.min(1,(distance-3)/9));
        return natural;
    }
    public static int surface(int x,int z){return river(x,z)?RIVER_LEVEL:height(x,z);}
    public static long hash(int x,int z){long n=x*341873128712L+z*132897987541L+0x4957415445L;n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);}
    private IwateTerrain(){}
}
