package character.uraume;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import cn.blockforge.generated.nnnnr1201forge2.Network;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 里梅·领域展开「霜天宫殿」（r291，玩家版）。
 *
 * <p>蓝冰地板 + 浮冰穹顶，界内禁飞/禁跳、霜寒永久满层、冰系伤害翻倍（由 {@link UraumeCombat} 判定），
 * 天空贴图走 {@code Effect(650)}（客户端 {@code UraumePalaceClient}）。
 */
public final class UraumePalaceDomain {
    public static final int RADIUS = 26, TICKS = 600;   // 30 秒
    private record Job(ServerLevel level, BlockPos center, long end, Map<BlockPos, BlockState> old) { }
    private static final Map<UUID, Job> JOBS = new HashMap<>();

    public static boolean active(ServerPlayer p) { return JOBS.containsKey(p.getUUID()); }

    public static boolean inDomain(net.minecraft.world.entity.Entity e) {
        for (Job job : JOBS.values()) {
            if (e.level() != job.level) continue;
            if (e.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(job.center))
                <= (double) RADIUS * RADIUS) return true;
        }
        return false;
    }

    public static void open(ServerPlayer p) {
        if (active(p) || !(p.level() instanceof ServerLevel level)) return;
        BlockPos center = p.blockPosition();
        Map<BlockPos, BlockState> old = new HashMap<>();
        int r = RADIUS;
        for (int dx = -r; dx <= r; dx++) for (int dy = -r; dy <= r; dy++) for (int dz = -r; dz <= r; dz++) {
            double d2 = (double) dx * dx + dy * dy + dz * dz;
            boolean shell = d2 <= (double) r * r && d2 > (double) (r - 1) * (r - 1);
            boolean floor = dy == -1 && d2 <= (double) r * r;
            if (!shell && !floor && dy > 0) continue;
            BlockPos pos = center.offset(dx, dy, dz);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() == Blocks.BEDROCK) continue;
            BlockState put = floor ? Blocks.BLUE_ICE.defaultBlockState()
                : shell ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.AIR.defaultBlockState();
            if (!shell && !floor && !state.isAir()) continue;
            old.put(pos.immutable(), state);
            level.setBlock(pos, put, 2);
        }
        JOBS.put(p.getUUID(), new Job(level, center, level.getGameTime() + TICKS, old));
        Network.send(level, new Network.Effect(650, p.getId(),
            net.minecraft.world.phys.Vec3.atCenterOf(center), net.minecraft.world.phys.Vec3.ZERO, r, TICKS, 0));
        level.playSound(null, center, net.minecraft.sounds.SoundEvents.GLASS_PLACE,
            net.minecraft.sounds.SoundSource.PLAYERS, 1.6f, .5f);
    }

    public static void close(ServerPlayer p, String why) {
        Job job = JOBS.remove(p.getUUID());
        if (job == null) return;
        job.old.forEach((pos, state) -> job.level.setBlock(pos, state, 2));
        Network.send(job.level, new Network.Effect(650, p.getId(),
            net.minecraft.world.phys.Vec3.atCenterOf(job.center), net.minecraft.world.phys.Vec3.ZERO, 0f, 0, 0));
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("霜天宫殿 · 解除（" + why + "）"), true);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || JOBS.isEmpty()) return;
        for (var it = JOBS.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            Job job = entry.getValue();
            ServerPlayer owner = job.level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (owner == null || !owner.isAlive() || owner.level() != job.level
                || job.level.getGameTime() >= job.end) {
                if (owner != null) close(owner, job.level.getGameTime() >= job.end ? "时间到" : "施术者离开");
                else it.remove();
                continue;
            }
            for (ServerPlayer p : job.level.players()) {
                if (p.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(job.center))
                    > (double) RADIUS * RADIUS) continue;
                if (p != owner) {
                    if (p.getAbilities().flying) { p.getAbilities().flying = false; p.onUpdateAbilities(); }
                    if (p.getDeltaMovement().y > 0.42) {
                        var v = p.getDeltaMovement();
                        p.setDeltaMovement(v.x, 0.42, v.z);
                        p.hurtMarked = true;
                    }
                    if (job.level.getGameTime() % 20 == 0) {
                        p.setTicksFrozen(Math.max(p.getTicksFrozen(), 100));
                        p.getPersistentData().putInt("FrostStacks", cn.blockforge.generated.nnnnr1201forge2.UraumeIce.FROST_MAX);
                    }
                }
            }
        }
    }

    private UraumePalaceDomain() { }
}
