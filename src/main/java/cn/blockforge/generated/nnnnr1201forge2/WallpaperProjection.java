package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;

/** 裁切齐次坐标后投影，保持壁纸固定在屏幕上，跨近裁面也不会翻到镜头后方。 */
public final class WallpaperProjection {
    public record Point(double x,double y,double z,double w){
        Point lerp(Point b,double t){return new Point(x+(b.x-x)*t,y+(b.y-y)*t,z+(b.z-z)*t,w+(b.w-w)*t);}
    }
    public static List<Point> clip(List<Point> polygon){
        if(polygon.stream().anyMatch(p->!Double.isFinite(p.x+p.y+p.z+p.w)))return List.of();
        List<Point> out=new ArrayList<>(polygon);
        for(int plane=0;plane<7&&!out.isEmpty();plane++){
            List<Point> input=out;out=new ArrayList<>();Point previous=input.get(input.size()-1);double a=distance(previous,plane);
            for(Point current:input){double b=distance(current,plane);if((a>=0)!=(b>=0))out.add(previous.lerp(current,a/(a-b)));if(b>=0)out.add(current);previous=current;a=b;}
        }
        return out;
    }
    private static double distance(Point p,int plane){return switch(plane){case 0->p.w-.0001;case 1->p.x+p.w;case 2->p.w-p.x;case 3->p.y+p.w;case 4->p.w-p.y;case 5->p.z+p.w;default->p.w-p.z;};}
    public static double[] uv(double x,double y,double ox,double oy,double sx,double sy){double u=x*.5+.5,v=.5-y*.5,du=ox+u*sx,dv=oy+v*sy;if(du<0||du>1||dv<0||dv>1){du=u;dv=v;}return new double[]{Math.max(.001,Math.min(.999,du)),Math.max(.001,Math.min(.999,dv))};}
    private WallpaperProjection(){}
}
