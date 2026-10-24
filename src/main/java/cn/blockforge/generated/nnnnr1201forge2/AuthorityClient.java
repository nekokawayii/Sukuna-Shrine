package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector4f;

/** 编辑器保留世界画面，用鼠标点击投影后的真实生物；红框不修改队伍颜色。 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class AuthorityClient {
    private static AuthorityPackets.State state=new AuthorityPackets.State(false,0,0,new int[0],new int[0]);
    private record Box(int id,int left,int top,int right,int bottom,double distance){}
    private static final List<Box> boxes=new ArrayList<>();
    private static final Set<Integer> targets=new HashSet<>();
    public static boolean enabled(){return state.enabled();}
    public static void setup(){AuthorityPackets.receiver=s->{state=s;targets.clear();for(int id:s.targets())targets.add(id);};AuthorityPackets.openReceiver=()->Minecraft.getInstance().setScreen(new EditorScreen());}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){state=new AuthorityPackets.State(false,0,0,new int[0],new int[0]);targets.clear();boxes.clear();}
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        boxes.clear();var mc=Minecraft.getInstance();if(!enabled()||mc.level==null||mc.player==null)return;
        boolean editor=mc.screen instanceof EditorScreen;
        var pose=e.getPoseStack();Vec3 camera=e.getCamera().getPosition();
        var buffers=mc.renderBuffers().bufferSource();var lines=buffers.getBuffer(RenderType.lines());
        for(var entity:mc.level.entitiesForRendering())if(entity instanceof LivingEntity t&&t!=mc.player&&t.isAlive()&&!t.isInvisible()&&t.distanceToSqr(mc.player)<96*96){
            AABB bb=t.getBoundingBox().move(t.getPosition(e.getPartialTick()).subtract(t.position())).inflate(.12);
            if(targets.contains(t.getId()))LevelRenderer.renderLineBox(pose,lines,bb.move(camera.scale(-1)),1,.05f,.1f,1);
            if(!editor||!mc.player.hasLineOfSight(t))continue;
            float left=Float.MAX_VALUE,top=Float.MAX_VALUE,right=-Float.MAX_VALUE,bottom=-Float.MAX_VALUE;boolean visible=true;
            for(int i=0;i<8;i++){Vector4f v=new Vector4f((float)((i&1)==0?bb.minX-camera.x:bb.maxX-camera.x),(float)((i&2)==0?bb.minY-camera.y:bb.maxY-camera.y),(float)((i&4)==0?bb.minZ-camera.z:bb.maxZ-camera.z),1);v.mul(pose.last().pose()).mul(e.getProjectionMatrix());if(v.w<=.01f){visible=false;break;}float x=(v.x/v.w*.5f+.5f)*mc.getWindow().getGuiScaledWidth(),y=(.5f-v.y/v.w*.5f)*mc.getWindow().getGuiScaledHeight();left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}
            if(visible)boxes.add(new Box(t.getId(),(int)left,(int)top,(int)right,(int)bottom,t.distanceToSqr(mc.player)));
        }
        buffers.endBatch(RenderType.lines());
    }
    private static final class EditorScreen extends Screen {
        private int chosen,page;
        EditorScreen(){super(Component.literal("世界权限编辑器"));chosen=state.skills().length==0?0:state.skills()[0];}
        @Override public boolean isPauseScreen(){return false;}
        @Override protected void init(){rebuild();}
        private void rebuild(){
            clearWidgets();int w=Math.min(188,width-12),x=width-w-6,y=20;
            int rows=Math.max(1,Math.min(6,(height-182)/22));int start=page*rows;
            if(start>=state.skills().length){page=0;start=0;}
            for(int i=start;i<Math.min(start+rows,state.skills().length);i++){int id=state.skills()[i];addRenderableWidget(Button.builder(Component.literal((chosen==id?"▶ ":"")+AuthoritySkills.name(id)),b->{chosen=id;rebuild();}).bounds(x,y,w,20).build());y+=22;}
            addRenderableWidget(Button.builder(Component.literal("上一页"),b->{page=Math.max(0,page-1);rebuild();}).bounds(x,y,w/2-2,20).build());
            addRenderableWidget(Button.builder(Component.literal("下一页"),b->{if((page+1)*rows<state.skills().length)page++;rebuild();}).bounds(x+w/2+2,y,w/2-2,20).build());y+=24;
            addRenderableWidget(Button.builder(Component.literal("刻写主手"),b->{AuthorityPackets.send(AuthorityPackets.INSCRIBE,chosen,0);onClose();}).bounds(x,y,w/2-2,20).build());
            addRenderableWidget(Button.builder(Component.literal("刻写副手"),b->{AuthorityPackets.send(AuthorityPackets.INSCRIBE,chosen,1);onClose();}).bounds(x+w/2+2,y,w/2-2,20).build());y+=24;
            addRenderableWidget(Button.builder(Component.literal("全部收回"),b->AuthorityPackets.send(AuthorityPackets.RECALL,0,0)).bounds(x,y,w/2-2,20).build());
            addRenderableWidget(Button.builder(Component.literal("全部放出"),b->AuthorityPackets.send(AuthorityPackets.RELEASE,0,0)).bounds(x+w/2+2,y,w/2-2,20).build());y+=24;
            addRenderableWidget(Button.builder(Component.literal("清除标记"),b->AuthorityPackets.send(AuthorityPackets.CLEAR,0,0)).bounds(x,y,w/2-2,20).build());
            addRenderableWidget(Button.builder(Component.literal("补领权限道具"),b->AuthorityPackets.send(AuthorityPackets.REPLACE_TOOLS,0,0)).bounds(x+w/2+2,y,w/2-2,20).build());
        }
        @Override public void render(GuiGraphics g,int mx,int my,float partial){
            g.fill(width-Math.min(200,width),0,width,height,0xDD160D22);
            g.drawString(font,title,8,8,0xFFFF536C);
            g.drawString(font,"军团 "+state.members()+" · 魂番内 "+state.stored(),8,22,0xFFDDC5EE);
            g.drawString(font,"界面内点击生物：伤害 + 随机十倍咒术",8,height-26,0xFFFFB4C6);
            g.drawString(font,"Esc 返回准星视角",8,height-14,0xFFCCCCCC);
            boxes.stream().filter(b->mx>=b.left&&mx<=b.right&&my>=b.top&&my<=b.bottom).min(Comparator.comparingDouble(Box::distance)).ifPresent(b->g.renderOutline(b.left,b.top,Math.max(1,b.right-b.left),Math.max(1,b.bottom-b.top),0xFFFF193E));
            super.render(g,mx,my,partial);
        }
        @Override public boolean mouseClicked(double x,double y,int button){
            if(super.mouseClicked(x,y,button))return true;if(button!=0)return false;
            if(x>=width-Math.min(200,width))return true;
            boxes.stream().filter(b->x>=b.left&&x<=b.right&&y>=b.top&&y<=b.bottom).min(Comparator.comparingDouble(Box::distance)).ifPresent(b->AuthorityPackets.send(AuthorityPackets.STRIKE,b.id,0));return true;
        }
        @Override public void tick(){if(!enabled()||minecraft.player==null||!Authority.editorHeld(minecraft.player))onClose();}
    }
    private AuthorityClient(){}
}
