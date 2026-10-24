package cn.blockforge.generated.nnnnr1201forge2;

import java.util.HashSet;
import java.util.Set;

/** 无游戏依赖的动作保障规则；主绘制、漏画补绘和新动作入口共用。 */
public final class MotionSafety {
    public static double age(double age,double partial,double duration){
        if(!Double.isFinite(age)||!Double.isFinite(partial)||!Double.isFinite(duration)||duration<=0)return 0;
        return Math.max(0,Math.min(duration,age+Math.max(0,Math.min(1,partial))));
    }
    /** 心跳只能往前校时，重复包不能把正在进行的动作拉回起手。 */
    public static double synchronize(double current,double received,double duration){
        return Math.max(age(current,0,duration),age(received,0,duration));
    }
    public static boolean finite(double... values){for(double v:values)if(!Double.isFinite(v))return false;return true;}
    public static final class Frames<T> {
        private final Set<T> drawn=new HashSet<>();
        public void begin(){drawn.clear();}
        public void mark(T player){drawn.add(player);}
        public boolean missing(T player){return !drawn.contains(player);}
    }
    private MotionSafety(){}
}
