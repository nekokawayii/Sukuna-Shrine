package character.mahito;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 无为转变的「攻击 / 具象化 / 潜能激发」实体效果与临时地形。 */
public final class MahitoPowers {
    /** 模组自带的「无为转变改造肉块」。 */
    private static net.minecraft.world.level.block.Block fleshBlock() {
        return cn.blockforge.generated.nnnnr1201forge2.ProgressionContent.TRANSFIGURED_FLESH.get();
    }

    private record Wall(ServerLevel level, Vec3 center, Vec3 dir, long end) {}
    private record Bridge(ServerLevel level, ServerPlayer owner, long end) {}
    private static final List<Wall> WALLS = new ArrayList<>();
    private static final List<Bridge> BRIDGES = new ArrayList<>();

    /** 墙：身后 2 格生成 5×5 肉墙，墙上触手每 0.5 秒攻击前方 10 格内的敌人。 */
    public static void wall(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        Vec3 look = p.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0, -look.z).normalize();
        Vec3 side = new Vec3(-back.z, 0, back.x);
        Vec3 base = p.position().add(back.scale(2));
        for (int w = -2; w <= 2; w++) for (int h = 0; h <= 4; h++) {
            Vec3 at = base.add(side.scale(w)).add(0, h, 0);
            BlockPos pos = BlockPos.containing(at);
            if (level.isEmptyBlock(pos) || level.getBlockState(pos).canBeReplaced())
                level.setBlockAndUpdate(pos, fleshBlock().defaultBlockState());
        }
        WALLS.add(new Wall(level, p.position(), look, level.getGameTime() + 200));
        p.sendSystemMessage(Component.literal("无为转变·墙：触手将在 10 秒内攻击前方敌人").withStyle(ChatFormatting.AQUA));
    }

    /** 桥：脚下不断生成 3×3 肉块，持续 5 秒。 */
    public static void bridge(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        BRIDGES.add(new Bridge(level, p, level.getGameTime() + 100));
        p.sendSystemMessage(Component.literal("无为转变·桥：脚下持续生成肉块 5 秒").withStyle(ChatFormatting.AQUA));
    }

    /** 具象化：把贮存体变成生物列表中的第 index 个生物，保留其血量与防御。 */
    public static void materialize(ServerPlayer p, int index) {
        ItemStack held = p.getMainHandItem();
        CompoundTag base = held.getTagElement(MahitoTransfigure.BASE_TAG);
        if (base == null || !(p.level() instanceof ServerLevel level)) return;
        List<ResourceLocation> keys = entityKeys();
        if (index < 0 || index >= keys.size()) return;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(keys.get(index));
        Entity entity = type.create(level);
        if (entity == null) return;
        Vec3 at = p.position().add(p.getLookAngle().scale(2));
        entity.moveTo(at.x, p.getY(), at.z, p.getYRot(), 0f);
        level.addFreshEntity(entity);
        if (entity instanceof LivingEntity living) {
            float hp = Math.max(1f, base.getFloat("health"));
            living.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
            living.setHealth(hp);
            if (living.getAttribute(Attributes.ARMOR) != null) living.getAttribute(Attributes.ARMOR).setBaseValue(20);
            if (living instanceof Mob mob) mob.setPersistenceRequired();
            living.setCustomName(Component.literal(base.getString("name")));
        }
        held.shrink(1);
        p.sendSystemMessage(Component.literal("无为转变·具象化：" + base.getString("name") + " → " + keys.get(index))
            .withStyle(ChatFormatting.AQUA));
    }

    /** 潜能激发：让目标随机获得一种术式；对玩家解锁对应记忆旗标，对生物给予强化。 */
    public static void potential(ServerPlayer p) {
        LivingEntity target = p.level().getEntitiesOfClass(LivingEntity.class, new AABB(p.position(), p.position()).inflate(8),
                e -> e != p && e.isAlive()).stream().min(Comparator.comparingDouble(p::distanceToSqr)).orElse(p);
        String[] names = {"宿傩·解与捌", "赤血操术", "无下限", "冰凝咒法"};
        int pick = p.getRandom().nextInt(names.length);
        if (target instanceof ServerPlayer sp) {
            switch (pick) {
                case 0 -> CurseFlag.set(sp, "Memory");
                case 1 -> CurseFlag.set(sp, "Nine");
                case 2 -> CurseFlag.set(sp, "GojoMemory");
                default -> CurseFlag.set(sp, "UraumeMemory");
            }
            sp.sendSystemMessage(Component.literal("潜能激发：获得术式「" + names[pick] + "」").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (target instanceof Mob mob) {
            if (mob.getAttribute(Attributes.MAX_HEALTH) != null) {
                double hp = mob.getAttributeValue(Attributes.MAX_HEALTH) * 1.5;
                mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                mob.setHealth((float) hp);
            }
            mob.sendSystemMessage(Component.literal("潜能激发：「" + names[pick] + "」的力量涌入身体")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        p.sendSystemMessage(Component.literal("潜能激发：" + target.getName().getString() + " → " + names[pick])
            .withStyle(ChatFormatting.AQUA));
    }

    /** 具象化可选生物列表（服务端/客户端都用同一排序，保证下标一致）。 */
    public static List<ResourceLocation> entityKeys() {
        List<ResourceLocation> keys = new ArrayList<>();
        for (ResourceLocation rl : BuiltInRegistries.ENTITY_TYPE.keySet()) keys.add(rl);
        keys.sort(Comparator.comparing(ResourceLocation::toString));
        return keys;
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (var it = WALLS.iterator(); it.hasNext(); ) {
            Wall wall = it.next();
            if (wall.level().getGameTime() >= wall.end()) { it.remove(); continue; }
            if (wall.level().getGameTime() % 10 != 0) continue;
            for (LivingEntity enemy : wall.level().getEntitiesOfClass(LivingEntity.class, new AABB(wall.center(), wall.center()).inflate(10),
                    e -> e.isAlive() && !(e instanceof Player p && p.isCreative()))) {
                Vec3 to = enemy.position().subtract(wall.center());
                if (to.lengthSqr() < 0.01) continue;
                if (to.normalize().dot(wall.dir()) > 0.5) {
                    enemy.hurt(wall.level().damageSources().magic(), 6f);
                }
            }
        }
        for (var it = BRIDGES.iterator(); it.hasNext(); ) {
            Bridge bridge = it.next();
            ServerPlayer owner = bridge.owner();
            if (bridge.level().getGameTime() >= bridge.end() || owner == null || !owner.isAlive()
                || owner.level() != bridge.level()) { it.remove(); continue; }
            if (bridge.level().getGameTime() % 4 != 0) continue;
            BlockPos center = BlockPos.containing(owner.position()).below();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (bridge.level().isEmptyBlock(pos) || bridge.level().getBlockState(pos).canBeReplaced())
                    bridge.level().setBlockAndUpdate(pos, fleshBlock().defaultBlockState());
            }
        }
    }

    @SubscribeEvent public static void stop(ServerStoppedEvent event) {
        WALLS.clear();
        BRIDGES.clear();
    }

    private MahitoPowers() {}

    /** 设置记忆旗标并同步（潜能激发用）。 */
    private static final class CurseFlag {
        static void set(ServerPlayer p, String flag) {
            cn.blockforge.generated.nnnnr1201forge2.Curse.data(p).putBoolean(flag, true);
            cn.blockforge.generated.nnnnr1201forge2.Curse.sync(p);
        }
    }
}
