package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.world.phys.Vec3;
import static cn.blockforge.generated.nnnnr1201forge2.Client.*;

/** 领域心象覆盖：不写入真实世界方块，战斗结束即恢复。 */
public final class SoulScenery {
    private static void vertex(BufferBuilder b,PoseStack ps,Vec3 p,int c){b.vertex(ps.last().pose(),(float)p.x,(float)p.y,(float)p.z).color(c>>16&255,c>>8&255,c&255,c>>>24&255).endVertex();}
    private static void face(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 c,Vec3 d,Vec3 e,int color){vertex(b,ps,a,color);vertex(b,ps,c,color);vertex(b,ps,d,color);vertex(b,ps,e,color);}
    private static void box(BufferBuilder b,PoseStack ps,Vec3 c,double x,double y,double z,int color){
        Vec3 a=c.add(-x,0,-z),d=c.add(x,0,-z),e=c.add(x,0,z),f=c.add(-x,0,z);Vec3 A=a.add(0,y,0),D=d.add(0,y,0),E=e.add(0,y,0),F=f.add(0,y,0);
        face(b,ps,a,d,D,A,color);face(b,ps,d,e,E,D,color);face(b,ps,e,f,F,E,color);face(b,ps,f,a,A,F,color);face(b,ps,A,D,E,F,color);
    }
    public static void station(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 cam,float radius){
        double r=Math.max(16,radius);box(b,ps,c.add(0,-0.03,0),r,0.05,r,0xFF98998C);
        for(int x=-2;x<=2;x++){double track=x*6;box(b,ps,c.add(track,0.03,0),0.06,0.035,r,0xFF373A3F);}
        for(double z=-r;z<=r;z+=2)box(b,ps,c.add(0,0.025,z),13,0.035,0.13,0xFF584B41);
        for(int side:new int[]{-1,1}){double x=side*20;box(b,ps,c.add(x,0.1,0),5,0.6,r,0xFFC6C7BC);box(b,ps,c.add(x-side*4.5,0.72,0),0.25,0.025,r,0xFFE8C641);box(b,ps,c.add(x,5,0),6,0.3,r,0xFF626F67);
            for(double z=-r+6;z<r;z+=12){box(b,ps,c.add(x,0.7,z),0.18,4.3,0.18,0xFF808F82);box(b,ps,c.add(x,0.7,z+3),2,0.6,0.5,0xFF865E46);box(b,ps,c.add(x,4.9,z),2,0.05,0.2,0xFFFFEAB2);}
        }
        box(b,ps,c.add(0,0,r-5),12,4,3,0xFFDFDDCD);box(b,ps,c.add(0,4,r-5),13,0.5,4,0xFF435A4A);box(b,ps,c.add(0,2.3,r-8.03),5,1.1,0.03,0xFF254B35);
        for(int i=-3;i<=3;i++)box(b,ps,c.add(i*1.3,2.55,r-8.08),0.25,0.5,0.04,0xFFF3F1D9);
    }
    public static void draw(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 cam,float age){
        // 地面略高于走廊表面；侧面风景铺在黑墙内侧，保留实际碰撞边界。
        Vec3 floor=c.add(0,0.025,0);face(b,ps,floor.add(-29,0,-48),floor.add(29,0,-48),floor.add(29,0,48),floor.add(-29,0,48),0xFF94A767);
        for(int z=-48;z<48;z+=4)for(int x=-28;x<29;x+=4){if(Math.abs(x)<4)continue;int color=((x+z)/4%3==0)?0xFFBDAB62:0xFF869450;Vec3 p=floor.add(x,0.005,z);face(b,ps,p,p.add(3.5,0,0),p.add(3.5,0,3.5),p.add(0,0,3.5),color);}
        face(b,ps,floor.add(-2,0.02,-48),floor.add(2,0.02,-48),floor.add(2,0.02,48),floor.add(-2,0.02,48),0xFFC7AA7A);
        for(int side:new int[]{-1,1}){
            double x=side*29.3;face(b,ps,c.add(x,0,-48),c.add(x,0,48),c.add(x,18,48),c.add(x,18,-48),0xFFF2DAB4);
            for(int z=-48;z<48;z+=12){Vec3 p=c.add(x-side*0.04,0,z);double h=5+Math.sin(z*0.9)*2;face(b,ps,p,p.add(0,0,12),p.add(-side*0.02,h,6),p.add(-side*0.02,h,6),0xFF969E7C);}
        }
        face(b,ps,c.add(-29,17.8,-48),c.add(29,17.8,-48),c.add(29,17.8,48),c.add(-29,17.8,48),0xFFEBD1AC);
        for(int sign:new int[]{-1,1})face(b,ps,c.add(-29,0,sign*47.8),c.add(29,0,sign*47.8),c.add(29,18,sign*47.8),c.add(-29,18,sign*47.8),0xFFE4CFA9);
        // 低矮房屋放在场地边缘，中央仍保留宽敞战斗空间。
        for(int side:new int[]{-1,1})for(int z:new int[]{-30,25}){
            Vec3 p=c.add(side*24,0.05,z);box(b,ps,p,3,3.2,4,0xFFD5C3A2);box(b,ps,p.add(0,3.2,0),3.5,0.35,4.5,0xFF635B55);box(b,ps,p.add(0,3.55,0),2.5,0.35,4,0xFF75655D);box(b,ps,p.add(-side*3.01,0,0),0.03,2,0.65,0xFF493C32);
        }
        for(int i=0;i<100;i++){double x=Math.sin(i*7.31)*27,z=Math.cos(i*3.79)*46,y=15-((age*0.035+i*0.713)%15);Vec3 p=c.add(x+Math.sin(age*0.02+i),y,z);ribbon(b,ps,p,p.add(0,0.09,0),0.045,0xCFFFFBE5,cam);}
    }
}
