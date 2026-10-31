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
        target.discard();
        p.getPersistentData().put(PENDING, snap);
        cn.blockforge.generated.nnnnr1201forge2.Network.send((net.minecraft.server.level.ServerLevel) p.level(),
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
        tag.putFloat("width", target.getBbWidth());
        tag.putFloat("height", target.getBbHeight());
        tag.putString("form", "body");
        return tag;
    }

    public static void handle(ServerPlayer p, int action) {
        com.mojang.logging.LogUtils.getLogger().info("[无为] 服务端收到 action={}", action);
        if (action == 0) { storePending(p); return; }
        if (action >= 1000) { MahitoPowers.materialize(p, action - 1000); return; }
        if (action == 200) { MahitoPowers.wall(p); return; }
        if (action == 201) { MahitoPowers.bridge(p); return; }
        if (action == 300) { MahitoPowers.potential(p); return; }
        if (action >= FORM_CURSED_BASE && action <= FORM_CURSED_BASE + 4) { transformCursed(p, action - FORM_CURSED_BASE); return; }
        if (action >= FORM_SWORD && action <= FORM_SHOVEL) transform(p, action);
    }

    private static void storePending(ServerPlayer p) {
        if (!p.getPersistentData().contains(PENDING)) return;
        ItemStack body = new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_BODY.get());
        CompoundTag snap = p.getPersistentData().getCompound(PENDING).copy();
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
