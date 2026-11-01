package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.*;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityRenderersEvent;
import java.util.*;

/** 视觉只读取同步状态，不生成可拾取实体；离开视距后发射器自行到期。 */
public final class RitualClient implements BlockEntityRenderer<RitualBlockEntity> {
    private final Map<net.minecraft.core.BlockPos,Long> pulses=new HashMap<>();
    private Object lastLevel;
    private static Object progressConnection;
    private static int scrollMask;
    public static void progress(ExpeditionProgress.State s){progressConnection=Minecraft.getInstance().getConnection();scrollMask=s.scrollMask();}
    public static boolean ultimate(){return progressConnection!=null&&progressConnection==Minecraft.getInstance().getConnection()&&(scrollMask&4)!=0;}
    public RitualClient(BlockEntityRendererProvider.Context context){}
    public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(ExpeditionContent.RITUAL.get(),RitualClient::new);}
    @Override public void render(RitualBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(be.getLevel()==null)return;var mc=Minecraft.getInstance();long tick=be.getLevel().getGameTime();
        if(lastLevel!=be.getLevel()){pulses.clear();lastLevel=be.getLevel();}
        var item=be.display();float progress=be.recipe()==null?1:Math.min(1,be.elapsed/(float)be.recipe().ticks());
        if(!item.isEmpty()){
            pose.pushPose();double y=be.tier()==1?.8+progress*.35:1.1+progress*.7;
            pose.translate(.5,y+Math.sin((tick+partial)*.05)*.08,.5);pose.mulPose(Axis.YP.rotationDegrees((tick+partial)*2));pose.scale(.65f,.65f,.65f);
            mc.getItemRenderer().renderStatic(item,ItemDisplayContext.GROUND,15728880,OverlayTexture.NO_OVERLAY,pose,buffers,be.getLevel(),0);pose.popPose();
        }
        if(!be.running()||mc.player==null||mc.player.distanceToSqr(Vec3.atCenterOf(be.getBlockPos()))>64*64)return;
        if(tick-pulses.getOrDefault(be.getBlockPos(),-100L)<10)return;pulses.put(be.getBlockPos(),tick);
        if(pulses.size()>128){pulses.entrySet().removeIf(e->tick-e.getValue()>200);while(pulses.size()>128)pulses.remove(pulses.keySet().iterator().next());}
        Vec3 at=Vec3.atBottomCenterOf(be.getBlockPos());boolean photon=net.minecraftforge.fml.ModList.get().isLoaded("photon");
        if(be.tier()==0){for(int[] d:RitualBlockEntity.FOUR){Vec3 start=at.add(d[0],1,d[1]),end=at.add(0,1.4+progress*.4,0);if(photon)PhotonFx.ritualBeam(start,end,.07,0xDDB05FFF,true,12);else line(start,end,0xAA55FF,.12f);} }
        else if(be.tier()==1){
            if(photon)PhotonFx.sphere(at.add(0,1.4,0),1.8,1.3,0x702C8D16,0,"textures/effect/dust.png",false,12,30,4,.1);
            for(int i=0;i<14;i++)be.getLevel().addParticle(ParticleTypes.SNEEZE,at.x+(Math.random()-.5)*2,at.y+1,at.z+(Math.random()-.5)*2,0,.035,0);
        }else{
            for(int[] d:RitualBlockEntity.EIGHT){Vec3 node=at.add(d[0],3,d[1]);if(photon)PhotonFx.spark(node,.2,.32,0xFFD5CCFF,0xFF8A57FF,"textures/effect/glow.png",true,12,10,2,0);else be.getLevel().addParticle(ParticleTypes.END_ROD,node.x,node.y,node.z,0,.03,0);}
            if(photon&&be.elapsed%20<10)PhotonFx.sphere(at.add(0,35,0),9,6,0x55260511,0,"textures/effect/dust.png",false,20,30,2,.08);
        }
    }
    private static void line(Vec3 a,Vec3 b,int rgb,float size){var l=Minecraft.getInstance().level;if(l==null)return;var dust=new DustParticleOptions(new org.joml.Vector3f(((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f),size*5);for(int i=0;i<32;i++){Vec3 p=a.lerp(b,i/31.0);l.addParticle(dust,p.x,p.y,p.z,0,0,0);}}
    public static void lightning(Network.Effect effect){
        Vec3 ground=effect.position(),top=ground.add(0,45,0);Random random=new Random(effect.seed());boolean photon=net.minecraftforge.fml.ModList.get().isLoaded("photon");
        for(int i=1;i<=9;i++){Vec3 next=i==9?ground:ground.add((random.nextDouble()-.5)*6,45-i*5,(random.nextDouble()-.5)*6);
            if(photon){PhotonFx.ritualBeam(top,next,.4,0xDA9E072C,true,12);PhotonFx.ritualBeam(top,next,.17,0xF0080208,false,12);}else line(top,next,0xA30928,.3f);top=next;
        }
        if(photon)PhotonFx.shockwave(ground,0xDDB00830,5,12);Client.shake(10);
    }
}
