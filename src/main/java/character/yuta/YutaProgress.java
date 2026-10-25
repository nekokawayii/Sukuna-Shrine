package character.yuta;

import cn.blockforge.generated.nnnnr1201forge2.CombatSettings;
import cn.blockforge.generated.nnnnr1201forge2.Content;
import cn.blockforge.generated.nnnnr1201forge2.Curse;
import cn.blockforge.generated.nnnnr1201forge2.GetoEntity;
import cn.blockforge.generated.nnnnr1201forge2.LowCurseEntity;
import character.CharacterService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 乙骨忧太三条个人线的真实判定（框架只提供 {@link CharacterService#markChapter}，由这里触发）。
 * <ol>
 *   <li><b>目睹里香</b>：100 格内有村民死亡 → 开 120 秒窗口 → 窗口内（自身 32 格内）击败 3 只敌对生物。</li>
 *   <li><b>决战夏油杰</b>：击败夏油杰 → 完成并自动传送到百鬼夜行。</li>
 *   <li><b>百鬼夜行</b>：新维度内击败 100 只低级咒灵（维度内每 5 秒补怪，上限 15）。</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = "sukuna")
public final class YutaProgress {
    public static final ResourceKey<Level> HYAKKI =
        ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sukuna", "hyakki"));

    private static final int VILLAGER_RANGE = 100;
    private static final int WINDOW_TICKS = 2400;      // 120 秒
    private static final int NEEDED_KILLS = 3;
    private static final int HYAKKI_NEEDED = 100;
    private static final int SPAWN_INTERVAL = 100;     // 5 秒
    private static final int CURSE_CAP = 15;

    private static final String WIN_START = "YutaVillagerWindow";
    private static final String WIN_KILLS = "YutaWindowKills";
    private static final String HYAKKI_KILLS = "YutaHyakkiKills";

    @SubscribeEvent
    public static void death(LivingDeathEvent e) {
        if (e.isCanceled()) return;
        Entity dead = e.getEntity();

        // ① 村民死亡：给附近所有乙骨开窗
        if (dead instanceof Villager && dead.level() instanceof ServerLevel sl) {
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(dead.blockPosition()).inflate(VILLAGER_RANGE))) {
                if (!isYuta(p)) continue;
                Curse.data(p).putLong(WIN_START, p.level().getGameTime());
                Curse.data(p).putInt(WIN_KILLS, 0);
                p.displayClientMessage(Component.literal("目睹里香：120 秒内击败 3 只敌人"), false);
            }
        }

        ServerPlayer killer = CombatSettings.caster(e.getSource());
        if (killer == null || !isYuta(killer)) return;

        // ① 窗口内击杀计数
        if (dead instanceof Enemy) {
            var data = Curse.data(killer);
            long start = data.getLong(WIN_START);
            if (start > 0 && killer.level().getGameTime() - start <= WINDOW_TICKS
                && killer.distanceToSqr(dead) <= 32 * 32) {
                int n = data.getInt(WIN_KILLS) + 1;
                data.putInt(WIN_KILLS, n);
                if (n >= NEEDED_KILLS) {
                    data.remove(WIN_START);
                    data.remove(WIN_KILLS);
                    CharacterService.markChapter(killer, "witness_rika");
                } else {
                    killer.displayClientMessage(Component.literal("目睹里香 " + n + "/" + NEEDED_KILLS), true);
                }
            }
        }

        // ② 击败夏油杰
        if (dead instanceof GetoEntity) {
            CharacterService.markChapter(killer, "geto_duel");
            teleportHyakki(killer);
        }

        // ③ 百鬼夜行维度内击杀
        if (dead instanceof LowCurseEntity && dead.level().dimension().equals(HYAKKI)) {
            var data = Curse.data(killer);
            int n = data.getInt(HYAKKI_KILLS) + 1;
            data.putInt(HYAKKI_KILLS, n);
            if (n % 10 == 0) killer.displayClientMessage(Component.literal("百鬼夜行 " + n + "/" + HYAKKI_NEEDED), true);
            if (n >= HYAKKI_NEEDED) CharacterService.markChapter(killer, "hyakki_yagyo");
        }
    }

    /** 维度内定时补怪：每 5 秒最多 1 只，玩家 64 格内上限 15。 */
    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        ServerLevel level = e.getServer().getLevel(HYAKKI);
        if (level == null || level.getGameTime() % SPAWN_INTERVAL != 0) return;
        for (ServerPlayer p : level.players()) {
            if (level.getEntitiesOfClass(LowCurseEntity.class, p.getBoundingBox().inflate(64)).size() >= CURSE_CAP) continue;
            LowCurseEntity curse = Content.LOW_CURSE.get().create(level);
            if (curse == null) continue;
            double a = level.random.nextDouble() * Math.PI * 2, r = 10 + level.random.nextDouble() * 12;
            int x = (int) Math.floor(p.getX() + Math.cos(a) * r), z = (int) Math.floor(p.getZ() + Math.sin(a) * r);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            curse.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360f, 0);
            level.addFreshEntity(curse);
        }
    }

    /** 完成第二章后把玩家送进百鬼夜行；首次进入时用方块搭一片废墟场地。 */
    public static void teleportHyakki(ServerPlayer p) {
        ServerLevel level = p.server.getLevel(HYAKKI);
        if (level == null) {
            p.displayClientMessage(Component.literal("百鬼夜行维度未注册（检查 data/sukuna/dimension/hyakki.json）"), false);
            return;
        }
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
        BlockPos base = new BlockPos(0, surface, 0);
        buildRuins(level, base);
        p.teleportTo(level, 0.5, base.getY() + 1, 0.5, p.getYRot(), p.getXRot());
        p.displayClientMessage(Component.literal("百鬼夜行：击败 100 只咒灵"), false);
    }

    /** 简易废墟：石砖地面 + 外围断墙 + 四根残柱。已存在则不重复生成。 */
    private static void buildRuins(ServerLevel level, BlockPos base) {
        if (!level.getBlockState(base).isAir()) return;
        int r = 14;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                level.setBlockAndUpdate(base.offset(x, 0, z), Blocks.STONE_BRICKS.defaultBlockState());
                boolean edge = Math.abs(x) >= r - 1 || Math.abs(z) >= r - 1;
                if (edge) for (int y = 1; y <= 3; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.COBBLESTONE.defaultBlockState());
            }
        }
        for (int[] c : new int[][]{{-8, -8}, {8, -8}, {-8, 8}, {8, 8}}) {
            int h = 4 + level.random.nextInt(4);
            for (int y = 1; y <= h; y++) level.setBlockAndUpdate(base.offset(c[0], y, c[1]), Blocks.STONE_BRICKS.defaultBlockState());
        }
    }

    private static boolean isYuta(ServerPlayer p) {
        var d = CharacterService.selected(p);
        return d != null && "yuta".equals(d.id());
    }

    private YutaProgress() {}
}
