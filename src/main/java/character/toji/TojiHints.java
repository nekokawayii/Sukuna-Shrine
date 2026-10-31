package character.toji;

import java.util.List;

/**
 * 连招提示表：**服务端配方与客户端屏幕提示共用这一份**，避免两边写两套导致"提示与实战对不上"。
 *
 * <p>五种键：左键＝攻击 · G＝抓取/投技 · H＝咒具 · J＝身法 · K＝重击。
 */
public final class TojiHints {
    /** ONE = 左键。 */
    public static final int ONE = TojiQte.ATTACK, G = TojiQte.GRAB, H = TojiQte.WEAPON_YUYUN,
        J = TojiQte.MOBILITY, K = TojiQte.HEAVY;

    public record Hint(int combo, String name, int[] keys) {
        public String keyText() { return text(keys); }
    }

    /** 提示与配方共用；顺序即匹配优先级（同长时靠条件自动往后退）。 */
    public static final List<Hint> ALL = List.of(
        new Hint(19, "空中体操", new int[]{ONE, J, ONE}),
        new Hint(11, "极速切武·三连处决", new int[]{H, ONE, H, ONE}),
        new Hint(10, "蓄力破甲", new int[]{H, H, K}),
        new Hint(13, "浮空乱打", new int[]{ONE, ONE, ONE, ONE, ONE, ONE, ONE, ONE}),
        new Hint(18, "游云·打棒球（敌冲刺）", new int[]{H, H, ONE}),
        new Hint(15, "跑酷踩头（空中）", new int[]{J, J, ONE}),
        new Hint(5, "滑铲切足（地面）", new int[]{J, J, ONE}),
        new Hint(8, "夺命挡箭牌", new int[]{G, G, ONE}),
        new Hint(7, "游云·破膝击", new int[]{H, H, ONE}),
        new Hint(2, "裂骨膝撞", new int[]{ONE, ONE}),
        new Hint(12, "先之先·截击", new int[]{K, ONE}),
        new Hint(17, "夺刃反杀", new int[]{G, K}),
        new Hint(3, "飞踢拉扯（地面）", new int[]{J, ONE}),
        new Hint(4, "过肩重摔", new int[]{G, ONE}),
        new Hint(6, "天逆鉾·破魔", new int[]{H, ONE}),
        new Hint(9, "踩头处决（空中）", new int[]{J, ONE}),
        new Hint(16, "刀反（附近有弹幕）", new int[]{K, K}),
        new Hint(14, "墙壁反杀（背后有墙）", new int[]{ONE}),
        new Hint(1, "基础闪避反击", new int[]{ONE}),
        new Hint(20, "暴君处决（闪避满 3 次）", new int[]{K}));

    public static String keyName(int token) {
        return switch (token) {
            case ONE -> "左";
            case G -> "G";
            case H -> "H";
            case J -> "J";
            case K -> "K";
            default -> "?";
        };
    }

    public static String text(int[] keys) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) builder.append(' ');
            builder.append(keyName(keys[i]));
        }
        return builder.toString();
    }

    private TojiHints() {}
}
