package character.hakari;

import cn.blockforge.generated.nnnnr1201forge2.*;
import character.domain.DomainTerrainJournal;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** 坐杀博徒：中奖即收起；逻辑关闭与分批还原分离，不把数万方块挤在一个游戏刻。 */
public final class HakariDomain {
    public static final int SKILL = HakariSkills.DOMAIN;
    private static final int RADIUS=30, BATCH=512, GLOBAL_BATCH=1024, MAX_TICKS=1200, DRAIN_PER_SECOND=40;
    private record Rec(BlockPos pos,BlockState state) { }
    private static final class Job {
        final ServerPlayer owner;final ServerLevel level;final BlockPos center;
        final character.domain.DomainTerrain.Handle terrain;final List<Rec> plan=new ArrayList<>();
        final long start;long extra;int cursor;
        Job(ServerPlayer p){owner=p;level=p.serverLevel();center=p.blockPosition();start=level.getGameTime();terrain=character.domain.DomainTerrain.begin(level,p.getUUID());}
    }
    private static final Map<UUID,Job> JOBS=new LinkedHashMap<>();
    public static boolean active(ServerPlayer p){return p!=null&&JOBS.containsKey(p.getUUID());}
    public static void open(ServerPlayer p){
        if(!character.mainline.Mainline.requireDomain(p))return;
        if(active(p)){close(p,"已收起");return;}
        if(HakariCombat.jackpot(p)){p.displayClientMessage(Component.literal("中奖期间不重复展开赌场"),true);return;}
        Job job=new Job(p);buildPlan(job);JOBS.put(p.getUUID(),job);
        DomainClash.opened(p,Vec3.atCenterOf(job.center),RADIUS);
        if(!active(p))return;
        p.displayClientMessage(Component.literal("领域展开 · 坐杀博徒"),false);
        job.level.playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.get(),net.minecraft.sounds.SoundSource.PLAYERS,1.4f,.8f);
    }
    public static void toggle(ServerPlayer p){if(active(p))close(p,"已收起");else open(p);}
    public static void extend(ServerPlayer p,int ticks){var job=JOBS.get(p.getUUID());if(job!=null)job.extra+=Math.max(0,ticks);}
    public static void close(ServerPlayer p,String reason){var job=JOBS.remove(p.getUUID());if(job!=null)finish(job,reason);}
    private static void finish(Job job,String reason){
        job.plan.clear();job.terrain.close();DomainClash.closed(job.owner);
        Curse.data(job.owner).putBoolean("NpcHadDomain",false);
        if(!HakariCombat.jackpot(job.owner))Curse.endDomain(job.owner,false);
        job.owner.displayClientMessage(Component.literal("领域解除："+reason),false);
    }
    private static void buildPlan(Job job){
        int outer=RADIUS*RADIUS,inner=(RADIUS-1)*(RADIUS-1);
        var floor=Blocks.GOLD_BLOCK.defaultBlockState();var shell=Blocks.BLACK_CONCRETE.defaultBlockState();
        var light=Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL,15);
        for(int x=-RADIUS;x<=RADIUS;x++)for(int z=-RADIUS;z<=RADIUS;z++){
            if(x*x+z*z>outer)continue;
            job.plan.add(new Rec(job.center.offset(x,-1,z),floor));
            for(int y=0;y<=RADIUS;y++){
                int squared=x*x+y*y+z*z;if(squared>outer)break;
                job.plan.add(new Rec(job.center.offset(x,y,z),squared>inner?shell:light));
            }
        }
    }
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        int budget=GLOBAL_BATCH;
        // 中奖会同步 close，必须迭代快照，不能边 roll 边移除 HashMap 迭代器。
        for(Job job:List.copyOf(JOBS.values())){
            var p=job.owner;long age=job.level.getGameTime()-job.start;
            if(!p.isAlive()||p.isRemoved()||p.hasDisconnected()||p.level()!=job.level){close(p,"施术者离场");continue;}
            if(HakariCombat.jackpot(p)){close(p,"中奖后结界解除");continue;}
            if(age>=MAX_TICKS+job.extra){close(p,"本局结束");continue;}
            if(!DomainClash.active(p)&&age>0&&age%20==0&&!Curse.spend(p,DRAIN_PER_SECOND)){close(p,"咒力不足");continue;}
            int allowance=Math.min(BATCH,budget);
            while(job.cursor<job.plan.size()&&allowance-- >0){
                budget--;var entry=job.plan.get(job.cursor++);
                job.terrain.place(entry.pos,entry.state);
            }
            if(DomainClash.active(p))continue; // 对拼只暂停效果和开奖，不暂停本方半区施工。
            // 开奖不再依赖全部区块施工完成；未加载区块不会让本局无限等待。
            if(age>0&&age%40==0)HakariCombat.roll(p,1);
            if(!active(p))continue;
            if(age%10==0)job.level.sendParticles(ParticleTypes.END_ROD,job.center.getX()+.5,job.center.getY()+3,job.center.getZ()+.5,4,1.5,1,1.5,.01);
        }
        for(ServerLevel level:event.getServer().getAllLevels()){
            // 仅迁移清理旧版赌场日志；新版统一由 DomainTerrain 驱动。
            DomainTerrainJournal.get(level).restore(level,Set.of(),2048);
        }
    }
    public static void reset(){
        var jobs=List.copyOf(JOBS.values());JOBS.clear();
        Set<ServerLevel> levels=new HashSet<>();for(Job job:jobs){finish(job,"服务器关闭");levels.add(job.level);}
        for(var level:levels){var journal=DomainTerrainJournal.get(level);journal.restore(level,Set.of(),journal.size());}
    }
    private HakariDomain() { }
}
