package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 秤金次的世界内表现：车门猛砸的**正方形边框向内收拢**。
 *
 * <p>注意总线：`RenderLevelStageEvent` 属于 **Forge 总线**，所以这个类**不能**和
 * `HakariClient`（注册 HUD 的 `RegisterGuiOverlaysEvent` 是**模组总线**）放在一起——
 * 混在一个 `@Mod.EventBusSubscriber(bus = MOD)` 类里会在启动时直接抛
 * IllegalArgumentException 把整个模组打成 broken state（r375 崩服的原因）。
 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class HakariVisuals {
    /** 车门猛砸：正方形边框向内收拢。 */
    record Door(Vec3 at, Vec3 dir, long born, int life) {}
    static final java.util.List<Door> DOORS = new java.util.ArrayList<>();

    static void add(Network.Effect effect) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        DOORS.add(new Door(effect.position(), effect.direction(), mc.level.getGameTime(),
            Math.max(6, effect.life())));
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || DOORS.isEmpty()) return;
        long now = mc.level.getGameTime();
        DOORS.removeIf(door -> now - door.born() > door.life());
        if (DOORS.isEmpty()) return;
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
            float t = Math.min(1f, (now - door.born()) / (float) door.life());
            double half = 3.2 * (1.0 - t) + 0.25;                 // 由外向内收拢
            int alpha = (int) (235 * (1 - t * 0.3));
            int gold = (alpha << 24) | 0xFFC44D, core = (alpha << 24) | 0xFFF6D6;
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
            for (int i = 0; i < 4; i++) {                          // 四角朝内的火花
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

    private HakariVisuals() {}
}
