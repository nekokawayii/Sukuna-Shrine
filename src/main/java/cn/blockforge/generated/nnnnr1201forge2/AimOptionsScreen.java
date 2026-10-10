package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 控制器中的个人索敌设置页。 */
public final class AimOptionsScreen extends Screen {
    private final Screen parent;
    private Network.ControllerState state;
    private EditBox angle,range;
    private Button enabled,circle;
    private String angleDraft,rangeDraft,message="范围越小，越接近准星才会被选中";
    public AimOptionsScreen(Screen parent){super(Component.literal("准星附近自动索敌"));this.parent=parent;}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){
        int x=width/2-140,y=Math.max(30,(height-190)/2);
        enabled=addRenderableWidget(Button.builder(Component.literal("读取设置中"),b->Network.sendControllerRequest(AimAssist.ENABLED,1)).bounds(x,y+26,280,20).build());
        angle=new EditBox(font,x+176,y+54,104,20,Component.literal("半角0.5～15度"));angle.setMaxLength(8);angle.setValue(angleDraft==null?"3":angleDraft);angle.setResponder(s->angleDraft=s);addRenderableWidget(angle);
        range=new EditBox(font,x+176,y+82,104,20,Component.literal("距离4～256格"));range.setMaxLength(8);range.setValue(rangeDraft==null?"64":rangeDraft);range.setResponder(s->rangeDraft=s);addRenderableWidget(range);
        circle=addRenderableWidget(Button.builder(Component.literal("读取设置中"),b->Network.sendControllerRequest(AimAssist.CIRCLE,1)).bounds(x,y+110,280,20).build());
        addRenderableWidget(Button.builder(Component.literal("保存范围与距离"),b->save()).bounds(x,y+158,170,20).build());
        addRenderableWidget(Button.builder(Component.literal("返回控制器"),b->onClose()).bounds(x+178,y+158,102,20).build());
        update();Network.sendControllerRequest(-1,0);
    }
    private void update(){
        enabled.active=circle.active=state!=null;
        if(state==null)return;var t=state.guardian();
        enabled.setMessage(Component.literal("小范围自动索敌："+(t.getBoolean("AimEnabled")?"开启":"关闭")));
        circle.setMessage(Component.literal("准星小圆圈："+(t.getBoolean("AimCircle")?"显示":"隐藏")));
    }
    public void receive(Network.ControllerState value){state=value;if(angleDraft==null)angle.setValue(Double.toString(value.guardian().getDouble("AimAngle")));if(rangeDraft==null)range.setValue(Double.toString(value.guardian().getDouble("AimRange")));update();}
    private void save(){
        try{double a=Double.parseDouble(angle.getValue()),r=Double.parseDouble(range.getValue());
            if(!Double.isFinite(a)||a<AimRules.MIN_ANGLE||a>AimRules.MAX_ANGLE||!Double.isFinite(r)||r<AimRules.MIN_RANGE||r>AimRules.MAX_RANGE)throw new NumberFormatException();
            Network.sendControllerValue(AimAssist.ANGLE,angle.getValue());Network.sendControllerValue(AimAssist.RANGE,range.getValue());message="已发送保存；开关点击即生效";
        }catch(NumberFormatException e){message="请填：半角0.5～15度，距离4～256格";}
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public void tick(){angle.tick();range.tick();}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);int x=width/2-140,y=Math.max(30,(height-190)/2);
        g.drawCenteredString(font,title,width/2,y+5,0xFFFFD8E8);
        g.drawString(font,"准星范围半角（度）",x,y+60,0xFFE7D7EA);g.drawString(font,"自动索敌距离（格）",x,y+88,0xFFE7D7EA);
        g.drawCenteredString(font,message,width/2,y+138,0xFFAED7ED);
        g.drawCenteredString(font,"只改你自己；不穿墙、不转动镜头",width/2,y+188,0xFFB8A6BE);
        super.render(g,mx,my,partial);
    }
}
