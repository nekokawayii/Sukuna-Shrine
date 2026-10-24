package cn.blockforge.generated.nnnnr1201forge2;

import java.util.UUID;

/** 世界剧情可以共用，碎片、亲自读信和战斗入口条件始终按玩家分别判断。 */
public final class BeyondProgressRules {
    public static boolean blocksFragment(UUID player,UUID dropOwner){return player.equals(dropOwner);}
    public static boolean needsFragment(boolean eligible,boolean authority,boolean carrying,boolean ownDrop){
        return eligible&&!authority&&!carrying&&!ownDrop;
    }
    public static boolean canReceiveNote(boolean carrying,boolean atHome,boolean worldSceneDone,boolean personalSceneSeen,boolean read){
        return carrying&&atHome&&(worldSceneDone||personalSceneSeen)&&!read;
    }
    public static boolean canStart(boolean inArena,boolean carrying,boolean personallyRead,boolean active){
        return inArena&&carrying&&personallyRead&&!active;
    }
    private BeyondProgressRules(){}
}
