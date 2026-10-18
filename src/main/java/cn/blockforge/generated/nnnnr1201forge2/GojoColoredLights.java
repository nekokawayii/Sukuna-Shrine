package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * 苍 / 赫 / 茈 的**真彩色光照**：把 {@link ColoredLightBlock} 贴在光球位置，交给原版光照引擎传播。
 *
 * <p>分工：本类只负责"把光源方块搬到光球所在的那一格"；**颜色由 Colorful Lighting 按方块 id 上色**
 * （{@code assets/sukuna/light/emitters.json}）。所以拿到了真阴影、真遮挡、光穿彩玻璃染色，
 * 而且它跟 Oculus 光影兼容（Colorful Lighting 自带 shader 补丁器）。</p>
 *
 * <p>四条保命规则：</p>
 * <ol>
 *   <li>没装 Colorful Lighting 就**完全不写世界** —— 退回 r267 的顶点发光壳，玩法一点不变；</li>
 *   <li>只在原方块是空气时放置，**绝不覆盖地形**；回收时也先确认那一格还是自己的方块；</li>
 *   <li>光球**跨格才挪方块**（同一格只续期），避免每刻 setBlock 把光照引擎刷爆；</li>
 *   <li>最多 {@link #MAX} 个光源，全部按 {@link #STALE} 刻没刷新就回收，退出/关服清干净。</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid="sukuna")
public final class GojoColoredLights {
    /** 三个方块的亮度（Colorful Lighting 不写 {@code ;N} 时就吃这个值）。 */
    public static final int LIGHT_BLUE=12,LIGHT_RED=9,LIGHT_PURPLE=13;
    /** 同时存在的光源上限；超了先回收最早的一条。 */
    private static final int MAX=4;
    /** 超过这么多刻没收到光球刷新就回收（光球每刻都会发 BLUE/RED/PURPLE，所以只是兜底）。 */
    private static final int STALE=8;
    /** 前置 modid：Colorful Lighting（客户端 mod，装上才有颜色）。 */
    public static final String DEPENDENCY="colorful_lighting";

    /** 一条光源记录；{@code pos} 为 null 表示"当前没放成"（光球埋在方块里），但仍然记着这个弹体。 */
    private record Held(long key,ServerLevel level,BlockPos pos,Block block,long seen){}
    private static final Map<Long,Held> HELD=new LinkedHashMap<>();
    private static Boolean present;
    private static boolean announced;
    private static final org.slf4j.Logger LOG=com.mojang.logging.LogUtils.getLogger();

    /** 前置是否在场（缓存一次）。单机=集成服务端与客户端同一个 JVM，所以这里就是"客户端装没装"。 */
    public static boolean present(){
        if(present==null)present=ModList.get().isLoaded(DEPENDENCY);
        return present;
    }

    /** 由 {@code Network.send} 在自己的特效下发入口统一喂进来（服务端线程）。 */
    public static void onEffect(ServerLevel level,Network.Effect e){
        if(level==null||e==null)return;
        int kind=e.kind();
        Block block=null;
        if(kind==GojoFx.BLUE)block=ProgressionContent.BLUE_LIGHT.get();
        else if(kind==GojoFx.RED)block=ProgressionContent.RED_LIGHT.get();
        else if(kind==GojoFx.PURPLE)block=ProgressionContent.PURPLE_LIGHT.get();
        long key=((long)e.owner()<<32)^(e.seed()&0xFFFFFFFFL);
        if(block==null){
            // 苍/赫/茈 的收尾包：光球没了，光也撤掉。
            if(kind==GojoFx.BLUE_END||kind==GojoFx.RED_END||kind==GojoFx.PURPLE_BURST)drop(key);
            return;
        }
        if(!present())return;
        hold(key,level,e.position(),block);
    }

    private static void hold(long key,ServerLevel level,Vec3 at,Block block){
        BlockPos target=BlockPos.containing(at.x,at.y,at.z);
        long now=level.getGameTime();
        Held old=HELD.get(key);
        if(old!=null){
            if(old.level()==level&&old.block()==block&&target.equals(old.pos())){
                HELD.put(key,new Held(key,level,target,block,now));   // 同一格：只续期，不动世界
                return;
            }
            HELD.remove(key);
            clear(old);
        }
        if(HELD.size()>=MAX){
            Iterator<Map.Entry<Long,Held>> it=HELD.entrySet().iterator();
            if(it.hasNext()){Held oldest=it.next().getValue();it.remove();clear(oldest);}
        }
        BlockPos spot=spotFor(level,target);
        if(spot!=null){
            level.setBlock(spot,block.defaultBlockState(),Block.UPDATE_CLIENTS);
            level.getLightEngine().checkBlock(spot);
            if(!announced){announced=true;LOG.info("[彩色光照] 已接管苍/赫/茈：光源方块开始跟随光球（亮度 苍{} 赫{} 茈{}，颜色见 assets/sukuna/light/emitters.json）",LIGHT_BLUE,LIGHT_RED,LIGHT_PURPLE);}
        }
        HELD.put(key,new Held(key,level,spot,block,now));
    }

    /** 光球中心那一格如果不是空气（埋进地形/建筑里），就往上/四周找一格空气，绝不覆盖别人的方块。 */
    private static BlockPos spotFor(ServerLevel level,BlockPos target){
        if(level.getBlockState(target).isAir())return target;
        BlockPos above=target.above();
        if(level.getBlockState(above).isAir())return above;
        for(BlockPos p:new BlockPos[]{target.north(),target.south(),target.east(),target.west()})
            if(level.getBlockState(p).isAir())return p;
        return null;
    }

    private static void drop(long key){
        Held old=HELD.remove(key);
        if(old!=null)clear(old);
    }

    private static void clear(Held held){
        BlockPos pos=held.pos();
        if(pos==null||held.level()==null)return;
        if(!held.level().getBlockState(pos).is(held.block()))return;   // 那一格已经不是我们的方块了，别乱清
        for(Held other:HELD.values())if(pos.equals(other.pos()))return; // 还有别的光球用着这一格
        held.level().setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
        held.level().getLightEngine().checkBlock(pos);
    }

    /** 给 {@link ColoredLightBlock#randomTick} 用：这一格是否还有活动光球占着。 */
    public static boolean isReferenced(BlockPos pos,Block block){
        for(Held held:HELD.values())
            if(held.block()==block&&pos.equals(held.pos()))return true;
        return false;
    }

    /** 光球一直在刷新，正常轮不到这里；这里只兜底丢包/异常的情况。 */
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||HELD.isEmpty())return;
        Iterator<Map.Entry<Long,Held>> it=HELD.entrySet().iterator();
        while(it.hasNext()){
            Held held=it.next().getValue();
            if(held.level()==null){it.remove();continue;}
            if(held.level().getGameTime()-held.seen()<=STALE)continue;
            it.remove();
            clear(held);
        }
    }

    /** 整个世界卸载（退世界/换维度）：那一局的方块已经不重要了，直接忘掉，别去碰别的维度。 */
    @SubscribeEvent public static void unload(LevelEvent.Unload event){
        if(HELD.isEmpty())return;
        HELD.values().removeIf(held->held.level()==event.getLevel());
    }

    @SubscribeEvent public static void stopping(ServerStoppingEvent event){HELD.clear();}

    /** 开局打一行状态：装没装前置、能放几个光源，一眼可验。 */
    @SubscribeEvent public static void started(net.minecraftforge.event.server.ServerStartedEvent event){LOG.info("[彩色光照] {}",status());}

    /** 状态自检（给 /sukuna 之类的诊断用，也方便日志里确认前置在不在）。 */
    public static String status(){
        return "彩色光照 前置="+(present()?"已装 "+DEPENDENCY:"未装，退回自绘发光壳")+" 活动光源="+HELD.size()+"/"+MAX;
    }

    private GojoColoredLights(){}
}
