package character.mahito;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 无为转变（第一版）：
 * ① 敌人站在玩家 1 格内时释放术式 → 锁定该目标；
 * ② 再击打一次目标 → 贮存其数据（id / 名字 / 当前与最大生命 / 碰撞箱大小），生成「贮存体」物品；
 * ③ 贮存体右键打开菜单 → 「1 变成剑」把它转成剑形。
 * 后续轮盘/更多形态在 {@link #BASE_TAG} 上扩展。
 */
public final class MahitoTransfigure {
    /** 预留的 NBT 根键：以后加新形态只需要往这个 tag 里加字段。 */
    public static final String BASE_TAG = "SukunaTransfigured";
    private static final int ARM_TICKS = 200;
    /** 命中后暂存的待贮存快照（等轮盘选择）。 */
    public static final String PENDING = "SukunaPendingTransfig";

    private record Armed(UUID target, long at) {}
    private static final Map<UUID, Armed> ARMED = new HashMap<>();

    /** 施放 2100 时调用：锁定 1 格内最近的目标。 */
    public static void arm(ServerPlayer p) {
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

    /** 击打目标后贮存：生成带 NBT 的贮存体物品。 */
    @SubscribeEvent public static void hurt(LivingHurtEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        Armed armed = ARMED.get(p.getUUID());
        if (armed == null) return;
        if (!e.getEntity().getUUID().equals(armed.target())) return;
        if (p.level().getGameTime() - armed.at() > ARM_TICKS) { ARMED.remove(p.getUUID()); return; }
        if (p.distanceToSqr(e.getEntity()) > 9) return;
        ARMED.remove(p.getUUID());
        LivingEntity target = e.getEntity();
        p.getPersistentData().put(PENDING, snapshot(target));
        cn.blockforge.generated.nnnnr1201forge2.Network.CHANNEL.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p),
            new cn.blockforge.generated.nnnnr1201forge2.Network.OpenTransfigure(0));
        p.sendSystemMessage(Component.literal("无为转变：选择要贮存的形态").withStyle(ChatFormatting.AQUA));
    }

    /** 生成形态快照（给后续形态/轮盘预留字段）。 */
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

    /** 轮盘回调：action 0 = 储存（生成贮存体），1 = 变成剑。 */
    public static void handle(ServerPlayer p, int action) {
        if (action == 0) {
            if (!p.getPersistentData().contains(PENDING)) return;
            ItemStack body = new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_BODY.get());
            body.getOrCreateTag().put(BASE_TAG, p.getPersistentData().getCompound(PENDING).copy());
            p.getPersistentData().remove(PENDING);
            if (!p.getInventory().add(body)) p.drop(body, false);
            p.sendSystemMessage(Component.literal("无为转变·贮存完成：右键贮存体可打开轮盘").withStyle(ChatFormatting.AQUA));
            return;
        }
        if (action != 1) return;
        ItemStack held = p.getMainHandItem();
        if (!held.is(cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_BODY.get())) return;
        CompoundTag tag = held.getTag();
        ItemStack sword = new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_SWORD.get());
        if (tag != null) sword.setTag(tag.copy());
        p.getInventory().setItem(p.getInventory().selected, sword);
        p.sendSystemMessage(Component.literal("无为转变：形态变为「剑」").withStyle(ChatFormatting.AQUA));
    }

    private MahitoTransfigure() {}
}
