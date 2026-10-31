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
    /** 车门猛砸：正方形边框向内收拢。 */
    private record Door(net.minecraft.world.phys.Vec3 at, net.minecraft.world.phys.Vec3 dir, long born, int life) {}
    private static final java.util.List<Door> DOORS = new java.util.ArrayList<>();

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
                DOORS.add(new Door(e.position(), e.direction(), now, Math.max(6, e.life())));
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

    /** 世界内绘制：车门猛砸的四条边从外向内收拢 + 边角火花。 */
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void render(
            net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || DOORS.isEmpty()) return;
        long now = mc.level.getGameTime();
        var cam = event.getCamera().getPosition();
        var ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.disableCull();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
        var builder = com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder();
        builder.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
            com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
        for (Door door : DOORS) {
            float t = Math.min(1f, (now - door.born()) / (float) door.life);
            double half = 3.2 * (1.0 - t) + 0.25;        // 由外向内收拢
            int alpha = (int) (235 * (1 - t * 0.3));
            int gold = (alpha << 24) | 0xFFC44D, core = (alpha << 24) | 0xFFF6D6;
            // 以命中点为中心、面向相机的正方形四边
            Vec3 look = door.at().subtract(cam);
            if (look.lengthSqr() < 1e-4) look = new Vec3(0, 0, 1);
            look = look.normalize();
            Vec3 right = look.cross(new Vec3(0, 1, 0));
            if (right.lengthSqr() < 1e-4) right = new Vec3(1, 0, 0);
            right = right.normalize();
            Vec3 up = right.cross(look).normalize();
            Vec3 c = door.at();
            Vec3 tl = c.add(right.scale(-half)).add(up.scale(half));
            Vec3 tr = c.add(right.scale(half)).add(up.scale(half));
            Vec3 bl = c.add(right.scale(-half)).subtract(up.scale(half));
            Vec3 br = c.add(right.scale(half)).subtract(up.scale(half));
            Client.ribbon(builder, ps, tl, tr, 0.16, gold, cam);
            Client.ribbon(builder, ps, tr, br, 0.16, gold, cam);
            Client.ribbon(builder, ps, br, bl, 0.16, gold, cam);
            Client.ribbon(builder, ps, bl, tl, 0.16, gold, cam);
            Client.ribbon(builder, ps, tl, tr, 0.06, core, cam);
            Client.ribbon(builder, ps, br, bl, 0.06, core, cam);
            // 四角火花朝内
            for (int i = 0; i < 4; i++) {
                Vec3 corner = switch (i) { case 0 -> tl; case 1 -> tr; case 2 -> br; default -> bl; };
                Vec3 inward = c.subtract(corner).normalize().scale(0.5 + 0.7 * t);
                Client.ribbon(builder, ps, corner, corner.add(inward), 0.05, core, cam);
            }
        }
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(builder.end());
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.enableCull();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        ps.popPose();
    }

    public static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) { slotUntil = 0; jackpotUntil = 0; DOORS.clear(); HakariState.reset(); return; }
        long now = mc.level.getGameTime();
        if (jackpotUntil != 0 && now >= jackpotUntil) { jackpotUntil = 0; HakariState.jackpot = false; }
        if (slotUntil != 0 && now >= slotUntil) slotUntil = 0;
        DOORS.removeIf(door -> now - door.born() > door.life());
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
