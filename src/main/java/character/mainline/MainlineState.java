package character.mainline;

/** 客户端主线快照（由专用服务端状态包更新）。 */
public final class MainlineState {
    public static volatile int mask;
    public static volatile int simple, domain;
    /** 累计击杀咒灵数（咒力总量训练用）。 */
    public static volatile int kills;

    public static boolean unlocked(int id) { return id >= 0 && id < Mainline.NAMES.length && (mask & (1 << id)) != 0; }

    private MainlineState() {}
}
