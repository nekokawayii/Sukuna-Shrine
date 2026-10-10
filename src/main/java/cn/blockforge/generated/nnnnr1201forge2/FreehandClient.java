package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** 绘制时释放鼠标，在真实分辨率画布上记录；定稿后恢复鼠标控制相机。 */
public final class FreehandClient {
    private static UUID id;private static int pixelWidth,pixelHeight,state;private static double pixels,acknowledged;
    private static final List<SlashFormRules.Segment> lines=new ArrayList<>(),batch=new ArrayList<>();
    private static boolean held,blockedUntilRelease;private static double lastX,lastY;private static boolean lastPoint;
    public static boolean canvasOpen(){return Minecraft.getInstance().screen instanceof Canvas;}
    public static boolean keyDown(){return ClientInput.held(Client.CAST);}
    private static boolean allowed(){var mc=Minecraft.getInstance();return mc.player!=null&&mc.player.isAlive()&&mc.level!=null&&mc.isWindowActive()&&Client.selected==SlashFormRules.FREEHAND&&Client.progress.sukuna()&&!Client.progress.burnout()&&!DomainClashClient.active()&&!NetBranchVisuals.locked()&&!DismantleMotionClient.locked();}
    private static void send(int operation,List<SlashFormRules.Segment> segments){var mc=Minecraft.getInstance();if(id!=null)Network.CHANNEL.sendToServer(new Network.DrawInput(id,operation,pixelWidth,pixelHeight,segments,mc.options.keyUp.isDown(),mc.options.keyDown.isDown(),mc.options.keyJump.isDown(),mc.options.keyShift.isDown()));}
    public static void receive(Network.DrawState m){if(id==null||!id.equals(m.id()))return;acknowledged=m.pixels();if(m.state()==0){clear();held=keyDown();blockedUntilRelease=held;return;}if(m.state()==3){trim(m.pixels());state=3;closeCanvas();} /* 只由本地按键更新绘制/暂停，避免延迟确认把暂停状态倒退。 */ }
    private static void trim(double accepted){double n=0;int count=0;for(var s:lines){if(n+s.length()>accepted+0.001)break;n+=s.length();count++;}while(lines.size()>count)lines.remove(lines.size()-1);pixels=n;batch.clear();}
    public static void clear(){id=null;state=0;held=false;blockedUntilRelease=false;lastPoint=false;pixels=acknowledged=0;lines.clear();batch.clear();closeCanvas();}
    private static void closeCanvas(){var mc=Minecraft.getInstance();if(mc.screen instanceof Canvas c){c.finished=true;mc.setScreen(null);}}
    public static void cancel(){send(FreehandDrawing.CANCEL,List.of());clear();held=keyDown();blockedUntilRelease=held;}
    public static void tick(boolean down){
        var mc=Minecraft.getInstance();if(id!=null&&(!allowed()||mc.getWindow().getWidth()!=pixelWidth||mc.getWindow().getHeight()!=pixelHeight||mc.screen!=null&&!(mc.screen instanceof Canvas))){cancel();return;}
        if(!allowed()){held=down;return;}
        if(blockedUntilRelease){if(!down)blockedUntilRelease=false;held=down;return;}
        if(id==null){if(down&&!held&&mc.screen==null){id=UUID.randomUUID();pixelWidth=mc.getWindow().getWidth();pixelHeight=mc.getWindow().getHeight();state=1;pixels=acknowledged=0;lines.clear();batch.clear();send(FreehandDrawing.BEGIN,List.of());mc.setScreen(new Canvas());beginStroke();}}
        else if(state==3){if(down&&!held){send(FreehandDrawing.FIRE,List.of());blockedUntilRelease=true;}}
        else {
            if(down&&!held){send(FreehandDrawing.RESUME,List.of());state=1;beginStroke();}
            if(!down&&held){flush();send(FreehandDrawing.PAUSE,List.of());state=2;lastPoint=false;}
            flush();
        }
        held=down;
    }
    private static void flush(){if(!batch.isEmpty()){send(FreehandDrawing.SEGMENTS,List.copyOf(batch));batch.clear();}}
    private static void beginStroke(){var mc=Minecraft.getInstance();lastPoint=false;sample(mc.mouseHandler.xpos()*pixelWidth/mc.getWindow().getScreenWidth()-pixelWidth/2.0,mc.mouseHandler.ypos()*pixelHeight/mc.getWindow().getScreenHeight()-pixelHeight/2.0);}
    private static void sample(double x,double y){
        if(id==null||state!=1||!keyDown()){lastPoint=false;return;}
        x=Math.max(-pixelWidth/2.0,Math.min(pixelWidth/2.0,Math.rint(x)));y=Math.max(-pixelHeight/2.0,Math.min(pixelHeight/2.0,Math.rint(y)));
        if(!lastPoint){lastX=x;lastY=y;lastPoint=true;return;}
        double length=Math.hypot(x-lastX,y-lastY);if(length<0.000001)return;
        double room=Math.min(SlashFormRules.MAX_PIXELS-pixels,Math.max(0,Client.progress.energy()-Math.max(0,pixels-acknowledged)));
        if(length>room){double f=room/length;x=lastX+(x-lastX)*f;y=lastY+(y-lastY)*f;length=room;}
        if(length>0&&lines.size()<SlashFormRules.MAX_SEGMENTS){var s=new SlashFormRules.Segment(lastX,lastY,x,y);lines.add(s);batch.add(s);pixels+=length;lastX=x;lastY=y;if(batch.size()>=64)flush();}
        if(pixels>=SlashFormRules.MAX_PIXELS-0.001||room<=length+0.001||lines.size()>=SlashFormRules.MAX_SEGMENTS){flush();send(FreehandDrawing.PAUSE,List.of());state=2;lastPoint=false;}
    }
    public static void hud(GuiGraphics g){if(id==null||state!=3)return;var mc=Minecraft.getInstance();g.drawCenteredString(mc.font,"纵心已定稿 · "+(int)pixels+" / 2000 像素 · "+String.format(Locale.ROOT,"%.0f",SlashFormRules.freehandDamage(pixels)*100)+"%解 · 再按 "+Client.CAST.getTranslatedKeyMessage().getString()+" 斩出",mc.getWindow().getGuiScaledWidth()/2,mc.getWindow().getGuiScaledHeight()-62,0xFFFFCDE9);}
    public static void preview(BufferBuilder b,PoseStack ps,Vec3 camera){
        var mc=Minecraft.getInstance();if(id==null||state!=3||mc.player==null)return;
        SlashAppearance.preview();Vec3 dir=mc.player.getLookAngle();
        Vec3 side=NetBranches.side(dir),up=side.cross(dir).normalize(),center=camera.add(dir.scale(8));
        for(var s:SlashFormRules.centered(lines)){Vec3 a=center.add(side.scale(s.x1()*12/pixelHeight)).subtract(up.scale(s.y1()*12/pixelHeight)),z=center.add(side.scale(s.x2()*12/pixelHeight)).subtract(up.scale(s.y2()*12/pixelHeight));SlashStyle.draw(b,ps,a,z,.05,.9f,camera);}
    }
    private static final class Canvas extends Screen {
        boolean finished;
        Canvas(){super(Component.literal("纵心 · 绘制斩击"));}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void tick(){FreehandClient.tick(keyDown());}
        @Override public void mouseMoved(double x,double y){sample(x*pixelWidth/width-pixelWidth/2.0,y*pixelHeight/height-pixelHeight/2.0);}
        @Override public boolean keyPressed(int key,int scan,int mods){ClientInput.key(key,scan,GLFW.GLFW_PRESS,mods);if(Client.CAST.matches(key,scan)&&keyDown()){FreehandClient.tick(true);return true;}return super.keyPressed(key,scan,mods);}
        @Override public boolean keyReleased(int key,int scan,int mods){ClientInput.key(key,scan,GLFW.GLFW_RELEASE,mods);if(Client.CAST.matches(key,scan)){FreehandClient.tick(false);return true;}return super.keyReleased(key,scan,mods);}
        // 鼠标由 InputEvent.MouseButton.Post 统一记账，避免不同鼠标键串到释放键。
        @Override public boolean mouseClicked(double x,double y,int button){return Client.CAST.matchesMouse(button)||super.mouseClicked(x,y,button);}
        @Override public boolean mouseReleased(double x,double y,int button){return Client.CAST.matchesMouse(button)||super.mouseReleased(x,y,button);}
        @Override public void removed(){if(!finished){send(FreehandDrawing.CANCEL,List.of());id=null;state=0;lines.clear();batch.clear();}}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){
            for(var s:lines)line(g,(s.x1()+pixelWidth/2.0)*width/pixelWidth,(s.y1()+pixelHeight/2.0)*height/pixelHeight,(s.x2()+pixelWidth/2.0)*width/pixelWidth,(s.y2()+pixelHeight/2.0)*height/pixelHeight);
            g.drawCenteredString(font,state==2?"已暂停 · 1.5秒内再按释放键继续画，超时自动定稿":"按住 "+Client.CAST.getTranslatedKeyMessage().getString()+" 移动鼠标画斩击 · 松手不画 · Esc取消",width/2,12,0xFFFFDAEC);
            g.drawCenteredString(font,(int)pixels+" / 2000像素 · 每像素1咒力 · 每100像素50%解",width/2,26,0xFFE8D8FF);
        }
        private void line(GuiGraphics g,double x,double y,double z,double w){int n=Math.max(1,(int)Math.ceil(Math.hypot(z-x,w-y)));for(int i=0;i<=n;i++){int px=(int)Math.round(x+(z-x)*i/n),py=(int)Math.round(y+(w-y)*i/n);g.fill(px,py,px+2,py+2,0xFFFFDDEE);}}
    }
    private FreehandClient(){}
}
