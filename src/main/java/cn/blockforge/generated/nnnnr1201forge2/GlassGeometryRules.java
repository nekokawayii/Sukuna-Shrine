package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;

/** 不规则三角玻璃；使用(2,1)振型决定双瓣碎裂位移，中央节点保持稳定。 */
public final class GlassGeometryRules {
    public record Point(double x,double y){}
    public record Shard(Point a,Point b,Point c,int shade){}
    private static Point node(int x,int y,double width,double height){
        double u=x/12.0,v=y/8.0;
        double jx=x==0||x==12?0:Math.sin(x*37.7+y*17.3)*.026;
        double jy=y==0||y==8?0:Math.sin(x*19.1+y*41.7)*.035;
        return new Point((u+jx)*width,(v+jy)*height);
    }
    private static Shard shard(Point a,Point b,Point c,double width,double height,double age,int id){
        double x=(a.x+b.x+c.x)/3,y=(a.y+b.y+c.y)/3,mode=FinaleRules.mode(x/width,y/height);
        double angle=mode*age*.07,cos=Math.cos(angle),sin=Math.sin(angle),dx=mode*age*3,dy=Math.abs(mode)*age*age*.5;
        return new Shard(move(a,x,y,dx,dy,cos,sin),move(b,x,y,dx,dy,cos,sin),move(c,x,y,dx,dy,cos,sin),id%3);
    }
    private static Point move(Point p,double x,double y,double dx,double dy,double cos,double sin){return new Point(x+dx+(p.x-x)*cos-(p.y-y)*sin,y+dy+(p.x-x)*sin+(p.y-y)*cos);}
    public static List<Shard> shards(double width,double height,double age){
        List<Shard> result=new ArrayList<>(192);
        for(int y=0;y<8;y++)for(int x=0;x<12;x++){
            Point a=node(x,y,width,height),b=node(x+1,y,width,height),c=node(x+1,y+1,width,height),d=node(x,y+1,width,height);int id=(y*12+x)*2;
            if((x+y)%2==0){result.add(shard(a,b,c,width,height,age,id));result.add(shard(a,c,d,width,height,age,id+1));}
            else{result.add(shard(a,b,d,width,height,age,id));result.add(shard(b,c,d,width,height,age,id+1));}
        }
        return result;
    }
    private GlassGeometryRules(){}
}
