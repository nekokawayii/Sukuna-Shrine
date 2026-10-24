package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

/** 客户端只上报输入；护盾、招式冷却、QTE 与通关由服务端计算。 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class ObserverFight {
    private static ObserverPackets.State state;
    private static java.util.List<ObserverPackets.Cursor> otherCursors=java.util.List.of();
    public static java.util.List<ObserverPackets.Cursor> otherCursors(){return otherCursors;}
    public static void cursors(ObserverPackets.Cursors packet){if(state!=null&&state.session().equals(packet.session()))otherCursors=packet.players();}
    private static void drawCursors(GuiGraphics g,int w,int h){for(var c:otherCursors){int x=Math.max(3,Math.min(w-4,(int)(c.x()*w))),y=Math.max(3,Math.min(h-12,(int)(c.y()*h)));int color=0xFF000000|java.awt.Color.HSBtoRGB((c.player().hashCode()&255)/255f,.65f,1);for(int row=0;row<10;row++)g.fill(x,y+row,x+1+row/2,y+row+1,color);g.drawString(Minecraft.getInstance().font,c.name(),Math.min(w-Minecraft.getInstance().font.width(c.name())-2,x+7),y,color);}}
    private static BattleScreen screen;
    private static float weakX=-1000,weakY=-1000,radius=12,cursorX=.5f,cursorY=.5f;
    private static int lastWeak=-1,stale;
    private static WindowYuji windowYuji;
    private static int slashAt=-1000;
    public static void slash(){if(state!=null)slashAt=state.age();}
    private static boolean exitRequested,closeOnVictory,drawingMelee;
    public static boolean melee(){return state!=null&&state.phase()==1&&state.attack()==ObserverBattleRules.MELEE&&state.attackLeft()>0;}
    @SubscribeEvent public static void hideTeleported(net.minecraftforge.client.event.RenderLivingEvent.Pre<?,?> e){
        if(melee()&&!drawingMelee&&e.getEntity().getId()==state.boss())e.setCanceled(true);
    }
    public static boolean closeOnVictory(){return closeOnVictory;}
    private static final String[] CODE={
        "0x00000000","BOUNDARY_NULL","INVALID_HANDLE","CONNECTION_LOST",
        "0xFFFFFFFF","PLAYER_NOT_FOUND","ACCESS_VIOLATION"};
    public static boolean fighting(){return state!=null&&state.phase()==1;}
    public static boolean shatter(){return state!=null&&state.phase()==2;}
    public static ObserverPackets.State state(){return state;}
    public static ObserverEntity boss(){var mc=Minecraft.getInstance();return state!=null&&mc.level!=null&&mc.level.getEntity(state.boss()) instanceof ObserverEntity b?b:null;}
    public static void receive(ObserverPackets.State next){
        var old=state;boolean fresh=old==null||!old.session().equals(next.session());
        if(fresh){otherCursors=java.util.List.of();ObserverClient.begin();ObserverDialog.clear();ObserverWindow.begin();screen=new BattleScreen();exitRequested=false;closeOnVictory=false;lastWeak=-1;}
        state=next;stale=0;
        var mc=Minecraft.getInstance();if(mc.level!=null&&mc.player!=null&&mc.screen!=screen)mc.setScreen(screen);
        if(next.hits()>5&&(old==null||old.hits()<=5)){ObserverWindow.windowed();ObserverDialog.say("边界错误","虎杖：窗框也不是你的避难所。");}
        if(next.staggerLeft()>0&&(old==null||old.staggerLeft()==0))ObserverWindow.impact();
        if(next.phase()==1&&(old==null||!old.move().equals(next.move())))ObserverWindow.technique(next.move());
        if(next.phase()==2&&(old==null||old.phase()!=2)){ObserverWindow.restore();ObserverDialog.clear();}
        if(next.phase()==3&&(old==null||old.phase()!=3)){ObserverWindow.restore();ObserverDialog.say("护盾耗尽","你输了。这次先退回竞技场，下次再来。");}
        switch(next.line()){
            case 1 -> {ScreenCracks.trigger(next.age());ObserverDialog.say("屏幕外的你","宿傩：这一次，疼的是屏幕后面的你。护盾 "+next.shield()+" / 15");}
            case 2 -> ObserverDialog.say("世界外打击","宿傩：你竟然能从外面……伤到我？");
            case 3 -> {ObserverDialog.say("世界外打击","宿傩：这具身体……竟然不再服从我！");ObserverDialog.say("虎杖受击","虎杖：别停下来！这道边界正在松动！");}
            case 4 -> ObserverDialog.say("世界斩落空","宿傩：连窗口的位置，也能改变吗？");
            case 5 -> ObserverDialog.say("领域中断","宿傩：你把我的规则……打断了。");
            case 6 -> ObserverDialog.say("体术落空","宿傩：反应不错……下次可没有这么幸运。");
            default -> {}
        }
    }
    public static void end(){
        otherCursors=java.util.List.of();state=null;windowYuji=null;slashAt=-1000;weakX=weakY=-1000;lastWeak=-1;stale=0;exitRequested=false;closeOnVictory=false;
        ObserverWindow.restore();ObserverDialog.clear();
        var mc=Minecraft.getInstance();if(mc.screen==screen)mc.setScreen(null);screen=null;
    }
    public static void input(int op,int value,float x,float y){if(state!=null)Network.CHANNEL.sendToServer(new ObserverPackets.Input(state.session(),op,value,x,y));}
    private static void send(int op,int value){input(op,value,cursorX,cursorY);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||state==null)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null){ObserverClient.end();return;}
        ObserverDialog.tick();ObserverWindow.tick();
        if(++stale>100){ObserverWindow.restore();return;}
        var desktop=ObserverWindow.desktopAllowed()?ObserverDesktop.sample():null;
        boolean outside=desktop!=null&&desktop.focused();send(ObserverBattleRules.DESKTOP,outside?1:0);
        if(outside){cursorX=desktop.cursorX();cursorY=desktop.cursorY();}
        send(ObserverBattleRules.CURSOR,0);ObserverDesktop.tick(state);
    }
    /** 使用当前世界姿态矩阵投影红圈，不能把上一帧的全局 GUI 矩阵当作世界矩阵。 */
    @SubscribeEvent public static void project(RenderLevelStageEvent e){
        if(state==null||e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var boss=boss();if(boss==null){weakX=-1000;return;}
        double height=switch(state.weakPart()){case 0->1.55;case 1->1.0;default->.48;};
        Vec3 point=boss.getPosition(e.getPartialTick()).add(0,height,0).subtract(e.getCamera().getPosition());
        Vector4f v=new Vector4f((float)point.x,(float)point.y,(float)point.z,1);
        v.mul(e.getPoseStack().last().pose()).mul(e.getProjectionMatrix());
        if(v.w<=.01f){weakX=-1000;return;}
        var w=Minecraft.getInstance().getWindow();weakX=(v.x/v.w*.5f+.5f)*w.getGuiScaledWidth();weakY=(.5f-v.y/v.w*.5f)*w.getGuiScaledHeight();
        radius=Math.max(8,Math.min(22,w.getGuiScaledHeight()/v.w*.18f));
    }
    private static void render(GuiGraphics g,int w,int h){
        if(state==null)return;var mc=Minecraft.getInstance();int age=state.age();
        if(age<30){
            int fall=age*age/12;
            for(int i=0;i<12;i++){int x=i*w/12;g.fill(x,h/2+fall+(i%3)*8,x+w/12-2,h+fall,0xCC23232B);}
        }
        if(ScreenCracks.visible()&&state.phase()==1){g.flush();ScreenCracks.render(w,h);}
        if(age>=160&&age<180)g.fill(0,0,w,h,0xFF000000);
        if(state.phase()==2){shattered(g,w,h);return;}
        if(state.phase()==3){g.fill(0,0,w,h,0xEE000000);ObserverDialog.render(g,w,h);return;}
        if(state.phase()==1){
            if(state.hits()>5)code(g,w,h,age);
            bars(g,w);
            if(age-slashAt>=0&&age-slashAt<14&&weakX>0){
                float t=(age-slashAt)/14f;int cx=(int)(weakX*(1-t)+w*.5f*t),cy=(int)(weakY*(1-t)+h*.5f*t),len=(int)(10+t*w*.5f);
                for(int i=-len;i<=len;i++){int thick=Math.max(1,(int)((1-Math.abs(i)/(float)len)*(3+t*9)));g.fill(cx+i,cy+i/4-thick-1,cx+i+1,cy+i/4+thick+2,0xFFEAEAEA);g.fill(cx+i,cy+i/4-thick,cx+i+1,cy+i/4+thick,0xFF1B0A12);}
            }
            if(state.weakLeft()>0&&weakX>0&&state.attackLeft()==0){
                int cx=(int)weakX,cy=(int)weakY,r=(int)radius;
                for(int i=0;i<32;i++){double a=i*Math.PI/16;int x=cx+(int)(Math.cos(a)*r),y=cy+(int)(Math.sin(a)*r);g.fill(x-1,y-1,x+2,y+2,0xFFFF1744);}
                g.fill(cx-3,cy-3,cx+3,cy+3,0xFFFFCED6);
            }
            if(!ObserverDesktop.active())for(var blade:state.blades()){
                if(blade.style()==11){double n=Math.max(.00001,Math.hypot(blade.vx(),blade.vy()));for(int i=0;i<=80;i++){double f=i/80.0;int x=(int)((blade.x()-blade.vx()/n*.38*f)*w),y=(int)((blade.y()-blade.vy()/n*.38*f)*h);g.fill(x-3,y-3,x+4,y+4,0xBB9E001C);g.fill(x-1,y-1,x+2,y+2,0xFFFF6478);}}
                else ObserverGlyph.draw(g,(int)(blade.x()*w),(int)(blade.y()*h),blade.style(),age);
            }
            if(state.attackLeft()>0){
                if(state.attack()==ObserverBattleRules.MELEE){
                    int cx=(int)(state.meleeX()*w),cy=(int)(state.meleeY()*h);
                    int rx=(int)(w*ObserverBattleRules.MELEE_RADIUS),ry=(int)(h*ObserverBattleRules.MELEE_RADIUS);
                    for(int i=0;i<48;i++){double a=i*Math.PI/24;int x=cx+(int)(Math.cos(a)*rx),y=cy+(int)(Math.sin(a)*ry);g.fill(x-1,y-1,x+2,y+2,0xFFFF1838);}
                    var b=boss();if(b!=null){drawingMelee=true;int old=b.action();b.action(5);
                        try{ObserverPortrait.draw(g,b,age,true);if(!ObserverDesktop.active()){int x=Math.max(30,Math.min(w-30,cx+(cx>w/2?-35:35)));int y=Math.max(80,Math.min(h-15,cy+40));GuiEntityPreview.render(g,x,y,40,cx-x,0,b);}}
                        finally{b.action(old);drawingMelee=false;}
                    }
                    int bw=Math.min(160,w-24);g.fill(w/2-bw/2,h-31,w/2+bw/2,h-26,0xFF442030);
                    g.fill(w/2-bw/2,h-31,w/2-bw/2+bw*state.attackLeft()/ObserverBattleRules.MELEE_REACTION,h-26,0xFFFF2345);
                }else if(state.attack()==ObserverBattleRules.DOMAIN){
                    g.fill(0,0,w,h,0xF0000000);bars(g,w);
                    int count=state.keys().length,step=Math.min(34,Math.max(12,(w-20)/count));
                    for(int i=0;i<count;i++){int x=w/2+(2*i-count+1)*step/2;g.fill(x-step/2+1,h/2-12,x+step/2-1,h/2+22,i<state.keysDone()?0xFF286A42:0xFF34232C);g.drawCenteredString(mc.font,Character.toString((char)(state.keys()[i]+32)),x,h/2+1,0xFFFFFFFF);}
                }else if(state.attack()==ObserverBattleRules.SUPPORT){
                    if(weakX>0)for(int i=0;i<32;i++){double a=i*Math.PI/16;int x=(int)(weakX+Math.cos(a)*(20+state.attackLeft())),y=(int)(weakY+Math.sin(a)*14);g.fill(x,y,x+3,y+3,0xFF57F5C5);}
                }else{
                    g.fill(12,h-30,w-12,h-25,0xFF442630);g.fill(12,h-30,12+(int)((w-24)*state.dodge()),h-25,0xFFFF3355);
                }
                g.drawCenteredString(mc.font,String.format(java.util.Locale.ROOT,"%.2f 秒",state.attackLeft()/20f),w/2,h/2+34,0xFFFF8899);
            }
            if(state.staggerLeft()>0){g.fill(0,0,w,19,0xFF182B44);g.drawString(mc.font,"Minecraft",5,5,0xFFFFFFFF);}
        }
        drawCursors(g,w,h);
        ObserverDialog.render(g,w,h);
    }
    private static void bars(GuiGraphics g,int w){
        var f=Minecraft.getInstance().font;int x=12,bw=w-24;
        g.fill(x,8,x+bw,20,0xEE241924);g.fill(x+1,9,x+1+(bw-2)*(100-state.hits()*5)/100,19,0xFFD72245);
        g.drawCenteredString(f,"高位  "+(100-state.hits()*5)+"%",w/2,10,0xFFFFFFFF);
        for(int i=0;i<15;i++){int a=x+i*bw/15;g.fill(a,25,a+bw/15-2,31,i<state.shield()?0xFF23A9ED:0xFF6A6975);}
        g.drawString(f,"屏幕外护盾 "+state.shield()+" / 15",x,35,0xFF26374B,false);
        g.drawString(f,state.move(),x,47,0xFF82203D,false);
    }
    private static void code(GuiGraphics g,int w,int h,int age){
        // 窗框仍保留故障纹理；角色与弹幕交给独立桌面窗口。
        g.fill(0,0,6,h,0xFF152330);g.fill(w-6,0,w,h,0xFF152330);
        g.fill(0,0,w,5,0xFF152330);g.fill(0,h-20,w,h-16,0xFF152330);
        for(int i=0;i<14;i++){int y=(age*2+i*41)%Math.max(1,h);String text=CODE[i%CODE.length];int x=i%2==0?2:w-Minecraft.getInstance().font.width(text)-2;g.drawString(Minecraft.getInstance().font,text,x,y,0x883ACF69,false);}
        var mc=Minecraft.getInstance();
        if(mc.level!=null){
            if(windowYuji==null||windowYuji.level()!=mc.level)windowYuji=new WindowYuji(mc.level);
            windowYuji.tickCount=age;
            ObserverPortrait.draw(g,windowYuji,age,false);
            if(!ObserverDesktop.active())GuiEntityPreview.render(g,w-35,88,32,12,0,windowYuji);
        }
        if(state.staggerLeft()>10)ObserverDialog.frame(g,6,60,Math.min(210,w-12),54,"Java Runtime Error","BoundaryAccessException: 高位正在越界");
    }
    private static void shattered(GuiGraphics g,int w,int h){
        g.fill(0,0,w,h,0xFF000000);var f=Minecraft.getInstance().font;
        for(int i=0;i<4;i++)g.drawCenteredString(f,"Connection Lost: Player_Not_Found",w/2,Math.max(8,h/2-65)+i*14,i==3?0xFFFF3349:0xFF841F2B);
        g.drawCenteredString(f,screen==null?"":screen.command+"_",w/2,h/2+47,0xFFFFFFFF);
    }
    /** 仅用于 GUI 画面、不加入世界的虎杖；复用已有模型皮肤，不产生 AI 与存档实体。 */
    private static final class WindowYuji extends YujiEntity {
        WindowYuji(net.minecraft.world.level.Level level){super(SoulContent.YUJI.get(),level);}
        @Override public int action(){return state!=null&&(state.staggerLeft()>0||state.age()%160>=30&&state.age()%160<44)?4:1;}
        @Override public int actionAge(){return state==null?0:state.age()%20;}
    }
    private static final class BattleScreen extends Screen {
        private String command="";
        private final java.util.Set<Integer> heldKeys=new java.util.HashSet<>();
        BattleScreen(){super(Component.literal("高位"));}
        @Override public boolean isPauseScreen(){return false;}
        @Override public boolean shouldCloseOnEsc(){return false;}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){
            cursorX=Math.max(0,Math.min(1,mx/(float)Math.max(1,width)));cursorY=Math.max(0,Math.min(1,my/(float)Math.max(1,height)));
            ObserverFight.render(g,width,height);
        }
        @Override public boolean mouseClicked(double x,double y,int button){
            if(state==null||state.phase()!=1)return true;
            if(button==0&&state.weakLeft()>0&&state.staggerLeft()==0&&state.attackLeft()==0&&lastWeak!=state.weakId()
                &&Math.hypot(x-weakX,y-weakY)<=radius+6){lastWeak=state.weakId();send(ObserverBattleRules.WEAK,state.weakId());}
            return true;
        }
        @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
            if(button==1&&state!=null){ObserverWindow.drag(dx,dy);if(state.attack()==1&&state.attackLeft()>0)ObserverClient.drag(dx,dy);}
            return true;
        }
        @Override public boolean keyPressed(int key,int scan,int mods){
            if(key==GLFW.GLFW_KEY_F7){ObserverNativeDialog.toggle();return true;}
            if(key==GLFW.GLFW_KEY_F8){ObserverWindow.disable();return true;}
            if(key==GLFW.GLFW_KEY_ESCAPE){
                if(!exitRequested){exitRequested=true;closeOnVictory=shatter();send(shatter()?ObserverBattleRules.RESET:ObserverBattleRules.ABORT,0);ObserverWindow.restore();}
                if(stale>200)ObserverClient.end();return true;
            }
            if(shatter()){
                if(key==GLFW.GLFW_KEY_BACKSPACE&&!command.isEmpty())command=command.substring(0,command.length()-1);
                else if(key==GLFW.GLFW_KEY_ENTER&&command.trim().equals("/sukuna reset"))send(ObserverBattleRules.RESET,0);
            }else if(state!=null&&state.attack()==2&&state.attackLeft()>0&&heldKeys.add(key))send(ObserverBattleRules.KEY,key);
            return true;
        }
        @Override public boolean keyReleased(int key,int scan,int mods){heldKeys.remove(key);return true;}
        @Override public void tick(){if(!Minecraft.getInstance().isWindowActive())heldKeys.clear();}
        @Override public boolean charTyped(char c,int mods){if(shatter()&&command.length()<40&&!Character.isISOControl(c))command+=c;return true;}
        @Override public void onClose(){send(shatter()?ObserverBattleRules.RESET:ObserverBattleRules.ABORT,0);ObserverWindow.restore();}
    }
    private ObserverFight(){}
}
