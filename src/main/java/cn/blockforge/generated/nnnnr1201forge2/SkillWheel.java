package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** 里梅独立固定在屏幕最左侧；宿傩使用完整圆环并独立翻页。 */
public final class SkillWheel extends Screen {
    private int page,current,branchHover=-1;
    private boolean repairHover,outputHover,reverseHover,confirmed,finaleHover,fistHover;
    private double mouseX,mouseY;
    private static final int[] GUARDIAN_WHEEL={0,1,2,3,4,5,6,8,9};
    private static final String[] SHORT={"解","网","捌","开","灶","领域","剥离"};
    public SkillWheel(){
        super(Component.literal("咒术 · 技能轮盘"));page=Client.selected>=7&&Client.selected<17&&Client.progress.pureLandVictory()?1:0;Client.wheelPage=page;
        current=Client.selected;current=switch(current){case 17,21->1;case 18,24,26,27,28,29->0;case 19,20,25,32->2;case 30->3;case FurnaceMax.SKILL->3;case 31->5;case 33->0;default->current;};
        if(!Client.availableSkill(current))current=Client.progress.sukuna()?0:BranchRules.FROST;
    }
    private boolean ice(){return Client.progress.uraume();}
    private boolean gojo(){return Client.progress.gojo();}
    private boolean pages(){return Client.progress.sukuna()&&Client.progress.pureLandVictory();}
    private int centerX(){return WheelSideSlots.centerX(width,height,ice(),gojo());}
    private int count(){return page==1?GUARDIAN_WHEEL.length:Client.baseWheelCount();}
    private int global(int index){return page==1?GUARDIAN_WHEEL[index]+7:index;}
    private String shortName(int skill){if(skill==Skills.FIST_SKILL)return "咒力附拳";if(skill==FurnaceMax.SKILL)return FurnaceMax.SHORT;if(skill==SimpleDomain.SKILL)return "简易领域 · 捌";if(skill>=34)return GojoPlayer.shortName(skill-34);if(CombatExpansionRules.branch(skill))return CombatExpansionRules.name(skill).replace("解 · ","").replace("捌 · ","");if(skill>=24)return SlashFormRules.name(skill).replace("解 · ","").replace("捌 · ","");return skill==22?"霜凪":skill==23?"直瀑":skill>=7&&skill<17?GuardianPlayerSkills.SHORT[skill-7]:skill>=17?BranchRules.name(skill):SHORT[skill];}
    private Button modeButton;
    @Override protected void init(){
        if(Client.progress.sukuna())modeButton=addRenderableWidget(Button.builder(Component.literal(modeText()),b->Network.CHANNEL.sendToServer(new Network.Cast(SkillAction.TOGGLE_DISMANTLE))).bounds(centerX()-76,3,152,20).build());
        if(pages()){
            addRenderableWidget(Button.builder(Component.literal("上一页"),b->changePage()).bounds(centerX()-143,3,62,20).build());
            addRenderableWidget(Button.builder(Component.literal("下一页"),b->changePage()).bounds(centerX()+81,3,62,20).build());
        }
    }
    private String modeText(){return "解："+(Client.progress.dismantleQi()?"剑气":"斩击")+"模式 · 点击切换";}
    private void changePage(){if(!pages())return;page=1-page;Client.wheelPage=page;if(current<22)current=global(0);}
    @Override public boolean mouseScrolled(double x,double y,double delta){if(delta!=0&&pages()){changePage();return true;}return super.mouseScrolled(x,y,delta);}
    @Override public boolean keyPressed(int key,int scan,int mods){if((key==GLFW.GLFW_KEY_LEFT||key==GLFW.GLFW_KEY_RIGHT||key==GLFW.GLFW_KEY_PAGE_UP||key==GLFW.GLFW_KEY_PAGE_DOWN)&&pages()){changePage();return true;}return super.keyPressed(key,scan,mods);}
    @Override public boolean isPauseScreen(){return false;}
    private int outerRadius(){return WheelSideSlots.radius(width,height,ice(),gojo());}
    private int[] branches(){if(page!=0||!Client.progress.sukuna())return new int[0];
        // 「咒力附拳」不占分支：它是轮盘右上角的固定格（见 {@link WheelSideSlots#fist}），始终显示。
        if(current==0)return Client.progress.awakened()?new int[]{18,24,26,27,28,29}:new int[]{24,26,28,29};if(current==1&&Client.progress.awakened())return new int[]{17,21};if(current==2&&Client.progress.fingers()>=5)return new int[]{19,20,25,32,SimpleDomain.SKILL};if(current==3&&Client.voidStage())return new int[]{30,FurnaceMax.SKILL};if(current==5&&Client.progress.awakened())return new int[]{31};return new int[0];}
    private WheelSideSlots.Box branchBox(int i){int n=branches().length,step=Math.min(34,Math.max(18,(height-94)/Math.max(1,n)));return new WheelSideSlots.Box(centerX()+outerRadius()+4,height/2-n*step/2+i*step,74,step-3);}
    private int skillAt(double x,double y){return BranchRules.wheelSkill(true,false,page,count(),x,y);}
    private void updateHover(double mx,double my){
        finaleHover=page==0&&FinaleClient.ready()&&Math.pow(mx-centerX(),2)+Math.pow(my-height/2.0,2)<Math.pow(outerRadius()/2.0-3,2);
        mouseX=mx;mouseY=my;reverseHover=Client.progress.sukuna()&&WheelSideSlots.reverse(width,height,ice()).contains(mx,my);
        // 右上角固定格：咒力附拳（5 根手指起显示/可用）
        fistHover=Client.progress.sukuna()&&Client.progress.fingers()>=5&&WheelSideSlots.fist(width,height,ice()).contains(mx,my);
        repairHover=Client.progress.sukuna()&&Client.progress.fingers()>=15&&Client.progress.burnout()&&Client.progress.clashPenaltyTicks()==0&&WheelSideSlots.repair(width,height,ice()).contains(mx,my);
        outputHover=Client.progress.sukuna()&&page==1&&mx>=centerX()+outerRadius()+4&&mx<centerX()+outerRadius()+78&&my>=height/2-24&&my<height/2+24;
        branchHover=-1;int[] branches=branches();for(int i=0;i<branches.length;i++)if(branchBox(i).contains(mx,my))branchHover=branches[i];
    }
    private void confirm(){
        if(confirmed||minecraft==null||minecraft.screen!=this)return;confirmed=true;updateHover(mouseX,mouseY);
        if(minecraft.player!=null&&minecraft.player.isAlive()&&!minecraft.player.isSpectator()&&Client.progress.unlocked()){
            if(finaleHover)Client.selected=FinaleRules.SKILL;
            else if(fistHover)Network.CHANNEL.sendToServer(new Network.Cast(SkillAction.TOGGLE_FIST));   // 右上角固定格：松手即开关附拳
            else if(reverseHover){if(Client.progress.fingers()>=15)Network.CHANNEL.sendToServer(new Network.Cast(SkillAction.REVERSE));}
            else if(repairHover)Network.CHANNEL.sendToServer(new Network.Cast(SkillAction.REPAIR_BURNOUT));
            else if(outputHover)Network.CHANNEL.sendToServer(new Network.Cast(SkillAction.GUARDIAN_REVERSE));
            else {Client.selected=branchHover>=0?branchHover:current;if(Client.selected==BranchRules.WALL){minecraft.setScreen(new WallOptionsScreen());return;}}
        }
        minecraft.setScreen(null);
    }
    @Override public void tick(){if(minecraft.player==null||!minecraft.player.isAlive()||!Client.progress.unlocked()||!minecraft.isWindowActive()){confirmed=true;minecraft.setScreen(null);return;}if(!Client.wheelHeld())confirm();}
    @Override public boolean keyReleased(int key,int scan,int mods){ClientInput.key(key,scan,GLFW.GLFW_RELEASE,mods);if(Client.WHEEL.matches(key,scan)){confirm();return true;}return super.keyReleased(key,scan,mods);}
    @Override public boolean mouseReleased(double x,double y,int button){updateHover(x,y);if(Client.WHEEL.matchesMouse(button)){confirm();return true;}return super.mouseReleased(x,y,button);}
    private boolean branchCorridor(double mx,double my){
        int[] choices=branches();if(choices.length==0)return false;
        double a=-Math.PI/2+current*Math.PI*2/count();
        double radius=outerRadius()*0.72,startX=centerX()+Math.cos(a)*radius,startY=height/2.0+Math.sin(a)*radius;
        var first=branchBox(0);var last=branchBox(choices.length-1);
        return BranchRules.corridor(mx,my,startX,startY,first.x(),first.y(),last.y()+last.height());
    }
    @Override public void mouseMoved(double mx,double my){
        if(Client.wheelHeld()){
            if(ice()&&WheelSideSlots.iceContains(width,height,mx,my))current=my<height/2.0?BranchRules.FROST:BranchRules.ICEFALL;
            else if(gojo()&&WheelSideSlots.gojoContains(width,height,mx,my))current=34+WheelSideSlots.gojoSector(width,height,mx,my);
            else if(Client.progress.sukuna()&&!branchCorridor(mx,my)){
                double x=mx-centerX(),y=my-height/2.0,r=x*x+y*y;int outer=outerRadius();
                if(r>=outer*outer/4.0&&r<=outer*outer)current=skillAt(x,y);
            }
        }
        updateHover(mx,my);super.mouseMoved(mx,my);
    }
    private int cost(int skill){if(skill==Skills.FIST_SKILL)return 0;if(skill==FurnaceMax.SKILL)return FurnaceMax.COST;if(skill==SimpleDomain.SKILL)return 150;if(skill>=34)return GojoPlayer.costOf(skill-34);if(skill==FinaleRules.SKILL)return (int)Client.progress.energy();if(skill==29||skill==32)return Curse.COST[0];if(skill==30)return Curse.COST[3];if(skill==31)return 2000;if(skill==28)return Curse.COST[0];if(skill>=24)return skill==24?120:skill==25?180:skill==26?0:2000;if(skill==22||skill==23)return Uraume.cost(skill);if(skill==19)return 180;if(skill==20)return 120;if(skill==21)return BranchRules.wallCost(Client.progress.wallSpacing());if(NetBranchRules.branch(skill))return NetBranchRules.cost(skill);return skill<7?Curse.COST[skill]:GuardianPlayerSkills.COST[skill-7];}
    private static final String[] GOJO_DESC={"引力拉扯：把敌人吸向蓝色光球并施加缓慢","红色斥力球：高伤害与超级击退","蓄力3秒后打出圆柱激光，沿直线贯穿并击飞","双手合十5秒，苍与赫对撞成紫球后打出","念完四句咒词后原地引爆，范围48格 · 反噬自己40%生命","向准星方向瞬移最多8格","近距离三连拳，命中减速","领域展开：范围内敌人无法行动并持续受伤","长按引导：光球每秒5格跟随准星，松手后停留5秒"};
    private String desc(int skill){return switch(skill){
        case 33->"蓄力8秒 → 五阶段终结 · 耗尽咒力 · 停恢复60秒 · 熔断180秒";
        case 24->"随机形状 · 每道间隔0.1秒 · 跟随顶部剑气/斩击模式";
        case 25->"身后1～20道十字 · 先慢后快 · 发出后不追踪";
        case 26->"按住画、松手暂停 · 1.5秒内续画 · 每像素1咒力 · 上限2000";
        case 27->"公开咏唱：龙鳞·反发·成双之流星 · 冷却120秒";
        case 29,32->"短按普通解 · 长按随准星2格/秒 · 松手停15秒 · 碰敌10斩";
        case 30->"虚空阶段保留灵魂炎弓效果 · 紫色灶火与爆炸";
        case FurnaceMax.SKILL->"长按蓄力20秒 · 蓄力中无法移动、前10秒每秒上升1格、周围15格燃烧 · 松手或受伤中断（冷却30秒）· 半径100格火焰爆炸 + 火柱40秒熄灭（柱内150/s）";
        case 31->"半秒内逐按六笔成星，最后长按咏唱 · 龙与120颗小行星 · 熔断75秒";
        case 28->"原地旋转 · 向四周释放水平斩击圈 · 动作后恢复视角";
        case 17->"上下八角向中心 · 每道原版40%解 · 冷却50秒";
        case 18->"3倍解伤害 · 穿墙贯穿 · 冷却5秒";
        case 19->"贴合地形的斩击蜘蛛网 · 1秒后引爆 · 半径10～100格";
        case 20->"强化下一次普攻：追加5～40道解，随阶段提升";
        case 21->"墙 "+Client.wallSize()+"×"+Client.wallSize()+" 格 · 松手设置间隔和角度";
        case 22->"冰汽持续5秒 · 基础10×3×5格 · 命中强控10秒";
        case 23->"身后50～60根冰柱 · 准星半径5格散射 · 冰鱼强化";
        case 0->"无冷却长按连发 · 蹲下横斩 · 疾跑滑铲/跳拳 · 下落护身与落地追击";
        case 1->"网的分支：十字斩 / 墙 · 觉醒后向右选择";
        case 2->"满咒力150%捌伤害 · 分支：蜘蛛网 / 多重斩 / 十字飞行";
        case 3->"凝火成弓，向准星方向释放";
        case 4->"粉尘焚爆，领域或觉醒后可用";
        case 5->"开放领域 · 半径200格 · 密集60～120格斩击";
        case 6->"灵魂剥离 · 单体虚弱5秒 · 冷却90秒";
        default->skill>=34?GojoPlayer.desc(skill-34):skill>=7&&skill<17?GuardianPlayerSkills.DESC[skill-7]:"";
    };}
    private void gojoRing(GuiGraphics g,int cx,int cy,int outer){
        int inner=outer/2;
        for(int y=-outer;y<=outer;y++){int start=-1,last=-1;for(int x=-outer;x<=outer+1;x++){
            int sector=-1;double r=x*x+y*y;
            if(x<=outer&&r<=outer*outer&&r>=inner*inner)sector=WheelSideSlots.gojoSector(width,height,cx+x,cy+y);
            if(sector!=last){if(last>=0)g.fill(cx+start,cy+y,cx+x,cy+y+1,34+last==current?0xE9C98A3A:0xD22A2136);start=x;last=sector;}
        }}
    }
    private void ring(GuiGraphics g,int cx,int cy,int outer,boolean iceRing){
        int inner=outer/2;
        for(int y=-outer;y<=outer;y++){int start=-1,last=-1;for(int x=-outer;x<=outer+1;x++){
            int sector=-1;double r=x*x+y*y;
            if(x<=outer&&r<=outer*outer&&r>=inner*inner)sector=iceRing?(y<0?BranchRules.FROST:BranchRules.ICEFALL):skillAt(x,y);
            if(sector!=last){if(last>=0)g.fill(cx+start,cy+y,cx+x,cy+y+1,iceRing?(last==current?0xE94392AC:0xD2153448):(last==current?0xE69D263B:0xD22C1928));start=x;last=sector;}
        }}
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        if(modeButton!=null)modeButton.setMessage(Component.literal(modeText()));
        updateHover(mx,my);int cx=centerX(),cy=height/2,outer=outerRadius(),inner=outer/2,skill=finaleHover?FinaleRules.SKILL:branchHover>=0?branchHover:current;
        g.fill(0,0,width,height,0xBA100D18);WheelBackdrop.draw(g,width,height,minecraft.level==null?0:minecraft.level.getGameTime()+partial);
        if(Client.progress.sukuna()){
            ring(g,cx,cy,outer,false);
            for(int i=0;i<count();i++)label(g,global(i),cx,cy,outer,inner,-Math.PI/2+i*Math.PI*2/count());
            if(page==0&&FinaleClient.ready()){
                for(int y=-inner+3;y<=inner-3;y++){int x=(int)Math.sqrt(Math.max(0,(inner-3)*(inner-3)-y*y));g.fill(cx-x,cy+y,cx+x,cy+y+1,finaleHover?0xEEA88736:0xDC45331C);}
                g.drawCenteredString(font,"終式",cx,cy-16,0xFFFFE49A);g.drawCenteredString(font,"六道連斬",cx,cy-2,0xFFFFFFFF);g.drawCenteredString(font,"长按8秒",cx,cy+13,0xFFFFE1B8);
            }else{g.drawCenteredString(font,page==1?"守护者":"宿 傩",cx,cy-14,0xFFDFBACF);g.drawCenteredString(font,current<22?shortName(current):"",cx,cy+4,0xFFFFFFFF);}
            reverse(g);fist(g);
        }
        if(ice()){
            int ix=WheelSideSlots.iceX(width,height),ir=WheelSideSlots.iceRadius(width,height);
            ring(g,ix,cy,ir,true);label(g,22,ix,cy,ir,ir/2,-Math.PI/2);label(g,23,ix,cy,ir,ir/2,Math.PI/2);
            g.drawCenteredString(font,"里梅",ix,cy-4,0xFFBEEFFF);
            g.drawCenteredString(font,"冰凝咒法",ix,cy-ir-14,0xFFBEEFFF);
            g.drawCenteredString(font,"冰鱼 "+Client.progress.iceFish(),ix,cy+ir+7,0xFFAEE3F1);
        }
        if(gojo()){
            int gx=WheelSideSlots.gojoX(width,height),gr=WheelSideSlots.gojoRadius(width,height);
            gojoRing(g,gx,cy,gr);
            int sel=current-34;
            // 扇区 i 覆盖 [-π/2+i*2π/N, +2π/N)，标签必须放在扇区**中心**（多加半格），
            // 否则每个字都会压在两条扇区的分界线上，看起来就是"轮盘和字错位"。
            for(int i=0;i<GojoPlayer.SLOTS;i++){
                double a=-Math.PI/2+(i+0.5)*Math.PI*2/GojoPlayer.SLOTS;
                int lx=gx+(int)Math.round(Math.cos(a)*gr*0.75),ly=cy+(int)Math.round(Math.sin(a)*gr*0.75);
                String name=GojoPlayer.shortName(GojoPlayer.WHEEL[i]);
                boolean on=34+i==current;
                if(on){int w=font.width(name);g.fill(lx-w/2-3,ly-6,lx+w/2+3,ly+6,0x99000000);} // 选中格加个底片，明确"这个字属于这一格"
                g.drawCenteredString(font,name,lx,ly-4,on?0xFFFFFFFF:0xFFE6DCE6);
            }
            g.drawCenteredString(font,sel>=0&&sel<GojoPlayer.SLOTS?GojoPlayer.shortName(GojoPlayer.WHEEL[sel]):"五条悟",gx,cy-4,0xFFFFFFFF);
            g.drawCenteredString(font,"六眼",gx,cy+8,0xFF9FC7F0);
            g.drawCenteredString(font,"五条悟技能",gx,cy-gr-14,0xFFBEE0FF);
            g.drawCenteredString(font,"碎片已食用 · 7 式（苍/赫长按为最大功率）",gx,cy+gr+7,0xFFA9C8F1);
        }
        if(Client.progress.sukuna()&&page==1){int x=cx+outer+4,y=cy-24;g.fill(x,y,x+74,y+48,outputHover?0xD6348A74:0xCC17322B);g.drawCenteredString(font,"术式反转",x+37,y+7,0xFFE2FFF2);g.drawCenteredString(font,"外放 · "+Client.energyText(100),x+37,y+25,0xFFA9DBC8);}
        int[] branches=branches();for(int i=0;i<branches.length;i++){var box=branchBox(i);int id=branches[i];g.fill(box.x(),box.y(),box.x()+box.width(),box.y()+box.height(),branchHover==id?0xEE9D263B:0xDD321722);g.drawCenteredString(font,shortName(id),box.x()+37,box.y()+(box.height()<27?3:4),0xFFFFE6DE);if(box.height()>=27)g.drawCenteredString(font,id==21?Client.wallSize()+"×"+Client.wallSize()+"格":id==26?"1咒力/像素":id==SimpleDomain.SKILL?Client.energyText(cost(id))+"咒力/秒":Client.energyText(cost(id))+"咒力",box.x()+37,box.y()+17,0xFFDCC1F4);}
        int detailX=Client.progress.sukuna()?cx:width/2;
        g.drawCenteredString(font,Client.skillName(skill),detailX,Math.max(28,cy-outer-23),0xFFFFDDD1);
        g.drawCenteredString(font,skill==26?"每像素消耗1咒力 · 上限2000像素":"消耗 "+Client.energyText(cost(skill))+((skill==2&&!Client.voidStage()||skill==5)?" 咒力/秒":" 咒力/次"),detailX,Math.max(39,cy-outer-11),0xFFCB9BF0);
        g.drawCenteredString(font,desc(skill),width/2,height-40,0xFFE8D8DA);
        String pageText=pages()?"宿傩第 "+(page+1)+"/2 页 · 滚轮/方向键翻页":Client.progress.sukuna()?"宿傩第1页 · 净土通关后可翻页":gojo()?"五条悟技能轮盘已解锁":"冰伤 +"+Client.progress.iceFish()+"%";
        g.drawCenteredString(font,pageText,width/2,height-26,0xFFAEE3F1);
        g.drawCenteredString(font,"长按 "+Client.WHEEL.getTranslatedKeyMessage().getString()+" 移动选择，松手确定 · "+Client.CAST.getTranslatedKeyMessage().getString()+" 释放",width/2,height-14,0xFFE2D7DC);super.render(g,mx,my,partial);
    }
    private void label(GuiGraphics g,int id,int cx,int cy,int outer,int inner,double a){int x=cx+(int)(Math.cos(a)*(outer+inner)/2),y=cy+(int)(Math.sin(a)*(outer+inner)/2);g.drawCenteredString(font,shortName(id),x,y-4,id==current?0xFFFFFF:0xD5C9D3);}
    private void reverse(GuiGraphics g){        var box=WheelSideSlots.reverse(width,height,ice());boolean unlocked=Client.progress.fingers()>=15;int x=box.x(),y=box.y();g.fill(x,y,x+64,y+64,reverseHover?0xE6378B77:0xCC173A32);g.drawCenteredString(font,"反转术式",x+32,y+6,0xFFE2FFF2);
        g.drawCenteredString(font,unlocked?"恢复 "+BalanceRules.number(Client.progress.reverseHealing()):"15根手指",x+32,y+23,0xFFB7FFE2);
        g.drawCenteredString(font,unlocked?BalanceRules.number(Client.progress.reverseCost())+" 咒力":"解锁自疗",x+32,y+39,0xFFD5C1F4);
        if(unlocked&&Client.progress.burnout()){var r=WheelSideSlots.repair(width,height,ice());g.fill(r.x(),r.y(),r.x()+64,r.y()+36,repairHover?0xEE8A2E3B:0xCC321722);g.drawCenteredString(font,Client.progress.clashPenaltyTicks()>0?"对拼熔断":"修复熔断",r.x()+32,r.y()+5,0xFFFFE6DE);g.drawCenteredString(font,Client.progress.clashPenaltyTicks()>0?((Client.progress.clashPenaltyTicks()+19)/20)+"秒 禁修复":"35%咒力",r.x()+32,r.y()+21,0xFFD9B8C4);}
    }
    /** 右上角的「咒力附拳」固定格：不属于任何分支，轮盘打开就一直在；悬浮后松开轮盘键即开关。 */
    private void fist(GuiGraphics g){
        if(!Client.progress.sukuna()||Client.progress.fingers()<5)return;
        var box=WheelSideSlots.fist(width,height,ice());boolean on=Client.progress.fist();
        int x=box.x(),y=box.y();
        g.fill(x,y,x+box.width(),y+box.height(),fistHover?(on?0xE69D263B:0xE63B7A9D):(on?0xDD5A1530:0xDD1B2A38));
        g.drawCenteredString(font,"咒力附拳",x+box.width()/2,y+4,on?0xFFFFC9C1:0xFFD8E6F2);
        g.drawCenteredString(font,on?"开启 · 再按关闭":"关闭 · 手燃咒力",x+box.width()/2,y+19,on?0xFFFF9E7A:0xFFA8C4D8);
    }
}
