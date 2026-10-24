package character.jogo;

/** 漏壶专用特效号（服务端发 Network.Effect，客户端 {@link JogoFxClient} 用 PhotonFx 绘制）。 */
public final class JogoFx {
    public static final int BUG=258, BUG_BURST=259, METEOR_TRAIL=260, METEOR_IMPACT=261, PILLAR=262, DOMAIN=263, DOMAIN_END=264;
    public static final int FIRST=BUG, LAST=DOMAIN_END;
    private JogoFx() {}
}
