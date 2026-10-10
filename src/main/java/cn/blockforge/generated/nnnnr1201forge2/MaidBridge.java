package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.registries.ForgeRegistries;

/** 不引用女仆类；可选联动为每个实体提供独立施术上下文，共用玩家技能队列。 */
public final class MaidBridge {
    private static final Map<UUID,Actor> ACTORS=new HashMap<>();
    private static final UUID HEALTH=UUID.fromString("dc148a77-3c9b-4775-8b36-e02d40832101"),ATTACK=UUID.fromString("dc148a77-3c9b-4775-8b36-e02d40832102");
    public static boolean isMaid(Entity entity){var id=entity==null?null:ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());return id!=null&&id.getNamespace().equals("touhou_little_maid")&&id.getPath().equals("maid");}
    public static boolean isCaster(Player p){return p instanceof Actor;}
    public static LivingEntity body(ServerPlayer p){return p instanceof Actor a?a.maid:p;}
    public static Actor caster(Entity entity){if(entity instanceof Actor a)return a;return entity==null?null:ACTORS.get(entity.getUUID());}
    public static Actor actor(LivingEntity maid){
        Actor old=ACTORS.get(maid.getUUID());
        if(old!=null&&old.maid==maid&&old.level()==maid.level())return old;
        if(old!=null)old.retire();
        Actor actor=new Actor((ServerLevel)maid.level(),maid);ACTORS.put(maid.getUUID(),actor);return actor;
    }
    public static boolean modCreature(LivingEntity target){var id=ForgeRegistries.ENTITY_TYPES.getKey(target.getType());return id!=null&&id.getNamespace().equals("sukuna");}
    public static boolean recovering(Player p){return isCaster(p)&&Curse.data(p).getBoolean("MaidRecovering");}
    public static boolean domainReady(ServerPlayer p){return !isCaster(p)||!Curse.active(p,"MaidDomainCooldown");}
    public static void domainOpened(ServerPlayer p){if(isCaster(p))Curse.status(p,"MaidDomainCooldown",ShrineRules.MAID_DOMAIN_COOLDOWN_TICKS);}
    public static ServerPlayer owner(Actor actor){return actor.maid instanceof TamableAnimal pet&&pet.getOwnerUUID()!=null?actor.server.getPlayerList().getPlayer(pet.getOwnerUUID()):null;}
    public static boolean hasMemory(Actor actor){return Curse.data(actor).getBoolean("Memory");}
    public static boolean consumeMemory(Actor actor){
        if(!BranchRules.acceptMemory(hasMemory(actor),Uraume.unlocked(actor),true,false))return false;
        Curse.data(actor).putBoolean("Memory",true);Curse.data(actor).putBoolean("MemoryGiven",true);Curse.apply(actor);
        return true;
    }
    public static void recordBossDefeat(Actor actor,LivingEntity victim){
        CompoundTag maid=Curse.data(actor);String flag=null;
        if(victim instanceof BuddhaEntity)flag="BuddhaDefeated";
        else if(victim instanceof YujiEntity y&&!y.decoy()&&!y.familyMember())flag="YujiDefeated";
        else if(victim instanceof BrokenMahitoEntity)flag="BrokenMahitoDefeated";
        else if(victim instanceof VoidBossEntity)flag="VoidBossDefeated";
        if(flag==null)return;maid.putBoolean(flag,true);
        ServerPlayer owner=owner(actor);if(owner!=null){Curse.data(owner).putBoolean(flag,true);Curse.sync(owner);}
        Curse.sync(actor);
    }
    public static void rememberCondensed(Actor actor){
        if(SoulJourney.ready(actor)&&actor.maid instanceof TamableAnimal pet&&pet.getOwnerUUID()!=null)SoulWorldState.get(actor.server.overworld()).markMaidCondensed(pet.getOwnerUUID());
    }
    private static void syncOwnerFlags(Actor actor){
        ServerPlayer owner=owner(actor);if(owner==null)return;CompoundTag maid=Curse.data(actor),player=Curse.data(owner);
        for(String flag:List.of("BuddhaDefeated","YujiDefeated","BrokenMahitoDefeated","VoidBossDefeated"))if(maid.getBoolean(flag))player.putBoolean(flag,true);
        if(maid.getBoolean("VoidBossDefeated")&&maid.getBoolean("VoidSettlementR60"))player.putBoolean("VoidSettlementR60",true);
    }
    public static boolean hasCondensedMaid(ServerPlayer p){
        var state=SoulWorldState.get(p.server.overworld());if(state.maidCondensed(p.getUUID()))return true;
        for(ServerLevel level:p.server.getAllLevels())for(Entity entity:level.getAllEntities())if(entity instanceof TamableAnimal pet&&isMaid(pet)&&pet.isAlive()&&p.getUUID().equals(pet.getOwnerUUID())&&SoulJourney.ready(actor(pet))){state.markMaidCondensed(p.getUUID());return true;}
        return false;
    }
    public static boolean canHit(Actor actor,LivingEntity target){
        LivingEntity maid=actor.maid;if(target==null||target==maid||!target.isAlive()||target.isRemoved()||target.isSpectator()||target.level()!=maid.level()||EternalLove.settling(maid))return false;
        if(target instanceof GuardianEntity g&&g.peacefulResident())return false;
        if(EternalLove.protects(actor,target))return false;
        ServerPlayer owner=owner(actor);
        if(owner!=null&&EternalLove.protects(owner,target))return false;
        boolean duel=EternalLove.duel(maid);
        if(target instanceof Player player){
            boolean own=maid instanceof TamableAnimal pet&&target.getUUID().equals(pet.getOwnerUUID());
            int relation=owner==null?CombatSettings.DEFAULT:CombatSettings.relation(owner,target.getUUID());
            return MaidCombatRules.playerTarget(duel,own,player.isCreative(),player.isSpectator(),actor.server.isPvpAllowed(),relation==CombatSettings.ENEMY);
        }
        if(duel)return false;
        if(modCreature(target))return true;
        if(target.isAlliedTo(maid)||target instanceof TamableAnimal t&&t.isTame())return false;
        if(maid instanceof Mob mob&&(target==mob.getTarget()||target==mob.getLastHurtByMob()))return true;
        return target instanceof net.minecraft.world.entity.monster.Enemy;
    }
    private static void modifier(LivingEntity maid,Attribute attribute,UUID id,double amount){var a=maid.getAttribute(attribute);if(a==null)return;var old=a.getModifier(id);if(old!=null&&Math.abs(old.getAmount()-amount)<0.00001)return;a.removeModifier(id);if(amount!=0)a.addTransientModifier(new AttributeModifier(id,"御厨子成长",amount,AttributeModifier.Operation.ADDITION));}
    public static void apply(Actor a){
        modifier(a.maid,Attributes.MAX_HEALTH,HEALTH,Curse.healthBonus(a));modifier(a.maid,Attributes.ATTACK_DAMAGE,ATTACK,Curse.attackBonus(a));
        a.getAttribute(Attributes.MAX_HEALTH).setBaseValue(a.maid.getMaxHealth());
        if(a.maid.getAttribute(Attributes.ATTACK_DAMAGE)!=null)a.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(a.maid.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }
    public static ServerPlayer progression(LivingEntity living){return living instanceof ServerPlayer p?p:isMaid(living)&&living.level() instanceof ServerLevel?actor(living):null;}
    public static ServerPlayer find(ServerLevel level,UUID id){ServerPlayer player=level.getServer().getPlayerList().getPlayer(id);if(player!=null)return player;var entity=level.getEntity(id);return entity instanceof LivingEntity living&&isMaid(living)?actor(living):null;}
    public static final class Actor extends FakePlayer {
        public final LivingEntity maid;
        public LivingEntity aim;
        private Actor(ServerLevel level,LivingEntity maid){super(level,new GameProfile(maid.getUUID(),"ShrineMaid"));this.maid=maid;setId(maid.getId());progress(maid);refreshDimensions();syncPosition();apply(this);}
        public void syncPosition(){setPos(maid.getX(),maid.getY(),maid.getZ());setYRot(maid.getYRot());setXRot(maid.getXRot());tickCount=maid.tickCount;if(aim!=null){Vec3 d=aim.getEyePosition().subtract(maid.getEyePosition());setYRot((float)(Math.toDegrees(Math.atan2(d.z,d.x))-90));setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));}}
        @Override public CompoundTag getPersistentData(){return maid==null?super.getPersistentData():maid.getPersistentData();}
        @Override public float getAttackStrengthScale(float partial){return 1;}
        @Override public float getStandingEyeHeight(Pose pose,EntityDimensions dimensions){return maid==null?super.getStandingEyeHeight(pose,dimensions):maid.getEyeHeight();}
        @Override public Iterable<ItemStack> getArmorSlots(){return maid==null?super.getArmorSlots():maid.getArmorSlots();}
        @Override public ItemStack getItemBySlot(EquipmentSlot slot){return maid==null?super.getItemBySlot(slot):maid.getItemBySlot(slot);}
        @Override public boolean isAlive(){return maid==null?super.isAlive():!isRemoved()&&maid.isAlive()&&!maid.isRemoved();}
        @Override public float getHealth(){return maid==null?super.getHealth():maid.getHealth();}
        @Override public void heal(float amount){if(maid!=null)maid.heal(amount);}
        @Override public void setHealth(float health){if(maid==null)super.setHealth(health);else maid.setHealth(health);}
        @Override public void sendSystemMessage(Component message){}
        @Override public void displayClientMessage(Component message,boolean actionBar){}
        @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}
        @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return maid!=null&&maid.hurt(source,amount);}
        @Override public void teleportTo(double x,double y,double z){if(maid!=null)maid.teleportTo(x,y,z);setPos(x,y,z);}
        void retire(){Skills.releaseCaster(this);GuardianPlayerSkills.cancel(this);setRemoved(RemovalReason.UNLOADED_TO_CHUNK);}
    }
    public static CompoundTag progress(LivingEntity maid){var root=maid.getPersistentData();if(!root.contains("SukunaProgress")){var tag=new CompoundTag();tag.putDouble("Energy",1000);root.put("SukunaProgress",tag);}return root.getCompound("SukunaProgress");}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(Actor a:List.copyOf(ACTORS.values())){if(!a.isAlive()||a.maid.level()!=a.level()){a.retire();ACTORS.remove(a.getUUID(),a);continue;}a.syncPosition();syncOwnerFlags(a);if(Curse.data(a).getBoolean("Memory"))Journey.tickCaster(a);if(a.maid.tickCount%20==0){Curse.tickCaster(a);apply(a);VoidJourney.repairOldVictory(a);GuardianProgress.deliverMaid(a);}}
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public static void damaged(LivingHurtEvent event){
        Actor a=caster(event.getEntity());if(a==null||event.getEntity()==a)return;
        // 守护者防御招式对真实女仆承伤也生效。
        GuardianPlayerSkills.guardCaster(a,event);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void bossDefeated(LivingDeathEvent event){
        if(event.isCanceled()||event.getEntity().level().isClientSide)return;
        ServerPlayer killer=CombatSettings.caster(event.getSource());
        if(killer instanceof Actor actor)recordBossDefeat(actor,event.getEntity());
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event){ACTORS.clear();}
    private MaidBridge(){}
}
