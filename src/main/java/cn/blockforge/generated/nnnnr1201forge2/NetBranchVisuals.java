package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

/** 固定在世界坐标中的横向十字与实体月牙，不随镜头旋转。 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class NetBranchVisuals {
    private record Visual(Network.Effect effect,long born){}
    private record Hold(long id,long end){}
    private static final List<Visual> crosses=new ArrayList<>();
    private static final Map<Long,Visual> qi=new HashMap<>();
    private static final Map<Integer,Hold> holds=new HashMap<>();
    private static long tick;
    public static void collect(Vec3 center,double radius,List<Vec3> out){crosses.removeIf(v->{Vec3 at=v.effect.position().add(v.effect.direction().scale(NetBranchRules.crossTravel(tick-v.born)));if(at.distanceToSqr(center)>radius*radius)return false;if(out.size()<6000)out.add(at);return true;});qi.values().removeIf(v->{Vec3 at=v.effect.position().add(v.effect.direction().scale((tick-v.born)*NetBranchRules.QI_SPEED));if(at.distanceToSqr(center)>radius*radius)return false;if(out.size()<6000)out.add(at);return true;});}
    public static void clear(){crosses.clear();qi.clear();holds.clear();tick=0;}
    public static boolean locked(){var p=Minecraft.getInstance().player;return p!=null&&holds.containsKey(p.getId());}
    public static void tick(){tick++;crosses.removeIf(v->tick-v.born>=v.effect.life());qi.values().removeIf(v->tick-v.born>=v.effect.life());holds.values().removeIf(h->tick>=h.end);}
    public static void receive(Network.Effect e){
        if(e.kind()==NetBranchRules.FX_HOLD){if(e.life()>0)holds.put(e.owner(),new Hold(e.seed(),tick+e.life()));else{Hold h=holds.get(e.owner());if(h!=null&&h.id==e.seed())holds.remove(e.owner());}return;}
        if(e.kind()==NetBranchRules.FX_STOP){crosses.removeIf(v->v.effect.seed()==e.seed());qi.remove(e.seed());return;}
        if(e.kind()==NetBranchRules.FX_CROSS){if(crosses.size()<2048)crosses.add(new Visual(e,tick));}
        if(e.kind()==NetBranchRules.FX_QI){if(qi.size()<256||qi.containsKey(e.seed()))qi.put(e.seed(),new Visual(e,tick));}
    }
    @SubscribeEvent public static void input(MovementInputUpdateEvent event){if(!locked()||event.getEntity()!=Minecraft.getInstance().player)return;var input=event.getInput();input.forwardImpulse=input.leftImpulse=0;input.up=input.down=input.left=input.right=input.jumping=input.shiftKeyDown=false;event.getEntity().setDeltaMovement(Vec3.ZERO);}
    public static void draw(BufferBuilder b,PoseStack ps,Vec3 camera,float partial){
        // direction是角到对顶角的完整位移；在一半路程处穿过受控目标。
        for(Visual v:crosses){Network.Effect e=v.effect;SlashAppearance.use(e);double age=tick-v.born+partial;SlashAppearance.age(age);Vec3 p=e.position().add(e.direction().scale(NetBranchRules.crossTravel(age)));
            float fade=(float)Math.min(1,Math.max(0,(e.life()-age)/2));
            for(int arm=0;arm<2;arm++){
                Vec3 forward=e.direction().normalize(),side=NetBranches.side(forward),up=forward.cross(side).normalize();
                Vec3 axis=side.add(up.scale(arm==0?1:-1)).normalize();
                Client.monochromeCut(b,ps,p,axis,e.size(),fade,camera);
                Vec3 trail=e.position().add(e.direction().scale(NetBranchRules.crossTravel(Math.max(0,age-2))));
                SlashStyle.detail(b,ps,trail,p,0.03,((int)(90*fade)<<24)|0xFFFFFF,camera);
            }
        }
        for(Visual v:qi.values()){
            Network.Effect e=v.effect;SlashAppearance.use(e);float age=tick-v.born+partial;SlashAppearance.age(age);Vec3 direction=e.direction(),side=new Vec3(-direction.z,0,direction.x).normalize(),p=e.position().add(direction.scale(age*NetBranchRules.QI_SPEED));if(side.lengthSqr()<0.01)side=new Vec3(1,0,0);
            // 与服务端剑气半径一致的黑芯白边尖梭。
            SlashStyle.draw(b,ps,p.subtract(side.scale(NetBranchRules.QI_RADIUS)),p.add(side.scale(NetBranchRules.QI_RADIUS)),.4,Math.min(1,(e.life()-age)/3),camera);
        }
    }
    private static Vec3 point(Vec3 p,Vec3 forward,Vec3 side,double angle){return p.add(side.scale(NetBranchRules.qiSide(angle))).add(forward.cross(side).scale(NetBranchRules.qiBend(angle)));}
    private NetBranchVisuals(){}
}
