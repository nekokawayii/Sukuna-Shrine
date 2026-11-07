package cn.blockforge.generated.nnnnr1201forge2;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 宿傩夺舍（r327）：吞手指 / 虎杖每日两根 / 聊天输入"宿傩"主动夺舍 / 夺舍体按手指数强化。 */
public final class FingerPossession {
    private static final int CAMERA_LOCK = 798, ACTIVE_TICKS = 600, SEARCH = 200;
    /** 主动夺舍倒计时（到点后宿傩停止 AI，提示玩家换回来）。 */
    private record Pending(ServerPlayer player, Mob npc, long at) { }
    private static final List<Pending> ACTIVE = new ArrayList<>();

    private static final String[] TAUNTS = {
        "手指在喉间化作一声低笑——「这具身体，归我了。」",
        "「这副皮囊还算结实。就从你这儿开始吧。」",
        "「别挣扎了，你的咒力已经在给我暖手。」",
        "「有意思……这具身体里，居然还有能让我笑出来的东西。」",
        "「借你身体一用。放心，我会好好『照顾』它的。」",
        "「你以为是你吃了我？不，是我吞了你。」",
        "「这副身体的味道……比上一个好些。」"
    };
    private static final String[] YUJI_TAUNTS = {
        "宿傩咧开嘴：「……又是你？这副身体，我倒是熟了。」",
        "「虎杖悠仁。你总是这么不听话。」",
        "「又见面了，小鬼。这次，轮到我说了算。」",
        "「你的身体记得我。这就够了。」"
    };
    private static final String[] END_LINES = {
        "「下次再陪你玩。」",
        "「扫兴。这具身体先还你。」",
        "「呵……你会再求我回来的。」",
        "「记住这种感觉，小鬼。」"
    };
    private static final String[] RESIGNED = {
        "你放任宿傩继续支配这具身体。远处传来一声轻笑。",
        "「这就对了。看着就好。」",
        "你没有反抗，任由那双眼睛用你的身体看世界。"
    };
    private static final String[] RECLAIM = {
        "你夺回了身体的控制权，宿傩的身影散去。",
        "「……啧。」宿傩退回意识深处，身体重新属于你。",
        "你咬紧牙关把那张脸从意识里挤出去——控制权回来了。"
    };
    private static final java.util.Map<String, String[]> ROLE_TAUNTS = java.util.Map.ofEntries(
        java.util.Map.entry("gojo", new String[]{
            "「五条悟？呵……最强的身体，正好拿来用。」",
            "六眼有些刺眼。「不过，戴着它看世界还挺清楚。」",
            "「无下限……原来你是这么挡住别人的。」"}),
        java.util.Map.entry("uraume", new String[]{
            "「里梅。你总是这么守时。」",
            "「起来吧，我的厨师。今晚的料理由我来选。」",
            "「冰凝咒法……冷得很，倒也衬你。」"}),
        java.util.Map.entry("megumi", new String[]{
            "「惠……影子里那个孩子，我认得。」",
            "「十种影法术，可惜在你这儿只用来藏东西。」"}),
        java.util.Map.entry("jogo", new String[]{
            "「漏壶，火候还差得远。」",
            "「火山与岩浆……你这副身体倒是热得很。」"}),
        java.util.Map.entry("blood", new String[]{
            "「胀相。诅咒的血脉，闻着就亲切。」",
            "「赤血操术……你流的是我的血。」"}),
        java.util.Map.entry("kashimo", new String[]{
            "「鹿紫云一——当年那个疯子。还没打够吗？」"}),
        java.util.Map.entry("mahito", new String[]{
            "「真人。同类之间，就不必客气了。」"}),
        java.util.Map.entry("toji", new String[]{
            "「甚尔。这具身体真干净，一点咒力都没有。」"}),
        java.util.Map.entry("nobara", new String[]{"「那把锤子……有点吵。」"})
    );
    private static String pick(ServerPlayer p, String[] pool) { return pool[p.getRandom().nextInt(pool.length)]; }
    private static String taunt(ServerPlayer p, String role, boolean yuji) {
        if (yuji) return pick(p, YUJI_TAUNTS);
        String[] own = role == null ? null : ROLE_TAUNTS.get(role);
        if (own != null && p.getRandom().nextInt(100) < 80) return pick(p, own); // 80% 说专属台词
        return pick(p, TAUNTS);
    }
    private static int fingers(ServerPlayer p) { return Curse.fingers(p); }

    /** 术式伤害永久倍率（与 /sukuna damage 用的是同一把键）。 */
    private static void multiplyDamage(ServerPlayer p, double factor) {
        var d = Curse.data(p);
        double now = d.contains("DamageMultiplier") ? d.getDouble("DamageMultiplier") : 1.0;
        d.putDouble("DamageMultiplier", Math.min(10000.0, Math.max(0.001, now * factor)));
    }

    /** 手指吃完后的掷骰。返回 true = 被夺舍（不再走正常成长）。 */
    public static boolean tryPossess(ServerPlayer p) {
        var definition = character.CharacterService.selected(p);
        String id = definition == null ? "sukuna" : definition.id();
        boolean yuji = "yuji".equals(id);
        if ("sukuna".equals(id)) return false;
        int total = fingers(p) + 1; // 这一根即将入账
        if (yuji) {
            long day = p.level().getDayTime() / 24000L;
            var d = Curse.data(p);
            if (d.getLong("YujiFingerDay") != day) { d.putLong("YujiFingerDay", day); d.putInt("YujiFingerToday", 0); }
            int today = d.getInt("YujiFingerToday");
            if (today < 2) { // 虎杖：每日两根内，咒力直接翻倍、无副作用
                d.putInt("YujiFingerToday", today + 1);
                Curse.addRaw(p, (int) Math.min(1_000_000, Curse.maximum(p)));
                multiplyDamage(p, 1.1);
                Curse.consume(p, false);
                return true;
            }
            // 超过每日两根 → 也会被夺舍（宿傩用它自己的计数）
            Curse.consume(p, false);
            possess(p, total, true);
            return true;
        }
        if (p.getRandom().nextInt(100) < 30) { // 30%：咒力翻倍 + 反胃/黑暗/缓慢 15 秒
            Curse.addRaw(p, (int) Math.min(1_000_000, Curse.maximum(p)));
            multiplyDamage(p, 1.5);
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 0, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 300, 0, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 4, false, true));
            String[] surge = {"诅咒之力灌入体内：咒力猛然翻倍，但胃里翻江倒海、眼前漆黑、四肢沉重。",
                "一股腥甜冲上喉咙，咒力暴涨的同时，世界开始倾斜。",
                "咒力在血管里翻涌，像有什么东西在皮下爬。"};
            p.displayClientMessage(Component.literal(pick(p, surge)), true);
            return false;
        }
        Curse.consume(p, false);
        possess(p, total, true);
        return true;
    }

    /** 聊天框输入「宿傩」主动夺舍（至少吃过一根手指）。 */
    @SubscribeEvent
    public static void chat(ServerChatEvent e) {
        ServerPlayer p = e.getPlayer();
        if (!e.getMessage().getString().trim().equals("宿傩")) return;
        if (fingers(p) < 1) return;
        e.setCanceled(true);
        if (p.getPersistentData().getBoolean("SukunaPossessed")) return;
        Mob npc = possess(p, fingers(p), false);
        if (npc != null) ACTIVE.add(new Pending(p, npc, p.level().getGameTime() + ACTIVE_TICKS));
    }

    private static Mob possess(ServerPlayer p, int power, boolean aggressive) {
        if (!(p.level() instanceof ServerLevel level)) return null;
        Vec3 at = p.position();
        var npc = Content.SUKUNA_NPC.get().create(level);
        if (npc == null) return null;
        npc.moveTo(at.x, at.y, at.z, p.getYRot(), 0);
        npc.setPersistenceRequired();
        npc.finalizeSpawn(level, level.getCurrentDifficultyAt(p.blockPosition()), MobSpawnType.EVENT, null, null);
        npc.getPersistentData().putBoolean("PossessedSukuna", true);
        npc.getPersistentData().putInt("PossessedPower", power);
        var def = character.CharacterService.selected(p);
        npc.getPersistentData().putString("PossessedRole", def == null ? "sukuna" : def.id());
        tune(npc, power);
        level.addFreshEntity(npc);
        p.getPersistentData().putBoolean("SukunaPossessed", true);
        // 记录吃手指时的游戏模式，结束后恢复（用户明确要求）
        if (p.getPersistentData().getString("PossessedPrevMode").isEmpty())
            p.getPersistentData().putString("PossessedPrevMode", p.gameMode.getGameModeForPlayer().getName());
        p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);  // 旁观，但镜头挂在宿傩身上
        p.setCamera(npc);
        Network.send(level, new Network.Effect(CAMERA_LOCK, p.getId(), at, Vec3.ZERO, 1f, ACTIVE_TICKS, 0));
        var definition = character.CharacterService.selected(p);
        boolean yuji = definition != null && "yuji".equals(definition.id());
        String roleId = definition == null ? "sukuna" : definition.id();
        p.sendSystemMessage(Component.literal(taunt(p, roleId, yuji)).withStyle(Style.EMPTY.withColor(0xFF8A00)));
        if (yuji) {
            p.sendSystemMessage(button("继续", "/sukuna possess keep", 0xFFFFD27A, "让宿傩继续使用这具身体"));
            p.sendSystemMessage(button("夺回", "/sukuna possess back", 0xFFFF4A4A, "夺回控制权，宿傩消失（无掉落）"));
        } else {
            p.sendSystemMessage(button("自杀", "/sukuna possess suicide", 0xFFFF4A4A, "放弃这具身体，原地重生（宿傩留下）"));
        }
        return npc;
    }

    /** 每根手指强化：1 指 150 血 / 攻击 2 / 1 秒一击，越多越强。 */
    public static void tune(Mob npc, int power) {
        double hp = 150.0 * Math.max(1, power);
        var max = npc.getAttribute(Attributes.MAX_HEALTH); if (max != null) max.setBaseValue(hp);
        var atk = npc.getAttribute(Attributes.ATTACK_DAMAGE); if (atk != null) atk.setBaseValue(2 + Math.max(0, power - 1) * 0.6);
        var spd = npc.getAttribute(Attributes.MOVEMENT_SPEED); if (spd != null) spd.setBaseValue(0.30 + Math.min(0.15, power * 0.006));
        int interval = Math.max(10, 24 - power);   // 1 指约 1.2 秒，20 指 0.5 秒
        var aspd = npc.getAttribute(Attributes.ATTACK_SPEED);
        if (aspd != null) aspd.setBaseValue(20.0 / interval);
        npc.setHealth((float) hp);
        npc.getPersistentData().putInt("PossessedInterval", interval);
    }

    /** 夺舍体 AI：杀光半径 200 格内所有生物，优先近的，地下目标最后。 */
    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        long now = e.getServer().overworld().getGameTime();
        for (var it = ACTIVE.iterator(); it.hasNext(); ) {
            Pending pd = it.next();
            if (now < pd.at) continue;
            it.remove();
            if (pd.npc != null && pd.npc.isAlive()) {
                pd.npc.setNoAi(true);
                pd.player.sendSystemMessage(Component.literal("宿傩忽然停下，砸了砸嘴：「……无聊了，换回来。」").withStyle(Style.EMPTY.withColor(0xFF8A00)));
                pd.player.sendSystemMessage(button("换回来", "/sukuna possess return", 0xFF7FD7FF, "拿回身体，宿傩退去"));
            }
        }
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            if (!p.getPersistentData().getBoolean("SukunaPossessed")) continue;
            Mob host = null;
            for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32),
                    m -> m.getPersistentData().getBoolean("PossessedSukuna"))) { host = m; break; }
            if (host == null) continue;
            if (host.isNoAi() || now % 10 != 0) continue;
            final Mob self = host;
            LivingEntity best = null; double bestScore = Double.MAX_VALUE;
            for (LivingEntity t : self.level().getEntitiesOfClass(LivingEntity.class,
                    new AABB(self.blockPosition()).inflate(SEARCH), x -> x != self && x.isAlive() && x != p)) {
                double d = t.position().distanceToSqr(self.position());
                boolean underground = t.getY() < self.getY() - 3;
                double score = d + (underground ? 400.0 * 400.0 : 0); // 地下目标排最后
                if (score < bestScore) { bestScore = score; best = t; }
            }
            if (best != null) { self.setTarget(best); self.getNavigation().moveTo(best, 1.2); }
            if (now % 20 == 0) castSkills(self, best);
        }
    }

    /** 夺舍体轮流放"被夺舍角色的术式"和"宿傩自己的术式"（例：五条悟 → 无下限 + 御厨子）。 */
    private static void castSkills(Mob npc, LivingEntity target) {
        if (target == null) return;
        try {
            var actor = cn.blockforge.generated.nnnnr1201forge2.MaidBridge.actor(npc);
            if (actor == null) return;
            actor.aim = target;
            actor.syncPosition();
            String role = npc.getPersistentData().getString("PossessedRole");
            var pool = new ArrayList<String>();
            for (String r : new String[]{role, "sukuna"}) {
                character.CharacterDefinition def = null;
                for (var d : character.CharacterRegistry.all()) if (d.id().equals(r)) { def = d; break; }
                if (def == null) continue;
                for (int skill : def.skills()) if (skill >= 1000) pool.add(r + ":" + skill);
            }
            if (pool.isEmpty()) return;
            String pick = pool.get(npc.getRandom().nextInt(pool.size()));
            String[] parts = pick.split(":");
            character.CharacterCasting.castAs(actor, parts[0], Integer.parseInt(parts[1]), 1);
            Network.send((ServerLevel) npc.level(), new Network.Effect(CAMERA_LOCK + 1, npc.getId(), npc.position(), npc.getLookAngle(), 1f, 12, 0));
        } catch (Throwable ignored) { }
    }

    // ===== 聊天按钮对应的命令（/sukuna possess …） =====
    public static void suicide(ServerPlayer p) {
        // 保留夺舍体，只让玩家"重生"
        p.getPersistentData().remove("SukunaPossessed");
        p.setCamera(p);
        restoreMode(p);
        var spawn = p.server.getLevel(p.serverLevel().dimension()).getSharedSpawnPos();
        p.teleportTo(p.serverLevel(), spawn.getX() + .5, spawn.getY(), spawn.getZ() + .5, p.getYRot(), p.getXRot());
        p.setHealth(p.getMaxHealth());
        Network.send(p.serverLevel(), new Network.Effect(CAMERA_LOCK, p.getId(), p.position(), Vec3.ZERO, 1f, 0, 0));
        p.sendSystemMessage(Component.literal("你松开了那具身体，在出生点重新醒来。耳畔只剩一声若有若无的笑。").withStyle(Style.EMPTY.withColor(0xFF7FD7FF)));
    }

    public static void keep(ServerPlayer p) {
        p.sendSystemMessage(Component.literal(pick(p, RESIGNED)).withStyle(Style.EMPTY.withColor(0xFF8A00)));
    }

    public static void back(ServerPlayer p) {
        removeHost(p);
        finish(p);
        p.sendSystemMessage(Component.literal(pick(p, RECLAIM)).withStyle(Style.EMPTY.withColor(0xFF7FD7FF)));
    }

    public static void returnTo(ServerPlayer p) {
        removeHost(p);
        finish(p);
        p.sendSystemMessage(Component.literal(pick(p, END_LINES)).withStyle(Style.EMPTY.withColor(0xFF8A00)));
    }

    private static void removeHost(ServerPlayer p) {
        for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(64),
                m -> m.getPersistentData().getBoolean("PossessedSukuna"))) m.discard(); // 消失，无掉落
    }

    private static void finish(ServerPlayer p) {
        p.getPersistentData().remove("SukunaPossessed");
        p.setCamera(p);
        restoreMode(p);
        Network.send(p.serverLevel(), new Network.Effect(CAMERA_LOCK, p.getId(), p.position(), Vec3.ZERO, 1f, 0, 0));
    }

    /** 恢复到"吃手指那一刻"的游戏模式（创造/生存/冒险），没有记录就回生存。 */
    private static void restoreMode(ServerPlayer p) {
        String prev = p.getPersistentData().getString("PossessedPrevMode");
        net.minecraft.world.level.GameType type = net.minecraft.world.level.GameType.SURVIVAL;
        if (!prev.isEmpty()) {
            type = net.minecraft.world.level.GameType.byName(prev);
            if (type == null) type = net.minecraft.world.level.GameType.SURVIVAL;
        }
        p.getPersistentData().remove("PossessedPrevMode");
        p.setGameMode(type);
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent e) {
        if (e.getEntity().getPersistentData().getBoolean("PossessedSukuna")) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        if (!(e.getEntity() instanceof ServerPlayer to)) return;
        if (e.getOriginal().getPersistentData().getBoolean("SukunaPossessed")) {
            to.getPersistentData().putBoolean("SukunaPossessed", true);
            to.getPersistentData().putString("PossessedPrevMode", e.getOriginal().getPersistentData().getString("PossessedPrevMode"));
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        boolean possessed = p.getPersistentData().getBoolean("SukunaPossessed");
        if (!possessed) {
            // 背包/存档里可能残留旁观模式：只要带过夺舍标记就拉回生存
            if (p.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SPECTATOR) {
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            }
            p.setCamera(p); restoreMode(p);
            return;
        }
        p.getPersistentData().remove("SukunaPossessed");
        p.setCamera(p);
        restoreMode(p);
        Network.send(p.serverLevel(), new Network.Effect(CAMERA_LOCK, p.getId(), p.position(), Vec3.ZERO, 1f, 0, 0));
    }

    private static Component button(String label, String command, int color, String hover) {
        return Component.literal("[ " + label + " ]").withStyle(Style.EMPTY.withColor(color).withBold(true)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    private FingerPossession() { }
}
