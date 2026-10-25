package character.domainking;

import character.CharacterCasting;
import net.minecraft.world.phys.Vec3;
import cn.blockforge.generated.nnnnr1201forge2.Network;
import character.CharacterSkill;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 领域王的技能效果：每种领域各自展开 30 秒，界内敌人持续受伤。 */
public final class DomainKingCombat {
    private record Field(ServerPlayer owner, String name, int color, double radius, long end, float dps, int slowAmp) {}
    private static final List<Field> FIELDS = new ArrayList<>();
    private static final java.util.Map<UUID,Integer> ACTIVE = new java.util.HashMap<>();

    public static void register() {
        for (int id : DomainKingSkills.all().stream().mapToInt(CharacterSkill::id).toArray()) {
            CharacterCasting.registerEffect("domainking", id, DomainKingCombat::cast);
        }
    }

    public static void cast(ServerPlayer p, CharacterSkill skill, int rank) {
        // 再次点击同一领域 = 立即收起（不消耗冷却）
        Integer active = ACTIVE.get(p.getUUID());
        if (active != null && active == skill.id()) {
            close(p, active);
            ACTIVE.remove(p.getUUID());
            p.sendSystemMessage(Component.literal("领域 · " + skill.name() + " 已收起"));
            return;
        }
        if (active != null) { close(p, active); ACTIVE.remove(p.getUUID()); }
        ACTIVE.put(p.getUUID(), skill.id());
        String name = skill.name();
        int color = skill.color();
        double radius = switch (skill.id()) {
            case DomainKingSkills.SIMPLE -> 8;
            default -> 24;
        };
        // ---- 直接转调各条线已有的领域实现（真实效果）----
        switch (skill.id()) {
            case DomainKingSkills.JOGO_DOMAIN -> {   // 盖棺铁围山：真实方块清场 + 岩浆地板
                character.jogo.JogoTerrain.open(p, p.position(), 27);
                Network.send((ServerLevel) p.level(), new Network.Effect(character.jogo.JogoFx.DOMAIN, p.getId(),
                    p.position(), Vec3.ZERO, 27f, 600, 0));
                p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.6f, 0.45f);
                p.sendSystemMessage(Component.literal("领域展开 · 盖棺铁围山（地形改造）"));
                FIELDS.add(new Field(p, name, color, 27, p.level().getGameTime() + 600, 0f, 0));
                return;
            }
            case DomainKingSkills.VOID -> {           // 无量空处：五条线原实现
                cn.blockforge.generated.nnnnr1201forge2.GojoCombat.openVoidDomain(p);
                p.sendSystemMessage(Component.literal("领域展开 · 无量空处"));
                return;
            }
            case DomainKingSkills.SHRINE -> {         // 伏魔御厨子：绕过宿傩阶段门禁，直接调原 openDomain
                boolean opened = openSukunaDomain(p);
                if (!opened) p.sendSystemMessage(Component.literal("伏魔御厨子打开失败（见日志）"));
                return;
            }
            case DomainKingSkills.BLOOD -> {          // 血之领域·胎海：赤血线原实现
                cn.blockforge.generated.nnnnr1201forge2.BloodDomain.open(p, p.position(), false);
                p.sendSystemMessage(Component.literal("领域展开 · 血之领域·胎海"));
                return;
            }
            case DomainKingSkills.YUTA -> {           // 真赝相爱：乙骨线原实现
                character.yuta.YutaDomain.open(p);
                boolean ok = character.yuta.YutaDomain.active(p);
                com.mojang.logging.LogUtils.getLogger().info("[领域王] 真赝相爱 open 后 active={}", ok);
                p.sendSystemMessage(Component.literal(ok ? "领域展开 · 真赝相爱" : "真赝相爱未展开（日志已记录）"));
                return;
            }
            case DomainKingSkills.SELF_EMBRYO -> {     // 自闭圆盾裹：真人领域
                SelfEmbryoDomain.open(p);
                p.sendSystemMessage(Component.literal("领域展开 · 自闭圆盾裹"));
                return;
            }
            case DomainKingSkills.SIMPLE -> {          // 简易领域·捌：原实现
                cn.blockforge.generated.nnnnr1201forge2.SimpleDomain.start(p);
                p.sendSystemMessage(Component.literal("领域展开 · 简易领域·捌"));
                return;
            }
            default -> { }
        }
    }

    /** 咒力无限：领域王每刻把咒力补满（上限 1,000,000，HUD 显示 ∞）。 */
    @SubscribeEvent public static void refill(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        var d = character.CharacterService.selected(p);
        if (d == null || !d.id().equals("domainking")) return;
        if (p.tickCount % 10 != 0) return;
        cn.blockforge.generated.nnnnr1201forge2.Curse.addRaw(p, 1000000);
        cn.blockforge.generated.nnnnr1201forge2.Curse.sync(p);
    }

    /** 只负责盖棺铁围山的地形按时还原；其它领域的伤害/特效全由各条线自己的系统处理。 */
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || FIELDS.isEmpty()) return;
        for (var it = FIELDS.iterator(); it.hasNext(); ) {
            Field f = it.next();
            ServerPlayer owner = f.owner();
            if (owner == null || !owner.isAlive() || !(owner.level() instanceof ServerLevel level)) { it.remove(); continue; }
            if (level.getGameTime() >= f.end()) {
                it.remove();
                if (f.name().equals("盖棺铁围山")) character.jogo.JogoTerrain.restore(owner);
            }
        }
    }

    /** 该技能是否是玩家当前展开中的领域。 */
    public static boolean isActive(ServerPlayer p, int skill) {
        Integer id = ACTIVE.get(p.getUUID());
        return id != null && id == skill;
    }

    /** 从施法入口直接收起（绕开"领域维持中不能施法"的门禁）。 */
    public static boolean toggleClose(ServerPlayer p, int skill) {
        Integer id = ACTIVE.get(p.getUUID());
        if (id == null || id != skill) return false;
        close(p, id);
        ACTIVE.remove(p.getUUID());
        p.sendSystemMessage(Component.literal("领域 · " + skill + " 已收起"));
        return true;
    }

    /** 按技能号收起对应的原实现领域。 */
    private static void close(ServerPlayer p, int id) {
        switch (id) {
            case DomainKingSkills.JOGO_DOMAIN -> {
                character.jogo.JogoTerrain.restore(p);
                Network.send((ServerLevel) p.level(), new Network.Effect(character.jogo.JogoFx.DOMAIN_END,
                    p.getId(), p.position(), Vec3.ZERO, 0, 0, 0));
            }
            case DomainKingSkills.VOID -> cn.blockforge.generated.nnnnr1201forge2.GojoCombat.closeDomain(p);
            case DomainKingSkills.SHRINE -> cn.blockforge.generated.nnnnr1201forge2.Skills.closeDomain(p);
            case DomainKingSkills.BLOOD -> cn.blockforge.generated.nnnnr1201forge2.BloodDomain.close(p);
            case DomainKingSkills.YUTA -> character.yuta.YutaDomain.close(p, "手动收起");
            case DomainKingSkills.SELF_EMBRYO -> SelfEmbryoDomain.close(p, "手动收起");
            case DomainKingSkills.SIMPLE -> cn.blockforge.generated.nnnnr1201forge2.SimpleDomain.release(p, "手动收起");
            default -> { }
        }
        FIELDS.removeIf(f -> f.owner() == p);
    }

    /** 直接调用宿傩线的私有 openDomain（跳过 Journey.unlocked / 结印时间轴等宿傩专属门禁）。 */
    private static boolean openSukunaDomain(ServerPlayer p) {
        try {
            var m = cn.blockforge.generated.nnnnr1201forge2.Skills.class.getDeclaredMethod("openDomain", ServerPlayer.class);
            m.setAccessible(true);
            m.invoke(null, p);
            boolean ok = cn.blockforge.generated.nnnnr1201forge2.Skills.domainActive(p);
            com.mojang.logging.LogUtils.getLogger().info("[领域王] 御厨子 openDomain 反射调用完成 domainActive={}", ok);
            p.sendSystemMessage(Component.literal(ok ? "领域展开 · 伏魔御厨子" : "伏魔御厨子 openDomain 已调用但未生效"));
            return ok;
        } catch (Throwable t) {
            com.mojang.logging.LogUtils.getLogger().error("[领域王] 御厨子 openDomain 反射失败", t);
            p.sendSystemMessage(Component.literal("伏魔御厨子调用失败：" + t.getClass().getSimpleName()));
            return false;
        }
    }

    private DomainKingCombat() {}
}
