package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** 言灵恩赐：服务端成员、目标、传送和序列化，实体 AI 仍可使用原生技能。 */
@Mod.EventBusSubscriber(modid="sukuna")
public final class Legion {
    private static final String OWNER="AuthorityLegionOwner";
    private static final Map<UUID,LinkedHashSet<UUID>> TARGETS=new HashMap<>();
    private static final Map<UUID,Integer> NEXT=new HashMap<>();
    private static final Set<UUID> RELEASING=new HashSet<>();
    private static final Map<UUID,Mob> UNLOADED=new HashMap<>();
    @SubscribeEvent public static void unloaded(net.minecraftforge.event.entity.EntityLeaveLevelEvent e){if(e.getLevel() instanceof ServerLevel&&e.getEntity() instanceof Mob mob&&member(mob)&&mob.getHealth()>0&&mob.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)UNLOADED.put(mob.getUUID(),mob);}
    @SubscribeEvent public static void recoverUnloaded(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        for(Mob mob:List.copyOf(UNLOADED.values())){
            UNLOADED.remove(mob.getUUID());if(!(mob.level() instanceof ServerLevel level))continue;
            var state=LegionStore.get(level.getServer());var army=state.army(ownerId(mob));ListTag stored=army.getList("Stored",Tag.TAG_COMPOUND);
            boolean exists=false;for(int i=0;i<stored.size();i++)if(stored.getCompound(i).hasUUID("UUID")&&stored.getCompound(i).getUUID("UUID").equals(mob.getUUID()))exists=true;
            if(exists||stored.size()>=AuthorityRules.MAX_MEMBERS)continue;
            var actor=MaidBridge.caster(mob);if(actor!=null)AuthoritySkills.cancel(actor);
            var versions=army.getCompound("Versions");int rev=Math.max(versions.getInt(mob.getStringUUID()),mob.getPersistentData().getInt("AuthorityRevision"))+1;mob.getPersistentData().putInt("AuthorityRevision",rev);versions.putInt(mob.getStringUUID(),rev);army.put("Versions",versions);
            CompoundTag saved=mob.saveWithoutId(new CompoundTag());saved.putString("id",net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).toString());saved.remove("Passengers");stored.add(saved);army.put("Stored",stored);state.setDirty();
            ServerPlayer p=owner(mob);if(p!=null&&p.isAlive()&&!p.hasDisconnected()&&!army.getBoolean("RecallPending"))releaseAll(p);
        }
    }
    public static UUID ownerId(Entity mob){return mob!=null&&mob.getPersistentData().hasUUID(OWNER)?mob.getPersistentData().getUUID(OWNER):null;}
    public static ServerPlayer owner(Entity mob){UUID id=ownerId(mob);return id!=null&&mob.level() instanceof ServerLevel l?l.getServer().getPlayerList().getPlayer(id):null;}
    public static boolean member(Entity mob){return ownerId(mob)!=null;}
    public static boolean allied(ServerPlayer caster,LivingEntity target){ServerPlayer p=caster instanceof MaidBridge.Actor a?Authority.owner(a):caster;if(p==null)return false;return p.getUUID().equals(ownerId(target))||p==target&&caster!=p;}
    public static List<Mob> loaded(ServerPlayer p){List<Mob> list=new ArrayList<>();for(ServerLevel level:p.server.getAllLevels())for(Entity e:level.getAllEntities())if(e instanceof Mob mob&&p.getUUID().equals(ownerId(mob))&&mob.isAlive())list.add(mob);return list;}
    public static int stored(ServerPlayer p){return LegionStore.get(p.server).army(p.getUUID()).getList("Stored",Tag.TAG_COMPOUND).size();}
    public static int count(ServerPlayer p){return roster(p).size();}
    private static ListTag roster(ServerPlayer p){return LegionStore.get(p.server).army(p.getUUID()).getList("Members",Tag.TAG_INT_ARRAY);}
    public static void recruit(ServerPlayer p,LivingEntity entity){
        if(!Authority.unlocked(p)||!p.isAlive()||p.isSpectator()||ObserverRitual.active(p)||!(entity instanceof Mob mob)||!mob.isAlive()||mob.distanceToSqr(p)>36||!p.hasLineOfSight(mob))return;
        if(mob instanceof ObserverEntity||Authority.echo(mob))return;
        UUID existing=ownerId(mob);if(existing!=null&&!existing.equals(p.getUUID()))return;
        if(mob instanceof TamableAnimal pet&&pet.isTame()&&!p.getUUID().equals(pet.getOwnerUUID()))return;
        var store=LegionStore.get(p.server);var army=store.army(p.getUUID());ListTag members=roster(p);
        if(existing==null&&members.size()>=AuthorityRules.MAX_MEMBERS){p.displayClientMessage(Component.literal("魂番已满（128名）"),true);return;}
        if(existing==null){members.add(NbtUtils.createUUID(mob.getUUID()));army.put("Members",members);store.setDirty();}
        army.putBoolean("RecallPending",false);store.setDirty();
        mob.getPersistentData().putUUID(OWNER,p.getUUID());if(existing==null){var versions=army.getCompound("Versions");versions.putInt(mob.getStringUUID(),1);army.put("Versions",versions);mob.getPersistentData().putInt("AuthorityRevision",1);store.setDirty();}mob.setPersistenceRequired();mob.setTarget(null);mob.setLastHurtByMob(null);mob.setLastHurtByPlayer(null);
        if(mob instanceof TamableAnimal pet){pet.setOwnerUUID(p.getUUID());pet.setTame(true);pet.setOrderedToSit(false);}
        var actor=MaidBridge.actor(mob);AuthoritySkills.inherit(p,actor);Skills.protectAlly(p,mob.getUUID());SoulCombat.protect(p,mob);
        mob.clearFire();for(String key:List.of("SukunaSoulFire","SukunaNoHeal","SukunaSoulSlashOwner","SukunaVoidIgnitionUntil"))mob.getPersistentData().remove(key);
        p.displayClientMessage(Component.literal("言灵·恩赐："+mob.getName().getString()),true);AuthorityPackets.sync(p);
    }
    public static void clearTargets(ServerPlayer p){TARGETS.remove(p.getUUID());for(Mob mob:loaded(p)){mob.setTarget(null);var actor=MaidBridge.caster(mob);if(actor!=null){actor.aim=null;AuthoritySkills.cancel(actor);}}AuthorityPackets.sync(p);}
    public static List<Integer> targetIds(ServerPlayer p){List<Integer> out=new ArrayList<>();var selected=TARGETS.getOrDefault(p.getUUID(),new LinkedHashSet<>());selected.removeIf(id->!(p.serverLevel().getEntity(id) instanceof LivingEntity e)||!e.isAlive());for(UUID id:selected)if(p.serverLevel().getEntity(id) instanceof LivingEntity e&&e.isAlive())out.add(e.getId());return out;}
    public static void select(ServerPlayer p,LivingEntity target){
        if(!Authority.unlocked(p)||!p.isAlive()||p.isSpectator()||ObserverRitual.active(p)||target.distanceToSqr(p)>96*96||!p.hasLineOfSight(target)||!CombatSettings.canHit(p,target))return;
        var set=TARGETS.computeIfAbsent(p.getUUID(),id->new LinkedHashSet<>());
        if(!set.remove(target.getUUID())&&set.size()<AuthorityRules.MAX_TARGETS)set.add(target.getUUID());AuthorityPackets.sync(p);
    }
    public static LivingEntity commanded(Mob mob){ServerPlayer p=owner(mob);return p==null?null:target(p,mob);}
    private static LivingEntity target(ServerPlayer p,Mob mob){
        var set=TARGETS.get(p.getUUID());if(set==null)return null;
        List<LivingEntity> found=new ArrayList<>();for(UUID id:set)if(p.serverLevel().getEntity(id) instanceof LivingEntity e&&e.isAlive()&&CombatSettings.canHit(p,e)&&e.distanceToSqr(p)<128*128)found.add(e);
        if(found.isEmpty())return null;return found.get(Math.floorMod(mob.getUUID().hashCode(),found.size()));
    }
    private static Vec3 landing(ServerPlayer p,Entity mob){
        for(int radius=2;radius<=5;radius++)for(int i=0;i<12;i++){double a=i*Math.PI/6;Vec3 q=p.position().add(Math.cos(a)*radius,0,Math.sin(a)*radius);for(int y=1;y>=-2;y--){Vec3 at=q.add(0,y,0);var box=mob.getBoundingBox().move(at.subtract(mob.position()));var below=net.minecraft.core.BlockPos.containing(at).below();if(p.serverLevel().getWorldBorder().isWithinBounds(below)&&p.serverLevel().noCollision(mob,box)&&!p.serverLevel().getBlockState(below).getCollisionShape(p.serverLevel(),below).isEmpty()&&p.serverLevel().getFluidState(below).isEmpty())return at;}}
        return p.position();
    }
    private static void recall(ServerPlayer p,Mob mob){
        Vec3 at=landing(p,mob);mob.stopRiding();mob.ejectPassengers();mob.getNavigation().stop();mob.setTarget(null);mob.fallDistance=0;mob.setDeltaMovement(Vec3.ZERO);
        mob.teleportTo(p.serverLevel(),at.x,at.y,at.z,Set.of(),mob.getYRot(),mob.getXRot());
    }
    public static void toggleStorage(ServerPlayer p){if(stored(p)>0)releaseAll(p);else storeAll(p);}
    public static void storeAll(ServerPlayer p){
        if(!Authority.unlocked(p))return;var state=LegionStore.get(p.server);var army=state.army(p.getUUID());ListTag stored=army.getList("Stored",Tag.TAG_COMPOUND);
        for(Mob mob:loaded(p)){
            if(stored.size()>=AuthorityRules.MAX_MEMBERS)break;
            // 拒绝把骑乘关系中的其他生物偷带入魂番。
            mob.stopRiding();mob.ejectPassengers();CompoundTag saved=new CompoundTag();
            var actor=MaidBridge.caster(mob);if(actor!=null){AuthoritySkills.cancel(actor);actor.aim=null;}
            var versions=army.getCompound("Versions");int version=Math.max(versions.getInt(mob.getStringUUID()),mob.getPersistentData().getInt("AuthorityRevision"))+1;mob.getPersistentData().putInt("AuthorityRevision",version);
            if(!mob.save(saved))continue;
            versions.putInt(mob.getStringUUID(),version);army.put("Versions",versions);
            stored.add(saved);army.put("Stored",stored);state.setDirty();mob.discard();
        }
        army.putBoolean("RecallPending",true);state.setDirty();AuthorityPackets.sync(p);
    }
    public static void releaseAll(ServerPlayer p){
        if(!Authority.unlocked(p)||!p.isAlive()||p.isSpectator())return;
        var state=LegionStore.get(p.server);var army=state.army(p.getUUID());ListTag stored=army.getList("Stored",Tag.TAG_COMPOUND),remaining=new ListTag();
        army.putBoolean("RecallPending",false);
        for(int i=0;i<stored.size();i++){
            CompoundTag tag=stored.getCompound(i);UUID id=tag.hasUUID("UUID")?tag.getUUID("UUID"):null;boolean exists=false;
            if(id!=null)for(ServerLevel l:p.server.getAllLevels())if(l.getEntity(id)!=null){exists=true;break;}
            if(exists){remaining.add(tag);continue;}
            Entity entity=EntityType.loadEntityRecursive(tag,p.serverLevel(),e->e);
            if(!(entity instanceof Mob mob)){remaining.add(tag);continue;}
            Vec3 pos=landing(p,mob);mob.moveTo(pos.x,pos.y,pos.z,p.getYRot(),0);mob.getPersistentData().putUUID(OWNER,p.getUUID());mob.setPersistenceRequired();
            RELEASING.add(mob.getUUID());try{if(!p.serverLevel().tryAddFreshEntityWithPassengers(mob))remaining.add(tag);}finally{RELEASING.remove(mob.getUUID());}
        }
        army.put("Stored",remaining);state.setDirty();AuthorityPackets.sync(p);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interact(PlayerInteractEvent.EntityInteract e){if(e.getItemStack().is(Authority.CURSE.get())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);if(e.getEntity() instanceof ServerPlayer p&&e.getTarget() instanceof LivingEntity living)recruit(p,living);}}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void tick(LivingEvent.LivingTickEvent e){
        if(!(e.getEntity() instanceof Mob mob)||mob.level().isClientSide||!member(mob)||!mob.isAlive())return;
        ServerPlayer p=owner(mob);if(p==null||!Authority.unlocked(p)||!p.isAlive())return;
        if(LegionStore.get(p.server).army(p.getUUID()).getBoolean("RecallPending")){storeAll(p);return;}
        if(ObserverRitual.active(p)){mob.setTarget(null);return;}
        if(mob.level()!=p.level()||AuthorityRules.shouldRecall(mob.distanceToSqr(p),mob.getPersistentData().getBoolean("AuthorityRecall"))){mob.getPersistentData().remove("AuthorityRecall");recall(p,mob);return;}
        LivingEntity t=target(p,mob);mob.setTarget(t);var actor=MaidBridge.actor(mob);actor.aim=t;actor.syncPosition();
        if(SixfoldFinale.active(actor)){SixfoldFinale.heartbeat(actor);return;}
        if(mob.tickCount%10!=0)return;
        AuthoritySkills.inherit(p,actor);
        if(t==null){if(mob.distanceToSqr(p)>16)mob.getNavigation().moveTo(p,1.2);return;}
        mob.getLookControl().setLookAt(t,90,90);
        if(AuthoritySkills.busy(actor))return;
        int bloodFollowup=BloodArts.followup(actor);if(bloodFollowup>=0){AuthoritySkills.cast(actor,bloodFollowup);return;}
        var skills=AuthoritySkills.owned(actor);int start=NEXT.getOrDefault(mob.getUUID(),Math.floorMod(mob.getId(),Math.max(1,skills.size())));
        for(int n=0;n<skills.size();n++){int index=(start+n)%skills.size(),skill=skills.get(index);if(!AuthoritySkills.ready(actor,skill))continue;NEXT.put(mob.getUUID(),index+1);AuthoritySkills.cast(actor,skill);break;}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void targetChange(LivingChangeTargetEvent e){if(!member(e.getEntity()))return;ServerPlayer p=owner(e.getEntity());if(e.getEntity() instanceof Mob mob)e.setNewTarget(p==null?null:target(p,mob));}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void hurt(LivingAttackEvent e){
        Entity source=e.getSource().getEntity();ServerPlayer p=source instanceof ServerPlayer s?s:owner(source);
        UUID commander=source instanceof Player player?player.getUUID():ownerId(source);
        if(commander!=null&&(commander.equals(ownerId(e.getEntity()))||member(source)&&commander.equals(e.getEntity().getUUID()))){e.setCanceled(true);return;}
        if(source instanceof LivingEntity mob&&member(mob)&&!CombatSettings.scripted(e.getSource())&&(p==null||!Authority.unlocked(p))){e.setCanceled(true);return;}
        // 原生技能同样从军团池扣费，普通近战不另外扣费。
        if(source instanceof LivingEntity mob&&member(mob)&&!CombatSettings.scripted(e.getSource())&&p!=null&&!e.getSource().is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)){long now=p.level().getGameTime();if(mob.getPersistentData().getLong("AuthorityNativePaid")!=now){if(!Curse.spend(p,10))e.setCanceled(true);else mob.getPersistentData().putLong("AuthorityNativePaid",now);}}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void damaged(LivingDamageEvent e){if(member(e.getEntity())&&!e.isCanceled()&&e.getAmount()>0&&e.getEntity() instanceof Mob mob){ServerPlayer p=owner(mob);if(p!=null&&p.isAlive()&&p.level()==mob.level()){Vec3 at=landing(p,mob);mob.teleportTo(at.x,at.y,at.z);mob.fallDistance=0;mob.getNavigation().stop();}else mob.getPersistentData().putBoolean("AuthorityRecall",true);}}
    @SubscribeEvent public static void joined(EntityJoinLevelEvent e){
        if(!(e.getLevel() instanceof ServerLevel level)||!member(e.getEntity())||!(e.getEntity() instanceof Mob mob))return;
        var army=LegionStore.get(level.getServer()).army(ownerId(mob));
        if(!RELEASING.contains(mob.getUUID())){
            if(mob.getPersistentData().getInt("AuthorityRevision")<army.getCompound("Versions").getInt(mob.getStringUUID())){e.setCanceled(true);return;}
            var stored=army.getList("Stored",Tag.TAG_COMPOUND);for(int i=0;i<stored.size();i++)if(stored.getCompound(i).hasUUID("UUID")&&stored.getCompound(i).getUUID("UUID").equals(mob.getUUID())){e.setCanceled(true);return;}
        }
        mob.setPersistenceRequired();
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void died(LivingDeathEvent e){if(!(e.getEntity().level() instanceof ServerLevel level)||!member(e.getEntity()))return;var state=LegionStore.get(level.getServer());var army=state.army(ownerId(e.getEntity()));var ids=army.getList("Members",Tag.TAG_INT_ARRAY);ids.removeIf(tag->NbtUtils.loadUUID(tag).equals(e.getEntity().getUUID()));army.put("Members",ids);state.setDirty();NEXT.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){if(Authority.unlocked(p))storeAll(p);TARGETS.remove(p.getUUID());}}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p&&Authority.unlocked(p))for(Mob mob:loaded(p))recall(p,mob);}
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent e){TARGETS.clear();NEXT.clear();RELEASING.clear();UNLOADED.clear();AuthoritySkills.clear();}
    private Legion(){}
}
