package character.culling;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 死灭洄游结界内的刷新与敌对（r218）。
 *
 * <p>上限：宿傩 1 只、咒灵 5 只、咒术师 8 只；数值随波次递增——头 8 个咒术师只有 150 血，
 * 之后每 8 个一波，生命与攻击逐步抬高。界内所有生物强制互相敌对（玩家、NPC、咒灵混战）。
 */
public final class CullingSpawns {
    public static final int CAP_SORCERER = 8, CAP_CURSE = 5, CAP_SUKUNA = 1;
    private static final double BASE_SORCERER_HP = 150, BASE_CURSE_HP = 90;
    private static final int WAVE = 8;
    private static final String[] SORCERERS = {"participant_ryu", "participant_kashimo", "participant_mahito", "naoya", "kenjaku", "renegade", "toji_echo"};
    private static final String[] CURSES = {"mutant", "special_curse", "low_curse", "ash_curse", "wraith"};
    /** 波次计数：每触发一轮刷新 +1，用来抬高后续一波的数值。 */
    private static int WAVE_INDEX;

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var server = e.getServer();
        if (server.getTickCount() % 100 != 0) return;
        ServerLevel level = server.getLevel(CullingGame.DIM);
        if (level == null || level.players().isEmpty()) return;
        List<Mob> all = level.getEntitiesOfClass(Mob.class, new AABB(-256, 0, -256, 256, 512, 256),
            m -> m.isAlive() && m.getPersistentData().getBoolean("CullingEnemy"));
        int sukuna = 0, sorcerers = 0, curses = 0;
        for (Mob m : all) {
            String kind = m.getPersistentData().getString("CullingKind");
            if ("sukuna".equals(kind)) sukuna++;
            else if ("sorcerer".equals(kind)) sorcerers++;
            else curses++;
        }
        boolean elite = anyRule(level, "elite") || anyRule(level, "elite_law");
        boolean ancient = anyRule(level, "ancient");
        int wave = WAVE_INDEX;
        if (sukuna < CAP_SUKUNA) spawnSukuna(level, wave);
        if (curses < CAP_CURSE) spawn(level, CURSES, "curse", Math.min(CAP_CURSE - curses, elite ? 1 : 2), wave, elite);
        int sorcererCap = ancient ? CAP_SORCERER * 3 : CAP_SORCERER;
        if (sorcerers < sorcererCap) spawn(level, SORCERERS, "sorcerer", Math.min(sorcererCap - sorcerers, elite ? 1 : 2), wave, elite);
        if (anyRule(level, "overflow") && level.getRandom().nextInt(3) == 0)
            spawn(level, CURSES, "curse", 2, wave, false);
        // 全员敌对
        for (Mob m : all) hostile(level, m);
        // 波次推进：每次触发刷新后记一次，每 8 个参赛者抬一档数值
        WAVE_INDEX = wave + 1;
    }

    private static void spawn(ServerLevel level, String[] pool, String kind, int count, int wave, boolean elite) {
        var rnd = level.random;
        for (int i = 0; i < count; i++) {
            ServerPlayer anchor = level.players().get(rnd.nextInt(level.players().size()));
            BlockPos at = findSpot(level, anchor, rnd);
            if (at == null) continue;
            String role = pool[rnd.nextInt(pool.length)];
            if (kind.equals("curse") && elite) role = "special_curse";
            if (kind.equals("sorcerer") && elite) role = "kenjaku";
            Mob mob = ExpeditionMobs.spawn(level, at, role, anchor.isCreative() ? null : anchor);
            if (mob == null) continue;
            level.getScoreboard().removePlayerFromTeam(mob.getScoreboardName());
            mob.getPersistentData().putBoolean("CullingEnemy", true);
            mob.getPersistentData().putString("CullingKind", kind);
            mob.getPersistentData().putLong("ExpeditionDespawnAt", level.getGameTime() + 9600);
            scale(level, mob, kind, wave);
            name(level, mob, kind);
        }
    }

    /** 数值从低到高：头 8 个只有基础血，之后每波 ×1.6。 */
    private static void scale(ServerLevel level, Mob mob, String kind, int wave) {
        int stage = Math.max(0, wave / WAVE);
        double base = kind.equals("sorcerer") ? BASE_SORCERER_HP : BASE_CURSE_HP;
        double hp = base * (1 + 0.25 * stage);
        if (kind.equals("curse") && "special_curse".equals(mob.getPersistentData().getString("ExpeditionRole"))) hp *= 3;
        var max = mob.getAttribute(Attributes.MAX_HEALTH);
        if (max != null) { max.setBaseValue(hp); mob.setHealth((float) hp); }
        var atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue(Math.min(120, 6 * (1 + 0.15 * stage)));
        mob.getPersistentData().putInt("CullingStage", stage);
    }

    private static void name(ServerLevel level, Mob mob, String kind) {
        int stage = mob.getPersistentData().getInt("CullingStage");
        String label = kind.equals("sorcerer") ? "死灭回游参赛者" : "死灭咒灵";
        mob.setCustomName(Component.literal(label + " · 第 " + (stage + 1) + " 波 · " + (int) mob.getMaxHealth() + " 血"));
        mob.setCustomNameVisible(true);
    }

    private static void spawnSukuna(ServerLevel level, int wave) {
        var type = Content.SUKUNA_NPC.get();
        if (type == null) return;
        ServerPlayer anchor = level.players().get(level.random.nextInt(level.players().size()));
        BlockPos at = findSpot(level, anchor, level.random);
        if (at == null) return;
        Mob mob = type.create(level);
        if (mob == null) return;
        mob.moveTo(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
        if (!level.noCollision(mob)) { mob.discard(); return; }
        level.getScoreboard().removePlayerFromTeam(mob.getScoreboardName());
        mob.getPersistentData().putBoolean("CullingEnemy", true);
        mob.getPersistentData().putString("CullingKind", "sukuna");
        mob.getPersistentData().putLong("ExpeditionDespawnAt", level.getGameTime() + 9600);
        var max = mob.getAttribute(Attributes.MAX_HEALTH);
        if (max != null) { double hp = Math.max(2000, max.getBaseValue()) * 3; max.setBaseValue(hp); mob.setHealth((float) hp); }
        mob.setCustomName(Component.literal("宿傩 · 死灭霸主 · " + (int) mob.getMaxHealth() + " 血"));
        mob.setCustomNameVisible(true);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        for (ServerPlayer p : level.players())
            p.displayClientMessage(Component.literal("宿傩降临死灭洄游：全场只剩一个，杀了它收益最高。").withStyle(net.minecraft.ChatFormatting.DARK_RED), false);
    }

    private static void hostile(ServerLevel level, Mob mob) {
        LivingEntity current = mob.getTarget();
        if (current != null && current.isAlive() && current.level() == level && mob.distanceToSqr(current) < 48 * 48) return;
        LivingEntity best = null; double bestD = Double.MAX_VALUE;
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(48),
            t -> t != mob && t.isAlive() && !t.isSpectator() && !(t instanceof ServerPlayer sp && (sp.isCreative() || sp.isSpectator())) && t.level() == level)) {
            double d = mob.distanceToSqr(t);
            if (d < bestD) { bestD = d; best = t; }
        }
        if (best != null) mob.setTarget(best);
    }

    private static BlockPos findSpot(ServerLevel level, ServerPlayer anchor, net.minecraft.util.RandomSource rnd) {
        for (int tries = 0; tries < 24; tries++) {
            int x = (int) anchor.getX() + rnd.nextInt(65) - 32;
            int z = (int) anchor.getZ() + rnd.nextInt(65) - 32;
            if (Math.abs(x) < 24 && Math.abs(z) < 24) continue;    // 中央通讯塔留空
            for (int dy = 8; dy >= -8; dy--) {
                BlockPos at = new BlockPos(x, 65 + dy, z);
                if (!level.hasChunkAt(at)) continue;
                if (level.getBlockState(at).isAir() && level.getBlockState(at.above()).isAir()
                    && level.getBlockState(at.below()).isSolidRender(level, at.below())) return at;
            }
        }
        return null;
    }

    // ===== r220：参赛者也会攒积分制定规则 =====
    /** NPC 已经制定过的规则（不含玩家选的）。 */
    private static final java.util.List<Integer> NPC_RULES = new java.util.ArrayList<>();
    private static final int NPC_RULE_COST = 100, NPC_POINTS_PER_KILL = 10;

    public static java.util.List<String> npcRuleIds() {
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (int i : NPC_RULES) if (i >= 0 && i < CullingRules.ALL.size()) ids.add(CullingRules.ALL.get(i).id());
        return ids;
    }

    /** 怪物打人的伤害倍率（来自 NPC 制定的规则）。 */
    public static double npcDamageMul() {
        double v = 1;
        for (int i : NPC_RULES) if (i >= 0 && i < CullingRules.ALL.size()) v *= CullingRules.ALL.get(i).mods().damage;
        return v;
    }

    /** 怪物挨打的倍率（来自 NPC 制定的规则）。 */
    public static double npcTakenMul() {
        double v = 1;
        for (int i : NPC_RULES) if (i >= 0 && i < CullingRules.ALL.size()) v *= CullingRules.ALL.get(i).mods().taken;
        return v;
    }

    @SubscribeEvent
    public static void npcKill(net.minecraftforge.event.entity.living.LivingDeathEvent e) {
        if (!e.getEntity().level().dimension().equals(CullingGame.DIM)) return;
        if (!(e.getSource().getEntity() instanceof Mob killer)) return;
        if (!killer.getPersistentData().getBoolean("CullingEnemy")) return;
        var data = killer.getPersistentData();
        int points = data.getInt("CullingNpcPoints") + NPC_POINTS_PER_KILL;
        if (points < NPC_RULE_COST) { data.putInt("CullingNpcPoints", points); return; }
        data.putInt("CullingNpcPoints", points - NPC_RULE_COST);
        pickNpcRule(killer);
    }

    private static void pickNpcRule(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        java.util.List<Integer> pool = new java.util.ArrayList<>();
        for (int i = 0; i < CullingRules.ALL.size(); i++) if (!NPC_RULES.contains(i)) pool.add(i);
        if (pool.isEmpty()) return;
        int index = pool.get(level.random.nextInt(pool.size()));
        NPC_RULES.add(index);
        var rule = CullingRules.ALL.get(index);
        String name = mob.getCustomName() == null ? "死灭参赛者" : mob.getCustomName().getString();
        Component message = Component.literal("【死灭规则】" + name + " 制定了规则：" + rule.name() + " · " + rule.desc())
            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
        for (ServerPlayer p : level.players()) {
            p.sendSystemMessage(message);
            p.displayClientMessage(Component.literal("【死灭规则】" + rule.name()).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
        }
        level.playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
            net.minecraft.sounds.SoundSource.HOSTILE, 1.4f, .8f);
    }

    private static boolean anyRule(ServerLevel level, String special) {
        for (ServerPlayer p : level.players())
            if (CullingRules.special(p, special)) return true;
        return false;
    }

    private CullingSpawns() { }
}
