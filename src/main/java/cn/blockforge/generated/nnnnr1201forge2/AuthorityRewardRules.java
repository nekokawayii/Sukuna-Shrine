package cn.blockforge.generated.nnnnr1201forge2;

/** 高位通关奖励的交付判定；纯逻辑，可在无游戏环境回归。 */
public final class AuthorityRewardRules {
    /** 背包满时的补发间隔（游戏刻）。 */
    public static final int RETRY_TICKS=20;

    /** 击碎阶段才算通关：护盾耗尽（FAILED）与中途退出都不算。 */
    public static boolean isVictory(int phase){return phase==ObserverBattleRules.SHATTER;}

    /** 需要把「权限」放进背包：通关了、没吃下、没真正交到手、身上也没有。 */
    public static boolean shouldPlace(boolean cleared,boolean unlocked,boolean delivered,boolean holding){
        return cleared&&!unlocked&&!delivered&&!holding;
    }

    /** 已吃下或已在背包：视为交到手，不再重复发放（旧存档的丢失补发也走这条）。 */
    public static boolean shouldMarkDelivered(boolean unlocked,boolean holding){return unlocked||holding;}

    private AuthorityRewardRules(){}
}
