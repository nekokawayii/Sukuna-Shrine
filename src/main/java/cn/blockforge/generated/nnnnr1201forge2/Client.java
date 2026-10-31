package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraftforge.client.settings.KeyConflictContext;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** 事件由 ClientBootstrap 显式注册，避免核心渲染路径只依赖自动扫描。 */
public final class Client {
    private static KeyMapping gameplayKey(String name,int key){return new KeyMapping(name,KeyConflictContext.IN_GAME,com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,key,"key.categories.sukuna");}
    public static final KeyMapping WHEEL=gameplayKey("key.sukuna.wheel",GLFW.GLFW_KEY_N);
    public static final KeyMapping CAST=gameplayKey("key.sukuna.cast",GLFW.GLFW_KEY_R);
    public static final KeyMapping GUIDE=gameplayKey("key.sukuna.guide",GLFW.GLFW_KEY_J);
    public static final KeyMapping ROTATE=gameplayKey("key.sukuna.rotate",GLFW.GLFW_KEY_V);
    public static final KeyMapping RELATIONS=gameplayKey("key.sukuna.relations",GLFW.GLFW_KEY_O);
    public static final KeyMapping FIST=gameplayKey("key.sukuna.fist",GLFW.GLFW_KEY_DELETE);
    public static final KeyMapping STREAM=gameplayKey("key.sukuna.stream",GLFW.GLFW_KEY_HOME);
    public static final KeyMapping AMPLIFY=gameplayKey("key.sukuna.amplify",GLFW.GLFW_KEY_G);
    public static final KeyMapping LIMBS=gameplayKey("key.sukuna.limbs",GLFW.GLFW_KEY_U);
    public static final KeyMapping LIMB_1=gameplayKey("key.sukuna.limb1",GLFW.GLFW_KEY_LEFT_BRACKET);
    public static final KeyMapping LIMB_2=gameplayKey("key.sukuna.limb2",GLFW.GLFW_KEY_RIGHT_BRACKET);
    public static final KeyMapping LIMB_3=gameplayKey("key.sukuna.limb3",GLFW.GLFW_KEY_SEMICOLON);
    public static final KeyMapping LIMB_4=gameplayKey("key.sukuna.limb4",GLFW.GLFW_KEY_APOSTROPHE);
    private static void limbUse(int index) {
        if (Minecraft.getInstance().screen != null) return;
        Network.CHANNEL.sendToServer(new TechniqueInput.Input(2100, 7, false, false, false, false, 522 + index, 0));
    }
    private static boolean castHeld,charging,cleaveHeld;
    private static long chargeBorn;
    public static int selected;
    public static Network.Progress progress=new Network.Progress(1000,1000,0,false,false,false,false,false,false,false,"",false,false,false,false,1,300,false,0,false,false,0,4,false,false,false,false,DomainOptions.DEFAULT_RADIUS,DomainOptions.DEFAULT_DAMAGE,DomainOptions.DEFAULT_SPEED,true);
    public static String energyText(double amount){return BalanceRules.number(amount);}
    public static int wallSize(){return BranchRules.wallSize(progress.nine(),progress.soulShrine(),progress.voidShrine(),progress.pureLandVictory());}
    public static boolean voidStage(){return progress.voidShrine();}
    public static boolean soulStage(){return progress.soulShrine()&&!progress.voidShrine();}
    /** 本地玩家的伏魔御厨子是不是已经开着（服务端展开/收起时会广播 kind=1 / kind=5 的领域表现，这里读的就是它）：
     *  点数分配界面只在"开启御厨子"的那一次按下弹，第二次（放下）与第三次（收起）不弹。 */
    /** 上一次的轮盘选中项：用于"在轮盘里**选中**御厨子(技能5)时"弹出点数分配界面（用户要求：选中后出现、调整完确定，再按释放键释放）。 */
    private static int lastShrineSel=-1;
    public static boolean shrineDomainOpen(){var p=Minecraft.getInstance().player;return p!=null&&domains.containsKey(p.getId());}
    public static int wheelPage;
    public static int wheelCount(){if(wheelPage>=2&&progress.nine())return BloodRules.count(wheelPage);return wheelPage==1&&progress.pureLandVictory()?GuardianPlayerSkills.COUNT-1:baseWheelCount();}
    public static int baseWheelCount(){return voidStage()||soulStage()?7:6;}
    /** 当前选择不可用时退回一套自己真能用的：宿傩→里梅→六眼。只吃六眼的玩家也能直接开打。 */
    public static int fallbackSkill(){
        var role=character.CharacterClient.selected();if(role!=null)return Math.max(0,role.firstSkill());
        if(progress.gojo())return 34;   // 只吃六眼的玩家默认落在五条悟轮盘上
        if(progress.sukuna())return 0;
        if(progress.uraume())return BranchRules.FROST;
        return 0;
    }
    /** 手持神武解：术式全部不可用（释放键改为充能）。 */
    public static boolean toolBlocksTechnique(){
        var p=net.minecraft.client.Minecraft.getInstance().player;
        return p!=null&&(p.getMainHandItem().is(VoidContent.SHINBUKE.get())||p.getOffhandItem().is(VoidContent.SHINBUKE.get()));
    }
    public static boolean availableSkill(int skill){if(toolBlocksTechnique()||!character.CharacterClient.allows(skill))return false;
        // 乙骨的复制品：借用的是旧术式编号，但不该受原角色的解锁链约束。
        // 否则永远选不中，Client.selected 会停在 1000（里香），表现为「复制术式变成里香、共用冷却」。
        if(character.CharacterClient.copied(skill))return true;
        if(character.CharacterSkill.custom(skill))return character.CharacterClient.custom(skill)!=null;
        var role=character.CharacterClient.selected();if(role!=null&&role.family()==character.CharacterDefinition.Family.MARTIAL)return role.skills().contains(skill);
        if(BloodRules.skill(skill))return progress.nine()&&!DeathStarClient.anemic()&&(skill!=BloodRules.DEATH_STAR||BloodClient.ownDomain());
        if(skill==Skills.FIST_SKILL)return progress.sukuna()&&progress.fingers()>=5;
        if(skill==SimpleDomain.SKILL)return progress.sukuna()&&progress.fingers()>=5;if(skill==FurnaceMax.SKILL)return progress.sukuna()&&progress.voidShrine();if(skill==FinaleRules.SKILL)return FinaleClient.ready()||FinaleClient.local();if(skill==22||skill==23)return progress.uraume();if(skill>=34&&skill<34+GojoPlayer.COUNT)return progress.gojo();
        if(progress.gojo()&&!progress.sukuna())return false; // 只有六眼：其余技能组一律不可选
        if(!progress.sukuna())return false;if(CombatExpansionRules.branch(skill))return skill==CombatExpansionRules.PURPLE_FURNACE?progress.voidShrine():skill==CombatExpansionRules.DRAGON_STAR?progress.awakened():skill!=CombatExpansionRules.CLEAVE_LINGER||progress.fingers()>=5;if(skill==SlashFormRules.SHAPE||skill==SlashFormRules.FREEHAND||skill==DismantleMotionRules.RING_SKILL)return true;if(skill==SlashFormRules.CROSS_FLIGHT)return progress.fingers()>=5;if(skill==SlashFormRules.WORLD)return progress.awakened();if(skill==19||skill==20)return progress.fingers()>=5;if(skill==21||NetBranchRules.branch(skill))return progress.awakened();return skill>=0&&(skill<baseWheelCount()||progress.pureLandVictory()&&skill>=7&&skill<17);}
    public static String skillName(int i){if(character.CharacterSkill.custom(i))return character.CharacterClient.name(i);if(BloodRules.skill(i))return BloodRules.name(i);if(i==Skills.FIST_SKILL)return "咒力附拳";if(i==FurnaceMax.SKILL)return FurnaceMax.NAME;if(i==SimpleDomain.SKILL)return "简易领域 · 捌";if(i==FinaleRules.SKILL)return FinaleRules.NAME;if(CombatExpansionRules.branch(i))return CombatExpansionRules.name(i);if(i>=34)return GojoPlayer.nameOf(i-34);if(i>=24)return SlashFormRules.name(i);if(i==0)return progress.dismantleQi()?"解 · 剑气模式":"解 · 斩击模式";if(i>=19&&i<=23)return BranchRules.name(i);if(NetBranchRules.branch(i))return (i==18?"解 · ":"网 · ")+NetBranchRules.name(i);if(i>=7&&i<17)return GuardianPlayerSkills.NAMES[i-7];if(voidStage())return i==5&&progress.pureLandVictory()?"领域展开 · 虚空御厨子（蓝）":VoidCombat.NAMES[i];return i==5&&soulStage()?"领域展开 · 灵魂御厨子":(progress.awakened()?Skills.AWAKENED:Skills.NAMES)[i];}
    public static double growth(){return 1;}
    private static long shakeUntil,flashUntil,transitionUntil,eyesUntil;
    private static float reflected;
    private static boolean redCut,soulCut;
    private static boolean jumpHeld;
    private static int jumpPress;
    private static long jumpAt;
    /** 空格三击（三次按下都落在 {@link ShrineRules#TRIPLE_JUMP_TICKS} 刻的总窗口内）：请服务端把自己送上神龛。 */
    private static void tripleJump(Minecraft mc){
        boolean down=progress.sukuna()&&mc.screen==null&&mc.isWindowActive()&&mc.options.keyJump.isDown();
        if(down&&!jumpHeld){
            // 超时就从这一击重新起算，避免"随便连跳"被当成三击。
            if(jumpPress==0||tick-jumpAt>ShrineRules.TRIPLE_JUMP_TICKS){jumpPress=0;jumpAt=tick;}
            if(++jumpPress>=3){jumpPress=0;Network.CHANNEL.sendToServer(new Network.ShrineLeap());}
        }
        jumpHeld=down;
    }

    private static final List<Visual> visuals=new ArrayList<>();
    private static final List<Transient> transients=new ArrayList<>();
    private static final Map<Integer,DomainVisual> domains=new HashMap<>();
    private static final Map<Integer,AuraVisual> auras=new HashMap<>();
    private static final Set<Integer> supreme=new HashSet<>();
    private static Object world;
    private static long tick,window;
    private static int generated;
    private static boolean heldLast;
    private static final Random random=new Random();
    private static class Visual {
        Network.Effect e;long born;boolean background;double floor;PhotonBridge.Handle handle;
        final Vec3[] axes=new Vec3[3];
        Visual(Network.Effect e){
            this.e=e;born=tick;
            Random rng=new Random(e.seed());
            for(int i=0;i<axes.length;i++){
                double y=rng.nextDouble()*2-1,a=rng.nextDouble()*Math.PI*2,r=Math.sqrt(1-y*y);
                axes[i]=new Vec3(Math.cos(a)*r,y,Math.sin(a)*r);
            }
        }
    }
    private record Transient(PhotonBridge.Handle handle,long end){}
    private static class DomainVisual {
        Network.Effect appearance;Vec3 center,shrine;long end,start,born;int owner;float yaw,radius;boolean burning,voidFloor,soul;
        DomainVisual(Network.Effect e){
            appearance=e;center=e.position();shrine=center.add(e.direction());end=tick+e.life();owner=e.owner();soul=e.seed()<0;start=Math.abs(e.seed());radius=e.size();voidFloor=false;
            born=tick-Math.max(0,(VoidBossClient.shrine(e.owner())?400:1200)-e.life());
            yaw=(float)Math.toDegrees(Math.atan2(-e.direction().x,-e.direction().z));
        }
    }
    private static class AuraVisual {
        Vec3 position;long end,born,start;int owner;float radius;final Vec3[] axes=new Vec3[9];
        AuraVisual(Network.Effect e){
            position=e.position();radius=e.size();owner=e.owner();end=tick+e.life();born=tick-Math.max(0,400-e.life());start=e.seed();
            Random rng=new Random(start^owner);
            for(int i=0;i<axes.length;i++){double a=rng.nextDouble()*Math.PI*2,y=rng.nextDouble()*2-1,r=Math.sqrt(1-y*y);axes[i]=new Vec3(Math.cos(a)*r,y,Math.sin(a)*r);}
        }
    }
    public static class Registration {
        @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{ItemBlockRenderTypes.setRenderLayer(SoulContent.THREAD.get(),RenderType.cutout());ItemBlockRenderTypes.setRenderLayer(SoulContent.RICE.get(),RenderType.cutout());MahoragaArmorModel.register();});}
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(WHEEL);e.register(CAST);e.register(GUIDE);e.register(ROTATE);e.register(RELATIONS);e.register(FIST);e.register(STREAM);e.register(AMPLIFY);e.register(LIMBS);e.register(LIMB_1);e.register(LIMB_2);e.register(LIMB_3);e.register(LIMB_4);AuthorityClient.setup();TenShadows.receiver=TenShadowsClient::receive;SlashTerrain.receiver=SlashTerrainClient::receive;ObserverPackets.receiver=ObserverFight::receive;ObserverPackets.cursorReceiver=ObserverFight::cursors;Network.aimReceiver=AimAssistClient::receive;Network.testResetReceiver=Client::testReset;WallpaperPackets.receiver=WallpaperClient::receive;Network.motionReceiver=DismantleMotionClient::receive;Network.drawReceiver=FreehandClient::receive;Network.clashReceiver=DomainClashClient::receive;Network.clientReceiver=Client::receive;Network.progressReceiver=p->progress=p;Network.combatReceiver=FinaleClient::state;Network.relationsReceiver=p->{if(Minecraft.getInstance().screen instanceof RelationsScreen screen)screen.receive(p);};Network.controllerReceiver=p->{if(Minecraft.getInstance().screen instanceof VoidControllerScreen screen)screen.receive(p);else if(Minecraft.getInstance().screen instanceof AimOptionsScreen screen)screen.receive(p);};}
    }
    public static boolean wheelHeld(){return ClientInput.held(WHEEL);}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clear();character.CharacterClient.clear();world=null;}
    static void watchRenderers(Minecraft mc){EntityRendererRegistry.ensureOwn(mc);}
    private static void testReset(){DeathStarClient.clear();BloodClient.clear();FinaleClient.clear();TechniqueInputClient.clear();DomainClashClient.clear();DismantleMotionClient.clear();FreehandClient.clear();NetBranchVisuals.clear();SoulClient.clear();CastHands.clear();MeleeComboClient.clear();charging=cleaveHeld=false;}
    private static void clear(){AimAssistClient.clear();FinaleClient.clear();TechniqueInputClient.clear();ExpansionVisuals.clear();ClientInput.clear();DomainClashClient.clear();DismantleMotionClient.clear();FreehandClient.clear();SlashFormVisuals.clear();SlashTerrainClient.clear();BranchVisuals.clear();NetBranchVisuals.clear();Network.clientModpackMode=null;selected=0;wheelPage=0;VoidTechniqueClient.clear();VoidBossClient.clear();GojoFx.clear();CastHands.clear();MeleeComboClient.clear();RuinClient.clear();SoulClient.clear();FistFlameClient.clear();FistAura.clear();character.jogo.JogoFxClient.clear();cleaveHeld=false;VoidVisuals.clear();supreme.clear();FurnaceMaxClient.clear();for(Visual v:visuals)PhotonBridge.stop(v.handle);for(Transient t:transients)PhotonBridge.stop(t.handle);visuals.clear();transients.clear();domains.clear();auras.clear();heldLast=false;castHeld=false;charging=false;generated=0;window=0;shakeUntil=flashUntil=transitionUntil=eyesUntil=0;progress=new Network.Progress(1000,1000,0,false,false,false,false,false,false,false,"",false,false,false,false,1,300,false,0,false,false,0,4,false,false,false,false,DomainOptions.DEFAULT_RADIUS,DomainOptions.DEFAULT_DAMAGE,DomainOptions.DEFAULT_SPEED,true);jumpPress=0;jumpHeld=false;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();if(world!=mc.level){clear();world=mc.level;}watchRenderers(mc);ClientInput.tick();character.CharacterClient.tick();character.jogo.JogoFxClient.tick();TechniqueInputClient.tick();ExpansionVisuals.tick();
        // 失焦、菜单和死亡属于取消；即使单人游戏暂停，也先结束服务端蓄力。
        if((charging||cleaveHeld)&&(mc.player==null||!mc.player.isAlive()||mc.screen!=null||!mc.isWindowActive()||!mc.mouseHandler.isMouseGrabbed())){
            if(mc.player!=null)Network.CHANNEL.sendToServer(new Network.Cast(SkillAction.CANCEL_CHARGE));
            charging=cleaveHeld=false;
        }
        if(mc.level==null||mc.player==null||mc.isPaused())return;if(progress.unlocked()&&!availableSkill(selected))selected=fallbackSkill();
        // 轮盘里刚选中御厨子(5) → 弹出点数分配界面；确定后按释放键正常释放
        if(!AuthorityClient.enabled()&&selected==5&&lastShrineSel!=5&&mc.screen==null&&!shrineDomainOpen()&&availableSkill(5))mc.setScreen(new DomainOptionsScreen());
        lastShrineSel=selected;tick++;tripleJump(mc);DismantleMotionClient.tick();SlashTerrainClient.tick();SlashFormVisuals.tick();FreehandClient.tick(selected==SlashFormRules.FREEHAND&&FreehandClient.keyDown());BranchVisuals.tick();NetBranchVisuals.tick();VoidTechniqueClient.tick();VoidBossClient.tick();FurnaceMaxClient.tick();GojoFx.tick();MahoragaWheelClient.tick();ShinbukeVisuals.tick();SoulClient.tick();FistFlameClient.tick();BlackFlashMarks.clientTick();VoidVisuals.tick();if(charging&&tick-chargeBorn>200)charging=false;
        if(tick-window>=20){window=tick;generated=0;}
        if(ObserverClient.active()||FinaleClient.local()||DomainClashClient.active()||NetBranchVisuals.locked()||DismantleMotionClient.locked()){
            charging=cleaveHeld=false;castHeld=ClientInput.held(CAST);heldLast=wheelHeld();
            while(CAST.consumeClick()){}while(ROTATE.consumeClick()){}while(FIST.consumeClick()){}
        }else{
        boolean down=wheelHeld();
        if(progress.unlocked()&&!character.CharacterClient.toji()&&down&&!heldLast&&mc.screen==null){
            if(!availableSkill(selected))selected=fallbackSkill();
            wheelPage=BloodRules.pageOf(selected);
            character.CharacterClient.openWheel();
        }
        heldLast=down;
        while(RELATIONS.consumeClick())if(mc.screen==null&&mc.isWindowActive()&&mc.mouseHandler.isMouseGrabbed())mc.setScreen(new RelationsScreen());
        while(GUIDE.consumeClick())if(mc.screen==null)mc.player.connection.sendCommand("sukuna guide");
        while(ROTATE.consumeClick())if(mc.screen==null&&progress.nine())Network.CHANNEL.sendToServer(new Network.Cast(SkillAction.ROTATE));
        while(LIMBS.consumeClick())if(mc.screen==null&&character.mahito.LimbScreen.arms()>0)mc.setScreen(new character.mahito.LimbScreen());
        while(LIMB_1.consumeClick())limbUse(0);
        while(LIMB_2.consumeClick())limbUse(1);
        while(LIMB_3.consumeClick())limbUse(2);
        while(LIMB_4.consumeClick())limbUse(3);
        boolean pressed=ClientInput.held(CAST);
        if(charging&&(!progress.unlocked()||!progress.awakened()||mc.screen!=null||!mc.player.isAlive()||selected!=0)){Network.CHANNEL.sendToServer(new Network.Cast(SkillAction.CANCEL_CHARGE));charging=false;}
        if(cleaveHeld&&(!progress.unlocked()||mc.screen!=null||!mc.player.isAlive()||selected!=2)){Network.CHANNEL.sendToServer(new Network.Cast(SkillAction.CANCEL_CHARGE));cleaveHeld=false;}
        while(FIST.consumeClick()){}   // 附拳已搬进轮盘右上角固定格，Del 仅保留键位不再触发
        if(progress.unlocked()&&mc.screen==null){
            // 释放键由 TechniqueInputClient 独立处理；失焦时也能取消服务端按住状态。
            while(STREAM.consumeClick())if(mc.isWindowActive()&&mc.mouseHandler.isMouseGrabbed())Network.CHANNEL.sendToServer(new Network.StreamToggle());
            while(AMPLIFY.consumeClick())if(mc.screen==null&&mc.isWindowActive()&&mc.mouseHandler.isMouseGrabbed())Network.CHANNEL.sendToServer(new Network.Cast(SkillAction.DOMAIN_AMPLIFICATION));
        }
        castHeld=pressed;while(CAST.consumeClick()){}
        }
        for(Iterator<Visual> it=visuals.iterator();it.hasNext();){
            Visual v=it.next();
            if(tick-v.born>=v.e.life()||(v.background&&!domains.containsKey(v.e.owner()))){PhotonBridge.stop(v.handle);it.remove();continue;}
            if(v.e.kind()==3){PhotonBridge.move(v.handle,position(v,0));if((tick-v.born)%3==0)burst(position(v,0),0xFFFF650C,0.7,5,10);}
        }
        for(Iterator<Transient> it=transients.iterator();it.hasNext();){Transient t=it.next();if(tick>=t.end){PhotonBridge.stop(t.handle);it.remove();}}
        auras.values().removeIf(a->tick>=a.end);
        for(AuraVisual a:auras.values()){var p=mc.level.getEntity(a.owner);if(p!=null)a.position=p.position();}
        domains.values().removeIf(d->tick>=d.end);
        for(DomainVisual d:domains.values()){
            // 完整领域内随机生成；不占主动技能或受击切痕名额。
            int budget=FinaleClient.absorbing(d.center)?0:Math.min(140,Math.min(2200-(int)visuals.stream().filter(v->v.background).count(),2800-visuals.size()));
            for(int i=0;i<budget;i++)spawnDomainSlash(d);
            if(tick%8==0&&!VoidBossClient.shrine(d.owner)){
                double a=tick*0.17;
                burst(d.shrine.add(Math.cos(a)*7,1+random.nextDouble()*7,Math.sin(a)*7),0xFFFF273D,0.8,10,20);
                burst(d.center.add(random.nextGaussian()*35,1+random.nextDouble()*5,random.nextGaussian()*35),0x80B9A9AA,2,8,30);
            }
            if(!VoidBossClient.shrine(d.owner)&&(tick-d.born==20||tick-d.born==40))burst(d.shrine.add(0,6,0),0xFFFFCCAA,3,24,22);
        }
    }
    private static long slashCount(){return visuals.stream().filter(v->!v.background&&(v.e.kind()==0||v.e.kind()==12||v.e.kind()==72)).count();}
    private static Vec3 slashAxis(Network.Effect e){
        Vec3 side=new Vec3(-e.direction().z,0,e.direction().x).normalize();
        if(side.lengthSqr()<0.1)side=new Vec3(1,0,0);
        Vec3 up=e.direction().cross(side).normalize();
        double angle=Math.toRadians(e.seed());
        return side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle))).normalize();
    }
    private static void spawnDomainSlash(DomainVisual d){
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        // 相机只负责远距离停画，绝不参与位置、长度或方向的生成。
        if(mc.player.position().distanceToSqr(d.center)>(d.radius+64)*(d.radius+64))return;
        float size=NetBranchRules.domainLength(random.nextDouble());
        double room=d.radius-size/2;if(room<=0)return;
        double angle=random.nextDouble()*Math.PI*2,r=Math.cbrt(random.nextDouble())*room;
        double vertical=random.nextDouble()*2-1,flat=Math.sqrt(1-vertical*vertical);
        Vec3 offset=new Vec3(Math.cos(angle)*r*flat,vertical*r,Math.sin(angle)*r*flat);
        // 部分斩击集中在展开高度，仍以固定领域中心采样，转动准星不会拖动它们。
        if(random.nextInt(3)!=0){double y=(random.nextDouble()-0.5)*24,radial=Math.sqrt(random.nextDouble())*Math.sqrt(Math.max(0,room*room-y*y));offset=new Vec3(Math.cos(angle)*radial,y,Math.sin(angle)*radial);}
        Vec3 pos=d.center.add(offset);
        double directionY=random.nextDouble()*2-1,directionAngle=random.nextDouble()*Math.PI*2,directionFlat=Math.sqrt(1-directionY*directionY);
        Vec3 direction=new Vec3(Math.cos(directionAngle)*directionFlat,directionY,Math.sin(directionAngle)*directionFlat);
        long seed=random.nextInt(360)+(d.soul?2000:d.burning?720:0);
        // 中心到边界预留半条长度，不截短60–120格的完整斩击。
        Visual v=new Visual(new Network.Effect(0,d.owner,pos,direction,size,SlashMotion.LIFE,seed).wallpaper(d.appearance.wallpaperOwner(),d.appearance.desktopSlashes()));
        v.background=true;visuals.add(v);
    }
    private static boolean slashEffect(int kind){return switch(kind){case 0,7,10,11,12,51,59,61,62,64,72,92,93,96,113,115,119,121,122,123,125,126,134,138,139,140,141,142,145,146,147,148,149,170,171,172->true;default->false;};}
    public static void collectSlashes(Vec3 center,double radius,List<Vec3> out){visuals.removeIf(v->{if(!slashEffect(v.e.kind()))return false;Vec3 at=v.e.position();if(at.distanceToSqr(center)>radius*radius)return false;if(out.size()<6000)out.add(at);PhotonBridge.stop(v.handle);return true;});}
    public static void slashShake(){shakeUntil=Math.max(shakeUntil,tick+3);}
    public static void receive(Network.Effect e){
        if(e.kind()==300){character.mahito.TransfigureScreen.open((int)e.size());return;}
        if(e.kind()==305){character.mahito.LimbScreen.setArms((int)e.size());character.mahito.MahitoArmsClient.set(e.owner(),(int)e.size());return;}
        if(e.kind()==303){character.mahito.TransformedScaleClient.put(e.owner(),e.size(),e.life()/1000f);return;}
        if(e.kind()==302){character.mahito.TransfigureModelScreen.open(
            (float)e.position().x,(float)e.position().y,(float)e.position().z);return;}
        if(e.kind()==301){
            int flesh=0xFF8E2334, bloom=0xFFC2415A;
            PhotonFx.spark(e.position(),1.6,0.6,flesh,bloom,"textures/effect/glow.png",true,1,10,28,0.35);
            PhotonFx.ring(e.position(),1.6,0.5,flesh,bloom,20,2,8);
            return;
        }
        if(DeathStarClient.receive(e)||BloodClient.receive(e))return;
        if(Minecraft.getInstance().level==null)return;
        if(e.kind()==SlashTerrain.SCAR){SlashTerrainClient.scar(e);return;}
        if(e.kind()==SlashImpact.FX){SlashImpactClient.receive(e);return;}
        if(e.kind()>=character.jogo.JogoFx.FIRST&&e.kind()<=character.jogo.JogoFx.LAST){character.jogo.JogoFxClient.receive(e);return;}
        if(e.kind()>=ObserverRitual.FX_START&&e.kind()<=ObserverRitual.FX_FIGHT){ObserverClient.receive(e);return;}
        if(e.kind()>=FurnaceMax.FX_CHARGE&&e.kind()<=FurnaceMax.FX_BURN){FurnaceMaxClient.receive(e);return;}
        if((e.kind()>=GojoFx.BLOCK&&e.kind()<=GojoFx.DOMAIN_CLOSE)||e.kind()==GojoFx.BIG_BANG
            ||e.kind()==GojoFx.PURPLE_CHARGE||e.kind()==GojoFx.PURPLE_TAP||e.kind()==GojoFx.PURPLE_TAP_BURST
            ||e.kind()==GojoFx.PURPLE_GATHER_SPIN){GojoFx.receive(e);return;}
        if(e.kind()>=MahoragaVisuals.FIRST&&e.kind()<=MahoragaVisuals.LAST){MahoragaVisuals.receive(e);return;}
        if(e.kind()==ShinbukeVisuals.STRIKE||e.kind()==ShinbukeVisuals.CHARGE){ShinbukeVisuals.receive(e);return;}
        if(e.kind()>=FinaleRules.FX_STATE&&e.kind()<=FinaleRules.FX_ABSORB){FinaleClient.receive(e);return;}
        if(slashEffect(e.kind())&&FinaleClient.absorbing(e.position()))return;
        if(e.kind()==CombatExpansionRules.FX_BLADE_STOP){SlashFormVisuals.stop(e);return;}
        if(e.kind()>=CombatExpansionRules.FX_SLASH&&e.kind()<=CombatExpansionRules.FX_PURPLE){ExpansionVisuals.receive(e);return;}
        if(e.kind()>=SlashFormRules.FLIGHT&&e.kind()<=DismantleMotionRules.RING_FX){SlashFormVisuals.receive(e);return;}
        if(e.kind()>=NetBranchRules.FX_CROSS&&e.kind()<=NetBranchRules.FX_STOP){NetBranchVisuals.receive(e);return;}
        if(e.kind()>=BranchRules.FX_LINE&&e.kind()<=BranchRules.FX_FREEZE){BranchVisuals.receive(e);return;}
        if(e.kind()==144){VoidBossClient.receive(e);return;}
        if(e.kind()>=113&&e.kind()<=121){VoidTechniqueClient.receive(e);return;}
        if(e.kind()>=100&&e.kind()<=112){VoidBossClient.receive(e);return;}
        if(e.kind()==97){RuinClient.receive(e);return;}
        if(e.kind()==2||e.kind()==70||e.kind()==71)CastHands.receive(e);   // 71 = 无量空处的单手结印姿态
        // 手臂动作与炎弓独立恢复，屏幕切割和释放法阵保持关闭。
        if(e.kind()==50||e.kind()==52||e.kind()==53||e.kind()==71||e.kind()==70&&BloodRules.skill((int)e.size()))return;
        if(e.kind()==BlackFlashMarks.FX_MARK){BlackFlashMarks.receive(e);return;}
        if(e.kind()==FistFlameClient.FX){FistFlameClient.receive(e);return;}
        if(e.kind()>=80&&e.kind()<=95||e.kind()>=125&&e.kind()<=127){SoulClient.receive(e);return;}
        if(e.kind()==67){if(e.size()>0)supreme.add(e.owner());else supreme.remove(e.owner());return;}
        if(e.kind()>=61&&e.kind()<=63){VoidVisuals.receive(e);return;}
        var player=Minecraft.getInstance().player;
        if(player!=null&&player.distanceToSqr(e.position())<4096){if(e.kind()==32||e.kind()==36||e.kind()==40||e.kind()==41||e.kind()==20)shakeUntil=tick+14;if(e.kind()==36)transitionUntil=tick+80;if(e.kind()==42&&e.owner()==player.getId()){flashUntil=tick+12;reflected=e.size();}}
        if(e.kind()==44&&player!=null&&e.owner()==player.getId())eyesUntil=tick+30;
        if(e.kind()==58&&player!=null&&e.seed()==player.getId()){flashUntil=tick+10;reflected=0;shakeUntil=tick+10;}
        if(e.kind()==24){var d=domains.get(e.owner());if(d!=null)d.burning=true;return;}
        if(e.kind()==25){var d=domains.get(e.owner());if(d!=null)d.voidFloor=e.size()>0;return;}
        if(e.kind()==21||e.kind()==68)return;
        if(e.kind()==1){
            DomainVisual d=domains.get(e.owner());
            if(d==null||d.start!=Math.abs(e.seed())){
                visuals.removeIf(v->v.background&&v.e.owner()==e.owner());
                d=new DomainVisual(e);domains.put(e.owner(),d);
                if(e.life()>1140){
                    burst(d.shrine.add(0,3,0),0xFFFF1B29,4,40,35);
                    for(int i=0;i<36;i++)spawnDomainSlash(d);
                }
            }else {d.end=tick+e.life();d.appearance=e;}return;
        }
        if(e.kind()==5){VoidBossClient.closeShrine(e.owner());domains.remove(e.owner());visuals.removeIf(v->v.background&&v.e.owner()==e.owner());return;}
        if(e.kind()==8){
            AuraVisual a=auras.get(e.owner());
            if(a==null||a.start!=e.seed())auras.put(e.owner(),new AuraVisual(e));
            else {a.end=tick+e.life();a.position=e.position();}return;
        }
        if(e.kind()==9){auras.remove(e.owner());return;}
        if(e.kind()==7||e.kind()==10||e.kind()==11){
            // 每个目标最多保留两次受击，密集领域伤害始终能刷新最新斩痕。
            List<Visual> hits=visuals.stream().filter(v->(v.e.kind()==7||v.e.kind()==10||v.e.kind()==11)&&v.e.owner()==e.owner()).toList();
            if(hits.size()>=2)visuals.remove(hits.get(0));
            if(visuals.stream().filter(v->v.e.kind()==7||v.e.kind()==10||v.e.kind()==11).count()>=256){
                Visual old=visuals.stream().filter(v->v.e.kind()==7||v.e.kind()==10||v.e.kind()==11).findFirst().orElse(null);visuals.remove(old);
            }
        }
        if(e.kind()==0||e.kind()==12||e.kind()==72){if(slashCount()>=200||generated>=200)return;generated++;}
        if(e.kind()==4){for(Iterator<Visual> it=visuals.iterator();it.hasNext();){Visual v=it.next();if(v.e.kind()==3&&v.e.owner()==e.owner()){PhotonBridge.stop(v.handle);it.remove();}}burst(e.position(),0xFFFF5B0B,e.size()/3,45,28);}
        Visual v=new Visual(e);
        if(e.kind()==3)v.handle=PhotonBridge.spawn(e.position(),0xFFFF6F00,1.2,6,e.life());
        if(visuals.size()<2800)visuals.add(v);else PhotonBridge.stop(v.handle);
    }
    private static void burst(Vec3 p,int color,double size,int count,int life){if(transients.size()>=96)return;var handle=PhotonBridge.spawn(p,color,size,count,life);if(handle!=null)transients.add(new Transient(handle,tick+life+2));}
    private static Vec3 position(Visual v,float partial){double age=tick-v.born+partial;return v.e.position().add(v.e.direction().scale(age*(v.e.kind()==3?3:2)));}
    private static boolean inside(){Minecraft mc=Minecraft.getInstance();if(mc.player==null)return false;return domains.values().stream().anyMatch(d->mc.player.position().distanceToSqr(d.center)<d.radius*d.radius);}
    static boolean voidPulse(int owner,float partial){DomainVisual d=domains.get(owner);return d!=null&&(tick-d.born+partial)%160<8;}
    private static boolean insideVoid(){var mc=Minecraft.getInstance();return mc.player!=null&&domains.values().stream().anyMatch(d->VoidBossClient.shrine(d.owner)&&mc.player.position().distanceToSqr(d.center)<d.radius*d.radius);}
    private static boolean insideBlueVoid(){var mc=Minecraft.getInstance();return mc.player!=null&&domains.values().stream().anyMatch(d->VoidBossClient.blue(d.owner)&&mc.player.position().distanceToSqr(d.center)<d.radius*d.radius);}
    /** 给其它特效系统用的镜头震动/闪光入口（最大功率·开 打出去时要"光污染"）。 */
    public static void shake(int ticks){shakeUntil=Math.max(shakeUntil,tick+ticks);}
    public static void flash(int ticks){flashUntil=Math.max(flashUntil,tick+ticks);}
    /** 蓄力期间的视角变化（最大功率·开：FOV 放大 2 倍）。 */
    @SubscribeEvent public static void computeFov(net.minecraftforge.client.event.ViewportEvent.ComputeFov e){FurnaceMaxClient.fov(e);}
    private static boolean bossDark(){var mc=Minecraft.getInstance();return mc.player!=null&&mc.player.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS)&&visuals.stream().anyMatch(v->v.e.kind()==38&&mc.player.distanceToSqr(v.e.position())<2304);}
    @SubscribeEvent public static void fog(ViewportEvent.ComputeFogColor e){if(ObserverClient.white()){e.setRed(1);e.setGreen(1);e.setBlue(1);return;}if(bossDark()){e.setRed(0.028f);e.setGreen(0.008f);e.setBlue(0.045f);}else if(insideBlueVoid()){e.setRed(0.02f);e.setGreen(0.10f);e.setBlue(0.24f);}else if(insideVoid()){e.setRed(0.11f);e.setGreen(0.045f);e.setBlue(0.19f);}else if(inside()){e.setRed(0.40f);e.setGreen(0.018f);e.setBlue(0.045f);}FurnaceMaxClient.fog(e);}
    @SubscribeEvent public static void fogDistance(ViewportEvent.RenderFog e){if(ObserverClient.white()&&e.getType()==net.minecraft.world.level.material.FogType.NONE){e.setNearPlaneDistance(0.2f);e.setFarPlaneDistance(64);e.setCanceled(true);return;}if(bossDark()&&e.getType()==net.minecraft.world.level.material.FogType.NONE){e.setNearPlaneDistance(1);e.setFarPlaneDistance(5);e.setCanceled(true);}}
    @SubscribeEvent public static void screenSeverance(RenderGuiEvent.Pre e){
        Minecraft mc=Minecraft.getInstance();
        if(ObserverClient.active())return;
        FurnaceMaxClient.overlay(e.getGuiGraphics());
        boolean heat=mc.player!=null&&domains.values().stream().anyMatch(d->d.burning&&mc.player.position().distanceToSqr(d.center)<d.radius*d.radius);
        if(heat)FistAura.heat(e.getGuiGraphics(),tick+e.getPartialTick());
        if(!heat)FistAura.clear();
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.options.hideGui||ObserverClient.active()||(mc.screen instanceof SkillWheel||mc.screen instanceof character.CharacterWheel||mc.screen instanceof character.CharacterSelectionScreen||mc.screen instanceof WallOptionsScreen||mc.screen instanceof DomainOptionsScreen||mc.screen instanceof RelationsScreen))return;
        int h=mc.getWindow().getGuiScaledHeight(),w=mc.getWindow().getGuiScaledWidth();var g=e.getGuiGraphics();if(character.CharacterClient.hud(g))return;FreehandClient.hud(g);
        if(!progress.unlocked()){
            boolean holding=mc.player.getMainHandItem().is(ProgressionContent.MEMORY.get())||mc.player.getOffhandItem().is(ProgressionContent.MEMORY.get());
            boolean ice=mc.player.getMainHandItem().is(IceContent.MEMORY.get())||mc.player.getOffhandItem().is(IceContent.MEMORY.get());
            if(holding||ice)g.drawString(mc.font,ice?"吃下里梅记忆解锁冰凝咒法":"右键咒术选择书选择角色",10,h-35,0xFFE7C8FF);
            return;
        }
        var lines=mc.font.split(net.minecraft.network.chat.Component.literal(progress.guide()+" · "+GUIDE.getTranslatedKeyMessage().getString()+" 查看详情"),Math.min(330,w-20));
        int ly=12;for(var line:lines){g.drawString(mc.font,line,10,ly,0xFFE6C7F1);ly+=11;}
        if(progress.nine())g.drawString(mc.font,progress.rotation()?"九相轮转进行中 · 每秒20次斩击判定":"九相轮转 · "+ROTATE.getTranslatedKeyMessage().getString()+" 开启15秒",12,h-128,0xFFD48AFF);
        if(charging&&tick-chargeBorn>=Skills.CHARGE_DELAY){int elapsed=(int)(tick-chargeBorn)-Skills.CHARGE_DELAY;g.fill(w/2-70,h/2+28,w/2+70,h/2+34,0xB022122D);g.fill(w/2-70,h/2+28,w/2-70+(int)(140*Math.min(1,elapsed/(float)Skills.CHARGE_TIME)),h/2+34,elapsed>=Skills.CHARGE_TIME?0xFFFFDDEE:0xFFAF397B);g.drawCenteredString(mc.font,elapsed>=Skills.CHARGE_TIME?"空间斩就绪 · 松开释放 · "+energyText(2000)+"咒力":"空间正在崩裂……",w/2,h/2+40,0xFFFFD7EE);}

        g.fill(7,h-49,215,h-11,0xB0181019);g.fill(7,h-49,10,h-11,0xFFBD283D);
        g.drawString(mc.font,skillName(selected),16,h-42,0xFFF3D3CB);
        g.drawString(mc.font,"长按 "+WHEEL.getTranslatedKeyMessage().getString()+" 选择 · "+CAST.getTranslatedKeyMessage().getString()+" 释放",16,h-26,0xFFB8A4AE);
        g.fill(7,h-102,215,h-78,0xCC140E20);
        g.fill(10,h-84,212,h-80,0xFF342639);
        g.fill(10,h-84,10+(int)(202*progress.energy()/Math.max(1,progress.maximum())),h-80,progress.awakened()?0xFFE23455:0xFFAF66EB);
        g.drawString(mc.font,(AuthorityClient.enabled()?"军团咒力池 ":"咒力 ")+(progress.maximum()>=999999.0?"∞":(int)progress.energy()+" / "+(int)progress.maximum())+(progress.sukuna()?"  手指 "+progress.fingers()+"/20":"  冰鱼 "+progress.iceFish()),12,h-97,0xFFEED5FF);
        if(voidStage())g.drawString(mc.font,progress.pureLandVictory()?"蓝色虚空术式 · 生命+200 · 普攻+20 · 术式+200":"虚空术式 · 解/捌造裂隙 → 灶点燃 → 开引爆",12,h-115,progress.pureLandVictory()?0xFF53CFFF:0xFFBE7CEF);
        else if(progress.awakened())g.drawString(mc.font,"肉体融合 · 解长按连发 · 世界斩在分支中选择",12,h-115,0xFFFF5774);
        if(progress.amplification())g.drawString(mc.font,"领域展延 · 每秒 "+BalanceRules.number(DomainAmplification.COST_PER_SECOND)+" 咒力 · 术式穿透无下限 · "+AMPLIFY.getTranslatedKeyMessage().getString()+" 关闭",12,h-160,0xFF9FE8FF);
        if(progress.fist())g.drawString(mc.font,"咒力附拳 · 黑红咒力与击退 · 15根手指自动触发解",12,h-130,0xFFE04DCE);
        if(progress.fingers()>=15)g.drawString(mc.font,"反转术式 · "+WHEEL.getTranslatedKeyMessage().getString()+" 轮盘左侧 · 松手治疗",12,h-145,0xFF80FFD4);
        if(progress.burnout())g.drawCenteredString(mc.font,progress.clashPenaltyTicks()>0?"对拼熔断 "+((progress.clashPenaltyTicks()+19)/20)+"秒 · 治疗55%咒力 · 不可修复":"术式熔断：术式伤害 -30% · 只能反转/修复",w/2,h-62,0xFFFF4F65);
        else if(progress.disorder())g.drawCenteredString(mc.font,"咒力紊乱：术式伤害 -20%",w/2,h-62,0xFFECC064);
        if(progress.siphon())for(int i=0;i<12;i++){int c=((55-i*4)<<24)|0x641195;g.fill(i*5,0,i*5+5,h,c);g.fill(w-i*5-5,0,w-i*5,h,c);g.fill(0,i*4,w,i*4+4,c);g.fill(0,h-i*4-4,w,h-i*4,c);}
        if(eyesUntil>tick)for(int i=0;i<4;i++){int x=i%2==0?35:w-35,y=h/2-20+(i/2)*22;g.fill(x-15,y-3,x+15,y+3,0xAADD1744);g.fill(x-2,y-5,x+2,y+5,0xEE150610);}
        if(flashUntil>tick){g.fill(0,0,w,h,0x55990022);if(reflected>0)g.drawCenteredString(mc.font,"反噬 -"+String.format(Locale.ROOT,"%.1f",reflected),w/2,h/2+30,0xFFFF3434);}
        if(transitionUntil>tick){double t=80-(transitionUntil-tick);for(int i=0;i<20;i++){double f=t<40?t/40:(80-t)/40;int x=w/2-90+i*9+(int)(Math.sin(i*7)*f*55),y=38+(int)(Math.cos(i*3)*f*28);g.fill(x,y,x+7,y+5,t<40?0xFFE0BE67:0xFFAA1539);}g.drawCenteredString(mc.font,"即身佛 · 开眼转生",w/2,70,0xFFE099C5);}
        AuraVisual aura=auras.get(mc.player.getId());
        if(aura!=null){
            g.fill(7,h-72,215,h-53,0xD0280811);
            g.drawString(mc.font,"捌已开启 · 半径"+(int)aura.radius+"格 · "+((aura.end-tick+19)/20)+"秒",16,h-67,0xFFFFC6CC);
            g.fill(7,h-55,7+(int)(208*Math.min(1,(aura.end-tick)/400.0)),h-53,0xFFFF5065);
        }
        if(inside()){
            boolean blue=insideBlueVoid();
            g.drawCenteredString(mc.font,blue?"虚空御厨子 · 净土觉醒":skillName(5).replace("领域展开 · ",""),w/2,18,blue?0xFF53CFFF:0xFFFF6464);
            long age=domains.values().stream().filter(d->mc.player.position().distanceToSqr(d.center)<d.radius*d.radius).mapToLong(d->tick-d.born).min().orElse(100);
            if(age<60){int alpha=(int)(55*(1-age/60.0));g.fill(0,0,w,h,(alpha<<24)|(blue?0x073880:0x800718));g.drawCenteredString(mc.font,"领 域 展 开",w/2,h/3,blue?0xFFBAF2FF:0xFFFFE4DB);}
            for(int i=0;i<5;i++){int color=((24-i*4)<<24)|(blue?0x065CA0:0xA00623);g.fill(i*3,0,i*3+3,h,color);g.fill(w-i*3-3,0,w-i*3,h,color);}
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        Minecraft mc=Minecraft.getInstance();if(mc.level==null)return;
        if(e.getStage()==RenderLevelStageEvent.Stage.AFTER_SKY){if(ObserverClient.active()){ObserverClient.whiteSky(e);return;}if(inside())sky(e);return;}
        // 盖棺铁围山的球壳画在粒子之前：否则它写深度会把领域内的火柱/火花全部盖住。
        // A 方案：盖棺铁围山用真实方块外壳，不再画渲染球壳
        // if(e.getStage()==RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)character.jogo.JogoDomainRender.render(e);
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        PoseStack ps=e.getPoseStack();Vec3 cam=e.getCamera().getPosition();ps.pushPose();ps.translate(-cam.x,-cam.y,-cam.z);
        MultiBufferSource.BufferSource source=mc.renderBuffers().bufferSource();
        for(DomainVisual d:domains.values()){
            double rise=Math.min(1,(tick-d.born+e.getPartialTick())/35.0);
            ps.pushPose();ps.translate(d.shrine.x,d.shrine.y+0.1-(1-rise)*5,d.shrine.z);ps.mulPose(Axis.YP.rotationDegrees(d.yaw));if(VoidBossClient.shrine(d.owner))VoidBossClient.renderShrine(ps,source,(float)(tick-d.born+e.getPartialTick()),d.owner);else ShrineModel.render(ps,source,d.burning,d.soul,(float)(tick-d.born+e.getPartialTick()));ps.popPose();
        }source.endBatch();
        RenderSystem.enableDepthTest();RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.depthMask(false);RenderSystem.setShader(GameRenderer::getPositionColorShader);RenderSystem.setShaderColor(1,1,1,1);
        BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        WallpaperSlash.begin();
        SlashFormVisuals.draw(b,ps,cam,e.getPartialTick());SlashTerrainClient.draw(b,ps,cam,e.getPartialTick());
        ExpansionVisuals.draw(b,ps,cam,e.getPartialTick());
        FinaleClient.draw(b,ps,cam,e.getPartialTick());
        VoidVisuals.render(b,ps,cam,e.getPartialTick());
        VoidTechniqueClient.draw(b,ps,cam,e.getPartialTick());
        SoulClient.draw(b,ps,cam,e.getPartialTick());
        NetBranchVisuals.draw(b,ps,cam,e.getPartialTick());
        BranchVisuals.draw(b,ps,cam,e.getPartialTick());
        BeyondRiftClient.draw(b,ps,cam,e.getPartialTick());
        // 背景先画、低透明度、不写入深度；方块与神龛已写入的深度会正常遮住斩击。
        for(Visual v:visuals)if(v.background)drawSlash(b,ps,v,cam,e.getPartialTick());
        for(DomainVisual d:domains.values())domain(b,ps,d,cam,e.getPartialTick());
        for(AuraVisual a:auras.values())aura(b,ps,a,cam,e.getPartialTick());
        for(Visual v:visuals){if(v.background)continue;SlashAppearance.use(v.e);float age=tick-v.born+e.getPartialTick();SlashAppearance.age(age);int kind=v.e.kind();
            if(v.e.size()==SlashReach.INFINITE&&(kind==96||kind==72||kind==123)){drawInfiniteCut(b,ps,v.e,cam,age);continue;}
            if(kind==96){Vec3 axis=slashAxis(v.e);SlashStyle.draw(b,ps,v.e.position().subtract(axis.scale(v.e.size()/2)),v.e.position().add(axis.scale(v.e.size()/2)),.12,Math.max(0,1-age/v.e.life()),cam);}
            if(kind==0||kind==12||kind==72||kind==123)drawSlash(b,ps,v,cam,e.getPartialTick());
             if(kind==122)VoidTechniqueClient.drawSwordQi(b,ps,v.e,cam,age);
            if(kind==265)drawRope(b,ps,v.e,cam,age);
            if(kind>=20)BattleVisuals.draw(b,ps,v.e,cam,age);
            if(kind==7||kind==10||kind==11)drawHit(b,ps,v,cam,e.getPartialTick());
            if(kind==2||kind==70&&v.e.size()==3){var p=mc.level.getEntity(v.e.owner());if(p!=null){
                Vec3 center=p.getEyePosition().add(p.getLookAngle().scale(1.1)).add(new Vec3(-p.getLookAngle().z,0,p.getLookAngle().x).normalize().scale(-0.55));
                bow(b,ps,center,p.getLookAngle(),age/v.e.life(),cam,kind==70);
                float charge=Math.min(1,age/30);
                for(int j=0;j<8;j++){double a=j*Math.PI/4+age*0.18,r=0.35+(1-charge)*0.7;Vec3 q=center.add(Math.cos(a)*r,Math.sin(a)*r,0);ribbon(b,ps,q,q.add(0,0.15+charge*0.3,0),0.035,0xCFFFF0B0,cam);}
            }}
            if(kind==3){
                Vec3 p=position(v,e.getPartialTick()),dir=v.e.direction();
                ribbon(b,ps,p.subtract(dir.scale(11)),p,0.55,0x70FF3800,cam);
                ribbon(b,ps,p.subtract(dir.scale(8)),p,0.30,0xEFFF3800,cam);ribbon(b,ps,p.subtract(dir.scale(6)),p,0.09,0xFFFFFFC0,cam);
                Vec3 side=new Vec3(-dir.z,0,dir.x).normalize();if(side.lengthSqr()<0.1)side=new Vec3(1,0,0);Vec3 up=dir.cross(side).normalize();
                for(int j=0;j<12;j++){double a=age*0.7+j*0.8;Vec3 q=p.subtract(dir.scale(j*0.7)).add(side.scale(Math.cos(a)*0.4)).add(up.scale(Math.sin(a)*0.4));ribbon(b,ps,q,q.subtract(dir.scale(0.7)),0.06,0xDFFFBE45,cam);}
            }
            if(kind==4){
                float f=Math.min(1,age/15),radius=v.e.size()*f,fade=Math.max(0,1-age/30);
                for(int j=0;j<10;j++)ring(b,ps,v.e.position().add(0,j*radius/11,0),Math.sqrt(Math.max(0,radius*radius-j*j*radius*radius/121)),0.45,((int)(190*fade)<<24)|0xFF5108,cam);
                for(int j=0;j<3;j++)ring(b,ps,v.e.position().add(0,0.15+j*0.12,0),radius*(1.2+j*0.2),0.4-j*0.08,((int)(220*fade)<<24)|0xFFE3B0,cam);
                for(int j=0;j<20;j++){double a=j*Math.PI/10+v.e.seed()%100;Vec3 dir=new Vec3(Math.cos(a),0.3+(j%4)*0.15,Math.sin(a)).normalize();Vec3 p=v.e.position().add(dir.scale(radius));ribbon(b,ps,p,p.add(dir.scale(1.5+f*3)),0.07,((int)(230*fade)<<24)|0xFFF4BC,cam);}
            }
        }
        BufferUploader.drawWithShader(b.end());WallpaperSlash.end(ps);
        // 桌面是在 end() 里最后 flush 的：红色乱码条必须等它画完再另起一批，才能压在桌面之上。
        BeyondRiftClient.overlay(ps,cam,e.getPartialTick());
        RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();ps.popPose();
    }
    /** 无限线按相机可见区段绘制；长度不进入伤害判定，也不发送无穷浮点数。 */
    public static void drawInfiniteCut(BufferBuilder b,PoseStack ps,Network.Effect e,Vec3 cam,float age){
        SlashAppearance.use(e);SlashAppearance.age(age);Vec3 axis=SlashReach.axis(e.direction(),e.seed()),center=e.position().add(axis.scale(cam.subtract(e.position()).dot(axis)));
        double half=Math.max(128,Minecraft.getInstance().options.renderDistance().get()*24.0);
        float fade=Math.min(1,Math.max(0,(e.life()-age)/4));
        SlashStyle.draw(b,ps,center.subtract(axis.scale(half)),center.add(axis.scale(half)),.3,fade,cam);
    }
    private static void drawSlash(BufferBuilder b,PoseStack ps,Visual v,Vec3 cam,float partial){
        SlashAppearance.use(v.e);if(v.background&&VoidBossClient.shrine(v.e.owner())){float age=tick-v.born+partial;SlashAppearance.age(age);VoidTechniqueClient.cut(b,ps,v.e.position(),slashAxis(v.e),v.e.size()*SlashMotion.progress(age),Math.max(0,1-age/v.e.life()),cam,VoidBossClient.blue(v.e.owner()));return;}
        redCut=v.e.seed()>=720&&v.e.seed()<2000;soulCut=v.e.seed()>=2000;
        float age=tick-v.born+partial,fade=Math.min(1,Math.max(0,(v.e.life()-age)/4f));SlashAppearance.age(age);
        Vec3 p=v.e.kind()==12||v.e.kind()==72||v.e.kind()==123||v.background?v.e.position():position(v,partial),axis=slashAxis(v.e);
        if(v.e.kind()==123){
            Vec3 near=cam.subtract(p).normalize().scale(0.08);p=p.add(near);
            SlashStyle.draw(b,ps,p.subtract(axis.scale(v.e.size()/2)),p.add(axis.scale(v.e.size()/2)),.16,fade,cam);
            redCut=false;soulCut=false;return;
        }
        if(v.e.kind()==12||v.e.kind()==72||v.background){
            float sweep=SlashMotion.progress(age);
            float trailFade=1-SlashMotion.recovery(age);
            if(v.background)fade=0.95f*Math.max(0,1-age/v.e.life());
            Vec3 start=p.subtract(axis.scale(v.e.size()*0.5)),tip=start.add(axis.scale(v.e.size()*sweep));
            float length=v.e.size()*sweep;
            if(length>0.001){
                if(v.e.seed()>=1440&&!v.background&&!soulCut)VoidVisuals.needle(b,ps,start,tip,Math.min(0.28,Math.max(0.10,v.e.size()*0.012)),fade*trailFade,cam);
                else if(soulCut)coloredCut(b,ps,start.lerp(tip,0.5),axis,length,fade*trailFade,cam,false);else cut(b,ps,start.lerp(tip,0.5),axis,length,fade*trailFade,cam);
                if(sweep<1)SlashStyle.detail(b,ps,tip.subtract(axis.scale(Math.min(length,0.6))),tip,0.07,0xFFFFF0D8,cam);
            }
            redCut=false;soulCut=false;return;
        }
        cut(b,ps,p.subtract(v.e.direction().scale(0.7)),axis,v.e.size()*0.94f,fade*0.18f,cam);
        if(v.e.seed()>=1440&&!v.background&&!soulCut){VoidVisuals.needle(b,ps,p.subtract(axis.scale(v.e.size()/2)),p.add(axis.scale(v.e.size()/2)),Math.min(0.28,Math.max(0.10,v.e.size()*0.012)),fade,cam);for(int i=-1;i<=1;i+=2)SlashStyle.detail(b,ps,p.add(axis.scale(v.e.size()*0.25*i)),p.add(axis.scale(v.e.size()*0.38*i)).subtract(v.e.direction().scale(3)),0.08,((int)(230*fade)<<24)|0xFFD777,cam);}else {if(soulCut){soulCut=true;coloredCut(b,ps,p,axis,v.e.size(),fade,cam,false);}else cut(b,ps,p,axis,v.e.size(),fade,cam);}redCut=false;soulCut=false;
    }
    private static void drawHit(BufferBuilder b,PoseStack ps,Visual v,Vec3 cam,float partial){
        SlashAppearance.use(v.e);redCut=v.e.kind()==11;
        float age=tick-v.born+partial,fade=Math.max(0,1-age/v.e.life());SlashAppearance.age(age);
        var level=Minecraft.getInstance().level;
        var target=level==null?null:level.getEntity(v.e.owner());
        Vec3 center=target==null?v.e.position():target.getPosition(partial).add(v.e.direction());
        Vec3 toward=cam.subtract(center).normalize();
        // 把切痕贴到朝镜头的一侧，仍使用深度测试，不会透过墙体。
        double surface=0.25;
        if(target!=null){
            double halfWidth=target.getBbWidth()*0.5,halfHeight=target.getBbHeight()*0.5;
            surface=Math.min(halfWidth/Math.max(0.001,Math.max(Math.abs(toward.x),Math.abs(toward.z))),halfHeight/Math.max(0.001,Math.abs(toward.y)));
        }
        Vec3 p=center.add(toward.scale(surface+0.035));
        for(int i=0;i<3;i++){
            // 随机三维方向投影到受击面，每次命中固定角度，避免逐帧抖动。
            Vec3 axis=v.axes[i].subtract(toward.scale(v.axes[i].dot(toward))).normalize();
            if(axis.lengthSqr()<0.01)continue;
            float length=v.e.size()*(i==0?1:0.65f)*(0.8f+0.2f*Math.min(1,age/2));
            float sweep=SlashMotion.progress(age);
            Vec3 start=p.add(0,(i-1)*0.13,0).subtract(axis.scale(length*0.5)),tip=start.add(axis.scale(length*sweep));
            if(soulCut)coloredCut(b,ps,start.lerp(tip,0.5),axis,length*sweep,fade,cam,false);else cut(b,ps,start.lerp(tip,0.5),axis,length*sweep,fade,cam);
            if(sweep<1)SlashStyle.detail(b,ps,tip.subtract(axis.scale(0.15)),tip,0.028,((int)(fade*230)<<24)|0xFFFFFF,cam);
        }
        redCut=false;
    }
    private static void sky(RenderLevelStageEvent e){
        PoseStack ps=e.getPoseStack();ps.pushPose();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.depthMask(false);RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        boolean voidSky=insideVoid(),blueSky=insideBlueVoid();
        double r=180,y=100;quad(b,ps,new Vec3(-r,y,-r),new Vec3(r,y,-r),new Vec3(r,y,r),new Vec3(-r,y,r),blueSky?0xEE03152D:voidSky?0xEE180B29:0xDE850B1A);
        for(int i=0;i<32;i++){double a=i*Math.PI/16,z=(i+1)*Math.PI/16;quad(b,ps,new Vec3(Math.cos(a)*r,-40,Math.sin(a)*r),new Vec3(Math.cos(z)*r,-40,Math.sin(z)*r),new Vec3(Math.cos(z)*r,y,Math.sin(z)*r),new Vec3(Math.cos(a)*r,y,Math.sin(a)*r),blueSky?0xEF072A4B:voidSky?0xEF220D34:0xCF690515);}
        Vec3 eye=Vec3.ZERO;
        for(int j=0;j<5;j++)ring(b,ps,new Vec3(0,96-j*2,0),25+j*25,0.5,blueSky?0x652699FF:voidSky?0x657A349D:0x65FF6172,eye);
        for(int i=0;i<24;i++){
            double a=i*Math.PI/12-(tick+e.getPartialTick())*0.002;
            Vec3 p=new Vec3(Math.cos(a)*75,91,Math.sin(a)*75),q=new Vec3(Math.cos(a+0.05)*84,91,Math.sin(a+0.05)*84);
            ribbon(b,ps,p,q,0.3,blueSky?0x8066CFFF:voidSky?0x808D51BC:0x80FFC3BE,eye);
        }
        for(int i=0;i<18;i++){
            double a=i*Math.PI/9+(tick+e.getPartialTick())*0.0015;
            Vec3 p=new Vec3(Math.cos(a)*42,94,Math.sin(a)*42),q=new Vec3(Math.cos(a+0.07)*160,94,Math.sin(a+0.07)*160);
            ribbon(b,ps,p,q,0.16,blueSky?0x6530B6FF:voidSky?0x657637A9:0x65FFB7B7,eye);
        }
        BufferUploader.drawWithShader(b.end());
        // ---- 内层天空盒（用户给的无量空处云图）：套在原有星河天空盒**里面**的一层 ----
        // 这张图是"黑底(71%)+白云(23%)+蓝雾"，黑底是不透明黑，直接当半透明天空盒会糊成一块黑，
        // 所以走**加法混合**：黑加 0 不改变画面、白云与蓝雾自然发光，正好是"里层云海"的观感。
        if(voidSky){
            RenderSystem.setShader(GameRenderer::getPositionTexShader);RenderSystem.setShaderTexture(0,VOID_SKY_TEXTURE);
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,GlStateManager.DestFactor.ONE);   // 加法
            BufferBuilder s2=Tesselator.getInstance().getBuilder();
            s2.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            float t=(float)((tick+e.getPartialTick())*0.0006),pulse=(float)(0.72+0.28*Math.sin((tick+e.getPartialTick())*0.012));
            int col=((int)(140*pulse)<<24)|0xFFFFFF;
            double ir=118,iy0=-25,iy1=70;int sides=48;
            for(int i=0;i<sides;i++){                                  // 内层筒壁：UV 横向叠 3 次 + 随时间漂移
                double a0=i*Math.PI*2/sides,a1=(i+1)*Math.PI*2/sides;
                float u0=(float)(i*3.0/sides)+t,u1=(float)((i+1)*3.0/sides)+t;
                s2.vertex((float)(Math.cos(a0)*ir),(float)iy0,(float)(Math.sin(a0)*ir)).uv(u0,0f).color(col).endVertex();
                s2.vertex((float)(Math.cos(a1)*ir),(float)iy0,(float)(Math.sin(a1)*ir)).uv(u1,0f).color(col).endVertex();
                s2.vertex((float)(Math.cos(a1)*ir),(float)iy1,(float)(Math.sin(a1)*ir)).uv(u1,1f).color(col).endVertex();
                s2.vertex((float)(Math.cos(a0)*ir),(float)iy1,(float)(Math.sin(a0)*ir)).uv(u0,1f).color(col).endVertex();
            }
            for(int i=0;i<sides;i++){                                  // 内层顶盖（云海顶）：UV 按 xz 展开并叠 2 次
                double a0=i*Math.PI*2/sides,a1=(i+1)*Math.PI*2/sides;
                double x0=Math.cos(a0)*ir,z0=Math.sin(a0)*ir,x1=Math.cos(a1)*ir,z1=Math.sin(a1)*ir;
                s2.vertex(0f,(float)iy1,0f).uv(0.5f+t*0.3f,0.5f).color(col).endVertex();
                s2.vertex((float)x0,(float)iy1,(float)z0).uv((float)(0.5+x0/ir*0.5+t*0.3f),(float)(0.5+z0/ir*0.5)).color(col).endVertex();
                s2.vertex((float)x1,(float)iy1,(float)z1).uv((float)(0.5+x1/ir*0.5+t*0.3f),(float)(0.5+z1/ir*0.5)).color(col).endVertex();
                s2.vertex((float)x0,(float)iy1,(float)z0).uv((float)(0.5+x0/ir*0.5+t*0.3f),(float)(0.5+z0/ir*0.5)).color(col).endVertex();
            }
            BufferUploader.drawWithShader(s2.end());
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();ps.popPose();
    }
    /** 用户提供的无量空处云图（`assets/sukuna/textures/effect/unlimited_void_sky.png`）：黑底不透明，故用加法层。 */
    private static final ResourceLocation VOID_SKY_TEXTURE=new ResourceLocation("sukuna","textures/effect/unlimited_void_sky.png");
    private static void aura(BufferBuilder b,PoseStack ps,AuraVisual a,Vec3 cam,float partial){
        double age=tick-a.born+partial,fade=Math.min(1,(a.end-tick-partial)/15.0),pulse=0.5+0.5*Math.sin(age*0.22);
        Vec3 c=a.position.add(0,0.12,0);
        int color=supreme.contains(a.owner)?0xB5289B:0xFF334E;
        ring(b,ps,c,a.radius,0.10,((int)(fade*(150+pulse*75))<<24)|color,cam);
        ring(b,ps,c.add(0,0.025,0),a.radius*0.99,0.025,((int)(fade*180)<<24)|(supreme.contains(a.owner)?0xFFE09B:0xFFFFFF),cam);
    }
    private static void domain(BufferBuilder b,PoseStack ps,DomainVisual d,Vec3 cam,float partial){
        SlashAppearance.use(d.appearance);double age=tick-d.born+partial,fade=Math.min(1,Math.max(0,(d.end-tick-partial)/30.0));
        double expand=Math.min(1,age/55.0),pulse=0.5+0.5*Math.sin(age*0.075);
        if(VoidBossClient.shrine(d.owner)){VoidBossClient.domain(b,ps,d.center,d.shrine,cam,(float)age,(float)fade,d.owner);return;}
        if(d.soul){SoulClient.soulDomain(b,ps,d.shrine,d.center,cam,(float)age,(float)fade);for(int i=0;i<8;i++){double r=(i==7?d.radius:(12+i*25)*d.radius/200)*expand;ring(b,ps,d.center.add(0,0.12+i*0.004,0),r,i==7?1.2:0.28,((int)((70+pulse*45)*fade)<<24)|0x35BFFF,cam);}return;}
        if(d.burning)SoulClient.burning(b,ps,d.shrine,d.center,cam,(float)age,(float)fade);
        if(d.voidFloor){
            int alpha=(int)(Math.min(150,55+pulse*55)*fade);
            ring(b,ps,d.center.add(0,0.09,0),d.radius*expand,0.42,(alpha<<24)|0x18051F,cam);
            for(int i=0;i<48;i++){double a=i*Math.PI/24+age*0.008,r=d.radius*expand*(0.18+0.8*((i%7)/6.0));Vec3 p=d.center.add(Math.cos(a)*r,0.1,Math.sin(a)*r);ribbon(b,ps,p,p.add(Math.cos(a+0.24)*d.radius*0.08,0.03,Math.sin(a+0.24)*d.radius*0.08),0.08,(alpha<<24)|0x6B164A,cam);}
        }
        if(supreme.contains(d.owner)){for(int j=0;j<9;j++){double radius=(12+j*13)*expand;ring(b,ps,d.center.add(0,0.12+j*0.015,0),radius,0.07,((int)(110*fade)<<24)|(j%2==0?0xE4BD68:0xBC268D),cam);double angle=j*Math.PI*2/9+age*0.008;Vec3 p=d.shrine.add(Math.cos(angle)*18,14,Math.sin(angle)*18);VoidVisuals.needle(b,ps,p,p.add(0,20+j,0),0.4,(float)fade,cam);ring(b,ps,d.shrine.add(0,15+j*2,0),12+j,0.11,((int)(145*fade)<<24)|0xD59E53,cam);}for(int i=0;i<18;i++){double angle=i*Math.PI/9+age*0.01;Vec3 p=d.shrine.add(Math.cos(angle)*24,18,Math.sin(angle)*24);ribbon(b,ps,p,p.add(0,25,0),0.15,((int)(100*fade)<<24)|0xC02A90,cam);}return;}
        for(int i=0;i<8;i++){
            double r=(i==7?d.radius:(12+i*25)*d.radius/200)*expand;int alpha=(int)((i==7?55:90+pulse*60)*fade);
            ring(b,ps,d.center.add(0,0.12+i*0.004,0),r,i==7?1.2:0.28,(alpha<<24)|0xF23248,cam);
        }
        for(int i=0;i<3;i++){
            double phase=(age+i*40)%120/120.0;
            ring(b,ps,d.center.add(0,0.2+phase*0.4,0),d.radius*(0.04+0.96*phase),0.3+phase*0.4,((int)(160*(1-phase)*fade)<<24)|0xFF6570,cam);
        }
        if(age<80)for(int i=0;i<3;i++){
            double phase=Math.max(0,Math.min(1,(age-i*10)/60));
            ring(b,ps,d.center.add(0,0.4+i*0.6,0),d.radius*phase,0.9,((int)(220*(1-phase)*fade)<<24)|0xFFE4DB,cam);
        }
        // 神龛上方的三层旋转冠环，以及盘旋上升的咒力带。
        for(int j=0;j<3;j++){
            double radius=9+j*4,spin=age*(j%2==0?0.012:-0.012);
            Vec3 c=d.shrine.add(0,13+j*4,0);
            ring(b,ps,c,radius,0.12,((int)(160*fade)<<24)|0xFF4D62,cam);
            for(int i=0;i<12;i++){
                double a=i*Math.PI/6+spin;Vec3 radial=new Vec3(Math.cos(a),0,Math.sin(a));Vec3 p=c.add(radial.scale(radius));
                ribbon(b,ps,p,p.add(radial.scale(1.8)).add(0,1.6,0),0.14,((int)(210*fade)<<24)|0xFFD3BB,cam);
            }
        }
        for(int strand=0;strand<5;strand++)for(int i=0;i<48;i++){
            double a=age*0.035+strand*Math.PI*2/5+i*0.18,h=i*0.8,r=6+i*0.1;
            Vec3 p=d.shrine.add(Math.cos(a)*r,h,Math.sin(a)*r),q=d.shrine.add(Math.cos(a+0.18)*(r+0.1),h+0.8,Math.sin(a+0.18)*(r+0.1));
            ribbon(b,ps,p,q,0.12,((int)((190-i*2)*fade)<<24)|(strand==0?0xFFE0BD:0xF92B46),cam);
        }
        for(int i=0;i<4;i++){
            double phase=(age+i*20)%80/80.0;
            ring(b,ps,d.shrine.add(0,8+phase*48,0),6+phase*18,0.11,((int)(145*(1-phase)*fade)<<24)|0xFFBDC6,cam);
        }
        for(int i=0;i<16;i++){
            double a=i*Math.PI/8+age*0.007;Vec3 p=d.shrine.add(Math.cos(a)*22,2,Math.sin(a)*22);
            ribbon(b,ps,p,p.add(0,4+pulse*5+(i%3)*2,0),0.055,((int)(125*fade)<<24)|0xFF5877,cam);
        }
        Vec3 bottom=d.shrine.add(0,1,0),top=d.shrine.add(0,100+Math.sin(age*0.04)*10,0);
        ribbon(b,ps,bottom,top,2.8,((int)((30+pulse*18)*fade)<<24)|0xFF143D,cam);
        ribbon(b,ps,bottom,top,0.25,((int)(130*fade)<<24)|0xFFD7CE,cam);
    }
    private static void bow(BufferBuilder b,PoseStack ps,Vec3 center,Vec3 forward,float draw,Vec3 cam,boolean purple){int outer=purple?0xFF6F16D8:0xFFFF420A,inner=purple?0xFFE8B7FF:0xFFFFEAA0,string=purple?0xFFD98AFF:0xFFFFBE3B,tip=purple?0xFFF5D6FF:0xFFFFF5BB;Vec3 side=new Vec3(-forward.z,0,forward.x).normalize();Vec3 up=side.cross(forward).normalize();Vec3 top=center.add(up.scale(1.15)),bottom=center.add(up.scale(-1.15));Vec3 prev=bottom;for(int i=1;i<=16;i++){double t=i/16.0;Vec3 q=center.add(up.scale(-1.15+2.3*t)).add(forward.scale(Math.sin(t*Math.PI)*0.4));ribbon(b,ps,prev,q,0.12,outer,cam);ribbon(b,ps,prev,q,0.035,inner,cam);prev=q;}Vec3 pull=center.subtract(forward.scale(0.2+Math.min(1,draw)*0.9));ribbon(b,ps,top,pull,0.022,string,cam);ribbon(b,ps,bottom,pull,0.022,string,cam);Vec3 tipPoint=center.add(forward.scale(1.6));ribbon(b,ps,pull,tipPoint,0.075,tip,cam);ribbon(b,ps,tipPoint.subtract(forward.scale(0.35)).add(up.scale(0.2)),tipPoint,0.09,outer,cam);ribbon(b,ps,tipPoint.subtract(forward.scale(0.35)).subtract(up.scale(0.2)),tipPoint,0.09,outer,cam);}
    @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles e){if(tick<shakeUntil){float f=(shakeUntil-tick)/14f;e.setRoll(e.getRoll()+(float)Math.sin((tick+e.getPartialTick())*2.4)*0.65f*f);e.setPitch(e.getPitch()+(float)Math.cos((tick+e.getPartialTick())*1.8)*0.4f*f);}}
    @SubscribeEvent public static void fov(ViewportEvent.ComputeFov e){if(tick<shakeUntil&&e.usedConfiguredFov())e.setFOV(e.getFOV()+4*(shakeUntil-tick)/14.0);}
    /** 十字斩保留黑白包边；虚空及传承后的内部同样接入施术者壁纸。 */
    static void monochromeCut(BufferBuilder b,PoseStack ps,Vec3 p,Vec3 axis,float length,float alpha,Vec3 cam){
        boolean previousRed=redCut,previousSoul=soulCut;redCut=false;soulCut=false;
        try{cut(b,ps,p,axis,length,alpha,cam);}finally{redCut=previousRed;soulCut=previousSoul;}
    }
    static void coloredCut(BufferBuilder b,PoseStack ps,Vec3 p,Vec3 axis,float length,float alpha,Vec3 cam,boolean red){redCut=red;cut(b,ps,p,axis,length,alpha,cam);redCut=false;}
    private static void cut(BufferBuilder b,PoseStack ps,Vec3 p,Vec3 axis,float length,float alpha,Vec3 cam){
        axis=axis.normalize();
        SlashStyle.draw(b,ps,p.subtract(axis.scale(length/2)),p.add(axis.scale(length/2)),Math.min(.4,Math.max(.06,length*.018)),alpha,cam);
    }
    static void ring(BufferBuilder b,PoseStack ps,Vec3 c,double radius,double width,int color,Vec3 cam){for(int i=0;i<64;i++){double a=i*Math.PI/32,z=(i+1)*Math.PI/32;Vec3 p=c.add(Math.cos(a)*radius,0,Math.sin(a)*radius),q=c.add(Math.cos(z)*radius,0,Math.sin(z)*radius);ribbon(b,ps,p,q,width,color,cam);}}
    /** 黑绳：从施术者眼睛甩出去，末端下垂，像一根被舞动的绳子。 */
    private static void drawRope(BufferBuilder b,PoseStack ps,Network.Effect e,Vec3 cam,float age){
        Vec3 eye=e.position(),dir=e.direction().normalize();
        double len=Math.max(1,e.size()),extend=Math.min(1,age/4.0);
        double sway=Math.sin(age*0.9)*0.35;
        Vec3 prev=eye;
        for(int i=1;i<=22;i++){
            double t=i/22.0*extend;
            Vec3 q=eye.add(dir.scale(len*t))
                .add(new Vec3(-dir.z,0,dir.x).normalize().scale(Math.sin(t*Math.PI)*sway))
                .add(0,-Math.sin(Math.PI*t)*0.55,0);
            ribbon(b,ps,prev,q,0.11,0xFF16100C,cam);
            ribbon(b,ps,prev,q,0.05,0xFF3B2A1E,cam);
            prev=q;
        }
    }
    static void ribbon(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 z,double width,int color,Vec3 cam){
        Vec3 line=z.subtract(a);if(line.lengthSqr()<1.0E-8)return;
        Vec3 edge=line.cross(cam.subtract(a));
        // 第一人称正对光束时相机与线段共线，改用稳定轴避免四边形退化成零面积。
        if(edge.lengthSqr()<1.0E-8)edge=line.cross(new Vec3(0,1,0));
        if(edge.lengthSqr()<1.0E-8)edge=line.cross(new Vec3(1,0,0));
        edge=edge.normalize().scale(width);quad(b,ps,a.add(edge),z.add(edge),z.subtract(edge),a.subtract(edge),color);
    }
    static void quad(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 c,Vec3 d,Vec3 e,int color){vertex(b,ps,a,color);vertex(b,ps,c,color);vertex(b,ps,d,color);vertex(b,ps,e,color);}
    private static void vertex(BufferBuilder b,PoseStack ps,Vec3 p,int color){color=VoidTechniqueClient.tint(color);b.vertex(ps.last().pose(),(float)p.x,(float)p.y,(float)p.z).color((color>>16)&255,(color>>8)&255,color&255,(color>>>24)&255).endVertex();}
}
