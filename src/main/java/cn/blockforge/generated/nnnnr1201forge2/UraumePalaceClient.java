package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 里梅·霜天宫殿的天空（r288）：在领域上方铺一张月亮雪云贴图。 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class UraumePalaceClient {
    private static final ResourceLocation SKY =
        new ResourceLocation("sukuna", "textures/effect/domain_snow_moon.png");
    private static Vec3 center;
    private static float radius;
    private static long until;

    public static void set(Network.Effect e) {
        if (e.size() <= 0) {
            center = null;
            return;
        }
        center = e.position();
        radius = e.size();
        until = System.currentTimeMillis() + Math.max(1000, e.life() * 50L);
    }

    public static boolean inside() {
        var p = Minecraft.getInstance().player;
        return center != null && p != null && p.position().distanceTo(center) < radius + 4 && System.currentTimeMillis() < until;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (center == null || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (System.currentTimeMillis() > until) { center = null; return; }
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SKY);
        double y = center.y + radius * 0.9, s = radius * 2.2;
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.vertex(ps.last().pose(), (float) (center.x - s), (float) y, (float) (center.z - s)).uv(0, 0).endVertex();
        b.vertex(ps.last().pose(), (float) (center.x - s), (float) y, (float) (center.z + s)).uv(0, 1).endVertex();
        b.vertex(ps.last().pose(), (float) (center.x + s), (float) y, (float) (center.z + s)).uv(1, 1).endVertex();
        b.vertex(ps.last().pose(), (float) (center.x + s), (float) y, (float) (center.z - s)).uv(1, 0).endVertex();
        BufferUploader.drawWithShader(b.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ps.popPose();
    }

    private UraumePalaceClient() { }
}
