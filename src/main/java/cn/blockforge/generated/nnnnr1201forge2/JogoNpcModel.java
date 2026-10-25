package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** 由 tools/bbmodel_to_java.py 从 model.bbmodel 自动生成（方块数 85，贴图 128×128）。 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="sukuna", value=net.minecraftforge.api.distmarker.Dist.CLIENT, bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class JogoNpcModel extends EntityModel<JogoNpcEntity> {
    public static final net.minecraft.client.model.geom.ModelLayerLocation LAYER =
        new net.minecraft.client.model.geom.ModelLayerLocation(new net.minecraft.resources.ResourceLocation("sukuna", "jogo_npc"), "main");
    private final ModelPart root;

    public JogoNpcModel(ModelPart root) { this.root = root; }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition part = mesh.getRoot();
        PartDefinition p0 = part.addOrReplaceChild("e0", CubeListBuilder.create().texOffs(0, 0).addBox(-4.00F, 0.00F, 0.00F, 8.00F, 12.00F, 4.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p1 = part.addOrReplaceChild("e1", CubeListBuilder.create().texOffs(42, 8).addBox(-4.00F, -7.00F, -5.00F, 8.00F, 7.00F, 8.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p2 = part.addOrReplaceChild("e2", CubeListBuilder.create().texOffs(24, 11).addBox(5.00F, 0.00F, -3.00F, 4.00F, 12.00F, 4.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p3 = part.addOrReplaceChild("e3", CubeListBuilder.create().texOffs(0, 27).addBox(-0.10F, 12.00F, -1.75F, 4.00F, 12.00F, 4.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p4 = part.addOrReplaceChild("e4", CubeListBuilder.create().texOffs(16, 27).addBox(-3.90F, 12.00F, -1.75F, 4.00F, 12.00F, 4.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p5 = part.addOrReplaceChild("e5", CubeListBuilder.create().texOffs(0, 52).addBox(1.00F, 0.00F, -5.00F, 2.00F, 2.00F, 5.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p6 = part.addOrReplaceChild("e6", CubeListBuilder.create().texOffs(0, 43).addBox(4.00F, -3.00F, -4.00F, 3.00F, 3.00F, 6.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p7 = part.addOrReplaceChild("e7", CubeListBuilder.create().texOffs(18, 43).addBox(2.00F, -2.00F, -1.00F, 3.00F, 3.00F, 6.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p8 = part.addOrReplaceChild("e8", CubeListBuilder.create().texOffs(36, 43).addBox(-7.00F, -3.00F, -4.00F, 3.00F, 3.00F, 6.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p9 = part.addOrReplaceChild("e9", CubeListBuilder.create().texOffs(14, 52).addBox(-3.00F, 0.00F, -5.00F, 2.00F, 2.00F, 5.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p10 = part.addOrReplaceChild("e10", CubeListBuilder.create().texOffs(0, 16).addBox(0.00F, -3.00F, -2.00F, 4.00F, 3.00F, 8.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p11 = part.addOrReplaceChild("e11", CubeListBuilder.create().texOffs(46, 0).addBox(-5.00F, -2.00F, -1.00F, 3.00F, 3.00F, 6.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p12 = part.addOrReplaceChild("e12", CubeListBuilder.create().texOffs(24, 0).addBox(1.00F, -5.00F, -1.00F, 3.00F, 3.00F, 8.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p13 = part.addOrReplaceChild("e13", CubeListBuilder.create().texOffs(40, 60).addBox(8.00F, -4.00F, -6.00F, 2.00F, 2.00F, 4.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p14 = part.addOrReplaceChild("e14", CubeListBuilder.create().texOffs(62, 16).addBox(-10.00F, -4.00F, -6.00F, 2.00F, 2.00F, 4.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p15 = part.addOrReplaceChild("e15", CubeListBuilder.create().texOffs(62, 22).addBox(1.00F, 1.00F, -5.00F, 4.00F, 7.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p16 = part.addOrReplaceChild("e16", CubeListBuilder.create().texOffs(46, 66).addBox(0.12F, 2.06F, -5.36F, 2.00F, 5.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p17 = part.addOrReplaceChild("e17", CubeListBuilder.create().texOffs(64, 0).addBox(3.84F, 0.14F, -3.82F, 4.00F, 7.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p18 = part.addOrReplaceChild("e18", CubeListBuilder.create().texOffs(64, 38).addBox(4.36F, -0.81F, -2.02F, 4.00F, 3.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p19 = part.addOrReplaceChild("e19", CubeListBuilder.create().texOffs(32, 27).addBox(-9.00F, 0.00F, -3.00F, 4.00F, 12.00F, 4.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p20 = part.addOrReplaceChild("e20", CubeListBuilder.create().texOffs(44, 52).addBox(4.84F, 0.14F, -1.82F, 5.00F, 7.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p21 = part.addOrReplaceChild("e21", CubeListBuilder.create().texOffs(16, 66).addBox(2.36F, 0.19F, 1.98F, 4.00F, 3.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p22 = part.addOrReplaceChild("e22", CubeListBuilder.create().texOffs(54, 42).addBox(2.84F, 1.14F, 2.18F, 5.00F, 7.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p23 = part.addOrReplaceChild("e23", CubeListBuilder.create().texOffs(62, 64).addBox(5.86F, -0.85F, 0.40F, 3.00F, 8.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p24 = part.addOrReplaceChild("e24", CubeListBuilder.create().texOffs(0, 59).addBox(1.86F, -0.85F, 2.40F, 3.00F, 10.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p25 = part.addOrReplaceChild("e25", CubeListBuilder.create().texOffs(64, 30).addBox(-5.00F, 1.00F, -5.00F, 4.00F, 7.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p26 = part.addOrReplaceChild("e26", CubeListBuilder.create().texOffs(52, 64).addBox(-7.84F, 0.14F, -3.82F, 4.00F, 7.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p27 = part.addOrReplaceChild("e27", CubeListBuilder.create().texOffs(68, 52).addBox(-2.12F, 2.06F, -5.36F, 2.00F, 5.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p28 = part.addOrReplaceChild("e28", CubeListBuilder.create().texOffs(36, 66).addBox(-8.36F, -0.81F, -2.02F, 4.00F, 3.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p29 = part.addOrReplaceChild("e29", CubeListBuilder.create().texOffs(56, 50).addBox(-9.84F, 0.14F, -1.82F, 5.00F, 7.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p30 = part.addOrReplaceChild("e30", CubeListBuilder.create().texOffs(66, 42).addBox(-6.36F, 0.19F, 1.98F, 4.00F, 3.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p31 = part.addOrReplaceChild("e31", CubeListBuilder.create().texOffs(28, 57).addBox(-7.84F, 1.14F, 2.18F, 5.00F, 7.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p32 = part.addOrReplaceChild("e32", CubeListBuilder.create().texOffs(28, 65).addBox(-8.86F, -0.85F, 0.40F, 3.00F, 8.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p33 = part.addOrReplaceChild("e33", CubeListBuilder.create().texOffs(8, 59).addBox(-4.86F, -0.85F, 2.40F, 3.00F, 10.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p34 = part.addOrReplaceChild("e34", CubeListBuilder.create().texOffs(48, 20).addBox(-1.14F, -0.85F, 2.40F, 6.00F, 10.00F, 1.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p35 = part.addOrReplaceChild("e35", CubeListBuilder.create().texOffs(40, 11).addBox(0.00F, -3.00F, -2.00F, 4.00F, 3.00F, 6.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p36 = part.addOrReplaceChild("e36", CubeListBuilder.create().texOffs(56, 58).addBox(1.00F, -2.00F, -1.00F, 3.00F, 2.00F, 4.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p37 = part.addOrReplaceChild("e37", CubeListBuilder.create().texOffs(0, 70).addBox(-6.57F, -1.05F, -4.00F, 2.00F, 2.00F, 2.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p38 = part.addOrReplaceChild("e38", CubeListBuilder.create().texOffs(8, 70).addBox(4.57F, -1.05F, -4.00F, 2.00F, 2.00F, 2.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p39 = part.addOrReplaceChild("e39", CubeListBuilder.create().texOffs(48, 37).addBox(-3.00F, -8.78F, -3.27F, 6.00F, 3.00F, 2.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p40 = part.addOrReplaceChild("e40", CubeListBuilder.create().texOffs(28, 52).addBox(-3.00F, -8.78F, -0.73F, 6.00F, 3.00F, 2.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p41 = part.addOrReplaceChild("e41", CubeListBuilder.create().texOffs(16, 59).addBox(-3.00F, -9.00F, -3.00F, 2.00F, 3.00F, 4.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p42 = part.addOrReplaceChild("e42", CubeListBuilder.create().texOffs(60, 9).addBox(1.00F, -9.00F, -3.00F, 2.00F, 3.00F, 4.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p43 = part.addOrReplaceChild("e43", CubeListBuilder.create().texOffs(36, 70).addBox(-3.00F, -8.00F, -4.00F, 2.00F, 2.00F, 1.50F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p44 = part.addOrReplaceChild("e44", CubeListBuilder.create().texOffs(70, 58).addBox(1.00F, -8.00F, -4.00F, 2.00F, 2.00F, 1.50F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p45 = part.addOrReplaceChild("e45", CubeListBuilder.create().texOffs(70, 61).addBox(1.00F, -8.00F, 0.50F, 2.00F, 2.00F, 1.50F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p46 = part.addOrReplaceChild("e46", CubeListBuilder.create().texOffs(70, 64).addBox(-3.00F, -8.00F, 0.50F, 2.00F, 2.00F, 1.50F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p47 = part.addOrReplaceChild("e47", CubeListBuilder.create().texOffs(48, 31).addBox(-2.00F, -9.00F, -3.00F, 4.00F, 2.00F, 4.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p48 = part.addOrReplaceChild("e48", CubeListBuilder.create().texOffs(40, 20).addBox(-4.50F, -6.00F, -3.00F, 1.00F, 3.00F, 3.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p49 = part.addOrReplaceChild("e49", CubeListBuilder.create().texOffs(16, 70).addBox(-5.50F, -5.50F, -2.50F, 1.00F, 2.00F, 2.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p50 = part.addOrReplaceChild("e50", CubeListBuilder.create().texOffs(22, 70).addBox(4.50F, -5.50F, -2.50F, 1.00F, 2.00F, 2.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p51 = part.addOrReplaceChild("e51", CubeListBuilder.create().texOffs(68, 46).addBox(3.50F, -6.00F, -3.00F, 1.00F, 3.00F, 3.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p52 = part.addOrReplaceChild("e52", CubeListBuilder.create().texOffs(43, 9).addBox(-1.00F, 2.70F, -5.50F, 2.00F, 0.30F, 0.30F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p53 = part.addOrReplaceChild("e53", CubeListBuilder.create().texOffs(58, 8).addBox(-1.00F, 2.70F, -5.50F, 2.00F, 0.30F, 0.30F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p54 = part.addOrReplaceChild("e54", CubeListBuilder.create().texOffs(36, 19).addBox(-1.00F, 3.20F, -5.50F, 2.00F, 0.30F, 0.30F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p55 = part.addOrReplaceChild("e55", CubeListBuilder.create().texOffs(38, 62).addBox(-1.00F, 3.20F, -5.50F, 2.00F, 0.30F, 0.30F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p56 = part.addOrReplaceChild("e56", CubeListBuilder.create().texOffs(26, 8).addBox(-2.00F, 2.20F, -5.50F, 3.00F, 0.30F, 0.30F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p57 = part.addOrReplaceChild("e57", CubeListBuilder.create().texOffs(27, 46).addBox(-1.00F, 2.20F, -5.50F, 3.00F, 0.30F, 0.30F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p58 = part.addOrReplaceChild("e58", CubeListBuilder.create().texOffs(40, 9).addBox(-3.00F, -5.82F, -4.82F, 6.00F, 1.00F, 0.50F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p59 = part.addOrReplaceChild("e59", CubeListBuilder.create().texOffs(104, 113).addBox(-0.01F, -7.83F, -1.01F, 0.01F, 0.01F, 0.01F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p60 = part.addOrReplaceChild("e60", CubeListBuilder.create().texOffs(111, 104).addBox(-0.01F, -7.83F, -1.01F, 0.02F, 0.02F, 0.02F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p61 = part.addOrReplaceChild("e61", CubeListBuilder.create().texOffs(111, 104).addBox(-0.01F, -7.84F, -1.01F, 0.02F, 0.02F, 0.02F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p62 = part.addOrReplaceChild("e62", CubeListBuilder.create().texOffs(104, 113).addBox(-0.01F, -7.83F, -1.01F, 0.01F, 0.01F, 0.01F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p63 = part.addOrReplaceChild("e63", CubeListBuilder.create().texOffs(111, 104).addBox(-0.01F, -7.83F, -1.01F, 0.02F, 0.02F, 0.02F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p64 = part.addOrReplaceChild("e64", CubeListBuilder.create().texOffs(111, 104).addBox(-0.01F, -7.84F, -1.01F, 0.02F, 0.02F, 0.02F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p65 = part.addOrReplaceChild("e65", CubeListBuilder.create().texOffs(104, 113).addBox(-0.01F, -7.83F, -1.01F, 0.01F, 0.01F, 0.01F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p66 = part.addOrReplaceChild("e66", CubeListBuilder.create().texOffs(111, 104).addBox(-0.01F, -7.83F, -1.01F, 0.02F, 0.02F, 0.02F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p67 = part.addOrReplaceChild("e67", CubeListBuilder.create().texOffs(111, 104).addBox(-0.01F, -7.84F, -1.01F, 0.02F, 0.02F, 0.02F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p68 = part.addOrReplaceChild("e68", CubeListBuilder.create().texOffs(118, 113).addBox(-0.03F, -7.89F, -1.03F, 0.06F, 0.06F, 0.06F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p69 = part.addOrReplaceChild("e69", CubeListBuilder.create().texOffs(111, 113).addBox(-0.03F, -7.89F, -1.03F, 0.07F, 0.07F, 0.06F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p70 = part.addOrReplaceChild("e70", CubeListBuilder.create().texOffs(118, 113).addBox(-0.04F, -7.91F, -1.04F, 0.07F, 0.08F, 0.08F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p71 = part.addOrReplaceChild("e71", CubeListBuilder.create().texOffs(111, 113).addBox(-0.03F, -7.89F, -1.03F, 0.06F, 0.06F, 0.06F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p72 = part.addOrReplaceChild("e72", CubeListBuilder.create().texOffs(118, 113).addBox(-0.03F, -7.89F, -1.03F, 0.07F, 0.07F, 0.06F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p73 = part.addOrReplaceChild("e73", CubeListBuilder.create().texOffs(0, 0).addBox(-0.09F, -8.22F, -1.00F, 0.17F, 0.40F, 0.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p74 = part.addOrReplaceChild("e74", CubeListBuilder.create().texOffs(0, 0).addBox(-0.08F, -8.20F, -1.00F, 0.16F, 0.38F, 0.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p75 = part.addOrReplaceChild("e75", CubeListBuilder.create().texOffs(0, 0).addBox(-0.04F, -8.04F, -1.00F, 0.09F, 0.21F, 0.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p76 = part.addOrReplaceChild("e76", CubeListBuilder.create().texOffs(0, 0).addBox(-0.04F, -8.03F, -1.00F, 0.08F, 0.21F, 0.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p77 = part.addOrReplaceChild("e77", CubeListBuilder.create().texOffs(0, 0).addBox(-0.04F, -8.06F, -1.00F, 0.09F, 0.24F, 0.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p78 = part.addOrReplaceChild("e78", CubeListBuilder.create().texOffs(0, 0).addBox(-0.04F, -8.05F, -1.00F, 0.08F, 0.23F, 0.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p79 = part.addOrReplaceChild("e79", CubeListBuilder.create().texOffs(0, 0).addBox(-0.04F, -8.04F, -1.00F, 0.09F, 0.21F, 0.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p80 = part.addOrReplaceChild("e80", CubeListBuilder.create().texOffs(0, 0).addBox(-0.04F, -8.03F, -1.00F, 0.08F, 0.21F, 0.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p81 = part.addOrReplaceChild("e81", CubeListBuilder.create().texOffs(0, 0).addBox(-0.04F, -8.06F, -1.00F, 0.09F, 0.24F, 0.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p82 = part.addOrReplaceChild("e82", CubeListBuilder.create().texOffs(0, 0).addBox(-0.04F, -8.05F, -1.00F, 0.08F, 0.23F, 0.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p83 = part.addOrReplaceChild("e83", CubeListBuilder.create().texOffs(0, 0).addBox(-0.04F, -8.04F, -1.00F, 0.09F, 0.21F, 0.00F, CubeDeformation.NONE), PartPose.ZERO);
        PartDefinition p84 = part.addOrReplaceChild("e84", CubeListBuilder.create().texOffs(0, 0).addBox(-0.04F, -8.03F, -1.00F, 0.08F, 0.21F, 0.00F, CubeDeformation.NONE), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void registerLayer(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, JogoNpcModel::createBodyLayer);
    }

    @Override public void setupAnim(JogoNpcEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) { }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
