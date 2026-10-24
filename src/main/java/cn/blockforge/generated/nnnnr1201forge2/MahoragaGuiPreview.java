package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

/** 十影界面中央的魔虚罗预览：**直接在界面层画原始 bbmodel**。
 *
 *  <p>不再走原版 {@code InventoryScreen} 的实体预览通道——那条路要先从全局渲染表里
 *  拿到实体渲染器，而渲染表会被资源重载或别的模组整个换掉，一丢渲染器预览就静默消失
 *  （历次"魔虚罗看不见 / 点击十影崩溃"都是这个根因）。这里不创建实体、不查渲染表、
 *  不依赖实体状态：只有模型、贴图和界面自己的位姿栈，尺寸与位置完全可控。</p> */
public final class MahoragaGuiPreview {
    private static final BbModel BODY=new BbModel("mahoraga","mahoraga");
    /** 模型高 3.69 格，与 MahoragaRenderer 一致。 */
    private static final float HEIGHT=3.69f;
    /**
     * 以 (cx, baseY) 为脚底中心画一尊站立的魔虚罗。
     * @param heightPx 模型要占的像素高
     * @param seconds  待机动画时间（秒）
     * @param spin     水平转角（度，0=正对玩家）
     * @return 画成功返回 true；模型或贴图读不到返回 false，由调用方改画二维占位
     */
    public static boolean render(GuiGraphics g,int cx,int baseY,int heightPx,float seconds,float spin){
        if(heightPx<=0)return false;
        PoseStack ps=g.pose();
        ps.pushPose();
        boolean ok=false;
        try{
            com.mojang.blaze3d.platform.Lighting.setupForEntityInInventory(); // 与原版界面实体预览同款光照
            float k=heightPx/HEIGHT; // 每格模型高度对应的 GUI 像素
            // 与原版界面实体预览同一套位姿链：平移→缩放(翻转z)→绕Z转180°（把向下的y轴翻正）→绕Y转向
            ps.translate(cx,baseY,50.0);
            ps.mulPoseMatrix(new Matrix4f().scaling(k,k,-k));
            ps.mulPose(Axis.ZP.rotationDegrees(180.0F));
            ps.mulPose(Axis.YP.rotationDegrees(180.0F-spin));
            BODY.render(ps,g.bufferSource(),15728880,"idle",seconds,1,1,1,1);
            g.flush();
            ok=true;
        }catch(Throwable t){
            System.out.println("[宿傩] 十影界面直绘魔虚罗失败，改用二维占位："+t);
        }finally{
            ps.popPose();
            try{com.mojang.blaze3d.platform.Lighting.setupFor3DItems();}catch(Throwable ignored){} // 与原版一致：画完还原光照
        }
        return ok;
    }
    private MahoragaGuiPreview(){}
}
