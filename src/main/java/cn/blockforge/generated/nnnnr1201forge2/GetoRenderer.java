package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 夏油杰渲染器：HD 皮肤 + 原版宽臂玩家骨架。
 * <p>动作全部在 {@link Model#setupAnim} 里按同步下来的 {@code action()} 手写关节姿态，
 * 与 {@link GuardianRenderer} 同一套惯例（本仓库不用 GeckoLib，也没有动画文件）。
 */
public final class GetoRenderer extends MobRenderer<GetoEntity, GetoRenderer.Model> {
    private static final ResourceLocation SKIN = new ResourceLocation("sukuna", "textures/entity/geto.png");

    public GetoRenderer(EntityRendererProvider.Context context) { super(context, new Model(), 0.6f); }

    @Override public ResourceLocation getTextureLocation(GetoEntity entity) { return SKIN; }

    public static final class Model extends PlayerModel<GetoEntity> {
        Model() { super(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64).bakeRoot(), false); }

        @Override public void setupAnim(GetoEntity e, float swing, float amount, float age, float yaw, float pitch) {
            body.xRot = body.yRot = body.zRot = 0;
            super.setupAnim(e, swing, amount, age, yaw, pitch);
            float t = e.actionAge() + amount;
            switch (e.action()) {
                // 挥手/召唤：右臂前挥，左臂微抬，身体前倾。
                case GetoEntity.WAVE -> {
                    float s = (float) Math.sin(t * 0.35f);
                    rightArm.xRot = -1.05f + s * 0.75f;
                    rightArm.zRot = 0.15f + s * 0.25f;
                    leftArm.xRot = -0.35f;
                    leftArm.zRot = -0.12f;
                    body.xRot = 0.08f;
                }
                // 蓄力/吸收：双手张开上举，越到后面抬得越高。
                case GetoEntity.CHARGE -> {
                    float k = Math.min(1f, t / 60f);
                    rightArm.xRot = -1.35f - k * 0.35f;
                    leftArm.xRot = -1.35f - k * 0.35f;
                    rightArm.zRot = 0.45f + k * 0.25f;
                    leftArm.zRot = -0.45f - k * 0.25f;
                    body.xRot = -0.15f * k;
                }
                // 投掷：双手向前推出。
                case GetoEntity.THROW -> {
                    rightArm.xRot = -1.5f;
                    leftArm.xRot = -1.5f;
                    rightArm.zRot = 0.05f;
                    leftArm.zRot = -0.05f;
                    body.xRot = 0.12f;
                }
                // 抽武器：右手伸向背后。
                case GetoEntity.DRAW -> {
                    rightArm.xRot = 0.55f;
                    rightArm.yRot = -0.9f;
                    rightArm.zRot = -0.35f;
                    leftArm.xRot = -0.4f;
                    body.yRot = -0.18f;
                }
                // 长棍横扫：三连挥，按剩余次数决定挥击方向。
                case GetoEntity.SWEEP -> {
                    float phase = (t % 8f) / 8f;
                    float swingArc = (float) Math.sin(phase * Math.PI);
                    rightArm.xRot = -1.2f + swingArc * 1.1f;
                    rightArm.yRot = -0.7f + swingArc * 1.4f;
                    rightArm.zRot = 0.25f;
                    leftArm.xRot = -0.6f;
                    body.yRot = -0.35f + swingArc * 0.7f;
                }
                // 拳击：常态近战，左右交替。
                case GetoEntity.PUNCH -> {
                    boolean right = ((int) (t / 6f) & 1) == 0;
                    (right ? rightArm : leftArm).xRot = -1.45f;
                    (right ? leftArm : rightArm).xRot = -0.4f;
                    body.yRot = right ? -0.2f : 0.2f;
                }
                default -> { }
            }
            rightSleeve.copyFrom(rightArm);
            leftSleeve.copyFrom(leftArm);
            jacket.copyFrom(body);
            rightPants.copyFrom(rightLeg);
            leftPants.copyFrom(leftLeg);
            hat.copyFrom(head);
            hat.visible = false;
        }
    }
}
