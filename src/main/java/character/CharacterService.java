package character;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.*;
import character.toji.TojiCombat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.util.FakePlayer;

/** 服务端角色入口：模板授权、分角色存档、个人线推进；旧存档没有角色编号时不改变原玩法。 */
public final class CharacterService {
    private static final String ID="SelectedCharacter", PROFILES="CharacterProfiles";
    private static final List<String> SHARED=List.of("Relations","DamageMultiplier","VoidFloor","MemoryGiven","ChoiceBookGiven","R7Migrated","CharacterSwitchAfter","VoidKeyGiven","VoidSettlementR60","SoulRuinBuilt","SoulRuinAttempted");
    public static CharacterDefinition selected(Player p) { return p instanceof FakePlayer?null:CharacterRegistry.get(Curse.data(p).getString(ID)); }
    public static boolean active(Player p) { return selected(p)!=null; }
    public static boolean family(Player p,CharacterDefinition.Family f) { var d=selected(p);return d!=null&&d.family()==f; }
    public static boolean toji(Player p) { var d=selected(p);return d!=null&&d.resource()==CharacterDefinition.Resource.STAMINA; }
    public static boolean yuji(Player p) { return family(p,CharacterDefinition.Family.MARTIAL); }
    public static boolean permits(Player p,CharacterDefinition.Family f) { var d=selected(p);return d==null||d.family()==f; }
    public static int rank(Player p) {
        var d=selected(p);if(d==null||d.family()==CharacterDefinition.Family.SHRINE)return 0;
        // 事件型个人线（如乙骨忧太）按已写入的事件标记计数，不再看累计击杀。
        if(d.hasEvents()){int done=0;var data=Curse.data(p);for(var c:d.events())if(data.getBoolean("ChapterDone_"+c.id()))done++;return done;}
        return d.rank(Curse.data(p).getInt("CharacterKills"));
    }
    public static CharacterConfig.Stats stats(Player p) {
        var d=selected(p);if(d==null)return CharacterConfig.Stats.ZERO;
        return d.hasEvents()?d.eventStats(rank(p)):d.stats(rank(p));
    }
    /** 事件章节是否已完成（存于该角色的存档段，切角色/重登都保留）。 */
    public static boolean chapterDone(Player p,String chapterId){return Curse.data(p).getBoolean("ChapterDone_"+chapterId);}
    /**
     * 事件监听器完成一章时调用：写标记 → 重算属性 → 立刻发该章奖励 → 提示。
     * 重复调用是安全的（标记已存在时直接返回）。
     */
    public static void markChapter(ServerPlayer p,String chapterId) {
        var d=selected(p);if(d==null||!d.hasEvents())return;
        if(chapterDone(p,chapterId))return;
        Curse.data(p).putBoolean("ChapterDone_"+chapterId,true);
        Curse.apply(p);TojiCombat.attributes(p);CharacterRewards.grant(p);Curse.sync(p);CharacterPackets.sync(p,true);
        for(var c:d.events())if(c.id().equals(chapterId)){p.sendSystemMessage(Component.literal("个人线推进："+c.title()+" 完成。"));break;}
        p.sendSystemMessage(Component.literal(guide(p)));
    }
    /** 事件型个人线的 J 键说明：显示每章真实条件与当前进度。 */
    private static String eventGuide(Player p,CharacterDefinition d) {
        int done=rank(p);
        if(done>=d.events().size())return d.name()+" · 个人线完成 · 阶段"+done+" · 术式与肉体成长已生效";
        var c=d.events().get(done);
        String reward=c.grants().summary();
        return d.name()+" · "+c.title()+" · "+c.goal()+(reward.isBlank()?" · 奖励：属性成长":" · 奖励："+reward);
    }
    public static boolean allowsSkill(Player p,int skill) {
        var d=selected(p);if(d==null)return !CharacterSkill.custom(skill);
        if(CharacterSkill.custom(skill))return d.config().skill(skill)!=null&&d.unlocked(skill,rank(p));
        if(!d.hasWheel())return false;
        if(d.family()==CharacterDefinition.Family.SHRINE)return !(skill>=22&&skill<=23)&&!(skill>=34&&skill<=42);
        return d.unlocked(skill,rank(p))||(d.family()==CharacterDefinition.Family.BLOOD&&skill==BloodRules.DEATH_STAR);
    }
    public static boolean allowsAction(Player p,SkillAction action) {
        var d=selected(p);if(d==null||action==SkillAction.CANCEL_CHARGE)return true;
        if(toji(p))return false;
        int skill=switch(action){
            case GOJO_BLUE, GOJO_BLUE_MAX -> 34; case GOJO_RED, GOJO_RED_MAX -> 35;
            case GOJO_PURPLE -> 36; case GOJO_UNLIMITED -> 37; case GOJO_TELEPORT -> 38;
            case GOJO_MELEE -> 39; case GOJO_DOMAIN -> 40;
            case FROST_CALM -> 22; case ICE_FALL -> 23;
            case GUARDIAN_PUNCH -> 7; case GUARDIAN_DIVERGENT -> 8; case GUARDIAN_COMBO -> 13;
            default -> -1;
        };
        if(d.family()!=CharacterDefinition.Family.SHRINE&&skill>=0&&!d.unlocked(skill,rank(p)))return false;
        return switch(d.family()) {
            case LIMITLESS -> action.name().startsWith("GOJO_");
            case ICE -> action==SkillAction.FROST_CALM||action==SkillAction.ICE_FALL;
            case MARTIAL -> action==SkillAction.GUARDIAN_PUNCH||action==SkillAction.GUARDIAN_DIVERGENT||action==SkillAction.GUARDIAN_COMBO;
            case BLOOD -> action==SkillAction.ROTATE;
            case SHRINE -> !action.name().startsWith("GOJO_")&&action!=SkillAction.FROST_CALM&&action!=SkillAction.ICE_FALL;
            default -> false;
        };
    }
    public static boolean hasBook(ServerPlayer p) { return p.getInventory().countItem(ProgressionContent.MEMORY.get())>0; }
    public static void choose(ServerPlayer p,String id) {
        var definition=CharacterRegistry.get(id);
        if(definition==null||!p.isAlive()||p.isSpectator()||!hasBook(p))return;
        var old=selected(p);if(old!=null&&old.id().equals(id)){CharacterPackets.sync(p,true);return;}
        if(p.isPassenger()||p.isSleeping()||Curse.active(p,"CharacterSwitchAfter")||Curse.active(p,"Combat")||Curse.active(p,"FinaleNoRecovery")||Skills.sealing(p)||Skills.sustaining(p)||GojoCombat.domainOpen(p)||BloodDomain.active(p)||GuardianPlayerSkills.sustaining(p)||FurnaceMax.active(p)||BloodArts.casting(p)||DismantleMotion.active(p)||CombatExpansion.controlled(p)||SoulCombat.suppressed(p)||Curse.burnout(p)||DeathStar.anemic(p)||DeathStar.charging(p)||PrisonSystem.locked(p)||ObserverRitual.active(p)||TenShadows.locked(p)||SixfoldFinale.active(p)||SixfoldFinale.controlled(p)||DomainClash.active(p)||GojoEntity.mindLocked(p)||Uraume.locked(p)||NetBranches.locked(p)||GuardianPlayerSkills.locked(p)) {
            p.sendSystemMessage(Component.literal("现在不能切换角色：请收起领域，结束战斗、控制、蓄力或贫血，并等待10秒后重试。"));return;
        }
        float before=p.getHealth();
        CharacterBridge.stop(p);
        var current=Curse.data(p);var profiles=current.getCompound(PROFILES).copy();
        String oldId=old==null?legacyId(current):old.id();
        var snapshot=current.copy();snapshot.remove(PROFILES);snapshot.remove(ID);
        if(!oldId.isEmpty())profiles.put(oldId,snapshot);
        CompoundTag next=profiles.contains(id)?profiles.getCompound(id).copy():initial(definition);
        for(String key:SHARED){next.remove(key);if(current.contains(key))next.put(key,current.get(key).copy());}
        // 回程坐标属于玩家所在世界，不属于角色，换线后仍能回原地。
        for(String key:current.getAllKeys())if(key.startsWith("SoulReturn")||key.startsWith("VoidReturn"))next.put(key,current.get(key).copy());
        if(definition.family()==CharacterDefinition.Family.SHRINE)next.putBoolean("Memory",true);
        next.putBoolean("ChoiceBookGiven",true);
        next.put(ID,net.minecraft.nbt.StringTag.valueOf(id));next.put(PROFILES,profiles);
        next.putLong("CharacterSwitchAfter",p.level().getGameTime()+200);
        p.getPersistentData().put("SukunaProgress",next);
        if(!p.isCreative()&&!p.isSpectator()&&!Curse.awakened(p)){p.getAbilities().mayfly=false;p.getAbilities().flying=false;p.onUpdateAbilities();}
        Curse.apply(p);TojiCombat.attributes(p);p.setHealth(Math.max(1,Math.min(before,p.getMaxHealth())));
        CharacterRewards.grant(p);
        Curse.sync(p);CharacterPackets.sync(p,true);TenShadows.sync(p);ShadowSystem.sync(p);AuthorityPackets.sync(p);
        p.sendSystemMessage(Component.literal("已选择 "+definition.name()+" · "+definition.technique()+"。"+(definition.hasWheel()?"长按N打开中央轮盘，R释放，J查看个人线。":"无咒力、无轮盘：R突进，潜行+R重击，跳跃+R跃身；J查看个人线。")));
    }
    private static String legacyId(CompoundTag tag) { return tag.getBoolean("Memory")?"sukuna":tag.getBoolean("GojoMemory")?"gojo":tag.getBoolean("UraumeMemory")?"uraume":"sukuna"; }
    private static CompoundTag initial(CharacterDefinition d) {
        var tag=new CompoundTag();tag.putDouble("Energy",d.resource()==CharacterDefinition.Resource.STAMINA?0:d.family()==CharacterDefinition.Family.SHRINE?1000:d.config().base().energy());tag.putDouble("TojiStamina",100);tag.putBoolean("MemoryGiven",true);
        switch(d.family()) {
            case SHRINE -> tag.putBoolean("Memory",true);
            case LIMITLESS -> tag.putBoolean("GojoMemory",true);
            case ICE -> tag.putBoolean("UraumeMemory",true);
            // 不伪造宿傩、佛体或守护者通关：赤血和虎杖用角色授权入口。
            default -> {}
        }
        return tag;
    }
    public static String guide(Player p) {
        var d=selected(p);if(d==null)return "";
        if(d.family()==CharacterDefinition.Family.SHRINE)return "";
        if(d.hasEvents())return eventGuide(p,d);
        int kills=Curse.data(p).getInt("CharacterKills"),rank=rank(p);
        if(rank>=d.story().size())return d.name()+" · 个人线完成 · 阶段"+rank+" · "+(toji(p)?"体术上限"+(int)TojiCombat.maximum(p):"术式与肉体成长已生效");
        var chapter=d.story().get(rank);
        String rewards=d.rewards(rank);if(rewards.length()>800)rewards=rewards.substring(0,800)+"…按J查看详情";
        return d.name()+" · "+chapter.title()+" · "+chapter.goal()+" ["+kills+"/"+chapter.kills()+"] · 奖励："+rewards;
    }
    public static void showStory(ServerPlayer p) {
        var d=selected(p);if(d==null)return;
        p.sendSystemMessage(Component.literal(guide(p)));
        int rank=rank(p);
        if(d.hasEvents()){
            for(int i=0;i<d.events().size();i++){
                var c=d.events().get(i);
                String reward=c.grants().summary();
                p.sendSystemMessage(Component.literal((i<rank?"✓ ":"○ ")+c.title()+"："+c.goal()+(reward.isBlank()?"":" → "+reward)));
            }
            return;
        }
        for(int i=0;i<d.story().size();i++){
            var chapter=d.story().get(i);
            p.sendSystemMessage(Component.literal((i<rank?"✓ ":"○ ")+chapter.title()+"："+chapter.goal()+"（累计"+chapter.kills()+"） → "+d.rewards(i)));
        }
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void death(LivingDeathEvent e) {
        if(e.isCanceled())return;
        ServerPlayer p=CombatSettings.caster(e.getSource());if(p==null||!active(p)||family(p,CharacterDefinition.Family.SHRINE))return;
        // 不刷家畜；玩家必须在敌友菜单中明确标敌，且由服务端允许PvP。
        boolean enemy=e.getEntity() instanceof Enemy||e.getEntity() instanceof cn.blockforge.generated.nnnnr1201forge2.GojoEntity||e.getEntity() instanceof cn.blockforge.generated.nnnnr1201forge2.YujiEntity||e.getEntity() instanceof cn.blockforge.generated.nnnnr1201forge2.GuardianEntity||e.getEntity() instanceof Player target&&p.server.isPvpAllowed()&&CombatSettings.relation(p,target.getUUID())==CombatSettings.ENEMY;
        if(!enemy||e.getEntity()==p||CombatSettings.relation(p,e.getEntity().getUUID())==CombatSettings.ALLY||p.getUUID().equals(CombatSafety.ownerId(e.getEntity())))return;
        if(toji(p)&&!e.getSource().is(DamageTypes.PLAYER_ATTACK))return;
        int old=rank(p);var tag=Curse.data(p);tag.putInt("CharacterKills",Math.min(1000000,tag.getInt("CharacterKills")+1));
        if(rank(p)>old){Curse.apply(p);TojiCombat.attributes(p);CharacterRewards.grant(p);p.sendSystemMessage(Component.literal("个人线推进："+guide(p)));}
        Curse.sync(p);
    }
    private CharacterService() {}
}
