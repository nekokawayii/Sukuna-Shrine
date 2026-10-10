package cn.blockforge.generated.nnnnr1201forge2;

/** 160刻蓄力，240刻五阶段，10刻碎屏，40刻纯白收尾；一秒20刻。 */
public final class FinaleRules {
    public static final int SKILL=33,CHARGE=160,SHATTER=400,WHITE=410,END=450,RADIUS=150,REGEN_LOCK=1200,BURNOUT=3600;
    public static final int FX_STATE=180,FX_SHRINE=181,FX_COMBO=182,FX_WALL=183,FX_WORLD=184,FX_SCAR=185,FX_ABSORB=186;
    public static final String NAME="終式·六道連斬";
    public static final String[] CHANTS={"御厨子、開。","千手、千刀、千斬。","龍、昴、三千、斬。","龙鳞、反发、成雙之流星。","六道、連斬、終式。"};
    public static final int[] STARTS={160,202,262,316,356};
    public static final int[] TRIGGERS={190,232,292,346,386};
    public static final String[][] WORDS={{"御厨子","開"},{"千手","千刀","千斬"},{"龍","昴","三千","斬"},{"龙鳞","反发","成雙之流星"},{"六道","連斬","終式"}};
    /** 三十刻内均匀逐词发送，最后一词留足停顿，念完才触发阶段。 */
    public static int wordAt(int stage,int age){
        for(int word=0;word<WORDS[stage].length;word++)if(age==STARTS[stage]+word*30/WORDS[stage].length)return word;
        return -1;
    }
    public static boolean ready(double energy,double maximum,int linger){return maximum>0&&energy>maximum*.8&&linger>=3;}
    public static int stage(int age){if(age<CHARGE)return 0;for(int i=STARTS.length-1;i>=0;i--)if(age>=STARTS[i])return i+1;return 0;}
    public static int comboDue(int age,int count){return Math.min(count*5,Math.max(0,(age-TRIGGERS[1]+1)*count*5/30));}
    public static double mode(double x,double y){return Math.sin(2*Math.PI*x)*Math.sin(Math.PI*y);}
    private FinaleRules(){}
}
