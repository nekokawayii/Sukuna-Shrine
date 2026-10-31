package character.mainline;

/** 双端共享的主线状态（服务端写入、客户端由特效同步）。 */
public final class MainlineState {
    public static volatile int mask;
    public static volatile int simple, domain;

    public static boolean unlocked(int id) { return id >= 0 && id < 7 && (mask & (1 << id)) != 0; }

    private MainlineState() {}
}
