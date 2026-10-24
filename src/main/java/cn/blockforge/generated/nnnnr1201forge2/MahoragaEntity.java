package cn.blockforge.generated.nnnnr1201forge2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class MahoragaEntity extends Monster {
   public static final double HEALTH = 180000.0D;
   public static final double ARMOR = 24.0D;
   public static final int CELLS = 4;
   public static final int FLEE_RANGE = 500;
   public static final int CLEAR_RANGE = 64;
   public static final int HITS_PER_TURN = 6;
   public static final int CLEAR_HITS = 24;
   private static final int HIT_GAP = 4;
   public static final int ADAPT_TICKS = 1200;
   public static final int ADAPT_MAX = 6;
   public static final double ADAPT_DAMAGE = 0.2D;
   public static final double ALLY_HEALTH = 1200.0D;
   public static final int OWN_CLEAR = MahoragaAdaptStore.CLEAR;
   public static final int OWN_MASK_BITS = 24;
   public static final int ACT_NONE = 0;
   public static final int ACT_EMERGE = 1;
   public static final int ACT_CLEAVE = 2;
   public static final int ACT_SWEEP = 3;
   public static final int ACT_SLAM = 4;
   public static final int ACT_THRUST = 5;
   public static final int ACT_LEAP = 6;
   public static final int ACT_WHEEL = 7;
   public static final int RITUAL_NONE = 0;
   public static final int RITUAL_RUNNING = 1;
   public static final int RITUAL_FAILED = 2;
   public static final int RITUAL_DONE = 3;
   private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(MahoragaEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(MahoragaEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> IMMUNE = SynchedEntityData.defineId(MahoragaEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> OWN_MASK = SynchedEntityData.defineId(MahoragaEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> ALLY = SynchedEntityData.defineId(MahoragaEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> WHEEL = SynchedEntityData.defineId(MahoragaEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> CELL = SynchedEntityData.defineId(MahoragaEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> RITUAL = SynchedEntityData.defineId(MahoragaEntity.class, EntityDataSerializers.INT);
   private final ServerBossEvent bar = new ServerBossEvent(Component.literal("\u516b\u63e1\u5251\u5f02\u6212\u795e\u5c06\u00b7\u9b54\u865a\u7f57"), BossBarColor.YELLOW, BossBarOverlay.NOTCHED_20);
   private final int[] hits = new int[AttackTypes.SLOTS];
   private final int[] lastHit = new int[AttackTypes.SLOTS];
   private UUID summoner;
   private int rest;
   private int leapCooldown;
   private int wheelFlash;
   private int wheelSpin;
   private long ritualStart;
   private UUID allyOwner;
   private int ownerFriendlyNotice;
   private final Map<String, Integer> ownHits = new HashMap();

   public MahoragaEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
      this.xpReward = 8000;
   }

   public static void attributes(EntityAttributeCreationEvent e) {
      e.put((EntityType)VoidContent.MAHORAGA.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 400000.0D).add(Attributes.ARMOR, 24.0D).add(Attributes.MOVEMENT_SPEED, 0.32D).add(Attributes.ATTACK_DAMAGE, 50.0D).add(Attributes.FOLLOW_RANGE, 160.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).build());
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(ACTION, 0);
      this.entityData.define(AGE, 0);
      this.entityData.define(IMMUNE, 0);
      this.entityData.define(OWN_MASK, 0);
      this.entityData.define(ALLY, 0);
      this.entityData.define(WHEEL, -1);
      this.entityData.define(CELL, 0);
      this.entityData.define(RITUAL, 0);
   }

   public int action() {
      return this.entityData.get(ACTION);
   }

   public int actionAge() {
      return this.entityData.get(AGE);
   }

   public int immuneMask() {
      return this.entityData.get(IMMUNE);
   }

   public int wheelType() {
      return this.entityData.get(WHEEL);
   }

   public int wheelCells() {
      return this.entityData.get(CELL);
   }

   public int ritualState() {
      return this.entityData.get(RITUAL);
   }

   private void setAction(int action) {
      this.entityData.set(ACTION, action);
      this.entityData.set(AGE, 0);
   }

   private void syncWheel(int type, int cells) {
      this.entityData.set(WHEEL, type);
      this.entityData.set(CELL, cells);
   }

   private void pushMask() {
      int mask=0;
      for(int i=0;i<hits.length;i++)if(hits[i]>=CLEAR_HITS)mask|=1<<i;
      this.entityData.set(IMMUNE,mask);
   }

   public boolean adapted(String type) {
      int i = AttackTypes.index(type);
      return i >= 0 && this.hits[i] >= 24;
   }

   public int hits(String type) {
      int i = AttackTypes.index(type);
      return i < 0 ? 0 : this.hits[i];
   }

   public UUID summoner() {
      return this.summoner;
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putIntArray("MahoHits", this.hits);
      tag.putBoolean("MahoHitsPerTurn6", true);
      tag.putInt("MahoRitual", this.ritualState());
      tag.putLong("MahoRitualStart", this.ritualStart);
      tag.putInt("MahoOwnClear", 6);
      CompoundTag own = new CompoundTag();

      for(Map.Entry<String, Integer> e : this.ownHits.entrySet()) {
         own.putInt((String)e.getKey(), e.getValue());
      }

      tag.put("MahoOwn", own);
      tag.putBoolean("MahoAlly", this.ally());
      if (this.allyOwner != null) {
         tag.putUUID("MahoAllyOwner", this.allyOwner);
      }

      if (this.summoner != null) {
         tag.putUUID("MahoSummoner", this.summoner);
      }

   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      int[] saved = tag.getIntArray("MahoHits");

      for(int i = 0; i < Math.min(saved.length, this.hits.length); ++i) {
         this.hits[i] = tag.getBoolean("MahoHitsPerTurn6") ? Math.max(0,Math.min(CLEAR_HITS,saved[i])) : Math.min(4, Math.max(0, saved[i])) * 6;
      }

      this.entityData.set(RITUAL, tag.getInt("MahoRitual"));
      this.ritualStart = tag.getLong("MahoRitualStart");
      this.ownHits.clear();
      CompoundTag own = tag.getCompound("MahoOwn");

      for(String key : own.getAllKeys()) {
         String slot = normalizeSlot(key);
         if (slot != null) {
            this.ownHits.put(slot, Math.max(0, Math.min(MahoragaAdaptStore.IMMUNE, own.getInt(key))));
         }
      }

      this.pushOwnMask();
      this.allyOwner = tag.hasUUID("MahoAllyOwner") ? tag.getUUID("MahoAllyOwner") : null;
      this.entityData.set(ALLY, tag.getBoolean("MahoAlly") ? 1 : 0);
      this.summoner = tag.hasUUID("MahoSummoner") ? tag.getUUID("MahoSummoner") : null;
      this.pushMask();
   }

   public void startSeenByPlayer(ServerPlayer p) {
      super.startSeenByPlayer(p);
      this.bar.addPlayer(p);
   }

   public void stopSeenByPlayer(ServerPlayer p) {
      super.stopSeenByPlayer(p);
      this.bar.removePlayer(p);
   }

   private ServerLevel server() {
      return (ServerLevel)this.level();
   }

   private void fx(int kind, Vec3 p, Vec3 q, float size, int life, long seed) {
      Network.send(this.server(), new Network.Effect(kind, this.getId(), p, q, size, life, seed));
   }

   private void fxSummoner(int kind, int fallbackKind, Vec3 p, float size, long seed) {
      ServerPlayer owner = this.summonerPlayer();
      Network.send(this.server(), new Network.Effect(owner != null ? kind : fallbackKind, owner != null ? owner.getId() : this.getId(), p, Vec3.ZERO, size, 1, seed));
   }

   public int turns(String type) {
      int i = AttackTypes.index(type);
      return i < 0 ? 0 : this.hits[i] / 6;
   }

   public int hitsLeft(String type) {
      int i = AttackTypes.index(type);
      return i < 0 ? 0 : Math.max(0, 24 - this.hits[i]);
   }

   public int turnHits(String type) {
      int i = AttackTypes.index(type);
      return i < 0 ? 0 : this.hits[i] % 6;
   }

   public int rotations() {
      return Math.min(4, this.wheelCells());
   }

   private void turn(String type, DamageSource source) {
      int index = AttackTypes.index(type);
      if (index >= 0) {
         long now = this.level().getGameTime();
         if (now - (long)this.lastHit[index] >= 4L) {
            this.lastHit[index] = (int)now;
            int var10002 = this.hits[index]++;
            this.wheelFlash = 40;
            this.wheelSpin = 40;
            this.pushMask();
            int turned = this.hits[index] / 6;
            int within = this.hits[index] % 6;
            this.syncWheel(index, Math.min(4, turned));
            boolean done = this.hits[index] >= 24;
            if (within != 0 && !done) {
               this.announce("\u9b54\u865a\u7f57 \u00b7 " + AttackTypes.name(index) + " \u53d7\u529b " + within + "/6 \u00b7 \u8fd8\u9700 " + this.hitsLeft(type) + " \u6b21\u5b8c\u5168\u9002\u5e94");
            } else {
               this.announce("\u9b54\u865a\u7f57 \u00b7 \u6cd5\u8f6e\u8f6c\u52a8 " + Math.min(4, turned) + "/4 \u00b7 " + AttackTypes.name(index) + (done ? "\uff08\u5df2\u5b8c\u5168\u9002\u5e94\uff0c\u8be5\u7c7b\u4f24\u5bb3\u5f52\u96f6\uff09" : ""));
            }

            this.fx(MahoragaVisuals.WHEEL_ROTATE, this.position().add(0.0D, (double)this.getBbHeight() + 0.4D, 0.0D), Vec3.ZERO, (float)(Math.min(4, turned) * 16 + within), 40, (long)index);
            this.fxSummoner(MahoragaVisuals.WHEEL_BEAT, MahoragaVisuals.WHEEL_ROTATE, this.position(), (float)(Math.min(4, turned) * 16 + within), (long)index);
            if (within == 0 || done) {
               this.level().playSound((Player)null, this.blockPosition(), done ? SoundEvents.BEACON_ACTIVATE : SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, done ? 2.2F : 1.2F, done ? 1.6F : 0.8F);
            }

         }
      }
   }

   private void repel(String type, DamageSource source) {
      int index = AttackTypes.index(type);
      this.level().playSound((Player)null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.6F, 0.7F);
      Entity var5 = source.getEntity();
      if (var5 instanceof ServerPlayer p) {
         p.displayClientMessage(Component.literal("\u9b54\u865a\u7f57\u5df2\u5b8c\u5168\u9002\u5e94" + AttackTypes.name(index) + "\uff0c\u8be5\u7c7b\u4f24\u5bb3\u5f52\u96f6").setStyle(Style.EMPTY.withColor(16765514)), true);
      }

   }

   public boolean hurt(DamageSource source, float amount) {
      if(!level().isClientSide){
         ServerPlayer sourceOwner=CombatSettings.caster(source);
         if(sourceOwner!=null&&!CombatSafety.canAffect(sourceOwner,this))return false;
         if(ritualState()==RITUAL_RUNNING){
            tickRitual();if(isRemoved())return false;
            ServerPlayer owner=summonerPlayer();
            if(sourceOwner!=null&&owner!=null&&sourceOwner!=owner&&(!CombatSettings.canHit(owner,sourceOwner)||!CombatSettings.canHit(sourceOwner,owner)))fail("非敌对玩家从场外协助",owner);
         }
         if(ally()&&summonerPlayer()!=null)shareAdaptation(summonerPlayer());
      }
      if (!this.level().isClientSide && this.ally()) {
         Entity src = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
         ServerPlayer var10000;
         if (src instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)src;
            var10000 = sp;
         } else if (src instanceof LivingEntity) {
            LivingEntity le = (LivingEntity)src;
            var10000 = MaidBridge.progression(le);
         } else {
            var10000 = null;
         }

         ServerPlayer attacker = var10000;
         if (attacker != null && MahoragaAdaptStore.friendlyTo(attacker, this)) {
            if (this.ownerFriendlyNotice++ % 40 == 0) {
               attacker.displayClientMessage(Component.literal("\u53cb\u65b9\u9b54\u865a\u7f57\u662f\u4f60\u7684\u4eba\uff1a\u4f60\u7684\u672f\u5f0f\u5bf9\u5b83\u65e0\u6548").setStyle(Style.EMPTY.withColor(10217727)), true);
            }

            return false;
         }
      }

      if (!this.level().isClientSide && amount > 0.0F) {
         String type = AttackTypes.of(source);
         if (source.getEntity() != null) {
            source.getEntity();
         } else {
            source.getDirectEntity();
         }

         String ownKey = MahoragaAdaptStore.keyOf(source);
         // 「按来源」的适应进度只属于**友方魔虚罗**（它继承主人的法轮表）。
         // 本体魔虚罗的适应仍然是那 24 格"招式"法轮（turn/adapted）：如果让它也按来源算，
         // 挨玩家打 24 次后就会把"玩家"这一整个来源免疫掉，等于整只怪对玩家无敌。
         if (this.ally() && ownKey != null) {
            // 查表用**槽位键**（taken/<实体id>）：与主人法轮存在 Curse 里的键格式逐字一致，
            // 召唤时整张表原样拷过来就能直接命中，不会出现"继承了却一条都不生效"。
            String ownSlot = MahoragaAdaptStore.TAKEN + ownKey;
            int ownHits = this.ownHits.getOrDefault(ownSlot, 0);
            float ownFactor = MahoragaAdaptStore.takenFactor(ownHits); // 12 次减半 → 24 次免疫
            if (ownFactor <= 0.0F) {
               this.level().playSound((Player)null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.6F, 0.7F);
               return false;
            }
            if (ownFactor < 1.0F) amount *= ownFactor;
            this.turnOwn(ownSlot);
         }
         if (this.adapted(type)) {
            this.repel(type, source);
            return false;
         }

         this.turn(type, source);
      }

      return super.hurt(source, amount);
   }

   /** **绕过"适应"吃一次伤害**：给"点名数值"的技能用（例如灶·最大功率·开 对魔虚罗固定 100000）。
    *  直接走父类的受击流程（照常吃护甲/免伤事件），只是跳过本类里"按招式/按来源适应"的那套判定。 */
   public boolean forceDamage(DamageSource source, float amount) {
      if(!level().isClientSide){ServerPlayer owner=CombatSettings.caster(source);if(owner!=null&&!CombatSafety.canAffect(owner,this))return false;if(ritualState()==RITUAL_RUNNING){tickRitual();if(isRemoved())return false;}}
      return super.hurt(source, amount);
   }

   private void announce(String text) {
      if (!this.level().isClientSide) {
         for(ServerPlayer p : this.server().getServer().getPlayerList().getPlayers()) {
            if (p.level() == this.level() && p.distanceToSqr(this) <= 65536.0D) {
               p.displayClientMessage(Component.literal(text), true);
            }
         }

      }
   }

   public void setAllyMode(UUID owner, boolean ally) {
      this.allyOwner = ally ? owner : null;
      this.entityData.set(ALLY, ally ? 1 : 0);
      if (ally) {
         this.summoner = owner;
         this.entityData.set(RITUAL, 0);
      }

      this.setPersistenceRequired();
   }

   public boolean ally() {
      return this.entityData.get(ALLY) != 0;
   }

   public boolean isAllyOf(UUID owner) {
      return this.ally() && this.allyOwner != null && this.allyOwner.equals(owner);
   }

   public UUID allyOwner() {
      return this.allyOwner;
   }

   private boolean allyTarget(LivingEntity target) {
      if (!MahoragaRules.valid(this, target)) {
         return false;
      } else if (target instanceof ServerPlayer) {
         ServerPlayer p = (ServerPlayer)target;
         if (this.allyOwner != null && this.allyOwner.equals(p.getUUID())) {
            return false;
         } else {
            LivingEntity last = p.getLastHurtByMob();
            return last != null && this.allyOwner != null && this.allyOwner.equals(last.getUUID());
         }
      } else {
         if (target instanceof Mob) {
            Mob mob = (Mob)target;
            LivingEntity mt = mob.getTarget();
            if (mt != null && this.allyOwner != null && this.allyOwner.equals(mt.getUUID())) {
               return true;
            }

            ServerPlayer owner = this.summonerPlayer();
            if (owner != null && owner.getLastHurtMob() == mob) {
               return true;
            }

            if (mob.getLastHurtByMob() != null && this.allyOwner != null && this.allyOwner.equals(mob.getLastHurtByMob().getUUID())) {
               return true;
            }
         }

         return false;
      }
   }

   public void resizeAllyHealth() {
      AttributeInstance attr = this.getAttribute(Attributes.MAX_HEALTH);
      if (attr != null) {
         attr.setBaseValue(800.0D);
         this.setHealth(800.0F);
      }

   }

   public void emerge() {
      this.setAction(1);
      this.rest = 40;
   }

   public void clearRitual() {
      this.entityData.set(RITUAL, 0);
   }

   /** 继承主人在法轮上攒下的**整张适应表**：受击（taken/）与攻击（dealt/）两条进度原样带过来。
    *
    *  <p>键格式必须与 {@link MahoragaAdaptStore} **逐字一致**：库里存的是
    *  {@code taken/<实体id>} / {@code dealt/<实体id>} 这种<b>槽位键</b>，这只魔虚罗自己
    *  查表（{@link #hurt} / {@link #hitTarget}）用的也必须是同样的槽位键。此前这里把库里的
    *  槽位键原样放进 ownHits，但查表用的是<b>不带前缀的裸键</b>（{@code minecraft:zombie}），
    *  于是每一条都查不中 —— 表现为 <b>"友方魔虚罗一点适应都没继承"</b>。</p> */
   public void resetAdaptation() {
      java.util.Arrays.fill(hits,0);java.util.Arrays.fill(lastHit,0);ownHits.clear();
      entityData.set(IMMUNE,0);entityData.set(OWN_MASK,0);entityData.set(WHEEL,-1);entityData.set(CELL,0);wheelSpin=wheelFlash=0;
   }

   public void shareAdaptation(ServerPlayer owner) {
      if(!isAllyOf(owner.getUUID()))return;
      MahoragaAdaptStore.merge(owner,ownHits);
      importAdaptation(owner);
   }

   public void importAdaptation(ServerPlayer owner) {
      Map<String,Integer> snapshot=curseAdapt(owner);
      if(ownHits.equals(snapshot))return;
      this.ownHits.clear();

      for(Map.Entry<String, Integer> e : snapshot.entrySet()) {
         String slot = normalizeSlot((String)e.getKey());
         int hits = Math.max(0, Math.min(MahoragaAdaptStore.IMMUNE, (Integer)e.getValue()));
         if (slot != null && hits > 0) {
            this.ownHits.put(slot, hits);
         }
      }

      this.pushOwnMask();
      this.syncWheelOwn();
   }

   /** 旧存档里 ownHits 存的是裸键（没有 taken/ dealt/ 前缀），载入时统一补成 {@code taken/}。 */
   private static String normalizeSlot(String key) {
      if (key == null || key.isEmpty()) {
         return null;
      }
      return key.indexOf('/') >= 0 ? key : MahoragaAdaptStore.TAKEN + key;
   }

   private static Map<String, Integer> curseAdapt(ServerPlayer owner) {
      CompoundTag tag = Curse.data(owner).getCompound("MahoAdapted");
      Map<String, Integer> out = new LinkedHashMap();

      for(String key : tag.getAllKeys()) {
         out.put(key, tag.getInt(key));
      }

      return out;
   }

   public int ownMask() {
      return this.entityData.get(OWN_MASK);
   }

   private void pushOwnMask() {
      int mask = 0;
      int bit = 0;

      for(Map.Entry<String, Integer> e : this.ownHits.entrySet()) {
         // 与主人法轮同一判据：12 次（CLEAR）= 完全适应，一格法轮
         int threshold=e.getKey().startsWith(MahoragaAdaptStore.TAKEN)?MahoragaAdaptStore.IMMUNE:MahoragaAdaptStore.CLEAR;
         if (e.getValue() >= threshold && bit < OWN_MASK_BITS) {
            mask |= 1 << bit;
         }

         ++bit;
      }

      this.entityData.set(OWN_MASK, mask);
   }

   private void syncWheelOwn() {
      this.fxSummoner(MahoragaVisuals.WHEEL_BEAT, MahoragaVisuals.WHEEL_ROTATE, this.position(), (float)(Math.min(4, Integer.bitCount(this.ownMask())) * 16), (long)Math.max(0, this.wheelType()));
   }

   /** 适应进度 +1。刻度与主人法轮**完全一致**：12 次（{@link MahoragaAdaptStore#CLEAR}）算"完全适应"，
    *  24 次（{@link MahoragaAdaptStore#IMMUNE}）彻底免疫（免疫由 {@link MahoragaAdaptStore#takenFactor} 归零实现）。
    *  旧版把上限写死成 6，于是"打满 12 次才真伤"（{@link #OWN_CLEAR}）永远到不了，继承过来的进度也没法用。 */
   private void turnOwn(String slot) {
      if (slot != null) {
         int hits = this.ownHits.getOrDefault(slot, 0);
         if (hits < MahoragaAdaptStore.IMMUNE) {
            int next = hits + 1;
            this.ownHits.put(slot, next);
            ServerPlayer owner=summonerPlayer();
            if(ally()&&owner!=null)MahoragaAdaptStore.merge(owner,ownHits);
            this.pushOwnMask();
            this.syncWheelOwn();
            if (next == (slot.startsWith(MahoragaAdaptStore.TAKEN)?MahoragaAdaptStore.IMMUNE:OWN_CLEAR)) {
               this.announce("魔虚罗 · 已完全适应" + ownLabel(slot) + (slot.startsWith(MahoragaAdaptStore.TAKEN)?"：该来源伤害免疫":"：对其真伤翻倍并禁疗20秒"));
               this.level().playSound((Player)null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0F, 1.3F);
            }

         }
      }
   }

   private void turnOwnByAttack(LivingEntity victim) {
      if (victim != null) {
         ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
         if (id != null) {
            this.turnOwn(MahoragaAdaptStore.DEALT + id);
         }

      }
   }

   private static String ownLabel(String key) {
      // ownHits 的键是槽位键（taken/ dealt/ 前缀），显示前先剥掉前缀
      String bare = key == null ? "" : (key.indexOf('/') >= 0 ? key.substring(key.indexOf('/') + 1) : key);
      if ("technique:blood".equals(bare))return "「血液」";
      if ("technique:ten_shadows".equals(bare))return "「十影」";
      if ("env".equals(bare)) {
         return "\u5176\u4ed6\u4f24\u5bb3";
      } else {
         try {
            EntityType<?> type = (EntityType)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(bare));
            if (type != null) {
               return "\u300c" + type.getDescription().getString() + "\u300d";
            }
         } catch (RuntimeException var2) {
         }

         return "\u300c" + bare + "\u300d";
      }
   }

   // 时间适应（每 60 秒一层、满层真伤 + 禁疗）已按要求移除：
   // 现在魔虚罗只有两种适应——挨同一个来源打 12 次，或（友方）打对方 12 次。

   public void beginRitual(ServerPlayer owner) {
      this.summoner=owner.getUUID();
      this.ritualStart=this.level().getGameTime();
      this.entityData.set(RITUAL,1);
      this.setAction(1);
      this.rest=40;
      this.level().playSound(null,this.blockPosition(),SoundEvents.WARDEN_ROAR,SoundSource.HOSTILE,3.0F,.6F);
      this.announce("调服仪式开始：64格内不得有女仆、友方或未与召唤者交战的生物；只能由召唤者完成调服，逃出500格、死亡、离线或换维度判失败。可重新挑战。");
      tickRitual();
   }

   private List<LivingEntity> intruders() {
      List<LivingEntity> found=new ArrayList<>();
      ServerPlayer owner=summonerPlayer();
      for(LivingEntity e:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(CLEAR_RANGE),ex->ex!=this&&ex.isAlive()&&!ex.isSpectator())) {
         if(e==owner||!MahoragaRitualRules.participant(distanceToSqr(e)))continue;
         boolean mutual=owner!=null&&e instanceof ServerPlayer other&&CombatSettings.canHit(owner,other)&&CombatSettings.canHit(other,owner);
         boolean attacking=owner!=null&&e instanceof Mob mob&&mob.getTarget()==owner&&CombatSettings.canHit(owner,e);
         if(MahoragaRitualRules.intruder(MaidBridge.isMaid(e),e instanceof Player,mutual,attacking))found.add(e);
      }
      return found;
   }

   public void fail(String reason, ServerPlayer owner) {
      if (this.ritualState() != 2 && this.ritualState() != 3) {
         this.entityData.set(RITUAL, 2);
         Component text = Component.literal("\u8c03\u4f0f\u4eea\u5f0f\u5931\u8d25\uff1a" + reason);
         if (owner != null) {
            owner.displayClientMessage(text, false);
         } else {
            this.announce("\u8c03\u4f0f\u4eea\u5f0f\u5931\u8d25\uff1a" + reason);
         }

      }
   }

   private void succeed(ServerPlayer killer) {
      if (this.ritualState() != 3) {
         this.entityData.set(RITUAL, 3);
         ServerPlayer ritualOwner=summonerPlayer();
         if(ritualOwner!=null)TenShadows.tame(ritualOwner);
         this.announce("\u8c03\u4f0f\u6210\u529f\uff1a\u516b\u63e1\u5251\u5f02\u6212\u795e\u5c06\u00b7\u9b54\u865a\u7f57\u5df2\u88ab\u964d\u4f0f\u3002");
         if (killer != null) {
            killer.displayClientMessage(Component.literal("\u8c03\u4f0f\u6210\u529f\u3002").setStyle(Style.EMPTY.withColor(16765514)), false);
         }

      }
   }

   private ServerPlayer summonerPlayer() {
      return this.summoner == null ? null : this.server().getServer().getPlayerList().getPlayer(this.summoner);
   }

   private void tickRitual() {
      if(ritualState()!=RITUAL_RUNNING)return;
      ServerPlayer owner=summonerPlayer();
      if(MahoragaRitualRules.escaped(owner!=null&& !owner.hasDisconnected(),owner!=null&&owner.isAlive(),owner!=null&&owner.level()==level(),owner==null?Double.POSITIVE_INFINITY:distanceToSqr(owner))){
         fail("召唤者死亡、离线、换维度或离开500格挑战范围",owner);discard();return;
      }
      List<LivingEntity> outsiders=intruders();
      if(!outsiders.isEmpty())fail("64格内有非敌对参与者："+outsiders.get(0).getName().getString(),owner);
   }

   public boolean canAttack(LivingEntity target) {
      return this.ally() ? this.allyTarget(target) : MahoragaRules.valid(this, target);
   }

   public boolean removeWhenFarAway(double distance) {
      return false;
   }

   protected void customServerAiStep() {
      if (!this.level().isClientSide) {
         long now = this.level().getGameTime();
         this.bar.setName(Component.literal((this.ally() ? "\u53cb\u65b9 \u00b7 " : "") + "\u516b\u63e1\u5251\u5f02\u6212\u795e\u5c06\u00b7\u9b54\u865a\u7f57 \u00b7 " + (int)this.getHealth() + " / " + (int)this.getMaxHealth() + " \u00b7 \u9002\u5e94\u62db\u5f0f " + Integer.bitCount(this.immuneMask()) + "/" + AttackTypes.SLOTS + (this.ritualState() == 1 ? " \u00b7 \u8c03\u4f0f\u4eea\u5f0f" : (this.ritualState() == 2 ? " \u00b7 \u4eea\u5f0f\u5931\u8d25" : ""))));
         this.bar.setProgress(this.getHealth() / this.getMaxHealth());
         if(ally()&&now%20==0&&summonerPlayer()!=null)shareAdaptation(summonerPlayer());
         if (!this.ally()) {
            this.tickRitual();
         }

         // 时间适应已移除：现在只有"被打 12 次 / 攻击 12 次"两种适应
         if (now % 20L == 0L) {
            this.fxSummoner(MahoragaVisuals.WHEEL_BEAT, MahoragaVisuals.WHEEL_ROTATE, this.position(), (float)(Math.min(4, this.wheelCells()) * 16), (long)Math.max(0, this.wheelType()));
         }

         if (this.wheelFlash > 0) {
            --this.wheelFlash;
         }

         if (this.wheelSpin > 0) {
            --this.wheelSpin;
         }

         if (this.actionAge() > 0 || this.action() != 0) {
            this.entityData.set(AGE, this.actionAge() + 1);
         }

         if (this.action() != 0 && this.action() != 7) {
            this.tickAction();
            if (this.actionAge() >= this.actionLength()) {
               this.setAction(0);
               this.rest = 12;
            }

            this.getNavigation().stop();
         } else {
            if (this.leapCooldown > 0) {
               --this.leapCooldown;
            }

            LivingEntity target = this.pickTarget();
            if (target == null) {
               this.followOwner();
            } else {
               this.getLookControl().setLookAt(target, 60.0F, 60.0F);
               double distance = (double)this.distanceTo(target);
               if (this.rest > 0) {
                  --this.rest;
                  if (distance < 24.0D) {
                     this.getNavigation().moveTo(target, 1.2D);
                  }

               } else if (distance <= 4.2D) {
                  this.attack(target);
               } else if (distance > 10.0D && distance < 26.0D && this.leapCooldown <= 0) {
                  this.leapCooldown = 120;
                  this.setAction(6);
                  this.faceAt(target);
               } else {
                  this.getNavigation().moveTo(target, distance > 12.0D ? 1.35D : 1.05D);
               }
            }
         }
      }
   }

   private void followOwner() {
      if (this.ally()) {
         ServerPlayer owner = this.summonerPlayer();
         if (owner != null && owner.level() == this.level()) {
            double d = (double)this.distanceTo(owner);
            if (d > 8.0D) {
               this.getNavigation().moveTo(owner, 1.25D);
            } else if (d < 3.0D) {
               this.getNavigation().stop();
            }

         }
      }
   }

   private LivingEntity pickAllyTarget() {
      ServerPlayer owner=this.summonerPlayer();
      if(owner==null||owner.level()!=level())return null;
      if(ally())return CombatSafety.summonTarget(owner,this,24);
       LivingEntity victim=owner.getLastHurtMob();
      if(owner.tickCount-owner.getLastHurtMobTimestamp()<200&&allyFilter(victim))return victim;
      LivingEntity attacker=owner.getLastHurtByMob();
      if(owner.tickCount-owner.getLastHurtByMobTimestamp()<200&&allyFilter(attacker))return attacker;
      var nearby=level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(24),this::allyFilter);
      LivingEntity threat=nearby.stream().filter(e->e instanceof Mob m&&m.getTarget()==owner).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
      if(threat!=null)return threat;
      LivingEntity current=getTarget();if(allyFilter(current))return current;
      return nearby.stream().filter(e->e instanceof net.minecraft.world.entity.monster.Enemy).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
   }

   private boolean allyFilter(LivingEntity e) {
      return MahoragaRules.valid(this, e) && !this.isFriendlyTo(e) && MaidBridge.progression(e) != this.summonerPlayer();
   }

   private LivingEntity pickTarget() {
      if (!this.ally()&&getPersistentData().hasUUID("MegumiHunt")) {
         LivingEntity execution=getTarget();
         if(execution instanceof MegumiEntity megumi&&!megumi.downed())return execution;
         ServerPlayer hunter=server().getServer().getPlayerList().getPlayer(getPersistentData().getUUID("MegumiHunt"));
         if(hunter!=null&&hunter.isAlive()&&hunter.level()==level())return hunter;
      }
      if (this.ally()) {
         return this.pickAllyTarget();
      } else {
         ServerPlayer owner = this.summonerPlayer();
         if (this.ritualState() == 2) {
            List<LivingEntity> intruders = this.intruders();
            if (!intruders.isEmpty()) {
               return (LivingEntity)intruders.get(0);
            }
         }

         if (owner != null && owner.level() == this.level() && owner.isAlive() && !owner.isSpectator() && this.ritualState() == 1) {
            return owner;
         } else {
            LivingEntity best = this.getTarget();
            if (best == null || !MahoragaRules.valid(this, best) || this.distanceToSqr(best) > 65536.0D) {
               best = (LivingEntity)this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(48.0D), (e) -> MahoragaRules.valid(this, e) && this.hasLineOfSight(e)).stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            }

            if (best == null && owner != null && owner.level() == this.level() && owner.isAlive()) {
               best = owner;
            }

            return best;
         }
      }
   }

   private void faceAt(LivingEntity target) {
      Vec3 d = target.getEyePosition().subtract(this.getEyePosition());
      this.setYRot((float)Math.toDegrees(Math.atan2(-d.x, d.z)));
      this.setXRot((float)(-Math.toDegrees(Math.atan2(d.y, d.horizontalDistance()))));
      this.yBodyRot = this.getYRot();
      this.yHeadRot = this.getYRot();
   }

   private int actionLength() {
      byte var10000;
      switch (this.action()) {
         case 1:
            var10000 = 60;
            break;
         case 2:
         case 3:
         case 4:
         case 5:
            var10000 = 24;
            break;
         case 6:
            var10000 = 30;
            break;
         default:
            var10000 = 20;
      }

      return var10000;
   }

   private void attack(LivingEntity target) {
      this.faceAt(target);
      int[] options = new int[]{2, 5, 3, 4};
      this.setAction(options[this.getRandom().nextInt(this.distanceTo(target) <= 5.0F && this.getRandom().nextBoolean() ? 4 : 2)]);
   }

   private void tickAction() {
      int age = this.actionAge();
      if (age == 10 || age == 12) {
         switch (this.action()) {
            case 2:
            case 5:
               LivingEntity t = this.pickTarget();
               if (t != null && this.distanceToSqr(t) <= 25.0D && this.hasLineOfSight(t)) {
                  this.hitTarget(t, 1.0F, 0.6F);
               }
               break;
            case 3:
               for(LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(6.0D), (e) -> MahoragaRules.valid(this, e))) {
                  this.hitTarget(e, 0.7F, 0.5F);
               }
               break;
            case 4:
               for(LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(8.0D), (e) -> MahoragaRules.valid(this, e))) {
                  this.hitTarget(e, 0.85F, 1.0F);
               }

               this.level().playSound((Player)null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.4F, 0.6F);
               break;
            case 6:
               LivingEntity leapTarget = this.pickTarget();
               if (leapTarget != null) {
                  Vec3 to = leapTarget.position();
                  if (this.server().noCollision(this, this.getBoundingBox().move(to.subtract(this.position())))) {
                     this.teleportTo(to.x, to.y, to.z);
                  }

                  this.faceAt(leapTarget);
               }

         }

      }
   }

   private boolean isFriendlyTo(LivingEntity target) {
      if (!this.ally()) {
         return false;
      } else {
         ServerPlayer owner = this.summonerPlayer();
         if (owner == null) {
            return false;
         } else if (target.getUUID().equals(owner.getUUID())) {
            return true;
         } else if (target == MaidBridge.body(owner)) {
            return true;
         } else if (MaidBridge.progression(target) == owner) {
            return true;
         } else {
            if (target instanceof MahoragaEntity) {
               MahoragaEntity m = (MahoragaEntity)target;
               if (m.ally()) {
                  return true;
               }
            }

            return ShadowSystem.friendly(owner,target)||!CombatSettings.canHit(owner,target)||MahoragaAdaptStore.friendlyTo(owner,target);
         }
      }
   }

   private void hitTarget(LivingEntity target, float scale, float knock) {
      if (target == null || !MahoragaRules.valid(this, target) || this.isFriendlyTo(target)) {
         return;
      }

      ServerPlayer owner=ally()?summonerPlayer():null;
      if(ally()&&(owner==null||!CombatSafety.canAffect(owner,target)))return;
      if(owner!=null)shareAdaptation(owner);
      target.invulnerableTime=0;
      float damage=BalanceRules.damage(getAttributeValue(Attributes.ATTACK_DAMAGE)*scale,BalanceRules.Category.BOSS_ATTACK);
      String victimKey=MahoragaAdaptStore.keyOfEntity(target),slot=MahoragaAdaptStore.DEALT+victimKey;
      boolean adapted=ally()&&victimKey!=null&&ownHits.getOrDefault(slot,0)>=OWN_CLEAR;
      DamageSource source=adapted?new DamageSource(damageSources().genericKill().typeHolder(),this,this):damageSources().mobAttack(this);
      boolean accepted;
      try{AttackTypes.mark(AttackTypes.TEN_SHADOWS);accepted=owner!=null?CombatSafety.hurt(owner,target,source,adapted?damage*MahoragaAdaptStore.ADAPTED_DAMAGE:damage):target.hurt(source,damage);}finally{AttackTypes.clear();}
      if(!accepted)return;
      if(ally()&&victimKey!=null){
         if(adapted){target.getPersistentData().putLong(MahoragaAdaptStore.NO_HEAL_UNTIL,level().getGameTime()+MahoragaAdaptStore.ADAPTED_NO_HEAL_TICKS);target.getPersistentData().putBoolean("SukunaNoHeal",true);}
         else turnOwn(slot);
      }

      Vec3 push = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
      if (push.lengthSqr() > 1.0E-4D) {
         push = push.normalize();
         target.push(push.x * 3.2D * (double)knock, 1.4D * (double)knock, push.z * 3.2D * (double)knock);
         target.hurtMarked = true;
      }
   }

   public void die(DamageSource source) {
      if (!this.level().isClientSide) {
         this.bar.removeAllPlayers();
         Entity drop = source.getEntity();
         ServerPlayer var10000;
         if (drop instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)drop;
            var10000 = MaidBridge.progression(living);
         } else {
            var10000 = null;
         }

         ServerPlayer killer=CombatSettings.caster(source);
         if(ally()&&summonerPlayer()!=null)shareAdaptation(summonerPlayer());
         tickRitual();
         int state=this.ritualState();
         if(state==RITUAL_RUNNING){
            ServerPlayer owner=summonerPlayer();
            if(MahoragaRitualRules.reward(state,owner!=null&&owner.isAlive()&&owner.level()==level(),killer==owner))succeed(killer);
            else fail("必须由存活且在挑战范围内的召唤者击败魔虚罗",owner);
            state=ritualState();
         }

         this.fxSummoner(MahoragaVisuals.WHEEL_END, MahoragaVisuals.WHEEL_END, this.position(), 0.0F, 0L);
         if (!ally()&&state != RITUAL_FAILED) {
            ItemStack wheel = ((Item)VoidContent.MAHORAGA_WHEEL.get()).getDefaultInstance();
            wheel.setDamageValue(0);
            this.spawnAtLocation(wheel);
         } else if (killer != null) {
            killer.displayClientMessage(Component.literal("\u8fd9\u53ea\u9b54\u865a\u7f57\u7684\u8c03\u4f0f\u4eea\u5f0f\u5df2\u7ecf\u5931\u8d25\u4e86\uff0c\u5c38\u4f53\u4e0d\u4f1a\u7559\u4e0b\u6cd5\u8f6e").setStyle(Style.EMPTY.withColor(16751194)), false);
         }
      }

      super.die(source);
   }

   public void remove(Entity.RemovalReason reason) {
      if(!level().isClientSide&&ally()&&summonerPlayer()!=null)shareAdaptation(summonerPlayer());
      if (!this.level().isClientSide && !reason.shouldSave()) {
         this.fxSummoner(MahoragaVisuals.WHEEL_END, MahoragaVisuals.WHEEL_END, this.position(), 0.0F, 0L);
      }

      super.remove(reason);
   }
}
