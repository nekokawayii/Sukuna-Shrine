package character.toji;

import cn.blockforge.generated.nnnnr1201forge2.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import java.util.List;

/** 自动选择玩家连招，不再循环旧版突进/重击/跃身三招，也不向客户端伪造按键包。 */
public final class TojiNpcCombat {
    /** NPC 甚尔拿天逆蛑：每 5 秒放一次右键「术式切断」。 */
    public static final int SPEAR_INTERVAL = 100;
    private static final String SPEAR_NEXT = "TojiSpearNext";

    /** 到点了就斩一次；返回 true 表示这一轮真的放了。 */
    public static boolean spearTick(NpcBridge.NpcActor actor) {
        if (!actor.characterId.equals("toji") || !(actor.mob instanceof Mob mob) || mob.isNoAi() || !mob.isAlive()) return false;
        if (!mob.getMainHandItem().is(Content.TENKA_SPEAR.get())) return false;
        long now = mob.level().getGameTime();
        var data = Curse.data(actor);
        if (!data.contains(SPEAR_NEXT)) { data.putLong(SPEAR_NEXT, now + SPEAR_INTERVAL); return false; }
        if (now < data.getLong(SPEAR_NEXT)) return false;
        data.putLong(SPEAR_NEXT, now + SPEAR_INTERVAL);
        TenkaSpearItem.severAround(actor);
        mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return true;
    }

    public static void tick(NpcBridge.NpcActor actor, LivingEntity target, List<Integer> allowed) {
        spearTick(actor);
        TojiQte.clearExpired(actor,actor.level().getGameTime());
        if(Curse.active(actor,"TojiNpcComboCooldown") || !TojiCombat.allowed(actor))return;
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
            || Curse.active(actor,"TojiNpcComboCooldown") || !TojiQte.inCombo(actor))return false;
        double cost=combo==20?40:combo>=10?24:16;
        if(TojiCombat.stamina(actor)<cost)return false;
        if(!TojiCombos.automatic(actor,combo))return false;
        Curse.data(actor).putDouble("TojiStamina",TojiCombat.stamina(actor)-cost);
        Curse.status(actor,"TojiRecoveryDelay",30);
        Curse.status(actor,"TojiNpcComboCooldown",40); // 给对手收招、恢复与反击机会。
        TojiQte.resetSequence(actor);
        mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return true;
    }
    private TojiNpcCombat() { }
}
