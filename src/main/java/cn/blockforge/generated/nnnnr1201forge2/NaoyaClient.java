package cn.blockforge.generated.nnnnr1201forge2;

import character.CharacterPackets;
import character.naoya.NaoyaCombat;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * 直哉投射咒法的客户端输入：**只在「设定投影」的 10 秒模式内**接管
 * 右键（按下=蓄力，松开=留节点）与左键（执行选中的投射技），其余时间完全走原版。
 */
public final class NaoyaClient {
    private static boolean charging;
    private static long chargeAt;

    public static boolean mouse(int button, int action, int mods) {
        if (!NaoyaVisuals.projecting()) return false;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        var selected = character.CharacterClient.selected();
        if (selected == null || !"naoya".equals(selected.id())) return false;
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (action == GLFW.GLFW_PRESS) {
                charging = true;
                chargeAt = mc.level.getGameTime();
                send(NaoyaCombat.CHARGE);
            } else if (action == GLFW.GLFW_RELEASE && charging) {
                charging = false;
                send(NaoyaCombat.PLACE);
            }
            return true;
        }
        return false;
    }

    /** 领域内按住左键 = 千手乱舞（16 次/秒自动连打），与投影模式的右键互不影响。 */
    public static boolean attack(int action) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        var selected = character.CharacterClient.selected();
        if (selected == null || !"naoya".equals(selected.id())) return false;
        if (!NaoyaVisuals.domainActive()) return false;
        if (action == GLFW.GLFW_PRESS) send(NaoyaCombat.AUTO_ON);
        else if (action == GLFW.GLFW_RELEASE) send(NaoyaCombat.AUTO_OFF);
        return true;
    }

    public static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !NaoyaVisuals.projecting()) charging = false;
    }

    private static void send(int token) {
        Network.CHANNEL.sendToServer(new CharacterPackets.Physical(200 + token));
    }

    private NaoyaClient() {}
}
