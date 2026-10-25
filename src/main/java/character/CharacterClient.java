package character;

import cn.blockforge.generated.nnnnr1201forge2.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** 只由客户端启动器加载。服务端通用角色定义不引用此类。 */
public final class CharacterClient {
    private static CharacterPackets.State state=new CharacterPackets.State("",0,100,0,false,new int[0]);
    private static Object world;
    private static boolean physicalHeld;
    /** 乙骨复制到的术式编号；服务端同步，客户端拼进轮盘。 */
    private static int[] copies=new int[0];
    public static void install() {
        CharacterPackets.openReceiver=()->Minecraft.getInstance().setScreen(new CharacterSelectionScreen());
        CharacterPackets.receiver=CharacterClient::receive;
    }
    public static CharacterDefinition selected() { return CharacterRegistry.get(state.id()); }
    /**
     * 轮盘实际显示的技能：角色静态表 + 乙骨复制来的旧术式。
     * <p>不能把这些编号拼进 {@link CharacterDefinition}——它的构造器会拒绝 CUSTOM 家族的旧术式编号
     * （「技能编号与术式家族不匹配」），每帧 HUD 调用会把客户端打崩。
     */
    public static java.util.List<Integer> skills() {
        CharacterDefinition base=CharacterRegistry.get(state.id());
        if(base==null)return java.util.List.of();
        if(copies.length==0)return base.skills();
        var list=new java.util.ArrayList<Integer>(base.skills());
        for(int id:copies)if(!list.contains(id))list.add(id);
        return list;
    }
    /** 该编号是否是乙骨掠夺来的术式（客户端判定，仅用于绕过原有解锁链把选择放行）。 */
    public static boolean copied(int skill) {
        for(int id:copies)if(id==skill)return true;
        return false;
    }
    public static boolean toji() { var d=selected();return d!=null&&d.resource()==CharacterDefinition.Resource.STAMINA; }
    public static boolean customWheel() { var d=selected();return d!=null&&d.family()!=CharacterDefinition.Family.SHRINE&&d.hasWheel(); }
    public static boolean allows(int skill) {
        var d=selected();if(d==null)return !CharacterSkill.custom(skill);if(!d.hasWheel())return false;
        for(int id:copies)if(id==skill)return true;
        if(CharacterSkill.custom(skill))return d.config().skill(skill)!=null&&d.unlocked(skill,state.rank());
        if(d.family()==CharacterDefinition.Family.SHRINE)return !(skill>=22&&skill<=23)&&!(skill>=34&&skill<=42);
        return d.unlocked(skill,state.rank())||d.family()==CharacterDefinition.Family.BLOOD&&skill==BloodRules.DEATH_STAR;
    }
    private static void receive(CharacterPackets.State message) {
        var mc=Minecraft.getInstance();boolean changed=!state.id().equals(message.id())||message.changed();state=message;
        copies=message.copies()==null?new int[0]:message.copies();world=mc.level;
        if(changed){
            TechniqueInputClient.clear();var d=selected();
            Client.selected=d==null?0:Math.max(0,d.firstSkill());Client.wheelPage=d!=null&&d.family()==CharacterDefinition.Family.BLOOD?2:0;
            physicalHeld=ClientInput.held(Client.CAST);
            if(mc.screen instanceof SkillWheel||mc.screen instanceof CharacterWheel||mc.screen instanceof CharacterSelectionScreen)mc.setScreen(null);
        }
    }
    public static void clear() { state=new CharacterPackets.State("",0,100,0,false,new int[0]);copies=new int[0];world=null;physicalHeld=false; }
    public static void tick() {
        var mc=Minecraft.getInstance();if(world!=mc.level){clear();world=mc.level;}
        boolean down=ClientInput.held(Client.CAST);
        if(toji()&&down&&!physicalHeld&&mc.player!=null&&mc.player.isAlive()&&!mc.player.isSpectator()&&mc.screen==null&&mc.isWindowActive()&&mc.mouseHandler.isMouseGrabbed()) {
            int kind=mc.options.keyShift.isDown()?1:mc.options.keyJump.isDown()?2:0;
            Network.CHANNEL.sendToServer(new CharacterPackets.Physical(kind));
        }
        physicalHeld=down;
    }
    public static void openWheel() { var mc=Minecraft.getInstance();if(toji())return;mc.setScreen(customWheel()?new CharacterWheel():new SkillWheel()); }
    public static int rank() { return state.rank(); }
    public static CharacterSkill custom(int skill) { var d=selected();return d==null?null:d.config().skill(skill); }
    private static CharacterConfig.SkillText text(int skill) { var d=selected();return d==null?null:d.config().text().get(skill); }
    public static int skillColor(int skill) { var text=text(skill);var custom=custom(skill);return text!=null?text.color():custom!=null?custom.color():selected()==null?0xFFFFFFFF:selected().config().wheel().text(); }
    public static String label(int skill) {
        var text=text(skill);if(text!=null)return text.label();var custom=custom(skill);if(custom!=null)return custom.label();
        String label=BloodRules.skill(skill)?BloodRules.name(skill):skill>=34&&skill<41?GojoPlayer.WHEEL_LABELS[skill-34]:skill==22?"霜凪":skill==23?"直瀑":skill>=7&&skill<17?GuardianPlayerSkills.SHORT[skill-7]:name(skill);
        return label.replace("血之领域·", "").replace("赤鳞跃动·载", "跃动·载");
    }
    public static String name(int skill) {
        var text=text(skill);if(text!=null)return text.name();var custom=custom(skill);if(custom!=null)return custom.name();
        if(CharacterSkill.custom(skill))return "未配置技能";
        if(skill>=34&&skill<34+GojoPlayer.SLOTS)return GojoPlayer.nameOf(GojoPlayer.WHEEL[skill-34]);
        return Client.skillName(skill);
    }
    public static int cost(int skill) {
        var custom=custom(skill);if(custom!=null)return custom.cost();
        if(BloodRules.skill(skill))return BloodRules.cost(skill);
        if(skill==22||skill==23)return Uraume.cost(skill);
        if(skill>=34&&skill<34+GojoPlayer.SLOTS)return GojoPlayer.costOf(GojoPlayer.WHEEL[skill-34]);
        if(skill>=7&&skill<17)return GuardianPlayerSkills.COST[skill-7];
        return skill>=0&&skill<7?Curse.COST[skill]:0;
    }
    public static String description(int skill) {
        var text=text(skill);if(text!=null)return text.description();var custom=custom(skill);if(custom!=null)return custom.description()+" · 冷却"+(custom.cooldown()/20.0)+"秒 · 本阶段最多"+custom.targetsAt(state.rank())+"目标";
        if(BloodRules.skill(skill))return BloodRules.DESC[skill-BloodRules.FIRST];
        if(skill==22)return "前方冰汽冻结敌人；冰鱼强化保留";
        if(skill==23)return "身后冰柱向准星区域降落；冰鱼强化保留";
        if(skill>=34&&skill<34+GojoPlayer.SLOTS)return GojoPlayer.descOfSlot(skill-34);
        if(skill>=7&&skill<17)return GuardianPlayerSkills.DESC[skill-7];
        return "";
    }
    /** 返回 true 表示已经绘制角色专用HUD，旧宿傩HUD不再叠画。 */
    public static boolean hud(GuiGraphics g) {
        var d=selected();if(d==null||d.family()==CharacterDefinition.Family.SHRINE)return false;
        var mc=Minecraft.getInstance();if(mc.screen instanceof CharacterWheel||mc.screen instanceof CharacterSelectionScreen)return true;
        int h=mc.getWindow().getGuiScaledHeight(),w=mc.getWindow().getGuiScaledWidth(),bar=Math.min(224,w-20);
        int y=12;for(var line:mc.font.split(Component.literal(Client.progress.guide()+" · J查看个人线"),Math.min(350,w-20))){g.drawString(mc.font,line,10,y,0xFFE5E2EC);y+=11;}
        double amount=toji()?state.stamina():Client.progress.energy(),max=toji()?state.maximum():Client.progress.maximum();
        g.fill(7,h-102,7+bar,h-76,0xD0191C24);g.fill(11,h-84,3+bar,h-80,0xFF343A43);
        g.fill(11,h-84,11+(int)((bar-8)*Math.max(0,Math.min(1,amount/Math.max(1,max)))),h-80,d.color());
        g.drawString(mc.font,d.resourceName()+" "+(int)amount+" / "+(int)max+" · 阶段"+state.rank(),12,h-97,d.color());
        g.fill(7,h-68,7+bar,h-12,0xC0151821);
        g.drawString(mc.font,d.name()+" · "+d.technique(),12,h-63,d.color());
        if(toji()){
            g.drawString(mc.font,Client.CAST.getTranslatedKeyMessage().getString()+"突进20 · 潜行+释放 重击30",12,h-47,0xFFE3F4E9);
            g.drawString(mc.font,"跳跃+释放 跃身25 · 停手恢复体术",12,h-31,0xFFB6CCBD);
        }else{
            g.drawString(mc.font,name(Client.selected),12,h-47,0xFFF1E9E7);
            g.drawString(mc.font,"长按 "+Client.WHEEL.getTranslatedKeyMessage().getString()+" 选择 · "+Client.CAST.getTranslatedKeyMessage().getString()+" 释放",12,h-31,0xFFCAD0DB);
            if(Client.progress.burnout())g.drawCenteredString(mc.font,"术式熔断中，请等待恢复",w/2,h-112,0xFFFF7986);
        }
        return true;
    }
    private CharacterClient() {}
}
