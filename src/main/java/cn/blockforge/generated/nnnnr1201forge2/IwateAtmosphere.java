package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import static cn.blockforge.generated.nnnnr1201forge2.Client.ribbon;

/** 只在岩手近处添加稻穗轻摆、溪底咒力、树林光点与秋千运动。 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class IwateAtmosphere {
    private static final List<BlockPos> rice=new ArrayList<>();
    private static long scanned=-100;
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.level.dimension()!=SoulJourney.CORRIDOR||mc.player.getY()<1288||e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS)return;
        long tick=mc.level.getGameTime();Vec3 cam=e.getCamera().getPosition();float age=tick+e.getPartialTick();
        if(tick-scanned>=12||tick<scanned){scanned=tick;rice.clear();BlockPos base=mc.player.blockPosition();for(BlockPos p:BlockPos.betweenClosed(base.offset(-9,-5,-9),base.offset(9,5,9)))if(mc.level.getBlockState(p).is(SoulContent.RICE.get()))rice.add(p.immutable());}
        PoseStack ps=e.getPoseStack();ps.pushPose();ps.translate(-cam.x,-cam.y,-cam.z);
        RenderSystem.enableDepthTest();RenderSystem.depthMask(false);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        for(BlockPos pos:rice){double sway=Math.sin(age*0.025+pos.getX()*0.8+pos.getZ()*0.4)*0.07;Vec3 p=Vec3.atLowerCornerOf(pos).add(0.5,0.5,0.5),q=p.add(sway,0.25,0);ribbon(b,ps,p,q,0.012,0xFF8C9A42,cam);for(int i=0;i<3;i++){Vec3 grain=q.add(0.05*i,-0.035*i,0);ribbon(b,ps,grain,grain.add(0.035,-0.03,0),0.018,0xFFE0BD64,cam);}}
        for(int k=0;k<32;k++){int x=(int)Math.floor(cam.x/16)*16+(k%8-4)*9,z=(int)Math.floor(cam.z/16)*16+(k/8-2)*15;if(!IwateTerrain.forest(x,z))continue;Vec3 point=new Vec3(x,IwateVillage.height(x,z)+2+Math.sin(age*0.025+k)*0.3,z);double d=point.distanceTo(cam);if(d>7&&d<48)ribbon(b,ps,point,point.add(0,0.065,0),0.04,0xB4E1EAC3,cam);}
        // 秋千以附近真实的铜横梁定位，跟随结构放置位置和高度。
        BlockPos base=mc.player.blockPosition();for(BlockPos pos:BlockPos.betweenClosed(base.offset(-12,-2,-12),base.offset(12,8,12))){if(!mc.level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.EXPOSED_COPPER)||!mc.level.getBlockState(pos.below()).isAir()||!mc.level.getBlockState(pos.east()).is(net.minecraft.world.level.block.Blocks.EXPOSED_COPPER))continue;if(Math.floorMod(pos.getX(),4)!=0)continue;Vec3 pivot=Vec3.atCenterOf(pos),seat=pivot.add(0,-3.2,Math.sin(age*0.024)*0.22);ribbon(b,ps,pivot.add(-1,0,0),seat.add(-1,0,0),0.035,0xFF66594B,cam);ribbon(b,ps,pivot.add(1,0,0),seat.add(1,0,0),0.035,0xFF66594B,cam);ribbon(b,ps,seat.add(-1,0,0),seat.add(1,0,0),0.18,0xFF6D4E35,cam);}
        BufferUploader.drawWithShader(b.end());RenderSystem.enableCull();RenderSystem.disableBlend();RenderSystem.depthMask(true);ps.popPose();
    }
}
