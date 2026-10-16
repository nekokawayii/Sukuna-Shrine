package cn.blockforge.generated.nnnnr1201forge2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** 两个发行版只改变未设置时的默认值，绝不覆盖玩家已经保存的选择。 */
public final class WallpaperPolicy {
    public static final String DEFAULT_MODE=readDefault();
    private static String readDefault(){
        Properties properties=new Properties();
        try(InputStream in=WallpaperPolicy.class.getResourceAsStream("/sukuna-wallpaper-defaults.properties")){
            if(in!=null)properties.load(in);
        }catch(IOException ignored){return "ask";}
        return "on".equals(properties.getProperty("consent"))?"on":"ask";
    }
    public static String resolve(String saved,String fallback){
        if(saved==null||saved.equals("default"))return "on".equals(fallback)?"on":"ask";
        return switch(saved){case "on","off","ask"->saved;default->"ask";};
    }
    public static boolean allSlashes(boolean voidStage,boolean inherited){return voidStage||inherited;}
    private WallpaperPolicy(){}
}
