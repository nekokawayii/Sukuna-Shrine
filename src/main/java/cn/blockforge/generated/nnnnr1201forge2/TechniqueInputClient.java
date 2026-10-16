package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.Minecraft;

/** 不改原版全局按键表；按键沿与心跳仅属于本机玩家。 */
public final class TechniqueInputClient {
    private static boolean previous;private static int heldSkill=-1,ticks,worldLease;
    private static TechniqueInput.Input packet(int skill,int phase){
        var mc=Minecraft.getInstance();
        boolean up=mc.options.keyUp.isDown(),downKey=mc.options.keyDown.isDown(),jump=mc.options.keyJump.isDown(),crouch=mc.options.keyShift.isDown();
        boolean sprint=mc.player!=null&&mc.player.isSprinting(),ground=mc.player!=null&&mc.player.onGround();
        double vy=mc.player==null?0:mc.player.getDeltaMovement().y;
        // 动作意图按《使用说明》四(三)(四)当场算好：后退+跳=后翻、蹲下+前进=飞踢、蹲下不动=跪地、
        // 疾跑前进=滑铲、疾跑起跳=冲拳；0 表示"这不是动作，走普通解"。
        int motion=DismantleMotionRules.choose(crouch,up,downKey,jump,sprint,ground,vy);
        return new TechniqueInput.Input(skill,phase,up,downKey,jump,crouch,motion,Client.selected);
    }
    private static void send(int skill,int phase){Network.CHANNEL.sendToServer(packet(skill,phase));}
    public static void cancel(){if((heldSkill>=0||worldLease>0)&&Minecraft.getInstance().player!=null)send(heldSkill>=0?heldSkill:SlashFormRules.WORLD,3);heldSkill=-1;previous=false;ticks=0;worldLease=0;}
    public static void clear(){heldSkill=-1;previous=false;ticks=worldLease=0;}
    public static void tick(){
        var mc=Minecraft.getInstance();boolean down=ClientInput.held(Client.CAST);
        boolean valid=mc.player!=null&&mc.player.isAlive()&&mc.screen==null&&mc.isWindowActive()&&mc.mouseHandler.isMouseGrabbed()&&Client.progress.unlocked();
        // 手持神武解：释放键改为"充能"，与轮盘选择无关（先处理，避免被 selected 校验打断）
        boolean tool=mc.player!=null&&(mc.player.getMainHandItem().is(VoidContent.SHINBUKE.get())||mc.player.getOffhandItem().is(VoidContent.SHINBUKE.get()));
        if(tool){
            if(heldSkill>=0){send(heldSkill,3);heldSkill=-1;}
            if(down&&!previous)send(Shinbuke.CHARGE_ID,0);
            else if(!down&&previous)send(Shinbuke.CHARGE_ID,2);
            else if(down&&++ticks%10==0)send(Shinbuke.CHARGE_ID,1);
            previous=down;
            return;
        }
        if(!valid||heldSkill>=0&&heldSkill!=Client.selected||worldLease>0&&Client.selected!=SlashFormRules.WORLD){cancel();previous=down;return;}
        if(worldLease>0){worldLease--;if(worldLease%10==0)send(SlashFormRules.WORLD,1);}
        // 动作中的输入不能再次启动技能，但仍必须送达松手与取消。
        if(heldSkill>=0){if(!down){send(heldSkill,2);heldSkill=-1;}else if(++ticks%10==0)send(heldSkill,1);}
        if(down&&!previous&&heldSkill<0&&!FinaleClient.local()&&!DomainClashClient.active()&&!NetBranchVisuals.locked()&&!DismantleMotionClient.locked()&&Client.selected!=SlashFormRules.FREEHAND){
            heldSkill=Client.selected;ticks=0;
            if(heldSkill==SlashFormRules.WORLD)worldLease=320;send(heldSkill,0);
        }
        previous=down;
    }
    private TechniqueInputClient(){}
}
