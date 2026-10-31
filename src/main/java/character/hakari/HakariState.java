package character.hakari;

/** 双端安全的小状态：客户端据此切换轮盘与显示老虎机 UI（服务端不读）。 */
public final class HakariState {
    /** 是否处于中奖（4 分 11 秒）状态。 */
    public static volatile boolean jackpot;
    /** 老虎机滚动的图案（0~3，客户端显示用）。 */
    public static volatile int reel0, reel1, reel2;
    /** 当前幸运值（0~30），用于 HUD 进度条。 */
    public static volatile int luck;

    public static void reset() { jackpot = false; reel0 = 0; reel1 = 0; reel2 = 0; luck = 0; }

    private HakariState() {}
}
