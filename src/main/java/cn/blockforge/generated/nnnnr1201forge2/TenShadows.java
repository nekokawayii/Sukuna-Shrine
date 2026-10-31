package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.*;

@Mod.EventBusSubscriber(modid="sukuna")
public final class TenShadows {
    public static final int CHANT_TICKS=181;
    public static final int FX_POSE=262;
    public static final BlockPos BED=new BlockPos(43,65,19);
    private record Chant(ServerPlayer player,ServerLevel level,Vec3 anchor,long end){}
    private static final Map<UUID,Chant> chants=new HashMap<>();
    private static final String PARTIAL="TenShadowsMahoragaPartial";
    /** 两种显现的切换/重用冷却：120 秒（互斥：完全显现与不完全显现同时只能有一种生效）。 */
    public static final int MANIFEST_COOLDOWN=120*20;
    private static final String MANIFEST="TenShadowsManifestUntil";
    public record State(UUID player,boolean incarnated,boolean tamed,boolean partial,int remaining,int manifest){}
    /** 0=未调服时开始仪式，1=完全显现，2=不完全显现（开着时再点=解除）。 */
    public record Summon(int mode){}
    public static java.util.function.Consumer<State> receiver=s->{};
    public static boolean incarnated(Player p){return !character.CharacterService.toji(p)&&Curse.data(p).getBoolean("MegumiIncarnated");}
    /** 能否使用十影操作（召唤/调服/收回）：受肉伏黑，或直接选了伏黑惠角色。外观仍只看 incarnated。 */
    public static boolean canUse(Player p){
        var d=character.CharacterService.selected(p);
        return incarnated(p)||d!=null&&"megumi".equals(d.id());
    }
    public static boolean tamed(Player p){return Curse.data(p).getBoolean("TenShadowsMahoragaTamed");}
    /** 不完全显现是十影状态，不占头盔栏；服务端据此给予法轮适应。 */
    public static boolean partial(Player p){return tamed(p)&&Curse.data(p).getBoolean(PARTIAL);}
    /** 显现冷却剩余（游戏刻）。 */
    public static long manifestLeft(Player p){return Math.max(0,Curse.data(p).getLong(MANIFEST)-p.level().getGameTime());}
    private static boolean manifestReady(ServerPlayer p){
        long left=manifestLeft(p);
        if(left<=0)return true;
        p.displayClientMessage(Component.literal("影尚未平复 · 显现冷却还剩 "+((left+19)/20)+" 秒"),true);
        return false;
    }
    private static void startManifestCooldown(ServerPlayer p){Curse.data(p).putLong(MANIFEST,p.level().getGameTime()+MANIFEST_COOLDOWN);}
    public static boolean locked(Player p){return chants.containsKey(p.getUUID());}
    private static State state(ServerPlayer p){Chant c=chants.get(p.getUUID());return new State(p.getUUID(),incarnated(p),tamed(p),partial(p),c==null?0:(int)Math.max(0,c.end-c.level.getGameTime()),(int)manifestLeft(p));}
    public static void register(){
        Network.CHANNEL.messageBuilder(State.class,34,NetworkDirection.PLAY_TO_CLIENT).encoder((s,b)->{b.writeUUID(s.player);b.writeBoolean(s.incarnated);b.writeBoolean(s.tamed);b.writeBoolean(s.partial);b.writeVarInt(s.remaining);b.writeVarInt(s.manifest);}).decoder(b->new State(b.readUUID(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readVarInt())).consumerMainThread((s,c)->{receiver.accept(s);c.get().setPacketHandled(true);}).add();
        Network.CHANNEL.messageBuilder(Summon.class,35,NetworkDirection.PLAY_TO_SERVER).encoder((s,b)->b.writeVarInt(s.mode)).decoder(b->new Summon(b.readVarInt())).consumerMainThread((s,c)->{var p=c.get().getSender();if(p!=null)manifest(p,s.mode);c.get().setPacketHandled(true);}).add();
    }
    public static void sync(ServerPlayer p){Network.CHANNEL.send(PacketDistributor.ALL.noArg(),state(p));}
    public static void incarnate(ServerPlayer p){
        if(incarnated(p))return;
        if(!Journey.unlocked(p)){p.displayClientMessage(Component.literal("体内还没有能转移的宿傩灵魂"),true);return;}
        Curse.data(p).putBoolean("MegumiIncarnated",true);sync(p);ShadowSystem.sync(p);
        p.displayClientMessage(Component.literal("肉身已转移 · 十种影法术觉醒"),false);
    }
    public static void tame(ServerPlayer p){Curse.data(p).putBoolean("TenShadowsMahoragaTamed",true);sync(p);}
    /** 十影界面的两种显现选择；未调服时只允许 mode=0 开始调服。
     *  <p>两种显现**互斥**：放完全显现会自动解除不完全显现，反之亦然；
     *  每次显现（切换或重新使用）后进入 {@link #MANIFEST_COOLDOWN} 冷却，解除法轮本身不受冷却限制。</p> */
    public static void manifest(ServerPlayer p,int mode){
        if(PrisonSystem.locked(p)||!incarnated(p)||locked(p))return;
        if(!tamed(p)){if(mode==0)begin(p);return;}
        if(mode==1){
            if(!manifestReady(p))return;
            if(MahoragaAdaptStore.summonAlly(p)){startManifestCooldown(p);setPartial(p,false);}
        }else if(mode==2){
            if(partial(p)){setPartial(p,false);return;} // 解除不算切换，不占用冷却
            if(!manifestReady(p))return;
            MahoragaAdaptStore.recallAll(p); // 跨维度收回并合并适应，远处的完全显现也不能遗留
            startManifestCooldown(p);
            setPartial(p,true);
        }
    }
    private static void setPartial(ServerPlayer p,boolean enabled){
        boolean changed=partial(p)!=enabled;
        Curse.data(p).putBoolean(PARTIAL,enabled);sync(p);
        if(!changed)return;
        if(enabled){
            MahoragaAdaptStore.sync(p);
            p.displayClientMessage(Component.literal("不完全显现 · 适应之轮已悬于头顶"),true);
        }else{
            Network.send(p.serverLevel(),new Network.Effect(MahoragaVisuals.WHEEL_END,p.getId(),p.position(),Vec3.ZERO,0,1,0));
            p.displayClientMessage(Component.literal("不完全显现已解除"),true);
        }
    }
    public static void begin(ServerPlayer p){
        if(PrisonSystem.locked(p)||!incarnated(p)||locked(p)||!p.isAlive()||p.isSpectator())return;
        if(p.serverLevel().getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){p.displayClientMessage(Component.literal("请先把难度改为简单、普通或困难"),true);return;}
        TechniqueInput.cancel(p);Skills.cancelCharge(p);p.stopUsingItem();
        chants.put(p.getUUID(),new Chant(p,p.serverLevel(),p.position(),p.level().getGameTime()+CHANT_TICKS));
        SkillSounds.play(p,"ten_shadows_chant");sync(p);
    }
    private static void cancel(ServerPlayer p){if(chants.remove(p.getUUID())!=null)sync(p);}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        if(p.tickCount%40==0&&p.level().dimension()==SoulJourney.CORRIDOR&&p.getY()<100&&p.position().distanceToSqr(Vec3.atCenterOf(BED))<96*96)ensureRoom(p.serverLevel());
        Chant c=chants.get(p.getUUID());
        if(c==null){if(p.tickCount%40==0&&manifestLeft(p)>0)sync(p);return;}
        if(!p.isAlive()||p.isRemoved()||p.level()!=c.level||p.isSpectator()||p.isPassenger()){cancel(p);return;}
        p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
        if(p.position().distanceToSqr(c.anchor)>.0001)p.connection.teleport(c.anchor.x,c.anchor.y,c.anchor.z,p.getYRot(),p.getXRot());
        if(c.level.getGameTime()>=c.end){
            chants.remove(p.getUUID());sync(p);
            if(tamed(p))MahoragaAdaptStore.summonAlly(p);else MahoragaRitual.summon(p);
        }else if(p.tickCount%20==0)sync(p);
    }
    private static void ensureRoom(ServerLevel level){
        // 显式载入床位区块后再找实体，避免卸载期间误判为空而补刷重复肉身。
        level.getChunk(BED.getX()>>4,BED.getZ()>>4);
        SoulWorldState state=SoulWorldState.get(level);
        if(!state.megumiRoom()){
            // 只补一间侧室及连接通道；旧存档原有的房间、遗迹和玩家建筑不会整体重建。
            for(BlockPos pos:BlockPos.betweenClosed(34,63,14,49,71,24)){
                int x=pos.getX(),y=pos.getY(),z=pos.getZ();boolean shell=x==49||z==14||z==24||y==63||y==71||x==34;
                level.setBlockAndUpdate(pos,(shell?Blocks.POLISHED_DEEPSLATE:Blocks.AIR).defaultBlockState());
            }
            for(BlockPos pos:BlockPos.betweenClosed(33,64,18,34,66,20))level.setBlockAndUpdate(pos,Blocks.SCULK.defaultBlockState());
            for(BlockPos pos:BlockPos.betweenClosed(42,64,18,44,64,20))level.setBlockAndUpdate(pos,Blocks.SMOOTH_STONE.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(47,65,15),Blocks.SOUL_LANTERN.defaultBlockState());
            state.markMegumiRoom();
        }
        // 旧存档里封印与墙同色（黑色混凝土），统一换成幽布，让玩家能一眼看见门。
        if(level.getBlockState(new BlockPos(33,65,19)).is(Blocks.BLACK_CONCRETE))
            for(BlockPos pos:BlockPos.betweenClosed(33,64,18,34,66,20))level.setBlockAndUpdate(pos,Blocks.SCULK.defaultBlockState());
        // r129：旧侧室不再补刷可直接受肉的肉身；改为高专灵魂降灵挑战。
    }
    @SubscribeEvent public static void secret(PlayerInteractEvent.RightClickBlock e){
        var at=e.getPos();if(!(e.getEntity() instanceof ServerPlayer p)||p.level().dimension()!=SoulJourney.CORRIDOR||!p.isShiftKeyDown()||at.getX()!=33||at.getY()<64||at.getY()>66||at.getZ()<18||at.getZ()>20)return;
        ensureRoom(p.serverLevel());for(BlockPos pos:BlockPos.betweenClosed(33,64,18,34,66,20))p.serverLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
    }
    /** /sukuna soul room：补建侧室、破开幽布封印，并把玩家送到房间内肉身旁。 */
    public static void revealRoom(ServerPlayer p){
        if(p.level().dimension()!=SoulJourney.CORRIDOR){p.sendSystemMessage(Component.literal("请先走进灵魂通道，再使用 /sukuna soul room。"));return;}
        ServerLevel level=p.serverLevel();ensureRoom(level);
        for(BlockPos pos:BlockPos.betweenClosed(33,64,18,34,66,20))level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        for(BlockPos pos:BlockPos.betweenClosed(35,64,18,36,66,20))if(!level.getBlockState(pos).isAir())level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        p.connection.teleport(38.5,64.0,19.5,-90,0);
        p.sendSystemMessage(Component.literal("旧侧室已开启，但这里不再提供肉身。请在主世界高专遗迹寻找伏黑惠灵魂，合成降灵·伏黑惠后对村民使用。"),false);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent e){if(e.isCancelable()&&locked(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent public static void attack(AttackEntityEvent e){if(locked(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent public static void breaking(PlayerEvent.BreakSpeed e){if(locked(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){for(ServerPlayer other:p.server.getPlayerList().getPlayers())Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),state(other));sync(p);}}
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e){if(e.getEntity() instanceof ServerPlayer listener&&e.getTarget() instanceof ServerPlayer target)Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->listener),state(target));}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){cancel(p);sync(p);}}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){chants.clear();}
    private TenShadows(){}
}
