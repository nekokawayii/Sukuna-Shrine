package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 十影目录：独立黑白犬、批量脱兔、无限次调服；保留魔虚罗双显现入口。 */
public final class TenShadowsScreen extends Screen {
    private int page,batch=30;
    private boolean wasFused;
    private final List<Button> liveButtons=new ArrayList<>();
    private final long opened=System.nanoTime();
    public TenShadowsScreen(){super(Component.literal("十种影法术"));}
    private List<Integer> kinds(){List<Integer> out=new ArrayList<>();for(int k=0;k<11;k++)if(ShadowClient.fused()?k!=0&&k!=1:k!=2)out.add(k);return out;}
    private int left(){return Math.max(8,(width-540)/2);}
    private int cellWidth(){return (width-left()*2-12)/3;}
    private int rowHeight(){return Math.max(52,Math.min(76,(height-116)/2));}
    private Button button(String text,int x,int y,int w,java.util.function.Consumer<Button> action){Button b=Button.builder(Component.literal(text),action::accept).bounds(x,y,w,18).build();addRenderableWidget(b);return b;}
    @Override protected void init(){
        clearWidgets();liveButtons.clear();wasFused=ShadowClient.fused();ShadowClient.send(0,0,0);
        button("式神一",left(),27,64,b->{page=0;init();});button("式神二",left()+68,27,64,b->{page=1;init();});button("魔虚罗",left()+136,27,72,b->{page=2;init();});
        button("关闭",width-left()-48,27,48,b->onClose());
        int bottom=height-47;
        if(page<2){
            List<Integer> kinds=kinds();int cw=cellWidth(),rh=rowHeight();
            for(int i=page*6;i<Math.min(kinds.size(),page*6+6);i++){
                int k=kinds.get(i),slot=i-page*6,x=left()+(slot%3)*(cw+6),y=53+(slot/3)*rh;
                Button summon=button(ShadowClient.tamed(k)?"召唤":"调服",x+4,y+rh-24,(cw-12)/2,b->{ShadowClient.send(ShadowClient.tamed(k)?1:2,k,k==7?batch:1);});
                Button recall=button("收回",x+8+(cw-12)/2,y+rh-24,(cw-12)/2,b->ShadowClient.send(3,k,0));
                liveButtons.add(summon);liveButtons.add(recall);
            }
            Button dual=button("黑白同时召唤",left(),bottom,105,b->ShadowClient.send(5,0,1));dual.active=!ShadowClient.fused();
            button("脱兔每批："+batch,left()+111,bottom,105,b->{batch=batch==30?1:batch==1?10:30;b.setMessage(Component.literal("脱兔每批："+batch));});
        }else{
            int cx=width/2,y=height-71,cd=TenShadowsClient.manifestCooldown();
            Button ritual=button(TenShadowsClient.tamed()?"已调服":"开始调服",cx-126,y,80,b->{Network.CHANNEL.sendToServer(new TenShadows.Summon(0));onClose();});ritual.active=!TenShadowsClient.tamed();
            Button full=button("完全显现",cx-40,y,80,b->{Network.CHANNEL.sendToServer(new TenShadows.Summon(1));onClose();});full.active=TenShadowsClient.tamed()&&cd==0;
            Button partial=button(TenShadowsClient.partial()?"解除法轮":"不完全显现",cx+46,y,88,b->{Network.CHANNEL.sendToServer(new TenShadows.Summon(2));onClose();});partial.active=TenShadowsClient.partial()||TenShadowsClient.tamed()&&cd==0;
        }
        button("全部收回",width-left()-82,bottom,82,b->ShadowClient.send(6,0,0));
    }
    @Override public void onClose(){
        com.mojang.logging.LogUtils.getLogger().info("[十影] 界面被关闭", new Throwable("close-trace"));
        super.onClose();
    }
    @Override public void removed(){
        com.mojang.logging.LogUtils.getLogger().info("[十影] 界面 removed（被替换/关闭）");
        super.removed();
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0xE910101B);g.drawCenteredString(font,"十种影法术",width/2,9,0xFFFFE2A5);
        if(page<2){
            List<Integer> kinds=kinds();int cw=cellWidth(),rh=rowHeight(),index=0;
            for(int i=page*6;i<Math.min(kinds.size(),page*6+6);i++){
                int k=kinds.get(i),slot=i-page*6,x=left()+(slot%3)*(cw+6),y=53+(slot/3)*rh;boolean tamed=ShadowClient.tamed(k);int count=ShadowClient.active(k);
                g.fill(x,y,x+cw,y+rh-4,0xFF292633);g.fill(x,y,x+cw,y+2,tamed?0xFFBCB185:0xFF67536D);
                g.drawString(font,TenShadowRules.NAMES[k]+(tamed?" · 已调服":" · 未调服"),x+5,y+6,tamed?0xFFE9DCBA:0xFFD3A7BA);
                g.drawString(font,count>0?"在场 "+count+" · 最长 "+ShadowClient.seconds(k)+"秒":"消耗 "+TenShadowRules.cost(k)+" · "+TenShadowRules.duration(k)/20+"秒",x+5,y+19,0xFFADB3C8);
                if(index+1<liveButtons.size()){Button summon=liveButtons.get(index++),recall=liveButtons.get(index++);summon.setMessage(Component.literal(tamed?"召唤":"调服"));summon.active=!TenShadowsClient.locked()&&(!tamed||count<TenShadowRules.limit(k));recall.active=count>0;}
            }
            super.render(g,mx,my,partial);
            for(int i=page*6;i<Math.min(kinds.size(),page*6+6);i++){int k=kinds.get(i),slot=i-page*6,x=left()+(slot%3)*(cw+6),y=53+(slot/3)*rh;if(mx>=x&&mx<x+cw&&my>=y&&my<y+rh-27)g.renderTooltip(font,font.split(Component.literal(TenShadowRules.DETAILS[k]+(ShadowClient.tamed(k)?"":"；调服不扣咒力，可反复挑战")),Math.min(260,width-30)),mx,my);}
        }else{
            int feet=height-92;float seconds=(System.nanoTime()-opened)/1_000_000_000f;MahoragaGuiPreview.render(g,width/2,feet,Math.max(42,height-160),seconds,seconds*20%360);
            g.drawCenteredString(font,"魔虚罗 · "+(TenShadowsClient.tamed()?"已调服":"未调服"),width/2,53,0xFFFFE2A5);
            g.drawCenteredString(font,"两种显现互斥 · 冷却 "+TenShadowsClient.manifestCooldown()+"秒",width/2,67,0xFFADB3C8);super.render(g,mx,my,partial);
        }
        g.drawCenteredString(font,"L 收回准星式神 · 调服可反复挑战 · 普通式神到时化为黑水",width/2,height-18,0xFFADB3C8);
    }
    @Override public void tick(){if(!TenShadowsClient.has()||minecraft.level==null||minecraft.player==null||!minecraft.player.isAlive()){onClose();return;}if(wasFused!=ShadowClient.fused())init();}
}
