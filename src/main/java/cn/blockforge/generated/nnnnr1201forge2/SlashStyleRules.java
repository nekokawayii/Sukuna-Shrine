package cn.blockforge.generated.nnnnr1201forge2;

/** 纯数值样式：60刻缓慢闪黑，端点收尖，中心最宽。 */
public final class SlashStyleRules {
    public static final double CORE=.64,TIP_INSET=.035;
    public static int edge(double time){int c=(int)Math.round(255*(.5+.5*Math.cos(time*Math.PI/30)));return c<<16|c<<8|c;}
    public static double profile(double t){return Math.max(0,1-Math.abs(2*t-1));}
    private SlashStyleRules(){}
}
