package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.world.phys.Vec3;
import static cn.blockforge.generated.nnnnr1201forge2.Client.*;

/** 九相终式：每种招式独立轮廓，金边虚空刃、九重冠轮与立体焰纹。 */
public final class NineVisuals {
    public static void draw(BufferBuilder b,PoseStack ps,Network.Effect e,Vec3 cam,float age){
        float fade=Math.max(0,1-age/e.life());int gold=((int)(235*fade)<<24)|0xFFD975,purple=((int)(200*fade)<<24)|0xCD249D;
        Vec3 c=e.position().add(0,1,0),dir=e.direction().normalize();if(dir.lengthSqr()<0.1)dir=new Vec3(0,0,1);Vec3 side=new Vec3(-dir.z,0,dir.x).normalize();if(side.lengthSqr()<0.1)side=new Vec3(1,0,0);Vec3 up=dir.cross(side).normalize();int skill=(int)e.seed();
        for(int i=0;i<9;i++){
            double angle=i*Math.PI*2/9+age*0.055,r=2+Math.min(8,age*0.3);Vec3 radial=side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle))),p=c.add(radial.scale(r));
            if(skill==0||skill==6){Vec3 tip=p.add(dir.scale(8+age*0.65));VoidVisuals.needle(b,ps,p,tip,0.12,fade,cam);ribbon(b,ps,p,c.add(radial.scale(r*1.15)),0.055,gold,cam);}
            else if(skill==1){sphere(b,ps,c,r*1.6,angle,fade,cam);}
            else if(skill==2){Vec3 q=c.add(Math.cos(angle)*r,Math.sin(angle*2)*1.8,Math.sin(angle)*r);VoidVisuals.needle(b,ps,q,q.add(0,3,0),0.16,fade,cam);}
            else if(skill==3){Vec3 q=c.add(side.scale(Math.cos(angle)*3)).add(up.scale(Math.sin(angle)*3));ribbon(b,ps,q,q.add(dir.scale(4+age*0.3)),0.2,purple,cam);ribbon(b,ps,q,q.add(dir.scale(6+age*0.3)),0.035,gold,cam);}
            else {double radius=(skill==5?8:3)+age*0.6;Vec3 q=c.add(Math.cos(angle)*radius,0,Math.sin(angle)*radius);VoidVisuals.needle(b,ps,q,q.add(0,8+age*0.5,0),0.28,fade,cam);ring(b,ps,c.add(0,i*0.25,0),radius*(0.3+i*0.08),0.06,gold,cam);}
            Vec3 q=c.add(Math.cos(angle)*2,2+Math.sin(angle)*0.4,Math.sin(angle)*2);ring(b,ps,q,0.24,0.05,gold,cam);
        }
        ring(b,ps,e.position().add(0,0.12,0),3+age*0.12,0.1,purple,cam);
    }
    private static void sphere(BufferBuilder b,PoseStack ps,Vec3 c,double radius,double angle,float fade,Vec3 cam){for(int i=0;i<32;i++){double a=i*Math.PI/16,z=(i+1)*Math.PI/16;Vec3 p=c.add(Math.cos(angle)*Math.sin(a)*radius,Math.cos(a)*radius,Math.sin(angle)*Math.sin(a)*radius),q=c.add(Math.cos(angle)*Math.sin(z)*radius,Math.cos(z)*radius,Math.sin(angle)*Math.sin(z)*radius);ribbon(b,ps,p,q,0.08,((int)(210*fade)<<24)|0xD53AAD,cam);ribbon(b,ps,p,q,0.022,((int)(250*fade)<<24)|0xFFE09B,cam);}}
}
