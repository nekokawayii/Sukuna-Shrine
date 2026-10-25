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
    private static final int HYAKKI_NEEDED = 50;
    private static final int WAVE_SIZE = 10;      // 每 10 只一波，共 5 波
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
        // 里香不是 CombatSafety 认识的召唤物，所以「里香打死」的击杀会解析出 null，
        // 导致乙骨主要输出手段不计入章节。这里补一条：伤害来源是里香就记到主人头上。
        if (killer == null && e.getSource().getEntity() instanceof cn.blockforge.generated.nnnnr1201forge2.RikaEntity rika
            && rika.ownerId() != null && rika.level() instanceof ServerLevel rl) {
            killer = rl.getServer().getPlayerList().getPlayer(rika.ownerId());
        }
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
        // 第四章：击杀虎杖残影（sukuna:yuji_echo）。
        if (dead instanceof cn.blockforge.generated.nnnnr1201forge2.YujiEntity) {
            CharacterService.markChapter(killer, "execute_yuji");
        }

        if (dead instanceof GetoEntity) {
            CharacterService.markChapter(killer, "geto_duel");
            teleportHyakki(killer);
        }

        // ③ 百鬼夜行维度内击杀
        if (dead instanceof LowCurseEntity && dead.level().dimension().equals(HYAKKI)) {
            var data = Curse.data(killer);
            int n = data.getInt(HYAKKI_KILLS) + 1;
            data.putInt(HYAKKI_KILLS, n);
            if (n % WAVE_SIZE == 0) {
                int wave = n / WAVE_SIZE;
                Curse.add(killer, 200); Curse.sync(killer);   // 每波清完回补咒力
                killer.displayClientMessage(Component.literal(
                    "百鬼夜行 第 " + wave + "/" + (HYAKKI_NEEDED / WAVE_SIZE) + " 波完成（" + n + "/" + HYAKKI_NEEDED + "）· 咒力回补"), false);
            }
            if (n >= HYAKKI_NEEDED) {
                CharacterService.markChapter(killer, "hyakki_yagyo");
                returnToOverworld(killer);
            }
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

    /** 第三章完成：送回进入前的主世界坐标（没有记录则用世界出生点）。 */
    private static void returnToOverworld(ServerPlayer p) {
        var data = Curse.data(p);
        ServerLevel overworld = p.server.overworld();
        double x, y, z;
        if (data.contains("YutaReturnX")) {
            x = data.getDouble("YutaReturnX");
            y = data.getDouble("YutaReturnY");
            z = data.getDouble("YutaReturnZ");
            data.remove("YutaReturnX");
            data.remove("YutaReturnY");
            data.remove("YutaReturnZ");
        } else {
            var spawn = overworld.getSharedSpawnPos();
            x = spawn.getX() + 0.5;
            y = spawn.getY();
            z = spawn.getZ() + 0.5;
        }
        // 保险：用该列的地表高度校正 Y，避免回到实心方块里（会卡死客户端）。
        int surfaceY = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            net.minecraft.core.BlockPos.containing(x, 0, z).getX(),
            net.minecraft.core.BlockPos.containing(x, 0, z).getZ());
        if (y < surfaceY) y = surfaceY;
        p.teleportTo(overworld, x, y, z, p.getYRot(), p.getXRot());
        p.displayClientMessage(Component.literal("百鬼夜行结束，已返回主世界"), false);
    }

    /** 完成第二章后把玩家送进百鬼夜行；首次进入时用方块搭一片废墟场地。 */
    public static void teleportHyakki(ServerPlayer p) {
        ServerLevel level = p.server.getLevel(HYAKKI);
        if (level == null) {
            p.displayClientMessage(Component.literal("百鬼夜行维度未注册（检查 data/sukuna/dimension/hyakki.json）"), false);
            return;
        }
        // 记录进入前的主世界坐标，章成后自动送回去。
        // 只在主世界记录：若玩家已经在百鬼夜行里（重复触发），不能把维度内的坐标当成回程点，
        // 否则会把人送回主世界地底（实测 y=-59.5 卡死）。
        var data = Curse.data(p);
        boolean inOverworld = p.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD);
        if (inOverworld) {
            data.putDouble("YutaReturnX", p.getX());
            data.putDouble("YutaReturnY", p.getY());
            data.putDouble("YutaReturnZ", p.getZ());
        }
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
        BlockPos base = new BlockPos(0, surface, 0);
        buildRuins(level, base);
        p.teleportTo(level, 0.5, base.getY() + 1, 0.5, p.getYRot(), p.getXRot());
        p.displayClientMessage(Component.literal("百鬼夜行：击败 100 只咒灵"), false);
    }

    /** 简易废墟：石砖地面 + 外围断墙 + 四根残柱。已存在则不重复生成。 */
    private static void buildRuins(ServerLevel level, BlockPos base) {
        if (!level.getBlockState(base).isAir()) return; // 已生成过
        var rnd = level.random;
        final int R = 30;

        // 地面：石砖为主，混入裂砖/苔砖/砂砾，随机破洞
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                if (Math.sqrt(x * x + z * z) > R) continue;
                var state = switch (rnd.nextInt(14)) {
                    case 0, 1 -> Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
                    case 2, 3 -> Blocks.GRAVEL.defaultBlockState();
                    case 4 -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
                    default -> Blocks.STONE_BRICKS.defaultBlockState();
                };
                level.setBlock(base.offset(x, 0, z), state, 2);
            }
        }

        // 中央参道
        for (int z = -R; z <= R; z++) {
            for (int x = -1; x <= 1; x++) {
                level.setBlock(base.offset(x, 0, z), Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
            }
        }

        // 外围断墙（高低起伏 + 随机缺口）
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d < R - 1.2 || d > R) continue;
                if (rnd.nextInt(7) == 0) continue; // 缺口
                int h = 2 + rnd.nextInt(4);
                for (int y = 1; y <= h; y++) {
                    var state = (y == h && rnd.nextBoolean())
                        ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                        : Blocks.COBBLESTONE.defaultBlockState();
                    level.setBlock(base.offset(x, y, z), state, 2);
                }
            }
        }

        // 四角残塔
        for (int[] c : new int[][]{{-22, -22}, {22, -22}, {-22, 22}, {22, 22}}) tower(level, base.offset(c[0], 0, c[1]), rnd);

        // 参道尽头的鸟居
        torii(level, base.offset(0, 0, -24));

        // 参道两侧石灯笼
        for (int z = -20; z <= 20; z += 10) {
            for (int side : new int[]{-4, 4}) lantern(level, base.offset(side, 0, z));
        }

        // 墓碑 / 枯树 / 瓦砾 / 蛛网 / 白骨
        for (int i = 0; i < 26; i++) {
            int x = -R + 3 + rnd.nextInt(2 * R - 5), z = -R + 3 + rnd.nextInt(2 * R - 5);
            if (Math.abs(x) <= 2) continue; // 不挡参道
            BlockPos at = base.offset(x, 1, z);
            switch (rnd.nextInt(6)) {
                case 0 -> { // 墓碑
                    level.setBlock(at, Blocks.COBBLESTONE_WALL.defaultBlockState(), 2);
                    level.setBlock(at.above(), Blocks.STONE_SLAB.defaultBlockState(), 2);
                    if (rnd.nextBoolean()) level.setBlock(at.below(), Blocks.GRAVEL.defaultBlockState(), 2);
                }
                case 1 -> { // 枯树
                    int h = 3 + rnd.nextInt(3);
                    for (int y = 0; y < h; y++) level.setBlock(at.offset(0, y, 0), Blocks.DARK_OAK_LOG.defaultBlockState(), 2);
                    level.setBlock(at.offset(1, h - 1, 0), Blocks.DARK_OAK_LOG.defaultBlockState(), 2);
                    level.setBlock(at.offset(-1, h - 2, 0), Blocks.DARK_OAK_LOG.defaultBlockState(), 2);
                }
                case 2 -> level.setBlock(at, Blocks.COBWEB.defaultBlockState(), 2);
                case 3 -> level.setBlock(at, Blocks.BONE_BLOCK.defaultBlockState(), 2);
                case 4 -> level.setBlock(at, Blocks.COBBLESTONE_SLAB.defaultBlockState(), 2);
                default -> level.setBlock(at, Blocks.DEAD_BUSH.defaultBlockState(), 2);
            }
        }
    }

    /** 残塔：方形塔身，顶部随机残缺。 */
    private static void tower(ServerLevel level, BlockPos c, net.minecraft.util.RandomSource rnd) {
        int h = 9 + rnd.nextInt(6), w = 3;
        for (int x = -w; x <= w; x++) {
            for (int z = -w; z <= w; z++) {
                boolean shell = Math.abs(x) == w || Math.abs(z) == w;
                if (!shell) continue;
                for (int y = 1; y <= h; y++) {
                    if (y > h - 3 && rnd.nextInt(3) == 0) continue; // 顶部残缺
                    level.setBlock(c.offset(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
                }
            }
        }
        // 塔内一盏魂灯
        level.setBlock(c.offset(0, 1, 0), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
    }

    /** 鸟居：两根红柱 + 上下两道梁。 */
    private static void torii(ServerLevel level, BlockPos c) {
        for (int side : new int[]{-4, 4}) {
            for (int y = 1; y <= 5; y++) level.setBlock(c.offset(side, y, 0), Blocks.RED_TERRACOTTA.defaultBlockState(), 2);
        }
        for (int x = -6; x <= 6; x++) {
            level.setBlock(c.offset(x, 6, 0), Blocks.RED_TERRACOTTA.defaultBlockState(), 2); // 岛木
            level.setBlock(c.offset(x, 5, 0), Blocks.RED_TERRACOTTA.defaultBlockState(), 2); // 贯
        }
        level.setBlock(c.offset(0, 7, 0), Blocks.RED_TERRACOTTA.defaultBlockState(), 2);
    }

    /** 石灯笼：柱 + 灯。 */
    private static void lantern(ServerLevel level, BlockPos c) {
        level.setBlock(c, Blocks.STONE_BRICKS.defaultBlockState(), 2);
        level.setBlock(c.above(), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
        level.setBlock(c.above(2), Blocks.LANTERN.defaultBlockState(), 2);
    }

    private static boolean isYuta(ServerPlayer p) {
        var d = CharacterService.selected(p);
        return d != null && "yuta".equals(d.id());
    }

    private YutaProgress() {}
}
