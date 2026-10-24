package character;

import cn.blockforge.generated.nnnnr1201forge2.Network;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** 六项一页，注册新角色后自动增加页数；选择与确认分开，避免误切换。 */
public final class CharacterSelectionScreen extends Screen {
    private int page;
    private CharacterDefinition choice=CharacterClient.selected();
    private Button confirm;
    public CharacterSelectionScreen() { super(Component.literal("咒术选择书")); }
    @Override protected void init() {
        int panel=Math.min(440,width-24),left=(width-panel)/2,col=(panel-8)/2;
        var all=CharacterRegistry.all();int rows=3,gap=Math.max(24,Math.min(34,(height-137)/rows));
        for(int i=0;i<6;i++){
            int index=page*6+i;if(index>=all.size())break;var d=all.get(index);
            addRenderableWidget(Button.builder(Component.literal(d.technique()+"（"+d.name()+"）"),b->{choice=d;confirm.active=true;}).bounds(left+(i%2)*(col+8),37+(i/2)*gap,col,gap-4).build());
        }
        confirm=addRenderableWidget(Button.builder(Component.literal("确认选择"),b->{if(choice!=null){Network.CHANNEL.sendToServer(new CharacterPackets.Choose(choice.id()));confirm.active=false;}}).bounds(width/2-102,height-28,98,20).build());confirm.active=choice!=null;
        addRenderableWidget(Button.builder(Component.literal("取消"),b->onClose()).bounds(width/2+4,height-28,98,20).build());
        if(all.size()>6){
            addRenderableWidget(Button.builder(Component.literal("上一页"),b->{page=Math.floorMod(page-1,(all.size()+5)/6);rebuildWidgets();}).bounds(left,12,54,20).build());
            addRenderableWidget(Button.builder(Component.literal("下一页"),b->{page=(page+1)%((all.size()+5)/6);rebuildWidgets();}).bounds(left+panel-54,12,54,20).build());
        }
    }
    @Override public void tick() { if(minecraft.player==null||!minecraft.player.isAlive()){onClose();return;}if(confirm!=null)confirm.active=choice!=null; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        g.fill(0,0,width,height,0xED10151F);g.drawCenteredString(font,"咒术选择书",width/2,16,0xFFFFE4B2);
        int gap=Math.max(24,Math.min(34,(height-137)/3));
        int y=37+3*gap+5,wrap=Math.min(440,width-28);
        if(choice!=null){
            g.drawCenteredString(font,"选择："+choice.name()+" · "+choice.technique()+" · "+choice.resourceName()+"条",width/2,y,choice.color());
            int lineY=y+14;for(var line:font.split(Component.literal(choice.description()),wrap)){if(lineY>height-51)break;g.drawString(font,line,(width-wrap)/2,lineY,0xFFE0DEDA);lineY+=11;}
        }else g.drawCenteredString(font,"先点击角色，再确认选择",width/2,y,0xFFD1D7E2);
        g.drawCenteredString(font,"各角色进度分开保存；切换不回血，战斗中不能切换",width/2,height-42,0xFFAAB4C4);
        super.render(g,mx,my,partial);
    }
}
