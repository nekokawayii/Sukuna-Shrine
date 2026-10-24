package cn.blockforge.generated.nnnnr1201forge2;

import com.sun.jna.*;
import com.sun.jna.win32.StdCallLibrary;
import java.util.concurrent.*;

/** 可选的 Windows 原生错误窗口；单线程有界队列，每个框自动关闭，不阻塞游戏线程。 */
public final class ObserverNativeDialog {
    public interface User32 extends StdCallLibrary {
        int MessageBoxTimeoutW(Pointer owner,WString text,WString caption,int flags,int language,int milliseconds);
    }
    private record Message(String title,String text,int generation) {}
    private static final ArrayBlockingQueue<Message> QUEUE=new ArrayBlockingQueue<>(6);
    private static volatile boolean enabled=Platform.isWindows();
    private static volatile int generation;
    private static boolean started;
    public static void toggle(){
        if(!Platform.isWindows())return;
        enabled=!enabled;clear();
    }
    public static void clear(){generation++;QUEUE.clear();}
    public static synchronized void post(String title,String text){
        if(!enabled||!Platform.isWindows())return;
        if(!QUEUE.offer(new Message(title,text,generation)))return;
        if(started)return;started=true;
        Thread worker=new Thread(()->{
            try{
                User32 api=Native.load("user32",User32.class);
                while(true){
                    Message m=QUEUE.take();if(!enabled||m.generation!=generation)continue;
                    api.MessageBoxTimeoutW(null,new WString(m.text),new WString("Minecraft · "+m.title),0x10,0,1800);
                }
            }catch(InterruptedException ex){Thread.currentThread().interrupt();}
            catch(Throwable ex){enabled=false;com.mojang.logging.LogUtils.getLogger().warn("原生弹窗不可用，继续使用游戏内弹窗：{}",ex.toString());}
            finally{started=false;QUEUE.clear();}
        },"sukuna-high-dialog");
        worker.setDaemon(true);worker.start();
    }
    private ObserverNativeDialog(){}
}
