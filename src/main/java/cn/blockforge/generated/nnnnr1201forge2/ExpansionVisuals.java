package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.world.phys.Vec3;

/** 剑气拼接的龙、六芒星、紫火与烟尘；世界坐标表现，不占用实体模型或输入。 */
public final class ExpansionVisuals {
    private record Visual(Network.Effect e,long born){}
    private static final List<Visual> visuals=new ArrayList<>();private static long tick;
    public static void collect(Vec3 center,double radius,List<Vec3> out){visuals.removeIf(v->{if(v.e.kind()==CombatExpansionRules.FX_PURPLE||v.e.kind()==CombatExpansionRules.FX_DUST||v.e.position().distanceToSqr(center)>radius*radius)return false;if(out.size()<6000)out.add(v.e.position());return true;});}
    public static void clear(){visuals.clear();tick=0;}
    public static void tick(){tick++;visuals.removeIf(v->tick-v.born>=v.e.life());}
    public static void receive(Network.Effect e){if(visuals.size()<6000)visuals.add(new Visual(e,tick));}
    private static void cut(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 z,Vec3 camera,int color,double width){SlashStyle.draw(b,ps,a,z,width*2.5,((color>>>24)&255)/255f,camera);}
    private static void fire(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 z,Vec3 camera,int color,double width){Client.ribbon(b,ps,a,z,width*3,0xB518091F,camera);Client.ribbon(b,ps,a,z,width,color,camera);}
    public static void draw(BufferBuilder b,PoseStack ps,Vec3 camera,float partial){
        for(Visual v:visuals){var e=v.e;SlashAppearance.use(e);double age=tick-v.born+partial,fade=Math.min(1,(e.life()-age)/3);int alpha=(int)(255*Math.max(0,fade));
            Vec3 p=e.position(),forward=e.direction().lengthSqr()<.01?new Vec3(0,0,1):e.direction().normalize(),side=NetBranches.side(forward),up=side.cross(forward).normalize();
            if(e.kind()==CombatExpansionRules.FX_SLASH&&e.seed()==3){Vec3 center=p.add(e.direction().scale(Math.min(1,age/3)));cut(b,ps,center.add(-2,0,0),center.add(2,0,0),camera,(alpha<<24)|0xFFE5F5,.055);continue;}
            if(e.kind()==CombatExpansionRules.FX_SLASH){cut(b,ps,p,p.add(e.direction()),camera,(alpha<<24)|(e.seed()==2?0xBFD8FF:e.seed()==1?0xDC99FF:0xFFE5F5),e.seed()==1?.14:.035);continue;}
            if(e.kind()==CombatExpansionRules.FX_DRAGON){
                for(DragonGeometry.Cut c:DragonGeometry.cuts(p,forward,e.seed()))SlashStyle.draw(b,ps,c.a(),c.b(),c.width(),alpha/255f,camera);
                continue;
            }
            if(e.kind()==CombatExpansionRules.FX_STAR){
                for(DragonGeometry.Cut c:DragonGeometry.star(p))SlashStyle.draw(b,ps,c.a(),c.b(),c.width(),alpha/255f,camera);continue;
            }
            if(e.kind()==CombatExpansionRules.FX_DUST){
                double radius=e.size()*(.2+age/e.life());for(int i=0;i<22;i++){double a=i*Math.PI*2/22;Vec3 at=p.add(Math.cos(a)*radius,.15+Math.sin(i*13)*.3,Math.sin(a)*radius);Client.ribbon(b,ps,at,at.add(0,.4+age*.025,0),.2,(Math.min(160,alpha)<<24)|(age<5?0xEBC4A4:0x92818C),camera);}continue;
            }
            if(e.kind()==CombatExpansionRules.FX_PURPLE){
                if(e.seed()==1){
                    Vec3 center=p.add(forward.scale(1.5));for(int i=0;i<20;i++){double a=-Math.PI/2+i*Math.PI/20,z=-Math.PI/2+(i+1)*Math.PI/20;Vec3 a1=center.add(up.scale(Math.sin(a)*1.6)).add(forward.scale(Math.cos(a)*.65)),a2=center.add(up.scale(Math.sin(z)*1.6)).add(forward.scale(Math.cos(z)*.65));fire(b,ps,a1,a2,camera,0xFFD399FF,.11);}fire(b,ps,center.add(up.scale(-1.6)),center.subtract(forward.scale(.5)),camera,0xFFFAE6FF,.025);fire(b,ps,center.add(up.scale(1.6)),center.subtract(forward.scale(.5)),camera,0xFFFAE6FF,.025);
                }else if(e.seed()==2){for(int i=0;i<48;i++){double a=i*Math.PI*2/48,r=e.size()*Math.min(1,age/12),z=(i+1)*Math.PI*2/48;fire(b,ps,p.add(Math.cos(a)*r,Math.sin(age*.2)*.4,Math.sin(a)*r),p.add(Math.cos(z)*r,.2,Math.sin(z)*r),camera,(alpha<<24)|0xB352FF,.3);}}
                else{fire(b,ps,p.subtract(forward.scale(3)),p.add(forward),camera,0xFFE4A5FF,.2);for(int i=0;i<8;i++){double a=i*Math.PI/4+age*.4;Vec3 at=p.add(side.scale(Math.cos(a)*.5)).add(up.scale(Math.sin(a)*.5));fire(b,ps,at,at.subtract(forward.scale(2.2)),camera,0xFF942BFF,.07);}}
            }
        }
    }
    private ExpansionVisuals(){}
}
