package cn.blockforge.generated.nnnnr1201forge2;

/** 每人最多半兆压缩图；不传路径、桌面窗口或其他文件。 */
public final class WallpaperRules {
    public static final int CHUNK=24*1024,MAX_BYTES=512*1024,MAX_PARTS=(MAX_BYTES+CHUNK-1)/CHUNK,MAX_EDGE=2048,MAX_PIXELS=2048*1152;
    public static boolean validPart(int index,int count,int total,int bytes){
        return total>0&&total<=MAX_BYTES&&count==(total+CHUNK-1)/CHUNK&&index>=0&&index<count&&bytes==Math.min(CHUNK,total-index*CHUNK);
    }
    public static boolean validImage(int w,int h){return w>0&&h>0&&w<=MAX_EDGE&&h<=MAX_EDGE&&(long)w*h<=MAX_PIXELS;}
    public static float coordinate(double fragment,double framebuffer,double origin,double window,double monitorOrigin,double monitorSize){return (float)((origin+fragment/framebuffer*window-monitorOrigin)/monitorSize);}
    private WallpaperRules(){}
}
