package character.domain;

/** 无游戏依赖的经纬投影与接缝规则，可在正式打包前单测。 */
public final class DomainVisualRules {
    public static double u(double x,double z){return .5+Math.atan2(z,x)/(2*Math.PI);}
    public static double v(double x,double y,double z){
        double length=Math.max(.00001,Math.sqrt(x*x+y*y+z*z));
        return .5-Math.asin(Math.max(-1,Math.min(1,y/length)))/Math.PI;
    }
    /** 晚霞是地平线照片而非整球全景：太阳留在地平线，下半球作为地板倒影。 */
    public static double sunsetV(double sphereV){return Math.max(0,Math.min(1,1-Math.abs(sphereV*2-1)));}
    public static double unwrap(double first,double next){
        if(!Double.isFinite(first)||!Double.isFinite(next))return 0;
        return next-Math.floor(next-first+.5);
    }
    public static int clashLimit(boolean human){return human?1200:400;}
    /** 优势推动分界，但胜负决定前双方始终保有空间；胜方随后恢复完整球。 */
    public static double displacement(int lead,double radius){return Math.max(-1,Math.min(1,lead/25.0))*Math.max(0,radius)*.75;}
    /** 在双方共同覆盖的轴向区间内推进，大小领域接触时不能把分界推到小球之外。 */
    public static double partitionOffset(double distance,double a,double b,int lead){
        double low=Math.max(-a,distance-b),high=Math.min(a,distance+b);
        if(high<low)return distance*.5;
        double middle=(low+high)*.5,half=(high-low)*.5;
        return middle+Math.max(-1,Math.min(1,lead/25.0))*half*.75;
    }
    /** 线段与切面的准确交点比例，避免整块四边形舍弃造成锯齿大洞。 */
    public static double intersection(double from,double to){return Math.max(0,Math.min(1,from/(from-to)));}
    private DomainVisualRules(){ }
}
