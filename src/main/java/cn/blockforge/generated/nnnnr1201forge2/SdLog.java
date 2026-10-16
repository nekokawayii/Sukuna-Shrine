package cn.blockforge.generated.nnnnr1201forge2;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalTime;

/** 临时诊断：追加写 {@code logs/sukuna-simpledomain.log}（stdout 不会进 latest.log，所以必须写文件）。 */
public final class SdLog {
    private static final Path FILE=Paths.get("logs","sukuna-simpledomain.log");
    public static void w(String msg){
        try{
            Files.createDirectories(FILE.getParent());
            Files.write(FILE,("["+LocalTime.now().withNano(0)+"] "+msg+System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        }catch(Exception ignored){}
    }
    private SdLog(){}
}
