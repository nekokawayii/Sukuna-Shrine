package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.gui.GuiGraphics;

/** 屏幕底部的二维动态火舌、余烬与掠过的斩痕，不遮挡轮盘按钮。 */
public final class WheelBackdrop {
    public static void draw(GuiGraphics g,int width,int height,double tick){
        int base=height,top=Math.max(height*2/3,height-66);
        for(int x=0;x<width;x+=3){double wave=Math.sin(x*0.075+tick*0.19)+0.5*Math.sin(x*0.21-tick*0.28);int h=12+(int)(14*(1+wave));h=Math.min(height-top,h);
            g.fill(x,base-h,x+3,base,0x853D0718);g.fill(x,base-h*3/4,x+3,base,0xA4A2211E);g.fill(x,base-h/2,x+3,base,0xC2F16B24);g.fill(x,base-h/5,x+3,base,0xADF9C876);
        }
        for(int i=0;i<20;i++){int x=Math.floorMod(i*73+(int)(Math.sin(tick*0.03+i)*14),Math.max(1,width));int rise=(int)((tick*0.8+i*11)%55);g.fill(x,height-8-rise,x+1,height-6-rise,0xBFFF9853);}
        for(int i=0;i<5;i++){double phase=(tick+i*19)%95/95.0;int x=(int)(phase*(width+160))-80,y=height-48+(i%3)*10;int a=(int)(100*Math.sin(phase*Math.PI));for(int j=0;j<75;j++){int px=x+j,py=y-j/5;g.fill(px,py,px+2,py+1,(a<<24)|0xFFF3ED);}}
    }
    private WheelBackdrop(){}
}
