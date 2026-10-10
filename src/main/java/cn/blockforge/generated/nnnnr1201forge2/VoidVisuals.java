package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.world.phys.Vec3;
import static cn.blockforge.generated.nnnnr1201forge2.Client.*;

/** 裂缝位置以服务端每刻同步为准；保留折点，不用平滑转向抹去锐角。 */
public final class VoidVisuals {
    private static final Map<Long,Needle> NEEDLES=new LinkedHashMap<>();
    private static class Needle {
        final Network.Effect appearance;Vec3 pos,previous,dir;int age,timeout;float tier;PhotonBridge.Handle photon;
        final Deque<Vec3> trail=new ArrayDeque<>();
        Needle(Network.Effect e){appearance=e;pos=previous=e.position();dir=e.direction();tier=e.size();photon=PhotonBridge.needle(pos,18,0);}
    }
    public static void receive(Network.Effect e){
        if(e.kind()==61){if(NEEDLES.size()>=256){Long first=NEEDLES.keySet().iterator().next();PhotonBridge.stop(NEEDLES.remove(first).photon);}Needle old=NEEDLES.put(e.seed(),new Needle(e));if(old!=null)PhotonBridge.stop(old.photon);}
        if(e.kind()==62){Needle n=NEEDLES.get(e.seed());if(n!=null){n.previous=n.pos;n.pos=e.position();n.dir=e.direction();n.timeout=0;n.trail.addLast(n.pos);while(n.trail.size()>12)n.trail.removeFirst();PhotonBridge.move(n.photon,n.pos);}}
        if(e.kind()==63){Needle n=NEEDLES.remove(e.seed());if(n!=null)PhotonBridge.stop(n.photon);}
    }
    public static void tick(){for(Iterator<Needle> it=NEEDLES.values().iterator();it.hasNext();){Needle n=it.next();n.age++;n.timeout++;if(n.age>145){PhotonBridge.stop(n.photon);it.remove();continue;}if(n.age%8==0){PhotonBridge.stop(n.photon);n.photon=PhotonBridge.needle(n.pos,12,(float)Math.toDegrees(Math.atan2(n.dir.y,n.dir.x))-90);}}}
    public static void collect(Vec3 center,double radius,List<Vec3> out){NEEDLES.values().removeIf(n->{if(n.pos.distanceToSqr(center)>radius*radius)return false;if(out.size()<6000)out.add(n.pos);PhotonBridge.stop(n.photon);return true;});}
    public static void clear(){for(Needle n:NEEDLES.values())PhotonBridge.stop(n.photon);NEEDLES.clear();}
    public static void render(BufferBuilder b,PoseStack ps,Vec3 cam,float partial){
        for(Needle n:NEEDLES.values()){
            SlashAppearance.use(n.appearance);Vec3 p=n.previous.lerp(n.pos,partial);boolean opening=n.age<12||n.trail.isEmpty();Vec3 axis=opening?n.dir:n.dir.normalize();double length=opening?3.2*Math.min(1,(n.age+partial)/6):4.8,width=opening?0.30:0.12;
            Vec3 a=p.subtract(axis.scale(length*0.5)),z=p.add(axis.scale(length*0.5));needle(b,ps,a,z,width,1,cam);
            if(opening){for(int i=0;i<5;i++){Vec3 q=p.add(axis.scale((i-2)*0.5)),side=axis.cross(cam.subtract(p)).normalize().scale((i%2==0?1:-1)*0.6);SlashStyle.detail(b,ps,q,q.add(side),0.022,0xEFFFCD58,cam);}}
            Vec3 old=null;int index=0;for(Vec3 q:n.trail){if(old!=null){float alpha=(++index)/(float)Math.max(1,n.trail.size());SlashStyle.detail(b,ps,old,q,0.14,((int)(150*alpha)<<24)|0xC02083,cam);SlashStyle.detail(b,ps,old,q,0.025,((int)(220*alpha)<<24)|0xFFD563,cam);}old=q;}
        }
    }
    static void needle(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 z,double width,float alpha,Vec3 cam){SlashStyle.draw(b,ps,a,z,width,alpha,cam);}
    private static void diamond(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 z,Vec3 mid,Vec3 edge,double width,int color){for(Vec3 p:new Vec3[]{a,mid.add(edge.scale(width)),z,mid.subtract(edge.scale(width))})b.vertex(ps.last().pose(),(float)p.x,(float)p.y,(float)p.z).color((color>>16)&255,(color>>8)&255,color&255,(color>>>24)&255).endVertex();}
}
