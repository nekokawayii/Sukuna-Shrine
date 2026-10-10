package cn.blockforge.generated.nnnnr1201forge2;

/** 两个轮盘、侧栏按钮的绘制和鼠标命中共用布局。 */
public final class WheelSideSlots {
    public record Box(int x,int y,int width,int height){
        public boolean contains(double mx,double my){return mx>=x&&mx<x+width&&my>=y&&my<y+height;}
    }
    public static int iceRadius(int width,int height){return Math.max(22,Math.min(52,Math.min(width/8,height/2-70)));}
    public static int iceX(int width,int height){return 8+iceRadius(width,height);}
    public static boolean iceContains(int width,int height,double x,double y){int r=iceRadius(width,height);double dx=x-iceX(width,height),dy=y-height/2.0,d=dx*dx+dy*dy;return d>=r*r/4.0&&d<=r*r;}
    public static int radius(int width,int height){return radius(width,height,false);}
    public static int radius(int width,int height,boolean ice){
        int left=ice?8+2*iceRadius(width,height)+76:82;
        return Math.max(12,Math.min((width-left-82)/2,Math.min(100,height/2-48)));
    }
    public static int centerX(int width,int height,boolean ice){return ice?Math.max(width/2,8+2*iceRadius(width,height)+76+radius(width,height,true)):width/2;}
    public static Box reverse(int width,int height){return reverse(width,height,false);}
    public static Box reverse(int width,int height,boolean ice){return new Box(centerX(width,height,ice)-radius(width,height,ice)-72,height/2-32,64,64);}
    public static Box repair(int width,int height){return repair(width,height,false);}
    public static Box repair(int width,int height,boolean ice){Box reverse=reverse(width,height,ice);return new Box(reverse.x(),reverse.y()+68,64,36);}
    public static Box netBranch(int width,int height,int branch){return netBranch(width,height,branch,false);}
    public static Box netBranch(int width,int height,int branch,boolean ice){return new Box(centerX(width,height,ice)+radius(width,height,ice)+4,height/2-66+branch*34,74,30);}
    private WheelSideSlots(){}
}
