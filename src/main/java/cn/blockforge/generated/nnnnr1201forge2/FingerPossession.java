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
            p.displayClientMessage(Component.literal("诅咒之力灌入体内：咒力猛然翻倍，但胃里翻江倒海、眼前漆黑、四肢沉重。"), true);
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
        p.setCamera(npc);                 // 看宿傩的视角，而不是旁观模式
        p.setInvisible(true);             // 自己的身体留在原地，但不显眼
        p.setDeltaMovement(0, 0, 0);
        p.hurtMarked = true;
        Network.send(level, new Network.Effect(CAMERA_LOCK, p.getId(), at, Vec3.ZERO, 1f, ACTIVE_TICKS, 0));
        var definition = character.CharacterService.selected(p);
        boolean yuji = definition != null && "yuji".equals(definition.id());
        if (yuji) p.sendSystemMessage(Component.literal("宿傩咧开嘴：「……又是你？这副身体，我倒是熟了。」").withStyle(Style.EMPTY.withColor(0xFF8A00)));
        else p.sendSystemMessage(Component.literal("手指在喉间化作一声低笑——「这具身体，归我了。」").withStyle(Style.EMPTY.withColor(0xFF8A00)));
        if (yuji) {
            p.sendSystemMessage(button("继续", "/sukuna possess keep", 0xFFFFD27A, "让宿傩继续使用这具身体"));
            p.sendSystemMessage(button("夺回", "/sukuna possess back", 0xFFFF4A4A, "夺回控制权，宿傩消失（无掉落）"));
        } else {
            p.sendSystemMessage(button("自杀", "/kill @s", 0xFFFF4A4A, "放弃这具身体，重生后恢复生存（宿傩留下）"));
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
    public static void keep(ServerPlayer p) {
        p.sendSystemMessage(Component.literal("你放任宿傩继续支配这具身体。").withStyle(Style.EMPTY.withColor(0xFF8A00)));
    }

    public static void back(ServerPlayer p) {
        removeHost(p);
        finish(p);
        p.sendSystemMessage(Component.literal("你夺回了身体的控制权，宿傩的身影散去。").withStyle(Style.EMPTY.withColor(0xFF7FD7FF)));
    }

    public static void returnTo(ServerPlayer p) {
        removeHost(p);
        finish(p);
        p.sendSystemMessage(Component.literal("「下次再陪你玩。」宿傩退去。").withStyle(Style.EMPTY.withColor(0xFF8A00)));
    }

    private static void removeHost(ServerPlayer p) {
        for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(64),
                m -> m.getPersistentData().getBoolean("PossessedSukuna"))) m.discard(); // 消失，无掉落
    }

    private static void finish(ServerPlayer p) {
        p.getPersistentData().remove("SukunaPossessed");
        p.setCamera(p);
        p.setInvisible(false);
        Network.send(p.serverLevel(), new Network.Effect(CAMERA_LOCK, p.getId(), p.position(), Vec3.ZERO, 1f, 0, 0));
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent e) {
        if (e.getEntity().getPersistentData().getBoolean("PossessedSukuna")) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        if (!(e.getEntity() instanceof ServerPlayer to)) return;
        if (e.getOriginal().getPersistentData().getBoolean("SukunaPossessed")) to.getPersistentData().putBoolean("SukunaPossessed", true);
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
            p.setCamera(p); p.setInvisible(false);
            return;
        }
        p.getPersistentData().remove("SukunaPossessed");
        p.setCamera(p);
        p.setInvisible(false);
        if (p.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.SURVIVAL)
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        Network.send(p.serverLevel(), new Network.Effect(CAMERA_LOCK, p.getId(), p.position(), Vec3.ZERO, 1f, 0, 0));
    }

    private static Component button(String label, String command, int color, String hover) {
        return Component.literal("[ " + label + " ]").withStyle(Style.EMPTY.withColor(color).withBold(true)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    private FingerPossession() { }
}
