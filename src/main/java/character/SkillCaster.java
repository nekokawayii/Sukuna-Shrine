package character;

import cn.blockforge.generated.nnnnr1201forge2.Curse;
import cn.blockforge.generated.nnnnr1201forge2.NpcCombat;
import cn.blockforge.generated.nnnnr1201forge2.NpcCurse;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 「施术者」抽象：玩家与生物共用同一套技能入口。
 * <p>玩家实现走 {@link Curse}；生物实现走 {@code NpcCurse}（咒力池）与 {@code NpcCombat}（伤害归属）。
 * 技能实现只依赖本接口，不再直接依赖 {@link ServerPlayer}。</p>
 */
public interface SkillCaster {
    LivingEntity body();
    Level level();
    Vec3 eye();
    String character();
    int rank();
    /** 扣咒力；不足返回 false。 */
    boolean spend(double amount);
    /** 造成伤害，来源归属本施术者。 */
    boolean hurt(LivingEntity target, float amount);
    /** 聊天/动作栏提示（生物默认静默）。 */
    void message(String text);
    default boolean isPlayer() { return body() instanceof ServerPlayer; }

    static SkillCaster of(LivingEntity entity) {
        return entity instanceof ServerPlayer player ? new PlayerCaster(player) : new MobCaster(entity);
    }

    /** 玩家施术者：咒力与伤害都走既有玩家系统。 */
    record PlayerCaster(ServerPlayer player) implements SkillCaster {
        @Override public LivingEntity body() { return player; }
        @Override public Level level() { return player.level(); }
        @Override public Vec3 eye() { return player.getEyePosition(); }
        @Override public String character() {
            var d = CharacterService.selected(player);
            return d == null ? "" : d.id();
        }
        @Override public int rank() { return CharacterService.rank(player); }
        @Override public boolean spend(double amount) { return Curse.spend(player, amount); }
        @Override public boolean hurt(LivingEntity target, float amount) {
            return cn.blockforge.generated.nnnnr1201forge2.CombatSafety.hurt(player, target, player.damageSources().indirectMagic(player, player), amount);
        }
        @Override public void message(String text) { player.displayClientMessage(Component.literal(text), true); }
    }

    /** 生物施术者：咒力池 + 伤害兜底，全部归属这只生物。 */
    record MobCaster(LivingEntity entity) implements SkillCaster {
        @Override public LivingEntity body() { return entity; }
        @Override public Level level() { return entity.level(); }
        @Override public Vec3 eye() { return entity.getEyePosition(); }
        @Override public String character() { return ""; }
        @Override public int rank() { return 1; }
        @Override public boolean spend(double amount) { return NpcCurse.spend(entity, amount); }
        @Override public boolean hurt(LivingEntity target, float amount) {
            return entity instanceof Mob mob && NpcCombat.hurt(mob, target, amount);
        }
        @Override public void message(String text) { }
    }
}
