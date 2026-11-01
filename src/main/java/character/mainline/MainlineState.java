package character.mainline;

/** 客户端主线快照（由专用服务端状态包更新）。 */
public final class MainlineState {
    public static volatile int mask;
    public static volatile int simple, domain;

    public static boolean unlocked(int id) { return id >= 0 && id < 7 && (mask & (1 << id)) != 0; }

    private MainlineState() {}
}
