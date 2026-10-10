package cn.blockforge.generated.nnnnr1201forge2;

/** 两条有限线段的真实距离，避免把滞留周围整块立方体误判成剑气。 */
public final class SlashCollision {
    public record Point(double x,double y,double z){
        Point add(Point b){return new Point(x+b.x,y+b.y,z+b.z);}
        Point subtract(Point b){return new Point(x-b.x,y-b.y,z-b.z);}
        Point scale(double n){return new Point(x*n,y*n,z*n);}
        double dot(Point b){return x*b.x+y*b.y+z*b.z;}
    }
    private static double clamp(double v){return Math.max(0,Math.min(1,v));}
    public static double distanceSquared(Point a,Point b,Point c,Point d){
        Point u=b.subtract(a),v=d.subtract(c),w=a.subtract(c);double aa=u.dot(u),bb=u.dot(v),cc=v.dot(v),dd=u.dot(w),ee=v.dot(w),s,t;
        if(aa<1e-12&&cc<1e-12)return w.dot(w);
        if(aa<1e-12){s=0;t=clamp(ee/cc);}
        else if(cc<1e-12){t=0;s=clamp(-dd/aa);}
        else{double det=aa*cc-bb*bb;s=det>1e-12?clamp((bb*ee-cc*dd)/det):0;t=(bb*s+ee)/cc;
            if(t<0){t=0;s=clamp(-dd/aa);}else if(t>1){t=1;s=clamp((bb-dd)/aa);}}
        Point delta=w.add(u.scale(s)).subtract(v.scale(t));return Math.max(0,delta.dot(delta));
    }
    private SlashCollision(){}
}
