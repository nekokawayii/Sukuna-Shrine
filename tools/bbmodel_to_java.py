# -*- coding: utf-8 -*-
"""把 Blockbench .bbmodel 转成 Java EntityModel（方案 B：手写 ModelPart + CubeListBuilder）。
只处理 axis-aligned cube（忽略每块旋转），按 box_uv 取 texOffs；坐标系做 Y 翻转。
"""
import json
import sys

SRC = sys.argv[1]
OUT = sys.argv[2]
CLASS = sys.argv[3]
LAYER = sys.argv[4]

d = json.load(open(SRC, encoding='utf-8'))
res = d.get('resolution') or {'width': 64, 'height': 64}
TW, TH = int(res['width']), int(res['height'])
els = [e for e in d.get('elements', []) if e.get('type', 'cube') == 'cube' and e.get('visibility', True)]

lines = []
for i, e in enumerate(els):
    fx, fy, fz = e['from']
    tx, ty, tz = e['to']
    # bbmodel: Y 向上、脚底为 0；MC 模型空间：Y 向下、脚底为 24。
    y0 = 24.0 - ty
    w, h, dep = tx - fx, ty - fy, tz - fz
    # box uv：取所有面 uv 的最小 u/v 作为展开起点
    u, v = 1e9, 1e9
    for f, data in e.get('faces', {}).items():
        uv = data.get('uv')
        if uv:
            u, v = min(u, uv[0]), min(v, uv[1])
    if u > 1e8:
        u, v = 0, 0
    lines.append(
        f'        PartDefinition p{i} = part.addOrReplaceChild("e{i}", CubeListBuilder.create().texOffs({u:.0f}, {v:.0f}).addBox('
        f'{fx:.2f}F, {y0:.2f}F, {fz:.2f}F, {w:.2f}F, {h:.2f}F, {dep:.2f}F, CubeDeformation.NONE), PartPose.ZERO);')

body = '\n'.join(lines)

java = f'''package cn.blockforge.generated.nnnnr1201forge2;

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

/** 由 tools/bbmodel_to_java.py 从 model.bbmodel 自动生成（方块数 {len(els)}，贴图 {TW}×{TH}）。 */
public final class {CLASS} extends EntityModel<JogoNpcEntity> {{
    private final ModelPart root;

    public {CLASS}(ModelPart root) {{ this.root = root; }}

    public static LayerDefinition createBodyLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition part = mesh.getRoot();
{body}
        return LayerDefinition.create(mesh, {TW}, {TH});
    }}

    @Override public void setupAnim(JogoNpcEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {{ }}

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {{
        root.render(pose, buffer, light, overlay, r, g, b, a);
    }}
}}
'''
open(OUT, 'w', encoding='utf-8').write(java)
print(f'生成 {OUT}: {len(els)} 个方块, 贴图 {TW}x{TH}')
