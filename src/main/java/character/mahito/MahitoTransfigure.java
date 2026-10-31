package character.mahito;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 无为转变：
 * ① 1 格内释放 → 锁定；再击中一次 → 目标立刻消失（不判死亡、不掉落），贮存快照并弹贮存轮盘；
 * ② 自闭圆盾裹内释放 → 自动锁定「离准星最近」的目标并直接贮存（无距离限制、无需击打）；
 * ③ 右键任意无为转变物品 → 多页变化轮盘（工具 / 咒具 / 具象化 / 攻击 / 潜能激发）。
 */
public final class MahitoTransfigure {
    public static final String BASE_TAG = "SukunaTransfigured";
    public static final String PENDING = "SukunaPendingTransfig";
    public static final String PENDING_TARGET = "SukunaPendingTarget";
    public static final int FORM_SWORD = 1, FORM_AXE = 2, FORM_PICKAXE = 3, FORM_HOE = 4, FORM_SHOVEL = 5, FORM_CURSED = 6;
    public static final int FORM_CURSED_BASE = 100;

    private static final int ARM_TICKS = 200;
    private static final String[] TIER_NAMES = {"木", "石", "金", "铁", "钻石", "下界合金"};
    private static final String[] CURSED_NAMES = {"黑绳", "屠坐魔", "游云", "神武解", "飞天"};

    private record Armed(UUID target, long at) {}
    private static final Map<UUID, Armed> ARMED = new HashMap<>();

    public static void arm(ServerPlayer p) {
        if (character.domainking.SelfEmbryoDomain.active(p)) {
            Vec3 look = p.getLookAngle();
            LivingEntity inDomain = p.level().getEntitiesOfClass(LivingEntity.class,
                    new AABB(p.position(), p.position()).inflate(30),
                    e -> e != p && e.isAlive() && !(e instanceof Player))
                .stream()
                .min(java.util.Comparator.comparingDouble(e -> {
                    Vec3 to = e.getBoundingBox().getCenter().subtract(p.getEyePosition());
                    return (1.0 - to.normalize().dot(look)) * 100.0 + p.distanceToSqr(e) * 0.001;
                })).orElse(null);
            if (inDomain == null) {
                p.sendSystemMessage(Component.literal("无为转变：领域内没有可锁定的目标").withStyle(ChatFormatting.GRAY));
                return;
            }
            store(p, inDomain);
            return;
        }
        boolean noneAround = p.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(p.position(), p.position()).inflate(16),
                e -> e != p && e.isAlive() && !(e instanceof Player)).isEmpty();
        if (noneAround) {
            cn.blockforge.generated.nnnnr1201forge2.Network.sendTo(p,
                new cn.blockforge.generated.nnnnr1201forge2.Network.Effect(300, p.getId(), p.position(),
                    Vec3.ZERO, 3f, 0, 0));
            p.sendSystemMessage(Component.literal("无为转变：周围没有生物 → 自我改变").withStyle(ChatFormatting.AQUA));
            return;
        }
        LivingEntity target = p.level().getEntitiesOfClass(LivingEntity.class,
                p.getBoundingBox().inflate(1.0),
                e -> e != p && e.isAlive() && !(e instanceof Player))
            .stream().min(java.util.Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
        if (target == null) {
            p.sendSystemMessage(Component.literal("无为转变：需要 1 格内有敌人").withStyle(ChatFormatting.GRAY));
            return;
        }
        ARMED.put(p.getUUID(), new Armed(target.getUUID(), p.level().getGameTime()));
        p.sendSystemMessage(Component.literal("无为转变·锁定 " + target.getName().getString() + "：击中一次即可贮存")
            .withStyle(ChatFormatting.AQUA));
    }

    /** 剑（及其它无为转变武器）：最终伤害改成贮存生物的攻击力。 */
    @SubscribeEvent public static void swordDamage(LivingHurtEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        if (e.getAmount() <= 0) return;
        ItemStack held = p.getMainHandItem();
        if (!(held.getItem() instanceof TransfiguredSwordItem)) return;
        CompoundTag tag = held.getTagElement(BASE_TAG);
        if (tag == null || !tag.contains("attack")) return;
        e.setAmount(Math.max(0.5f, tag.getFloat("attack")));
    }

    @SubscribeEvent public static void hurt(LivingHurtEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        Armed armed = ARMED.get(p.getUUID());
        if (armed == null) return;
        if (!e.getEntity().getUUID().equals(armed.target())) return;
        if (p.level().getGameTime() - armed.at() > ARM_TICKS) { ARMED.remove(p.getUUID()); return; }
        if (p.distanceToSqr(e.getEntity()) > 9) return;
        ARMED.remove(p.getUUID());
        e.setCanceled(true);
        store(p, e.getEntity());
    }

    private static void store(ServerPlayer p, LivingEntity target) {
        CompoundTag snap = snapshot(target);
        p.getPersistentData().put(PENDING, snap);
        p.getPersistentData().putUUID(PENDING_TARGET, target.getUUID());
        cn.blockforge.generated.nnnnr1201forge2.Network.sendTo(p,
            new cn.blockforge.generated.nnnnr1201forge2.Network.Effect(300, p.getId(), p.position(),
                Vec3.ZERO, 0f, 0, 0));
        p.sendSystemMessage(Component.literal("无为转变：选择要贮存的形态").withStyle(ChatFormatting.AQUA));
    }

    private static CompoundTag snapshot(LivingEntity target) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType())));
        tag.putString("name", target.getName().getString());
        tag.putFloat("health", target.getHealth());
        tag.putFloat("maxHealth", target.getMaxHealth());
        var atk = target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        tag.putFloat("attack", atk == null ? 1f : (float) atk.getValue());
        tag.putFloat("width", target.getBbWidth());
        tag.putFloat("height", target.getBbHeight());
        tag.putString("form", "body");
        return tag;
    }

    /** 各动作的咒力消耗。 */
    private static double costOf(int action) {
        if (action == 0) return 200;                       // 储存
        if (action >= 1 && action <= 5) return 100;        // 工具
        if (action >= 100 && action <= 104) return 300;    // 咒具
        if (action == 200 || action == 201) return 500;    // 墙 / 桥
        if (action == 300) return 800;                     // 潜能激发
        if (action == 400 || (action >= 401 && action <= 406) || action == 410) return 0; // 改变是纯粹的微调
        if (action >= 1000) return 400;                    // 具象化
        if (action == MahitoSelf.STORAGE_HEAL || action == MahitoSelf.HEAL) return 600;
        if (action == MahitoSelf.STORAGE_BURST) return 700;
        if (action == MahitoSelf.CLONE) return 900;
        if (action == MahitoSelf.SPIRIT) return 1200;
        if (action == MahitoSelf.LIMBS) return 700;
        return 0;
    }

    public static void handle(ServerPlayer p, int action) {
        double cost = costOf(action);
        if (cost > 0) {
            if (cn.blockforge.generated.nnnnr1201forge2.Curse.energy(p) < cost) {
                p.sendSystemMessage(Component.literal("咒力不足：需要 " + (int) cost).withStyle(ChatFormatting.GRAY));
                return;
            }
            cn.blockforge.generated.nnnnr1201forge2.Curse.add(p, -cost);
        }
        com.mojang.logging.LogUtils.getLogger().info("[无为] 服务端收到 action={}", action);
        if (action == 0) { storePending(p); return; }
        if (action >= 1000) { MahitoPowers.materialize(p, action - 1000); return; }
        if (action == 200) { MahitoPowers.wall(p); return; }
        if (action == 201) { MahitoPowers.bridge(p); return; }
        if (action == 300) { MahitoPowers.potential(p); return; }
        if (action == 400) { sendModelData(p); return; }
        if (action == MahitoSelf.STORAGE_HEAL || action == MahitoSelf.HEAL) { MahitoSelf.heal(p); return; }
        if (action == MahitoSelf.STORAGE_BURST) { MahitoSelf.burst(p, pendingTarget(p)); return; }
        if (action == MahitoSelf.CLONE) { MahitoSelf.clone(p); return; }
        if (action == MahitoSelf.SPIRIT) { MahitoSelf.spirit(p); return; }
        if (action == MahitoSelf.LIMBS) { MahitoSelf.limbs(p); return; }
        if (action == MahitoSelf.LIMB_USE) { MahitoSelf.limbUse(p); return; }
        if (action == MahitoSelf.LIMB_STORE) { MahitoSelf.limbStore(p); return; }
        if (action >= MahitoSelf.LIMB_USE_AT && action < MahitoSelf.LIMB_USE_AT + 4) {
            MahitoSelf.limbUseAt(p, action - MahitoSelf.LIMB_USE_AT); return;
        }
        if (action >= 401 && action <= 406) { adjust(p, action); return; }
        if (action == 410) { applyToMonster(p); return; }
        if (action >= FORM_CURSED_BASE && action <= FORM_CURSED_BASE + 4) { transformCursed(p, action - FORM_CURSED_BASE); return; }
        if (action >= FORM_SWORD && action <= FORM_SHOVEL) transform(p, action);
    }

    private static LivingEntity pendingTarget(ServerPlayer p) {
        if (p.level() instanceof net.minecraft.server.level.ServerLevel level
            && p.getPersistentData().hasUUID(PENDING_TARGET)) {
            var e = level.getEntity(p.getPersistentData().getUUID(PENDING_TARGET));
            if (e instanceof LivingEntity living && living.isAlive()) return living;
        }
        return p.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(p.position(), p.position()).inflate(8),
                e -> e != p && e.isAlive()).stream()
            .min(java.util.Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
    }

    private static void sendModelData(ServerPlayer p) {
        CompoundTag snap = p.getPersistentData().getCompound(PENDING);
        if (snap.isEmpty()) return;
        sendModel(p, snap);
    }

    private static void sendModel(ServerPlayer p, CompoundTag snap) {
        cn.blockforge.generated.nnnnr1201forge2.Network.sendTo(p,
            new cn.blockforge.generated.nnnnr1201forge2.Network.Effect(302, p.getId(),
                new Vec3(snap.getFloat("width"), snap.getFloat("height"), snap.getFloat("health")),
                Vec3.ZERO, 0f, 0, 0));
    }

    private static void adjust(ServerPlayer p, int action) {
        CompoundTag snap = p.getPersistentData().getCompound(PENDING).copy();
        if (snap.isEmpty()) return;
        float w = snap.getFloat("width"), h = snap.getFloat("height"), hp = snap.getFloat("health");
        switch (action) {
            case 401 -> h = Math.min(8f, h + 0.2f);
            case 402 -> h = Math.max(0.4f, h - 0.2f);
            case 403 -> w = Math.min(6f, w + 0.2f);
            case 404 -> w = Math.max(0.2f, w - 0.2f);
            case 405 -> hp = Math.min(1000f, hp + 5f);
            default -> hp = Math.max(1f, hp - 5f);
        }
        snap.putFloat("width", w);
        snap.putFloat("height", h);
        snap.putFloat("health", hp);
        snap.putFloat("maxHealth", hp);
        p.getPersistentData().put(PENDING, snap);
        sendModel(p, snap);
    }

    /** 「完成」：把调整直接应用到那只怪物身上（生命与渲染体型），不储存。 */
    private static void applyToMonster(ServerPlayer p) {
        CompoundTag snap = p.getPersistentData().getCompound(PENDING);
        if (snap.isEmpty() || !(p.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        if (p.getPersistentData().hasUUID(PENDING_TARGET)) {
            var e = level.getEntity(p.getPersistentData().getUUID(PENDING_TARGET));
            if (e instanceof LivingEntity living) {
                float hp = Math.max(1f, snap.getFloat("health"));
                if (living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != null) {
                    living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(hp);
                }
                living.setHealth(hp);
                float baseW = Math.max(0.1f, living.getBbWidth());
                float baseH = Math.max(0.2f, living.getBbHeight());
                float scaleW = snap.getFloat("width") / baseW;
                float scaleH = snap.getFloat("height") / baseH;
                cn.blockforge.generated.nnnnr1201forge2.Network.send(level,
                    new cn.blockforge.generated.nnnnr1201forge2.Network.Effect(303, living.getId(), living.position(),
                        Vec3.ZERO, scaleW, (int) Math.max(1, scaleH * 1000), 0));
                p.sendSystemMessage(Component.literal("无为转变·改变：" + living.getName().getString()
                    + " 的形态已被改写（生命 " + (int) hp + "）").withStyle(ChatFormatting.AQUA));
            }
        }
        p.getPersistentData().remove(PENDING);
        p.getPersistentData().remove(PENDING_TARGET);
    }

    private static void storePending(ServerPlayer p) {
        if (!p.getPersistentData().contains(PENDING)) return;
        ItemStack body = new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_BODY.get());
        CompoundTag snap = p.getPersistentData().getCompound(PENDING).copy();
        // 点击储存的这一刻才让怪物消失（不判死亡、不掉落）
        if (p.getPersistentData().hasUUID(PENDING_TARGET)
            && p.level() instanceof net.minecraft.server.level.ServerLevel level) {
            var e = level.getEntity(p.getPersistentData().getUUID(PENDING_TARGET));
            if (e != null) e.discard();
        }
        p.getPersistentData().remove(PENDING_TARGET);
        body.getOrCreateTag().put(BASE_TAG, snap);
        p.getPersistentData().remove(PENDING);
        body.setHoverName(Component.literal("贮存体·" + snap.getString("name")).withStyle(ChatFormatting.AQUA));
        body.setDamageValue(0);
        if (!p.getInventory().add(body)) p.drop(body, false);
        p.sendSystemMessage(Component.literal("无为转变·贮存完成：右键可打开轮盘").withStyle(ChatFormatting.AQUA));
    }

    private static int tierIndex(float hp) {
        if (hp < 5) return 0;
        if (hp < 10) return 1;
        if (hp < 20) return 2;
        if (hp < 40) return 3;
        if (hp < 110) return 4;
        return 5;
    }

    public static boolean isTransfigured(ItemStack stack) {
        net.minecraft.world.item.Item item = stack.getItem();
        if (item == cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_BODY.get()
            || item == cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_SWORD.get()) return true;
        for (int i = 0; i < 6; i++) {
            if (item == cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_AXES[i].get()
                || item == cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_PICKAXES[i].get()
                || item == cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_HOES[i].get()
                || item == cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_SHOVELS[i].get()) return true;
        }
        return false;
    }

    private static void transform(ServerPlayer p, int form) {
        ItemStack held = p.getMainHandItem();
        CompoundTag base = held.getTagElement(BASE_TAG);
        if (base == null) return;
        int tier = tierIndex(base.getFloat("maxHealth"));
        ItemStack out;
        String label;
        switch (form) {
            case FORM_SWORD -> { out = new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_SWORD.get()); label = "剑"; }
            case FORM_AXE -> { out = new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_AXES[tier].get()); label = TIER_NAMES[tier] + "斧"; }
            case FORM_PICKAXE -> { out = new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_PICKAXES[tier].get()); label = TIER_NAMES[tier] + "镐"; }
            case FORM_HOE -> { out = new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_HOES[tier].get()); label = TIER_NAMES[tier] + "锄"; }
            default -> { out = new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_SHOVELS[tier].get()); label = TIER_NAMES[tier] + "铲"; }
        }
        finish(p, held, out, label, false);
    }

    private static void transformCursed(ServerPlayer p, int index) {
        ItemStack held = p.getMainHandItem();
        if (held.getTagElement(BASE_TAG) == null || index < 0 || index > 4) return;
        ItemStack out = switch (index) {
            case 0 -> new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.BLACK_ROPE.get());
            case 1 -> new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.SLAUGHTER_DEMON.get());
            case 2 -> new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.YUYUN.get());
            case 3 -> new ItemStack(cn.blockforge.generated.nnnnr1201forge2.VoidContent.SHINBUKE.get());
            default -> new ItemStack(cn.blockforge.generated.nnnnr1201forge2.VoidContent.HITEN.get());
        };
        finish(p, held, out, CURSED_NAMES[index], true);
    }

    private static void finish(ServerPlayer p, ItemStack held, ItemStack out, String label, boolean cursed) {
        CompoundTag base = held.getTagElement(BASE_TAG);
        if (base == null) return;
        out.getOrCreateTag().put(BASE_TAG, base.copy());
        out.setHoverName(Component.literal(label + "·" + base.getString("name")).withStyle(ChatFormatting.AQUA));
        if (cursed) {
            int max = out.getMaxDamage();
            out.setDamageValue(Math.max(0, max - Math.max(1, max / 100)));
        } else {
            out.setDamageValue(0);
        }
        p.getInventory().setItem(p.getInventory().selected, out);
        p.sendSystemMessage(Component.literal("无为转变：形态变为「" + label + "」").withStyle(ChatFormatting.AQUA));
    }

    @SubscribeEvent public static void rightClick(PlayerInteractEvent.RightClickItem e) {
        if (!isTransfigured(e.getItemStack()) || e.getItemStack().getTagElement(BASE_TAG) == null) return;
        e.setCanceled(true);
        if (e.getLevel().isClientSide) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> TransfigureScreen.open(1));
        }
    }

    private MahitoTransfigure() {}
}
