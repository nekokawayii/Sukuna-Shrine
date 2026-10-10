package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/** 每次开始招式独立抽签；返回本次招式的固定附加伤害。 */
public final class BossRevelation {
    public static float roll(Mob boss,String description){
        if(boss.getRandom().nextInt(GrowthConfig.REVEAL_DENOMINATOR.get())!=0)return 0;
        int bonus=RevelationRules.damage(boss.getRandom().nextInt(RevelationRules.TOTAL_WEIGHT));
        if(boss.level() instanceof ServerLevel level)for(var player:level.players())if(player.distanceToSqr(boss)<=128*128)player.sendSystemMessage(Component.literal("术式公开 · "+description+" · 本次强化：伤害额外+"+BalanceRules.number(BalanceRules.strength(bonus))+"，主要强化数量、持续、效果或范围"));
        SkillSounds.play(boss,"boss_reveal");return bonus;
    }
    public static String guardian(int skill){return switch(skill){case 1->"咒力拳：贴身连击，后退保持拳距之外";case 3->"径庭拳：延迟灵魂冲击，出拳前拉开24格";case 4->"超新星：追踪毒血，持续横移拉开距离";case 5->"赤鳞跃动：固定增加移速与拳速，拉开距离等强化结束";case 6->"网格斩：网格线持续伤害，快速离开64格范围";case 8->"净土领域：领域内必中且禁疗，展开前离开48格";case 9->"连续拳：瞬移后冲刺连打，前摇时向侧面闪避";case 10->"反转术式：持续回血，用远程攻击打断";case 11->"斩击无下限：近战弹开且远程减伤，等待防御结束";case 12->"百敛·穿血：直线持续伤害，横向移动或利用实心墙阻挡";default->"灵魂攻击：观察动作并拉开距离";};}
    public static String yuji(int skill){return switch(skill){case 1->"径庭拳：拳后延迟二次伤害，离开近战距离";case 2->"黑闪冲刺：蓄力后直线冲刺，向侧面躲避";case 3->"灵魂间隙：封锁术式，离开脚下9格预警";case 4->"残影围攻：分身突袭，击中本体可打断";case 5->"灵魂连打：近战叠加僵直，前摇时拉开距离";case 6->"岩手领域：领域内必中，展开前离开48格";default->"灵魂斩：延迟补刀，看到剪切线立即移动";};}
    private BossRevelation(){}
}
