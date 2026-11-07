package character;

import cn.blockforge.generated.nnnnr1201forge2.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** 非宿傩角色的中央轮盘，直接读取角色模板；赤血保持19招双环和领域中央死胎星。 */
public final class CharacterWheel extends Screen {
    private final CharacterDefinition definition;
    private int current,mainlineHover=-1,branchHover=-1;
    private boolean confirmed,starHover,ultHover;
    public CharacterWheel() { super(Component.literal("角色术式轮盘"));definition=CharacterClient.selected();current=definition!=null&&CharacterClient.skills().contains(Client.selected)?Client.selected:definition==null?0:definition.firstSkill(); }
    private int radius() { return Math.max(42,Math.min(156,Math.min(width/2-100,(height-92)/2))); }
    private boolean blood() { return definition!=null&&definition.family()==CharacterDefinition.Family.BLOOD; }
    private boolean star() { return blood()&&BloodClient.ownDomain(); }
    /** 鹿紫云一：轮盘中心是终奥「幻兽·琥珀」。 */
    private boolean kashimo() { return definition!=null&&"kashimo".equals(definition.id()); }
    private int skillAt(double x,double y) {
        if(definition==null||CharacterClient.skills().isEmpty())return -1;
        if(blood()&&CharacterClient.skills().size()==19)return CharacterClient.skills().get(BloodRules.wheelSlot(x,y,radius()));
        int n=CharacterClient.skills().size();double angle=Math.atan2(y,x)+Math.PI/2+Math.PI/n;
        return CharacterClient.skills().get(Math.floorMod((int)Math.floor(angle/(Math.PI*2)*n),n));
    }
    private int inner() { return (int)(radius()*(blood()?.31:.46)); }
    /** 五条悟的轮盘分支：选中「苍/赫/无量」时在屏幕右侧列出一列子技能。 */
    private boolean gojo(){return definition!=null&&"gojo".equals(definition.id());}
    private int[] branches(){
        if(!gojo())return new int[0];
        if(current==34)return new int[]{GojoBranches.SINGULARITY,GojoBranches.TEAR,GojoBranches.METEOR,GojoBranches.SURROUND};
        if(current==35)return new int[]{GojoBranches.HEAVY,GojoBranches.TURBULENCE};
        if(current==40)return new int[]{GojoBranches.BRAINDEAD};
        return new int[0];
    }
    private int[] branchBox(int i){int n=branches().length;int h=28,gap=4;int y0=height/2-(n*(h+gap)-gap)/2;return new int[]{width-96,y0+i*(h+gap),88,h};}
    private void hover(double mx,double my) {
        mainlineHover=character.mainline.MainlineWheel.hover(width,height,mx,my);
        branchHover=-1;
        int[] bs=branches();
        for(int i=0;i<bs.length;i++){int[] b=branchBox(i);
            if(mx>=b[0]&&mx<=b[0]+b[2]&&my>=b[1]&&my<=b[1]+b[3])branchHover=bs[i];}
        if(mainlineHover>=0){starHover=false;ultHover=false;return;}
        double x=mx-width/2.0,y=my-height/2.0,r=x*x+y*y;
        starHover=star()&&r<inner()*inner();
        ultHover=kashimo()&&r<inner()*inner();
        if(r>=inner()*inner()&&r<=radius()*radius())current=skillAt(x,y);
    }
    private void confirm() {
        if(confirmed)return;confirmed=true;
        if(character.mainline.MainlineWheel.cast(mainlineHover)){minecraft.setScreen(null);return;}
        // 鹿紫云一：轮盘中心 = 终奥「幻兽·琥珀」（phase=7 直接请求，不走技能表）
        if(kashimo()&&ultHover){
            Network.CHANNEL.sendToServer(new TechniqueInput.Input(character.kashimo.KashimoCombat.ULT,7,false,false,false,false,0,0));
            minecraft.setScreen(null);return;
        }
        if(branchHover>=0){Client.selected=branchHover;minecraft.setScreen(null);return;}
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
        int count=CharacterClient.skills().size();
        for(int i=0;i<count;i++){
            int n=blood()&&count==19?(i<8?8:11):count,index=blood()&&count==19?(i<8?i:i-8):i;
            // 技能少时把文字摆位往内收：5 个技能用 .74 会把左右两侧的文字挤出圆环外，
            // 看起来像“没在轮盘里”。.60 能保证文字中心落在环带内。
            double rr=r*(blood()&&count==19?(i<8?.48:.82):count<=6?.60:.74);
            double angle=-Math.PI/2+index*Math.PI*2/n;
            int skill=CharacterClient.skills().get(i),x=cx+(int)(Math.cos(angle)*rr),y=cy+(int)(Math.sin(angle)*rr);
            String label=CharacterClient.label(skill);
            int textColor=CharacterClient.allows(skill)?CharacterClient.skillColor(skill):0xFF9299A5;
            float scale=Math.min(1f,r/135f);g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);
            var lines=font.split(Component.literal(label),42);int yy=lines.size()>1?-10:-4;
            for(int line=0;line<Math.min(2,lines.size());line++){var text=lines.get(line);g.drawString(font,text,-font.width(text)/2,yy+line*11,textColor);}
            g.pose().popPose();
        }
        g.drawCenteredString(font,kashimo()?"极之番":star()?"极之番":style.title().isBlank()?definition.name():style.title(),cx,cy-14,definition.color());
        g.drawCenteredString(font,kashimo()?"幻兽·琥珀":star()?"死胎星":style.subtitle().isBlank()?definition.technique():style.subtitle(),cx,cy+1,kashimo()?(ultHover?0xFFFFFFFF:style.text()):starHover?0xFFFFBAC9:style.text());
        if(kashimo())g.drawCenteredString(font,ultHover?"满咒力释放终奥 · 生命低于30%自动触发":"轮盘中心：幻兽·琥珀",cx,cy+14,ultHover?0xFFBFF4FF:0xFF7FA8BC);
        if(star()&&DeathStarClient.anemic())g.drawCenteredString(font,"贫血禁用",cx,cy+14,0xFFFF7480);
        int skill=starHover?BloodRules.DEATH_STAR:current;
        g.drawCenteredString(font,CharacterClient.name(skill),cx,12,definition.color());
        g.drawCenteredString(font,!CharacterClient.allows(skill)?character.mainline.Mainline.domainSkill(definition.id(),skill)?"完成共同主线的领域展开成就后解锁":"完成对应个人线后解锁":skill==BloodRules.DEATH_STAR?"献祭90%当前生命与全部咒力":"消耗 "+CharacterClient.cost(skill)+" 咒力",cx,25,0xFFDEE2EE);
        int ly=height-43;for(var line:font.split(Component.literal(CharacterClient.description(skill)),Math.max(80,width-28))){if(ly>=height-15)break;g.drawString(font,line,14,ly,0xFFDADBE0);ly+=10;}
        g.drawCenteredString(font,"长按 "+Client.WHEEL.getTranslatedKeyMessage().getString()+" 移动选择，松手确认 · "+Client.CAST.getTranslatedKeyMessage().getString()+" 释放",cx,height-12,0xFFF1DADE);
        int[] bs=branches();
        for(int i=0;i<bs.length;i++){int[] b=branchBox(i);int id=bs[i];
            boolean on=branchHover==id;
            g.fill(b[0],b[1],b[0]+b[2],b[1]+b[3],on?0xEE9D263B:0xDD321722);
            g.drawCenteredString(font,GojoBranches.label(id),b[0]+b[2]/2,b[1]+5,on?0xFFFFFFFF:0xFFFFE6DE);
            g.drawCenteredString(font,Client.energyText(GojoBranches.cost(id))+"咒力",b[0]+b[2]/2,b[1]+16,0xFFDCC1F4);}
        character.mainline.MainlineWheel.render(g,font,width,height,mx,my);
        super.render(g,mx,my,partial);
    }
}
