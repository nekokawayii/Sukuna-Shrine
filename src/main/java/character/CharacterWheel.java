package character;

import cn.blockforge.generated.nnnnr1201forge2.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** 非宿傩角色的中央轮盘，直接读取角色模板；赤血保持19招双环和领域中央死胎星。 */
public final class CharacterWheel extends Screen {
    private final CharacterDefinition definition;
    private int current;
    private boolean confirmed,starHover;
    public CharacterWheel() { super(Component.literal("角色术式轮盘"));definition=CharacterClient.selected();current=definition!=null&&definition.skills().contains(Client.selected)?Client.selected:definition==null?0:definition.firstSkill(); }
    private int radius() { return Math.max(42,Math.min(156,Math.min(width/2-12,(height-92)/2))); }
    private boolean blood() { return definition!=null&&definition.family()==CharacterDefinition.Family.BLOOD; }
    private boolean star() { return blood()&&BloodClient.ownDomain(); }
    private int skillAt(double x,double y) {
        if(definition==null||definition.skills().isEmpty())return -1;
        if(blood()&&definition.skills().size()==19)return definition.skills().get(BloodRules.wheelSlot(x,y,radius()));
        int n=definition.skills().size();double angle=Math.atan2(y,x)+Math.PI/2+Math.PI/n;
        return definition.skills().get(Math.floorMod((int)Math.floor(angle/(Math.PI*2)*n),n));
    }
    private int inner() { return (int)(radius()*(blood()?.31:.46)); }
    private void hover(double mx,double my) {
        double x=mx-width/2.0,y=my-height/2.0,r=x*x+y*y;
        starHover=star()&&r<inner()*inner();
        if(r>=inner()*inner()&&r<=radius()*radius())current=skillAt(x,y);
    }
    private void confirm() {
        if(confirmed)return;confirmed=true;
        int skill=starHover?BloodRules.DEATH_STAR:current;
        if(Client.availableSkill(skill))Client.selected=skill;
        minecraft.setScreen(null);
    }
    @Override public void tick() {
        if(definition==null||minecraft.player==null||!minecraft.player.isAlive()||CharacterClient.selected()!=definition||!minecraft.isWindowActive()){confirmed=true;onClose();return;}
        if(!Client.wheelHeld())confirm();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void mouseMoved(double x,double y) { hover(x,y);super.mouseMoved(x,y); }
    @Override public boolean keyReleased(int key,int scan,int mods) { ClientInput.key(key,scan,GLFW.GLFW_RELEASE,mods);if(Client.WHEEL.matches(key,scan)){confirm();return true;}return super.keyReleased(key,scan,mods); }
    @Override public boolean mouseReleased(double x,double y,int button) { hover(x,y);if(Client.WHEEL.matchesMouse(button)){confirm();return true;}return super.mouseReleased(x,y,button); }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        if(definition==null)return;hover(mx,my);int cx=width/2,cy=height/2,r=radius(),inner=inner();
        var style=definition.config().wheel();
        g.fill(0,0,width,height,style.background());
        for(int y=-r;y<=r;y++){
            int start=0,last=-1;
            for(int x=-r;x<=r+1;x++){
                int s=x<=r&&x*x+y*y<=r*r&&x*x+y*y>=inner*inner?skillAt(x,y):-1;
                if(s!=last){if(last>=0)g.fill(cx+start,cy+y,cx+x,cy+y+1,!CharacterClient.allows(last)?style.locked():last==current?style.selected():style.idle());start=x;last=s;}
            }
        }
        int count=definition.skills().size();
        for(int i=0;i<count;i++){
            int n=blood()&&count==19?(i<8?8:11):count,index=blood()&&count==19?(i<8?i:i-8):i;
            double angle=-Math.PI/2+index*Math.PI*2/n,rr=r*(blood()&&count==19?(i<8?.48:.82):.74);
            int skill=definition.skills().get(i),x=cx+(int)(Math.cos(angle)*rr),y=cy+(int)(Math.sin(angle)*rr);
            String label=CharacterClient.label(skill);
            int textColor=CharacterClient.allows(skill)?CharacterClient.skillColor(skill):0xFF9299A5;
            float scale=Math.min(1f,r/135f);g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);
            var lines=font.split(Component.literal(label),42);int yy=lines.size()>1?-10:-4;
            for(int line=0;line<Math.min(2,lines.size());line++){var text=lines.get(line);g.drawString(font,text,-font.width(text)/2,yy+line*11,textColor);}
            g.pose().popPose();
        }
        g.drawCenteredString(font,star()?"极之番":style.title().isBlank()?definition.name():style.title(),cx,cy-14,definition.color());
        g.drawCenteredString(font,star()?"死胎星":style.subtitle().isBlank()?definition.technique():style.subtitle(),cx,cy+1,starHover?0xFFFFBAC9:style.text());
        if(star()&&DeathStarClient.anemic())g.drawCenteredString(font,"贫血禁用",cx,cy+14,0xFFFF7480);
        int skill=starHover?BloodRules.DEATH_STAR:current;
        g.drawCenteredString(font,CharacterClient.name(skill),cx,12,definition.color());
        g.drawCenteredString(font,!CharacterClient.allows(skill)?"完成第"+definition.config().unlockChapter(skill)+"章后解锁":skill==BloodRules.DEATH_STAR?"献祭90%当前生命与全部咒力":"消耗 "+CharacterClient.cost(skill)+" 咒力",cx,25,0xFFDEE2EE);
        int ly=height-43;for(var line:font.split(Component.literal(CharacterClient.description(skill)),Math.max(80,width-28))){if(ly>=height-15)break;g.drawString(font,line,14,ly,0xFFDADBE0);ly+=10;}
        g.drawCenteredString(font,"长按 "+Client.WHEEL.getTranslatedKeyMessage().getString()+" 移动选择，松手确认 · "+Client.CAST.getTranslatedKeyMessage().getString()+" 释放",cx,height-12,0xFFF1DADE);
        super.render(g,mx,my,partial);
    }
}
