package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** 按窗口高度分页，输入草稿保留到主动保存，网络回包不覆盖未提交内容。 */
public final class VoidControllerScreen extends Screen {
    private static final String[] ORIGINAL={"虚无相生命","虚无相出招频率","虚无相伤害倍率","虚空术式伤害倍率","领域持续秒数","裂隙共鸣倍率","最大裂隙数量","斩击长度","虎杖残影黑闪概率","红色剪切线","虎杖残影生命","虎杖残影频率","虎杖残影伤害","即身佛生命","即身佛频率","即身佛伤害"};
    private Network.ControllerState state;
    private final Map<Integer,String> drafts=new HashMap<>();
    private final List<EditBox> boxes=new ArrayList<>();
    private int page;
    public VoidControllerScreen(){super(Component.literal("咒术战斗控制器"));}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){Network.sendControllerRequest(-1,0);rebuild();}
    private int rows(){return Math.max(1,(height-132)/24);}
    private int total(){return ORIGINAL.length+GuardianSettings.KEYS.length+2;}
    private int pages(){return (total()+rows()-1)/rows();}
    private int left(){return (width-Math.min(620,width-20))/2;}
    private String label(int i){return i==total()-1?"全部技能破坏方块（默认关闭）":i==total()-2?"全部首领与所有生物互相敌对":i<16?ORIGINAL[i]:GuardianSettings.LABELS[i-16];}
    private int field(int i){return i<16?i:100+i-16;}
    private void rebuild(){clearWidgets();boxes.clear();int x=left(),w=width-2*x;page=Math.min(page,pages()-1);
        for(int i=page*rows();i<Math.min(total(),(page+1)*rows());i++){int y=58+(i-page*rows())*24,idx=i;
            if(i==total()-1){addRenderableWidget(Button.builder(Component.literal(state!=null&&state.guardian().getBoolean("Desblock")?"关闭":"开启"),b->Network.sendControllerRequest(201,1)).bounds(x+w-100,y,100,20).build());continue;}
            if(i==total()-2){addRenderableWidget(Button.builder(Component.literal(state!=null&&state.guardian().getBoolean("AllHostile")?"关闭":"开启"),b->Network.sendControllerRequest(200,1)).bounds(x+w-100,y,100,20).build());continue;}
            if(i==9){addRenderableWidget(Button.builder(Component.literal(state!=null&&state.redCut()?"关闭":"开启"),b->Network.sendControllerRequest(9,1)).bounds(x+w-100,y,100,20).build());continue;}
            EditBox box=new EditBox(font,x+w-150,y,150,20,Component.literal(label(i)));box.setMaxLength(64);box.setValue(drafts.getOrDefault(i,value(i)));box.setResponder(s->drafts.put(idx,s));addRenderableWidget(box);boxes.add(box);
        }
        addRenderableWidget(Button.builder(Component.literal("准星索敌设置"),b->minecraft.setScreen(new AimOptionsScreen(this))).bounds(x+w-150,height-54,150,20).build());
        addRenderableWidget(Button.builder(Component.literal("上一页"),b->{page=Math.max(0,page-1);rebuild();}).bounds(x,height-30,62,20).build());
        addRenderableWidget(Button.builder(Component.literal("下一页"),b->{page=Math.min(pages()-1,page+1);rebuild();}).bounds(x+66,height-30,62,20).build());
        addRenderableWidget(Button.builder(Component.literal("保存修改"),b->save()).bounds(x+w-150,height-30,78,20).build());
        addRenderableWidget(Button.builder(Component.literal("关闭"),b->onClose()).bounds(x+w-68,height-30,68,20).build());
    }
    private void save(){for(var e:Map.copyOf(drafts).entrySet())Network.sendControllerValue(field(e.getKey()),e.getValue());drafts.clear();}
    public void receive(Network.ControllerState s){state=s;rebuild();}
    @Override public void tick(){super.tick();for(EditBox box:boxes)box.tick();}
    @Override public boolean keyPressed(int key,int scan,int mods){if(key==257){save();return true;}return super.keyPressed(key,scan,mods);}
    private String value(int i){if(state==null)return "";double n;if(i>=16)n=state.guardian().getDouble(GuardianSettings.KEYS[i-16]);else n=switch(i){case 0->state.bossHealth();case 1->state.bossFrequency();case 2->state.bossDamage();case 3->state.voidDamage();case 4->state.domainSeconds();case 5->state.resonance();case 6->state.maxRifts();case 7->state.slashLength();case 8->state.blackFlashChance();case 10->state.yujiHealth();case 11->state.yujiFrequency();case 12->state.yujiDamage();case 13->state.buddhaHealth();case 14->state.buddhaFrequency();case 15->state.buddhaDamage();default->0;};return java.math.BigDecimal.valueOf(n).stripTrailingZeros().toPlainString();}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){g.fill(0,0,width,height,0xF0100B18);int x=left(),w=width-2*x;g.drawCenteredString(font,title,width/2,12,0xFFFFD8E8);g.drawCenteredString(font,"直接填写数值 · 第 "+(page+1)+" / "+pages()+" 页",width/2,28,0xFFB8A6BE);g.drawCenteredString(font,state!=null&&state.guardian().getBoolean("ModpackMode")?"整合包模式 · 填写原值，强度100%生效":"普通模式 · 填写原值，强度10%生效；时间、数量、范围不变",width/2,42,0xFFE7D7EA);for(int i=page*rows();i<Math.min(total(),(page+1)*rows());i++){int y=58+(i-page*rows())*24;g.fill(x,y-2,x+w,y+20,i%2==0?0xFF211625:0xFF1A1220);g.drawString(font,label(i),x+4,y+5,0xFFE7D7EA);}super.render(g,mx,my,partial);}
}
