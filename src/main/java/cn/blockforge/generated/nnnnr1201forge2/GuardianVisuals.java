package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import static cn.blockforge.generated.nnnnr1201forge2.Client.*;

/** 净土守护者独立演出：强化、反转、穿血、灵魂迟滞与连打各自使用不同轮廓。 */
public final class GuardianVisuals {
    public static void draw(BufferBuilder b,PoseStack ps,Network.Effect e,Vec3 cam,float age){
        var level=Minecraft.getInstance().level;if(level==null)return;
        var owner=level.getEntity(e.owner());
        float fade=Math.max(0,1-age/Math.max(1,e.life()));
        Vec3 c=e.position();
        int crimson=color(0xD91439,fade),white=color(0xFFF2E7,fade),cyan=color(0x68E9F2,fade);
        // 百敛·穿血专用：由外到内只有三层暗红，不再有白色内芯与猩红高光。
        int bloodDeep=color(0x3C0307,fade),bloodMid=color(0x7E0812,fade),bloodCore=color(0x9C0F1B,fade);
        switch(e.kind()){
            case 136 -> {
                if(owner!=null)c=owner.position();
                // 赤鳞跃动：绕身体向上攀升的双股赤红咒力。
                for(int strand=0;strand<2;strand++)for(int i=0;i<14;i++){
                    double a=i*0.55+age*0.18+strand*Math.PI;
                    Vec3 p=c.add(Math.cos(a)*0.64,0.12+i*0.14,Math.sin(a)*0.64);
                    Vec3 q=c.add(Math.cos(a+0.55)*0.64,0.26+i*0.14,Math.sin(a+0.55)*0.64);
                    ribbon(b,ps,p,q,0.045,crimson,cam);
                }
                ring(b,ps,c.add(0,0.08,0),0.9+age*0.025,0.045,crimson,cam);
            }
            case 137 -> {
                if(owner!=null)c=owner.position();
                // 反转：薄荷色上升光环与交叉亮点，不与强化共用红色气焰。
                for(int j=0;j<3;j++){
                    double h=(age*0.10+j*0.7)%2.2;
                    ring(b,ps,c.add(0,h,0),0.64+0.12*Math.sin(h*3),0.027,color(0x80FFD4,fade),cam);
                }
                for(int i=0;i<8;i++){
                    double a=i*Math.PI/4+age*0.08;
                    Vec3 p=c.add(Math.cos(a)*0.8,0.25+(i%4)*0.45,Math.sin(a)*0.8);
                    ribbon(b,ps,p.add(-0.09,0,0),p.add(0.09,0,0),0.024,white,cam);
                    ribbon(b,ps,p.add(0,-0.09,0),p.add(0,0.09,0),0.024,white,cam);
                }
            }
            case 138 -> {
                // 穿血蓄力：血丝向双手前方的压缩血球收束，纯暗红。
                double r=0.12+Math.max(0,Math.min(1,e.size()))*0.5;
                Vec3 normal=owner==null?new Vec3(0,0,1):owner.getLookAngle();
                disc(b,ps,c,normal,r,0.06,bloodMid,cam);
                disc(b,ps,c,normal,r*0.45,0.035,bloodCore,cam);
                for(int i=0;i<8;i++){
                    double a=i*Math.PI/4+age*0.3;
                    Vec3 d=new Vec3(Math.cos(a),Math.sin(a),Math.sin(a*2)*0.3).normalize();
                    ribbon(b,ps,c.add(d.scale(r+0.45)),c.add(d.scale(r)),0.025,bloodMid,cam);
                }
            }
            case 139 -> {
                // 发射终点直接使用服务端碰撞结果，不在客户端延伸穿墙；光束由外到内全部为暗红。
                Vec3 end=e.direction(),dir=end.subtract(c).normalize();
                // 第一人称时光束起点在镜头附近，直接沿视线会被相机裁掉；向镜头侧偏移并保留稳定宽度。
                Vec3 side=dir.cross(cam.subtract(c)).normalize();
                if(side.lengthSqr()<0.01)side=dir.cross(new Vec3(0,1,0)).normalize();
                if(side.lengthSqr()<0.01)side=new Vec3(1,0,0);
                Vec3 offset=side.scale(0.035);
                c=c.add(offset);end=end.add(offset);
                double width=Math.max(0.08,Math.min(0.48,e.size()+0.06));
                ribbon(b,ps,c,end,width,bloodDeep,cam);
                ribbon(b,ps,c,end,width*0.50,bloodMid,cam);
                ribbon(b,ps,c,end,width*0.16,bloodCore,cam);
                disc(b,ps,end,dir,0.24+age*0.04,0.035,bloodCore,cam);
            }
            case 140 -> {
                // 逕庭拳第一拍：蓝白断续魂线，区别于连打的实线拳风。
                Vec3 end=e.direction();
                for(int i=0;i<8;i++)ribbon(b,ps,c.lerp(end,i/8.0),c.lerp(end,(i+0.6)/8.0),0.045,cyan,cam);
            }
            case 141 -> {
                var target=level.getEntity((int)e.seed());if(target!=null)c=target.getBoundingBox().getCenter();
                Vec3 normal=cam.subtract(c).normalize();
                disc(b,ps,c,normal,0.3+age*0.09,0.04,cyan,cam);
                disc(b,ps,c,normal,0.18+Math.max(0,age-3)*0.08,0.025,white,cam);
                for(int i=0;i<6;i++){
                    double a=i*Math.PI/3;
                    Vec3 d=new Vec3(Math.cos(a),Math.sin(a),0.25*Math.sin(a*2)).normalize();
                    ribbon(b,ps,c.add(d.scale(0.15+age*0.04)),c.add(d.scale(0.55+age*0.09)),0.045,cyan,cam);
                }
            }
            case 142 -> {
                // 连打：交替左右的短拳风；没有黑闪的黑红闪电。
                Vec3 end=e.direction(),dir=end.subtract(c).normalize();
                Vec3 side=dir.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<0.01)side=new Vec3(1,0,0);
                Vec3 offset=side.scale((e.seed()%2==0?1:-1)*0.2);
                Vec3 start=c.add(offset),tip=end.add(offset.scale(0.3));
                ribbon(b,ps,start,tip,0.08,white,cam);
                ribbon(b,ps,start.add(0,-0.16,0),tip.add(0,-0.16,0),0.025,color(0xF5BAA2,fade*0.7f),cam);
                disc(b,ps,tip,dir,0.14+age*0.025,0.025,white,cam);
            }
            default -> {}
        }
    }
    private static int color(int rgb,float alpha){return ((int)(235*Math.max(0,Math.min(1,alpha)))<<24)|rgb;}
    static void disc(BufferBuilder b,PoseStack ps,Vec3 center,Vec3 normal,double radius,double width,int color,Vec3 cam){
        Vec3 n=normal.normalize();if(n.lengthSqr()<0.01)n=new Vec3(0,0,1);
        Vec3 side=n.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<0.01)side=new Vec3(1,0,0);
        Vec3 up=n.cross(side).normalize();
        for(int i=0;i<24;i++){
            double a=i*Math.PI/12,z=(i+1)*Math.PI/12;
            Vec3 p=center.add(side.scale(Math.cos(a)*radius)).add(up.scale(Math.sin(a)*radius));
            Vec3 q=center.add(side.scale(Math.cos(z)*radius)).add(up.scale(Math.sin(z)*radius));
            ribbon(b,ps,p,q,width,color,cam);
        }
    }
    private GuardianVisuals(){}
}
