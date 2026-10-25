package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * NPC 走"玩家技能"的桥：照抄女仆 MaidBridge.Actor 的思路，
 * 把敌对生物包成一个 {@link FakePlayer}（{@link NpcActor}），再交给玩家技能入口。
 * 玩家技能链完全不用改；伤害归属用 {@link #body(ServerPlayer)} 换回 NPC 本体。
 */
public final class NpcBridge {
    private static final Map<UUID, NpcActor> ACTORS = new HashMap<>();

    /** 取（或创建）某只生物的假玩家施术者。 */
    public static NpcActor actorFor(LivingEntity mob) {
        if (mob == null || mob.level().isClientSide || !(mob.level() instanceof ServerLevel level)) return null;
        return ACTORS.computeIfAbsent(mob.getUUID(), id -> new NpcActor(level, mob));
    }

    /** 假玩家 → NPC 本体（供 MaidBridge.body / CombatSafety 使用）。 */
    public static LivingEntity body(ServerPlayer p) {
        return p instanceof NpcActor a ? a.mob : p;
    }

    public static boolean isNpcActor(Player p) { return p instanceof NpcActor; }

    /** NPC 的敌友：攻击玩家（非创造/旁观）、其它非己方生物；不打自己与同类 NPC。 */
    public static boolean canHit(NpcActor actor, LivingEntity target) {
        if (target == null || actor == null || target == actor.mob) return false;
        if (!target.isAlive() || target.isSpectator()) return false;
        if (target instanceof Player p) return !p.isCreative() && !p.isSpectator() && !(p instanceof NpcActor);
        return !(target instanceof Mob m && ACTORS.containsKey(m.getUUID()));
    }

    public static void tick() {
        for (var it = ACTORS.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            NpcActor a = e.getValue();
            if (a.mob == null || !a.mob.isAlive() || a.mob.level() != a.level()) { a.setRemoved(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED); it.remove(); continue; }
            a.syncPosition();
            if (a.mob.tickCount % 20 == 0) {
                NpcCurse.add(a.mob, 0);                 // 初始化咒力池
                a.setHealth(a.mob.getHealth());
            }
        }
    }

    public static void clear() { ACTORS.clear(); }

    public static final class NpcActor extends FakePlayer {
        public final LivingEntity mob;
        private NpcActor(ServerLevel level, LivingEntity mob) {
            super(level, new GameProfile(mob.getUUID(), "SukunaNpc"));
            this.mob = mob;
            setId(mob.getId());
            refreshDimensions();
            syncPosition();
        }
        public void syncPosition() {
            setPos(mob.getX(), mob.getY(), mob.getZ());
            setYRot(mob.getYRot());
            setXRot(mob.getXRot());
            tickCount = mob.tickCount;
            var target = mob instanceof Mob m ? m.getTarget() : null;
            if (target != null) {
                Vec3 d = target.getEyePosition().subtract(mob.getEyePosition());
                setYRot((float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90));
                setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
            }
        }
        @Override public CompoundTag getPersistentData() { return mob == null ? super.getPersistentData() : mob.getPersistentData(); }
        @Override public float getHealth() { return mob == null ? super.getHealth() : mob.getHealth(); }
        @Override public void setHealth(float health) { if (mob == null) super.setHealth(health); else mob.setHealth(health); }
        @Override public void heal(float amount) { if (mob != null) mob.heal(amount); }
        @Override public boolean isAlive() { return mob != null && mob.isAlive() && !isRemoved(); }
        @Override public boolean isInvulnerableTo(DamageSource source) { return false; }
        @Override public boolean hurt(DamageSource source, float amount) { return mob != null && mob.hurt(source, amount); }
        @Override public void teleportTo(double x, double y, double z) { if (mob != null) mob.teleportTo(x, y, z); setPos(x, y, z); }
        @Override public void sendSystemMessage(Component message) { }
        @Override public void displayClientMessage(Component message, boolean actionBar) { }
        @Override public float getAttackStrengthScale(float partial) { return 1f; }
        @Override public Iterable<ItemStack> getArmorSlots() { return mob == null ? super.getArmorSlots() : mob.getArmorSlots(); }
        @Override public ItemStack getItemBySlot(EquipmentSlot slot) { return mob == null ? super.getItemBySlot(slot) : mob.getItemBySlot(slot); }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
    }

    private NpcBridge() { }
}
