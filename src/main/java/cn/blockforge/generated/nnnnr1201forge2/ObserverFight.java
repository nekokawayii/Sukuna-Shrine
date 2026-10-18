package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 观测者战（第三阶段后半）：你不能控制史蒂夫，只能控制鼠标指针。
 * 护盾值在屏幕最上方；剑气球追着指针咬（5 秒寿命，被咬 15 口就强制崩端）；
 * 点身上的红光造成"世界外打击"；躲闪 QTE（猛甩鼠标）与按键 QTE（4 个随机键）；
 * 半血强制窗口化；打完进入黑屏 Connection Lost，靠 /sukuna reset 或退游戏收下胜利。
 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class ObserverFight {
    public static final int SHIELD=15,SHIELD_MAX=15;
    private static final float WEAK_STEP=0.075f;      // 一次弱点 = 7.5% 血
    private static final int WEAK_LIFE=55;            // 红光持续刻数
    private static final int QI_LIFE=100;             // 剑气球寿命 5 秒
    private static final String[] CODE={
        "public class Sukuna extends Monster {","WorldSlash.damage(target, 1.0e7F);","ObserverRitual.tick(session);",
        "if(player.hp<=0) shatter();","SlashEye.open(age);","WallpaperSlash.core(b,ps,p,q,r,s,1f,self);",
        "ScreenCracks.trigger(seed);","ObserverClient.end();","while(true){ world.tick(); }","player_not_found.exception();",
        "if(!allowed) return; // 你无法逃离。","DamageSource.genericKill()","session.boss.discard();"
    };

    private static boolean fighting,released,shatter,windowed,dodge,busy;
    private static float bossHp=1,shield=SHIELD_MAX,hurtFlash,shakePx;
    private static long tick,weakFrom=-1,dodgeFrom=-1,dodgeUntil=-1,keyFrom=-1,keyUntil=-1,nextQi,nextWeak,nextDodge,nextDomain,lastClickAt;
    private static int weakPart;
    private static final List<Qi> QIS=new ArrayList<>();
    private static final List<Integer> KEYS=new ArrayList<>();
    private static int keysDone;
    private static float cursorX,cursorY,lastX,lastY,shakeAmount;
    private static long jitterUntil;private static boolean baseSaved;private static int baseX,baseY;
    private static final Random RNG=new Random();
    private static float bossScreenX=-1,bossScreenY=-1;
    private static final List<float[]> RAIN=new ArrayList<>();
    private record Qi(float x,float y,long born,float speed){}

    public static boolean fighting(){return fighting;}
    public static boolean shatter(){return shatter;}

    public static void start(){
        fighting=true;shatter=false;windowed=false;bossHp=1;shield=SHIELD_MAX;hurtFlash=0;
        tick=0;ScreenCracks.clear();
        QIS.clear();KEYS.clear();keysDone=0;
        nextQi=60;nextWeak=80;nextDodge=260;nextDomain=520;
        var mc=Minecraft.getInstance();
        cursorX=lastX=0.5f;cursorY=lastY=0.5f;
        if(mc.mouseHandler.isMouseGrabbed()){mc.mouseHandler.releaseMouse();released=true;}
        RAIN.clear();
    }

    public static void end(){
        fighting=false;shatter=false;QIS.clear();KEYS.clear();RAIN.clear();
        if(released){Minecraft.getInstance().mouseHandler.grabMouse();released=false;}
    }

    /** 崩端：先落桌面日志与假崩溃报告，再硬退出（这就是"你无法逃离"）。 */
    public static void terminate(){
        var mc=Minecraft.getInstance();
        try{
            var home=System.getProperty("user.home");
            java.nio.file.Files.writeString(java.nio.file.Path.of(home,"Desktop","你无法逃离.txt"),"你无法逃离。\n\nPlayer_Not_Found\n");
            var dir=java.nio.file.Path.of(home,"Desktop");
            java.nio.file.Files.writeString(dir.resolve("crash-Player_Not_Found.txt"),
                "---- Minecraft Crash Report ----\n// 你无法逃离。\n\nTime: "+new java.util.Date()+"\nDescription: Player_Not_Found\n\n"
                +"java.lang.IllegalStateException: Player_Not_Found\n\tat cn.blockforge.generated.nnnnr1201forge2.ObserverFight.terminate(ObserverFight.java)\n"
                +"\tCaused by: the observer is watching you\n");
        }catch(Exception ignored){}
        Runtime.getRuntime().halt(1);
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        if(!fighting)return;
        tick++;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){end();return;}
        hurtFlash=Math.max(0,hurtFlash-0.05f);
        if(jitterUntil>0){
            var wh=mc.getWindow().getWindow();
            if(tick<jitterUntil)GLFW.glfwSetWindowPos(wh,baseX+RNG.nextInt(21)-10,baseY+RNG.nextInt(21)-10);
            else{GLFW.glfwSetWindowPos(wh,baseX,baseY);jitterUntil=0;baseSaved=false;}
        }
        if(mc.mouseHandler.isMouseGrabbed()){mc.mouseHandler.releaseMouse();released=true;}
        // 光标（窗口逻辑坐标 → 0..1）
        double sw=Math.max(1,mc.getWindow().getScreenWidth()),sh=Math.max(1,mc.getWindow().getScreenHeight());
        float nx=(float)(mc.mouseHandler.xpos()/sw),ny=(float)(mc.mouseHandler.ypos()/sh);
        shakeAmount+=Math.abs(nx-cursorX)+Math.abs(ny-cursorY);
        cursorX=Math.max(0.02f,Math.min(0.98f,nx));cursorY=Math.max(0.02f,Math.min(0.98f,ny));
        if(shatter)return;
        // 剑气球：生命周期 5 秒，追着光标咬
        if(tick>=nextQi&&QIS.size()<3){
            nextQi=tick+120;
            float side=RNG.nextFloat();
            QIS.add(new Qi(side<0.5f?0f:1f,RNG.nextFloat(),tick,0.0055f+RNG.nextFloat()*0.002f));
        }
        for(int i=QIS.size()-1;i>=0;i--){
            Qi q=QIS.get(i);
            if(tick-q.born>QI_LIFE){QIS.remove(i);continue;}
            float dx=cursorX-q.x,dy=cursorY-q.y,dist=(float)Math.sqrt(dx*dx+dy*dy);
            if(dist<0.045f){QIS.remove(i);hurt(1,"剑气擦过");if(!fighting)return;continue;}
            QIS.set(i,new Qi(q.x+dx*Math.min(1,q.speed),q.y+dy*Math.min(1,q.speed),q.born,q.speed));
        }
        // 弱点：隔一段时间身上亮红光
        if(tick>=nextWeak&&weakFrom<0){weakFrom=tick;weakPart=RNG.nextInt(3);nextWeak=tick+90;}
        if(weakFrom>=0&&tick-weakFrom>WEAK_LIFE)weakFrom=-1;
        // 躲闪 QTE：世界斩
        if(tick>=nextDodge&&dodgeFrom<0){dodgeFrom=tick;dodgeUntil=tick+70;shakeAmount=0;nextDodge=tick+320;}
        if(dodgeFrom>=0&&tick>dodgeUntil){
            boolean ok=shakeAmount>1.6f;
            dodgeFrom=-1;
            if(!ok)hurt(2,"没躲开世界斩");
            else burstShake();
        }
        // 领域 QTE：黑屏 + 4 个键
        if(tick>=nextDomain&&keyFrom<0){
            keyFrom=tick;keyUntil=tick+120;KEYS.clear();keysDone=0;
            while(KEYS.size()<4){int k=GLFW.GLFW_KEY_A+RNG.nextInt(26);if(!KEYS.contains(k))KEYS.add(k);}
            nextDomain=tick+520;
        }
        if(keyFrom>=0&&tick>keyUntil){
            keyFrom=-1;
            hurt(3,"领域展开完成");
        }
        if(bossHp<=0.5f&&!windowed)forceWindowed();
        if(bossHp<=0f&&!shatter)beginShatter();
    }

    private static void hurt(int amount,String why){
        if(shatter)return;
        shield-=amount;hurtFlash=1;shakePx=12;Client.shake(10);shakeWindow(24);
        if(shield<=0){shield=0;terminate();return;}
    }
    private static void burstShake(){shakePx=8;Client.shake(6);shakeWindow(14);}
    private static void shakeWindow(int ticks){
        var wh=Minecraft.getInstance().getWindow().getWindow();
        if(!baseSaved){int[] x=new int[1],y=new int[1];GLFW.glfwGetWindowPos(wh,x,y);baseX=x[0];baseY=y[0];baseSaved=true;}
        jitterUntil=Math.max(jitterUntil,tick+ticks);
    }

    /** 点中红光：世界外打击。 */
    private static boolean weakActive(){return weakFrom>=0&&tick-weakFrom<=WEAK_LIFE;}
    private static boolean hitWeak(float gx,float gy){
        if(!weakActive()||bossScreenX<0)return false;
        double dx=gx-bossScreenX,dy=gy-bossScreenY;
        if(dx*dx+dy*dy>48*48)return false;
        bossHp=Math.max(0,bossHp-WEAK_STEP);
        weakFrom=-1;nextWeak=tick+60;
        shakePx=10;Client.shake(8);shakeWindow(20);
        ScreenCracks.trigger((long)(1000+bossHp*1000));
        if(bossHp<=0f)beginShatter();
        return true;
    }

    /** 半血强制窗口化：直接走 GLFW，800x600。 */
    private static void forceWindowed(){
        windowed=true;
        try{
            long h=Minecraft.getInstance().getWindow().getWindow();
            GLFW.glfwSetWindowMonitor(h,0L,120,120,800,600,GLFW.GLFW_DONT_CARE);
            GLFW.glfwSetWindowSize(h,800,600);
            GLFW.glfwSetWindowPos(h,120,120);
            GLFW.glfwShowWindow(h);
        }catch(Throwable ignored){}
        shakePx=16;Client.shake(18);
    }

    private static void beginShatter(){
        if(shatter)return;
        shatter=true;bossHp=0;
        QIS.clear();KEYS.clear();
        Client.shake(24);
    }

    // ---- 输入 ----
    @SubscribeEvent public static void mouse(InputEvent.MouseButton.Pre e){
        if(!fighting||shatter)return;
        if(e.getButton()!=GLFW.GLFW_MOUSE_BUTTON_LEFT||e.getAction()!=GLFW.GLFW_PRESS)return;
        long now=System.currentTimeMillis();
        if(now-lastClickAt<120)return;
        lastClickAt=now;
        var mc=Minecraft.getInstance();
        double sw=Math.max(1,mc.getWindow().getScreenWidth()),sh=Math.max(1,mc.getWindow().getScreenHeight());
        float gx=(float)(mc.mouseHandler.xpos()/sw*mc.getWindow().getGuiScaledWidth());
        float gy=(float)(mc.mouseHandler.ypos()/sh*mc.getWindow().getGuiScaledHeight());
        if(keyFrom>=0){
            for(int i=0;i<KEYS.size();i++){
                float kx=keyX(i),ky=keyY();
                if(Math.abs(gx-kx)<26&&Math.abs(gy-ky)<26){keyDone(i);break;}
            }
            return;
        }
        hitWeak(gx,gy);
    }
    @SubscribeEvent public static void key(InputEvent.Key e){
        if(!fighting||shatter)return;
        if(e.getAction()!=GLFW.GLFW_PRESS)return;
        if(keyFrom>=0)for(int i=0;i<KEYS.size();i++)if(KEYS.get(i)==e.getKey()){keyDone(i);return;}
        if(e.getKey()==GLFW.GLFW_KEY_ESCAPE)return;
    }
    private static void keyDone(int index){
        if(keyFrom<0)return;
        keysDone++;
        if(keysDone>=4){keyFrom=-1;burstShake();}
    }

    // ---- 绘制 ----
    @SubscribeEvent public static void project(RenderLevelStageEvent e){
        if(!fighting||e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        var boss=mc.level.getEntitiesOfClass(ObserverEntity.class,new net.minecraft.world.phys.AABB(mc.player.blockPosition()).inflate(48)).stream().findFirst().orElse(null);
        if(boss==null){bossScreenX=-1;return;}
        Vec3 at=boss.position().add(0,1.15,0);
        Vec3 camPos=e.getCamera().getPosition();
        Vec3 rel=at.subtract(camPos);
        var mvp=new Matrix4f(e.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
        var v=new org.joml.Vector4f((float)rel.x,(float)rel.y,(float)rel.z,1f).mul(mvp);
        if(v.w<=0.01f){bossScreenX=-1;return;}
        float ndcX=v.x/v.w,ndcY=v.y/v.w;
        bossScreenX=(ndcX*0.5f+0.5f)*mc.getWindow().getGuiScaledWidth();
        bossScreenY=(1-(ndcY*0.5f+0.5f))*mc.getWindow().getGuiScaledHeight();
    }

    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){
        if(!fighting)return;
        var mc=Minecraft.getInstance();var g=e.getGuiGraphics();
        int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
        if(shatter){drawShatter(g,w,h);return;}
        drawBars(g,w,h);
        drawWeak(g);
        drawQi(g,w,h);
        drawQte(g,w,h);
        if(hurtFlash>0)g.fill(0,0,w,h,((int)(110*hurtFlash)<<24)|0xC00020);
        if(bossHp<=0.5f)drawCodeRain(g,w,h);
    }

    private static void drawBars(GuiGraphics g,int w,int h){
        int barW=Math.min(360,w-80);
        g.fill(40,14,40+barW,26,0xCC120A0E);
        g.fill(42,16,42+(int)((barW-4)*bossHp),24,0xFFC21C2E);
        g.fill(40,14,40+barW,14+1,0xFF5A0A14);
        g.drawString(Minecraft.getInstance().font,"观测者  "+Math.round(bossHp*100)+"%",44,17,0xFFFFE6E9);
        for(int i=0;i<SHIELD_MAX;i++){
            int x=40+i*18;
            g.fill(x,30,x+14,36,i<shield?0xFF44B7FF:0x331A2A33);
        }
        g.drawString(Minecraft.getInstance().font,"护盾",44,38,0xFF9FD8FF);
    }
    private static void drawWeak(GuiGraphics g){
        if(!weakActive()||bossScreenX<0)return;
        var mc=Minecraft.getInstance();
        if(mc.level==null)return;
        int cx=(int)bossScreenX,cy=(int)bossScreenY;
        float pulse=(float)(0.5+0.5*Math.sin(tick*0.45));
        int a=(int)(90+110*pulse);
        for(int i=0;i<3;i++){int r=14+i*7;int c=((int)(a*(1-i*0.25))<<24)|0xFF2036;
            g.fill(cx-r,cy-2,cx+r,cy+2,c);g.fill(cx-2,cy-r,cx+2,cy+r,c);}
    }
    private static void drawQi(GuiGraphics g,int w,int h){
        for(Qi q:QIS){
            int cx=(int)(q.x*w),cy=(int)(q.y*h);
            float life=1-(tick-q.born)/(float)QI_LIFE;
            int a=(int)(230*Math.max(0.15f,life));
            for(int i=0;i<5;i++){
                int len=10+i*6;int c=((int)(a*(1-i*0.18))<<24)|0xFF1E28;
                g.fill(cx-len,cy-1+i,cx+len,cy+1+i,c);
            }
            g.fill(cx-3,cy-3,cx+3,cy+3,((int)a<<24)|0xFFD9E0);
        }
    }
    private static void drawQte(GuiGraphics g,int w,int h){
        var mc=Minecraft.getInstance();
        if(dodgeFrom>=0){
            String text="【猛甩鼠标】";
            int tw=mc.font.width(text);
            g.fill(w/2-tw/2-14,h/2-70,w/2+tw/2+14,h/2-46,0xCC000000);
            g.drawString(mc.font,text,w/2-tw/2,h/2-64,0xFFFFD24A);
            g.fill(w/2-120,h/2-40,w/2+120,h/2-36,0x66FFD24A);
            float progress=Math.min(1,shakeAmount/1.6f);
            g.fill(w/2-120,h/2-40,w/2-120+(int)(240*progress),h/2-36,0xFFFFD24A);
        }
        if(keyFrom>=0){
            g.fill(0,0,w,h,0xB0000000);
            String tip="打断领域展开";
            g.drawString(mc.font,tip,w/2-mc.font.width(tip)/2,h/2-90,0xFFFF5566);
            for(int i=0;i<KEYS.size();i++){
                int x=(int)keyX(i),y=(int)keyY();
                boolean done=i<keysDone;
                g.fill(x-22,y-22,x+22,y+22,done?0xFF1E7A2E:0xFF1A1A1E);
                g.fill(x-22,y-22,x+22,y-20,done?0xFF4BE06A:0xFFE0364A);
                String k=GLFW.glfwGetKeyName(KEYS.get(i),0);
                if(k==null)k="?";
                g.drawString(mc.font,k.toUpperCase(Locale.ROOT),x-mc.font.width(k.toUpperCase(Locale.ROOT))/2,y-8,0xFFFFFFFF);
            }
        }
    }
    private static float keyX(int i){var mc=Minecraft.getInstance();return mc.getWindow().getGuiScaledWidth()/2f+(i-1.5f)*70f;}
    private static float keyY(){return Minecraft.getInstance().getWindow().getGuiScaledHeight()/2f;}
    private static void drawCodeRain(GuiGraphics g,int w,int h){
        var mc=Minecraft.getInstance();
        if(RAIN.isEmpty())for(int i=0;i<26;i++)RAIN.add(new float[]{RNG.nextFloat(),RNG.nextFloat(),0.4f+RNG.nextFloat()*1.6f,RNG.nextFloat()});
        int green=0xFF3BFF6E;
        for(float[] r:RAIN){
            r[1]+=0.0028f*r[2];
            if(r[1]>1.05f){r[1]=-0.05f;r[0]=RNG.nextFloat();}
            String code=CODE[(int)(r[3]*CODE.length)%CODE.length];
            int x=(int)(r[0]*w),y=(int)(r[1]*h);
            if(r[0]<0.30f)x=(int)(r[0]*w*0.6);
            g.drawString(mc.font,code,x,y,green);
        }
    }
    private static void drawShatter(GuiGraphics g,int w,int h){
        g.fill(0,0,w,h,0xFF000000);
        var mc=Minecraft.getInstance();
        int y=h/2-40;
        g.drawString(mc.font,"Connection Lost: Player_Not_Found",w/2-mc.font.width("Connection Lost: Player_Not_Found")/2,y,0xFFFF2A3C);
        g.drawString(mc.font,"世界拒绝继续运行。",w/2-mc.font.width("世界拒绝继续运行。")/2,y+18,0xFFB0121F);
        g.drawString(mc.font,"输入 /sukuna reset 收回控制权，或直接关掉游戏。",w/2-mc.font.width("输入 /sukuna reset 收回控制权，或直接关掉游戏。")/2,y+40,0xFF7A0A12);
    }
    private ObserverFight(){}
}
