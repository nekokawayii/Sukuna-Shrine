package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;

/** 界面里画「实体预览」的安全封装。
 *  原版绘制路径从渲染表拿渲染器后完全不检查 null，一旦某个实体类型不在表里
 *  （资源重载把 renderers 字段整个换成新表、或别的模组覆盖过它），客户端直接崩溃——
 *  十影界面那次崩溃就是这么来的。这里先确认渲染器存在、再套 try/catch 绘制；
 *  失败返回 false，由调用方改画 2D 占位图，绝不让一个预览毁掉整个游戏。 */
public final class GuiEntityPreview {
    private GuiEntityPreview(){}
    /** 成功画出返回 true；实体为 null、世界已卸载、渲染器缺失或绘制抛错都返回 false。 */
    public static boolean render(GuiGraphics g,int x,int y,int scale,float angleX,float angleY,LivingEntity entity){
        if(entity==null)return false;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null)return false; // 退出世界后屏幕还在画时，跳过 3D 预览
        try{
            if(mc.getEntityRenderDispatcher().getRenderer(entity)==null){
                System.out.println("[宿傩] 实体预览缺少渲染器，已改用二维占位："+net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
                return false;
            }
            InventoryScreen.renderEntityInInventoryFollowsMouse(g,x,y,scale,angleX,angleY,entity);
            return true;
        }catch(Throwable t){
            System.out.println("[宿傩] 实体预览绘制抛错，已改用二维占位："+net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType())+" "+t);
            return false;
        }
    }
}
