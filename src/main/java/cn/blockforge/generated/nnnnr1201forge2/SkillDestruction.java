package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 默认不破坏地形；所有攻击共用开关、领地事件和当刻去重破坏。 */
public final class SkillDestruction {
    public static final GameRules.Key<GameRules.BooleanValue> RULE=GameRules.register("sukunaDesblock",GameRules.Category.MISC,GameRules.BooleanValue.create(false));
    public static void init(){MinecraftForge.EVENT_BUS.register(SkillDestruction.class);}
    public static boolean enabled(ServerLevel level){return level.getServer().overworld().getGameRules().getBoolean(RULE);}
    public static void set(net.minecraft.server.MinecraftServer server,boolean on){server.overworld().getGameRules().getRule(RULE).set(on,server);if(!on)visitedNow.clear();}
    public static void line(ServerPlayer p,Vec3 from,Vec3 to,double radius){
        immediate(p,from,to,radius);
    }
    private static final Map<ServerLevel,Set<net.minecraft.world.level.ChunkPos>> loaded=new HashMap<>();
    private static final Map<UUID,Set<BlockPos>> visitedNow=new HashMap<>();
    private static long visitedTick=Long.MIN_VALUE;
    @SubscribeEvent public static void load(net.minecraftforge.event.level.ChunkEvent.Load e){if(e.getLevel() instanceof ServerLevel l)loaded.computeIfAbsent(l,k->new HashSet<>()).add(e.getChunk().getPos());}
    @SubscribeEvent public static void unload(net.minecraftforge.event.level.ChunkEvent.Unload e){if(e.getLevel() instanceof ServerLevel l){Set<net.minecraft.world.level.ChunkPos> set=loaded.get(l);if(set!=null)set.remove(e.getChunk().getPos());}}
    /** 当刻直接破坏；不排队、不延后追着特效补破坏。仍保留原有地形开关和领地保护。 */
    public static void immediate(ServerPlayer p,Vec3 from,Vec3 to,double radius){
        ServerLevel l=p.serverLevel();if(!enabled(l)||!p.isAlive()||DomainClash.active(p)||EternalLove.settling(MaidBridge.body(p))||!Double.isFinite(from.lengthSqr()+to.lengthSqr()))return;
        long tick=l.getServer().overworld().getGameTime();if(tick!=visitedTick){visitedTick=tick;visitedNow.clear();}
        Set<BlockPos> visited=visitedNow.computeIfAbsent(p.getUUID(),k->new HashSet<>());
        int n=Math.max(1,(int)Math.ceil(from.distanceTo(to)*1.75)),r=(int)Math.ceil(Math.min(4,Math.max(0,radius)));
        for(int i=0;i<=n;i++){Vec3 point=from.lerp(to,i/(double)n);BlockPos c=BlockPos.containing(point);
            for(BlockPos pos:BlockPos.betweenClosed(c.offset(-r,-r,-r),c.offset(r,r,r))){
                if(Vec3.atCenterOf(pos).distanceToSqr(point)>(radius+0.8)*(radius+0.8)||!visited.add(pos.immutable())||!l.hasChunkAt(pos)||!l.getWorldBorder().isWithinBounds(pos))continue;
                var state=l.getBlockState(pos);if(state.isAir()||state.getDestroySpeed(l,pos)<0||state.hasBlockEntity()||SoulJourney.protectedBlock(l,pos))continue;
                ServerPlayer authority=p instanceof MaidBridge.Actor a?MaidBridge.owner(a):p;if(authority==null||!l.mayInteract(authority,pos))continue;
                if(MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(l,pos.immutable(),state,authority))||!ForgeEventFactory.onEntityDestroyBlock(MaidBridge.body(p),pos,state))continue;
                l.destroyBlock(pos,false,MaidBridge.body(p));
            }
        }
    }
    /** 掀块和断流也走相同的开关、领地事件与不可破坏方块保护。 */
    private static boolean mayChange(ServerPlayer p,BlockPos pos){
        ServerLevel l=p.serverLevel();if(!enabled(l)||!p.isAlive()||DomainClash.active(p)||!l.hasChunkAt(pos)||!l.getWorldBorder().isWithinBounds(pos))return false;
        var state=l.getBlockState(pos);if(state.isAir()||state.getDestroySpeed(l,pos)<0||state.hasBlockEntity()||SoulJourney.protectedBlock(l,pos))return false;
        ServerPlayer authority=p instanceof MaidBridge.Actor a?MaidBridge.owner(a):p;
        return authority!=null&&l.mayInteract(authority,pos)&&!MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(l,pos,state,authority))&&ForgeEventFactory.onEntityDestroyBlock(MaidBridge.body(p),pos,state);
    }
    public static boolean eraseFluid(ServerPlayer p,BlockPos pos){
        if(p.serverLevel().getFluidState(pos).isEmpty()||!mayChange(p,pos))return false;var state=p.serverLevel().getBlockState(pos);
        if(state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))return p.serverLevel().setBlock(pos,state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,false),3);
        return p.serverLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
    }
    public static boolean fling(ServerPlayer p,BlockPos pos,Vec3 away){
        if(!mayChange(p,pos))return false;var state=p.serverLevel().getBlockState(pos);
        if(state.getCollisionShape(p.serverLevel(),pos).isEmpty())return false;
        // fall() 在同一游戏刻替换原方块并生成真正的 FallingBlockEntity，不经过掉落物。
        if(state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))state=state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,false);
        var falling=net.minecraft.world.entity.item.FallingBlockEntity.fall(p.serverLevel(),pos,state);falling.dropItem=false;falling.disableDrop();falling.setDeltaMovement(away.normalize().scale(.5).add(0,.85,0));falling.hurtMarked=true;return true;
    }
    /** 同 {@link #fling}，但把生成的下落实体交回调用方 —— 苍的"吸入"要每刻把它往球心拽，赫的"掀飞"要它自己飞。 */
    public static net.minecraft.world.entity.item.FallingBlockEntity flingEntity(ServerPlayer p,BlockPos pos,Vec3 away){
        if(!mayChange(p,pos))return null;
        var state=p.serverLevel().getBlockState(pos);
        if(state.getCollisionShape(p.serverLevel(),pos).isEmpty())return null;
        if(state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))state=state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,false);
        var falling=net.minecraft.world.entity.item.FallingBlockEntity.fall(p.serverLevel(),pos,state);
        falling.dropItem=false;falling.disableDrop();
        falling.setDeltaMovement(away.normalize().scale(.5).add(0,.85,0));falling.hurtMarked=true;
        return falling;
    }
    /** **断流专用**：把球形范围内所有流体一次斩掉（水/岩浆源与含水方块），**完全不碰方块、不看 {@link #RULE} 开关**。
     *  仍尊重领地（mayInteract）、保护方块与世界边界 —— 用户要求"不管开没开破坏方块都强制斩没流体，而且一瞬间全斩掉"。 */
    public static int flushFluids(ServerPlayer p,double radius){
        ServerLevel l=p.serverLevel();if(!p.isAlive())return 0;
        ServerPlayer authority=p instanceof MaidBridge.Actor a?MaidBridge.owner(a):p;
        if(authority==null)return 0;
        BlockPos c=BlockPos.containing(p.getEyePosition());int r=(int)Math.ceil(radius),erased=0;
        double limit=radius*radius;
        for(BlockPos pos:BlockPos.betweenClosed(c.offset(-r,-r,-r),c.offset(r,r,r))){
            if(Vec3.atCenterOf(pos).distanceToSqr(c.getX()+0.5,c.getY()+0.5,c.getZ()+0.5)>limit)continue;
            if(!l.hasChunkAt(pos)||!l.getWorldBorder().isWithinBounds(pos))continue;
            if(l.getFluidState(pos).isEmpty())continue;
            if(SoulJourney.protectedBlock(l,pos))continue;
            if(!l.mayInteract(authority,pos.immutable()))continue;
            net.minecraft.world.level.block.state.BlockState state=l.getBlockState(pos);
            if(state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)){
                l.setBlock(pos,state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,false),3);
            }else{
                l.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            }
            erased++;
        }
        return erased;
    }
    public static boolean forceStream(ServerPlayer p,BlockPos pos,Vec3 away){
        var state=p.serverLevel().getBlockState(pos);if(state.isAir())return false;
        // 纯流体直接清除；固体（含含水方块）直接化为下落实体。
        if(!state.getCollisionShape(p.serverLevel(),pos).isEmpty())return fling(p,pos,away);
        return !state.getFluidState().isEmpty()&&eraseFluid(p,pos);
    }
    /** 无限线与已加载区块逐一求交；没有固定长度，也不强制加载远方。 */
    public static void infinite(ServerPlayer p,Vec3 center,Vec3 axis){infinite(p,center,axis,.45);}
    public static void infinite(ServerPlayer p,Vec3 center,Vec3 axis,double width){
        if(!enabled(p.serverLevel()))return;ServerLevel l=p.serverLevel();
        for(var chunk:List.copyOf(loaded.getOrDefault(l,Set.of()))){double low=-Double.MAX_VALUE,high=Double.MAX_VALUE;
            double[] o={center.x,center.y,center.z},d={axis.x,axis.y,axis.z},min={chunk.x*16-width,l.getMinBuildHeight(),chunk.z*16-width},max={chunk.x*16+16+width,l.getMaxBuildHeight(),chunk.z*16+16+width};boolean miss=false;
            for(int k=0;k<3;k++){if(Math.abs(d[k])<1e-8){if(o[k]<min[k]||o[k]>max[k]){miss=true;break;}}else{double a=(min[k]-o[k])/d[k],b=(max[k]-o[k])/d[k];low=Math.max(low,Math.min(a,b));high=Math.min(high,Math.max(a,b));}}
            if(!miss&&low<=high)immediate(p,center.add(axis.scale(low)),center.add(axis.scale(high)),width);
        }
    }
    /** 大范围终极技：同一刻结算，仍遵守总开关、领地、基岩和容器保护。 */
    public static void sphere(ServerPlayer p,Vec3 center,double radius){
        if(!enabled(p.serverLevel())||!p.isAlive()||DomainClash.active(p))return;
        int r=(int)Math.ceil(Math.min(50,Math.max(0,radius)));BlockPos c=BlockPos.containing(center);
        for(BlockPos pos:BlockPos.betweenClosed(c.offset(-r,-r,-r),c.offset(r,r,r)))if(Vec3.atCenterOf(pos).distanceToSqr(center)<=radius*radius&&mayChange(p,pos))p.serverLevel().destroyBlock(pos,false,MaidBridge.body(p));
    }
    public static void impact(ServerPlayer p,Vec3 point,double radius){line(p,point,point,radius);}
    public static void effect(ServerLevel level,Network.Effect e){
        if(!enabled(level)||e.life()<=0)return;
        var entity=level.getEntity(e.owner());ServerPlayer p=entity instanceof ServerPlayer sp?sp:MaidBridge.caster(entity);
        if(p==null)return;
        switch(e.kind()){
            case 0,12,51,59,64,72,92,93,96,115,118,119,122,123,126,130,131,132,133,134,135,141 -> {
                Vec3 direction=e.direction().lengthSqr()<0.01?p.getLookAngle():e.direction().normalize();
                double length=e.size()==SlashReach.INFINITE?120:Math.max(2,Math.min(120,e.size()));
                Vec3 side=(e.kind()==0||e.kind()==12||e.kind()==51||e.kind()==72||e.kind()==96||e.kind()==115||e.kind()==123)?SlashReach.axis(direction,e.seed()):NetBranches.side(direction);
                if(e.size()==SlashReach.INFINITE)infinite(p,e.position(),side);else line(p,e.position().subtract(side.scale(length/2)),e.position().add(side.scale(length/2)),0.6);
            }
            case 121,125,139,140,142 -> line(p,e.position(),e.direction(),0.7);
            case 4,23,116,120 -> impact(p,e.position(),Math.min(4,e.size()));
            case 1,90 -> {for(int i=0;i<24;i++){double a=p.getRandom().nextDouble()*Math.PI*2,r=Math.sqrt(p.getRandom().nextDouble())*e.size();Vec3 at=e.position().add(Math.cos(a)*r,0,Math.sin(a)*r);line(p,at.add(0,12,0),at.add(0,-8,0),0.7);}}
            default -> {}
        }
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("sukuna").then(Commands.literal("gamerule").then(Commands.literal("desblock").requires(s->s.hasPermission(2))
            .executes(c->{boolean on=enabled(c.getSource().getLevel());c.getSource().sendSuccess(()->Component.literal("技能破坏方块："+on),false);return on?1:0;})
            .then(Commands.argument("value",BoolArgumentType.bool()).executes(c->{boolean on=BoolArgumentType.getBool(c,"value");var server=c.getSource().getServer();set(server,on);c.getSource().sendSuccess(()->Component.literal("全部攻击技能破坏方块："+(on?"开启，请远离建筑":"关闭")),true);return 1;})))));
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent e){loaded.clear();visitedNow.clear();visitedTick=Long.MIN_VALUE;}
    private SkillDestruction(){}
}
