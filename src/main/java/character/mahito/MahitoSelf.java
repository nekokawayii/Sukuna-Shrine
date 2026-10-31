package character.mahito;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.UUID;

/** 无为转变的「自我改变」与贮存轮盘的辅助功能：治疗 / 爆裂 / 遍杀即灵体 / 肢体。 */
public final class MahitoSelf {
    public static final int HEAL = 510, CLONE = 511, SPIRIT = 512, LIMBS = 513;
    public static final int STORAGE_HEAL = 500, STORAGE_BURST = 501;
    private static final long HEAL_COOLDOWN = 4000;   // 200 秒
    private static final long SPIRIT_TICKS = 1200;    // 遍杀即灵体 60 秒

    private static final UUID SPIRIT_HEALTH = UUID.fromString("5f2b1a3c-1111-4a4a-9c9c-000000000001");
    private static final UUID SPIRIT_ATTACK = UUID.fromString("5f2b1a3c-1111-4a4a-9c9c-000000000002");
    private static final UUID SPIRIT_ARMOR = UUID.fromString("5f2b1a3c-1111-4a4a-9c9c-000000000003");
    private static final UUID SPIRIT_SPEED = UUID.fromString("5f2b1a3c-1111-4a4a-9c9c-000000000004");

    public static final String SPIRIT_FLAG = "MahitoSpirit";
    public static final String ARMS = "MahitoArms";
    public static final String BURST_USED = "MahitoBurstUsed";

    /** 治疗：把生命恢复到上限（冷却 200 秒）。目标优先给自己或 8 格内最近的玩家。 */
    public static void heal(ServerPlayer p) {
        long now = p.level().getGameTime();
        long ready = cn.blockforge.generated.nnnnr1201forge2.Curse.data(p).getLong("MahitoHealReady");
        if (now < ready) {
            p.sendSystemMessage(Component.literal("治疗冷却中 · " + String.format("%.1f", (ready - now) / 20.0) + " 秒")
                .withStyle(ChatFormatting.GRAY));
            return;
        }
        ServerPlayer[] target = {p};
        p.level().getEntitiesOfClass(ServerPlayer.class, new AABB(p.position(), p.position()).inflate(8))
            .stream().filter(t -> t != p).min(Comparator.comparingDouble(p::distanceToSqr)).ifPresent(t -> target[0] = t);
        LivingEntity t = target[0];
        t.setHealth(t.getMaxHealth());
        t.removeAllEffects();
        t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 2, false, false));
        cn.blockforge.generated.nnnnr1201forge2.Curse.data(p).putLong("MahitoHealReady", now + HEAL_COOLDOWN);
        p.sendSystemMessage(Component.literal("无为转变·治疗：" + t.getName().getString() + " 已恢复").withStyle(ChatFormatting.GREEN));
    }

    /** 爆裂：对目标造成「生命上限 -10%」伤害；生命上限 <60 直接炸成血雾；每个目标一次。 */
    public static void burst(ServerPlayer p, LivingEntity target) {
        if (target == null) {
            p.sendSystemMessage(Component.literal("爆裂：没有目标").withStyle(ChatFormatting.GRAY));
            return;
        }
        CompoundTag data = cn.blockforge.generated.nnnnr1201forge2.Curse.data(p);
        CompoundTag used = data.getCompound(BURST_USED);
        String key = target.getUUID().toString();
        if (used.getBoolean(key)) {
            p.sendSystemMessage(Component.literal("爆裂：这个目标已经用过一次").withStyle(ChatFormatting.GRAY));
            return;
        }
        used.putBoolean(key, true);
        data.put(BURST_USED, used);
        float hp = target.getMaxHealth();
        if (hp < 60f) {
            target.discard();
            if (p.level() instanceof net.minecraft.server.level.ServerLevel level) {
                cn.blockforge.generated.nnnnr1201forge2.Network.send(level,
                    new cn.blockforge.generated.nnnnr1201forge2.Network.Effect(304, target.getId(), target.position(),
                        Vec3.ZERO, 3f, 20, 0));
            }
            p.sendSystemMessage(Component.literal("爆裂：" + target.getName().getString() + " 炸成一团血雾")
                .withStyle(ChatFormatting.DARK_RED));
            return;
        }
        target.hurt(p.damageSources().magic(), hp * 0.9f);
        if (p.level() instanceof net.minecraft.server.level.ServerLevel level) {
            cn.blockforge.generated.nnnnr1201forge2.Network.send(level,
                new cn.blockforge.generated.nnnnr1201forge2.Network.Effect(304, target.getId(), target.position(),
                    Vec3.ZERO, 2f, 12, 0));
        }
        p.sendSystemMessage(Component.literal("爆裂：" + target.getName().getString() + " 承受 " + (int) (hp * 0.9f) + " 伤害")
            .withStyle(ChatFormatting.DARK_RED));
    }

    /** 遍杀即灵体：减伤 50%、生命上限 ×2、攻击 +25%、防御 ×2、速度 ×2。 */
    public static void spirit(ServerPlayer p) {
        if (cn.blockforge.generated.nnnnr1201forge2.Curse.data(p).getBoolean(SPIRIT_FLAG)) {
            p.sendSystemMessage(Component.literal("遍杀即灵体：已经处于该形态").withStyle(ChatFormatting.GRAY));
            return;
        }
        cn.blockforge.generated.nnnnr1201forge2.Curse.data(p).putBoolean(SPIRIT_FLAG, true);
        cn.blockforge.generated.nnnnr1201forge2.Curse.data(p).putLong("MahitoSpiritUntil", p.level().getGameTime() + SPIRIT_TICKS);
        apply(p, Attributes.MAX_HEALTH, SPIRIT_HEALTH, 1.0, "MahitoSpiritHealth");
        apply(p, Attributes.ATTACK_DAMAGE, SPIRIT_ATTACK, 0.25, "MahitoSpiritAttack");
        apply(p, Attributes.ARMOR, SPIRIT_ARMOR, 1.0, "MahitoSpiritArmor");
        apply(p, Attributes.MOVEMENT_SPEED, SPIRIT_SPEED, 1.0, "MahitoSpiritSpeed");
        p.setHealth(p.getMaxHealth());
        spawnTail(p);
        p.sendSystemMessage(Component.literal("遍杀即灵体（60 秒）：减伤 50% · 生命上限 ×2 · 攻击 +25% · 防御 ×2 · 速度 ×2")
            .withStyle(ChatFormatting.DARK_PURPLE));
    }

    private static void apply(ServerPlayer p, net.minecraft.world.entity.ai.attributes.Attribute attr,
                              UUID id, double amount, String name) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null || inst.getModifier(id) != null) return;
        inst.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    /** 肢体：增加一只手臂（最多 4），并把当前数量同步给客户端用于打开肢体菜单。 */
    public static void limbs(ServerPlayer p) {
        CompoundTag data = cn.blockforge.generated.nnnnr1201forge2.Curse.data(p);
        int arms = Math.min(4, data.getInt(ARMS) + 1);
        data.putInt(ARMS, arms);
        if (p.level() instanceof net.minecraft.server.level.ServerLevel level) {
            cn.blockforge.generated.nnnnr1201forge2.Network.send(level,
                new cn.blockforge.generated.nnnnr1201forge2.Network.Effect(305, p.getId(), p.position(),
                    Vec3.ZERO, arms, 0, 0));
        }
        p.sendSystemMessage(Component.literal("无为转变·肢体：手臂 " + arms + " / 4（按 U 打开肢体菜单）")
            .withStyle(ChatFormatting.AQUA));
    }

    private static final UUID CLONE_HEALTH = UUID.fromString("5f2b1a3c-1111-4a4a-9c9c-000000000005");
    private static final String CLONE_ID = "MahitoCloneId", TAIL_ID = "MahitoTailId";

    /** 分身：血量对半、装备相同、友方；再按一次或死亡则收回并恢复上限。 */
    public static void clone(ServerPlayer p) {
        if (!(p.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        if (removeExisting(p, CLONE_ID)) { restoreHealth(p); return; }
        character.mahito.MahitoCloneEntity clone = cn.blockforge.generated.nnnnr1201forge2.Content.MAHITO_CLONE.get().create(level);
        if (clone == null) return;
        clone.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0f);
        clone.getPersistentData().putUUID(character.mahito.MahitoCloneEntity.OWNER, p.getUUID());
        clone.setCustomName(Component.literal(p.getName().getString() + "·分身"));
        level.addFreshEntity(clone);
        float half = Math.max(1f, p.getMaxHealth() / 2f);
        if (clone.getAttribute(Attributes.MAX_HEALTH) != null) clone.getAttribute(Attributes.MAX_HEALTH).setBaseValue(half);
        clone.setHealth(half);
        for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            if (slot.getType() == net.minecraft.world.entity.EquipmentSlot.Type.HAND || slot.isArmor())
                clone.setItemSlot(slot, p.getItemBySlot(slot).copy());
        }
        AttributeInstance max = p.getAttribute(Attributes.MAX_HEALTH);
        if (max != null && max.getModifier(CLONE_HEALTH) == null)
            max.addPermanentModifier(new AttributeModifier(CLONE_HEALTH, "MahitoClone", -0.5, AttributeModifier.Operation.MULTIPLY_TOTAL));
        cn.blockforge.generated.nnnnr1201forge2.Curse.data(p).putUUID(CLONE_ID, clone.getUUID());
        p.setHealth(Math.min(p.getHealth(), half));
        p.sendSystemMessage(Component.literal("无为转变·分身：血量对半（当前 " + (int) p.getHealth() + "）").withStyle(ChatFormatting.AQUA));
    }

    /** 遍杀即灵体附带的「尾巴」：小号友方分身，跟随并攻击面前敌人。 */
    public static void spawnTail(ServerPlayer p) {
        if (!(p.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        removeExisting(p, TAIL_ID);
        character.mahito.MahitoCloneEntity tail = cn.blockforge.generated.nnnnr1201forge2.Content.MAHITO_CLONE.get().create(level);
        if (tail == null) return;
        tail.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0f);
        tail.getPersistentData().putUUID(character.mahito.MahitoCloneEntity.OWNER, p.getUUID());
        tail.getPersistentData().putBoolean(character.mahito.MahitoCloneEntity.TAIL, true);
        tail.setCustomName(Component.literal("尾巴"));
        level.addFreshEntity(tail);
        if (tail.getAttribute(Attributes.MAX_HEALTH) != null) tail.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60);
        tail.setHealth(60);
        cn.blockforge.generated.nnnnr1201forge2.Curse.data(p).putUUID(TAIL_ID, tail.getUUID());
    }

    private static boolean removeExisting(ServerPlayer p, String key) {
        CompoundTag data = cn.blockforge.generated.nnnnr1201forge2.Curse.data(p);
        if (!data.hasUUID(key) || !(p.level() instanceof net.minecraft.server.level.ServerLevel level)) return false;
        var e = level.getEntity(data.getUUID(key));
        data.remove(key);
        if (e != null && e.isAlive()) { e.discard(); return true; }
        return true;
    }

    public static void restoreHealth(ServerPlayer p) {
        AttributeInstance max = p.getAttribute(Attributes.MAX_HEALTH);
        if (max != null) max.removeModifier(CLONE_HEALTH);
        // 上限恢复、当前生命不回复（只做上限裁剪）
        p.setHealth(Math.min(p.getHealth(), p.getMaxHealth()));
    }

    /** 服务端 tick：遍杀到期回收、分身死亡恢复上限。 */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            CompoundTag data = cn.blockforge.generated.nnnnr1201forge2.Curse.data(p);
            if (data.getBoolean(SPIRIT_FLAG) && p.level().getGameTime() >= data.getLong("MahitoSpiritUntil")) {
                data.putBoolean(SPIRIT_FLAG, false);
                AttributeInstance max = p.getAttribute(Attributes.MAX_HEALTH);
                for (UUID id : new UUID[]{SPIRIT_HEALTH, SPIRIT_ATTACK, SPIRIT_ARMOR, SPIRIT_SPEED}) {
                    if (max != null) max.removeModifier(id);
                    AttributeInstance inst = switch (id.equals(SPIRIT_ATTACK) ? 1 : id.equals(SPIRIT_ARMOR) ? 2 : id.equals(SPIRIT_SPEED) ? 3 : 0) {
                        case 1 -> p.getAttribute(Attributes.ATTACK_DAMAGE);
                        case 2 -> p.getAttribute(Attributes.ARMOR);
                        case 3 -> p.getAttribute(Attributes.MOVEMENT_SPEED);
                        default -> null;
                    };
                    if (inst != null) inst.removeModifier(id);
                }
                removeExisting(p, TAIL_ID);
                p.sendSystemMessage(Component.literal("遍杀即灵体：已恢复原形").withStyle(ChatFormatting.GRAY));
            }
            if (data.hasUUID(CLONE_ID) && !(p.level() instanceof net.minecraft.server.level.ServerLevel sl
                && sl.getEntity(data.getUUID(CLONE_ID)) instanceof LivingEntity le && le.isAlive())) {
                data.remove(CLONE_ID);
                restoreHealth(p);
                p.sendSystemMessage(Component.literal("分身已消失：生命上限恢复（当前生命不回复）").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /** 治疗减伤：遍杀灵体期间受到伤害减半。 */
    public static float damageScale(ServerPlayer p) {
        return cn.blockforge.generated.nnnnr1201forge2.Curse.data(p).getBoolean(SPIRIT_FLAG) ? 0.5f : 1f;
    }

    private MahitoSelf() {}
}
