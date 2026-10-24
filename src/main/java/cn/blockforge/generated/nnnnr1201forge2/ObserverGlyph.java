package cn.blockforge.generated.nnnnr1201forge2;

import java.awt.*;
import java.awt.image.BufferedImage;
import net.minecraft.client.gui.GuiGraphics;

/** 几何术式效果，不读取桌面画面；两种绘制出口使用完全相同的像素图元。 */
public final class ObserverGlyph {
    private static final int[] COLORS={0xFFFF2345,0xFFFF7926,0xFF29BFFF,0xFFFF2359,0xFFFFD3E3,0xFFFFC64F,0xFFEC75FF,0xFFA556FF,0xFFA9EEFF,0xFFAE092A,0xFF7DFFDC};
    private static boolean ink(int style,double x,double y,int age){
        double r=Math.hypot(x,y),a=Math.atan2(y,x)+age*.035;
        return switch(style){
            case 1->r<.26||Math.abs(y+x*.25)<.07&&x<.75&&x>-.9||r<.55&&Math.sin(a*5+age*.2)>.55;
            case 2,3,7->r<.32||r>.50&&r<.57||r<.73&&r>.66&&Math.sin(a*4)>.15;
            case 4->Math.abs(x)<.035&&Math.abs(y)<.9||Math.abs(y)<.035&&Math.abs(x)<.9||Math.abs(Math.abs(x)-Math.abs(y))<.045&&r<.9;
            case 5->Math.abs(r-(.48+.17*Math.cos(a*6)))<.055||r<.12;
            case 6->Math.abs(y-x*.3)<.045&&Math.abs(x)<.85||Math.abs(r-.65)<.025;
            case 8->Math.abs(x)<(.8-Math.abs(y))*.22&&Math.abs(y)<.85||Math.abs(y)<(.8-Math.abs(x))*.22&&Math.abs(x)<.85;
            case 9->Math.abs(y)<.08&&Math.abs(x)<.9||r<.24;
            case 10->Math.abs(r-.55)<.07||Math.abs(x-y)<.045&&r<.8;
            default->Math.abs(y-x*.28)<.07*(1-Math.abs(x))&&Math.abs(x)<.95;
        };
    }
    public static BufferedImage image(int style,int size,int age){
        BufferedImage image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);int color=COLORS[Math.floorMod(style,COLORS.length)];
        for(int y=0;y<size;y++)for(int x=0;x<size;x++)if(ink(style,(x-size*.5)/(size*.5),(y-size*.5)/(size*.5),age))image.setRGB(x,y,color);
        return image;
    }
    public static void draw(GuiGraphics g,int cx,int cy,int style,int age){
        int size=32,color=COLORS[Math.floorMod(style,COLORS.length)];
        for(int y=0;y<size;y++)for(int x=0;x<size;x++)if(ink(style,(x-16)/16.0,(y-16)/16.0,age))g.fill(cx+x-16,cy+y-16,cx+x-15,cy+y-15,color);
    }
    private ObserverGlyph(){}
}
