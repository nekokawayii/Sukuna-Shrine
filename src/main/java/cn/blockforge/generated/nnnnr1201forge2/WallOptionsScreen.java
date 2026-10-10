package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class WallOptionsScreen extends Screen {
    private int halfBlocks=Math.max(1,Math.min(20,Client.progress.wallSpacing()));
    private boolean diagonal=Client.progress.wallDiagonal();
    public WallOptionsScreen(){super(Component.literal("墙 · 斩击设置"));}
    @Override protected void init(){
        addRenderableWidget(new AbstractSliderButton(width/2-110,height/2-38,220,20,Component.empty(),(halfBlocks-1)/19.0){
            {updateMessage();}
            @Override protected void updateMessage(){setMessage(Component.literal("斩击间隔："+BranchRules.spacing(halfBlocks)+" 格"));}
            @Override protected void applyValue(){halfBlocks=1+(int)Math.round(value*19);updateMessage();}
        });
        addRenderableWidget(Button.builder(Component.literal("网格角度："+(diagonal?"45°":"90°")),b->{diagonal=!diagonal;b.setMessage(Component.literal("网格角度："+(diagonal?"45°":"90°")));}).bounds(width/2-110,height/2-10,220,20).build());
        addRenderableWidget(Button.builder(Component.literal("确定并选中墙"),b->{Network.CHANNEL.sendToServer(new Network.WallOptions(halfBlocks,diagonal));Client.selected=BranchRules.WALL;minecraft.setScreen(null);}).bounds(width/2-110,height/2+50,220,20).build());
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        g.fill(0,0,width,height,0xE8100D18);WheelBackdrop.draw(g,width,height,minecraft.level==null?0:minecraft.level.getGameTime()+partial);
        g.drawCenteredString(font,"墙 · "+Client.wallSize()+"×"+Client.wallSize()+" 格",width/2,height/2-75,0xFFFFD5E4);
        g.drawCenteredString(font,"间隔越小，斩击越密，咒力消耗越多",width/2,height/2-58,0xFFD6BFDA);
        int cost=BranchRules.wallCost(halfBlocks);g.drawCenteredString(font,BranchRules.wallLines(halfBlocks,Client.wallSize())+" 道斩击 · 消耗 "+Client.energyText(cost)+" 咒力",width/2,height/2+20,Client.progress.energy()>=cost?0xFFA8ECCD:0xFFFF8C95);
        g.drawCenteredString(font,"确定后按 "+Client.CAST.getTranslatedKeyMessage().getString()+" 向准星方向释放",width/2,height/2+36,0xFFCFC5D0);super.render(g,x,y,partial);
    }
}
