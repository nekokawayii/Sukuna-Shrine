package character.naoya;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 禅院直哉 · 领域展开「时胞月宫殿」。
 *
 * <p>**正圆**（不是半球）：半径 30 的完整球体，外壁骨白、内壁肉粉，内部清空并填 15 级光源；
 * 圆心是一座「子宫」——中间一只巨大的眼睛、眼睛前面一条通向球壁的路（参考用户给的图）。
 *
 * <p>领域效果：①领域内直哉的**每一套动作（三次命中）会被记成一组**，然后由**半透明影子复制这套动作**
 * 再对目标结算一次（必中，玩家本人不动）；②「千手乱舞」：领域内按住左键即以 **16 次/秒**对所有目标连打，
 * 每次命中 **95%** 概率把目标压成二次元平面。
 */
public final class NaoyaDomain {
    public static final double RADIUS = 30;
    public static final int EXPAND_TICKS = 80;        // 4 秒展开
    public static final int CLOSE_TICKS = 100;        // 5 秒收起
    public static final int LIFE_TICKS = 600;         // 30 秒
    public static final int GROUP_SIZE = 3;           // 三次命中 = 一组动作
    public static final double SHADOW_RADIUS = 6;
    public static final int AUTO_INTERVAL = 1;        // 每刻 1 次 ≈ 16~20 次/秒
    public static final double AUTO_DAMAGE = 6;
    public static final double AUTO_PLANE_CHANCE = 0.95;
    public static final double AUTO_RANGE = 5;

    private static final class Realm {
        final ServerLevel level;
        final UUID owner;
        final BlockPos center;
        final List<BlockPos> outer = new ArrayList<>(), inner = new ArrayList<>(),
            light = new ArrayList<>(), decor = new ArrayList<>();
        /** 与 decor 一一对应的方块状态（来自用户改好的结构）。 */
        final List<BlockState> decorStates = new ArrayList<>();
        final Map<BlockPos, BlockState> before = new LinkedHashMap<>();
        int cursor, total;
        boolean closing;
        long openedAt, closeAt, nextAuto;
        int group;
        Realm(ServerLevel level, UUID owner, BlockPos center) {
            this.level = level; this.owner = owner; this.center = center;
        }
    }

    private static final Map<UUID, Realm> ACTIVE = new java.util.HashMap<>();

    public static boolean active(ServerPlayer p) {
        Realm realm = ACTIVE.get(p.getUUID());
        return realm != null && !realm.closing;
    }

    public static boolean isOwner(ServerPlayer p) { return ACTIVE.containsKey(p.getUUID()); }

    // ── 开关 ─────────────────────────────────────────────
    public static void toggle(ServerPlayer p) {
        Realm realm = ACTIVE.get(p.getUUID());
        if (realm != null && !realm.closing) { close(p, "主动收起"); return; }
        open(p);
    }

    public static void open(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level) || ACTIVE.containsKey(p.getUUID())) return;
        Realm realm = new Realm(level, p.getUUID(), p.blockPosition());
        build(realm);
        realm.total = realm.outer.size() + realm.inner.size() + realm.decor.size() + realm.light.size();
        realm.openedAt = level.getGameTime();
        realm.closeAt = realm.openedAt + LIFE_TICKS;
        ACTIVE.put(p.getUUID(), realm);
        Network.send(level, new Network.Effect(NaoyaFx.DOMAIN, p.getId(), p.position(), Vec3.ZERO,
            (float) RADIUS, LIFE_TICKS, 0));
        level.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.4f);
        p.sendSystemMessage(Component.literal("领域展开·时胞月宫殿：半径 30 的正圆 · 影子复制你的动作 · 按住左键千手乱舞")
            .withStyle(net.minecraft.ChatFormatting.AQUA));
    }

    public static void close(ServerPlayer p, String reason) {
        Realm realm = ACTIVE.get(p.getUUID());
        if (realm == null || realm.closing) return;
        realm.closing = true;
        realm.closeAt = realm.level.getGameTime();
        Network.send(realm.level, new Network.Effect(NaoyaFx.DOMAIN, p.getId(), p.position(), Vec3.ZERO, 0f, 0, 0));
        com.mojang.logging.LogUtils.getLogger().info("[直哉领域] 收起（{}）", reason);
    }

    /** 球壳 + 内部 + 子宫/眼睛/路。 */
    private static void build(Realm realm) {
        int r = (int) Math.ceil(RADIUS);
        int outerSq = r * r, innerSq = (r - 1) * (r - 1);
        int floorY = realm.center.getY() - 1;
        Vec3 center = Vec3.atCenterOf(realm.center);
        // 眼睛朝向：以圆心为原点、沿施术者朝向（用 +Z 作为路的方向，客户端按需要旋转即可）
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int d = dx * dx + dy * dy + dz * dz;
                    BlockPos pos = realm.center.offset(dx, dy, dz);
                    if (d > outerSq) continue;
                    if (d >= innerSq) { realm.outer.add(pos); continue; }          // 外壁（黑色混凝土）
                    // 半径 (r-2) 以内**整颗球都清空**（含地板以下），否则下方还是原地形
                    if (d <= (r - 2) * (r - 2)) realm.light.add(pos);
                    if (d >= (r - 2) * (r - 2)) realm.inner.add(pos);              // 内壁（黑色混凝土）
                }
            }
        }
        // 地板：黑混凝土圆盘（清空后没有地形可站，必须补一层）
        int floorSq = (r - 2) * (r - 2);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > floorSq) continue;
                BlockPos pos = new BlockPos(realm.center.getX() + dx, floorY, realm.center.getZ() + dz);
                realm.inner.add(pos);
            }
        }
        // 内部装饰：用户改好的结构（黑/白混凝土 + 紫水晶），施工时最后铺
        byte[] layout = NaoyaDomainLayout.bytes();
        for (int i = 0; i + 3 < layout.length; i += 4) {
            int x = layout[i] & 0xFF, y = layout[i + 1] & 0xFF, z = layout[i + 2] & 0xFF, state = layout[i + 3] & 0xFF;
            BlockPos pos = new BlockPos(realm.center.getX() + x - NaoyaDomainLayout.OFFSET_X,
                realm.center.getY() + y + NaoyaDomainLayout.OFFSET_Y,
                realm.center.getZ() + z - NaoyaDomainLayout.OFFSET_Z);
            realm.decor.add(pos);
            realm.decorStates.add(switch (state) {
                case 1 -> Blocks.WHITE_CONCRETE.defaultBlockState();
                case 2 -> Blocks.AMETHYST_BLOCK.defaultBlockState();
                default -> Blocks.BLACK_CONCRETE.defaultBlockState();
            });
        }
    }

    // ── 每刻 ─────────────────────────────────────────────
    public static void tick(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;
        long now = server.overworld().getGameTime();
        for (var iterator = ACTIVE.entrySet().iterator(); iterator.hasNext(); ) {
            Realm realm = iterator.next().getValue();
            ServerPlayer owner = server.getPlayerList().getPlayer(realm.owner);
            if (owner == null || !owner.isAlive()) { forceRestore(realm); iterator.remove(); continue; }
            if (!realm.closing && now >= realm.closeAt) { realm.closing = true; realm.closeAt = now; }
            if (realm.closing) {
                if (!restoreStep(realm)) iterator.remove();
                continue;
            }
            placeStep(realm);
            tickAuto(owner, realm, now);
        }
    }

    private static void placeStep(Realm realm) {
        int budget = Math.max(600, realm.total / EXPAND_TICKS);
        for (int i = 0; i < budget; i++) {
            int index = realm.cursor++;
            if (index >= realm.total) return;
            int outerEnd = realm.outer.size(), innerEnd = outerEnd + realm.inner.size(),
                lightEnd = innerEnd + realm.light.size();
            BlockPos pos;
            BlockState target;
            if (index < outerEnd) {
                pos = realm.outer.get(index);
                target = Blocks.BLACK_CONCRETE.defaultBlockState();   // 外壁：黑色混凝土
            } else if (index < innerEnd) {
                pos = realm.inner.get(index - outerEnd);
                target = Blocks.BLACK_CONCRETE.defaultBlockState();   // 内壁：黑色混凝土
            } else if (index < lightEnd) {
                pos = realm.light.get(index - innerEnd);
                target = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
            } else {
                // 用户结构最后铺：否则会被内部光源方块整个盖掉（r360 内部全空的根因）
                int decorIndex = index - lightEnd;
                pos = realm.decor.get(decorIndex);
                target = realm.decorStates.get(decorIndex);
            }
            if (!realm.level.hasChunkAt(pos)) continue;
            realm.before.putIfAbsent(pos, realm.level.getBlockState(pos));
            if (!realm.level.getBlockState(pos).equals(target)) realm.level.setBlock(pos, target, 2);
        }
    }

    private static boolean restoreStep(Realm realm) {
        int budget = Math.max(600, realm.total / CLOSE_TICKS);
        int done = 0;
        while (done < budget && !realm.before.isEmpty()) {
            var iterator = realm.before.entrySet().iterator();
            var entry = iterator.next();
            if (realm.level.hasChunkAt(entry.getKey())) realm.level.setBlock(entry.getKey(), entry.getValue(), 2);
            iterator.remove();
            done++;
        }
        return !realm.before.isEmpty();
    }

    private static void forceRestore(Realm realm) {
        for (var entry : realm.before.entrySet()) {
            if (realm.level.hasChunkAt(entry.getKey())) realm.level.setBlock(entry.getKey(), entry.getValue(), 2);
        }
        realm.before.clear();
    }

    public static void forceClose(ServerPlayer p) {
        Realm realm = ACTIVE.remove(p.getUUID());
        if (realm != null) forceRestore(realm);
    }

    public static void reset() {
        for (Realm realm : ACTIVE.values()) forceRestore(realm);
        ACTIVE.clear();
    }

    // ── 领域效果①：影子复制动作（每 3 次命中一组，必中结算） ──
    @SubscribeEvent public static void hurt(LivingHurtEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || !NaoyaDomain.active(p)) return;
        Realm realm = ACTIVE.get(p.getUUID());
        if (realm == null) return;
        if (++realm.group < GROUP_SIZE) return;
        realm.group = 0;
        if (!(e.getEntity() instanceof net.minecraft.world.entity.LivingEntity)) return;
        net.minecraft.world.entity.LivingEntity target = (net.minecraft.world.entity.LivingEntity) e.getEntity();
        Vec3 at = target.position();
        double damage = Math.max(4, e.getAmount() * 0.8);
        // 半透明影子：从施术者身边出发、复制这套动作砸在目标身上（玩家本人不动）
        Network.send(realm.level, new Network.Effect(NaoyaFx.SHADOW, p.getId(), p.position(),
            at.subtract(p.position()), 3f, 20, 0));
        for (var other : realm.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                new AABB(at, at).inflate(SHADOW_RADIUS),
                t -> t != p && t.isAlive() && CombatSettings.canHit(p, t))) {
            other.invulnerableTime = 0;
            CombatSettings.hurt(p, other, p.damageSources().playerAttack(p), (float) damage);
            NaoyaCombat.freeze(p, other);
            Network.send(realm.level, new Network.Effect(NaoyaFx.HIT, other.getId(), other.position(),
                Vec3.ZERO, 1f, 6, 0));
        }
    }

    // ── 领域效果②：千手乱舞（按住左键 16 次/秒 + 95% 变平面） ──
    public static void autoAttack(ServerPlayer p, boolean on) {
        Curse.data(p).putBoolean("NaoyaAutoAttack", on);
    }

    private static void tickAuto(ServerPlayer p, Realm realm, long now) {
        if (!Curse.data(p).getBoolean("NaoyaAutoAttack")) return;
        if (now < realm.nextAuto) return;
        realm.nextAuto = now + AUTO_INTERVAL;
        ServerLevel level = realm.level;
        var targets = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
            p.getBoundingBox().inflate(AUTO_RANGE),
            t -> t != p && t.isAlive() && !t.isSpectator() && CombatSettings.canHit(p, t));
        if (targets.isEmpty()) return;
        var random = level.random;
        for (var target : targets) {
            target.invulnerableTime = 0;                 // 无视无敌帧：每一刻都真正结算
            CombatSettings.hurt(p, target, p.damageSources().playerAttack(p), (float) AUTO_DAMAGE);
            target.invulnerableTime = 0;
            if (random.nextDouble() < AUTO_PLANE_CHANCE) NaoyaCombat.freeze(p, target);
            Network.send(level, new Network.Effect(NaoyaFx.HIT, target.getId(), target.position(),
                Vec3.ZERO, 1f, 4, 0));
        }
    }

    private NaoyaDomain() {}
}
