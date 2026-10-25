package character.domainking;

import character.CharacterCasting;
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
        long end = p.level().getGameTime() + 600;   // 30 秒
        FIELDS.removeIf(f -> f.owner() == p);
        FIELDS.add(new Field(p, name, color, radius, end, (float) skill.damage(), 1));
        if (p.level() instanceof ServerLevel level) {
            level.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.4f, 0.6f);
        }
        p.sendSystemMessage(Component.literal("领域展开 · " + name));
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
                owner.sendSystemMessage(Component.literal("领域 · " + f.name() + " 已收起"));
                continue;
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
