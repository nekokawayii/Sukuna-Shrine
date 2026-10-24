package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 分页名单由服务器回传，按钮不自行假定修改成功。 */
public final class RelationsScreen extends Screen {
    private Network.Relations state;
    private int page,ticks;
    private boolean pending=true;
    public RelationsScreen(){super(Component.literal("宿傩 · 敌友面板"));}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){rebuild();request();}
    private void request(){Network.CHANNEL.sendToServer(new Network.RelationRequest(page));}
    public void receive(Network.Relations value){state=value;page=value.page();pending=false;rebuild();}
    private int left(){return (width-Math.min(440,width-16))/2;}
    private int panelWidth(){return Math.min(440,width-16);}
    private int rowHeight(){return Math.min(27,Math.max(19,(height-132)/CombatSettings.PAGE_SIZE));}
    private void rebuild(){
        clearWidgets();int left=left(),w=panelWidth();
        if(state!=null)for(int i=0;i<state.rows().size();i++){
            Network.RelationRow row=state.rows().get(i);int y=60+i*rowHeight();
            addChoice(row,CombatSettings.ALLY,"盟友",left+w-145,y);
            addChoice(row,CombatSettings.ENEMY,"敌人",left+w-97,y);
            addChoice(row,CombatSettings.DEFAULT,"默认",left+w-49,y);
        }
        Button previous=addRenderableWidget(Button.builder(Component.literal("上一页"),b->{page--;pending=true;rebuild();request();}).bounds(left,height-45,65,20).build());
        previous.active=!pending&&page>0;
        Button next=addRenderableWidget(Button.builder(Component.literal("下一页"),b->{page++;pending=true;rebuild();request();}).bounds(left+70,height-45,65,20).build());
        next.active=!pending&&state!=null&&page+1<state.pages();
        addRenderableWidget(Button.builder(Component.literal("关闭"),b->onClose()).bounds(left+w-65,height-45,65,20).build());
        addRenderableWidget(Button.builder(Component.literal("领域地面虚空："+(state!=null&&state.voidFloor()?"开":"关")),b->{Network.CHANNEL.sendToServer(new Network.SettingsToggle(1));pending=true;request();rebuild();}).bounds(left,height-70,150,20).build());
    }
    private void addChoice(Network.RelationRow row,int mode,String text,int x,int y){
        Button button=addRenderableWidget(Button.builder(Component.literal((row.mode()==mode?"✓":"")+text),b->{pending=true;rebuild();Network.CHANNEL.sendToServer(new Network.RelationChange(row.id(),mode,page));}).bounds(x,y,46,18).build());
        button.active=!pending&&row.mode()!=mode;
    }
    @Override public void tick(){if(++ticks%40==0)request();}
    @Override public boolean keyPressed(int key,int scan,int mods){if(Client.RELATIONS.matches(key,scan)){onClose();return true;}return super.keyPressed(key,scan,mods);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0xED100D18);int left=left(),w=panelWidth();
        g.drawCenteredString(font,title,width/2,10,0xFFFFD8E4);
        g.drawCenteredString(font,"控制你的技能和十影式神对玩家的索敌与伤害",width/2,26,0xFFBBAAC8);
        String info=state==null?"正在读取名单……":"术式伤害 ×"+state.multiplier()+"   第 "+(page+1)+" / "+state.pages()+" 页";
        g.drawCenteredString(font,info,width/2,42,0xFFFFCE7C);
        if(state!=null){
            if(state.rows().isEmpty())g.drawCenteredString(font,"暂无其他在线玩家或已保存的敌友",width/2,85,0xFFCCC0D2);
            for(int i=0;i<state.rows().size();i++){
                Network.RelationRow row=state.rows().get(i);int y=60+i*rowHeight();
                int color=row.mode()==CombatSettings.ALLY?0xFF79DBA1:row.mode()==CombatSettings.ENEMY?0xFFFF798B:0xFFC4B9CE;
                g.fill(left,y-1,left+w,y+19,0xFF251D30);
                String name=(row.online()?"● ":"○ ")+row.name();
                g.drawString(font,font.plainSubstrByWidth(name,Math.max(20,w-153)),left+4,y+5,color);
            }
        }
        g.drawCenteredString(font,"仅敌人会被你的术式锁定；盟友和默认均不攻击",width/2,height-61,0xFFBBAAC8);
        g.drawCenteredString(font,"● 在线  ○ 离线   设置自动保存",width/2,height-15,0xFF93839F);
        super.render(g,mx,my,partial);
    }
}
