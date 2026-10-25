package character.domainking;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.*;

/**
 * 真人领域「自闭圆盾裹」：
 * · 地面 = 纯黑混凝土，上方再铺一层亮度 15 的光源方块；
 * · 球壳 = 半径 50 的黑色混凝土，每 5 格嵌 2 格宽肉色（粉红）混凝土网格；
 * · 玩家身后的球壳内侧 5 格处，放一排「不可移动的圆钝果实体」（暂用无 AI 僵尸替代）。
 * 方块按队列逐刻施工并记录旧状态，收起/关服时整批还原。
 */
public final class SelfEmbryoDomain {
    public static final double RADIUS = 30;          // 领域半径（用户指定）
    public static final int GRID_SPACING = 15;       // 网格间隔（用户指定）
    public static final int GRID_WIDTH = 2;          // 网格宽度
    public static final int LIFE_TICKS = 600;        // 30 秒
    private static final int PER_TICK = 8000;        // 每刻施工方块数上限
    private static final int BUILD_TICKS = 60;       // 3 秒铺完（20 刻/秒）
    private static final int MAX_BLOCKS = 260000;    // 上限保护

    private record Edit(BlockPos pos, BlockState old) {}

    private static final class Build {
        final ServerLevel level;
        final BlockPos center;
        final long end;
        final List<Edit> journal = new ArrayList<>();
        final List<BlockPos> blocks = new ArrayList<>();
        final List<UUID> fruits = new ArrayList<>();
        int cursor;
        int rate;
        Build(ServerLevel level, BlockPos center, long end) { this.level = level; this.center = center; this.end = end; }
    }

    private static final Map<UUID, Build> ACTIVE = new HashMap<>();

    public static boolean active(ServerPlayer p) { return ACTIVE.containsKey(p.getUUID()); }

    public static void open(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        if (active(p)) return;
        BlockPos center = p.blockPosition();
        Build build = new Build(level, center, level.getGameTime() + LIFE_TICKS);
        plan(level, center, build);
        ACTIVE.put(p.getUUID(), build);
        spawnFruits(level, p, build);
        level.playSound(null, center, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
            net.minecraft.sounds.SoundSource.HOSTILE, 1.8f, 0.4f);
    }

    /** 收集所有需要改动的方块：按“环”排序（球壳 → 地板 → 光源），逐环在 3 秒内铺开；最外圈地板不放光源。 */
    private static void plan(ServerLevel level, BlockPos c, Build build) {
        double r2 = RADIUS * RADIUS, in2 = (RADIUS - 1) * (RADIUS - 1);
        int ri = (int) Math.ceil(RADIUS);
        record Work(int ring, int type, BlockPos pos) {}
        List<Work> works = new ArrayList<>();
        for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) for (int dy = -1; dy <= ri; dy++) {
            double d2 = dx * dx + dy * dy + dz * dz;
            BlockPos pos = c.offset(dx, dy, dz);
            int ring = (c.getY() + ri) - pos.getY();   // 从头顶往下逐层
            if (dy == -1 && dx * dx + dz * dz <= r2) {
                works.add(new Work(ring, 1, pos));
                // 最外圈地板不放光源：只有水平距离在 (R-1) 以内的地板才叠光源
                if (dx * dx + dz * dz <= in2) works.add(new Work(ring, 2, pos.above()));
                continue;
            }
            if (dy >= 0 && d2 <= in2) {
                works.add(new Work(ring, 3, pos));   // 内部清理由上往下
                continue;
            }
            if (dy >= 0 && d2 > in2 && d2 <= r2) {
                works.add(new Work(ring, 0, pos));   // 球壳优先
            }
        }
        works.sort(Comparator.<Work>comparingInt(Work::ring).thenComparingInt(Work::type));
        for (Work w : works) if (build.blocks.size() < MAX_BLOCKS) build.blocks.add(w.pos());
        build.rate = Math.max(1, Math.min(PER_TICK, (build.blocks.size() + BUILD_TICKS - 1) / BUILD_TICKS));
    }

    private static BlockState shellState(BlockPos pos, BlockPos c) {
        int gx = Math.floorMod(pos.getX() - c.getX(), GRID_SPACING);
        int gy = Math.floorMod(pos.getY() - c.getY(), GRID_SPACING);
        int gz = Math.floorMod(pos.getZ() - c.getZ(), GRID_SPACING);
        boolean grid = gx < GRID_WIDTH || gy < GRID_WIDTH || gz < GRID_WIDTH;
        return grid ? Blocks.PINK_CONCRETE.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState();
    }

    private static void spawnFruits(ServerLevel level, ServerPlayer p, Build build) {
        var look = p.getLookAngle();
        var back = new net.minecraft.world.phys.Vec3(-look.x, 0, -look.z).normalize();
        for (int i = -1; i <= 1; i++) {
            var side = new net.minecraft.world.phys.Vec3(-back.z, 0, back.x).scale(i * 1.6);
            var at = p.position()
                .add(back.scale(RADIUS - 5))
                .add(side)
                .add(0, 1.0, 0);
            Zombie fruit = EntityType.ZOMBIE.create(level);
            if (fruit == null) continue;
            fruit.moveTo(at.x, at.y, at.z, p.getYRot() + 180f, 0f);
            fruit.setNoAi(true);
            fruit.setSilent(true);
            fruit.setPersistenceRequired();
            fruit.setCustomName(net.minecraft.network.chat.Component.literal("圆钝果实"));
            fruit.setCustomNameVisible(false);
            Objects.requireNonNull(fruit.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(1024);
            fruit.setHealth(1024);
            level.addFreshEntity(fruit);
            build.fruits.add(fruit.getUUID());
        }
    }

    public static void close(ServerPlayer p, String reason) {
        Build build = ACTIVE.remove(p.getUUID());
        if (build == null) return;
        restore(build);
    }

    private static void restore(Build build) {
        for (int i = build.journal.size() - 1; i >= 0; i--) {
            Edit e = build.journal.get(i);
            build.level.setBlock(e.pos(), e.old(), 2);
        }
        for (UUID id : build.fruits) {
            var entity = build.level.getEntity(id);
            if (entity != null) entity.discard();
        }
        build.journal.clear();
        build.blocks.clear();
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (UUID id : new ArrayList<>(ACTIVE.keySet())) {
            ServerPlayer p = event.getServer().getPlayerList().getPlayer(id);
            Build build = ACTIVE.get(id);
            if (p == null || !p.isAlive() || p.level() != build.level) { ACTIVE.remove(id); if (build != null) restore(build); continue; }
            if (build.level.getGameTime() >= build.end) { ACTIVE.remove(id); restore(build); p.sendSystemMessage(net.minecraft.network.chat.Component.literal("领域 · 自闭圆盾裹 已收起")); continue; }
            int done = 0;
            while (build.cursor < build.blocks.size() && done < build.rate) {
                BlockPos pos = build.blocks.get(build.cursor++);
                BlockState old = build.level.getBlockState(pos);
                if (build.level.getBlockEntity(pos) != null) continue;
                if (old.getDestroySpeed(build.level, pos) < 0) continue;   // 基岩等不可破坏方块
                BlockState target;
                if (pos.getY() == build.center.getY() - 1 && old != Blocks.BLACK_CONCRETE.defaultBlockState()) {
                    target = Blocks.BLACK_CONCRETE.defaultBlockState();
                } else if (pos.getY() == build.center.getY()) {
                    // 地板层与其上一层：只给非最外圈铺亮度 15 光源（最外圈保持纯黑混凝土）
                    double hd2 = (pos.getX() - build.center.getX()) * (double) (pos.getX() - build.center.getX())
                        + (pos.getZ() - build.center.getZ()) * (double) (pos.getZ() - build.center.getZ());
                    if (pos.getY() == build.center.getY() && hd2 > (RADIUS - 1) * (RADIUS - 1)) {
                        target = Blocks.BLACK_CONCRETE.defaultBlockState();
                    } else {
                        target = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
                    }
                } else {
                    double d2 = pos.distSqr(build.center);
                    double in2 = (RADIUS - 1) * (RADIUS - 1), r2 = RADIUS * RADIUS;
                    if (d2 > in2 && d2 <= r2) target = shellState(pos, build.center);
                    else target = Blocks.AIR.defaultBlockState();
                }
                if (old == target) continue;
                build.journal.add(new Edit(pos.immutable(), old));
                build.level.setBlock(pos, target, 2);
                done++;
            }
        }
    }

    /** 关服兜底：未收起的领域全部还原，避免方块永久留在存档。 */
    @SubscribeEvent public static void stop(ServerStoppedEvent event) {
        for (Build build : ACTIVE.values()) restore(build);
        ACTIVE.clear();
    }

    private SelfEmbryoDomain() {}
}
