package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 咒术选择书界面：六行，点哪行选哪个流派。 */
public final class TechniqueScreen extends Screen {
    private final Button[] buttons=new Button[TechniqueRules.COUNT];
    private int hover=-1;

    public TechniqueScreen(){super(Component.literal("咒术选择书"));}

    @Override protected void init(){
        int top=height/2-64,step=24;
        for(int i=0;i<TechniqueRules.COUNT;i++){
            final int technique=i;
            buttons[i]=addRenderableWidget(Button.builder(Component.literal(TechniqueRules.NAMES[i]+" · "+TechniqueRules.OWNERS[i]),b->choose(technique))
                .bounds(width/2-150,top+i*step,300,20).build());
        }
    }

    private void choose(int technique){
        Network.CHANNEL.sendToServer(new Network.ChooseTechnique(technique));
        minecraft.setScreen(null);
    }

    @Override public boolean isPauseScreen(){return false;}

    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        renderBackground(g);
        g.drawCenteredString(font,"咒术选择书",width/2,height/2-88,0xFFEED9FF);
        int current=Client.progress.technique();
        String now=TechniqueRules.valid(current)?"当前："+TechniqueRules.NAMES[current]:"尚未选择术式";
        g.drawCenteredString(font,now,width/2,height/2-76,TechniqueRules.valid(current)?0xFFB9F5C0:0xFFB8A4AE);
        super.render(g,mouseX,mouseY,partial);
        for(int i=0;i<TechniqueRules.COUNT;i++){
            if(!buttons[i].isHovered())continue;
            g.drawCenteredString(font,TechniqueRules.DESC[i],width/2,height/2+84,0xFFFFD9A0);
        }
        g.drawCenteredString(font,"再吃一本《咒术选择书》即可随时重选 · Esc 关闭",width/2,height/2+96,0xFF8C7A90);
    }
}
