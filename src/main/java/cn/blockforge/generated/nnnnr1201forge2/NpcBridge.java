package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;

/** 每只 NPC 独立的施术上下文。技能队列、咒力、归属都绑定本体，不冒用附近玩家。 */
public final class NpcBridge {
    private static final Map<UUID, NpcActor> ACTORS = new HashMap<>();

    public static String character(LivingEntity e) {
        String override=e.getPersistentData().getString("NpcTechnique");
        if(character.CharacterRegistry.get(override)!=null)return override;
        if (e instanceof character.npc.NpcTechniqueUser user) {
            var loadout=user.npcTechniques();
            if(loadout!=null&&character.CharacterRegistry.get(loadout.role())!=null)return loadout.role();
        }
        if (e instanceof SorcererNpcEntity npc) return npc.characterId();
        if (e instanceof ChosoEntity) return "blood";
        if (e instanceof SukunaNpcEntity) return "sukuna";
        if (e instanceof JogoNpcEntity) return "jogo";
        if (e instanceof UraumeNpcEntity) return "uraume";
        if (e instanceof RyuNpcEntity) return "ryu";
        if (e instanceof HakariNpcEntity) return "hakari";
        if (e instanceof HigurumaNpcEntity) return "higuruma";
        if (e instanceof NaoyaNpcEntity) return "naoya";
        if (e instanceof KashimoNpcEntity) return "kashimo";
        if (e instanceof TodoNpcEntity) return "todo";
        // 新生物可在服务端写入已注册的角色编号，直接复用同一条施法路径。
        String id = e.getPersistentData().getString("NpcTechnique");
        return character.CharacterRegistry.get(id) == null ? "" : id;
    }
    public static boolean supports(LivingEntity e) { return e instanceof Mob && !character(e).isEmpty(); }
    public static NpcActor actorFor(LivingEntity mob) {
        if (mob == null || !(mob.level() instanceof ServerLevel level) || !supports(mob)) return null;
        NpcActor old = ACTORS.get(mob.getUUID());
        if (old != null && old.mob == mob && old.level() == level && old.characterId.equals(character(mob))) return old;
        if (old != null) old.retire();
        NpcActor actor = new NpcActor(level, mob);
        ACTORS.put(mob.getUUID(), actor);
        return actor;
    }
    public static NpcActor existing(Entity e) { return e == null ? null : ACTORS.get(e.getUUID()); }
    public static ServerPlayer find(MinecraftServer server, UUID id) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) return player;
        NpcActor a = ACTORS.get(id);
        if (a != null && a.isAlive() && a.mob.level() == a.level()) return a;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e instanceof LivingEntity living) {
                if (supports(living)) return actorFor(living);
                if (MaidBridge.isMaid(living) || Legion.member(living) || Authority.echo(living)) return MaidBridge.actor(living);
            }
        }
        return null;
    }
    public static LivingEntity body(ServerPlayer p) { return p instanceof NpcActor a ? a.mob : p; }
    public static boolean isNpcActor(Player p) { return p instanceof NpcActor; }
    public static boolean canHit(NpcActor a, LivingEntity target) { return a != null && canHit(a.mob, target); }
    public static boolean canHit(LivingEntity mob, LivingEntity target) {
        if (mob == null || target == null || CurseHostility.truce(mob,target) || target == mob || target.getUUID().equals(mob.getUUID())
                || !target.isAlive() || target.isRemoved() || target.isSpectator() || target.level() != mob.level()
                || target instanceof FakePlayer || mob.isAlliedTo(target)) return false;
        UUID own = CombatSafety.ownerId(mob), theirs = CombatSafety.ownerId(target);
        if (theirs != null && theirs.equals(mob.getUUID())) return false;
        if (own != null && (own.equals(target.getUUID()) || own.equals(theirs))) return false;
        if (target instanceof Player p) return !p.isCreative() && !p.isSpectator();
        // 同类默认不误伤；被强制设置的对战目标仍可对打。不同 NPC 不再被一律视为队友。
        return target.getType() != mob.getType() || mob instanceof Mob m && m.getTarget() == target;
    }
    public static void tick() {
        for (NpcActor a : List.copyOf(ACTORS.values())) {
            if (!a.isAlive() || a.mob.level() != a.level()) { a.retire(); ACTORS.remove(a.getUUID(), a); continue; }
            a.syncPosition();
            character.TechniqueApi.tick(a);
            boolean domain=DomainClash.hasDomain(a);
            if(Curse.data(a).getBoolean("NpcHadDomain")&&!domain&&!DomainClash.active(a))Curse.endDomain(a,false);
            Curse.data(a).putBoolean("NpcHadDomain",domain);
            AuthoritySkills.tickAutomatic(a);
            DeathStar.tickAnemia(a);
            if(a.characterId.equals("higuruma")&&a.mob.getMainHandItem().isEmpty()&&!character.higuruma.HigurumaCombat.returning(a))character.higuruma.HigurumaCombat.giveGavel(a);
            // 仅驱动战斗维护，不把假玩家送进登录、成就、物品奖励等真人事件。
            var event = new TickEvent.PlayerTickEvent(TickEvent.Phase.END, a);
            switch (a.characterId) {
                case "hakari" -> character.hakari.HakariCombat.tick(event);
                case "ryu" -> character.ryu.RyuCombat.tick(event);
                case "kashimo" -> character.kashimo.KashimoCombat.tick(event);
                case "naoya" -> {character.naoya.NaoyaDomain.autoAttack(a,character.naoya.NaoyaDomain.active(a)&&a.mob instanceof Mob m&&m.getTarget()!=null);character.naoya.NaoyaCombat.tick(event);}
                case "yuki" -> character.yuki.YukiCombat.onPlayerTick(event);
                case "nanami" -> character.nanami.NanamiCombat.tick(event);
                case "toji" -> character.toji.TojiCombat.tick(event);
                default -> { }
            }
            if (a.mob.tickCount % 20 == 0) Curse.tickCaster(a);
        }
    }
    public static void clear() { for (NpcActor a : List.copyOf(ACTORS.values())) a.retire(); ACTORS.clear(); }

    public static final class NpcActor extends FakePlayer {
        public final LivingEntity mob;
        public final String characterId;
        private NpcActor(ServerLevel level, LivingEntity mob) {
            super(level, new GameProfile(mob.getUUID(), "SukunaNpc"));
            this.mob = mob;
            characterId = character(mob);
            setId(mob.getId());
            CompoundTag d = Curse.data(this);
            d.putString("SelectedCharacter", characterId);
            if (characterId.equals("sukuna")) {
                for (String flag : List.of("Memory", "Awakened", "Nine", "SoulShrine", "VoidShrine", "PureLandVictory")) d.putBoolean(flag, true);
                d.putInt("Fingers", 20);
            }
            if (characterId.equals("uraume")) d.putBoolean("UraumeMemory", true);
            if (characterId.equals("gojo")) d.putBoolean("GojoMemory", true);
            if(characterId.equals("higuruma")){
                if(mob.getMainHandItem().isEmpty())character.higuruma.HigurumaCombat.giveGavel(this);
                if(mob.getOffhandItem().isEmpty())mob.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(net.minecraft.world.item.Items.IRON_AXE));
            }
            super.getAttribute(Attributes.MAX_HEALTH).setBaseValue(mob.getMaxHealth());
            refreshDimensions();
            syncPosition();
        }
        public void syncPosition() {
            setPos(mob.getX(), mob.getY(), mob.getZ());
            setYRot(mob.getYRot()); setXRot(mob.getXRot()); tickCount = mob.tickCount;setOnGround(mob.onGround());fallDistance=mob.fallDistance;
            if (mob instanceof Mob m && m.getTarget() != null) {
                Vec3 d = m.getTarget().getEyePosition().subtract(mob.getEyePosition());
                setYRot((float)(Math.toDegrees(Math.atan2(d.z, d.x)) - 90));
                setXRot((float)-Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
            }
        }
        private void retire() {
            AuthoritySkills.cancel(this);
            Skills.closeDomain(this);BloodDomain.close(this);GojoCombat.closeDomain(this);GuardianPlayerSkills.closeOwnedDomain(this,false);
            character.ryu.RyuCombat.testReset(this);Curse.data(this).remove("NpcChannelUntil");
            character.yuta.YutaDomain.close(this,"施术者离场");
            character.domainking.SelfEmbryoDomain.close(this,"施术者离场");
            character.ryu.RyuDomain.forceClose(this);
            character.naoya.NaoyaDomain.forceClose(this);
            character.hakari.HakariDomain.close(this, "施术者离场");
            character.higuruma.HigurumaDomain.close(this, "施术者离场");
            character.yuki.YukiDomain.close(this, "施术者离场");
            character.jogo.JogoCombat.closeDomain(this);
            setRemoved(RemovalReason.DISCARDED);
        }
        @Override public CompoundTag getPersistentData() { return mob == null ? super.getPersistentData() : mob.getPersistentData(); }
        @Override public boolean hasDisconnected() { return mob != null && (mob.isRemoved() || !mob.isAlive()); }
        @Override public AttributeInstance getAttribute(Attribute key) { return mob != null && mob.getAttribute(key) != null ? mob.getAttribute(key) : super.getAttribute(key); }
        @Override public double getAttributeValue(Attribute key) { return getAttribute(key) == null ? super.getAttributeValue(key) : getAttribute(key).getValue(); }
        @Override public float getHealth() { return mob == null ? super.getHealth() : mob.getHealth(); }
        @Override public void setHealth(float health) { if (mob == null) super.setHealth(health); else mob.setHealth(health); }
        @Override public void heal(float amount) { if (mob != null) mob.heal(amount); }
        @Override public boolean isAlive() { return mob != null && mob.isAlive() && !mob.isRemoved() && !isRemoved(); }
        @Override public boolean isInvulnerableTo(DamageSource source) { return false; }
        @Override public boolean hurt(DamageSource source, float amount) { return mob != null && mob.hurt(source, amount); }
        @Override public boolean addEffect(MobEffectInstance effect, Entity source) { return mob == null ? super.addEffect(effect, source) : mob.addEffect(effect, source); }
        @Override public void setDeltaMovement(Vec3 velocity) { if (mob == null) super.setDeltaMovement(velocity); else { mob.setDeltaMovement(velocity); mob.hurtMarked = true; } }
        @Override public void setDeltaMovement(double x,double y,double z){setDeltaMovement(new Vec3(x,y,z));}
        @Override public boolean hasEffect(net.minecraft.world.effect.MobEffect effect){return mob==null?super.hasEffect(effect):mob.hasEffect(effect);}
        @Override public MobEffectInstance getEffect(net.minecraft.world.effect.MobEffect effect){return mob==null?super.getEffect(effect):mob.getEffect(effect);}
        @Override public boolean removeEffect(net.minecraft.world.effect.MobEffect effect){return mob==null?super.removeEffect(effect):mob.removeEffect(effect);}
        @Override public void setNoGravity(boolean value){if(mob==null)super.setNoGravity(value);else mob.setNoGravity(value);}
        @Override public boolean isNoGravity(){return mob==null?super.isNoGravity():mob.isNoGravity();}
        @Override public Vec3 getDeltaMovement() { return mob == null ? super.getDeltaMovement() : mob.getDeltaMovement(); }
        @Override public void teleportTo(double x, double y, double z) { if (mob != null) mob.teleportTo(x, y, z); setPos(x, y, z); }
        @Override public void sendSystemMessage(Component message) { }
        @Override public void displayClientMessage(Component message, boolean actionBar) { }
        @Override public float getAttackStrengthScale(float partial) { return 1f; }
        @Override public float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) { return mob == null ? super.getStandingEyeHeight(pose, dimensions) : mob.getEyeHeight(); }
        @Override public Iterable<ItemStack> getArmorSlots() { return mob == null ? super.getArmorSlots() : mob.getArmorSlots(); }
        @Override public ItemStack getItemBySlot(EquipmentSlot slot) { return mob == null ? super.getItemBySlot(slot) : mob.getItemBySlot(slot); }
        @Override public void setItemSlot(EquipmentSlot slot, ItemStack item) { if (mob == null) super.setItemSlot(slot, item); else mob.setItemSlot(slot, item); }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
    }
    private NpcBridge() { }
}
