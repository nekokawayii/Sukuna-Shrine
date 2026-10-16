package cn.blockforge.generated.nnnnr1201forge2;

/** 两个轮盘、侧栏按钮的绘制和鼠标命中共用布局。 */
public final class WheelSideSlots {
    public record Box(int x,int y,int width,int height){
        public boolean contains(double mx,double my){return mx>=x&&mx<x+width&&my>=y&&my<y+height;}
    }
    public static int iceRadius(int width,int height){return Math.max(22,Math.min(52,Math.min(width/8,height/2-70)));}
    public static int iceX(int width,int height){return 8+iceRadius(width,height);}
    public static boolean iceContains(int width,int height,double x,double y){int r=iceRadius(width,height);double dx=x-iceX(width,height),dy=y-height/2.0,d=dx*dx+dy*dy;return d>=r*r/4.0&&d<=r*r;}
    public static int radius(int width,int height){return radius(width,height,false,false);}
    /** 五条悟有 9 格，圆环比里梅那圈放大一档，字才排得开。 */
    public static int gojoRadius(int width,int height){return Math.max(30,Math.min(68,Math.min(width/7,height/2-64)));}
    public static int gojoX(int width,int height){return width-8-gojoRadius(width,height);}
    public static boolean gojoContains(int width,int height,double x,double y){int r=gojoRadius(width,height);double dx=x-gojoX(width,height),dy=y-height/2.0,d=dx*dx+dy*dy;return d>=r*r/4.0&&d<=r*r;}
    /** 右侧五条悟圆环：正上方为「苍」，顺时针等分 9 格 —— 一格一个技能，34+i 就是技能号。 */
    public static int gojoSectors(){return GojoPlayer.SLOTS;} // 7 格：最大功率两式不占格
    public static int gojoSector(int width,int height,double x,double y){double dx=x-gojoX(width,height),dy=y-height/2.0;double a=Math.atan2(dy,dx)+Math.PI/2;if(a<0)a+=Math.PI*2;int n=Math.max(1,gojoSectors());return (int)Math.floor(a/(Math.PI*2/n))%n;}
    public static int radius(int width,int height,boolean ice){return radius(width,height,ice,false);}
    public static int radius(int width,int height,boolean ice,boolean gojo){
        int left=ice?8+2*iceRadius(width,height)+76:82;
        int right=gojo?8+2*gojoRadius(width,height)+76:82;
        return Math.max(12,Math.min((width-left-right)/2,Math.min(100,height/2-48)));
    }
    public static int centerX(int width,int height,boolean ice){return centerX(width,height,ice,false);}
    public static int centerX(int width,int height,boolean ice,boolean gojo){int r=radius(width,height,ice,gojo);if(gojo&&!ice)return Math.min(width/2,width-8-2*gojoRadius(width,height)-76-r);return ice?Math.max(width/2,8+2*iceRadius(width,height)+76+r):width/2;}
    public static Box reverse(int width,int height){return reverse(width,height,false);}
    public static Box reverse(int width,int height,boolean ice){return new Box(centerX(width,height,ice)-radius(width,height,ice)-72,height/2-32,64,64);}
    /** **屏幕右上角**的「咒力附拳」固定格：不属于任何分支，轮盘一打开就在（5 根手指起可用）。 */
    public static Box fist(int width,int height,boolean ice){
        return new Box(width-96,12,84,34);
    }
    public static Box repair(int width,int height){return repair(width,height,false);}
    public static Box repair(int width,int height,boolean ice){Box reverse=reverse(width,height,ice);return new Box(reverse.x(),reverse.y()+68,64,36);}
    public static Box netBranch(int width,int height,int branch){return netBranch(width,height,branch,false);}
    public static Box netBranch(int width,int height,int branch,boolean ice){return new Box(centerX(width,height,ice)+radius(width,height,ice)+4,height/2-66+branch*34,74,30);}
    private WheelSideSlots(){}
}
