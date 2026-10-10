package cn.blockforge.generated.nnnnr1201forge2;

/** 新分支的纯数值规则，客户端显示与服务端结算共用。 */
public final class BranchRules {
    private static final int[] GUARDIAN_WHEEL={7,8,9,10,11,12,13,15,16};
    public static final int WEB=19, MULTI=20, WALL=21, FROST=22, ICEFALL=23;
    public static final int FX_LINE=154, FX_WALL=155, FX_ICE=156, FX_FROST=157, FX_FREEZE=158;
    public static int radius(int stage){return 10+9*Math.max(0,Math.min(10,stage));}
    public static int cuts(int stage){return 5+(int)Math.round(3.5*Math.max(0,Math.min(10,stage)));}
    public static double spacing(int halfBlocks){return Math.max(1,Math.min(20,halfBlocks))/2.0;}
    /** 墙从觉醒时解锁：觉醒50、九相100、灵魂150、虚空200、传承300格。 */
    public static int wallSize(boolean nine,boolean soul,boolean voidShrine,boolean inheritance){return inheritance?300:voidShrine?200:soul?150:nine?100:50;}
    public static int wallLines(int halfBlocks,int size){return 2*((int)Math.floor(size/spacing(halfBlocks))+1);}
    // 阶段扩大范围属于成长奖励；保留原有密度消耗，避免高阶墙因范围增大而无法支付。
    public static int wallLines(int halfBlocks){return wallLines(halfBlocks,20);}
    public static int wallCost(int halfBlocks){return 80+wallLines(halfBlocks)*20;}
    public static double cleave(double energy,double maximum){return maximum<=0?0:1.5*Math.max(0,Math.min(1,energy/maximum));}
    public static double iceDamage(int fish){return 1+Math.max(0,fish)*0.01;}
    public static int upgrades(int fish){return Math.max(0,fish)/10;}
    public static boolean acceptMemory(boolean sukuna,boolean uraume,boolean maid,boolean wantIce){return wantIce?!uraume&&(!maid||!sukuna):!sukuna&&!uraume;}
    public static double iceLength(int fish){return 10+2*Math.log1p(upgrades(fish));}
    public static double iceWidth(int fish){return 5+Math.log1p(upgrades(fish));}
    public static double iceHeight(int fish){return 3+0.5*Math.log1p(upgrades(fish));}
    public static double iceScatter(int fish){return 5/Math.sqrt(1+upgrades(fish)*0.12);}
    public static int icePillars(int fish){return Math.min(140,(int)Math.floor(12*Math.log1p(upgrades(fish))));}
    public static boolean corridor(double x,double y,double sx,double sy,double panelX,double panelTop,double panelBottom){
        if(x<sx-3||x>panelX+78||panelX<=sx)return false;
        double near=Math.max(0,Math.min(1,(x-sx)/(panelX-sx))),far=Math.max(0,Math.min(1,(x-sx)/(panelX+74-sx)));
        double top=Math.min(sy+(panelTop-sy)*near,sy+(panelTop-sy)*far)-8,bottom=Math.max(sy+(panelBottom-sy)*near,sy+(panelBottom-sy)*far)+8;return y>=top&&y<=bottom;
    }
    public static int wheelSkill(boolean sukuna,boolean uraume,int page,int count,double x,double y){
        if(!sukuna)return y<0?FROST:ICEFALL;
        int index=WheelSelection.sector(x,y,count);
        return page==1?GUARDIAN_WHEEL[index]:index;
    }
    public static String name(int skill){return switch(skill){case WEB->"蜘蛛网";case MULTI->"多重斩";case WALL->"墙";case FROST->"冰凝咒法 · 霜凪";case ICEFALL->"冰凝咒法 · 直瀑";default->NetBranchRules.name(skill);};}
    private BranchRules(){}
}
