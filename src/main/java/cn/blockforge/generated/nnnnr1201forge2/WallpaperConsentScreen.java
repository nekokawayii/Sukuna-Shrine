package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 不默认勾选、不自动同意；关闭或按Esc都按本次拒绝处理。 */
public final class WallpaperConsentScreen extends Screen {
    private static final String QUESTION="是否愿意在斩击中显示并分享你的桌面壁纸？（拒绝则内部显示纯黑）";
    private static final String NOTICE="同意后会读取当前静态壁纸图片，并通过服务器分享给同服全部玩家。适用于空间斩、世界斩，以及虚空和传承阶段的全部斩击（含剑气模式）。不会截取屏幕、任务栏、桌面图标或其他窗口。";
    private static final String WARNING="请勿分享含隐私的壁纸。关闭会清除本模组的会话缓存，但无法收回别人已经保存的图片。始终开启会在以后进入其他服务器时继续分享。";
    private boolean decided;
    public WallpaperConsentScreen(){super(Component.literal("斩击壁纸分享授权"));}
    @Override protected void init(){
        int panel=Math.min(420,width-24),button=(panel-8)/2,x=(width-panel)/2,y=height-56;
        addRenderableWidget(Button.builder(Component.literal("仅本次同意"),b->choose(true,false)).bounds(x,y,button,20).build());
        addRenderableWidget(Button.builder(Component.literal("仅本次拒绝"),b->choose(false,false)).bounds(x+button+8,y,button,20).build());
        addRenderableWidget(Button.builder(Component.literal("始终开启并分享"),b->choose(true,true)).bounds(x,y+24,button,20).build());
        addRenderableWidget(Button.builder(Component.literal("始终关闭"),b->choose(false,true)).bounds(x+button+8,y+24,button,20).build());
    }
    private void choose(boolean on,boolean remember){decided=true;WallpaperClient.choose(on,remember);minecraft.setScreen(null);}
    @Override public void onClose(){choose(false,false);}
    @Override public void removed(){if(!decided){decided=true;WallpaperClient.choose(false,false);}}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        renderBackground(g);int panel=Math.min(420,width-24),x=(width-panel)/2,y=12;
        g.drawCenteredString(font,title,width/2,y,0xFFFFD68A);y+=20;
        g.drawWordWrap(font,Component.literal(QUESTION),x,y,panel,0xFFFFFFFF);y+=font.split(Component.literal(QUESTION),panel).size()*9+8;
        g.drawWordWrap(font,Component.literal(NOTICE),x,y,panel,0xFFD8E5F1);y+=font.split(Component.literal(NOTICE),panel).size()*9+8;
        g.drawWordWrap(font,Component.literal(WARNING),x,y,panel,0xFFFFAD89);y+=font.split(Component.literal(WARNING),panel).size()*9+8;
        if(y+12<height-60)g.drawWordWrap(font,Component.literal("随时输入 /sukuna_wallpaper 修改，无须管理员权限。"),x,y,panel,0xFFA8D9C6);
        super.render(g,mouseX,mouseY,partial);
    }
}
