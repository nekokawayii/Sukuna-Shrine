package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 伏魔御厨子（技能5）展开前的点数分配界面：结构照 {@link WallOptionsScreen}——± 按钮调数值、数值与剩余点数用
 *  {@link GuiGraphics} 画、「确定」立刻发 {@link Network.DomainOptions} 保存（没按确定就沿用上次保存的点数，
 *  第 {@link ShrineRules#DOMAIN_OPEN_TICKS} 刻真正展开时以那一刻的存档为准）。
 *
 *  <p>三项每点收益线性相同，所以"降低一项"退的点数就等于"最终收益的减少量"，界面上把这句话说清楚。
 *  另有「纵向伤敌」开关：关掉多 1 点可用（可用总点数 10→11），代价是领域打不到地下生物
 *  （判定在 {@link Skills} 的领域结算里，阈值 {@link DomainOptions#VERTICAL_MARGIN}）。</p> */
public final class DomainOptionsScreen extends Screen {
    private static final int ROW_H=24;
    /** 每行的三个条目：0=半径、1=伤害倍率、2=斩击速度。 */
    private int radiusPts=Client.progress.domainRadiusPts(),damagePts=Client.progress.domainDamagePts(),speedPts=Client.progress.domainSpeedPts();
    /** 「纵向伤敌」：per-player 设置，按「确定」时和三项点数一起发包（服务端存 NBT，展开时读那一刻的存档）。 */
    private boolean verticalKill=Client.progress.domainVerticalKill();
    private final Button[] minus=new Button[3],plus=new Button[3];
    private Button verticalButton;
    public DomainOptionsScreen(){super(Component.literal("伏魔御厨子 · 点数分配"));}
    @Override protected void init(){
        int left=width/2-110,right=left+194,top=height/2-46;
        for(int i=0;i<3;i++){
            int y=top+i*ROW_H,index=i;
            minus[i]=addRenderableWidget(Button.builder(Component.literal("-"),b->adjust(index,-1)).bounds(left,y,26,20).build());
            plus[i]=addRenderableWidget(Button.builder(Component.literal("+"),b->adjust(index,1)).bounds(right,y,26,20).build());
        }
        verticalButton=addRenderableWidget(Button.builder(Component.literal(""),b->{verticalKill=!verticalKill;trimToTotal();b.setMessage(Component.literal(verticalText()));refresh();}).bounds(left,height/2+26,220,20).build());
        addRenderableWidget(Button.builder(Component.literal("确定并选中"),b->{Network.CHANNEL.sendToServer(new Network.DomainOptions(radiusPts,damagePts,speedPts,verticalKill));minecraft.setScreen(null);}).bounds(left,height/2+62,220,20).build());
        refresh();
    }
    private int[] points(){return new int[]{radiusPts,damagePts,speedPts};}
    private String verticalText(){return "纵向伤敌："+(verticalKill?"开":"关");}
    /** 只有"关掉纵向伤敌多分出来的那 1 点"会超支：从关切回开时可用总点数少 1，就从**当前分得最多的一项**里收回那 1 点，
     *  保证发出去的分配永远合法（服务端按可用总点数整包校验，不合法的包会被直接拒收）。 */
    private void trimToTotal(){
        while(!DomainOptions.valid(radiusPts,damagePts,speedPts,verticalKill)){
            int[] pts=points();int most=0;for(int i=1;i<3;i++)if(pts[i]>pts[most])most=i;
            if(pts[most]<=0)break;
            pts[most]--;radiusPts=pts[0];damagePts=pts[1];speedPts=pts[2];
        }
    }
    private void adjust(int index,int delta){
        int[] pts=points();int value=pts[index]+delta;
        if(value<0||value>DomainOptions.total(verticalKill))return;
        // 加点必须有未分配的点数；减点直接退（每点收益线性相同 → 退掉的点数就是最终收益的减少量）。
        if(delta>0&&DomainOptions.remaining(radiusPts,damagePts,speedPts,verticalKill)<=0)return;
        pts[index]=value;radiusPts=pts[0];damagePts=pts[1];speedPts=pts[2];
        refresh();
    }
    /** 点数从 0 或已分配满时，对应的按钮直接置灰（不用玩家自己去猜为什么没反应）；开关按钮的文案也在这里刷。 */
    private void refresh(){
        int[] pts=points();int total=DomainOptions.total(verticalKill),remaining=DomainOptions.remaining(radiusPts,damagePts,speedPts,verticalKill);
        for(int i=0;i<3;i++){minus[i].active=pts[i]>0;plus[i].active=remaining>0&&pts[i]<total;}
        if(verticalButton!=null)verticalButton.setMessage(Component.literal(verticalText()));
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        g.fill(0,0,width,height,0xE8100D18);WheelBackdrop.draw(g,width,height,minecraft.level==null?0:minecraft.level.getGameTime()+partial);
        int top=height/2-46,total=DomainOptions.total(verticalKill);
        g.drawCenteredString(font,"伏魔御厨子 · 点数分配",width/2,height/2-92,0xFFFFD5E4);
        g.drawCenteredString(font,"共 "+total+" 点 · 剩余 "+DomainOptions.remaining(radiusPts,damagePts,speedPts,verticalKill)+" 点 · 每项 0~"+total+" 点",width/2,height/2-76,0xFFD6BFDA);
        g.drawCenteredString(font,"每点收益线性相同：降低一项即按其最终收益的减少退还点数",width/2,height/2-62,0xFFCFC5D0);
        g.drawCenteredString(font,"领域半径  "+DomainOptions.radius(radiusPts)+" 格（"+radiusPts+" 点）",width/2,top+6,0xFFE7D9F2);
        g.drawCenteredString(font,"伤害倍率  ×"+number(DomainOptions.damage(damagePts))+"（"+damagePts+" 点）",width/2,top+ROW_H+6,0xFFE7D9F2);
        g.drawCenteredString(font,"斩击速度  ×"+number(DomainOptions.speed(speedPts))+"（"+speedPts+" 点）",width/2,top+ROW_H*2+6,0xFFE7D9F2);
        g.drawCenteredString(font,"关＝+1点，但打不到地下生物",width/2,height/2+50,0xFFCFC5D0);
        g.drawCenteredString(font,"默认 "+DomainOptions.DEFAULT_RADIUS+"/"+DomainOptions.DEFAULT_DAMAGE+"/"+DomainOptions.DEFAULT_SPEED+" ＝ "+DomainOptions.radius(DomainOptions.DEFAULT_RADIUS)+" 格 · ×"+number(DomainOptions.damage(DomainOptions.DEFAULT_DAMAGE))+" · ×"+number(DomainOptions.speed(DomainOptions.DEFAULT_SPEED))+"（现状）",width/2,height/2+88,0xFFD6BFDA);
        g.drawCenteredString(font,"确定后立即保存 · ESC 沿用上次 · 第 "+ShrineRules.DOMAIN_OPEN_TICKS+" 刻展开时取那一刻已保存的点数",width/2,height/2+102,0xFFBFB2C6);
        super.render(g,x,y,partial);
    }
    private static String number(double value){return String.format(java.util.Locale.ROOT,"%.2f",value);}
}
