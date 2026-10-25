package cn.blockforge.generated.nnnnr1201forge2;

import character.SkillCaster;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * 玩家技能与 NPC 的统一转调层（第一步：玩家 100% 走原实现）。
 * <p>· 施术者是玩家 → 直接调用对应系统的**原有玩家方法**（JogoCombat / Uraume / BloodArts）；
 * · 施术者是生物 → 走 {@link NpcSkillCast}（内部已扣 NpcCurse、用 NpcCombat 归属伤害）。
 * 以后把玩家方法签名迁移到 {@link SkillCaster} 后，只需把生物分支换成同一方法即可，调用方不用改。</p>
 */
public final class PlayerSkillBridge {

    public static boolean cast(SkillCaster caster, String characterId, int skill) {
        LivingEntity body = caster.body();
        if (body instanceof ServerPlayer p) {
            return castPlayer(p, characterId, skill);
        }
        if (body instanceof Mob mob) {
            return NpcSkillCast.cast(NpcSkillCast.Caster.of(mob, characterId), skill, mob.getTarget());
        }
        return false;
    }

    /** 玩家分支：100% 转调玩家自己的实现，保证与玩家操作完全同款。 */
    private static boolean castPlayer(ServerPlayer p, String characterId, int skill) {
        switch (characterId) {
            case "jogo" -> {
                var def = character.CharacterService.selected(p);
                if (def == null) return false;
                character.CharacterCasting.cast(p, skill);   // 玩家角色框架原入口
                return true;
            }
            case "uraume" -> {
                Uraume.cast(p, skill);
                return true;
            }
            case "choso" -> {
                // 赤血操术的玩家入口是输入驱动：这里按"按下+释放"包一次完整施法
                TechniqueInput.receive(p, new TechniqueInput.Input(skill, 0, false, false, false));
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private PlayerSkillBridge() {}
}
