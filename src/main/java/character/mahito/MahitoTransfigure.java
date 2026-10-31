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
    public static final int FORM_SWORD=1, FORM_AXE=2, FORM_PICKAXE=3, FORM_HOE=4, FORM_SHOVEL=5, FORM_CURSED=6;
    /** 咒具子页：100 黑绳 / 101 屠坐魔 / 102 游云 / 103 神武解 / 104 飞天。 */
    public static final int FORM_CURSED_BASE=100;
    private static final String[] TIER_NAMES={"木","石","金","铁","钻石","下界合金"};
    private static final String[] TOOL_NAMES={"剑","斧","镐","锄","铲","咒具"};

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
        com.mojang.logging.LogUtils.getLogger().info("[无为] 锁定 {} ({})", target.getId(), target.getName().getString());
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
        CompoundTag snap = snapshot(target);
        // 左键命中即让目标立刻消失：不判定死亡、不触发掉落
        e.setCanceled(true);
        target.discard();
        p.getPersistentData().put(PENDING, snap);
        com.mojang.logging.LogUtils.getLogger().info("[无为] 命中 {}，发送轮盘包 28", target.getId());
        cn.blockforge.generated.nnnnr1201forge2.Network.send((net.minecraft.server.level.ServerLevel) p.level(),
            new cn.blockforge.generated.nnnnr1201forge2.Network.Effect(300, p.getId(), p.position(),
                net.minecraft.world.phys.Vec3.ZERO, 0f, 0, 0));
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
        com.mojang.logging.LogUtils.getLogger().info("[无为] 服务端收到 action={}", action);
        if (action == 0) {
            if (!p.getPersistentData().contains(PENDING)) return;
            ItemStack body = new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_BODY.get());
            var snap = p.getPersistentData().getCompound(PENDING).copy();
            body.getOrCreateTag().put(BASE_TAG, snap);
            body.setHoverName(Component.literal("贮存体·" + snap.getString("name")).withStyle(ChatFormatting.AQUA));
            body.setDamageValue(0);
            p.getPersistentData().remove(PENDING);
            if (!p.getInventory().add(body)) p.drop(body, false);
            p.sendSystemMessage(Component.literal("无为转变·贮存完成：右键贮存体可打开轮盘").withStyle(ChatFormatting.AQUA));
            return;
        }
        if (action >= 1000) { MahitoPowers.materialize(p, action - 1000); return; }
        if (action == 200) { MahitoPowers.wall(p); return; }
        if (action == 201) { MahitoPowers.bridge(p); return; }
        if (action == 300) { MahitoPowers.potential(p); return; }
        if (action >= FORM_CURSED_BASE && action <= FORM_CURSED_BASE + 4) { transformCursed(p, action - FORM_CURSED_BASE); return; }
        if (action < FORM_SWORD || action > FORM_CURSED) return;
        transform(p, action);
    }

    /** 血量决定材质：<5 木 / <10 石 / <20 金 / <40 铁 / <60 与 <110 钻石 / ≥110 下界合金。 */
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

    /** 按当前贮存数据换成指定形态（保留 NBT；工具耐久按材质满耐，其他咒具只留 1%）。 */
    private static void transform(ServerPlayer p, int form) {
        ItemStack held = p.getMainHandItem();
        CompoundTag base = held.getTagElement(BASE_TAG);
        if (base == null) return;
        float hp = base.getFloat("maxHealth");
        int tier = tierIndex(hp);
        ItemStack out;
        String label;
        switch (form) {
            case FORM_SWORD -> { out = new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_SWORD.get()); label = "剑"; }
            case FORM_AXE -> { out = new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_AXES[tier].get()); label = TIER_NAMES[tier] + "斧"; }
            case FORM_PICKAXE -> { out = new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_PICKAXES[tier].get()); label = TIER_NAMES[tier] + "镐"; }
            case FORM_HOE -> { out = new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_HOES[tier].get()); label = TIER_NAMES[tier] + "锄"; }
            case FORM_SHOVEL -> { out = new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.TRANSFIGURED_SHOVELS[tier].get()); label = TIER_NAMES[tier] + "铲"; }
            default -> { out = new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.YUYUN.get()); label = "咒具·游云"; }
        }
        out.getOrCreateTag().put(BASE_TAG, base.copy());
        out.setHoverName(Component.literal(label + "·" + base.getString("name")).withStyle(ChatFormatting.AQUA));
        if (form == FORM_CURSED) {
            int max = out.getMaxDamage();
            out.setDamageValue(Math.max(0, max - Math.max(1, max / 100)));
        } else {
            out.setDamageValue(0);
        }
        p.getInventory().setItem(p.getInventory().selected, out);
        p.sendSystemMessage(Component.literal("无为转变：形态变为「" + label + "」").withStyle(ChatFormatting.AQUA));
    }

    private static final String[] CURSED_NAMES={"黑绳","屠坐魔","游云","神武解","飞天"};

    /** 咒具子页：全部咒具，耐久只保留 1%。 */
    private static void transformCursed(ServerPlayer p, int index) {
        ItemStack held = p.getMainHandItem();
        CompoundTag base = held.getTagElement(BASE_TAG);
        if (base == null || index < 0 || index > 4) return;
        ItemStack out = switch (index) {
            case 0 -> new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.BLACK_ROPE.get());
            case 1 -> new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.SLAUGHTER_DEMON.get());
            case 2 -> new ItemStack(cn.blockforge.generated.nnnnr1201forge2.Content.YUYUN.get());
            case 3 -> new ItemStack(cn.blockforge.generated.nnnnr1201forge2.VoidContent.SHINBUKE.get());
            default -> new ItemStack(cn.blockforge.generated.nnnnr1201forge2.VoidContent.HITEN.get());
        };
        out.getOrCreateTag().put(BASE_TAG, base.copy());
        out.setHoverName(Component.literal(CURSED_NAMES[index] + "·" + base.getString("name")).withStyle(ChatFormatting.AQUA));
        int max = out.getMaxDamage();
        out.setDamageValue(Math.max(0, max - Math.max(1, max / 100)));
        p.getInventory().setItem(p.getInventory().selected, out);
        p.sendSystemMessage(Component.literal("无为转变：形态变为「" + CURSED_NAMES[index] + "」（耐久 1%）").withStyle(ChatFormatting.AQUA));
    }

    /** 右键任意「无为转变」物品 → 打开变化轮盘（mode=1）。 */
    @SubscribeEvent public static void rightClick(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem e) {
        if (!isTransfigured(e.getItemStack()) || e.getItemStack().getTagElement(BASE_TAG) == null) return;
        e.setCanceled(true);
        if (e.getLevel().isClientSide) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> TransfigureScreen.open(1));
        }
    }

    private MahitoTransfigure() {}
}
