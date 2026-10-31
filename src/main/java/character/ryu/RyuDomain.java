package character.ryu;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 石流龙 · 领域展开「打上花火」。
 *
 * <p>结构：外侧一层**基岩**球壳（半径 65）、里面一层**纯黑混凝土**（半径 64）；
 * 4 秒展开完（80 刻分批施工），5 秒收起（100 刻分批还原），打开 30 秒后自动收；
 * 内部天空换成 {@code textures/environment/ryu_domain_sky.png}（客户端画）。
 *
 * <p>领域效果：①自己所有招式**必中**（冲击波不再被方块截断、追踪弹穿墙）；
 * ②随机放烟花：小特效上升 5~15 格后炸成 15~45 发各色追踪弹幕。
 *
 * <p>地形借用遵循项目惯例：**每一格都记第一次的原始状态**，收起时逐格还原；关服/死亡/切角色都会兜底还原。
 */
public final class RyuDomain {
    public static final double RADIUS = 35;
    public static final int EXPAND_TICKS = 80;      // 4 秒展开完
    public static final int CLOSE_TICKS = 100;      // 5 秒收起
    public static final int LIFE_TICKS = 600;       // 30 秒后自动收
    public static final int FIREWORK_INTERVAL = 20; // 每 1 秒一波烟花（一波两枚）
    public static final int SHELL_MIN = 15, SHELL_MAX = 45;
    public static final double SHELL_SPEED = 0.65, SHELL_DAMAGE = 6;
    public static final int SHELL_LIFE = 60, SHELL_CAP = 320;
    public static final int COLOR_COUNT = 8;

    private static final class Fract {
        final ServerLevel level;
        final UUID owner;
        final BlockPos center;
        final List<BlockPos> outer = new ArrayList<>(), inner = new ArrayList<>(), floor = new ArrayList<>(),
            fill = new ArrayList<>();
        final Map<BlockPos, BlockState> before = new LinkedHashMap<>();
        int image;
        int cursor;            // 已施工/已还原的块数
        int total;
        boolean closing;
        boolean insideFlag;
        long openedAt, closeAt;
        long nextFirework;
        Fract(ServerLevel level, UUID owner, BlockPos center) {
            this.level = level; this.owner = owner; this.center = center;
        }
    }

    private record Rocket(ServerLevel level, UUID owner, Vec3 pos, double top, long born) {}
    private record Shell(ServerLevel level, UUID owner, Vec3 pos, Vec3 dir, int color, long born) {}

    private static final Map<UUID, Fract> ACTIVE = new HashMap<>();
    private static final List<Rocket> ROCKETS = new ArrayList<>();
    private static final List<Shell> SHELLS = new ArrayList<>();

    public static boolean isOwner(ServerPlayer p) {
        Fract f = ACTIVE.get(p.getUUID());
        return f != null && f.level == p.level();
    }

    /** 这次施法者自己开的领域是否正开着（用于"招式必中"）。 */
    public static boolean active(ServerPlayer p) {
        Fract f = ACTIVE.get(p.getUUID());
        return f != null && !f.closing;
    }

    /** 某个位置是否在（这名玩家开的）领域内。 */
    public static boolean inside(ServerPlayer p, Vec3 pos) {
        Fract f = ACTIVE.get(p.getUUID());
        return f != null && pos.distanceToSqr(Vec3.atCenterOf(f.center)) <= RADIUS * RADIUS;
    }

    public static Vec3 center(ServerPlayer p) {
        Fract f = ACTIVE.get(p.getUUID());
        return f == null ? null : Vec3.atCenterOf(f.center);
    }

    // ------------------------------------------------------------ 开关
    public static void toggle(ServerPlayer p) {
        if (isOwner(p) && !ACTIVE.get(p.getUUID()).closing) close(p, "主动收起");
        else if (!ACTIVE.containsKey(p.getUUID())) open(p);
    }

    public static void open(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level) || ACTIVE.containsKey(p.getUUID())) return;
        Fract f = new Fract(level, p.getUUID(), p.blockPosition());
        f.image = cn.blockforge.generated.nnnnr1201forge2.DomainScreenItem.imageOf(p);
        buildShell(f);
        f.total = f.outer.size() + f.inner.size() + f.fill.size() + f.floor.size();
        f.openedAt = level.getGameTime();
        f.closeAt = f.openedAt + LIFE_TICKS;
        f.nextFirework = f.openedAt + 20;
        ACTIVE.put(p.getUUID(), f);
        Network.send(level, new Network.Effect(RyuFx.DOMAIN, p.getId(), p.position(), Vec3.ZERO,
            (float) RADIUS, LIFE_TICKS, 0));
        level.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
            net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.6f);
        level.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_LAUNCH,
            net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.7f);
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "领域展开·打上花火：半径 65 格 · 4 秒展开 · 所有招式必中").withStyle(net.minecraft.ChatFormatting.AQUA));
        com.mojang.logging.LogUtils.getLogger().info("[石流龙] 领域展开 {} 中心 {} 总方块 {}", p.getScoreboardName(), f.center, f.total);
    }

    public static void close(ServerPlayer p, String reason) {
        Fract f = ACTIVE.get(p.getUUID());
        if (f == null || f.closing) return;
        f.closing = true;
        f.closeAt = f.level.getGameTime();
        Network.send(f.level, new Network.Effect(RyuFx.DOMAIN, p.getId(), p.position(), Vec3.ZERO, 0f, 0, 0));
        com.mojang.logging.LogUtils.getLogger().info("[石流龙] 领域收起 {}（{}）", p.getScoreboardName(), reason);
    }

    /** 球壳位置表：外层半径 65 基岩、内层半径 64 黑混凝土；按"绕 Y 扫一圈、由上往下"的顺序施工。 */
    private static void buildShell(Fract f) {
        int outerSq = (int) (RADIUS * RADIUS), innerSq = (int) ((RADIUS - 1) * (RADIUS - 1));
        List<BlockPos> outer = new ArrayList<>(), inner = new ArrayList<>();
        int r = (int) Math.ceil(RADIUS);
        int floorY = f.center.getY() - 1;
        for (int dy = -r; dy <= r; dy++) {
            if (f.center.getY() + dy < floorY) continue;   // 地板以下不动（否则会把地层一起挖空）
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int d = dx * dx + dy * dy + dz * dz;
                    if (d > outerSq || d < innerSq) continue;
                    BlockPos pos = f.center.offset(dx, dy, dz);
                    if (pos.getY() < floorY) continue;
                    if (d > innerSq) outer.add(pos);
                    else inner.add(pos);
                }
            }
        }
        // 由上往下、绕着中心扫一圈：看起来像一层壳在旋转着合拢
        Comparator<BlockPos> order = Comparator
            .comparingDouble((BlockPos p) -> -(p.getY() - f.center.getY()))
            .thenComparingDouble(p -> Math.atan2(p.getZ() - f.center.getZ(), p.getX() - f.center.getX()));
        outer.sort(order);
        inner.sort(order);
        // 地板：站在施术者脚下那一层，铺满半径内的圆盘（光源方块没有碰撞，必须有地板站人）
        List<BlockPos> floor = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > outerSq) continue;
                floor.add(new BlockPos(f.center.getX() + dx, floorY, f.center.getZ() + dz));
            }
        }
        // 内部：球壳以内**全部填 13 级光源方块**（而不是空气），地板最后铺
        int fillSq = (int) ((RADIUS - 2) * (RADIUS - 2));
        List<BlockPos> fill = new ArrayList<>();
        for (int dy = -r; dy <= r; dy++) {
            if (f.center.getY() + dy <= floorY) continue;   // 地板以下（含地板那一层）都不填
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int d = dx * dx + dy * dy + dz * dz;
                    if (d > fillSq) continue;
                    BlockPos pos = f.center.offset(dx, dy, dz);
                    if (pos.getY() <= floorY) continue;   // 地板本身要保持黑混凝土
                    fill.add(pos);
                }
            }
        }
        f.outer.addAll(outer);
        f.inner.addAll(inner);
        f.fill.addAll(fill);
        f.floor.addAll(floor);
    }

    // ------------------------------------------------------------ 每刻
    public static void tick(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;
        long now = server.overworld().getGameTime();
        for (Iterator<Map.Entry<UUID, Fract>> it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
            Fract f = it.next().getValue();
            ServerPlayer owner = server.getPlayerList().getPlayer(f.owner);
            if (owner == null || !owner.isAlive()) { forceRestore(f); it.remove(); continue; }
            if (!f.closing && now >= f.closeAt) { f.closing = true; f.closeAt = now; }
            if (f.closing) {
                if (!restoreStep(f)) it.remove();
            } else {
                placeStep(f);
                if (now >= f.nextFirework) {
                    f.nextFirework = now + FIREWORK_INTERVAL;
                    firework(f, now);
                    firework(f, now);   // 一波两枚：会同时/错开炸开，观感更密
                }
            }
        }
        tickFireworks(server, now);
    }

    /** 展开：每刻铺一批（总量/80 刻，最少 600）。 */
    private static void placeStep(Fract f) {
        int budget = Math.max(600, f.total / EXPAND_TICKS);
        // 施工顺序：**地板优先**（先让脚下有地）→ 外基岩壳 → 内黑混凝土壳 → 内部 13 级光方块
        int outerStart = f.floor.size(), innerStart = outerStart + f.outer.size(),
            fillStart = innerStart + f.inner.size();
        for (int i = 0; i < budget; i++) {
            int index = f.cursor++;
            if (index >= f.total) return;
            BlockPos pos;
            BlockState target;
            if (index < outerStart) {
                pos = f.floor.get(index);
                target = Blocks.BLACK_CONCRETE.defaultBlockState();
            } else if (index < innerStart) {
                pos = f.outer.get(index - outerStart);
                target = Blocks.BEDROCK.defaultBlockState();
            } else if (index < fillStart) {
                pos = f.inner.get(index - innerStart);
                target = Blocks.BLACK_CONCRETE.defaultBlockState();
            } else {
                pos = f.fill.get(index - fillStart);
                target = Blocks.LIGHT.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 13);
            }
            if (!f.level.hasChunkAt(pos)) continue;
            f.before.putIfAbsent(pos, f.level.getBlockState(pos));
            if (!f.level.getBlockState(pos).equals(target)) f.level.setBlock(pos, target, 2);
        }
    }

    /** 收起：每刻还原一批（总量/100 刻，最少 600）。返回 false 表示已全部还原。 */
    private static boolean restoreStep(Fract f) {
        int budget = Math.max(600, f.total / CLOSE_TICKS);
        int done = 0;
        while (done < budget && !f.before.isEmpty()) {
            var iterator = f.before.entrySet().iterator();
            var entry = iterator.next();
            if (f.level.hasChunkAt(entry.getKey())) f.level.setBlock(entry.getKey(), entry.getValue(), 2);
            iterator.remove();
            done++;
        }
        return !f.before.isEmpty();
    }

    /** 立刻整批还原（关服/死亡/切角色/恢复符兜底）。 */
    private static void forceRestore(Fract f) {
        for (var entry : f.before.entrySet()) {
            if (f.level.hasChunkAt(entry.getKey())) f.level.setBlock(entry.getKey(), entry.getValue(), 2);
        }
        f.before.clear();
    }

    public static void forceClose(ServerPlayer p) {
        Fract f = ACTIVE.remove(p.getUUID());
        if (f != null) forceRestore(f);
    }

    public static void reset() {
        for (Fract f : ACTIVE.values()) forceRestore(f);
        ACTIVE.clear();
        ROCKETS.clear();
        SHELLS.clear();
    }

    // ------------------------------------------------------------ 烟花
    /** 一枚小火箭：位置随机落在领域内，上升 5~15 格后炸开。 */
    private static void firework(Fract f, long now) {
        ServerPlayer owner = f.level.getServer() == null ? null
            : f.level.getServer().getPlayerList().getPlayer(f.owner);
        if (owner == null) return;
        double angle = f.level.random.nextDouble() * Math.PI * 2;
        double distance = 8 + f.level.random.nextDouble() * (RADIUS - 18);
        Vec3 pos = Vec3.atCenterOf(f.center)
            .add(Math.cos(angle) * distance, -10 + f.level.random.nextDouble() * 16, Math.sin(angle) * distance);
        double top = pos.y + 5 + f.level.random.nextDouble() * 10;   // 上升 5~15 格
        ROCKETS.add(new Rocket(f.level, f.owner, pos, top, now));
        Network.send(f.level, new Network.Effect(RyuFx.FIREWORK_RISE, owner.getId(), pos,
            new Vec3(0, top - pos.y, 0), 1f, 40, 0));
    }

    private static void tickFireworks(net.minecraft.server.MinecraftServer server, long now) {
        if (!ROCKETS.isEmpty()) {
            List<Rocket> keep = new ArrayList<>();
            for (Rocket rocket : ROCKETS) {
                Vec3 pos = rocket.pos().add(0, 0.35, 0);
                if (pos.y >= rocket.top() || now - rocket.born() > 60) {
                    burst(server, rocket, now);
                    continue;
                }
                keep.add(new Rocket(rocket.level(), rocket.owner(), pos, rocket.top(), rocket.born()));
            }
            ROCKETS.clear();
            ROCKETS.addAll(keep);
        }
        if (SHELLS.isEmpty()) return;
        List<Shell> keep = new ArrayList<>();
        for (Shell shell : SHELLS) {
            if (now - shell.born() > SHELL_LIFE) continue;
            ServerPlayer owner = server.getPlayerList().getPlayer(shell.owner());
            if (owner == null) continue;
            LivingEntity target = nearestTarget(shell.level(), owner, shell.pos());
            Vec3 dir = shell.dir();
            if (target != null) {
                Vec3 want = target.getBoundingBox().getCenter().subtract(shell.pos()).normalize();
                dir = dir.scale(0.75).add(want.scale(0.25)).normalize();
            }
            Vec3 pos = shell.pos().add(dir.scale(SHELL_SPEED));
            if ((now & 1) == 0) {
                Network.send(shell.level(), new Network.Effect(RyuFx.FIREWORK_SHELL, owner.getId(), pos, dir,
                    shell.color(), 5, 0));
            }
            LivingEntity hit = null;
            for (var candidate : shell.level().getEntitiesOfClass(LivingEntity.class,
                    new AABB(pos, pos).inflate(1.1),
                    t -> t != owner && t.isAlive() && !t.isSpectator()
                        && t.getBoundingBox().inflate(0.5).contains(pos))) {
                hit = candidate;
                break;
            }
            if (hit != null) {
                RyuCombat.hurtPublic(shell.level(), owner, hit, SHELL_DAMAGE);
                continue;
            }
            keep.add(new Shell(shell.level(), shell.owner(), pos, dir, shell.color(), shell.born()));
        }
        SHELLS.clear();
        SHELLS.addAll(keep);
    }

    private static void burst(net.minecraft.server.MinecraftServer server, Rocket rocket, long now) {
        ServerPlayer owner = server.getPlayerList().getPlayer(rocket.owner());
        if (owner == null) return;
        int count = SHELL_MIN + rocket.level().random.nextInt(SHELL_MAX - SHELL_MIN + 1);
        int color = rocket.level().random.nextInt(COLOR_COUNT);
        Network.send(rocket.level(), new Network.Effect(RyuFx.FIREWORK_BURST, owner.getId(), rocket.pos(),
            Vec3.ZERO, color, 18, 0));
        rocket.level().playSound(null, BlockPos.containing(rocket.pos()),
            net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST, net.minecraft.sounds.SoundSource.PLAYERS, 1f,
            (float) (0.9 + rocket.level().random.nextDouble() * 0.3));
        if (SHELLS.size() + count > SHELL_CAP) return;
        for (int i = 0; i < count; i++) {
            double a = rocket.level().random.nextDouble() * Math.PI * 2;
            double b = (rocket.level().random.nextDouble() - 0.5) * Math.PI;
            Vec3 dir = new Vec3(Math.cos(a) * Math.cos(b), Math.sin(b), Math.sin(a) * Math.cos(b)).normalize();
            SHELLS.add(new Shell(rocket.level(), rocket.owner(), rocket.pos(), dir,
                rocket.level().random.nextInt(COLOR_COUNT), now));
        }
    }

    private static LivingEntity nearestTarget(ServerLevel level, ServerPlayer owner, Vec3 around) {
        LivingEntity best = null;
        double bestDistance = RADIUS * RADIUS;
        for (var candidate : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(around, around).inflate(RADIUS),
                t -> t != owner && t.isAlive() && !t.isSpectator() && CombatSettings.canHit(owner, t))) {
            if (candidate.position().distanceToSqr(Vec3.atCenterOf(owner.blockPosition())) > RADIUS * RADIUS) continue;
            double d = candidate.position().distanceToSqr(around);
            if (d < bestDistance) { bestDistance = d; best = candidate; }
        }
        return best;
    }

    /** 领域里不掉血：球心在施术者身上、球底在 65 格外，掉下去不该摔死人。 */
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void fall(net.minecraftforge.event.entity.living.LivingFallEvent e) {
        if (e.getEntity().level().isClientSide()) return;
        for (Fract f : ACTIVE.values()) {
            if (f.level != e.getEntity().level()) continue;
            if (e.getEntity().position().distanceToSqr(Vec3.atCenterOf(f.center)) <= RADIUS * RADIUS) {
                e.setCanceled(true);
                return;
            }
        }
    }

    private RyuDomain() {}
}
