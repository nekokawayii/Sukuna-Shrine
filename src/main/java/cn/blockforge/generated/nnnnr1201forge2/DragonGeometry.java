package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.world.phys.Vec3;

/** 龙身的每一道解同时供服务端判定和客户端绘制，禁止用一团范围伤害替代。 */
public final class DragonGeometry {
    public record Cut(Vec3 a,Vec3 b,double width){}
    public static List<Cut> cuts(Vec3 p,Vec3 forward,double age){
        forward=forward.normalize();Vec3 side=NetBranches.side(forward),up=side.cross(forward).normalize();List<Cut> cuts=new ArrayList<>(63);
        for(int i=0;i<25;i++){
            double u=i/24.0,w=(1-u)*1.35+.1;
            Vec3 c=p.subtract(forward.scale(i*.5)).add(side.scale(Math.sin(age*.3-i*.6)*u*1.4)).add(up.scale(Math.sin(i*.45-age*.2)*u*.8));
            cuts.add(new Cut(c.subtract(side.scale(w)).subtract(forward.scale(.25)),c.add(side.scale(w)).add(up.scale(.12)),.15));
            cuts.add(new Cut(c.subtract(up.scale(w*.5)),c.add(up.scale(w*.5)),.1));
            if(i%4==0)cuts.add(new Cut(c,c.add(up.scale(1.4*w)).subtract(forward),.175));
        }
        Vec3 nose=p.add(forward.scale(2));cuts.add(new Cut(p.add(side),nose,.35));cuts.add(new Cut(p.subtract(side),nose,.35));
        for(int sign:new int[]{-1,1}){Vec3 root=p.add(side.scale(sign*.7)).add(up.scale(.5));cuts.add(new Cut(root,root.add(up.scale(1.9)).subtract(forward.scale(1.1)).add(side.scale(sign*.5)),.275));cuts.add(new Cut(nose,nose.add(side.scale(sign*2)).subtract(forward),.09));}
        return cuts;
    }
    public static List<Cut> star(Vec3 p){List<Cut> result=new ArrayList<>(10);for(int i=0;i<10;i++){double a=i*Math.PI/5-Math.PI/2,z=(i+1)*Math.PI/5-Math.PI/2,r=i%2==0?1.2:.45,s=i%2==0?.45:1.2;result.add(new Cut(p.add(Math.cos(a)*r,Math.sin(a)*r,0),p.add(Math.cos(z)*s,Math.sin(z)*s,0),.175));}return result;}
    private DragonGeometry(){}
}
