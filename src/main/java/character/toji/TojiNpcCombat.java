package character.toji;

import cn.blockforge.generated.nnnnr1201forge2.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import java.util.List;

/**
 * NPC 甚尔的战斗：自动搓玩家连招，不向客户端伪造按键包。
 * <p>r215 起：①无连招冷却、不扣体术，只要姿势窗口在就一直搓；
 * ②每 5 秒用手里天逆蛑放一次右键「术式切断」；③时不时甩一条装着天逆蛑的万里之锁把目标缠住。
 */
public final class TojiNpcCombat {
    /** 天逆蛑右键「术式切断」间隔：5 秒。 */
    public static final int SPEAR_INTERVAL = 100;
    /** 万里之锁间隔：8 秒（"时不时"用一下）。 */
    public static final int CHAIN_INTERVAL = 160;
    /** 缠住多久后自动收链：5 秒。 */
    public static final int CHAIN_HOLD = 100;
    private static final String SPEAR_NEXT = "TojiSpearNext", CHAIN_NEXT = "TojiChainNext";

    /** 只在「是甚尔 NPC 且能动手」时返回它的 Mob 本体。 */
    private static Mob ready(NpcBridge.NpcActor actor) {
        return actor.characterId.equals("toji") && actor.mob instanceof Mob m && !m.isNoAi() && m.isAlive() ? m : null;
    }

    /** 到点了就斩一次；返回 true 表示这一轮真的放了。 */
    public static boolean spearTick(NpcBridge.NpcActor actor) {
        Mob mob = ready(actor);
        if (mob == null) return false;
        if (!mob.getMainHandItem().is(Content.TENKA_SPEAR.get())) return false;
        long now = mob.level().getGameTime();
        var data = Curse.data(actor);
        if (!data.contains(SPEAR_NEXT)) { data.putLong(SPEAR_NEXT, now + SPEAR_INTERVAL); return false; }
        if (now < data.getLong(SPEAR_NEXT)) return false;
        data.putLong(SPEAR_NEXT, now + SPEAR_INTERVAL);
        TenkaSpearItem.severAround(actor);
        mob.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    /** 时不时甩一条「带着天逆蛑的万里之锁」把目标缠住（锁定 50 伤 + 每秒 2 点 + 持续封术式）。 */
    public static boolean chainTick(NpcBridge.NpcActor actor, LivingEntity target) {
        Mob owner = ready(actor);
        if (owner == null || target == null || target == owner) return false;
        Mob mob = owner;
        long now = mob.level().getGameTime();
        var data = Curse.data(actor);
        if (!data.contains(CHAIN_NEXT)) { data.putLong(CHAIN_NEXT, now + CHAIN_INTERVAL); return false; }
        if (now < data.getLong(CHAIN_NEXT) || ThousandMileChain.active(actor)) return false;
        if (mob.distanceToSqr(target) > 24 * 24 || !mob.hasLineOfSight(target)) return false;
        if (!ThousandMileChain.npcThrow(actor, target, true, CHAIN_HOLD)) return false;
        data.putLong(CHAIN_NEXT, now + CHAIN_INTERVAL);
        mob.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    public static void tick(NpcBridge.NpcActor actor, LivingEntity target, List<Integer> allowed) {
        spearTick(actor);
        chainTick(actor, target);
        TojiQte.clearExpired(actor,actor.level().getGameTime());
        if(!TojiCombat.allowed(actor))return;
        TojiQte.autoWindow(actor,target);
        if(!TojiQte.inCombo(actor))return;
        int start=Math.floorMod(Curse.data(actor).getInt("TojiNpcCursor"),Math.max(1,allowed.size()));
        // 有弹幕先弹反，攒满三次完美闪避后才允许处决。
        if(allowed.contains(16)&&cast(actor,16))return;
        if(allowed.contains(20)&&TojiQte.perfectCount(actor)>=TojiQte.PERFECT_PER_EXECUTE&&cast(actor,20))return;
        for(int i=0;i<allowed.size();i++){
            int index=(start+i)%allowed.size();int combo=allowed.get(index);
            if(combo==16||combo==20)continue;
            if(cast(actor,combo)){Curse.data(actor).putInt("TojiNpcCursor",(index+1)%allowed.size());return;}
        }
    }
    public static boolean cast(NpcBridge.NpcActor actor,int combo) {
        if(!actor.characterId.equals("toji") || !(actor.mob instanceof Mob mob) || mob.isNoAi()
            || !actor.isAlive() || !TojiCombat.allowed(actor) || DomainClash.stunned(mob)
            || !TojiQte.inCombo(actor))return false;
        // r215：用户要求 NPC 甚尔「无 CD 搓连招」——不再扣体术、不再写连招冷却，靠 tick 的 10 刻节流就是上限。
        if(!TojiCombos.automatic(actor,combo))return false;
        TojiQte.resetSequence(actor);
        mob.swing(InteractionHand.MAIN_HAND);
        return true;
    }
    private TojiNpcCombat() { }
}
