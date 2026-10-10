package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.Minecraft;

/** 不改原版全局按键表；按键沿与心跳仅属于本机玩家。 */
public final class TechniqueInputClient {
    private static boolean previous;private static int heldSkill=-1,ticks,worldLease;
    private static TechniqueInput.Input packet(int skill,int phase){var mc=Minecraft.getInstance();return new TechniqueInput.Input(skill,phase,mc.options.keyUp.isDown(),mc.options.keyDown.isDown(),mc.options.keyJump.isDown(),mc.options.keyShift.isDown());}
    private static void send(int skill,int phase){Network.CHANNEL.sendToServer(packet(skill,phase));}
    public static void cancel(){if((heldSkill>=0||worldLease>0)&&Minecraft.getInstance().player!=null)send(heldSkill>=0?heldSkill:SlashFormRules.WORLD,3);heldSkill=-1;previous=false;ticks=0;worldLease=0;}
    public static void clear(){heldSkill=-1;previous=false;ticks=worldLease=0;}
    public static void tick(){
        var mc=Minecraft.getInstance();boolean down=ClientInput.held(Client.CAST);
        boolean valid=mc.player!=null&&mc.player.isAlive()&&mc.screen==null&&mc.isWindowActive()&&mc.mouseHandler.isMouseGrabbed()&&Client.progress.unlocked();
        if(!valid||heldSkill>=0&&heldSkill!=Client.selected||worldLease>0&&Client.selected!=SlashFormRules.WORLD){cancel();previous=down;return;}
        if(worldLease>0){worldLease--;if(worldLease%10==0)send(SlashFormRules.WORLD,1);}
        // 动作中的输入不能再次启动技能，但仍必须送达松手与取消。
        if(heldSkill>=0){if(!down){send(heldSkill,2);heldSkill=-1;}else if(++ticks%10==0)send(heldSkill,1);}
        if(down&&!previous&&heldSkill<0&&!FinaleClient.local()&&!DomainClashClient.active()&&!NetBranchVisuals.locked()&&!DismantleMotionClient.locked()&&Client.selected!=SlashFormRules.FREEHAND){heldSkill=Client.selected;ticks=0;if(heldSkill==SlashFormRules.WORLD)worldLease=320;send(heldSkill,0);}
        previous=down;
    }
    private TechniqueInputClient(){}
}
