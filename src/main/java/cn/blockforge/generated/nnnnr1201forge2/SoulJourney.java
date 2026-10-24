package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.commands.arguments.ResourceLocationArgument;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 阶段五入口、岩手乡村、世界保护、首领补刷与维度重建。 */
public final class SoulJourney {
    public static final ResourceKey<Level> CORRIDOR=ResourceKey.create(Registries.DIMENSION,new ResourceLocation("sukuna","soul_corridor"));
    private static final ArrayDeque<ChunkPos> REBUILD_QUEUE=new ArrayDeque<>();
    private static ServerLevel rebuildLevel;
    private static UUID rebuildRequester;
    private static final Map<ServerLevel,Long> missingMahitoSince=new HashMap<>();
    public static boolean ready(net.minecraft.world.entity.player.Player p){return Journey.nine(p)&&Journey.residuePercent(p)>=100;}
    public static boolean evolved(net.minecraft.world.entity.player.Player p){return Curse.data(p).getBoolean("SoulShrine");}

    public static boolean entryReady(ServerPlayer p){return MaidBridge.hasCondensedMaid(p)||ready(p);}
    private static void ensureRuin(ServerPlayer p){if(p.level().dimension()!=Level.OVERWORLD||!entryReady(p))return;var tag=Curse.data(p);if(tag.getBoolean("SoulRuinBuilt")||tag.getBoolean("SoulRuinAttempted"))return;tag.putBoolean("SoulRuinAttempted",true);RuinAtmosphere.build(p);}
    public static void enter(ServerPlayer p){
        if(p.level().dimension()==CORRIDOR){leave(p);return;}
        if(!entryReady(p)){p.sendSystemMessage(Component.literal("灵魂之门未回应：你或任意一位自己的女仆需要完成残秽凝练。你当前 +"+Journey.residuePercent(p)+"%"));return;}
        if(p.level().getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){p.sendSystemMessage(Component.literal("虎杖试炼需要简单、普通或困难难度。"));return;}
        ServerLevel level=p.server.getLevel(CORRIDOR);if(level==null){p.sendSystemMessage(Component.literal("灵魂通道维度未载入，请重新打开存档后再试。"));return;}
        if(rebuildLevel!=null){p.sendSystemMessage(Component.literal("灵魂通道正在重建，请等待完成后再进入。"));return;}
        var tag=Curse.data(p);tag.putString("SoulReturnDimension",p.level().dimension().location().toString());tag.putDouble("SoulReturnX",p.getX());tag.putDouble("SoulReturnY",p.getY());tag.putDouble("SoulReturnZ",p.getZ());
        level.getChunk(0,0);p.teleportTo(level,0.5,65,-30.5,0,0);p.getPersistentData().putLong("SukunaGateCooldown",level.getGameTime()+100);Skills.closeDomain(p);Skills.cancelCharge(p);
        var area=new AABB(-32,60,-48,32,90,48);if(!evolved(p)&&level.getEntitiesOfClass(YujiEntity.class,area,e->e.isAlive()&&!e.decoy()&&!e.familyMember()).isEmpty()){YujiEntity boss=SoulContent.YUJI.get().create(level);if(boss!=null){boss.moveTo(0.5,64,0.5,180,0);boss.challenger=p.getUUID();boss.setTarget(p);level.addFreshEntity(boss);}}
        p.sendSystemMessage(Component.literal("灵魂通道 · 北端返回现世，南端通往 Y=1000 真人区域；穿过肉墙后可前往 Y=1300 露天岩手。"));
        if(!TenShadows.incarnated(p))p.sendSystemMessage(Component.literal("十影受肉已改为高专挑战：在主世界高专遗迹箱子里找到伏黑惠灵魂，合成降灵·伏黑惠后对村民使用。旧侧室不再提供肉身。"));
    }
    private static void ensurePhotoChest(ServerPlayer p){
        ServerLevel level=p.serverLevel();SoulWorldState state=SoulWorldState.get(level);if(state.photoChestRepaired())return;
        if(state.guardianSummoned()){state.markPhotoChestRepaired();return;}
        BlockPos pos=new BlockPos(IwateVillage.HOME_X+21,IwateVillage.Y,IwateVillage.HOME_Z+75);level.getChunkAt(pos);
        if(!(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest))return;
        chest.unpackLootTable(p);for(int slot=0;slot<chest.getContainerSize();slot++)if(chest.getItem(slot).is(Content.YUJI_FAMILY_PHOTO.get())){state.markPhotoChestRepaired();return;}
        for(int slot=0;slot<chest.getContainerSize();slot++)if(chest.getItem(slot).isEmpty()){chest.setItem(slot,new ItemStack(Content.YUJI_FAMILY_PHOTO.get()));chest.setChanged();state.markPhotoChestRepaired();p.sendSystemMessage(Component.literal("旧存档已修复：虎杖家西房的旧箱子补入了双人合照。"));return;}
    }
    private static void ensureMahitoWall(ServerLevel level,ServerPlayer challenger){
        SoulWorldState state=SoulWorldState.get(level);
        // 兼容旧档已写入个人通关、却漏写世界通关的情况；不再自动开启再战。
        if(Curse.data(challenger).getBoolean("BrokenMahitoDefeated")&&!state.mahitoDefeated())state.markMahitoDefeated(level.getGameTime());
        if(state.mahitoDefeated()){
            awardMahito(challenger);
            List<BrokenMahitoEntity> leftovers=new ArrayList<>();
            for(Entity entity:level.getAllEntities())if(entity instanceof BrokenMahitoEntity boss)leftovers.add(boss);
            for(BrokenMahitoEntity boss:leftovers)boss.discard();
            missingMahitoSince.remove(level);return;
        }
        if(level.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL)return;
        // 地形加载不等于实体加载；整个房间的实体就绪后才判断是否缺少首领。
        boolean loaded=true;
        for(int x=-2;x<=1;x++)for(int z=-2;z<=1;z++){level.getChunk(x,z);if(!level.areEntitiesLoaded(ChunkPos.asLong(x,z)))loaded=false;}
        if(!loaded){missingMahitoSince.remove(level);return;}
        for(Entity entity:level.getAllEntities())if(entity instanceof BrokenMahitoEntity boss&&boss.isAlive()){state.markMahitoSpawned();missingMahitoSince.remove(level);return;}
        long since=missingMahitoSince.computeIfAbsent(level,l->l.getGameTime());
        // 仅未通关且首领确实缺失时补刷；等待实体加载稳定，避免重复生成。
        if(level.getGameTime()-since<40)return;
        BrokenMahitoEntity boss=Content.BROKEN_MAHITO.get().create(level);if(boss!=null){boss.moveTo(0.5,SoulCorridorGenerator.MAHITO_Y,3.5,180,0);boss.setTarget(challenger);if(level.addFreshEntity(boss)){state.markMahitoSpawned();missingMahitoSince.remove(level);}}
    }
    public static void passage(ServerPlayer p,BlockPos pos){
        if(p.level().dimension()!=CORRIDOR)return;
        if(rebuildLevel!=null){p.displayClientMessage(Component.literal("区域正在重建，请等待完成"),true);return;}
        if(pos.getY()<900){
            if(!evolved(p)&&!VoidJourney.evolved(p)&&!Journey.maidCleared(p,"YujiDefeated")){p.displayClientMessage(Component.literal("击败虎杖残影后，这道通路才会开启"),true);return;}
            p.teleportTo(p.serverLevel(),-12.5,1000,-12.5,0,0);ensureMahitoWall(p.serverLevel(),p);
        }else if(pos.getY()>=1288)p.teleportTo(p.serverLevel(),0.5,1000,19,180,0);
        else if(pos.getZ()<0)p.teleportTo(p.serverLevel(),0.5,64,39.5,180,0);
        else {
            if(!SoulWorldState.get(p.serverLevel()).mahitoDefeated()&&!Journey.maidCleared(p,"BrokenMahitoDefeated")){p.displayClientMessage(Component.literal("真人的咒力仍然封锁着岩手通路"),true);return;}
            p.teleportTo(p.serverLevel(),0.5,1301,-4.5,0,0);
        }
        Skills.cancelCharge(p);Skills.closeDomain(p);
    }
    public static boolean protectedBlock(Level level,BlockPos pos){return level.dimension()==CORRIDOR&&pos.getY()<SoulCorridorGenerator.IWATE_FLOOR;}
    private static void awardMahito(ServerPlayer p){
        if(Curse.data(p).getBoolean("BrokenMahitoDefeated"))return;
        Curse.data(p).putBoolean("BrokenMahitoDefeated",true);Curse.sync(p);
        p.sendSystemMessage(Component.literal("真人任务已完成 · 岩手通路已开启，穿过南侧肉墙通道继续前进。"));
    }
    public static void brokenMahitoDefeated(BrokenMahitoEntity boss){
        ServerLevel level=(ServerLevel)boss.level();
        if(level.dimension()==CORRIDOR){SoulWorldState.get(level).markMahitoDefeated(level.getGameTime());missingMahitoSince.remove(level);}
        for(ServerPlayer p:level.players())if(p.distanceToSqr(boss)<128*128)awardMahito(p);
        for(Entity entity:level.getAllEntities())if(entity instanceof LivingEntity maid&&MaidBridge.isMaid(maid)&&maid.distanceToSqr(boss)<128*128)MaidBridge.recordBossDefeat(MaidBridge.actor(maid),boss);
    }
    @SubscribeEvent public static void noCorridorMobs(EntityJoinLevelEvent e){
        if(e.getLevel().dimension()!=CORRIDOR||!(e.getEntity() instanceof Monster))return;
        double y=e.getEntity().getY();
        if(e.getEntity() instanceof GuardianEntity||e.getEntity() instanceof ObserverEntity||e.getEntity() instanceof MahoragaEntity)return;
        if(e.getEntity() instanceof YujiEntity yuji&&yuji.familyMember())return;
        if(y>=SoulCorridorGenerator.IWATE_FLOOR){e.setCanceled(true);return;}
        if(e.getEntity() instanceof YujiEntity||e.getEntity() instanceof BrokenMahitoEntity||e.getEntity() instanceof MahitoLimbEntity)return;e.setCanceled(true);
    }
    @SubscribeEvent public static void protectBreak(BlockEvent.BreakEvent e){
        if(e.getLevel() instanceof Level level&&protectedBlock(level,e.getPos())){boolean flesh=e.getState().is(ProgressionContent.TRANSFIGURED_FLESH.get());boolean needle=e.getPlayer().getMainHandItem().is(Content.SOUL_SEPARATION_NEEDLE.get());if(!flesh||!needle){e.setCanceled(true);e.getPlayer().displayClientMessage(Component.literal(flesh?"无为转变改造肉块只能用无为转变·解离针挖掘":"灵魂通道内只有无为转变改造肉块可以被破坏"),true);}}}
    @SubscribeEvent public static void protectExplosion(ExplosionEvent.Detonate e){if(e.getLevel().dimension()==CORRIDOR)e.getAffectedBlocks().removeIf(pos->protectedBlock(e.getLevel(),pos));}
    @SubscribeEvent public static void protectPiston(net.minecraftforge.event.level.PistonEvent.Pre e){if(e.getLevel() instanceof Level level&&protectedBlock(level,e.getPos()))e.setCanceled(true);}
    @SubscribeEvent public static void protectMob(net.minecraftforge.event.entity.living.LivingDestroyBlockEvent e){if(protectedBlock(e.getEntity().level(),e.getPos()))e.setCanceled(true);}
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent e){REBUILD_QUEUE.clear();missingMahitoSince.clear();rebuildLevel=null;rebuildRequester=null;}
    public static void leave(ServerPlayer p){if(p.level().dimension()!=CORRIDOR)return;var tag=Curse.data(p);ServerLevel target=null;try{target=p.server.getLevel(ResourceKey.create(Registries.DIMENSION,new ResourceLocation(tag.getString("SoulReturnDimension"))));}catch(RuntimeException ignored){}if(target==null)target=p.server.overworld();BlockPos spawn=target.getSharedSpawnPos();double x=tag.contains("SoulReturnX")?tag.getDouble("SoulReturnX"):spawn.getX()+0.5,y=tag.contains("SoulReturnY")?tag.getDouble("SoulReturnY"):spawn.getY(),z=tag.contains("SoulReturnZ")?tag.getDouble("SoulReturnZ"):spawn.getZ()+0.5;Skills.closeDomain(p);Skills.cancelCharge(p);p.teleportTo(target,x,y,z,p.getYRot(),p.getXRot());p.getPersistentData().putLong("SukunaGateCooldown",target.getGameTime()+100);SoulCombat.clearTransient(p);}
    @SubscribeEvent public static void death(LivingDeathEvent e){
        if(e.isCanceled()||e.getEntity().level().isClientSide||!(e.getEntity() instanceof YujiEntity boss)||boss.decoy()||boss.familyMember())return;
        if(!(boss.level() instanceof ServerLevel level))return;
        for(ServerPlayer p:level.players())if(ready(p)&&boss.distanceToSqr(p)<128*128&&boss.participated(p.getUUID()))p.sendSystemMessage(Component.literal("虎杖残影已击败：拾取并吃下灵魂御厨子碎片，或留给已完成残秽凝练的女仆。"));
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent e){if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;if(p.tickCount%40==0&&p.level().dimension()==Level.OVERWORLD)ensureRuin(p);if(p.tickCount%100==0&&rebuildLevel==null&&p.level().dimension()==CORRIDOR){if(p.getY()>=990&&p.getY()<1030&&(evolved(p)||VoidJourney.evolved(p)||Journey.maidCleared(p,"YujiDefeated")))ensureMahitoWall(p.serverLevel(),p);if(p.getY()>=SoulCorridorGenerator.IWATE_FLOOR&&p.distanceToSqr(IwateVillage.HOME_X+64,IwateVillage.Y,IwateVillage.HOME_Z+64)<192*192)ensurePhotoChest(p);}}
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||rebuildLevel==null)return;int budget=1;while(budget-->0&&!REBUILD_QUEUE.isEmpty()){ChunkPos pos=REBUILD_QUEUE.removeFirst();SoulCorridorGenerator.rebuild(rebuildLevel.getChunk(pos.x,pos.z));}
        if(REBUILD_QUEUE.isEmpty()){ServerPlayer requester=rebuildRequester==null?null:rebuildLevel.getServer().getPlayerList().getPlayer(rebuildRequester);if(requester!=null)requester.sendSystemMessage(Component.literal("灵魂通道核心区域重建完成。未探索区域会按新岩手地形自然生成。"));rebuildLevel=null;rebuildRequester=null;}
    }
    private static int rebuild(net.minecraft.commands.CommandSourceStack source,ResourceLocation id){
        ServerLevel level=source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,id));if(level==null){source.sendFailure(Component.literal("找不到维度："+id));return 0;}if(level.dimension()!=CORRIDOR){source.sendFailure(Component.literal("当前版本只支持格式化 sukuna:soul_corridor，其他维度不会被破坏。"));return 0;}if(rebuildLevel!=null){source.sendFailure(Component.literal("已有维度正在重建，剩余区块："+REBUILD_QUEUE.size()));return 0;}
        List<Entity> entities=new ArrayList<>();for(Entity entity:level.getAllEntities())entities.add(entity);for(Entity entity:entities)if(!(entity instanceof ServerPlayer))entity.discard();SoulWorldState.get(level).reset();for(ServerPlayer visitor:new ArrayList<>(level.players()))leave(visitor);REBUILD_QUEUE.clear();for(int x=-8;x<=15;x++)for(int z=-8;z<=15;z++)REBUILD_QUEUE.add(new ChunkPos(x,z));rebuildLevel=level;ServerPlayer p=source.getPlayer();rebuildRequester=p==null?null:p.getUUID();source.sendSuccess(()->Component.literal("开始分批重建 "+id+"：共 "+REBUILD_QUEUE.size()+" 个区块。"),true);return 1;
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("sukuna").then(Commands.literal("rebuild").requires(s->s.hasPermission(2)).then(Commands.argument("dimension",ResourceLocationArgument.id()).executes(c->rebuild(c.getSource(),ResourceLocationArgument.getId(c,"dimension"))))));
        e.getDispatcher().register(Commands.literal("sukuna").then(Commands.literal("soul").then(Commands.literal("ruins").requires(s->s.hasPermission(2)).executes(c->RuinAtmosphere.build(c.getSource().getPlayerOrException())?1:0)).then(Commands.literal("leave").executes(c->{leave(c.getSource().getPlayerOrException());return 1;})).then(Commands.literal("room").executes(c->{TenShadows.revealRoom(c.getSource().getPlayerOrException());return 1;})).then(Commands.literal("status").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();p.sendSystemMessage(Component.literal("咒胎受肉："+SoulCombat.layers(p)+"/9；灵魂进食："+Curse.data(p).getInt("SoulFeasts")+"/300；"+Journey.guide(p)));return 1;})).then(Commands.literal("prepare_test").requires(s->s.hasPermission(2)).executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();var d=Curse.data(p);d.putBoolean("Memory",true);d.putInt("Fingers",20);d.putBoolean("Awakened",true);d.putBoolean("BuddhaDefeated",true);d.putBoolean("Nine",true);d.putInt("ResidueUpgrades",Math.max(34,d.getInt("ResidueUpgrades")));d.putInt("SoulLayers",9);Curse.apply(p);Curse.add(p,Curse.maximum(p));Curse.sync(p);p.sendSystemMessage(Component.literal("测试成长已准备。"));return 1;}))));
    }
}
