package cn.blockforge.generated.nnnnr1201forge2;

import com.sun.jna.*;
import com.sun.jna.win32.StdCallLibrary;
import java.awt.image.BufferedImage;
import java.util.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFWNativeWin32;

/** Windows 分层无边框窗；仅本战临时创建，不截屏、不注入输入、不改变其他程序。 */
public final class ObserverDesktop {
    public interface Win extends StdCallLibrary {
        Pointer CreateWindowExW(int ex,WString type,WString title,int style,int x,int y,int w,int h,Pointer parent,Pointer menu,Pointer instance,Pointer param);
        boolean DestroyWindow(Pointer h);boolean ShowWindow(Pointer h,int cmd);
        Pointer GetDC(Pointer h);int ReleaseDC(Pointer h,Pointer dc);
        boolean UpdateLayeredWindow(Pointer h,Pointer dc,Point at,Size size,Pointer source,Point origin,int key,Blend blend,int flags);
        boolean GetCursorPos(Point point);boolean GetClientRect(Pointer h,Rect rect);boolean ClientToScreen(Pointer h,Point point);
        Pointer GetForegroundWindow();
    }
    public interface Gdi extends StdCallLibrary {
        Pointer CreateCompatibleDC(Pointer dc);boolean DeleteDC(Pointer dc);boolean DeleteObject(Pointer object);
        Pointer SelectObject(Pointer dc,Pointer object);
        Pointer CreateDIBSection(Pointer dc,Bitmap info,int usage,com.sun.jna.ptr.PointerByReference pixels,Pointer section,int offset);
    }
    @Structure.FieldOrder({"x","y"}) public static class Point extends Structure {public int x,y;public Point(){}public Point(int x,int y){this.x=x;this.y=y;}}
    @Structure.FieldOrder({"w","h"}) public static class Size extends Structure {public int w,h;public Size(int w,int h){this.w=w;this.h=h;}}
    @Structure.FieldOrder({"left","top","right","bottom"}) public static class Rect extends Structure {public int left,top,right,bottom;}
    @Structure.FieldOrder({"op","flags","alpha","format"}) public static class Blend extends Structure {public byte op=0,flags=0,alpha=(byte)255,format=1;}
    @Structure.FieldOrder({"size","width","height","planes","bits","compression","imageSize","xppm","yppm","used","important","color"})
    public static class Bitmap extends Structure {public int size=40,width,height;public short planes=1,bits=32;public int compression,imageSize,xppm,yppm,used,important,color;}
    public record View(int x,int y,int width,int height,int mouseX,int mouseY,boolean focused) {
        public float cursorX(){return (mouseX-x)/(float)Math.max(1,width);}
        public float cursorY(){return (mouseY-y)/(float)Math.max(1,height);}
    }
    private static Win win;private static Gdi gdi;private static boolean broken;
    private static final Map<Integer,Pane> panes=new HashMap<>();
    private static BufferedImage yuji,boss;
    private static View view;
    private static int anchorX,anchorY,anchorWidth,anchorHeight;
    private static boolean anchored;
    private static long updated;
    private static final Map<Integer,int[]> previousRed=new HashMap<>();
    private static final Set<Integer> pushed=new HashSet<>();
    private static final class Pane {
        Pointer handle,dc,bitmap,previous,pixels;int w,h,x,y;boolean visible;
        Pane(int width,int height){
            w=width;h=height;
            try{
                // LAYERED | NOACTIVATE | TOOLWINDOW | TOPMOST | TRANSPARENT：不抢焦点、不拦截桌面操作。
                handle=win.CreateWindowExW(0x080800A8,new WString("STATIC"),new WString("Minecraft"),0x80000000,0,0,w,h,null,null,null,null);
                if(handle==null)throw new IllegalStateException("CreateWindowExW");
                dc=gdi.CreateCompatibleDC(null);if(dc==null)throw new IllegalStateException("CreateCompatibleDC");
                Bitmap info=new Bitmap();info.width=w;info.height=-h;info.write();
                var reference=new com.sun.jna.ptr.PointerByReference();bitmap=gdi.CreateDIBSection(dc,info,0,reference,null,0);pixels=reference.getValue();
                if(bitmap==null||pixels==null)throw new IllegalStateException("CreateDIBSection");previous=gdi.SelectObject(dc,bitmap);
            }catch(RuntimeException|LinkageError ex){close();throw ex;}
        }
        void show(int x,int y,BufferedImage image){
            this.x=x;this.y=y;int[] data=image.getRGB(0,0,w,h,null,0,w);
            for(int i=0;i<data.length;i++){int c=data[i],a=c>>>24;data[i]=(a<<24)|(((c>>16&255)*a/255)<<16)|(((c>>8&255)*a/255)<<8)|((c&255)*a/255);}
            pixels.write(0,data,0,data.length);
            if(!win.UpdateLayeredWindow(handle,null,new Point(x,y),new Size(w,h),dc,new Point(0,0),0,new Blend(),2))throw new IllegalStateException("UpdateLayeredWindow");
            if(!visible){win.ShowWindow(handle,4);visible=true;}
        }
        void hide(){if(visible){win.ShowWindow(handle,0);visible=false;}}
        boolean contains(int mx,int my){return visible&&mx>=x&&mx<x+w&&my>=y&&my<y+h;}
        void close(){if(handle!=null){win.DestroyWindow(handle);handle=null;}if(dc!=null&&previous!=null)gdi.SelectObject(dc,previous);if(bitmap!=null)gdi.DeleteObject(bitmap);if(dc!=null)gdi.DeleteDC(dc);dc=bitmap=previous=pixels=null;}
    }
    public static boolean available(){
        if(broken||!Platform.isWindows())return false;
        if(win==null)try{win=Native.load("user32",Win.class);gdi=Native.load("gdi32",Gdi.class);}catch(RuntimeException|LinkageError ex){fail(ex);return false;}
        return true;
    }
    public static View sample(){
        if(!available())return null;
        try{
            long handle=GLFWNativeWin32.glfwGetWin32Window(Minecraft.getInstance().getWindow().getWindow());Pointer h=new Pointer(handle);
            Rect r=new Rect();Point origin=new Point(),mouse=new Point();
            if(!win.GetClientRect(h,r)||!win.ClientToScreen(h,origin)||!win.GetCursorPos(mouse))return null;
            if(!anchored){anchorX=origin.x;anchorY=origin.y;anchorWidth=Math.max(1,r.right);anchorHeight=Math.max(1,r.bottom);anchored=true;}
            // 桌面弹幕固定在屏幕坐标，玩家挪窗口不会把整片弹幕一起拖走。
            view=new View(anchorX,anchorY,anchorWidth,anchorHeight,mouse.x,mouse.y,h.equals(win.GetForegroundWindow()));return view;
        }catch(RuntimeException|LinkageError ex){fail(ex);return null;}
    }
    public static boolean active(){return !broken&&ObserverWindow.desktopAllowed()&&view!=null&&view.focused&&System.nanoTime()-updated<500_000_000L;}
    public static void portrait(BufferedImage image,boolean isBoss){if(isBoss)boss=image;else yuji=image;}
    public static void tick(ObserverPackets.State state){
        if(!ObserverWindow.desktopAllowed()||state.phase()!=1||state.hits()<=5){clear();return;}
        View v=sample();if(v==null||!v.focused){hide();return;}
        try{
            Set<Integer> alive=new HashSet<>();
            if(state.attackLeft()==0&&state.staggerLeft()==0)for(var b:state.blades()){
                alive.add(b.id());
                int cx=v.x+Math.round(b.x()*v.width),cy=v.y+Math.round(b.y()*v.height);
                if(b.style()==11){
                    double length=Math.max(.00001,Math.hypot(b.vx(),b.vy()));int tx=cx-(int)(b.vx()/length*.38*v.width),ty=cy-(int)(b.vy()/length*.38*v.height);
                    int x=Math.min(cx,tx)-12,y=Math.min(cy,ty)-12,w=Math.abs(cx-tx)+24,h=Math.abs(cy-ty)+24;
                    Pane pane=panes.get(b.id());if(pane==null||pane.w!=w||pane.h!=h){if(pane!=null)pane.close();pane=new Pane(w,h);panes.put(b.id(),pane);}
                    BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
                    try{g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,java.awt.RenderingHints.VALUE_ANTIALIAS_ON);g.setStroke(new java.awt.BasicStroke(18,1,1));g.setColor(new java.awt.Color(150,0,20,95));g.drawLine(cx-x,cy-y,tx-x,ty-y);g.setStroke(new java.awt.BasicStroke(7,1,1));g.setColor(new java.awt.Color(235,8,38,230));g.drawLine(cx-x,cy-y,tx-x,ty-y);g.setStroke(new java.awt.BasicStroke(2,1,1));g.setColor(new java.awt.Color(255,200,205));g.drawLine(cx-x,cy-y,tx-x,ty-y);}finally{g.dispose();}
                    pane.show(x,y,image);
                    if(ObserverBattleRules.touches(b,v.cursorX(),v.cursorY()))ObserverFight.input(ObserverBattleRules.CONTACT,b.id(),v.cursorX(),v.cursorY());
                }else{
                    Pane pane=panes.computeIfAbsent(b.id(),id->new Pane(64,64));pane.show(cx-32,cy-32,ObserverGlyph.image(b.style(),64,state.age()));
                    if(pane.contains(v.mouseX,v.mouseY))ObserverFight.input(ObserverBattleRules.CONTACT,b.id(),v.cursorX(),v.cursorY());
                    if(b.style()==2)ObserverWindow.attract(cx,cy);
                    if(b.style()==3){
                        int[] old=previousRed.put(b.id(),new int[]{cx,cy});int[] rect=ObserverWindow.rectangle();
                        if(old!=null&&!pushed.contains(b.id())&&crosses(old[0],old[1],cx,cy,rect)) {pushed.add(b.id());ObserverWindow.push(b.vx(),b.vy());}
                    }
                }
            }
            if(state.attack()==ObserverBattleRules.MELEE&&state.attackLeft()>0){
                alive.add(-2);Pane pane=panes.computeIfAbsent(-2,id->new Pane(256,256));
                BufferedImage strike=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);var graphics=strike.createGraphics();
                try{
                    if(boss!=null)graphics.drawImage(boss,48,28,null);
                    graphics.setColor(new java.awt.Color(255,24,56));graphics.setStroke(new java.awt.BasicStroke(3));
                    int rx=Math.min(125,Math.round(v.width*ObserverBattleRules.MELEE_RADIUS)),ry=Math.min(125,Math.round(v.height*ObserverBattleRules.MELEE_RADIUS));
                    graphics.drawOval(128-rx,128-ry,rx*2,ry*2);
                }finally{graphics.dispose();}
                pane.show(v.x+Math.round(state.meleeX()*v.width)-128,v.y+Math.round(state.meleeY()*v.height)-128,strike);
            }
            if(yuji!=null){
                alive.add(-1);Pane pane=panes.computeIfAbsent(-1,id->new Pane(160,200));
                int x=v.x+Math.round(v.width*1.10f)-80,y=v.y+Math.round(v.height*.55f)-100;
                // 显示区不够时留在工作区内；碰撞使用实际归一化位置并由服务端确认。
                int[] bounds=ObserverWindow.workarea();x=Math.max(bounds[0],Math.min(bounds[0]+bounds[2]-160,x));y=Math.max(bounds[1],Math.min(bounds[1]+bounds[3]-200,y));
                pane.show(x,y,yuji);
                if(state.attackLeft()==0&&pane.contains(v.mouseX,v.mouseY))
                    ObserverFight.input(ObserverBattleRules.CONTACT,-1,1.10f+(v.mouseX-x-80)/(float)v.width,.55f+(v.mouseY-y-100)/(float)v.height);
            }
            int cursorIndex=0;for(var cursor:ObserverFight.otherCursors()){
                int id=-100-cursorIndex++;alive.add(id);Pane pane=panes.computeIfAbsent(id,key->new Pane(160,36));
                BufferedImage image=new BufferedImage(160,36,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
                try{g.setColor(java.awt.Color.getHSBColor((cursor.player().hashCode()&255)/255f,.65f,1));g.fillPolygon(new int[]{1,1,13,7,6},new int[]{1,20,13,12,23},5);g.setFont(new java.awt.Font("Dialog",java.awt.Font.BOLD,13));g.drawString(cursor.name(),17,15);}finally{g.dispose();}
                pane.show(v.x+Math.round(cursor.x()*v.width),v.y+Math.round(cursor.y()*v.height),image);
            }
            var it=panes.entrySet().iterator();while(it.hasNext()){var entry=it.next();if(!alive.contains(entry.getKey())){entry.getValue().close();it.remove();}}
            previousRed.keySet().retainAll(alive);pushed.retainAll(alive);
            updated=System.nanoTime();
        }catch(RuntimeException|LinkageError ex){fail(ex);}
    }
    private static boolean crosses(int ax,int ay,int bx,int by,int[] r){
        return java.awt.geom.Line2D.linesIntersect(ax,ay,bx,by,r[0],r[1],r[0]+r[2],r[1])
            ||java.awt.geom.Line2D.linesIntersect(ax,ay,bx,by,r[0],r[1]+r[3],r[0]+r[2],r[1]+r[3])
            ||java.awt.geom.Line2D.linesIntersect(ax,ay,bx,by,r[0],r[1],r[0],r[1]+r[3])
            ||java.awt.geom.Line2D.linesIntersect(ax,ay,bx,by,r[0]+r[2],r[1],r[0]+r[2],r[1]+r[3]);
    }
    private static void hide(){for(Pane pane:panes.values())pane.hide();updated=0;}
    public static void clear(){for(Pane pane:panes.values())pane.close();panes.clear();previousRed.clear();pushed.clear();updated=0;view=null;anchored=false;}
    private static void fail(Throwable ex){broken=true;clear();com.mojang.logging.LogUtils.getLogger().warn("高位桌面窗口不可用，退回游戏内绘制：{}",ex.toString());}
    private ObserverDesktop(){}
}
