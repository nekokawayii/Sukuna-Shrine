package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import static cn.blockforge.generated.nnnnr1201forge2.ObserverBattleRules.*;

/** 只按永久解锁状态复制术式，不把玩家临时缺咒力或冷却误判成未拥有。 */
public final class ObserverLoadout {
    public static List<Move> copy(ServerPlayer p){
        List<Move> out=new ArrayList<>();
        if(Journey.unlocked(p)){
            String[] base=VoidJourney.evolved(p)?VoidCombat.NAMES:Curse.awakened(p)?Skills.AWAKENED:Skills.NAMES;
            int[] cd={0,60,40,60,100,1800,1800};
            for(int i=0;i<base.length;i++){
                if(i==2&&Curse.fingers(p)<5||i==6&&!SoulJourney.evolved(p)&&!VoidJourney.evolved(p))continue;
                String name=i==5&&SoulJourney.evolved(p)&&!VoidJourney.evolved(p)?"领域展开 · 灵魂御厨子":base[i];
                out.add(new Move(name,i==5?DOMAIN:SLASH,cd[i]));
            }
            add(out,SLASH,60,"解 · 形状斩","解 · 纵心","解 · 旋转环斩","解 · 滞留");
            add(out,MELEE,100,"解 · 后翻","解 · 飞踢","解 · 滑铲","解 · 冲拳","解 · 跪地斩","解 · 落地追击");
            // 终式通过现有术式积累前置，不是另一个永久解锁位。
            add(out,WORLD,3600,FinaleRules.NAME);
            if(Curse.fingers(p)>=5){add(out,SLASH,120,"蜘蛛网","多重斩击","捌 · 十字飞行","捌 · 滞留","简易领域 · 捌");add(out,MELEE,100,"咒力附拳");}
            if(Curse.fingers(p)>=15)add(out,SUPPORT,600,"反转术式","修复熔断");
            if(Curse.awakened(p)){add(out,SLASH,100,"十字斩击","解 · 剑气","斩击墙","龙之昴星");add(out,WORLD,2400,"解 · 世界斩");}
            if(VoidJourney.evolved(p))add(out,SLASH,600,"灶 · 开",FurnaceMax.NAME);
            if(DomainAmplification.unlocked(p))add(out,SUPPORT,300,"领域展延");
            if(Journey.nine(p))add(out,SUPPORT,300,"九相轮转");
            if(GuardianCombat.awakened(p)){
                int[] cooldown={20,60,160,500,120,3600,180,240,1800,400};
                for(int i=0;i<GuardianPlayerSkills.COUNT;i++)out.add(new Move(GuardianPlayerSkills.NAMES[i],switch(i){case 0,1,6->MELEE;case 5->DOMAIN;case 3,7,8->SUPPORT;default->SLASH;},cooldown[i]));
            }
        }
        if(Uraume.unlocked(p))add(out,SLASH,200,"霜凪","直瀑");
        if(GojoPlayer.unlocked(p))for(int i=0;i<GojoPlayer.COUNT;i++)out.add(new Move(GojoPlayer.nameOf(i),i==7?DOMAIN:i==5||i==6?MELEE:SLASH,GojoPlayer.COOLDOWN[i]));
        add(out,SLASH,180,"百敛穿血");
        return List.copyOf(out);
    }
    private static void add(List<Move> out,int style,int cooldown,String... names){for(String name:names)out.add(new Move(name,style,cooldown));}
    private ObserverLoadout(){}
}
