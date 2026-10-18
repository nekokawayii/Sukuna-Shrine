package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.logging.LogUtils;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.win32.StdCallLibrary;

/**
 * 第三阶段里那个「Windows 错误窗口」：真的用 Win32 MessageBox 弹一个系统级错误框
 * （带系统错误音），先「我成功了？」，点确定后变成「我穿过了，那道光！」。
 * 非 Windows / 弹不出来时只记日志，剧情继续（不会卡住玩家）。
 */
public final class ObserverDialog {
    public interface User32 extends StdCallLibrary{int MessageBoxW(Pointer owner,WString text,WString caption,int type);}
    private static final int MB_OK=0x00000000,MB_ICONERROR=0x00000010,MB_TOPMOST=0x00040000,MB_SETFOREGROUND=0x00010000;
    private static volatile boolean pending;
    public static boolean pending(){return pending;}
    public static void show(){
        if(pending)return;
        pending=true;
        Thread thread=new Thread(()->{
            try{
                User32 api=Native.load("user32",User32.class);
                api.MessageBoxW(null,new WString("我成功了？"),new WString("错误"),MB_OK|MB_ICONERROR|MB_TOPMOST|MB_SETFOREGROUND);
                api.MessageBoxW(null,new WString("我穿过了，那道光！"),new WString("错误"),MB_OK|MB_ICONERROR|MB_TOPMOST|MB_SETFOREGROUND);
            }catch(Throwable ex){
                LogUtils.getLogger().warn("[观测者] 系统错误窗口无法弹出，剧情继续",ex);
            }finally{
                net.minecraft.client.Minecraft.getInstance().execute(()->pending=false);
            }
        },"sukuna-observer-dialog");
        thread.setDaemon(true);thread.start();
    }
    private ObserverDialog(){}
}
