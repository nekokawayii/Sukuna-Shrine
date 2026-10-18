package cn.blockforge.generated.nnnnr1201forge2;

/** 虚空/传承阶段后的斩击演出：先斩出一道闭合的黑线，再像眼睛一样睁开，露出内层壁纸。 */
public final class SlashEye {
    /** 闭合黑线先保持的刻数：先让人看清"斩开了一道口子"。 */
    public static final int LINE_TICKS=1;
    /** 睁眼所用刻数（缓出：第二刻就到了 58%，第四刻基本全开）。 */
    public static final int OPEN_TICKS=4;
    /** 闭合时保留的宽度倍率：0.10 = 只剩十分之一粗的一条黑线。 */
    public static final float CLOSED=.10f;
    /** 眼睑（包边与黑芯）颜色：睁开前一律纯黑，睁开后内部才由壁纸填充。 */
    public static final int RIM=0x000000;
    /** 距斩开时刻的刻数 → 0 闭合成黑线，1 完全睁开。 */
    public static float open(double age){
        double t=(age-LINE_TICKS)/OPEN_TICKS;
        if(t<=0)return 0;
        if(t>=1)return 1;
        double rest=1-t;
        return (float)(1-rest*rest*rest);
    }
    /** 半宽随睁眼进度生长：两端始终收在斩击起止点，所以看起来是"从中间裂开"。 */
    public static double width(double full,float open){
        double o=Math.max(0,Math.min(1,open));
        return Math.max(1e-3,full*(CLOSED+(1-CLOSED)*o));
    }
    private SlashEye(){}
}
