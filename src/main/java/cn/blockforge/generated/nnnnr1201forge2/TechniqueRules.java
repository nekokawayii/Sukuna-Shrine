package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/** 咒术选择：开局《咒术选择书》定流派，之后再吃一次书可以重选（有冷却）。
 *  <p>选择只做两件事：①写 NBT {@code Technique}；②按流派授予/清除已有的解锁旗标。
 *  轮盘、咒力条等表现全部由这些旗标和 {@code Technique} 自己推导，不再各写一套。</p>
 *  <p>伏黑甚尔（天宇暴君）：没有轮盘、没有咒力，咒力条换成「体术条」（上限 100、普攻命中回 10）。</p> */
public final class TechniqueRules {
    public static final int NONE=-1,LIMITLESS=0,ICE=1,BLOOD=2,MELEE=3,SHRINE=4,TOJI=5,COUNT=6;
    public static final String KEY="Technique",STAMINA="Stamina",SELECTED_AT="TechniqueSelectedAt";
    public static final String[] NAMES={"无下限","冰凝咒法","赤血操术","体术","伏魔御厨子","天宇暴君"};
    public static final String[] OWNERS={"五条悟","里梅","咒胎九相图","虎杖悠仁","两面宿傩","伏黑甚尔"};
    public static final String[] DESC={
        "苍、赫、茈、瞬移与无量空处；屏幕右侧独立轮盘。",
        "霜凪与直瀑，冰鱼越吃越强；轮盘左侧独立冰轮盘。",
        "内外双环 19 招，胎海展开后中央出现死胎星；轮盘翻到赤血页。",
        "径庭拳、八段连击与拳风；中央轮盘保留。",
        "解、捌、灶、领域与成长任务；中央轮盘与分支全开。",
        "零咒力：没有轮盘，咒力条变为体术条；普攻命中回体术。"};
    /** 体术条：上限 100，普攻命中回 10。 */
    public static final float STAMINA_MAX=100,STAMINA_PER_HIT=10;
    /** 重选冷却：用户要求"再吃一本就能换"，所以为 0（保留字段与判定，将来要加冷却只改这里）。 */
    public static final int RESELECT_COOLDOWN=0;

    public static boolean valid(int technique){return technique>=0&&technique<COUNT;}
    public static int current(Player p){var data=Curse.data(p);return data.contains(KEY)?data.getInt(KEY):NONE;}
    public static boolean is(Player p,int technique){return current(p)==technique;}
    /** 已经选过任何一种咒式。所有"能不能施术/要不要显示"的总闸都应该把它算进去。 */
    public static boolean selected(Player p){return valid(current(p));}
    public static boolean toji(Player p){return is(p,TOJI);}
    public static boolean wheel(Player p){return !toji(p);}
    public static String name(Player p){int t=current(p);return valid(t)?NAMES[t]:"未选择";}

    /** 体术条。 */
    public static float stamina(Player p){return Math.max(0,Math.min(STAMINA_MAX,Curse.data(p).getFloat(STAMINA)));}
    public static void addStamina(ServerPlayer p,float amount){
        if(!toji(p)||amount<=0)return;
        float value=Math.min(STAMINA_MAX,stamina(p)+amount);
        if(value==stamina(p))return;
        Curse.data(p).putFloat(STAMINA,value);
        Curse.sync(p);
    }
    public static boolean spendStamina(ServerPlayer p,float amount){
        if(!toji(p))return false;
        float value=stamina(p);
        if(value<amount)return false;
        Curse.data(p).putFloat(STAMINA,value-amount);
        Curse.sync(p);
        return true;
    }

    /** 选择/重选。返回 false = 拒绝（未解锁、冷却中、编号非法）。 */
    public static boolean select(ServerPlayer p,int technique){
        if(!valid(technique))return false;
        long now=p.level().getGameTime();
        var data=Curse.data(p);
        boolean had=data.getInt(KEY)!=NONE||data.contains(SELECTED_AT);
        if(had){
            long at=data.getLong(SELECTED_AT);
            long left=RESELECT_COOLDOWN-(now-at);
            if(RESELECT_COOLDOWN>0&&at>0&&left>0){p.sendSystemMessage(Component.literal("咒术选择书冷却中 · 还需 "+((left+19)/20)+" 秒"));return false;}
        }
        data.putInt(KEY,technique);
        data.putLong(SELECTED_AT,now);
        grant(p,technique);
        data.putFloat(STAMINA,technique==TOJI?STAMINA_MAX:0);
        p.level().playSound(null,p.blockPosition(),SoundEvents.PLAYER_LEVELUP,SoundSource.PLAYERS,0.9f,1.25f);
        p.sendSystemMessage(Component.literal("术式已定 · "+NAMES[technique]+"（"+OWNERS[technique]+"）"+DESC[technique]));
        com.mojang.logging.LogUtils.getLogger().info("[流派] {} 选择了 {}（{}）",p.getScoreboardName(),NAMES[technique],OWNERS[technique]);
        Curse.sync(p);
        return true;
    }

    /** 按流派授予/清除旗标：六个流派互斥，切过去会把别的流派的解锁清掉。 */
    private static void grant(ServerPlayer p,int technique){
        var data=Curse.data(p);
        boolean shrine=technique==SHRINE,melee=technique==MELEE;
        data.putBoolean("Memory",shrine||melee);
        data.putBoolean("Awakened",shrine);
        data.putBoolean("GojoMemory",technique==LIMITLESS);
        data.putBoolean("UraumeMemory",technique==ICE);
        data.putBoolean("Nine",technique==BLOOD);
        if(technique==TOJI){
            data.remove("Fist");
            data.putFloat(STAMINA,STAMINA_MAX);
        }
    }

    /** 描述一行（聊天/书本用）。 */
    public static String line(int technique){return valid(technique)?NAMES[technique]+"｜"+OWNERS[technique]+"："+DESC[technique]:"";}

    private TechniqueRules(){}
}
