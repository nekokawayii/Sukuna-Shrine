package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** 游戏内 Windows 风格错误窗口：非模态、不抢系统焦点、不创建无法关闭的系统弹窗。 */
public final class ObserverDialog {
    private record Message(String title,String text,int born) {}
    private static final Deque<Message> messages=new ArrayDeque<>();
    private static int clock;
    public static void clear(){messages.clear();clock=0;ObserverNativeDialog.clear();}
    public static void tick(){clock++;while(!messages.isEmpty()&&clock-messages.peekFirst().born>95)messages.removeFirst();}
    public static void show(){say("系统错误","我成功了，我成功的穿过了，那道光！");}
    public static void say(String title,String text){
        if(messages.size()==5)messages.removeFirst();messages.addLast(new Message(title,text,clock));
        ObserverNativeDialog.post(title,text);
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BIT.value(),.65f,.18f));
    }
    public static void render(GuiGraphics g,int w,int h){
        int i=0;
        for(Message m:messages){
            int width=Math.min(270,w-16),x=Math.max(4,w-width-8-i*4),y=Math.max(65,h-82-i*42);
            frame(g,x,y,width,70,m.title,m.text);i++;
        }
    }
    public static void frame(GuiGraphics g,int x,int y,int w,int h,String title,String text){
        var font=Minecraft.getInstance().font;
        g.fill(x+3,y+3,x+w+3,y+h+3,0x55000000);g.fill(x,y,x+w,y+h,0xFF9CA1A8);
        g.fill(x+1,y+1,x+w-1,y+h-1,0xFFF1F1F1);g.fill(x+2,y+2,x+w-2,y+17,0xFF1266AE);
        g.drawString(font,title,x+6,y+5,0xFFFFFFFF,false);
        g.fill(x+w-17,y+3,x+w-3,y+15,0xFFC42B1C);g.drawString(font,"×",x+w-13,y+4,0xFFFFFFFF,false);
        g.fill(x+8,y+26,x+21,y+39,0xFFCF262E);g.drawString(font,"×",x+11,y+28,0xFFFFFFFF,false);
        var lines=font.split(Component.literal(text),Math.max(30,w-38));int yy=y+25;
        for(var line:lines){if(yy>y+h-10)break;g.drawString(font,line,x+28,yy,0xFF202025,false);yy+=11;}
    }
    private ObserverDialog(){}
}
