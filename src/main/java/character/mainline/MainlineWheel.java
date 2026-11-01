package character.mainline;

import cn.blockforge.generated.nnnnr1201forge2.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/** 两种轮盘共用同一套方形格：左侧治疗与结界，右侧简易领域。松开轮盘键立即使用。 */
public final class MainlineWheel {
    private static final int[] SKILLS={Mainline.SKILL_REVERSE,Mainline.SKILL_REVERSE_OUT,Mainline.SKILL_BARRIER,Mainline.SKILL_SIMPLE};
    private static final int[] UNLOCK={Mainline.REVERSE,Mainline.REVERSE_OUT,Mainline.BARRIER,Mainline.SIMPLE};
    private static WheelSideSlots.Box box(int w,int h,int index){
        int width=Math.max(56,Math.min(90,w/5));
        return new WheelSideSlots.Box(index==3?w-width-6:6,index==3?Math.max(48,h-86):h/2-58+index*39,width,34);
    }
    public static int hover(int w,int h,double x,double y){for(int i=0;i<4;i++)if(box(w,h,i).contains(x,y))return SKILLS[i];return -1;}
    public static boolean cast(int id){
        for(int i=0;i<4;i++)if(SKILLS[i]==id){if(MainlineState.unlocked(UNLOCK[i]))Network.CHANNEL.sendToServer(new TechniqueInput.Input(id,7,false,false,false,false,0,0));return true;}
        return false;
    }
    public static void render(GuiGraphics g,Font font,int w,int h,int mx,int my){
        for(int i=0;i<4;i++){
            var b=box(w,h,i);boolean known=MainlineState.unlocked(UNLOCK[i]),hot=b.contains(mx,my);
            g.fill(b.x(),b.y(),b.x()+b.width(),b.y()+b.height(),!known?0xE0262931:hot?0xF03E7295:0xE018354D);
            g.drawCenteredString(font,Mainline.NAMES[UNLOCK[i]],b.x()+b.width()/2,b.y()+5,known?0xFFE3F3FF:0xFF989BA2);
            g.drawCenteredString(font,known?i==3?"10格蓝圈 10秒":i==2?"15格球壳 10秒":"松开轮盘键使用":"L 进度中解锁",b.x()+b.width()/2,b.y()+20,0xFFACC7D8);
        }
        g.drawCenteredString(font,"简易领域 "+MainlineState.simple+"/15  ·  领域展开 "+MainlineState.domain+"/10",w/2,h-54,0xFFAAC7DF);
    }
    private MainlineWheel() { }
}
