package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.*;

/** 所有封印、解封、库存和传送约束只由服务端裁决。 */
public final class PrisonSystem {
    public static final ResourceKey<Level> DIMENSION=ResourceKey.create(Registries.DIMENSION,new ResourceLocation("sukuna","prison_realm"));
    private static final Set<UUID> TRANSFERRING=new HashSet<>();
    private static final TicketType<UUID> TICKET=TicketType.create("sukuna_prison",Comparator.comparing(UUID::toString),40);
    public static boolean locked(Entity entity){
        if(entity==null||entity.level().isClientSide)return false;
        return entity.getServer()!=null&&PrisonStore.get(entity.getServer()).prisoner(entity.getUUID())!=null;
    }
    public static void registerRealm(PrisonRealmEntity realm){
        if(realm.getServer()==null||realm.owner()==null)return;
        PrisonStore.get(realm.getServer()).create(realm.getUUID(),realm.owner(),realm.level().dimension().location().toString(),realm.getX(),realm.getY(),realm.getZ());
    }
    @SubscribeEvent public static void chat(ServerChatEvent event){
        String text=PrisonRules.phrase(event.getRawText());boolean open=text.equals("狱门僵，开门");
        if(!open&&!text.equals("狱门僵，闭门"))return;
        event.setCanceled(true);ServerPlayer p=event.getPlayer();if(locked(p)){tell(p,"封印中无法控制狱门僵");return;}
        PrisonRealmEntity realm=p.level().getEntitiesOfClass(PrisonRealmEntity.class,p.getBoundingBox().inflate(32),r->p.getUUID().equals(r.owner()))
            .stream().min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
        if(realm==null){tell(p,"32格内没有你放出的狱门僵");return;}
        PrisonStore store=PrisonStore.get(p.server);
        if(open){
            if(store.occupied(realm.getUUID())){tell(p,"里面已经封印了生物，只能用邪去悔天梯破坏解封");return;}
            realm.open(true);tell(p,"狱门僵，开门 · 巨眼正在展开");
        }else{
            if(!realm.open()||realm.frame()<PrisonRules.OPEN_TICKS){tell(p,"请先开门，并等待巨眼完全展开");return;}
            ServerLevel prison=p.server.getLevel(DIMENSION);if(prison==null){tell(p,"封印维度未加载，本次没有收走任何物品；请重启世界");return;}
            registerRealm(realm);CompoundTag room=store.realm(realm.getUUID());prepareRoom(prison,room);int count=0;
            for(LivingEntity target:List.copyOf(p.serverLevel().getEntitiesOfClass(LivingEntity.class,realm.getBoundingBox().inflate(5),e->e.isAlive()&&!e.getUUID().equals(realm.owner())&&PrisonRules.inRange(e.distanceToSqr(realm))&&!locked(e)))){
                if(capture(realm,target,prison,room,store))count++;
            }
            realm.open(false);tell(p,"狱门僵，闭门 · 已封印 "+count+" 个生物");
        }
    }
    private static void prepareRoom(ServerLevel prison,CompoundTag room){
        int x=room.getInt("CellX"),z=room.getInt("CellZ");
        if(room.getBoolean("Built"))return;
        BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
        for(int dx=-16;dx<=16;dx++)for(int dz=-16;dz<=16;dz++)for(int y=63;y<=88;y++){
            if(y!=63&&y!=88&&Math.abs(dx)!=16&&Math.abs(dz)!=16)continue;
            prison.setBlock(pos.set(x+dx,y,z+dz),Blocks.BEDROCK.defaultBlockState(),2);
        }
        room.putBoolean("Built",true);PrisonStore.get(prison.getServer()).setDirty();
    }
    private static boolean capture(PrisonRealmEntity realm,LivingEntity entity,ServerLevel prison,CompoundTag room,PrisonStore store){
        if(!PrisonCurios.available(entity)){if(entity instanceof ServerPlayer p)tell(p,"饰品接口无法安全读取，本次未封印你，也未收走物品");return false;}
        CompoundTag record=new CompoundTag();record.putUUID("Id",entity.getUUID());record.putUUID("Realm",realm.getUUID());record.putBoolean("Player",entity instanceof ServerPlayer);
        record.putString("Origin",entity.level().dimension().location().toString());record.putDouble("X",entity.getX());record.putDouble("Y",entity.getY());record.putDouble("Z",entity.getZ());
        record.putInt("CellX",room.getInt("CellX"));record.putInt("CellZ",room.getInt("CellZ"));
        double x=room.getInt("CellX")+.5,z=room.getInt("CellZ")+.5;
        entity.stopRiding();entity.ejectPassengers();
        if(entity instanceof ServerPlayer player){
            CompoundTag snapshot=new CompoundTag();player.saveWithoutId(snapshot);record.put("Entity",snapshot);
            record.putInt("GameMode",player.gameMode.getGameModeForPlayer().getId());
            record.putFloat("Health",player.getHealth());record.putInt("Food",player.getFoodData().getFoodLevel());record.putFloat("Saturation",player.getFoodData().getSaturationLevel());
            store.prisoners.put(player.getUUID(),record);store.setDirty();
            PrisonInventory.capture(player,record);TechniqueInput.cancel(player);Skills.cancelCharge(player);MeleeCombo.cancel(player);
            transferPlayer(player,prison,x,64,z);player.setGameMode(GameType.ADVENTURE);player.setDeltaMovement(Vec3.ZERO);
            tell(player,"你已被狱门僵封印 · 物品暂存，等待外部的邪去悔天梯解封");
        }else{
            CompoundTag snapshot=new CompoundTag();entity.saveWithoutId(snapshot);snapshot.putString("id",net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString());snapshot.remove("Passengers");
            record.put("Entity",snapshot);record.putBoolean("CanUpdate",entity.canUpdate());
            Entity copy=EntityType.loadEntityRecursive(snapshot.copy(),prison,e->e);if(!(copy instanceof LivingEntity captive))return false;
            captive.moveTo(x,64,z,entity.getYRot(),0);if(captive instanceof Mob mob){mob.setNoAi(true);mob.setPersistenceRequired();mob.setTarget(null);}captive.setInvulnerable(true);captive.canUpdate(false);
            PrisonInventory.confiscate(captive,record);
            store.prisoners.put(entity.getUUID(),record);store.setDirty();
            if(!prison.addFreshEntity(captive)){store.prisoners.remove(entity.getUUID());return false;}
            entity.remove(Entity.RemovalReason.CHANGED_DIMENSION);
        }
        prison.getChunkSource().addRegionTicket(TICKET,new ChunkPos(BlockPos.containing(x,64,z)),2,realm.getUUID());store.setDirty();return true;
    }
    private static void transferPlayer(ServerPlayer p,ServerLevel level,double x,double y,double z){
        TRANSFERRING.add(p.getUUID());try{p.stopRiding();p.teleportTo(level,x,y,z,p.getYRot(),p.getXRot());p.fallDistance=0;}finally{TRANSFERRING.remove(p.getUUID());}
    }
    public static void release(PrisonRealmEntity realm){
        if(!(realm.level() instanceof ServerLevel level))return;
        registerRealm(realm);PrisonStore store=PrisonStore.get(level.getServer());CompoundTag room=store.realm(realm.getUUID());
        if(room==null)return;
        room.putBoolean("Released",true);room.putString("Dimension",level.dimension().location().toString());room.putDouble("X",realm.getX());room.putDouble("Y",realm.getY());room.putDouble("Z",realm.getZ());store.setDirty();
        for(CompoundTag record:List.copyOf(store.prisoners.values()))if(record.getUUID("Realm").equals(realm.getUUID()))restore(level.getServer(),store,record,room);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,realm.getX(),realm.getY()+.7,realm.getZ(),1,0,0,0,0);
    }
    private static ServerLevel destination(MinecraftServer server,CompoundTag room){
        ResourceLocation id=ResourceLocation.tryParse(room.getString("Dimension"));ServerLevel level=id==null?null:server.getLevel(ResourceKey.create(Registries.DIMENSION,id));return level==null?server.overworld():level;
    }
    private static Vec3 safeSpot(ServerLevel level,Entity entity,CompoundTag room){
        double x=room.getDouble("X"),y=room.getDouble("Y"),z=room.getDouble("Z");level.getChunk(BlockPos.containing(x,y,z));
        for(int dy=0;dy<16;dy++)for(int ring=1;ring<=4;ring++)for(int i=0;i<8;i++){
            double a=i*Math.PI/4;Vec3 at=new Vec3(x+Math.cos(a)*ring,y+dy,z+Math.sin(a)*ring);
            AABB box=entity.getDimensions(entity.getPose()).makeBoundingBox(at);
            if(level.noCollision(box)&&!level.getBlockState(BlockPos.containing(at).below()).isAir())return at;
        }
        return new Vec3(x,y+1,z);
    }
    private static void restore(MinecraftServer server,PrisonStore store,CompoundTag record,CompoundTag room){
        ServerLevel destination=destination(server,room);UUID id=record.getUUID("Id");
        if(record.getBoolean("Player")){
            ServerPlayer p=server.getPlayerList().getPlayer(id);if(p==null)return;
            Vec3 at=safeSpot(destination,p,room);transferPlayer(p,destination,at.x,at.y,at.z);
            PrisonInventory.restore(p,record);p.setGameMode(GameType.byId(record.getInt("GameMode")));p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
            p.setHealth(Math.min(p.getMaxHealth(),Math.max(1,record.getFloat("Health"))));p.getFoodData().setFoodLevel(record.getInt("Food"));p.getFoodData().setSaturation(record.getFloat("Saturation"));
            store.prisoners.remove(id);store.setDirty();tell(p,"狱门僵已被圣光破坏 · 封印解除，物品和经验已归还");
        }else{
            ServerLevel prison=server.getLevel(DIMENSION);if(prison==null)return;
            BlockPos cell=new BlockPos(record.getInt("CellX"),64,record.getInt("CellZ"));prison.getChunk(cell);Entity captive=prison.getEntity(id);
            // 只在牢房实体列表确实完成载入后恢复；外部模组清理了囚徒也能用账本找回。
            if(captive==null&&!prison.areEntitiesLoaded(new ChunkPos(cell).toLong()))return;
            Entity existing=destination.getEntity(id);
            if(existing!=null){if(captive!=null)captive.discard();store.prisoners.remove(id);store.setDirty();return;}
            Entity restored=EntityType.loadEntityRecursive(record.getCompound("Entity").copy(),destination,e->e);if(restored==null)return;
            Vec3 at=safeSpot(destination,restored,room);restored.moveTo(at.x,at.y,at.z,restored.getYRot(),restored.getXRot());restored.canUpdate(record.getBoolean("CanUpdate"));
            if(destination.addFreshEntity(restored)){if(captive!=null)captive.discard();store.prisoners.remove(id);store.setDirty();}
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;MinecraftServer server=event.getServer();PrisonStore store=PrisonStore.get(server);if(store.prisoners.isEmpty())return;
        ServerLevel prison=server.getLevel(DIMENSION);if(prison==null)return;
        Set<UUID> tickets=new HashSet<>();
        for(CompoundTag record:List.copyOf(store.prisoners.values())){
            UUID id=record.getUUID("Id"),realm=record.getUUID("Realm");CompoundTag room=store.realm(realm);if(room==null)continue;
            if(server.getTickCount()%20==0&&tickets.add(realm))prison.getChunkSource().addRegionTicket(TICKET,new ChunkPos(new BlockPos(record.getInt("CellX"),64,record.getInt("CellZ"))),2,realm);
            if(room.getBoolean("Released")){restore(server,store,record,room);continue;}
            if(record.getBoolean("Player")){
                ServerPlayer p=server.getPlayerList().getPlayer(id);if(p==null)continue;
                int x=record.getInt("CellX"),z=record.getInt("CellZ");
                if(p.level()!=prison||!PrisonRules.inside(p.getX(),p.getY(),p.getZ(),x,z))transferPlayer(p,prison,x+.5,64,z+.5);
                if(p.gameMode.getGameModeForPlayer()!=GameType.ADVENTURE)p.setGameMode(GameType.ADVENTURE);
                if(p.getAbilities().flying||p.getAbilities().mayfly){p.getAbilities().flying=false;p.getAbilities().mayfly=false;p.onUpdateAbilities();}
                p.fallDistance=0;p.clearFire();p.setAirSupply(p.getMaxAirSupply());
                if(p.tickCount%10==0){PrisonInventory.confiscate(p,record);PrisonInventory.sync(p);store.setDirty();p.getFoodData().setFoodLevel(20);p.setHealth(Math.min(p.getMaxHealth(),Math.max(1,record.getFloat("Health"))));}
                if(p.tickCount%80==0)p.displayClientMessage(Component.literal("狱门僵封印中 · 传送与物品使用已封锁"),true);
            }else{
                Entity captive=prison.getEntity(id);if(captive!=null){captive.canUpdate(false);captive.setDeltaMovement(Vec3.ZERO);
                    int x=record.getInt("CellX"),z=record.getInt("CellZ");if(!PrisonRules.inside(captive.getX(),captive.getY(),captive.getZ(),x,z))captive.teleportTo(x+.5,64,z+.5);
                }
            }
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void joined(EntityJoinLevelEvent e){
        if(!(e.getLevel() instanceof ServerLevel level)||!(e.getEntity() instanceof LivingEntity living)||living instanceof net.minecraft.world.entity.player.Player)return;
        CompoundTag record=PrisonStore.get(level.getServer()).prisoner(living.getUUID());if(record==null)return;
        if(level.dimension().equals(DIMENSION)){
            living.canUpdate(false);living.setInvulnerable(true);if(living instanceof Mob mob){mob.setNoAi(true);mob.setPersistenceRequired();}
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void targeting(LivingChangeTargetEvent e){if(locked(e.getEntity())||locked(e.getNewTarget()))e.setNewTarget(null);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void explosion(net.minecraftforge.event.level.ExplosionEvent.Detonate e){if(e.getLevel().dimension().equals(DIMENSION)){e.getAffectedBlocks().clear();e.getAffectedEntities().clear();}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void travel(EntityTravelToDimensionEvent e){if(!TRANSFERRING.contains(e.getEntity().getUUID())&&locked(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void teleport(EntityTeleportEvent e){if(!TRANSFERRING.contains(e.getEntity().getUUID())&&locked(e.getEntity())&&e.isCancelable())e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void command(CommandEvent e){Entity sender=e.getParseResults().getContext().getSource().getEntity();if(locked(sender)){e.setCanceled(true);if(sender instanceof ServerPlayer p)tell(p,"封印中不能执行指令或小地图传送");}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void damage(LivingAttackEvent e){if(locked(e.getEntity())||locked(e.getSource().getEntity()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void death(LivingDeathEvent e){if(locked(e.getEntity())){e.setCanceled(true);e.getEntity().setHealth(Math.max(1,e.getEntity().getHealth()));}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void use(PlayerInteractEvent e){if(locked(e.getEntity())&&e.isCancelable())e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(AttackEntityEvent e){if(locked(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void pickup(EntityItemPickupEvent e){if(locked(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void breakBlock(BlockEvent.BreakEvent e){if(locked(e.getPlayer())||e.getLevel() instanceof ServerLevel l&&l.dimension().equals(DIMENSION))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void place(BlockEvent.EntityPlaceEvent e){if(locked(e.getEntity())||e.getLevel() instanceof ServerLevel l&&l.dimension().equals(DIMENSION))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void livingTick(LivingEvent.LivingTickEvent e){if(!(e.getEntity() instanceof net.minecraft.world.entity.player.Player)&&locked(e.getEntity()))e.setCanceled(true);}
    private static void tell(ServerPlayer p,String text){p.sendSystemMessage(Component.literal(text));}
    private PrisonSystem(){}
}
