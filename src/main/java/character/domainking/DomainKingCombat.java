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

    public static void register() {
        for (int id : DomainKingSkills.all().stream().mapToInt(CharacterSkill::id).toArray()) {
            CharacterCasting.registerEffect("domainking", id, DomainKingCombat::cast);
        }
    }

    public static void cast(ServerPlayer p, CharacterSkill skill, int rank) {
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
            case DomainKingSkills.SHRINE -> {         // 伏魔御厨子：宿傩线原实现（神龛/必中斩击）
                cn.blockforge.generated.nnnnr1201forge2.Skills.cast(p, 5);
                p.sendSystemMessage(Component.literal("领域展开 · 伏魔御厨子"));
                return;
            }
            case DomainKingSkills.BLOOD -> {          // 血之领域·胎海：赤血线原实现
                cn.blockforge.generated.nnnnr1201forge2.BloodDomain.open(p, p.position(), false);
                p.sendSystemMessage(Component.literal("领域展开 · 血之领域·胎海"));
                return;
            }
            case DomainKingSkills.YUTA -> {           // 真赝相爱：乙骨线原实现
                character.yuta.YutaDomain.open(p);
                p.sendSystemMessage(Component.literal("领域展开 · 真赝相爱"));
                return;
            }
            default -> { }                             // 简易领域·捌：走下面的通用实现
        }
        long end = p.level().getGameTime() + 600;   // 30 秒
        FIELDS.removeIf(f -> f.owner() == p);
        FIELDS.add(new Field(p, name, color, radius, end, (float) skill.damage(), 1));
        if (p.level() instanceof ServerLevel level) {
            level.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.4f, 0.6f);
            // 展开瞬间：中心光爆 + 一圈火柱，保证玩家肉眼可见
            Network.send(level, new Network.Effect(character.jogo.JogoFx.BUG_BURST, p.getId(), p.position(),
                Vec3.ZERO, (float) radius * 0.25f, 20, 0));
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI * 2 / 12;
                Vec3 at = p.position().add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
                Network.send(level, new Network.Effect(character.jogo.JogoFx.PILLAR, 0, at, Vec3.ZERO, 2f, 14, 0));
            }
        }
        p.sendSystemMessage(Component.literal("领域展开 · " + name));
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

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || FIELDS.isEmpty()) return;
        for (var it = FIELDS.iterator(); it.hasNext(); ) {
            Field f = it.next();
            ServerPlayer owner = f.owner();
            if (owner == null || !owner.isAlive() || !(owner.level() instanceof ServerLevel level)) { it.remove(); continue; }
            long now = level.getGameTime();
            if (now >= f.end()) {
                it.remove();
                if (f.name().equals("盖棺铁围山")) character.jogo.JogoTerrain.restore(owner);
                owner.sendSystemMessage(Component.literal("领域 · " + f.name() + " 已收起"));
                continue;
            }
            if (now % 10 == 0) {
                // 每 0.5 秒一圈脉冲火柱，避免"只有伤害没有画面"
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI * 2 / 8 + now * 0.02;
                    Vec3 at = owner.position().add(Math.cos(a) * f.radius(), 0, Math.sin(a) * f.radius());
                    Network.send(level, new Network.Effect(character.jogo.JogoFx.PILLAR, 0, at, Vec3.ZERO, 1.6f, 10, 0));
                }
            }
            if (now % 10 != 0) continue;
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(owner.position(), owner.position()).inflate(f.radius()),
                    t -> t != owner && t.isAlive() && t.position().distanceToSqr(owner.position()) <= f.radius() * f.radius())) {
                t.hurt(owner.damageSources().indirectMagic(owner, owner), f.dps());
                if (f.slowAmp() > 0) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, f.slowAmp(), false, false));
            }
        }
    }

    private DomainKingCombat() {}
}
