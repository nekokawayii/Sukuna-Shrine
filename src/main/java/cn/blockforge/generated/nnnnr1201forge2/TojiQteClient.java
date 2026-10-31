package cn.blockforge.generated.nnnnr1201forge2;

import character.CharacterPackets;
import character.toji.TojiQte;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * 甚尔 QTE 的客户端按键接管。
 *
 * <p>**只在红光窗口（0.8 秒）与闪避后的连招窗口（1.5 秒）内生效**，窗口外全部返回 false，
 * 原版按键（空格/Shift/Ctrl/E/F/G/R/1/2/左右键）完全不受影响。
 *
 * <p>窗口内的映射：空格＝闪避（空中＝空中闪避，Shift+空格＝后撤步）、Ctrl＝滑铲、
 * G＝抓取、E＝缴械、F＝处决、R＝投掷咒具、1＝游云、2＝天逆鉾、左键＝攻击（按住 1 秒＝蓄力）、
 * Shift+左键＝过肩摔、右键＝弹反/使用。
 */
public final class TojiQteClient {
    private static long windowUntil;
    private static long pressedAt;
    private static boolean holding;
    private static int airPresses;

    public static boolean active() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return false;
        return mc.level.getGameTime() < windowUntil;
    }

    /** 收到服务端的窗口/连招窗口包时延长本地接管时间。 */
    static void extend(long ticks) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        windowUntil = Math.max(windowUntil, mc.level.getGameTime() + ticks);
    }

    static void reset() { windowUntil = 0; holding = false; airPresses = 0; }

    /** 返回 true 表示这次按键已被 QTE 吃掉（调用方不应再走施法/轮盘）。 */
    public static boolean key(int key, int action, int mods) {
        if (!active() || action == GLFW.GLFW_RELEASE) return false;
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        int token = switch (key) {
            case GLFW.GLFW_KEY_SPACE -> player.onGround() ? TojiQte.DODGE
                : (airPresses++ == 0 ? TojiQte.AIR_DODGE : TojiQte.MOBILITY);
            case GLFW.GLFW_KEY_G -> TojiQte.GRAB;
            case GLFW.GLFW_KEY_H -> TojiQte.WEAPON_YUYUN;
            case GLFW.GLFW_KEY_J -> TojiQte.MOBILITY;
            case GLFW.GLFW_KEY_K -> TojiQte.HEAVY;
            default -> -1;
        };
        if (token < 0) return false;
        send(token);
        return true;
    }

    /** 鼠标：窗口内左键＝连招攻击（按住 1 秒＝蓄力重击），其余按键不碰。 */
    public static boolean mouse(int button, int action, int mods) {
        if (!active()) return false;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        if (action == GLFW.GLFW_PRESS) {
            holding = true;
            pressedAt = mc.level.getGameTime();
        } else if (action == GLFW.GLFW_RELEASE && holding) {
            holding = false;
            long held = mc.level.getGameTime() - pressedAt;
            send(held >= 20 ? TojiQte.HEAVY : TojiQte.ATTACK);
        }
        return true;
    }

    public static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) { reset(); return; }
        if (!active()) { holding = false; airPresses = 0; return; }
        if (mc.player != null && mc.player.onGround()) airPresses = 0;
    }

    private static void send(int token) {
        TojiQteVisuals.noteToken(token);
        Network.CHANNEL.sendToServer(new CharacterPackets.Physical(100 + token));
    }

    private TojiQteClient() {}
}
