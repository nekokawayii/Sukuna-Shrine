package cn.blockforge.generated.nnnnr1201forge2;

import character.hakari.HakariFx;
import character.hakari.HakariState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 秤金次的老虎机 HUD：屏幕上方三格滚动图案 + 幸运值进度条；中奖期间变成金色横幅与倒计时。
 * 被拉进「坐杀博徒」领域的玩家也会看到（服务端把 SLOT 广播给领域内所有人）。
 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HakariClient {
    private static long slotUntil, jackpotUntil;
    private static int luck;

    public static void receive(Network.Effect e) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        switch (e.kind()) {
            case HakariFx.SLOT -> {
                slotUntil = Math.max(slotUntil, now + Math.max(10, e.life()));
                if (e.size() >= 0) {
                    luck = (int) e.size();
                    HakariState.luck = luck;
                    HakariState.reel0 = mc.level.random.nextInt(4);
                    HakariState.reel1 = mc.level.random.nextInt(4);
                    HakariState.reel2 = mc.level.random.nextInt(4);
                }
            }
            case HakariFx.JACKPOT -> {
                if (e.size() <= 0.41f) { jackpotUntil = now + Math.max(1, e.life()); return; }
                jackpotUntil = now + Math.max(1, e.life());
                HakariState.jackpot = true;
                slotUntil = jackpotUntil;
                Client.shake(4);
            }
            case HakariFx.JACKPOT_END -> {
                jackpotUntil = 0;
                HakariState.jackpot = false;
            }
            case HakariFx.DOOR -> {
                if (PhotonFx.ready()) PhotonFx.ring(e.position(), 2.4, 0.2, 0xFFFFC44D, 0xFFFFFFFF, 18, 2, 10);
                Client.shake(2);
            }
            case HakariFx.SPARK -> {
                if (PhotonFx.ready()) {
                    PhotonFx.spark(e.position(), 3.0, 1.6, 0xFFFFFFFF, 0xFFE0E0E0,
                        "textures/effect/glow.png", true, 8, 10, 26, 0.35);
                }
            }
            case HakariFx.TRAIN -> {
                if (PhotonFx.ready()) {
                    Vec3 end = e.position().add(e.direction().normalize().scale(e.size()));
                    PhotonFx.spark(e.position(), 3.0, 1.2, 0xFF35E06A, 0xFFB8FFD0,
                        "textures/effect/glow.png", true, 8, 12, 30, 0.4);
                    PhotonFx.ring(end, 3.0, 0.25, 0xFF35E06A, 0xFFFFFFFF, 18, 2, 10);
                }
                Client.shake(6);
            }
            case HakariFx.BLOCK -> {
                if (PhotonFx.ready()) {
                    PhotonFx.ring(e.position().add(0, 1, 0), 1.6, 0.15, 0xFFB9C6D8, 0xFFFFFFFF, 16, 2, 8);
                }
            }
            default -> { }
        }
    }

    public static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) { slotUntil = 0; jackpotUntil = 0; HakariState.reset(); return; }
        long now = mc.level.getGameTime();
        if (jackpotUntil != 0 && now >= jackpotUntil) { jackpotUntil = 0; HakariState.jackpot = false; }
        if (slotUntil != 0 && now >= slotUntil) slotUntil = 0;
    }

    /** 屏幕上方：三格老虎机 + 幸运值进度条；中奖时是金色横幅与倒计时。 */
    @SubscribeEvent public static void overlay(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("sukuna_hakari_slot", (gui, graphics, partialTick, width, height) -> {
            var mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            long now = mc.level.getGameTime();
            boolean jackpot = jackpotUntil > now;
            if (slotUntil <= now && !jackpot) return;
            var font = mc.font;
            int boxWidth = 148, boxHeight = jackpot ? 34 : 30;
            int x = (width - boxWidth) / 2, y = 8;
            graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xE6000000);
            int edge = jackpot ? 0xFFFFD700 : 0xFFFF3B3B;
            graphics.fill(x, y, x + boxWidth, y + 1, edge);
            graphics.fill(x, y + boxHeight - 1, x + boxWidth, y + boxHeight, edge);
            graphics.fill(x, y, x + 1, y + boxHeight, edge);
            graphics.fill(x + boxWidth - 1, y, x + boxWidth, y + boxHeight, edge);
            if (jackpot) {
                long left = Math.max(0, jackpotUntil - now);
                graphics.drawString(font, "中奖！无限咒力 " + (left / 20) + "s",
                    x + 8, y + 5, 0xFFFFD700, false);
                int bar = (int) (boxWidth - 16);
                int fill = (int) (bar * (left / 5020.0));
                graphics.fill(x + 8, y + 20, x + 8 + bar, y + 26, 0x40FFFFFF);
                graphics.fill(x + 8, y + 20, x + 8 + fill, y + 26, 0xFFFFD700);
            } else {
                graphics.drawString(font, "老虎机 " + luck + " / 30", x + 8, y + 5, 0xFFFFB8B8, false);
                int bar = width2(boxWidth);
                int fill = (int) (bar * Math.min(1.0, luck / 30.0));
                graphics.fill(x + 8, y + 17, x + 8 + bar, y + 23, 0x40FFFFFF);
                graphics.fill(x + 8, y + 17, x + 8 + fill, y + 23, 0xFFFF7A7A);
                String[] faces = {"7", "★", "BAR", "◆"};
                graphics.drawString(font, faces[Math.floorMod(HakariState.reel0, 4)], x + 12, y + 8, 0xFFFFE066, false);
            }
        });
    }

    private static int width2(int boxWidth) { return boxWidth - 16; }

    private HakariClient() {}
}
