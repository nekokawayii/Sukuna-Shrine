package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;

/** 像素、时间和图形规则由两端共用。 */
public final class SlashFormRules {
    public static final int SHAPE=24, CROSS_FLIGHT=25, FREEHAND=26, WORLD=27;
    public static final int FLIGHT=159, ACCELERATING=160, WORLD_FX=161, STILL=162;
    public static final int WORLD_LIFE=700, GAP=2, RESUME_TICKS=30, MAX_PIXELS=2000, MAX_SEGMENTS=2048;
    public static final double SPEED=65.0/20, DAMAGE=0.4;
    public record Segment(double x1,double y1,double x2,double y2) {
        public double length(){return Math.hypot(x2-x1,y2-y1);}
        public boolean valid(){return Double.isFinite(x1+y1+x2+y2)&&Math.max(Math.max(Math.abs(x1),Math.abs(y1)),Math.max(Math.abs(x2),Math.abs(y2)))<=16384&&length()>0;}
    }
    public static int crosses(int stage){return 1+(int)Math.round(19*Math.max(0,Math.min(10,stage))/10.0);}
    public static double freehandDamage(double pixels){return Math.max(0,Math.min(MAX_PIXELS,pixels))/200.0;}
    public static double travel(double age,boolean accelerating){double t=Math.max(0,age);return accelerating?(t<=20?0.2*t+0.075*t*t:34+(t-20)*3.2):t*SPEED;}
    public static String name(int skill){return switch(skill){case SHAPE->"解 · 形状斩";case CROSS_FLIGHT->"捌 · 十字飞行";case FREEHAND->"解 · 纵心";case WORLD->"解 · 世界斩";case DismantleMotionRules.RING_SKILL->"解 · 旋转环斩";default->"解";};}
    public static List<Segment> shape(int kind){
        List<Segment> lines=new ArrayList<>();
        switch(Math.floorMod(kind,7)){
            case 0->polygon(lines,4,1,Math.PI/4);
            case 1->polygon(lines,3,1,-Math.PI/2);
            case 2->polygon(lines,5,2,-Math.PI/2);
            case 3->{polygon(lines,3,1,-Math.PI/2);polygon(lines,3,1,Math.PI/2);}
            case 4->polygon(lines,7,1,-Math.PI/2);
            case 5->{lines.add(new Segment(-3,-3,3,-3));lines.add(new Segment(0,-3,0,3));lines.add(new Segment(0,0,2.5,0));lines.add(new Segment(-2,0,-2,3));lines.add(new Segment(-3,3,3,3));}
            default->polygon(lines,6,1,0);
        }
        if(Math.floorMod(kind,7)!=5){
            // 四条完整斩线各自穿过图案中心，两组相邻对角线组成双笔 X。
            double minY=lines.stream().mapToDouble(s->Math.min(s.y1(),s.y2())).min().orElse(0);
            double maxY=lines.stream().mapToDouble(s->Math.max(s.y1(),s.y2())).max().orElse(0);
            double cy=(minY+maxY)/2;
            for(double degrees:new double[]{38,52,128,142}){
                double a=Math.toRadians(degrees),radius=Math.floorMod(kind,7)==1?1.5:2.6,x=Math.cos(a)*radius,y=Math.sin(a)*radius;
                lines.add(new Segment(-x,cy-y,x,cy+y));
            }
        }
        return List.copyOf(lines);
    }
    /** 只旋转整个图案，不能逐边随机角度，否则正方形、星形会散开。 */
    public static Segment rotate(Segment s,double degrees){double a=Math.toRadians(degrees),c=Math.cos(a),n=Math.sin(a);return new Segment(s.x1*c-s.y1*n,s.x1*n+s.y1*c,s.x2*c-s.y2*n,s.x2*n+s.y2*c);}
    public static List<Segment> centered(List<Segment> lines){
        if(lines.isEmpty())return List.of();double minX=Double.MAX_VALUE,minY=Double.MAX_VALUE,maxX=-Double.MAX_VALUE,maxY=-Double.MAX_VALUE;
        for(var s:lines){minX=Math.min(minX,Math.min(s.x1,s.x2));maxX=Math.max(maxX,Math.max(s.x1,s.x2));minY=Math.min(minY,Math.min(s.y1,s.y2));maxY=Math.max(maxY,Math.max(s.y1,s.y2));}
        double cx=(minX+maxX)/2,cy=(minY+maxY)/2;return lines.stream().map(s->new Segment(s.x1-cx,s.y1-cy,s.x2-cx,s.y2-cy)).toList();
    }
    private static void polygon(List<Segment> out,int count,int step,double angle){for(int i=0;i<count;i++){double a=angle+i*step*Math.PI*2/count,b=angle+(i+1)*step*Math.PI*2/count;out.add(new Segment(Math.cos(a)*4,Math.sin(a)*4,Math.cos(b)*4,Math.sin(b)*4));}}
    private SlashFormRules(){}
}
