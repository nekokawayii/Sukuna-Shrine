package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** 只控制本游戏窗口；所有退出路径恢复，F8 始终有效，不限制关闭或切换程序。 */
public final class ObserverWindow {
    private static boolean saved,fullscreen,enabled=true,windowed,pauseOnFocus;
    private static int x,y,width,height,baseX,baseY,age,shake,lastX,lastY,lastWidth,lastHeight,tug;
    private static float tugX,tugY;
    public static void capture(){
        if(saved)return;var mc=Minecraft.getInstance();var w=mc.getWindow();long h=w.getWindow();
        int[] a={0},b={0};GLFW.glfwGetWindowPos(h,a,b);x=a[0];y=b[0];
        GLFW.glfwGetWindowSize(h,a,b);width=a[0];height=b[0];fullscreen=w.isFullscreen();pauseOnFocus=mc.options.pauseOnLostFocus;saved=true;
    }
    public static boolean desktopAllowed(){return saved&&enabled&&windowed&&!Minecraft.getInstance().getWindow().isFullscreen();}
    public static int[] workarea(){
        long h=Minecraft.getInstance().getWindow().getWindow();int[] x={0},y={0};GLFW.glfwGetWindowPos(h,x,y);
        long selected=GLFW.glfwGetPrimaryMonitor();var monitors=GLFW.glfwGetMonitors();
        if(monitors!=null)for(int i=0;i<monitors.limit();i++){
            long m=monitors.get(i);int[] mx={0},my={0},mw={0},mh={0};GLFW.glfwGetMonitorWorkarea(m,mx,my,mw,mh);
            if(x[0]>=mx[0]&&x[0]<mx[0]+mw[0]&&y[0]>=my[0]&&y[0]<my[0]+mh[0]){selected=m;break;}
        }
        int[] px={0},py={0},pw={1920},ph={1080};if(selected!=0)GLFW.glfwGetMonitorWorkarea(selected,px,py,pw,ph);return new int[]{px[0],py[0],pw[0],ph[0]};
    }
    public static void windowed(){
        if(windowed||!enabled)return;capture();var mc=Minecraft.getInstance();var w=mc.getWindow();
        if(w.isFullscreen())w.toggleFullScreen();
        int[] area=workarea();lastWidth=Math.max(320,Math.min(800,area[2]-200));lastHeight=Math.max(240,Math.min(600,area[3]-100));
        w.setWindowed(lastWidth,lastHeight);GLFW.glfwSetWindowSize(w.getWindow(),lastWidth,lastHeight);
        baseX=area[0]+Math.max(0,(area[2]-lastWidth-180)/2);baseY=area[1]+Math.max(30,(area[3]-lastHeight)/2);
        GLFW.glfwSetWindowPos(w.getWindow(),baseX,baseY);lastX=baseX;lastY=baseY;windowed=true;
        mc.options.pauseOnLostFocus=false;
    }
    public static void impact(){if(enabled&&windowed)shake=18;}
    public static void technique(String name){
        if(!desktopAllowed())return;
        if(name.contains("最大功率")||name.contains("終式"))impact();
    }
    public static void drag(double dx,double dy){
        if(!desktopAllowed())return;var w=Minecraft.getInstance().getWindow();int[] a={0},b={0};GLFW.glfwGetWindowPos(w.getWindow(),a,b);
        double scale=w.getGuiScale();move(a[0]+(int)(dx*scale),b[0]+(int)(dy*scale));
    }
    private static void move(int nx,int ny){
        int[] r=workarea();nx=Math.max(r[0],Math.min(r[0]+Math.max(0,r[2]-lastWidth),nx));ny=Math.max(r[1]+28,Math.min(r[1]+Math.max(28,r[3]-lastHeight),ny));
        GLFW.glfwSetWindowPos(Minecraft.getInstance().getWindow().getWindow(),nx,ny);
    }
    public static void tick(){
        if(!saved||!enabled||!windowed)return;age++;
        var mc=Minecraft.getInstance();long h=mc.getWindow().getWindow();
        if(mc.getWindow().isFullscreen()){shake=0;ObserverDesktop.clear();return;}
        int[] a={0},b={0};GLFW.glfwGetWindowPos(h,a,b);
        if(shake>0){
            if(!mc.isWindowActive())shake=1;
            int n=--shake;
            move(baseX+(int)(Math.sin(age*2.3)*n),baseY+(int)(Math.cos(age*1.8)*n*.6));
            if(n==17)GLFW.glfwSetWindowSize(h,lastWidth-12,lastHeight-8);
            if(n==0){move(baseX,baseY);GLFW.glfwSetWindowSize(h,lastWidth,lastHeight);}
            GLFW.glfwGetWindowPos(h,a,b);lastX=a[0];lastY=b[0];
        }else if(tug>0&&mc.isWindowActive()){
            --tug;move(a[0]+Math.round(tugX),b[0]+Math.round(tugY));GLFW.glfwGetWindowPos(h,a,b);baseX=lastX=a[0];baseY=lastY=b[0];
        }else{
            tug=0;
            if(a[0]!=lastX||b[0]!=lastY)ObserverFight.input(ObserverBattleRules.WINDOW_MOVE,0,(a[0]-lastX)/(float)Math.max(1,lastWidth),(b[0]-lastY)/(float)Math.max(1,lastHeight));
            baseX=lastX=a[0];baseY=lastY=b[0];GLFW.glfwGetWindowSize(h,a,b);lastWidth=a[0];lastHeight=b[0];
        }
    }
    /** 由术式产生的位移不记入玩家主动拖窗的躲避进度。 */
    public static void attract(double x,double y){
        if(!desktopAllowed()||!Minecraft.getInstance().isWindowActive())return;
        double dx=x-(baseX+lastWidth*.5),dy=y-(baseY+lastHeight*.5),distance=Math.hypot(dx,dy);
        if(distance<10)return;tug=2;tugX=(float)(dx/distance*Math.min(9,distance*.035));tugY=(float)(dy/distance*Math.min(9,distance*.035));
    }
    public static void push(float vx,float vy){
        if(!desktopAllowed()||!Minecraft.getInstance().isWindowActive())return;
        int dx=Math.abs(vx)>=Math.abs(vy)?(vx>=0?500:-500):0,dy=dx==0?(vy>=0?500:-500):0;
        move(baseX+dx,baseY+dy);int[] a={0},b={0};GLFW.glfwGetWindowPos(Minecraft.getInstance().getWindow().getWindow(),a,b);baseX=lastX=a[0];baseY=lastY=b[0];shake=tug=0;
    }
    public static int[] rectangle(){int[] a={0},b={0},w={0},h={0};long handle=Minecraft.getInstance().getWindow().getWindow();GLFW.glfwGetWindowPos(handle,a,b);GLFW.glfwGetWindowSize(handle,w,h);return new int[]{a[0],b[0],w[0],h[0]};}
    public static void disable(){restore();enabled=false;}
    public static void restore(){
        ObserverDesktop.clear();ObserverPortrait.clear();
        if(!saved)return;var mc=Minecraft.getInstance();var w=mc.getWindow();mc.options.pauseOnLostFocus=pauseOnFocus;
        if(w.isFullscreen())w.toggleFullScreen();
        w.setWindowed(width,height);GLFW.glfwSetWindowSize(w.getWindow(),width,height);GLFW.glfwSetWindowPos(w.getWindow(),x,y);
        if(fullscreen&&!w.isFullscreen())w.toggleFullScreen();
        saved=false;windowed=false;shake=0;tug=0;
    }
    public static void begin(){restore();enabled=true;age=0;capture();}
    private ObserverWindow(){}
}
