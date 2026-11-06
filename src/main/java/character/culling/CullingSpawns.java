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
    /** 甚尔：每三波最多一只。 */
    private static final String TOJI_ROLE = "toji_echo";

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var server = e.getServer();
        if (server.getTickCount() % 100 != 0) return;
        ServerLevel level = server.getLevel(CullingGame.DIM);
        if (level == null || level.players().isEmpty()) return;
        List<Mob> all = level.getEntitiesOfClass(Mob.class, new AABB(-256, 0, -256, 256, 512, 256),
            m -> m.isAlive() && m.getPersistentData().getBoolean("CullingEnemy"));
        int sukuna = 0, sorcerers = 0, curses = 0, toji = 0;
        for (Mob m : all) {
            String kind = m.getPersistentData().getString("CullingKind");
            if ("sukuna".equals(kind)) sukuna++;
            else if ("sorcerer".equals(kind)) sorcerers++;
            else curses++;
            if (TOJI_ROLE.equals(m.getPersistentData().getString("ExpeditionRole"))) toji++;
        }
        boolean elite = anyRule(level, "elite") || anyRule(level, "elite_law");
        boolean ancient = anyRule(level, "ancient");
        int wave = WAVE_INDEX;
        int stage = Math.max(0, wave / WAVE);
        // r221：甚尔每三波最多一只——场上已有就不再刷，且只允许在第 0/3/6… 波刷出来
        boolean allowToji = toji == 0 && stage % 3 == 0;
        if (sukuna < CAP_SUKUNA) spawnSukuna(level, wave);
        if (curses < CAP_CURSE) spawnVanilla(level, Math.min(CAP_CURSE - curses, elite ? 2 : 3), wave, elite);
        // r230：羂索本体（轻量独立生物，只会召唤强化原版怪），场上最多一只
        if (level.random.nextFloat() < GETO_CHANCE) spawnGeto(level, wave);
        int sorcererCap = ancient ? CAP_SORCERER * 3 : CAP_SORCERER;
        if (sorcerers < sorcererCap) spawn(level, SORCERERS, "sorcerer", Math.min(sorcererCap - sorcerers, elite ? 1 : 2), wave, elite, allowToji);
        // r227：界内会偶尔刷出五条悟 / 虎杖残影这类"大人物"，血量按普通泳者算
        if (level.random.nextFloat() < BOSS_CHANCE) spawnBoss(level, wave);
        if (anyRule(level, "overflow") && level.getRandom().nextInt(3) == 0)
            spawn(level, CURSES, "curse", 2, wave, false);
        // 全员敌对
        for (Mob m : all) hostile(level, m);
        // 波次推进：每次触发刷新后记一次，每 8 个参赛者抬一档数值
        WAVE_INDEX = wave + 1;
    }

    /** 五条悟 / 虎杖残影作为参赛者出现的概率（每轮刷新掷一次）。 */
    private static final float BOSS_CHANCE = 0.18f;

    /** Boss 级角色池：全部按"普通参赛者"处理（血量与泳者相同）。 */
    private static final String[] BOSS_NAMES = {
        "五条悟", "虎杖残影", "伏黑惠", "即身佛", "咒胎戴天", "破碎真人"
    };

    private static Mob createBoss(ServerLevel level, int pick) {
        return switch (pick) {
            case 0 -> cn.blockforge.generated.nnnnr1201forge2.VoidContent.GOJO.get().create(level);
            case 1 -> cn.blockforge.generated.nnnnr1201forge2.SoulContent.YUJI.get().create(level);
            case 2 -> cn.blockforge.generated.nnnnr1201forge2.SoulContent.MEGUMI.get().create(level);
            case 3 -> cn.blockforge.generated.nnnnr1201forge2.Content.BUDDHA.get().create(level);
            case 4 -> cn.blockforge.generated.nnnnr1201forge2.Content.DAITIAN.get().create(level);
            default -> cn.blockforge.generated.nnnnr1201forge2.Content.BROKEN_MAHITO.get().create(level);
        };
    }

    /** 刷一只 Boss 级参赛者，血量与普通泳者完全相同。 */
    private static void spawnBoss(ServerLevel level, int wave) {
        for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(-256, 0, -256, 256, 512, 256),
            m -> m.isAlive() && m.getPersistentData().getBoolean("CullingBoss"))) return;
        if (level.players().isEmpty()) return;
        ServerPlayer anchor = level.players().get(level.random.nextInt(level.players().size()));
        BlockPos at = findSpot(level, anchor, level.random);
        if (at == null) return;
        int pick = level.random.nextInt(BOSS_NAMES.length);
        Mob mob = createBoss(level, pick);
        if (mob == null) return;
        mob.moveTo(at.getX() + .5, at.getY(), at.getZ() + .5, level.random.nextFloat() * 360f, 0);
        level.addFreshEntity(mob); // 先进世界（BossControl 会在这时按 Boss 原值铺属性）
        if (mob instanceof cn.blockforge.generated.nnnnr1201forge2.YujiEntity y) y.challenger = anchor.getUUID();
        level.getScoreboard().removePlayerFromTeam(mob.getScoreboardName());
        mob.getPersistentData().putBoolean("CullingEnemy", true);
        mob.getPersistentData().putBoolean("CullingBoss", true);
        mob.getPersistentData().putString("CullingKind", "sorcerer");
        mob.getPersistentData().putLong("ExpeditionDespawnAt", level.getGameTime() + 9600);
        scale(level, mob, "sorcerer", wave); // 覆盖成普通泳者血量
        applyNpcRuleMods(mob);
        mob.setCustomName(Component.literal("死灭回游参赛者 · " + BOSS_NAMES[pick]));
        mob.setCustomNameVisible(true);
        for (ServerPlayer p : level.players())
            p.displayClientMessage(Component.literal("【死灭洄游】" + BOSS_NAMES[pick] + " 作为参赛者出现了。"), true);
    }

    /** 羂索本体出现概率。 */
    private static final float GETO_CHANCE = .12f;
    /** 强化原版生物池（替代自定义咒灵，省性能）。 */
    private static final net.minecraft.world.entity.EntityType<?>[] VANILLA = {
        net.minecraft.world.entity.EntityType.ZOMBIE, net.minecraft.world.entity.EntityType.HUSK,
        net.minecraft.world.entity.EntityType.SKELETON, net.minecraft.world.entity.EntityType.STRAY,
        net.minecraft.world.entity.EntityType.SPIDER, net.minecraft.world.entity.EntityType.CREEPER
    };

    /** 刷一波强化原版生物。 */
    private static void spawnVanilla(ServerLevel level, int count, int wave, boolean elite) {
        var rnd = level.random;
        if (level.players().isEmpty()) return;
        int stage = Math.max(0, wave / WAVE);
        for (int i = 0; i < count; i++) {
            ServerPlayer anchor = level.players().get(rnd.nextInt(level.players().size()));
            BlockPos at = findSpot(level, anchor, rnd);
            if (at == null) continue;
            net.minecraft.world.entity.EntityType<?> type = VANILLA[rnd.nextInt(VANILLA.length)];
            if (!(type.create(level) instanceof Mob mob)) continue;
            mob.moveTo(at.getX() + .5, at.getY(), at.getZ() + .5, rnd.nextFloat() * 360f, 0);
            level.addFreshEntity(mob);
            level.getScoreboard().removePlayerFromTeam(mob.getScoreboardName());
            mob.getPersistentData().putBoolean("CullingEnemy", true);
            mob.getPersistentData().putString("CullingKind", "curse");
            mob.getPersistentData().putInt("CullingStage", stage);
            mob.getPersistentData().putLong("ExpeditionDespawnAt", level.getGameTime() + 9600);
            double hp = BASE_CURSE_HP * (1 + .25 * stage) * 2;
            var max = mob.getAttribute(Attributes.MAX_HEALTH);
            if (max != null) { max.setBaseValue(hp); mob.setHealth((float) hp); }
            var atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null) atk.setBaseValue(Math.min(120, 8 * (1 + .2 * stage)));
            var spd = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (spd != null) spd.setBaseValue(spd.getBaseValue() * 1.1);
            mob.setCustomName(Component.literal("死灭咒灵 · 强化" + mob.getName().getString()));
            mob.setCustomNameVisible(false);
            applyNpcRuleMods(mob);
        }
    }

    /** 刷一只死灭洄游专用的羂索（只会召唤强化原版生物）。 */
    private static void spawnGeto(ServerLevel level, int wave) {
        for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(-256, 0, -256, 256, 512, 256),
            m -> m.isAlive() && m.getPersistentData().getBoolean("CullingGeto"))) return;
        if (level.players().isEmpty()) return;
        ServerPlayer anchor = level.players().get(level.random.nextInt(level.players().size()));
        BlockPos at = findSpot(level, anchor, level.random);
        if (at == null) return;
        Mob mob = cn.blockforge.generated.nnnnr1201forge2.Content.CULLING_GETO.get().create(level);
        if (mob == null) return;
        mob.moveTo(at.getX() + .5, at.getY(), at.getZ() + .5, level.random.nextFloat() * 360f, 0);
        level.addFreshEntity(mob);
        level.getScoreboard().removePlayerFromTeam(mob.getScoreboardName());
        mob.getPersistentData().putBoolean("CullingEnemy", true);
        mob.getPersistentData().putBoolean("CullingGeto", true);
        mob.getPersistentData().putString("CullingKind", "sorcerer");
        mob.getPersistentData().putLong("ExpeditionDespawnAt", level.getGameTime() + 9600);
        scale(level, mob, "sorcerer", wave);
        applyNpcRuleMods(mob);
        mob.setCustomName(Component.literal("死灭回游参赛者 · 夏油杰"));
        mob.setCustomNameVisible(true);
        for (ServerPlayer p : level.players())
            p.displayClientMessage(Component.literal("【死灭洄游】夏油杰出现了——他只会不断召唤强化过的咒灵。"), true);
    }

    private static void spawn(ServerLevel level, String[] pool, String kind, int count, int wave, boolean elite) {
        spawn(level, pool, kind, count, wave, elite, true);
    }

    private static void spawn(ServerLevel level, String[] pool, String kind, int count, int wave, boolean elite, boolean allowToji) {
        var rnd = level.random;
        for (int i = 0; i < count; i++) {
            ServerPlayer anchor = level.players().get(rnd.nextInt(level.players().size()));
            BlockPos at = findSpot(level, anchor, rnd);
            if (at == null) continue;
            String role = pool[rnd.nextInt(pool.length)];
            if (kind.equals("sorcerer") && TOJI_ROLE.equals(role) && !allowToji) role = "participant_ryu";
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
            applyNpcRuleMods(mob);
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
        LivingEntity best = nearestPrey(level, mob, 48);
        if (best != null) mob.setTarget(best);
    }

    /** 强制互相攻击（r231）：每 20 刻把所有界内生物的目标重设成最近的「玩家 / 参赛者 / 咒灵」。 */
    @SubscribeEvent
    public static void forceHostile(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var server = e.getServer();
        if (server.getTickCount() % 20 != 0) return;
        ServerLevel level = server.getLevel(CullingGame.DIM);
        if (level == null) return;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(-256, 0, -256, 256, 512, 256),
            m -> m.isAlive() && m.getPersistentData().getBoolean("CullingEnemy"))) {
            LivingEntity best = nearestPrey(level, mob, 48);
            if (best != null && mob.getTarget() != best) mob.setTarget(best);
        }
    }

    private static LivingEntity nearestPrey(ServerLevel level, Mob mob, double range) {
        LivingEntity best = null; double bestD = range * range;
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(range),
            t -> canPrey(level, mob, t))) {
            double d = mob.distanceToSqr(t);
            if (d < bestD) { bestD = d; best = t; }
        }
        return best;
    }

    /** 谁能打谁：玩家/参赛者/咒灵全都算；召唤物与召唤师之间不互打（否则自己召的怪会先把他打死）。 */
    private static boolean canPrey(ServerLevel level, Mob mob, LivingEntity t) {
        if (t == mob || !t.isAlive() || t.isSpectator() || t.level() != level) return false;
        if (t instanceof ServerPlayer sp && (sp.isCreative() || sp.isSpectator())) return false;
        boolean prey = t instanceof ServerPlayer || t.getPersistentData().getBoolean("CullingEnemy");
        if (!prey) return false;
        if (mob.getPersistentData().getBoolean("CullingSummon") && t.getPersistentData().getBoolean("CullingGeto")) return false;
        if (mob.getPersistentData().getBoolean("CullingGeto") && t.getPersistentData().getBoolean("CullingSummon")) return false;
        return true;
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

    /** r222：NPC 制定的属性类规则直接加到界内怪物身上（伤害/生命/移速/攻速）。 */
    private static final java.util.UUID NPC_DMG = java.util.UUID.fromString("8c11a000-0000-4a11-9c00-00000000a001");
    private static final java.util.UUID NPC_HP = java.util.UUID.fromString("8c11a000-0000-4a11-9c00-00000000a002");
    private static final java.util.UUID NPC_SPD = java.util.UUID.fromString("8c11a000-0000-4a11-9c00-00000000a003");
    private static final java.util.UUID NPC_ASP = java.util.UUID.fromString("8c11a000-0000-4a11-9c00-00000000a004");

    public static void applyNpcRuleMods(Mob mob) {
        if (mob == null || NPC_RULES.isEmpty()) return;
        double dmg = 1, hp = 1, spd = 1, asp = 1;
        for (int i : NPC_RULES) {
            if (i < 0 || i >= CullingRules.ALL.size()) continue;
            var m = CullingRules.ALL.get(i).mods();
            dmg *= m.damage; hp *= m.health; spd *= m.speed; asp *= m.atkSpeed;
        }
        mod(mob, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, NPC_DMG, dmg - 1);
        mod(mob, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, NPC_HP, hp - 1);
        mod(mob, net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, NPC_SPD, spd - 1);
        mod(mob, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, NPC_ASP, asp - 1);
    }

    private static void mod(Mob mob, net.minecraft.world.entity.ai.attributes.Attribute attr, java.util.UUID id, double amount) {
        var a = mob.getAttribute(attr);
        if (a == null) return;
        if (Math.abs(amount) < 1e-6) { if (a.getModifier(id) != null) a.removeModifier(id); return; }
        if (a.getModifier(id) != null) a.removeModifier(id);
        a.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
            id, "死灭NPC规则", amount, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
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
        for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(-256, 0, -256, 256, 512, 256),
            m -> m.isAlive() && m.getPersistentData().getBoolean("CullingEnemy"))) applyNpcRuleMods(m);
    }

    private static boolean anyRule(ServerLevel level, String special) {
        for (ServerPlayer p : level.players())
            if (CullingRules.special(p, special)) return true;
        return false;
    }

    private CullingSpawns() { }
}
