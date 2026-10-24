package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;

/** 线段沿位移扫过的平行四边形，与盒子的精确参数空间裁剪。无游戏依赖，供回归测试。 */
public final class SlashSweepRules {
    public record Point(double u,double t){}
    public record Contact(double lo,double hi,double time){}
    public record Span(double lo,double hi,double time,boolean blocked){}
    private static List<Point> clip(List<Point> input,double u,double t,double bound){
        if(input.isEmpty())return input;
        List<Point> out=new ArrayList<>();Point a=input.get(input.size()-1);double da=a.u*u+a.t*t-bound;
        for(Point b:input){double db=b.u*u+b.t*t-bound;
            if((da<=0)!=(db<=0)){double f=da/(da-db);out.add(new Point(a.u+(b.u-a.u)*f,a.t+(b.t-a.t)*f));}
            if(db<=0)out.add(b);a=b;da=db;
        }
        return out;
    }
    public static Contact contact(double[] start,double[] edge,double[] step,double[] min,double[] max){
        List<Point> polygon=List.of(new Point(0,0),new Point(1,0),new Point(1,1),new Point(0,1));
        for(int axis=0;axis<3;axis++){
            // 刀面与方块表面共面时只有擦边，没有进入实体体积；不能把贴地/贴墙飞行整段吃掉。
            // 朝墙内运动仍走下面的连续裁剪，出生在实心方块里的部分也仍然立即停止。
            if(Math.abs(edge[axis])+Math.abs(step[axis])<1e-12
                &&(start[axis]<=min[axis]+1e-8||start[axis]>=max[axis]-1e-8))return null;
            polygon=clip(polygon,edge[axis],step[axis],max[axis]-start[axis]);
            polygon=clip(polygon,-edge[axis],-step[axis],start[axis]-min[axis]);
            if(polygon.isEmpty())return null;
        }
        double lo=1,hi=0,time=1;for(Point p:polygon){lo=Math.min(lo,p.u);hi=Math.max(hi,p.u);time=Math.min(time,p.t);}
        return hi-lo<=1e-9?null:new Contact(lo,hi,Math.max(0,time));
    }
    /** 只删除挡住的参数区间；多个障碍取最近的接触时间，不允许在远墙处重生。 */
    public static List<Span> split(List<Contact> contacts){
        TreeSet<Double> points=new TreeSet<>();points.add(0.0);points.add(1.0);
        for(Contact c:contacts){points.add(Math.max(0,Math.min(1,c.lo)));points.add(Math.max(0,Math.min(1,c.hi)));}
        List<Double> ends=new ArrayList<>(points);List<Span> out=new ArrayList<>();
        for(int i=1;i<ends.size();i++){
            double a=ends.get(i-1),b=ends.get(i),middle=(a+b)*.5,time=1;boolean blocked=false;
            if(b-a<1e-8)continue;
            for(Contact c:contacts)if(middle>c.lo-1e-9&&middle<c.hi+1e-9){blocked=true;time=Math.min(time,c.time);}
            out.add(new Span(a,b,time,blocked));
        }
        return out;
    }
    private SlashSweepRules(){}
}
